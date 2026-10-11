package xyz.daikirai.util;

import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public final class BlockBreaker {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean sending;
    public static C07PacketPlayerDigging.Action lastAction;

    private BlockBreaker() {
    }

    private static Module bedAura() {
        return ModuleManager.get("BedAura");
    }

    private static double range() {
        return ((SliderSetting) ModuleManager.setting(bedAura(), "Range")).getInput();
    }

    public static void swing() {
        PlayerUtils.swing(((ButtonSetting) ModuleManager.setting(bedAura(), "Silent swing")).isToggled(), false);
    }

    public static void abort(BlockPos pos) {
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), pos, -1);
        if (mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        sending = true;
        lastAction = C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK;
        PacketUtils.send(new C07PacketPlayerDigging(lastAction, pos, EnumFacing.DOWN));
        sending = false;
    }

    public static void stop(BlockPos pos) {
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), pos, -1);
        EnumFacing facing = EnumFacing.UP;
        float[] r = RotationUtils.toBlock(pos, null);
        MovingObjectPosition hit = PlayerUtils.rayTraceBlocks(range(), r[0], r[1]);
        if (hit != null) {
            facing = hit.sideHit;
        }
        sending = true;
        lastAction = C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK;
        PacketUtils.send(new C07PacketPlayerDigging(lastAction, pos, facing));
        sending = false;
        if (!((ButtonSetting) ModuleManager.setting(bedAura(), "Disable break effects")).isToggled()) {
            mc.theWorld.playAuxSFX(2001, pos, Block.getStateId(BlockUtils.getState(pos)));
        }
        swing();
    }

    public static void start(BlockPos pos) {
        if (!PacketUtils.sentSwing) {
            swing();
        }
        EnumFacing facing = EnumFacing.UP;
        float[] r = RotationUtils.toBlock(pos, null);
        MovingObjectPosition hit = PlayerUtils.rayTraceBlocks(range(), r[0], r[1]);
        if (hit != null) {
            facing = hit.sideHit;
        }
        sending = true;
        lastAction = C07PacketPlayerDigging.Action.START_DESTROY_BLOCK;
        PacketUtils.send(new C07PacketPlayerDigging(lastAction, pos, facing));
        sending = false;
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            swing();
        }
    }
}
