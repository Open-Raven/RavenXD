package openraven.ravenxd.module.movement;

import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.client.Overlay;
import openraven.ravenxd.client.RenderDispatcher;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;

public class FlyModule extends Module {
    public static FlyModule instance;
    public static SliderSetting mode, horizontalSpeed, verticalSpeed;
    public static ButtonSetting showBps, stopMotion;
    public static SliderSetting motionHorizontal, motionVertical;
    public static ButtonSetting yBob;

    public enum Mode {
        VANILLA, FAST, MOTION, MATRIX_JUMP, MATRIX_GLIDE, MATRIX_UP, AAC, NCP
    }

    public static Mode current = Mode.VANILLA;
    private boolean wasFlying;
    private final float baseSpeed = 0.05f;
    private boolean enableGround;

    public FlyModule(int key) {
        super("Fly", Category.movement, key);
        instance = this;
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Fly");
        horizontalSpeed = (SliderSetting) ModuleManager.setting(this, "Horizontal speed");
        verticalSpeed = (SliderSetting) ModuleManager.setting(this, "Vertical speed");
        showBps = (ButtonSetting) ModuleManager.setting(this, "Show BPS");
        stopMotion = (ButtonSetting) ModuleManager.setting(this, "Stop motion");
        motionHorizontal = (SliderSetting) ModuleManager.setting(this, "Motion horizontal");
        motionVertical = (SliderSetting) ModuleManager.setting(this, "Motion vertical");
        yBob = (ButtonSetting) ModuleManager.setting(this, "Y bob");
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.thePlayer.capabilities.isFlying = mc.thePlayer.capabilities.allowFlying ? this.wasFlying : false;
        this.wasFlying = false;
        mc.thePlayer.jumpMovementFactor = 0.02f;
        if (current == Mode.VANILLA) {
            mc.thePlayer.capabilities.setFlySpeed(this.baseSpeed);
        }
        if (stopMotion.enabled) {
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionX = 0.0;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (!showBps.enabled || e.phase != TickEvent.Phase.END || !Overlay.canRender()) {
            return;
        }
        drawBps();
    }

    public static void drawBps() {
        Minecraft mc = Minecraft.getMinecraft();
        double d = RenderDispatcher.bps(openraven.ravenxd.module.player.FreecamModule.camera == null ? mc.thePlayer : openraven.ravenxd.module.player.FreecamModule.camera, 2);
        int color;
        if (d < 10.0) {
            color = Color.green.getRGB();
        } else if (d < 30.0) {
            color = Color.yellow.getRGB();
        } else if (d < 60.0) {
            color = Color.orange.getRGB();
        } else if (d < 160.0) {
            color = Color.red.getRGB();
        } else {
            color = Color.black.getRGB();
        }
        String s = d + "bps";
        int x = RenderUtils.scaledWidth / 2 - mc.fontRendererObj.getStringWidth(s) / 2;
        int y = RenderUtils.scaledHeight / 2 + 12;
        mc.fontRendererObj.drawString(s, (float) x, (float) y, color, false);
    }

    @Override
    public void onEnable() {
        if (openraven.ravenxd.module.player.FreecamModule.camera != null) {
            openraven.ravenxd.client.Chat.print("&cCannot enable while using Freecam!");
            this.disable();
            return;
        }
        this.wasFlying = Minecraft.getMinecraft().thePlayer.capabilities.isFlying;
    }

    @Override
    public void guiUpdate() {
        current = Mode.values()[(int) mode.getInput()];
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        switch (current) {
            case VANILLA:
                mc.thePlayer.capabilities.setFlySpeed((float) ((double) this.baseSpeed * horizontalSpeed.getInput()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case FAST:
                mc.thePlayer.motionY = 0.0;
                if (GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
                    mc.thePlayer.motionY += 0.5 * verticalSpeed.getInput();
                }
                if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
                    mc.thePlayer.motionY -= 0.5 * verticalSpeed.getInput();
                }
                if (PlayerUtils.isMoving()) {
                    MoveUtils.strafe(0.7 * horizontalSpeed.getInput());
                    break;
                }
                mc.thePlayer.motionZ = 0.0;
                mc.thePlayer.motionX = 0.0;
                break;
            default:
                if (openraven.ravenxd.module.player.FreecamModule.camera == null) {
                    this.hynix(mc);
                }
        }
    }

    // The following modes are ported from Hynix's Fly.

    private static boolean jumpKey() {
        return GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindJump);
    }

    private static boolean sneakKey() {
        return GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak);
    }

    // Moves at the given speed in the input direction, or stops when there is no input.
    private static void setMotion(double speed) {
        if (PlayerUtils.isMoving()) {
            MoveUtils.strafe(speed);
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    private void hynix(Minecraft mc) {
        EntityPlayerSP p = mc.thePlayer;
        double h = motionHorizontal.getInput();
        double v = motionVertical.getInput();
        switch (current) {
            case MOTION:
                setMotion(h);
                p.motionY = sneakKey() ? -v : jumpKey() ? v : 0.0;
                if (yBob.isToggled() && !sneakKey() && !jumpKey()) {
                    p.motionY += p.ticksExisted % 2 == 0 ? 0.1 : -0.1;
                }
                break;
            case MATRIX_UP:
                p.motionY += 0.05499999761581421;
                break;
            case NCP: {
                boolean up = jumpKey(), down = p.isSneaking(), move = PlayerUtils.isMoving();
                double speed = up || down ? 0.0 : (move ? 4.01 / 20.0 : 0.0);
                float yaw = (float) Math.toRadians(MoveUtils.moveYaw(p.rotationYaw));
                double ySpeed = up ? 0.062 : down ? -0.102 : move ? 0.0 : (p.ticksExisted % 2 == 0 && !p.onGround ? 0.01 : -0.01);
                double x = p.posX - Math.sin(yaw) * speed, y = p.posY + ySpeed, z = p.posZ + Math.cos(yaw) * speed;
                p.motionX = 0.0;
                p.motionZ = 0.0;
                MoveUtils.strafe(-speed / 5.0);
                p.motionY = 1.0E-45;
                p.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                p.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(0.0, y + ySpeed, 0.0, true));
                break;
            }
            case MATRIX_JUMP:
                if (p.onGround) {
                    p.jump();
                } else {
                    setMotion(Math.min(h, 1.97));
                    p.motionY = v;
                }
                break;
            case MATRIX_GLIDE:
                setMotion(h);
                p.motionY = -0.003;
                break;
            case AAC:
                if (!jumpKey() && !solid(mc, new BlockPos(p.posX, p.posY - 0.044, p.posZ))) {
                    this.enableGround = false;
                }
                if (p.onGround && p.isCollidedVertically) {
                    this.enableGround = true;
                }
                if (jumpKey() && this.enableGround) {
                    p.motionY = 0.42;
                }
                if (MoveUtils.speed() > 0.19) {
                    p.jumpMovementFactor = 0.17f;
                }
                p.motionX *= 1.005;
                p.motionZ *= 1.005;
                break;
        }
    }

    private static boolean solid(Minecraft mc, BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        return b.getMaterial().isSolid() && b.isFullCube();
    }

    // MatrixJump turns itself off when the server sets the player back.
    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (current == Mode.MATRIX_JUMP && packet instanceof S08PacketPlayerPosLook) {
            Minecraft.getMinecraft().addScheduledTask(this::disable);
        }
        return true;
    }
}
