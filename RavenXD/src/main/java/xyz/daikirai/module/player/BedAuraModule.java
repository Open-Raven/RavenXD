package xyz.daikirai.module.player;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.combat.KillAuraModule;
import xyz.daikirai.module.minigames.BedWarsModule;
import xyz.daikirai.module.render.BedESPModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockBreaker;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.HashMap;

public class BedAuraModule extends Module {
    public static BedAuraModule instance;
    public static SliderSetting breakModeSetting, breakSpeed, fov, rangeSetting, rate;
    public static ButtonSetting allowKillAura, breakNearBlock, cancelKnockback, disableBhop, disableBreakEffects, disableRightClick, groundSpoof,
            ignoreSlow, maximizeSpeed, onlyWhileVisible, prioritizeKillAura, renderBlockOutline, sendAnimations, silentSwing, skipBreakVerification;

    public enum Mode {
        Legit, Instant, Swap
    }

    public static Mode mode = Mode.Legit;
    public static BlockPos target;
    public static BlockPos nearBlock;
    private static BlockPos nearBed;
    private static BlockPos quickBlock;
    private static boolean targetIsBed;
    public static double range;
    private static int radius;
    private static double rateSeconds;
    private static long rateTicks;
    private static long rateCounter;
    public static float progress, progressPerTick;
    public static float vanillaProgress;
    public static int rotateTick = -1;
    private static long waitTicks;
    private static long counter;
    private static int quickTicks, quickTotal;
    private static int quickBreakTicks = 4;
    private static int swapBack = -1;
    private static int sentSlot = -1;
    private static boolean skipVerification;
    private static final HashMap<Double, BlockPos> chosen = new HashMap<Double, BlockPos>();

    public BedAuraModule(int key) {
        super("BedAura", Category.player, key);
        instance = this;
        this.queueAfterDisable = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        breakModeSetting = (SliderSetting) ModuleManager.setting(this, "Break mode");
        breakSpeed = (SliderSetting) ModuleManager.setting(this, "Break speed");
        fov = (SliderSetting) ModuleManager.setting(this, "FOV");
        rangeSetting = (SliderSetting) ModuleManager.setting(this, "Range");
        rate = (SliderSetting) ModuleManager.setting(this, "Rate");
        allowKillAura = (ButtonSetting) ModuleManager.setting(this, "Allow KillAura");
        breakNearBlock = (ButtonSetting) ModuleManager.setting(this, "Break near block");
        cancelKnockback = (ButtonSetting) ModuleManager.setting(this, "Cancel knockback");
        disableBhop = (ButtonSetting) ModuleManager.setting(this, "Disable bhop");
        disableBreakEffects = (ButtonSetting) ModuleManager.setting(this, "Disable break effects");
        disableRightClick = (ButtonSetting) ModuleManager.setting(this, "Disable right click");
        groundSpoof = (ButtonSetting) ModuleManager.setting(this, "Ground spoof");
        ignoreSlow = (ButtonSetting) ModuleManager.setting(this, "Ignore slow");
        maximizeSpeed = (ButtonSetting) ModuleManager.setting(this, "Maximize speed");
        onlyWhileVisible = (ButtonSetting) ModuleManager.setting(this, "Only while visible");
        prioritizeKillAura = (ButtonSetting) ModuleManager.setting(this, "Prioritize KillAura");
        renderBlockOutline = (ButtonSetting) ModuleManager.setting(this, "Render block outline");
        sendAnimations = (ButtonSetting) ModuleManager.setting(this, "Send animations");
        silentSwing = (ButtonSetting) ModuleManager.setting(this, "Silent swing");
        skipBreakVerification = (ButtonSetting) ModuleManager.setting(this, "Skip break verification");
        guiUpdate();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button != 1 || !e.buttonstate || !Chat.inWorld() || mc().currentScreen != null) {
            return;
        }
        if (disableRightClick.isToggled() && target != null) {
            e.setCanceled(true);
        }
    }

    private static void clear() {
        quickBlock = null;
        nearBlock = null;
        target = null;
        quickTicks = 0;
        counter = 0;
        vanillaProgress = 0.0f;
        progress = 0.0f;
        skipVerification = false;
    }

    @Override
    public String getInfo() {
        return breakModeSetting.getDisplay(false);
    }

    private static boolean open(BlockPos pos) {
        if (skipVerification) {
            skipVerification = false;
            return true;
        }
        boolean bed = BlockUtils.is(pos, Blocks.bed);
        boolean glass = maximizeSpeed.isToggled() && Hypixel.onHypixel();
        for (int i = 0; i < 6; ++i) {
            Block block = BlockUtils.getBlock(BlockUtils.offset(i, pos));
            if (block == Blocks.air || block instanceof BlockLiquid || block == Blocks.fire || block == Blocks.ladder || block == Blocks.iron_bars) {
                return true;
            }
            if (block == Blocks.ender_chest || block == Blocks.enchanting_table || block == Blocks.skull || block == Blocks.anvil || block == Blocks.tripwire || block == Blocks.vine) {
                return true;
            }
            if (block == Blocks.flower_pot) {
                return true;
            }
            if (block instanceof BlockChest || block instanceof BlockDoor || block instanceof BlockTrapDoor || block instanceof BlockFence || block instanceof BlockFenceGate
                    || block instanceof BlockWall || block instanceof BlockStairs) {
                return true;
            }
            if (glass && (block == Blocks.glass || block == Blocks.stained_glass)) {
                return true;
            }
            if (i != 1 && block instanceof BlockSlab) {
                return true;
            }
            if (i < 2 || (bed || block != Blocks.bed) && block != Blocks.end_portal_frame) continue;
            return true;
        }
        return false;
    }

    private static float slowFor(Block block, float f) {
        if (ignoreSlow.isToggled()) {
            double d = 1.0;
            if (PlayerUtils.miningFatigueLevel() > 0) {
                if (block == Blocks.end_stone) {
                    if (f == 0.044444446f) {
                        d = 0.4;
                    }
                } else if (block == Blocks.hardened_clay || block == Blocks.stained_hardened_clay) {
                    if (f == 0.10666667f) {
                        d = 0.3;
                    }
                } else if ((block == Blocks.planks || block == Blocks.log || block == Blocks.log2) && f == 0.06666667f) {
                    d = 0.6;
                }
            } else if (block == Blocks.obsidian && f == 0.012f) {
                d = 0.999;
            }
            if (d != 1.0) {
                f = (float) ((double) f * d);
            }
        }
        return f;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!renderBlockOutline.isToggled() || target == null || !Chat.inWorld()) {
            return;
        }
        float h = !(!targetIsBed || ModuleManager.enabled("BedESP") != null && BedESPModule.renderFullBlock.isToggled()) ? 0.5625f : 1.0f;
        Render3D.blockBox(target.getX(), target.getY(), target.getZ(), 0.0f, 0.0f, 0.0f, 1.0f, h, 1.0f, BedESPModule.theme.getRGB(0.0), true, false);
    }

    private static void swapTicks() {
        if (mode == Mode.Swap) {
            if (rotateTick == 4) {
                rotateTick = -1;
                if (ModuleManager.enabled("KillAura") != null) {
                    KillAuraModule.bedAuraRotated = true;
                }
                if (target != null) {
                    if (BlockUtils.is(target, Blocks.bed)) {
                        quickBreak(target, PlayerUtils.miningFatigueLevel() == 0 ? quickBreakTicks : 17);
                    } else {
                        BlockBreaker.start(target);
                    }
                }
            } else if (rotateTick == 5) {
                rotateTick = -1;
                if (ModuleManager.enabled("KillAura") != null) {
                    KillAuraModule.bedAuraRotated = true;
                }
                if (target != null) {
                    BlockBreaker.stop(target);
                    if (skipBreakVerification.isToggled()) {
                        skipVerification = true;
                    }
                }
            }
        }
    }

    private static void swapToTool(BlockPos pos) {
        int n = ItemUtils.bestToolSlot(BlockUtils.getBlock(pos));
        if (n != -1 && n != PlayerUtils.currentSlot()) {
            swapBack = PlayerUtils.currentSlot();
            sendSlot(n);
        }
    }

    private static void tick(MotionEvent e) {
        Minecraft mc = mc();
        if (waitTicks > 0L && --waitTicks != 0L) {
            if (waitTicks == 1L && mode == Mode.Instant && BlockBreaker.lastAction == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                swapBackNow();
            }
            return;
        }
        BlockPos pos = target;
        if (pos != null) {
            boolean air = BlockUtils.is(pos, Blocks.air);
            if (breakNearBlock.isToggled() && !air && !BlockUtils.is(pos, Blocks.bed) && BlockUtils.is(nearBed, Blocks.bed)) {
                BlockPos other = BlockUtils.otherBedHalf(nearBed);
                if (skipVerification) {
                    swapBackNow();
                    clear();
                    skipVerification = true;
                    return;
                }
                if (open(nearBed) || open(other)) {
                    swapBackNow();
                    if (progress != 0.0f) {
                        BlockBreaker.abort(target);
                    }
                    clear();
                    return;
                }
            }
            if (air || !PlayerUtils.canReach(pos, range)) {
                swapBackNow();
                if (progress != 0.0f && BlockBreaker.lastAction == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                    BlockBreaker.abort(target);
                }
                clear();
            } else {
                if (mode == Mode.Legit) {
                    if (counter >= 2L) {
                        if (++counter >= 10L) {
                            clear();
                        }
                        return;
                    }
                    boolean started = false;
                    if (progress == 0.0f) {
                        if (Mouse.isButtonDown(0) && mc.objectMouseOver != null && BlockUtils.is(mc.objectMouseOver.getBlockPos(), Blocks.bed)) {
                            BlockBreaker.abort(target);
                        }
                        started = true;
                        swapToTool(target);
                        BlockBreaker.start(target);
                    } else if (onlyWhileVisible.isToggled() && !BlockUtils.isVisible(pos)) {
                        BlockBreaker.abort(target);
                        clear();
                        return;
                    }
                    progressPerTick = BlockUtils.breakSpeed(BlockUtils.getBlock(pos), mc.thePlayer.inventory.getStackInSlot(sentSlot == -1 ? mc.thePlayer.inventory.currentItem : sentSlot), ignoreSlow.isToggled());
                    double d = breakSpeed.getInput();
                    if (d != 1.0 && (double) (progress += progressPerTick) < (d = 1.0 - (2.0 - d))) {
                        progress = (float) d;
                    }
                    if (progress >= 1.0f) {
                        BlockBreaker.stop(target);
                        if (e != null && groundSpoof.isToggled() && !e.onGround) {
                            e.onGround = true;
                        }
                        waitTicks = 2L;
                        counter = 2L;
                        return;
                    }
                    if (!started && sendAnimations.isToggled() && !PacketUtils.sentSwing) {
                        BlockBreaker.swing();
                    }
                    mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), target, (int) (progress * 10.0f) - 1);
                    return;
                }
                if (mode == Mode.Instant) {
                    if (rotateTick == 0) {
                        rotateTick = 1;
                        swapToTool(target);
                        quickBreak(target, quickBreakTicks);
                    } else {
                        clear();
                    }
                    return;
                }
                if (mode == Mode.Swap) {
                    if (BlockUtils.is(pos, Blocks.bed)) {
                        if (++counter % 10L == 0L) {
                            target = null;
                            counter = 0L;
                        }
                    } else {
                        int n = ItemUtils.bestToolSlot(BlockUtils.getBlock(pos));
                        Block block = BlockUtils.getBlock(pos);
                        ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(n == -1 ? PlayerUtils.currentSlot() : n);
                        double d = 1.0 - (2.0 - breakSpeed.getInput());
                        double cap = (1.0 - d) / 2.0 - 0.01;
                        progressPerTick = (float) Math.min((double) BlockUtils.breakSpeed(block, itemStack, ignoreSlow.isToggled()), cap);
                        float vanilla = (float) Math.min((double) BlockUtils.breakSpeed(block, itemStack, false), cap);
                        float slowed = slowFor(block, progressPerTick);
                        if (progressPerTick == slowed) {
                            if ((double) progress < d) {
                                if (progress == 0.0f) {
                                    vanillaProgress = 0.0f;
                                }
                                progress = (float) d;
                            }
                        } else {
                            progressPerTick = slowed;
                        }
                        if (progress < 1.0f && progress + progressPerTick >= 1.0f) {
                            if (n != -1 && n != PlayerUtils.currentSlot()) {
                                swapBack = PlayerUtils.currentSlot();
                                sendSlot(n);
                            }
                            rotateTick = 1;
                        } else if (e != null && groundSpoof.isToggled() && !e.onGround && rotateTick == 3) {
                            e.onGround = true;
                        } else if (vanillaProgress >= 1.1f && mc.thePlayer.onGround && BlockBreaker.lastAction == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                            clear();
                            swapBackNow();
                            return;
                        }
                        progress += progressPerTick;
                        vanillaProgress += vanilla;
                        if (sendAnimations.isToggled() && !PacketUtils.sentSwing && BlockBreaker.lastAction == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK && rotateTick != 5) {
                            BlockBreaker.swing();
                        }
                    }
                    return;
                }
            }
        }
        if (++rateCounter % rateTicks != 0L || target != null) {
            return;
        }
        outer:
        for (int y = radius; y >= -radius; --y) {
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    if (mc.theWorld == null) break outer;
                    BlockPos p = new BlockPos(mc.thePlayer.posX + (double) x, mc.thePlayer.posY + (double) y, mc.thePlayer.posZ + (double) z);
                    IBlockState state = BlockUtils.getState(p);
                    BlockPos[] halves;
                    if (state.getBlock() != Blocks.bed || state.getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT || BedWarsModule.isOwnBed(p)
                            || !PlayerUtils.canReach((halves = new BlockPos[]{p, p.offset(state.getValue(BlockBed.FACING))})[0], range) && !PlayerUtils.canReach(halves[1], range)) continue;
                    float f = (float) fov.getInput();
                    if (f != 360.0f && !PlayerUtils.inFov(f, halves[0]) && !PlayerUtils.inFov(f, halves[1])) continue;
                    if (onlyWhileVisible.isToggled()) {
                        BlockPos visible = null;
                        if (BlockUtils.isVisible(halves[0])) {
                            visible = halves[0];
                        }
                        if (BlockUtils.isVisible(halves[1])) {
                            visible = visible == null ? halves[1] : PlayerUtils.closer(halves[0], halves[1]);
                        }
                        if (visible != null) {
                            setTarget(visible);
                        }
                        return;
                    }
                    double key = Hypixel.positionHash(halves[0].getX(), halves[0].getY(), halves[0].getZ()) * Hypixel.positionHash(halves[1].getX(), halves[1].getY(), halves[1].getZ());
                    BlockPos half = chosen.get(key);
                    if (half == null || !PlayerUtils.canReach(half, range)) {
                        half = PlayerUtils.closer(halves[0], halves[1]);
                        chosen.put(key, half);
                    }
                    if (breakNearBlock.isToggled() && nearBlock == null) {
                        if (open(halves[0]) || open(halves[1])) {
                            setTarget(half);
                            break outer;
                        }
                        nearBed = half;
                        nearBlock = BlockUtils.fastestAround(halves, range);
                        setTarget(nearBlock);
                        break outer;
                    }
                    setTarget(half);
                    break outer;
                }
            }
        }
    }

    private static void quickBreak(BlockPos pos, int ticks) {
        BlockBreaker.start(pos);
        quickBlock = pos;
        quickTotal = quickTicks = ticks;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        run(e);
    }

    private static void run(MotionEvent e) {
        Minecraft mc = mc();
        if (PlayerUtils.isAdventure() || Hypixel.state == Hypixel.State.BRIDGE_PRACTICE) {
            return;
        }
        if (Overlay.screenOpen() || ScaffoldModule.active || AutoLadderModule.target != null || AutoBlockinModule.active
                || prioritizeKillAura.isToggled() && KillAuraModule.target != null) {
            if (target != null) {
                if (progress != 0.0f) {
                    BlockBreaker.abort(target);
                }
                clear();
            }
            return;
        }
        if (quickBlock != null) {
            if (mc.thePlayer.capabilities.isCreativeMode) {
                quickBlock = null;
            } else {
                if (quickTicks > 0) {
                    --quickTicks;
                    if (sendAnimations.isToggled()) {
                        BlockBreaker.swing();
                    }
                    if (mode == Mode.Swap && quickTicks == 1) {
                        rotateTick = 6;
                    }
                } else {
                    BlockBreaker.stop(quickBlock);
                    quickBlock = null;
                    if (e != null && groundSpoof.isToggled() && !e.onGround) {
                        e.onGround = true;
                    }
                    if (mode == Mode.Swap) {
                        rotateTick = -1;
                    } else {
                        waitTicks = 2L;
                    }
                }
                return;
            }
        }
        tick(e);
        swapTicks();
    }

    @Override
    public void onPreUpdate() {
        if (mc().thePlayer.isRiding()) {
            run(null);
        }
    }

    @Override
    public void afterDisable() {
        if (target != null) {
            swapBackNow();
            if (BlockBreaker.lastAction == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                BlockBreaker.abort(target);
            }
        }
        target = null;
        targetIsBed = false;
        BlockBreaker.lastAction = null;
        swapBack = -1;
    }

    private static void swapBackNow() {
        if (swapBack != -1) {
            sendSlot(swapBack);
            swapBack = -1;
        }
    }

    private static void sendSlot(int n) {
        ItemStack itemStack;
        if (sentSlot == n) {
            return;
        }
        sentSlot = n;
        PacketUtils.send(new C09PacketHeldItemChange(sentSlot));
        if (mode == Mode.Swap && PlayerUtils.isBlocking() && ModuleManager.enabled("KillAura") != null && !KillAuraModule.autoBlock.packet
                && (itemStack = mc().thePlayer.inventory.getStackInSlot(n)) != null && itemStack.getItem() instanceof ItemSword) {
            KillAuraModule.setBlocking(true, true);
        }
    }

    @Override
    public void guiUpdate() {
        if (range != rangeSetting.getInput()) {
            range = rangeSetting.getInput();
            radius = (int) (Math.ceil(range) + 2.0);
        }
        if (rateSeconds != rate.getInput()) {
            rateSeconds = rate.getInput();
            rateTicks = (long) (rateSeconds * 20.0);
        }
        if (nearBlock != null && !breakNearBlock.isToggled()) {
            nearBlock = null;
        }
        if (mode.ordinal() != (int) breakModeSetting.getInput()) {
            if (this.isEnabled()) {
                this.onDisable();
            }
            mode = Mode.values()[(int) breakModeSetting.getInput()];
        }
        quickBreakTicks = maximizeSpeed.isToggled() ? 2 : 4;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            chosen.clear();
            clear();
        }
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        switch (mode) {
            case Legit: {
                if (counter > 2L) break;
                if (nearBlock != null) {
                    return RotationUtils.toBlockFixed(nearBlock, null, yaw, pitch);
                }
                if (target == null) break;
                return RotationUtils.toBlockFixed(target, null, yaw, pitch);
            }
            case Instant: {
                if (rotateTick == -1) break;
                if (rotateTick == 0) {
                    if (target != null) {
                        return RotationUtils.toBlockFixed(target, null, yaw, pitch);
                    }
                } else if (rotateTick == 1 && quickBlock != null) {
                    return RotationUtils.toBlockFixed(quickBlock, null, yaw, pitch);
                }
                rotateTick = -1;
                break;
            }
            case Swap: {
                if (rotateTick == -1) break;
                rotateTick += 2;
                return target == null ? null : RotationUtils.toBlockFixed(target, null, yaw, pitch);
            }
        }
        return null;
    }

    @Override
    public void onDisable() {
        counter = 0L;
        waitTicks = 0L;
        rateCounter = 0L;
        quickBlock = null;
        nearBed = null;
        nearBlock = null;
        rotateTick = -1;
        sentSlot = -1;
        quickTotal = 0;
        quickTicks = 0;
        progress = vanillaProgress = 0.0f;
        skipVerification = false;
        BlockBreaker.sending = false;
        chosen.clear();
    }

    public static boolean allowsKillAura(boolean attack) {
        if (ModuleManager.enabled("BedAura") != null) {
            if (prioritizeKillAura.isToggled() && KillAuraModule.target != null) {
                return true;
            }
            if (attack) {
                return allowKillAura.isToggled() || target == null && nearBlock == null;
            }
            if (allowKillAura.isToggled()) {
                if (mode == Mode.Swap && (rotateTick != -1 || progress > 0.0f && progress + progressPerTick >= 1.0f)) {
                    if (KillAuraModule.stopBlocking(true)) {
                        waitTicks = 2L;
                    }
                    return false;
                }
            } else if (target != null || nearBlock != null) {
                if (KillAuraModule.stopBlocking(false) && mode != Mode.Swap) {
                    waitTicks = 2L;
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging p = (C07PacketPlayerDigging) packet;
            return !(target != null && !BlockBreaker.sending && p.getStatus() != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM);
        }
        return true;
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (packet instanceof S12PacketEntityVelocity || packet instanceof S27PacketExplosion) {
            return !(cancelKnockback.isToggled() && target != null);
        }
        return true;
    }

    private static void setTarget(BlockPos pos) {
        target = pos;
        targetIsBed = BlockUtils.getBlock(pos) == Blocks.bed;
        switch (mode) {
            case Instant: {
                rotateTick = 0;
                break;
            }
            case Swap: {
                rotateTick = 0;
                break;
            }
            default:
                break;
        }
    }
}
