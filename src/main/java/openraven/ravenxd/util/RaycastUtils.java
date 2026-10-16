package openraven.ravenxd.util;

import openraven.ravenxd.util.rots.raycast.RaycastUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

// wrapper around kayasaka raycasts
public final class RaycastUtils {
    private RaycastUtils() {
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range) {
        return RaycastUtil.rayCast(yaw, pitch, range);
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand) {
        return RaycastUtil.rayCast(yaw, pitch, range, expand);
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand, Entity from) {
        return RaycastUtil.rayCast(yaw, pitch, range, expand, from);
    }

    public static MovingObjectPosition rayCast(float[] rotation, double range) {
        return rayCast(rotation[0], rotation[1], range);
    }

    public static MovingObjectPosition rayCast(float[] rotation, double range, float expand) {
        return rayCast(rotation[0], rotation[1], range, expand);
    }

    public static MovingObjectPosition rayCastServer(double range) {
        return rayCast(RotationManager.lastYaw, RotationManager.lastPitch, range);
    }

    public static MovingObjectPosition entityIntercept(Entity target, float yaw, float pitch, double range) {
        return RaycastUtil.entityIntercept(target, yaw, pitch, range);
    }

    public static MovingObjectPosition entityIntercept(Entity target, float[] rotation, double range) {
        return entityIntercept(target, rotation[0], rotation[1], range);
    }

    public static boolean hitsEntity(float yaw, float pitch, double range, Entity target) {
        return RaycastUtil.hitsEntity(yaw, pitch, range, target);
    }

    public static boolean hitsEntity(float[] rotation, double range, Entity target) {
        return hitsEntity(rotation[0], rotation[1], range, target);
    }

    public static boolean hitsEntityServer(double range, Entity target) {
        return hitsEntity(RotationManager.lastYaw, RotationManager.lastPitch, range, target);
    }

    public static boolean inView(Entity entity) {
        return RaycastUtil.inView(entity);
    }

    public static boolean overBlock(float[] rotation, BlockPos pos, EnumFacing facing, boolean strict) {
        return RaycastUtil.overBlock(vec(rotation), facing, pos, strict);
    }

    public static boolean overBlock(float[] rotation, BlockPos pos) {
        return RaycastUtil.overBlock(vec(rotation), pos);
    }

    public static boolean crosshairOver(BlockPos pos, EnumFacing facing, boolean strict) {
        return RaycastUtil.overBlock(facing, pos, strict);
    }

    public static Vec3 lookVector(float yaw, float pitch) {
        return RaycastUtil.vectorForRotation(pitch, yaw);
    }

    private static Vector2f vec(float[] rotation) {
        return new Vector2f(rotation[0], rotation[1]);
    }
}
