package openraven.ravenxd.util.rots.raycast;

import openraven.ravenxd.util.IMinecraft;
import openraven.ravenxd.util.rots.compat.Compat;import openraven.ravenxd.util.rots.MoveUtil;
import openraven.ravenxd.util.rots.stuff.MiauMinusRotationUtil;
import com.google.common.base.Predicates;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class MMRaycastUtil implements IMinecraft {

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range) {
        return rayCast(yaw, pitch, range, 0);
    }

    public static MovingObjectPosition rayCast(float yaw, float pitch, double range, float expand) {
        return rayCast(yaw, pitch, range, expand, mc.getRenderViewEntity());
    }

    public static MovingObjectPosition rayCast(
            float yaw, float pitch, double range, float expand, Entity entity) {
        final float partialTicks = Compat.timer().renderPartialTicks;
        MovingObjectPosition objectMouseOver;

        if (entity != null && mc.theWorld != null) {
            Vec3 eyePos = entity.getPositionEyes(partialTicks);
            Vec3 lookVec = getVectorForRotation(pitch, yaw);
            Vec3 targetPos =
                    eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);

            objectMouseOver = mc.theWorld.rayTraceBlocks(eyePos, targetPos, false, false, true);

            double d1 = range;
            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(eyePos);
            }

            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list =
                    mc.theWorld.getEntitiesInAABBexcluding(
                            entity,
                            entity
                                    .getEntityBoundingBox()
                                    .addCoord(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range)
                                    .expand(f, f, f),
                            Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize() + expand;
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition =
                        axisalignedbb.calculateIntercept(eyePos, targetPos);

                if (axisalignedbb.isVecInside(eyePos)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? eyePos : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    final double d3 = eyePos.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }

            return objectMouseOver;
        }

        return null;
    }

    /**
     * Performs a ray intercept check specifically against a target entity's bounding box, supporting
     * through-walls by ignoring block raycasting. This is used by KillAura for precise through-walls
     * targeting.
     *
     * @return MovingObjectPosition with entityHit set, or null if not intercepting
     */
    public static MovingObjectPosition getEntityIntercept(
            Entity target, float yaw, float pitch, double range) {
        final float partialTicks = Compat.timer().renderPartialTicks;
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null || mc.theWorld == null || target == null) return null;

        Vec3 eyePos = viewEntity.getPositionEyes(partialTicks);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);
        Vec3 targetPos =
                eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);

        float borderSize = target.getCollisionBorderSize();
        AxisAlignedBB bb = target.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        MovingObjectPosition mop = bb.calculateIntercept(eyePos, targetPos);

        if (mop != null || bb.isVecInside(eyePos)) {
            MovingObjectPosition result =
                    new MovingObjectPosition(target, mop != null ? mop.hitVec : eyePos);
            return result;
        }

        return null;
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    /**
     * Multi-point bounding box visibility check. Ported from Rise 6. For non-player entities: simple
     * raycast to center. For player entities: scans 27 points (3x3x3) on bounding box to check if any
     * part of the entity is visible.
     *
     * @return true if the entity is visible through raycast
     */
    public static boolean inView(final Entity entity) {
        final int renderDistance = 16 * mc.gameSettings.renderDistanceChunks;
        final float[] rotations = MiauMinusRotationUtil.calculate(entity);

        if (MoveUtil.wrappedDifference(mc.thePlayer.rotationYaw, rotations[0])
                > mc.gameSettings.fovSetting) {
            return false;
        }

        if (!(entity instanceof EntityPlayer)) {
            MovingObjectPosition mop =
                    MMRaycastUtil.rayCast(rotations[0], rotations[1], renderDistance, 0.2f);
            return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
        }

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double width = bb.maxX - bb.minX;
        double height = bb.maxY - bb.minY;
        double depth = bb.maxZ - bb.minZ;

        for (double yPercent = 1.0; yPercent >= -1.0; yPercent -= 0.5) {
            for (double xPercent = 1.0; xPercent >= -1.0; xPercent -= 1.0) {
                for (double zPercent = 1.0; zPercent >= -1.0; zPercent -= 1.0) {
                    Vec3 point =
                            new Vec3(
                                    bb.minX + width * xPercent,
                                    bb.minY + height * yPercent,
                                    bb.minZ + depth * zPercent);
                    float[] subRotations = MiauMinusRotationUtil.calculate(point);
                    MovingObjectPosition mop =
                            MMRaycastUtil.rayCast(subRotations[0], subRotations[1], renderDistance, 0.2f);
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}