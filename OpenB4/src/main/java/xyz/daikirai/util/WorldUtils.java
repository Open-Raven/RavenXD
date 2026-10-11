package xyz.daikirai.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class WorldUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    private WorldUtils() {
    }

    public static IBlockState getState(BlockPos pos) {
        return mc.theWorld.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).getBlock();
    }

    public static boolean clearLine(Vec3 from, Vec3 to) {
        MovingObjectPosition hit = rayTrace(from, to);
        return hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public static MovingObjectPosition rayTrace(Vec3 from, Vec3 to) {
        MovingObjectPosition hit;
        int x = MathHelper.floor_double(from.xCoord);
        int y = MathHelper.floor_double(from.yCoord);
        int z = MathHelper.floor_double(from.zCoord);
        scratch.set(x, y, z);
        IBlockState state = mc.theWorld.getBlockState(scratch);
        Block block = state.getBlock();
        if (block.canCollideCheck(state, false) && (hit = block.collisionRayTrace(mc.theWorld, scratch, from, to)) != null) {
            return hit;
        }
        int ex = MathHelper.floor_double(to.xCoord);
        int ey = MathHelper.floor_double(to.yCoord);
        int ez = MathHelper.floor_double(to.zCoord);
        double d = from.xCoord;
        double d2 = from.yCoord;
        double d3 = from.zCoord;
        double d4 = to.xCoord - d;
        double d5 = to.yCoord - d2;
        double d6 = to.zCoord - d3;
        int n7 = 200;
        while (n7-- >= 0) {
            MovingObjectPosition hit2;
            EnumFacing facing;
            if (x == ex && y == ey && z == ez) {
                return null;
            }
            boolean bx = true;
            boolean by = true;
            boolean bz = true;
            double d7 = 999.0;
            double d8 = 999.0;
            double d9 = 999.0;
            if (ex > x) {
                d7 = x + 1;
            } else if (ex < x) {
                d7 = x;
            } else {
                bx = false;
            }
            if (ey > y) {
                d8 = y + 1;
            } else if (ey < y) {
                d8 = y;
            } else {
                by = false;
            }
            if (ez > z) {
                d9 = z + 1;
            } else if (ez < z) {
                d9 = z;
            } else {
                bz = false;
            }
            double d10 = 999.0;
            double d11 = 999.0;
            double d12 = 999.0;
            if (bx) {
                d10 = (d7 - d) / d4;
            }
            if (by) {
                d11 = (d8 - d2) / d5;
            }
            if (bz) {
                d12 = (d9 - d3) / d6;
            }
            if (d10 == -0.0) {
                d10 = -1.0E-4;
            }
            if (d11 == -0.0) {
                d11 = -1.0E-4;
            }
            if (d12 == -0.0) {
                d12 = -1.0E-4;
            }
            if (d10 < d11 && d10 < d12) {
                facing = ex > x ? EnumFacing.WEST : EnumFacing.EAST;
                d = d7;
                d2 += d5 * d10;
                d3 += d6 * d10;
            } else if (d11 < d12) {
                facing = ey > y ? EnumFacing.DOWN : EnumFacing.UP;
                d += d4 * d11;
                d2 = d8;
                d3 += d6 * d11;
            } else {
                facing = ez > z ? EnumFacing.NORTH : EnumFacing.SOUTH;
                d += d4 * d12;
                d2 += d5 * d12;
                d3 = d9;
            }
            x = MathHelper.floor_double(d) - (facing == EnumFacing.EAST ? 1 : 0);
            y = MathHelper.floor_double(d2) - (facing == EnumFacing.UP ? 1 : 0);
            z = MathHelper.floor_double(d3) - (facing == EnumFacing.SOUTH ? 1 : 0);
            scratch.set(x, y, z);
            IBlockState state2 = mc.theWorld.getBlockState(scratch);
            Block block2 = state2.getBlock();
            if (!block2.canCollideCheck(state2, false) || (hit2 = block2.collisionRayTrace(mc.theWorld, scratch, new Vec3(d, d2, d3), to)) == null) continue;
            return hit2;
        }
        return null;
    }

    public static MovingObjectPosition rayTraceBlocks(double range, float yaw, float pitch) {
        Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        float f3 = MathHelper.cos((yaw *= -((float) Math.PI / 180)) - (float) Math.PI);
        float f4 = MathHelper.sin(yaw - (float) Math.PI);
        float f5 = -MathHelper.cos(pitch *= -((float) Math.PI / 180));
        float f6 = MathHelper.sin(pitch);
        Vec3 dir = new Vec3(f4 * f5, f6, f3 * f5);
        Vec3 end = eyes.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        return mc.theWorld.rayTraceBlocks(eyes, end, false, false, false);
    }

    public static int dropSide(Entity entity, int previous) {
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            return -1;
        }
        double px = -(dz /= len);
        double pz = dx /= len;
        double y = entity.posY;
        int minusSide = 0;
        int plusSide = 0;
        int behind = 0;
        for (int n4 : new int[]{1, 2, 3, 4, 6, 8}) {
            int weight = n4 < 6 ? 2 : 1;
            double bx = entity.posX + dx * (double) n4;
            double bz = entity.posZ + dz * (double) n4;
            for (int i = -1; i <= 1; ++i) {
                double x = bx + px * (double) i;
                double z = bz + pz * (double) i;
                if (mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) != null) continue;
                behind += weight;
            }
            double mx = entity.posX - px * (double) n4;
            double mz = entity.posZ - pz * (double) n4;
            for (int i = -1; i <= 1; ++i) {
                double x = mx + dx * (double) i;
                double z = mz + dz * (double) i;
                if (mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) != null) continue;
                minusSide += weight;
            }
            double qx = entity.posX + px * (double) n4;
            double qz = entity.posZ + pz * (double) n4;
            for (int i = -1; i <= 1; ++i) {
                double x = qx + dx * (double) i;
                double z = qz + dz * (double) i;
                if (mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) != null) continue;
                plusSide += weight;
            }
        }
        if (minusSide == 0 && plusSide == 0) {
            return -1;
        }
        if (behind >= minusSide && behind >= plusSide) {
            return -1;
        }
        if (minusSide == plusSide && previous != -1) {
            return previous;
        }
        return minusSide > plusSide ? 0 : 1;
    }

    public static double distanceSq(double x, double y, double z, Vec3 p) {
        double d4 = x - p.xCoord;
        double d5 = y - p.yCoord;
        double d6 = z - p.zCoord;
        return d4 * d4 + d5 * d5 + d6 * d6;
    }

    public static double behind(double x, double z, double dirX, double dirZ, Vec3 p) {
        double d5 = p.xCoord - x;
        double d6 = p.zCoord - z;
        return -(d5 * dirX + d6 * dirZ);
    }
}
