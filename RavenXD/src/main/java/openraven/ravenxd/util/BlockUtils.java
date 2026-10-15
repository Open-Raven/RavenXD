package xyz.daikirai.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

import java.util.List;
import java.util.Set;

public final class BlockUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private BlockUtils() {
    }

    public static float breakSpeed(Block block, ItemStack stack, boolean ignoreSlowdowns) {
        if (block == null) {
            return 0.0f;
        }
        float hardness = block.getBlockHardness(mc.theWorld, mc.thePlayer.getPosition());
        if (hardness < 0.0f) {
            return 0.0f;
        }
        boolean harvest = block.getMaterial().isToolNotRequired() || stack != null && stack.canHarvestBlock(block);
        if (harvest) {
            return digSpeed(stack, block, ignoreSlowdowns) / hardness / 30.0f;
        }
        return digSpeed(stack, block, ignoreSlowdowns) / hardness / 100.0f;
    }

    public static float digSpeed(ItemStack stack, Block block, boolean ignoreSlowdowns) {
        int n;
        float f = stack == null ? 1.0f : stack.getItem().getStrVsBlock(stack, block);
        if (f > 1.0f && (n = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack)) > 0) {
            f += (float) (n * n + 1);
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            f *= 1.0f + (float) (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2f;
        }
        if (!ignoreSlowdowns) {
            if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
                float f3;
                switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                    case 0:
                        f3 = 0.3f;
                        break;
                    case 1:
                        f3 = 0.09f;
                        break;
                    case 2:
                        f3 = 0.0027f;
                        break;
                    default:
                        f3 = 8.1E-4f;
                }
                f *= f3;
            }
            if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
                f /= 5.0f;
            }
            if (!mc.thePlayer.onGround) {
                f /= 5.0f;
            }
        }
        return f;
    }

    public static net.minecraft.util.BlockPos offset(int n, net.minecraft.util.BlockPos pos) {
        if (n == 0) {
            return pos.up();
        }
        if (n == 1) {
            return pos.down();
        }
        if (n == 2) {
            return pos.north();
        }
        if (n == 3) {
            return pos.south();
        }
        if (n == 4) {
            return pos.east();
        }
        return pos.west();
    }

    public static boolean exposed(net.minecraft.util.BlockPos pos) {
        boolean bed = pos != null && mc.theWorld.getBlockState(pos).getBlock() == net.minecraft.init.Blocks.bed;
        for (int i = 0; i < 6; ++i) {
            Block b = mc.theWorld.getBlockState(offset(i, pos)).getBlock();
            if (b == net.minecraft.init.Blocks.air || b instanceof net.minecraft.block.BlockLiquid || b == net.minecraft.init.Blocks.fire
                    || b == net.minecraft.init.Blocks.ladder || b == net.minecraft.init.Blocks.iron_bars || b == net.minecraft.init.Blocks.ender_chest
                    || b == net.minecraft.init.Blocks.enchanting_table || b == net.minecraft.init.Blocks.skull || b == net.minecraft.init.Blocks.anvil
                    || b == net.minecraft.init.Blocks.tripwire || b == net.minecraft.init.Blocks.vine || b == net.minecraft.init.Blocks.flower_pot
                    || b instanceof net.minecraft.block.BlockChest || b instanceof net.minecraft.block.BlockDoor || b instanceof net.minecraft.block.BlockTrapDoor
                    || b instanceof net.minecraft.block.BlockFence || b instanceof net.minecraft.block.BlockFenceGate || b instanceof net.minecraft.block.BlockWall
                    || b instanceof net.minecraft.block.BlockStairs) {
                return true;
            }
            if (i != 1 && b instanceof net.minecraft.block.BlockSlab) {
                return true;
            }
            if (i < 2 || (bed || b != net.minecraft.init.Blocks.bed) && b != net.minecraft.init.Blocks.end_portal_frame) continue;
            return true;
        }
        return false;
    }

    public static ItemStack itemOf(net.minecraft.block.state.IBlockState state) {
        Block block = state.getBlock();
        if (block == net.minecraft.init.Blocks.air) {
            return null;
        }
        net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
        if (item == null) {
            return null;
        }
        int n = block.damageDropped(state);
        return new ItemStack(item, 1, n);
    }
    private static final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    public static IBlockState getState(BlockPos pos) {
        return mc.theWorld.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).getBlock();
    }

    public static BlockPos pos(double x, double y, double z) {
        return new BlockPos(x, y, z);
    }

    public static Block getBlock(double x, double y, double z) {
        return getBlock(pos(x, y, z));
    }

    public static boolean is(BlockPos pos, Block block) {
        return pos != null && getBlock(pos) == block;
    }

    public static boolean opensOnClick(BlockPos pos) {
        return !mc.thePlayer.isSneaking() && isContainer(getBlock(pos));
    }

    public static BlockPos center(BlockPos pos) {
        return pos.add(0.5, 0.5, 0.5);
    }

    public static BlockPos fastestAround(BlockPos[] positions, double range) {
        BlockPos best = null;
        float f = -1.0f;
        for (BlockPos p : positions) {
            for (int i = 0; i < 6; ++i) {
                if (i == 1) continue;
                BlockPos side = offset(i, p);
                Block block = getBlock(side);
                if (block == Blocks.bed || !PlayerUtils.canReach(side, range)) continue;
                int n = ItemUtils.bestToolSlot(block);
                float f2 = breakSpeed(block, mc.thePlayer.inventory.getStackInSlot(n == -1 ? PlayerUtils.currentSlot() : n), true);
                if (f2 > f) {
                    f = f2;
                    best = side;
                    continue;
                }
                if (f2 != f) continue;
                best = PlayerUtils.closer(best, side);
            }
        }
        return best;
    }

    public static BlockPos below(double d) {
        return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - d, mc.thePlayer.posZ);
    }

    public static boolean isReplaceable(BlockPos pos) {
        Block block = getBlock(pos);
        return block == Blocks.air || block == Blocks.fire || block instanceof BlockLiquid || block.getMaterial().isReplaceable();
    }

    public static Block singleSlab(Block block) {
        if (block == Blocks.double_stone_slab) {
            return Blocks.stone_slab;
        }
        if (block == Blocks.double_stone_slab2) {
            return Blocks.stone_slab2;
        }
        return Blocks.wooden_slab;
    }

    public static boolean isSolid(int x, int y, int z) {
        return getState(new BlockPos(x, y, z)).getBlock().getMaterial().isSolid();
    }

    public static boolean isVisible(BlockPos pos) {
        Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        for (double d = 0.0; d <= 0.5; d += 0.5) {
            double y = (double) pos.getY() + d;
            if (sees(pos, eyes, new Vec3((double) (pos.getX() + 1), y, (double) pos.getZ() + 0.5))) {
                return true;
            }
            if (sees(pos, eyes, new Vec3((double) pos.getX(), y, (double) pos.getZ() + 0.5))) {
                return true;
            }
            if (sees(pos, eyes, new Vec3((double) pos.getX() + 0.5, y, (double) (pos.getZ() + 1)))) {
                return true;
            }
            if (sees(pos, eyes, new Vec3((double) pos.getX() + 0.5, y, (double) pos.getZ()))) {
                return true;
            }
        }
        return false;
    }

    public static boolean sees(BlockPos pos, Vec3 from, Vec3 to) {
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(from, to, false, false, false);
        if (hit == null) {
            return true;
        }
        BlockPos p;
        return hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && (p = hit.getBlockPos()).getX() == pos.getX() && p.getY() == pos.getY() && p.getZ() == pos.getZ();
    }

    public static boolean isPlaceable(Block block) {
        return block == Blocks.glass || block.isFullBlock() && block != Blocks.gravel && block != Blocks.sand && block != Blocks.soul_sand && block != Blocks.tnt && block != Blocks.crafting_table && block != Blocks.furnace && block != Blocks.dispenser && block != Blocks.dropper && block != Blocks.noteblock && block != Blocks.command_block;
    }

    public static boolean isBlockItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getItem() instanceof ItemBlock;
    }

    public static boolean isContainer(Block block) {
        return block instanceof BlockContainer || block == Blocks.crafting_table || block == Blocks.anvil;
    }

    public static boolean isInteractable(Block block) {
        if (isContainer(block) || block == Blocks.bed || block == Blocks.lever) {
            return true;
        }
        if (block instanceof BlockTrapDoor || block instanceof BlockFence || block instanceof BlockFenceGate) {
            return true;
        }
        if (!(block instanceof BlockDoor)) {
            return false;
        }
        return block != Blocks.iron_door;
    }

    public static Block blockOf(ItemStack itemStack) {
        return isBlockItem(itemStack) ? ((ItemBlock) itemStack.getItem()).getBlock() : null;
    }

    public static EnumDyeColor color(IBlockState state) {
        return (EnumDyeColor) state.getProperties().get(BlockColored.COLOR);
    }

    public static boolean nextToBed(BlockPos pos) {
        for (int i = 0; i < 6; ++i) {
            if (!is(offset(i, pos), Blocks.bed)) continue;
            return true;
        }
        return false;
    }

    public static boolean isOpenStair(BlockPos pos) {
        IBlockState state = getState(pos);
        if (state.getBlock() instanceof BlockStairs) {
            if (state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP) {
                return false;
            }
            for (int i = -1; i <= 1; i += 2) {
                if (getBlock(pos.add(i, 0, 0)) == Blocks.air) {
                    return true;
                }
                if (getBlock(pos.add(0, 0, i)) != Blocks.air) continue;
                return true;
            }
        }
        return false;
    }

    public static BlockPos placedPos(MovingObjectPosition hit) {
        return hit.getBlockPos().offset(hit.sideHit);
    }

    public static IBlockState placedState(MovingObjectPosition hit) {
        return getState(placedPos(hit));
    }

    public static boolean canPlace(MovingObjectPosition hit, ItemStack itemStack) {
        return ((ItemBlock) itemStack.getItem()).canPlaceBlockOnSide(mc.theWorld, hit.getBlockPos(), hit.sideHit, (EntityPlayer) mc.thePlayer, itemStack);
    }

    public static BlockPos otherBedHalf(BlockPos pos) {
        IBlockState state = getState(pos);
        EnumFacing facing = state.getValue(BlockBed.FACING);
        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
            facing = facing.getOpposite();
        }
        return pos.offset(facing);
    }

    public static float breakTicks(Block block, ItemStack itemStack, boolean ignoreSlowdowns) {
        float f = breakSpeed(block, itemStack, ignoreSlowdowns);
        if (f <= 0.0f) {
            return 0.0f;
        }
        return 1.0f / f;
    }

    public static boolean isPassable(BlockPos pos) {
        if (isReplaceable(pos)) {
            return true;
        }
        Block block = getBlock(pos);
        if (!block.isFullBlock()) {
            return block instanceof BlockSlab || block == Blocks.snow_layer;
        }
        return false;
    }

    public static boolean blocksNearby(int n) {
        for (int i = -n; i <= n; ++i) {
            for (int j = -n; j <= n; ++j) {
                for (int k = -n; k <= n; ++k) {
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + (double) i, mc.thePlayer.posY + (double) j, mc.thePlayer.posZ + (double) k);
                    if (getBlock(pos) instanceof BlockAir) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public static String name(Block block) {
        return block == null ? "null" : block.getRegistryName().replaceFirst("minecraft:", "");
    }

    public static boolean samePos(BlockPos a, BlockPos b) {
        return a == b || a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    public static int horizontalDistance(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    public static boolean bedAdjacent(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return getBlock(scratch.set(x + 1, y, z)) == Blocks.bed || getBlock(scratch.set(x - 1, y, z)) == Blocks.bed || getBlock(scratch.set(x, y, z + 1)) == Blocks.bed || getBlock(scratch.set(x, y, z - 1)) == Blocks.bed || getBlock(scratch.set(x, y - 1, z)) == Blocks.bed;
    }

    public static boolean hasSupport(BlockPos pos) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (!isSupport(getBlock(pos.offset(facing.getOpposite())))) continue;
            return true;
        }
        return false;
    }

    public static void touching(List<BlockPos> out, Set<BlockPos> solids, Set<BlockPos> candidates, BlockPos center) {
        block0:
        for (BlockPos p : candidates) {
            if (horizontalDistance(p, center) > 3) continue;
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (!solids.contains(p.offset(facing))) continue;
                out.add(p);
                continue block0;
            }
        }
    }

    public static void scan(Set<BlockPos> solids, Set<BlockPos> open, Set<BlockPos> exclude, BlockPos center, int r, int minY, int maxY) {
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        for (int i = -r; i <= r; ++i) {
            int ai = Math.abs(i);
            for (int j = -r; j <= r; ++j) {
                if (Math.max(ai, Math.abs(j)) > r) continue;
                for (int k = minY - 1; k <= maxY + 1; ++k) {
                    BlockPos p;
                    scratch.set(x + i, y + k, z + j);
                    if (isPlayerBlock(scratch)) continue;
                    if (isSupport(getBlock(scratch))) {
                        solids.add(new BlockPos((Vec3i) scratch));
                        continue;
                    }
                    if (k < minY || k > maxY || !isPassable(scratch) || exclude.contains(p = new BlockPos((Vec3i) scratch))) continue;
                    open.add(p);
                }
            }
        }
    }

    public static boolean isSupport(Block block) {
        if (block == null || block == Blocks.air) {
            return false;
        }
        if (block instanceof BlockLiquid) {
            return false;
        }
        if (block.getMaterial().isReplaceable()) {
            return false;
        }
        return !isInteractable(block);
    }

    public static BlockPos playerPos() {
        return new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ));
    }

    public static boolean isPlayerBlock(BlockPos pos) {
        BlockPos p = playerPos();
        return pos.equals(p) || pos.equals(p.up());
    }

    public static int nearestDistance(BlockPos pos, Set<BlockPos> set) {
        int n = Integer.MAX_VALUE;
        for (BlockPos p : set) {
            int dx = Math.abs(pos.getX() - p.getX());
            int dz = Math.abs(pos.getZ() - p.getZ());
            int dy = Math.abs(pos.getY() - p.getY());
            int d = Math.max(dx, dz) + dy * 2;
            if (d >= n) continue;
            n = d;
        }
        return n == Integer.MAX_VALUE ? 0 : n;
    }

    public static Vec3 hitVec(BlockPos pos, net.minecraft.util.EnumFacing facing) {
        double lo = 0.1, hi = 0.9, ylo = 0.05, yhi = 0.95, top = 1.0;
        double x = (double) pos.getX() + MathUtils.randomBetween(lo, hi);
        double y = (double) pos.getY() + MathUtils.randomBetween(ylo, yhi);
        double z = (double) pos.getZ() + MathUtils.randomBetween(lo, hi);
        IBlockState state = getState(pos);
        Block block = state.getBlock();
        if (!block.isFullBlock() && block instanceof net.minecraft.block.BlockSlab) {
            if (state.getValue(net.minecraft.block.BlockSlab.HALF) == net.minecraft.block.BlockSlab.EnumBlockHalf.TOP) {
                ylo = 0.505;
            } else if (facing == net.minecraft.util.EnumFacing.UP) {
                top = 0.5;
            } else {
                yhi = 0.495;
            }
            y = (double) pos.getY() + MathUtils.randomBetween(ylo, yhi);
        }
        switch (facing) {
            case EAST: x = pos.getX() + 1; break;
            case WEST: x = pos.getX(); break;
            case NORTH: z = pos.getZ(); break;
            case SOUTH: z = pos.getZ() + 1; break;
            case UP: y = (double) pos.getY() + top; break;
            case DOWN: y = pos.getY(); break;
        }
        return new Vec3(x, y, z);
    }
}
