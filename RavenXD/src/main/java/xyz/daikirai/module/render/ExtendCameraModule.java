package xyz.daikirai.module.render;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ExtendCameraModule extends Module {
    public static ExtendCameraModule instance;
    public static SliderSetting distance;

    public ExtendCameraModule(int key) {
        super("Extend Camera", Category.render, key);
        instance = this;
        this.queueAfterDisable = true;
    }

    public void link() {
        distance = (SliderSetting) ModuleManager.setting(this, "Distance");
    }

    public static double distance() {
        return instance != null && instance.isEnabled() ? distance.getInput() : (double) 4;
    }

    private boolean setDistance(double d) {
        try {
            ReflectionHelper.setPrivateValue(EntityRenderer.class, mc.entityRenderer, (float) d, "thirdPersonDistance", "field_78490_B");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onUpdate() {
        this.setDistance(distance.getInput());
    }

    @Override
    public void afterDisable() {
        if (!this.isEnabled()) {
            this.setDistance(4);
        }
    }
}
