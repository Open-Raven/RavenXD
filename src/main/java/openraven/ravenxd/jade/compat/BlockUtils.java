package openraven.ravenxd.jade.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public final class BlockUtils {
    private BlockUtils() {
    }

    public static boolean isReplaceableAt(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        return !ClientUtils.isInWorld() || mc.theWorld.getBlockState(pos).getBlock().isReplaceable(mc.theWorld, pos);
    }

    public static boolean canPlaceItemOnSide(ItemStack stack, BlockPos pos, EnumFacing side) {
        Minecraft mc = Minecraft.getMinecraft();
        return stack != null && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).canPlaceBlockOnSide(mc.theWorld, pos, side, mc.thePlayer, stack);
    }
}
