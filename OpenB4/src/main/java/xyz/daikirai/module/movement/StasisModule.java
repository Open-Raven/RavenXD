package xyz.daikirai.module.movement;

import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class StasisModule extends Module {
    public static StasisModule instance;
    public static SliderSetting pulse;
    public static boolean frozen;
    private static double savedX, savedY, savedZ;
    private static int frozenAt;
    private static int delay;

    public StasisModule(int key) {
        super("Stasis", Category.movement, key);
        instance = this;
    }

    public void link() {
        pulse = (SliderSetting) ModuleManager.setting(this, "Pulse");
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (delay > 0 && --delay == 0) {
            freeze(true);
        }
        if (frozen) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
            if (!pulse.isAtMin() && mc.thePlayer.ticksExisted - frozenAt >= (int) pulse.getInput()) {
                freeze(false);
                delay = 1;
            }
        }
    }

    public static void freeze(boolean b) {
        Minecraft mc = Minecraft.getMinecraft();
        if (b) {
            savedX = mc.thePlayer.motionX;
            savedY = mc.thePlayer.motionY;
            savedZ = mc.thePlayer.motionZ;
            frozenAt = mc.thePlayer.ticksExisted;
        } else {
            mc.thePlayer.motionX = savedX;
            mc.thePlayer.motionY = savedY;
            mc.thePlayer.motionZ = savedZ;
        }
        frozen = b;
    }

    @Override
    public boolean onSendPacketAlways(net.minecraft.network.Packet<?> packet) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!frozen) {
            return true;
        }
        C03PacketPlayer p = (C03PacketPlayer) packet;
        if (Hypixel.onHypixel() && !(p instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            PacketUtils.send(new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
            return false;
        }
        return true;
    }

    public static void onS08() {
        freeze(false);
        delay = 3;
    }

    @Override
    public void onVelocityAlways() {
        if (!frozen) {
            return;
        }
        freeze(false);
        delay = 4;
    }

    @Override
    public void onDisable() {
        freeze(false);
        delay = 0;
    }

    @Override
    public void onPostPlayerInput() {
        Minecraft mc = Minecraft.getMinecraft();
        if (frozen || delay == 1) {
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
        }
    }

    @Override
    public void onEnable() {
        delay = 2;
    }
}
