package xyz.daikirai.module.movement;

import xyz.daikirai.util.TeleportPath;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class VClipModule extends Module {
    public static SliderSetting distance;
    public static ButtonSetting sendMessage, sendPackets;

    public VClipModule(int key) {
        super("VClip", Category.movement, key);
    }

    public void link() {
        distance = (SliderSetting) ModuleManager.setting(this, "Distance");
        sendMessage = (ButtonSetting) ModuleManager.setting(this, "Send message");
        sendPackets = (ButtonSetting) ModuleManager.setting(this, "Send packets");
    }

    private static String format(double d) {
        return d == Math.floor(d) ? Long.toString((long) d) : Double.toString(d);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        double d = distance.getInput();
        if (d != 0.0) {
            if (sendPackets.enabled) {
                TeleportPath.teleport(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + d, mc.thePlayer.posZ), false);
            } else {
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + d, mc.thePlayer.posZ);
            }
            if (sendMessage.enabled) {
                Chat.print("&7Teleported you " + (d > 0.0 ? "upwards" : "downwards") + " &b" + format(d) + " &7blocks.");
            }
        }
        this.disable();
    }
}
