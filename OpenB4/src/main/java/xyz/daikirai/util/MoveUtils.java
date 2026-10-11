package xyz.daikirai.util;

import xyz.daikirai.mixin.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public final class MoveUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private MoveUtils() {
    }

    public static float moveYaw(float yaw, float forward, float strafe) {
        float f4 = 1.0f;
        if (forward < 0.0f) {
            yaw += 180.0f;
            f4 = -0.5f;
        } else if (forward > 0.0f) {
            f4 = 0.5f;
        }
        if (strafe > 0.0f) {
            yaw -= 90.0f * f4;
        } else if (strafe < 0.0f) {
            yaw += 90.0f * f4;
        }
        return yaw;
    }

    public static float moveYaw(float yaw) {
        return moveYaw(yaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
    }

    public static void setSpeed(double speed, float yaw) {
        if (speed == 0.0) {
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionX = 0.0;
            return;
        }
        mc.thePlayer.motionX = -Math.sin(yaw *= (float) Math.PI / 180) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }

    public static void setSpeed(double speed) {
        float yaw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.movementInput.moveForward < 0.0f) {
            yaw += 180.0f;
        }
        setSpeed(speed, yaw);
    }

    public static void strafe(double speed) {
        setSpeed(speed, moveYaw(RotationManager.movementYaw()));
    }

    public static double speed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public static Timer timer() {
        return ((IMinecraft) mc).getTimer();
    }

    public static void setTimer(float speed) {
        Timer timer = timer();
        if (timer != null) {
            timer.timerSpeed = speed;
        }
    }

    public static void resetTimer() {
        setTimer(1.0f);
    }
}
