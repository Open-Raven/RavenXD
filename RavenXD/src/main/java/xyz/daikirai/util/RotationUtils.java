package xyz.daikirai.util;

import xyz.daikirai.module.SettingsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RotationUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float lastYawStep, lastPitchStep;

    private RotationUtils() {
    }

    public static final class AimPoint {
        public final float yaw;
        public final float pitch;
        public final Vec3 point;

        public AimPoint(float yaw, float pitch, Vec3 point) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.point = point;
        }
    }

    public enum Precision {
        Low(40.0f),
        Medium(20.0f),
        High(5.0f),
        Very_high(1.5f);

        public final float offsetYaw;

        Precision(float offsetYaw) {
            this.offsetYaw = offsetYaw;
        }

        public float getOffsetYaw() {
            return this.offsetYaw;
        }

        public float getOffsetPitch(float pitch) {
            if (pitch > 75.0f) {
                return 1.0f;
            }
            if (pitch > 60.0f) {
                return 2.0f;
            }
            return 5.0f;
        }
    }

    private enum Axis {
        X, Y, Z
    }

    public static double yawDifference(Entity entity, boolean server) {
        float f2 = yawTo(entity.posX, entity.posZ);
        float f = server && RotationManager.rotating ? RotationManager.serverYaw : mc.thePlayer.rotationYaw;
        return MathHelper.wrapAngleTo180_double((double) (f2 - f));
    }

    public static double absYawDifference(Entity entity, boolean server) {
        return Math.abs(yawDifference(entity, server));
    }

    public static float[] toEntityPredicted(Entity entity, int ticks) {
        double d;
        if (entity == null) {
            return null;
        }
        if (ticks == 0) {
            return toEntity(entity);
        }
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;
        double dx = x - entity.lastTickPosX;
        double dz = z - entity.lastTickPosZ;
        for (int i = 0; i < ticks; ++i) {
            x += dx;
            z += dz;
        }
        double d7 = x - mc.thePlayer.posX;
        if (entity instanceof EntityLivingBase) {
            d = y + (double) ((EntityLivingBase) entity).getEyeHeight() * 0.9 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        } else {
            d = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        }
        double d8 = z - mc.thePlayer.posZ;
        double d9 = Math.sqrt(d7 * d7 + d8 * d8);
        float f = (float) (Math.atan2(d8, d7) * (double) 57.29578f) - 90.0f;
        float f2 = (float) (-(Math.atan2(d, d9) * (double) 57.29578f));
        return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(f - mc.thePlayer.rotationYaw), clampPitch(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(f2 - mc.thePlayer.rotationPitch))};
    }

    public static float[] toBlockFixed(BlockPos pos, EnumFacing facing, float lastYaw, float lastPitch) {
        float[] r = toBlock(pos, facing);
        return fixSensitivity(r[0], r[1], lastYaw, lastPitch);
    }

    public static float clampPitch(float pitch) {
        return MathHelper.clamp_float(pitch, -90.0f, 90.0f);
    }

    public static float[] toEntityFixed(Entity entity, float lastYaw, float lastPitch) {
        float[] r = toEntityBox(entity);
        if (r == null) {
            return null;
        }
        return fixSensitivity(r[0], r[1], lastYaw, lastPitch);
    }

    public static void setRotation(float yaw, float pitch) {
        mc.thePlayer.rotationYaw = yaw;
        mc.thePlayer.rotationPitch = pitch;
    }

    public static void setRotation(float[] rotations) {
        if (rotations != null) {
            setRotation(rotations[0], rotations[1]);
        }
    }

    public static float[] playerRotation() {
        return new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
    }

    public static float[] currentRotation() {
        if (RotationManager.rotating && RotationManager.wasRotating) {
            return new float[]{RotationManager.serverYaw, RotationManager.serverPitch};
        }
        return playerRotation();
    }

    public static float[] toBlock(BlockPos pos) {
        return toBlock(pos, null);
    }

    public static float[] toBlock(BlockPos pos, EnumFacing facing) {
        return toBlock(pos.getX(), pos.getY(), pos.getZ(), facing);
    }

    public static float[] toBlock(double x, double y, double z, EnumFacing facing) {
        double d4 = x + 0.45 - mc.thePlayer.posX;
        double d5 = y + 0.45 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        double d6 = z + 0.45 - mc.thePlayer.posZ;
        if (facing != null) {
            d4 += (double) facing.getDirectionVec().getX() * 0.5;
            d5 += (double) facing.getDirectionVec().getY() * 0.5;
            d6 += (double) facing.getDirectionVec().getZ() * 0.5;
        }
        return fromDelta(d4, d5, d6);
    }

    public static float[] toPosition(double x, double y, double z) {
        return fromDelta(x - mc.thePlayer.posX, y - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight()), z - mc.thePlayer.posZ);
    }

    public static float[] toPositionFixed(double x, double y, double z, float lastYaw, float lastPitch) {
        float[] r = toPosition(x, y, z);
        return fixSensitivity(r[0], r[1], lastYaw, lastPitch);
    }

    public static float[] toVec(Vec3 vec3) {
        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight();
        double z = mc.thePlayer.posZ;
        if (vec3.xCoord == x && vec3.yCoord == y && vec3.zCoord == z) {
            return playerRotation();
        }
        return fromDelta(vec3.xCoord - x, vec3.yCoord - y, vec3.zCoord - z);
    }

    public static void faceEntityFixed(Entity entity) {
        if (entity == null) {
            return;
        }
        setRotation(toEntityFixed(entity, RotationManager.lastYaw, RotationManager.lastPitch));
    }

    public static void faceEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        setRotation(toEntity(entity));
    }

    public static float[] fixSensitivity(float yaw, float pitch, float lastYaw, float lastPitch, boolean jitter) {
        int n;
        float f5 = yaw - lastYaw;
        float f6 = Math.abs(f5);
        float f7 = pitch - lastPitch;
        float f8 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        double d = (double) (f8 * f8 * f8) * 1.2;
        float f9 = lastYawStep = (float) ((double) Math.round((double) f5 / d) * d);
        float f10 = lastPitchStep = (float) ((double) Math.round((double) f7 / d) * d);
        yaw = lastYaw + f9;
        pitch = lastPitch + f10;
        if (jitter && (double) f6 >= d && (n = (int) SettingsModule.jitterYaw.getInput()) > 0) {
            int n2 = MathUtils.randomInt(-n, n);
            yaw += (float) ((double) n2 * d);
        }
        return new float[]{yaw, clampPitch(pitch)};
    }

    public static float[] fixSensitivity(float yaw, float pitch, float lastYaw, float lastPitch) {
        return fixSensitivity(yaw, pitch, lastYaw, lastPitch, true);
    }

    public static float yawTo(double x, double z) {
        return yawBetween(mc.thePlayer.posX, mc.thePlayer.posZ, x, z);
    }

    public static float yawBetween(double fromX, double fromZ, double toX, double toZ) {
        double d7 = Math.atan2(toX - fromX, toZ - fromZ) * (double) 57.29578f;
        return (float) (-d7);
    }

    public static float[] toEntity(Entity entity) {
        return toEntityBox(entity);
    }

    public static float randomOffset() {
        return (float) (Math.random() - 0.5);
    }

    public static double inputAngle() {
        double d = Math.toDegrees(Math.atan2(PlayerUtils.strafeInput(), -PlayerUtils.forwardInput()));
        d = (d + 360.0) % 360.0;
        return d;
    }

    public static double inputAngleSigned() {
        double d = Math.toDegrees(Math.atan2(-PlayerUtils.strafeInput(), PlayerUtils.forwardInput()));
        return d == 0.0 ? 0.0 : d;
    }

    public static float[] fromDelta(double dx, double dy, double dz) {
        double d4 = Math.sqrt(dx * dx + dz * dz);
        float f = (float) (Math.atan2(dz, dx) * (double) 57.29578f) - 90.0f;
        float f2 = (float) (-(Math.atan2(dy, d4) * (double) 57.29578f));
        float[] current = currentRotation();
        f = current[0] + MathHelper.wrapAngleTo180_float(f - current[0]);
        f2 = clampPitch(current[1] + MathHelper.wrapAngleTo180_float(f2 - current[1]));
        return new float[]{f, f2};
    }

    public static Vec3 getVectorForRotation(float yaw, float pitch) {
        float f3 = MathHelper.cos((yaw *= -((float) Math.PI / 180)) - (float) Math.PI);
        float f4 = MathHelper.sin(yaw - (float) Math.PI);
        float f5 = -MathHelper.cos(pitch *= -((float) Math.PI / 180));
        float f6 = MathHelper.sin(pitch);
        return new Vec3(f4 * f5, f6, f3 * f5);
    }

    public static Vec3 lookVector() {
        return getVectorForRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    public static float[] toEntityBox(Entity entity) {
        if (entity == null) {
            return null;
        }
        return toBox(entity, entity.getEntityBoundingBox());
    }

    public static float[] toBox(Entity entity, AxisAlignedBB box) {
        Vec3 eyes;
        if (entity == null) {
            return null;
        }
        float f = entity.getCollisionBorderSize();
        if ((box = box.expand(f, f, f)).isVecInside(eyes = mc.thePlayer.getPositionEyes(1.0f))) {
            return new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        }
        Vec3 p = aimPoint(eyes, box);
        return fromDelta(p.xCoord - eyes.xCoord, p.yCoord - eyes.yCoord, p.zCoord - eyes.zCoord);
    }

    public static Vec3 aimPoint(Vec3 eyes, AxisAlignedBB box) {
        double d = SettingsModule.aimClosestPoint.getInput();
        double cx = (box.minX + box.maxX) / 2.0;
        double cy = box.minY + (box.maxY - box.minY) * 0.825;
        double cz = (box.minZ + box.maxZ) / 2.0;
        if (d <= 0.0) {
            return new Vec3(cx, cy, cz);
        }
        if (SettingsModule.aimClosestPoint.isAtMin()) {
            return new Vec3(cx, cy, cz);
        }
        double d5 = 0.03;
        double px = MathHelper.clamp_double(eyes.xCoord, box.minX + d5, box.maxX - d5);
        double py = MathHelper.clamp_double(eyes.yCoord, box.minY + d5, box.maxY - d5);
        double pz = MathHelper.clamp_double(eyes.zCoord, box.minZ + d5, box.maxZ - d5);
        if (d >= 100.0) {
            return new Vec3(px, py, pz);
        }
        double t = d / 100.0;
        return new Vec3(cx + (px - cx) * t, cy + (py - cy) * t, cz + (pz - cz) * t);
    }

    public static Vec3 visiblePoint(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        float f = entity.getCollisionBorderSize();
        AxisAlignedBB box = entity.getEntityBoundingBox().expand(f, f, f);
        if (box.isVecInside(eyes)) {
            return eyes;
        }
        Vec3 p = aimPoint(eyes, box);
        if (WorldUtils.clearLine(eyes, p)) {
            return p;
        }
        return fallbackPoint(entity, eyes);
    }

    public static Vec3 fallbackPoint(Entity entity, Vec3 eyes) {
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;
        double eye = entity.getEyeHeight();
        double h1 = eye - 0.2;
        double h2 = eye - 0.9;
        double h3 = eye - 1.5;
        double o = 0.28;
        Vec3[] points = {
                new Vec3(x, y + (eye + 0.2), z),
                new Vec3(x, y + (eye - 0.6), z),
                new Vec3(x, y + h2, z),
                new Vec3(x, y + (eye - 1.2), z),
                new Vec3(x, y + h3, z),
                new Vec3(x + o, y + h1, z + o),
                new Vec3(x + o, y + h1, z - o),
                new Vec3(x - o, y + h1, z + o),
                new Vec3(x - o, y + h1, z - o),
                new Vec3(x + o, y + h2, z + o),
                new Vec3(x + o, y + h2, z - o),
                new Vec3(x - o, y + h2, z + o),
                new Vec3(x - o, y + h2, z - o),
                new Vec3(x + o, y + h3, z + o),
                new Vec3(x + o, y + h3, z - o),
                new Vec3(x - o, y + h3, z + o),
                new Vec3(x - o, y + h3, z - o),
        };
        for (Vec3 p : points) {
            if (WorldUtils.clearLine(eyes, p)) {
                return p;
            }
        }
        return null;
    }

    public static boolean canSee(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double x = entity.posX;
        double y = entity.posY;
        double z = entity.posZ;
        double h = (double) entity.getEyeHeight() - 0.2;
        if (WorldUtils.clearLine(eyes, new Vec3(x + 0.3, y + h, z))) {
            return true;
        }
        if (WorldUtils.clearLine(eyes, new Vec3(x - 0.3, y + h, z))) {
            return true;
        }
        if (WorldUtils.clearLine(eyes, new Vec3(x, y + h, z + 0.3))) {
            return true;
        }
        if (WorldUtils.clearLine(eyes, new Vec3(x, y + h, z - 0.3))) {
            return true;
        }
        for (double d = (double) entity.getEyeHeight() + 0.2; d > 0.0; d -= 0.2) {
            if (!WorldUtils.clearLine(eyes, new Vec3(x, y + d, z))) continue;
            return true;
        }
        return false;
    }

    public static float[] searchBlockRotation(BlockPos pos, EnumFacing facing, float offset, Precision precision, float airYaw, boolean playerYaw) {
        float bestYaw = 0.0f;
        float bestPitch = 0.0f;
        float base = playerYaw ? mc.thePlayer.rotationYaw : (mc.thePlayer.onGround ? PlayerUtils.movementAngle() : airYaw);
        float from = base + offset;
        float to = base + 170.0f;
        float reach = mc.playerController.getBlockReachDistance();
        float best = 360.0f;
        for (float yaw = from; yaw <= to; yaw += precision.getOffsetYaw()) {
            float y = (float) ((double) yaw + (Math.random() - 0.5));
            for (float pitch = 90.0f; pitch > 30.0f; pitch -= precision.getOffsetPitch(pitch)) {
                float diff;
                BlockPos hitPos;
                float p = MathHelper.clamp_float((float) ((double) pitch + (Math.random() - 0.5)), -90.0f, 90.0f);
                MovingObjectPosition hit = WorldUtils.rayTraceBlocks(reach, y, p);
                if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || facing != null && hit.sideHit != facing || !BlockUtils.samePos(hitPos = hit.getBlockPos(), pos) || !awayFromEdge((float) (hit.hitVec.yCoord - (double) hitPos.getY())) || !awayFromEdge((float) (hit.hitVec.xCoord - (double) hitPos.getX())) || !awayFromEdge((float) (hit.hitVec.zCoord - (double) hitPos.getZ())) || !((diff = Math.abs(base - y)) < best)) continue;
                best = diff;
                bestYaw = y;
                bestPitch = p;
            }
        }
        if (best == 360.0f) {
            return null;
        }
        if (playerYaw && best > (float) 70) {
            return null;
        }
        return new float[]{bestYaw, bestPitch};
    }

    public static boolean awayFromEdge(float f) {
        return f == 0.0f || f == 1.0f || (double) f > 0.06 && (double) f < 0.94;
    }

    public static AimPoint aimAtFace(BlockPos pos, EnumFacing facing) {
        double reach = mc.playerController.getBlockReachDistance();
        for (Vec3 p : facePoints(pos, facing)) {
            float[] r = toPosition(p.xCoord, p.yCoord, p.zCoord);
            if (PlayerUtils.rayTraceBlock(pos, facing, reach, r[0], r[1]) == null) continue;
            return new AimPoint(r[0], r[1], p);
        }
        return null;
    }

    public static List<Vec3> facePoints(BlockPos pos, EnumFacing facing) {
        Axis a;
        Axis b;
        ArrayList<Vec3> list = new ArrayList<Vec3>((2 * 2 + 1) * (2 * 2 + 1) + 8);
        double x = (double) pos.getX() + 0.5 + 0.5 * (double) facing.getFrontOffsetX();
        double y = (double) pos.getY() + 0.5 + 0.5 * (double) facing.getFrontOffsetY();
        double z = (double) pos.getZ() + 0.5 + 0.5 * (double) facing.getFrontOffsetZ();
        if (Math.abs(facing.getFrontOffsetY()) == 1) {
            a = Axis.X;
            b = Axis.Z;
        } else if (Math.abs(facing.getFrontOffsetZ()) == 1) {
            a = Axis.X;
            b = Axis.Y;
        } else {
            a = Axis.Z;
            b = Axis.Y;
        }
        double step = 0.49 / (double) 2;
        list.add(jitter(x, y, z));
        for (int i = 1; i <= 2; ++i) {
            list.add(offset(x, y, z, a, (double) i * step, b, 0.0));
            list.add(offset(x, y, z, a, (double) (-i) * step, b, 0.0));
            list.add(offset(x, y, z, a, 0.0, b, (double) i * step));
            list.add(offset(x, y, z, a, 0.0, b, (double) (-i) * step));
            for (int j = -i; j <= i; ++j) {
                for (int k = -i; k <= i; ++k) {
                    int ak;
                    int aj = Math.abs(j);
                    if (Math.max(aj, ak = Math.abs(k)) != i || j == 0 && ak == i || k == 0 && aj == i) continue;
                    list.add(offset(x, y, z, a, (double) j * step, b, (double) k * step));
                }
            }
        }
        return list;
    }

    private static Vec3 jitter(double x, double y, double z) {
        double d6 = 0.04;
        double d7 = 0.01;
        double lo;
        double hi;
        if (x % 1.0 != 0.0) {
            lo = Math.floor(x += MathUtils.randomBetween(-d6, d6));
            hi = Math.ceil(x);
            x = Math.max(lo + d7, Math.min(hi - d7, x));
        }
        if (y % 1.0 != 0.0) {
            lo = Math.floor(y += MathUtils.randomBetween(-d6, d6));
            hi = Math.ceil(y);
            y = Math.max(lo + d7, Math.min(hi - d7, y));
        }
        if (z % 1.0 != 0.0) {
            lo = Math.floor(z += MathUtils.randomBetween(-d6, d6));
            hi = Math.ceil(z);
            z = Math.max(lo + d7, Math.min(hi - d7, z));
        }
        return new Vec3(x, y, z);
    }

    private static Vec3 offset(double x, double y, double z, Axis a, double da, Axis b, double db) {
        if (a == Axis.X) {
            x += da;
        } else if (a == Axis.Y) {
            y += da;
        } else {
            z += da;
        }
        if (b == Axis.X) {
            x += db;
        } else if (b == Axis.Y) {
            y += db;
        } else {
            z += db;
        }
        return jitter(x, y, z);
    }

    public static float yawToPoint(double x, double z) {
        double d5 = Math.atan2(x - mc.thePlayer.posX, z - mc.thePlayer.posZ) * (double) 57.29578f;
        return (float) (d5 * -1.0);
    }

    public static double yawOffset(Entity entity) {
        return ((mc.thePlayer.rotationYaw - yawToPoint(entity.posX, entity.posZ)) % 360.0f + 540.0f) % 360.0f - 180.0f;
    }

    public static float[] toEntityLow(Entity entity) {
        double d;
        if (entity == null) {
            return null;
        }
        double d2 = entity.posX - mc.thePlayer.posX;
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase e = (EntityLivingBase) entity;
            d = e.posY + (double) e.getEyeHeight() * 0.9 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        } else {
            d = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        }
        double d3 = entity.posZ - mc.thePlayer.posZ;
        double d4 = Math.sqrt(d2 * d2 + d3 * d3);
        float f = (float) (Math.atan2(d3, d2) * (double) 57.29578f) - 90.0f;
        float f2 = (float) (-(Math.atan2(d, d4) * (double) 57.29578f));
        return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(f - mc.thePlayer.rotationYaw), mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(f2 - mc.thePlayer.rotationPitch) + 4.0f};
    }

    public static float[] toBlockRaw(BlockPos pos) {
        double d = (double) pos.getX() + 0.45 - mc.thePlayer.posX;
        double d2 = (double) pos.getY() + 0.45 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
        double d3 = (double) pos.getZ() + 0.45 - mc.thePlayer.posZ;
        double d4 = Math.sqrt(d * d + d3 * d3);
        float f = (float) (Math.atan2(d3, d) * (double) 57.29578f) - 90.0f;
        float f2 = (float) (-(Math.atan2(d2, d4) * (double) 57.29578f));
        return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(f - mc.thePlayer.rotationYaw), mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(f2 - mc.thePlayer.rotationPitch)};
    }
}
