package xyz.daikirai.module.player;

import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class AutoLadderModule extends Module {
    public static AutoLadderModule instance;
    public static SliderSetting minimumFallDistance;
    public static ButtonSetting onlyWhileHoldingLadder;
    private static int previousSlot = -1;
    public static BlockPos target;
    private static EnumFacing direction;
    private static EnumFacing face;
    private static boolean ladderNext;
    private static long placedAt;

    public AutoLadderModule(int key) {
        super("AutoLadder", Category.player, key);
        instance = this;
    }

    public void link() {
        minimumFallDistance = (SliderSetting) ModuleManager.setting(this, "Minimum fall distance");
        onlyWhileHoldingLadder = (ButtonSetting) ModuleManager.setting(this, "Only while holding ladder");
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused() || mc.thePlayer.capabilities.isFlying || mc.thePlayer.capabilities.isCreativeMode || AutoBlockinModule.active
                || ScaffoldModule.active) {
            reset();
            return;
        }
        if (mc.thePlayer.onGround || mc.thePlayer.isOnLadder() || (double) mc.thePlayer.fallDistance < 1.5) {
            reset();
            return;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - placedAt) < 1000L) {
            reset();
            return;
        }
        int most = 0;
        int blockSlot = -1;
        int ladderSlot = -1;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (ItemUtils.isScaffoldBlock(s)) {
                int n = s.stackSize;
                if (n <= most) continue;
                blockSlot = i;
                most = n;
                continue;
            }
            if (s == null || !(s.getItem() instanceof ItemBlock) || ((ItemBlock) s.getItem()).getBlock() != Blocks.ladder) continue;
            ladderSlot = i;
        }
        if (blockSlot == -1 || ladderSlot == -1) {
            reset();
            return;
        }
        if (onlyWhileHoldingLadder.enabled && target == null && mc.thePlayer.inventory.currentItem != ladderSlot) {
            reset();
            return;
        }
        MovingObjectPosition mop = PlayerUtils.rayTraceBlocks(7.0, mc.thePlayer.rotationYaw, mc.thePlayer.isSprinting() ? 80.0f : 90.0f);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mop.sideHit != EnumFacing.UP) {
            reset();
            return;
        }
        int slot;
        if (ladderNext) {
            ladderNext = false;
            slot = ladderSlot;
            target = target.up();
            face = direction.getOpposite();
            if (!BlockUtils.isReplaceable(target.offset(face))) {
                reset();
                return;
            }
        } else {
            previousSlot = mc.thePlayer.inventory.currentItem;
            slot = blockSlot;
            BlockPos ground = mop.getBlockPos();
            if (!BlockUtils.isReplaceable(ground.up())) {
                reset();
                return;
            }
            EnumFacing facing = mc.thePlayer.getHorizontalFacing();
            EnumFacing[] order = new EnumFacing[]{facing.rotateY(), facing.rotateYCCW(), facing, facing.getOpposite()};
            for (EnumFacing f : order) {
                BlockPos wall = ground.offset(f).up();
                if (BlockUtils.isReplaceable(wall) || !mc.theWorld.getBlockState(wall).getBlock().isFullBlock()) continue;
                slot = ladderSlot;
                target = wall;
                direction = f;
                face = direction.getOpposite();
                break;
            }
            if (target == null) {
                double x = mc.thePlayer.posX;
                double z = mc.thePlayer.posZ;
                double m = 0.415;
                double m1 = m + 1.0;
                for (EnumFacing f : order) {
                    BlockPos beside = ground.offset(f);
                    BlockPos above = beside.up();
                    if (x > (double) above.getX() - m && x < (double) above.getX() + m1 && z > (double) above.getZ() - m && z < (double) above.getZ() + m1) continue;
                    if (BlockUtils.isReplaceable(beside) || !BlockUtils.isReplaceable(above)) continue;
                    target = beside;
                    direction = f;
                    face = EnumFacing.UP;
                    break;
                }
            }
        }
        if (target == null) {
            reset();
            return;
        }
        if ((double) mc.thePlayer.fallDistance < minFall()) {
            return;
        }
        mop = PlayerUtils.rayTraceBlocks(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, 90.0f);
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            mc.thePlayer.inventory.currentItem = slot;
            place(new MovingObjectPosition(MovingObjectPosition.MovingObjectType.BLOCK, BlockUtils.hitVec(target, face), face, target));
            if (slot == blockSlot) {
                ladderNext = true;
            } else {
                placedAt = now;
            }
        }
    }

    @Override
    public void onDisable() {
        reset();
    }

    public static void reset() {
        if (previousSlot != -1) {
            Minecraft.getMinecraft().thePlayer.inventory.currentItem = previousSlot;
            previousSlot = -1;
        }
        ladderNext = false;
        target = null;
        direction = null;
        face = null;
        placedAt = 0L;
    }

    private static double minFall() {
        PotionEffect e = Minecraft.getMinecraft().thePlayer.getActivePotionEffect(Potion.jump);
        return minimumFallDistance.getInput() - 1.5 + (double) (e == null ? 0 : e.getAmplifier() + 1);
    }

    @Override
    public float[] getRotations(float lastYaw, float lastPitch) {
        if (target != null) {
            return RotationUtils.toBlockFixed(target, face, lastYaw, lastPitch);
        }
        return null;
    }

    private static void place(MovingObjectPosition m) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), m.getBlockPos(), m.sideHit, m.hitVec)) {
            PlayerUtils.swing(false, true);
        }
    }
}
