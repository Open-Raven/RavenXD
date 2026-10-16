package openraven.ravenxd.util;

import openraven.ravenxd.util.rots.RotationManager;
import openraven.ravenxd.util.rots.compat.Compat;
import openraven.ravenxd.util.rots.compat.EventPlayerMotion;
import openraven.ravenxd.util.rots.stuff.RiseMovementFix;
import openraven.ravenxd.util.rots.RotationUtil;
import openraven.ravenxd.util.rots.compat.Vector3D;
import openraven.ravenxd.util.rots.stuff.MiauMinusRotationUtil;
import openraven.ravenxd.util.rots.stuff.RiseRotationUtil;
import openraven.ravenxd.util.rots.stuff.YuriRotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
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

    public static float[] aim(float[] target, double speed) {
        Rots.speed = speed;
        return target;
    }

    public static float[] last() {
        return new float[]{openraven.ravenxd.util.RotationManager.lastYaw, openraven.ravenxd.util.RotationManager.lastPitch};
    }

    public static float[] toEntity(Entity entity) {
        return arr(RiseRotationUtil.calculate(entity));
    }

    public static float[] toEntity(Entity entity, boolean adaptive, double range) {
        return arr(RiseRotationUtil.calculate(entity, adaptive, range));
    }

    public static float[] toEntityMultipoint(Entity entity, double horizontal, double vertical, double range) {
        return MiauMinusRotationUtil.getRotationsWithBackup(entity, horizontal, vertical, openraven.ravenxd.util.RotationManager.lastYaw, openraven.ravenxd.util.RotationManager.lastPitch, range);
    }

    public static float[] toHitVec(EntityLivingBase entity, YuriRotationUtils.HitVecMode mode, double randomization) {
        return arr(YuriRotationUtils.getHitVecRotation(entity, mode, randomization));
    }

    public static float[] toTrajectory(Entity target, boolean predict, float predictSize, float gravity, float velocity) {
        return YuriRotationUtils.faceTrajectory(target, predict, predictSize, gravity, velocity);
    }

    public static float[] toBox(AxisAlignedBB box, float maxAngle, float smoothFactor) {
        return RotationUtil.getRotationsToBox(box, openraven.ravenxd.util.RotationManager.lastYaw, openraven.ravenxd.util.RotationManager.lastPitch, maxAngle, smoothFactor);
    }

    public static float[] toPoint(Vec3 point) {
        return arr(RiseRotationUtil.calculate(point));
    }

    public static float[] toBlock(BlockPos pos) {
        return arr(RiseRotationUtil.calculate(pos));
    }

    public static float[] toBlock(BlockPos pos, EnumFacing facing) {
        return arr(RiseRotationUtil.calculate(new Vector3D(pos.getX(), pos.getY(), pos.getZ()), facing));
    }

    public static float[] smooth(float[] last, float[] target, double speed) {
        return arr(RiseRotationUtil.smooth(vec(last), vec(target), speed));
    }

    public static float[] step(float[] last, float[] target, double speed) {
        return arr(RiseRotationUtil.move(vec(last), vec(target), speed));
    }

    public static float[] fixSensitivity(float[] rotation, float[] previous) {
        return arr(RiseRotationUtil.applySensitivityPatch(vec(rotation), vec(previous)));
    }

    public static float[] noise(float[] rotation, YuriRotationUtils.NoiseMode mode, double frequency, long seedOffset) {
        return arr(YuriRotationUtils.applyNoise(vec(rotation), mode, frequency, seedOffset));
    }

    private static Vector2f vec(float[] rotation) {
        return new Vector2f(rotation[0], rotation[1]);
    }

    private static float[] arr(Vector2f rotation) {
        return rotation == null ? null : new float[]{rotation.x, rotation.y};
    }
}
