package xyz.daikirai.module.movement;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import net.minecraft.client.Minecraft;

public class StopMotionModule extends Module {
    public static ButtonSetting stopX, stopY, stopZ;

    public StopMotionModule(int key) {
        super("Stop Motion", Category.movement, key);
    }

    public void link() {
        stopX = (ButtonSetting) ModuleManager.setting(this, "Stop X");
        stopY = (ButtonSetting) ModuleManager.setting(this, "Stop Y");
        stopZ = (ButtonSetting) ModuleManager.setting(this, "Stop Z");
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (stopX.enabled) mc.thePlayer.motionX = 0.0;
        if (stopY.enabled) mc.thePlayer.motionY = 0.0;
        if (stopZ.enabled) mc.thePlayer.motionZ = 0.0;
        this.disable();
    }
}
