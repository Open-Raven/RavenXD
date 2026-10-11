package xyz.daikirai.module.render;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.SliderSetting;

public class NoHurtCamModule extends Module {
    public static NoHurtCamModule instance;
    public static SliderSetting multiplier;

    public NoHurtCamModule(int key) {
        super("NoHurtCam", Category.render, key);
        instance = this;
    }

    public void link() {
        multiplier = (SliderSetting) ModuleManager.setting(this, "Multiplier");
    }
}
