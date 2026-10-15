package openraven.ravenxd.util;

import openraven.ravenxd.util.rots.RotationManager;
import openraven.ravenxd.util.rots.compat.Compat;
import openraven.ravenxd.util.rots.compat.EventPlayerMotion;
import openraven.ravenxd.util.rots.stuff.RiseMovementFix;
import org.lwjgl.util.vector.Vector2f;


public final class Rots {
    public static final RotationManager manager = new RotationManager();
    private static final double INSTANT = 180.0;
    public static double speed;

    private Rots() {
    }

    public static float[] apply(float[] target, float lastYaw, float lastPitch, boolean movementFix) {
        Vector2f last = new Vector2f(lastYaw, lastPitch);
        Compat.previousRotation = last;
        manager.lastRotations = last;
        manager.lastServerRotations = last;
        if (manager.rotations == null) {
            manager.rotations = last;
        }
        double s = speed > 0.0 ? speed : INSTANT;
        speed = 0.0;
        manager.setRotations(new Vector2f(target[0], target[1]), s, movementFix ? RiseMovementFix.NORMAL : RiseMovementFix.OFF);
        return new float[]{manager.rotations.x, manager.rotations.y};
    }

    public static void finishTick(float yaw, float pitch) {
        manager.onMotion(new EventPlayerMotion(yaw, pitch));
    }

    public static void stop() {
        speed = 0.0;
        manager.stop();
    }
}
