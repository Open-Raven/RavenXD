package xyz.daikirai.module.player;

import xyz.daikirai.client.Chat;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class FreecamModule extends Module {
    public static SliderSetting modeSetting, speed;
    public static ButtonSetting allowChatting, allowDigging, allowInteracting, allowPlacing, disableOnDamage, showArm;

    public enum Mode {
        Spoof, Freeze
    }

    public static Mode mode = Mode.Spoof;
    public static EntityOtherPlayerMP camera, body;
    private static final List<C00PacketKeepAlive> keepAlives = new ArrayList<C00PacketKeepAlive>();
    private static final List<C0FPacketConfirmTransaction> transactions = new ArrayList<C0FPacketConfirmTransaction>();
    private boolean wasInvisible;
    private final double[] chunk = new double[2];

    public FreecamModule(int key) {
        super("Freecam", Category.player, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        speed = (SliderSetting) ModuleManager.setting(this, "Speed");
        allowChatting = (ButtonSetting) ModuleManager.setting(this, "Allow chatting");
        allowDigging = (ButtonSetting) ModuleManager.setting(this, "Allow digging");
        allowInteracting = (ButtonSetting) ModuleManager.setting(this, "Allow interacting");
        allowPlacing = (ButtonSetting) ModuleManager.setting(this, "Allow placing");
        disableOnDamage = (ButtonSetting) ModuleManager.setting(this, "Disable on damage");
        showArm = (ButtonSetting) ModuleManager.setting(this, "Show arm");
    }

    @Override
    public void onEnable() {
        Minecraft mc = mc();
        this.wasInvisible = mc.thePlayer.isInvisible();
        mc.thePlayer.setInvisible(true);
        camera = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        camera.copyLocationAndAnglesFrom(mc.thePlayer);
        camera.setInvisible(true);
        body = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        body.copyLocationAndAnglesFrom(mc.thePlayer);
        body.rotationYawHead = mc.thePlayer.rotationYawHead;
        mc.theWorld.addEntityToWorld(camera.getEntityId(), camera);
        mc.theWorld.addEntityToWorld(body.getEntityId(), body);
        mc.setRenderViewEntity(camera);
    }

    @Override
    public void onDisable() {
        Minecraft mc = mc();
        if (mc.thePlayer != null) {
            mc.thePlayer.setInvisible(this.wasInvisible);
        }
        if (camera != null) {
            mc.setRenderViewEntity(mc.thePlayer);
            if (mc.theWorld != null) {
                mc.theWorld.removeEntity(camera);
                for (int i = -1; i <= 1; ++i) {
                    for (int j = -1; j <= 1; ++j) {
                        int x = mc.thePlayer.chunkCoordX + i;
                        int z = mc.thePlayer.chunkCoordZ + j;
                        mc.theWorld.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
                    }
                }
            }
            camera = null;
        }
        if (body != null) {
            if (mc.thePlayer != null) {
                mc.thePlayer.rotationYaw = body.rotationYaw;
                mc.thePlayer.rotationPitch = body.rotationPitch;
            }
            if (mc.theWorld != null) {
                mc.theWorld.removeEntity(body);
            }
            body = null;
        }
        if (Chat.inWorld() && mode == Mode.Freeze) {
            for (C00PacketKeepAlive p : keepAlives) {
                PacketUtils.sendThroughLag(p);
            }
            for (C0FPacketConfirmTransaction p : transactions) {
                PacketUtils.sendThroughLag(p);
            }
        }
        keepAlives.clear();
        transactions.clear();
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
    }

    @Override
    public boolean isFreecamActive() {
        return body != null && mode == Mode.Spoof;
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (!isFreecamActive()) {
            return null;
        }
        return new float[]{body.rotationYaw, body.rotationPitch};
    }

    @Override
    public void onPostPlayerInput() {
        if (mode == Mode.Freeze) {
            mc().thePlayer.movementInput.sneak = false;
        }
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        if (mode == Mode.Spoof) {
            e.sprinting = false;
            e.sneaking = false;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = mc();
        if (e.phase != TickEvent.Phase.END || camera == null) {
            return;
        }
        if (mc.theWorld == null || mc.currentScreen instanceof GuiGameOver) {
            this.disable();
            return;
        }
        if (disableOnDamage.isToggled() && mc.thePlayer.hurtTime > 0) {
            this.disable();
            return;
        }
        mc.thePlayer.setInvisible(true);
        mc.thePlayer.moveStrafing = 0.0f;
        mc.thePlayer.moveForward = 0.0f;
        double d = 0.215 * speed.getInput();
        if (!(mc.currentScreen instanceof GuiChat)) {
            double a;
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindForward)) {
                a = camera.rotationYaw * ((float) Math.PI / 180);
                camera.posX += -1.0 * Math.sin(a) * d;
                camera.posZ += Math.cos(a) * d;
            }
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindBack)) {
                a = camera.rotationYaw * ((float) Math.PI / 180);
                camera.posX -= -1.0 * Math.sin(a) * d;
                camera.posZ -= Math.cos(a) * d;
            }
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
                a = (camera.rotationYaw - 90.0f) * ((float) Math.PI / 180);
                camera.posX += -1.0 * Math.sin(a) * d;
                camera.posZ += Math.cos(a) * d;
            }
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
                a = (camera.rotationYaw + 90.0f) * ((float) Math.PI / 180);
                camera.posX += -1.0 * Math.sin(a) * d;
                camera.posZ += Math.cos(a) * d;
            }
            if (PlayerUtils.jumpKeyDown()) {
                camera.posY += 0.8 * d;
            }
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
                camera.posY -= 0.8 * d;
            }
        }
        int x = camera.chunkCoordX;
        int z = camera.chunkCoordZ;
        if (this.chunk[0] != (double) x || this.chunk[1] != (double) z) {
            mc.theWorld.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
        }
        this.chunk[0] = x;
        this.chunk[1] = z;
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (mode == Mode.Freeze) {
            if (packet instanceof C0BPacketEntityAction || packet instanceof C09PacketHeldItemChange || packet instanceof C16PacketClientStatus
                    || packet instanceof C0EPacketClickWindow || packet instanceof C0DPacketCloseWindow) {
                return false;
            }
            if (packet instanceof C00PacketKeepAlive) {
                keepAlives.add((C00PacketKeepAlive) packet);
                return false;
            }
            if (packet instanceof C0FPacketConfirmTransaction) {
                transactions.add((C0FPacketConfirmTransaction) packet);
                return false;
            }
        }
        if (packet instanceof C01PacketChatMessage && !allowChatting.isToggled()) {
            Chat.print("&cCannot send messages while using freecam!");
            return false;
        }
        if (packet instanceof C07PacketPlayerDigging && !allowDigging.isToggled()) {
            return false;
        }
        if (packet instanceof C02PacketUseEntity && !allowInteracting.isToggled()) {
            return false;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement && !allowPlacing.isToggled()) {
            return false;
        }
        return !(packet instanceof C0APacketAnimation) || allowDigging.isToggled() || allowInteracting.isToggled();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (!Chat.inWorld()) {
            return;
        }
        if (camera != null) {
            camera.rotationYaw = mc.thePlayer.rotationYaw;
            camera.prevRotationYaw = mc.thePlayer.prevRotationYaw;
            camera.prevRotationPitch = mc.thePlayer.prevRotationPitch;
            camera.rotationPitch = mc.thePlayer.rotationPitch;
            if (showArm.isToggled()) {
                mc.thePlayer.prevRenderArmYaw = camera.rotationYaw;
                mc.thePlayer.renderArmYaw = mc.thePlayer.prevRenderArmYaw;
            } else {
                mc.thePlayer.prevRenderArmPitch = 700.0f;
                mc.thePlayer.renderArmPitch = 700.0f;
            }
        }
        if (body != null) {
            Render3D.entityBox(body, 2, Theme.defaultTheme.getRGB(0.0), e.partialTicks);
        }
    }
}
