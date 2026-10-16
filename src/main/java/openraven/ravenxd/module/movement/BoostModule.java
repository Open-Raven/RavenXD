package openraven.ravenxd.module.movement;

import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;

public class BoostModule extends Module {
    public static SliderSetting boostTicks, multiplier;
    private int ticks;
    private boolean restoreTimer;

    public BoostModule(int key) {
        super("Boost", Category.movement, key);
    }

    public void link() {
        boostTicks = (SliderSetting) ModuleManager.setting(this, "Boost ticks");
        multiplier = (SliderSetting) ModuleManager.setting(this, "Multiplier");
    }

    @Override
    public void onEnable() {
        if (TimerModule.instance != null && TimerModule.instance.isEnabled()) {
            this.restoreTimer = true;
            TimerModule.instance.disable();
        }
    }

    @Override
    public void onUpdate() {
        if (this.ticks++ == (int) boostTicks.getInput()) {
            this.disable();
            return;
        }
        MoveUtils.setTimer((float) multiplier.getInput());
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
        MoveUtils.resetTimer();
        if (this.restoreTimer) {
            this.restoreTimer = false;
            TimerModule.instance.enable();
        }
    }

    @Override
    public String getInfo() {
        return String.valueOf((int) boostTicks.getInput() - this.ticks);
    }
}
