package openraven.ravenxd.module.render;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;

public class ExtraBobbingModule extends Module {
    public static SliderSetting offset;
    private boolean previousBobbing;

    public ExtraBobbingModule(int key) {
        super("Extra Bobbing", Category.render, key);
    }

    public void link() {
        offset = (SliderSetting) ModuleManager.setting(this, "Offset");
    }

    @Override
    public void onUpdate() {
        if (!mc.gameSettings.viewBobbing) {
            mc.gameSettings.viewBobbing = true;
        }
        if (mc.thePlayer.movementInput.moveForward != 0.0f || mc.thePlayer.movementInput.moveStrafe != 0.0f) {
            mc.thePlayer.cameraYaw = (float) ((double) mc.thePlayer.cameraYaw + offset.getInput() / 10.0);
        }
    }

    @Override
    public void onDisable() {
        mc.gameSettings.viewBobbing = this.previousBobbing;
    }

    @Override
    public void onEnable() {
        this.previousBobbing = mc.gameSettings.viewBobbing;
        if (!this.previousBobbing) {
            mc.gameSettings.viewBobbing = true;
        }
    }
}
