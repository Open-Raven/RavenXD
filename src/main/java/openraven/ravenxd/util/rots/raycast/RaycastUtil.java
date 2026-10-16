package openraven.ravenxd.util.rots.raycast;

import openraven.ravenxd.util.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

//we love wrappers!!!
public final class RaycastUtil implements IMinecraft {

    private RaycastUtil() {}

    public static MovingObjectPosition rayCast(final float yaw, final float pitch, final double range) {
        return MMRaycastUtil.rayCast(yaw, pitch, range);
    }

    public static MovingObjectPosition rayCast(final float yaw, final float pitch, final double range, final float expand) {
        return MMRaycastUtil.rayCast(yaw, pitch, range, expand);
    }

    public static MovingObjectPosition rayCast(final float yaw, final float pitch, final double range, final float expand, final Entity entity) {
        return MMRaycastUtil.rayCast(yaw, pitch, range, expand, entity);
    }

    public static MovingObjectPosition rayCast(final Vector2f rotation, final double range) {
        return MMRaycastUtil.rayCast(rotation.x, rotation.y, range);
    }

    public static MovingObjectPosition rayCast(final Vector2f rotation, final double range, final float expand) {
        return MMRaycastUtil.rayCast(rotation.x, rotation.y, range, expand);
    }

    public static MovingObjectPosition rayCast(final Vector2f rotation, final double range, final float expand, final Entity entity) {
        return MMRaycastUtil.rayCast(rotation.x, rotation.y, range, expand, entity);
    }

    // no blocks
    public static MovingObjectPosition entityIntercept(final Entity target, final float yaw, final float pitch, final double range) {
        return MMRaycastUtil.getEntityIntercept(target, yaw, pitch, range);
    }


    public static boolean hitsEntity(final float yaw, final float pitch, final double range, final Entity target) {
        final MovingObjectPosition mop = rayCast(yaw, pitch, range);
        return mop != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && (target == null || mop.entityHit == target);
    }

    public static boolean hitsEntity(final Vector2f rotation, final double range, final Entity target) {
        return hitsEntity(rotation.x, rotation.y, range, target);
    }

    public static boolean inView(final Entity entity) {
        return MMRaycastUtil.inView(entity);
    }

    public static Vec3 vectorForRotation(final float pitch, final float yaw) {
        return MMRaycastUtil.getVectorForRotation(pitch, yaw);
    }

    public static boolean overBlock(final Vector2f rotation, final EnumFacing facing, final BlockPos pos, final boolean strict) {
        return RiseRayCastUtil.overBlock(rotation, facing, pos, strict);
    }

    public static boolean overBlock(final EnumFacing facing, final BlockPos pos, final boolean strict) {
        return RiseRayCastUtil.overBlock(facing, pos, strict);
    }

    public static boolean overBlock(final Vector2f rotation, final BlockPos pos) {
        return RiseRayCastUtil.overBlock(rotation, pos);
    }

    public static boolean overBlock(final Vector2f rotation, final BlockPos pos, final EnumFacing facing) {
        return RiseRayCastUtil.overBlock(rotation, pos, facing);
    }
}
