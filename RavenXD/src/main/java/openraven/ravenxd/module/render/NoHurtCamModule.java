package openraven.ravenxd.module.render;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;

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
