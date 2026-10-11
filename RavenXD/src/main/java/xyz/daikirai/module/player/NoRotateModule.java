package xyz.daikirai.module.player;

import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class NoRotateModule extends Module {
    public static NoRotateModule instance;
    public static long lastS08;
    public static long lastSetback;

    public NoRotateModule(int key) {
        super("NoRotate", Category.player, key);
        instance = this;
    }

    public static boolean sinceS08(long ms) {
        return System.currentTimeMillis() - lastS08 < ms;
    }

    public static boolean sinceSetback(long ms) {
        return System.currentTimeMillis() - lastSetback < ms;
    }

    private static void onS08(S08PacketPlayerPosLook p) {
        lastS08 = System.currentTimeMillis();
        if (xyz.daikirai.module.movement.StasisModule.frozen) {
            xyz.daikirai.module.movement.StasisModule.onS08();
        }
        if (xyz.daikirai.module.movement.TimerModule.instance != null) {
            xyz.daikirai.module.movement.TimerModule.onS08();
        }
        if (xyz.daikirai.module.minigames.SkyWarsModule.pearlAt > 0L) {
            xyz.daikirai.module.minigames.SkyWarsModule.onSetback();
        }
    }

    private static void onSetback(S08PacketPlayerPosLook p) {
        lastSetback = lastS08;
        xyz.daikirai.module.Module longJump = xyz.daikirai.module.ModuleManager.enabled("Long Jump");
        if (longJump != null) {
            xyz.daikirai.client.Chat.moduleMessage(longJump, "&cReceived setback, disabling.");
            longJump.disable();
        }
    }

    @Override
    public boolean onReceivePacketAlways(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        Minecraft mc = Minecraft.getMinecraft();
        S08PacketPlayerPosLook p = (S08PacketPlayerPosLook) packet;
        if (mc.thePlayer == null) {
            return true;
        }
        onS08(p);
        if (p.getPitch() == 0.0f) {
            return true;
        }
        onSetback(p);
        if (instance == null || !instance.isEnabled()) {
            return true;
        }
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        float yaw = p.getYaw();
        float pitch = p.getPitch();
        if (p.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X)) {
            x += mc.thePlayer.posX;
        } else {
            mc.thePlayer.motionX = 0.0;
        }
        if (p.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
            y += mc.thePlayer.posY;
        } else {
            mc.thePlayer.motionY = 0.0;
        }
        if (p.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
            z += mc.thePlayer.posZ;
        } else {
            mc.thePlayer.motionZ = 0.0;
        }
        if (p.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
            pitch += mc.thePlayer.rotationPitch;
        }
        if (p.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
            yaw += mc.thePlayer.rotationYaw;
        }
        mc.thePlayer.prevPosX = x;
        mc.thePlayer.prevPosY = y;
        mc.thePlayer.prevPosZ = z;
        mc.thePlayer.setPosition(x, y, z);
        PacketUtils.send(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY, mc.thePlayer.posZ, yaw % 360.0f, pitch % 360.0f, false));
        try {
            boolean done = ReflectionHelper.getPrivateValue(NetHandlerPlayClient.class, mc.getNetHandler(), "doneLoadingTerrain", "field_147309_h");
            if (!done) {
                mc.thePlayer.prevPosX = mc.thePlayer.posX;
                mc.thePlayer.prevPosY = mc.thePlayer.posY;
                mc.thePlayer.prevPosZ = mc.thePlayer.posZ;
                ReflectionHelper.setPrivateValue(NetHandlerPlayClient.class, mc.getNetHandler(), true, "doneLoadingTerrain", "field_147309_h");
                mc.addScheduledTask(() -> mc.displayGuiScreen(null));
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
