package xyz.daikirai.module.render;

public final class CameraModules {
    private CameraModules() {
    }

    public static float hurtCam() {
        return NoHurtCamModule.instance != null && NoHurtCamModule.instance.isEnabled() ? (float) (int) NoHurtCamModule.multiplier.getInput() : 14.0f;
    }

    public static boolean shouldCameraClip() {
        return NoCameraClipModule.instance == null || !NoCameraClipModule.instance.isEnabled();
    }
}
