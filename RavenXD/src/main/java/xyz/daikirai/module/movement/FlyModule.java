package xyz.daikirai.module.movement;

import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.client.RenderDispatcher;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;

public class FlyModule extends Module {
    public static FlyModule instance;
    public static SliderSetting mode, horizontalSpeed, verticalSpeed;
    public static ButtonSetting showBps, stopMotion;

    public enum Mode {
        VANILLA, FAST
    }

    public static Mode current = Mode.VANILLA;
    private boolean wasFlying;
    private final float baseSpeed = 0.05f;

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
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.thePlayer.capabilities.isFlying = mc.thePlayer.capabilities.allowFlying ? this.wasFlying : false;
        this.wasFlying = false;
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
        double d = RenderDispatcher.bps(xyz.daikirai.module.player.FreecamModule.camera == null ? mc.thePlayer : xyz.daikirai.module.player.FreecamModule.camera, 2);
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
        if (xyz.daikirai.module.player.FreecamModule.camera != null) {
            xyz.daikirai.client.Chat.print("&cCannot enable while using Freecam!");
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
        }
    }
}
