package xyz.daikirai.module.player;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.network.BlinkModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class AutoBlockinModule extends Module {
    public static AutoBlockinModule instance;
    public static SliderSetting delay;
    public static ButtonSetting disableInCreative, disableObsidian, disableWhenComplete, onlyWhileMiddleClicking, prioritizeStrongerBlocks,
            showPercentage, silentAim, silentSwing, skipNearBed;

    private static final class Node {
        final BlockPos pos;
        final int score;
        final int cost;

        Node(BlockPos pos, int score, int cost) {
            this.pos = pos;
            this.score = score;
            this.cost = cost;
        }
    }

    public static boolean active;
    public static String percentText;
    private static int percentColor;
    private static float percentHalfWidth;
    private static float enclosed;
    private static long startedAt, stoppedAt;
    private static boolean middleHeld;
    private static boolean waitRelease;
    private static int previousSlot = -1, blockSlot = -1;
    private static BlockPos against;
    private static EnumFacing face;
    private static BlockPos targetPos;
    private static BlockPos aimedAgainst;
    private static EnumFacing aimedFace;
    private static List<BlockPos> path;
    private static final Deque<BlockPos> retry = new ArrayDeque<BlockPos>();
    private static final Set<BlockPos> retrySet = new HashSet<BlockPos>();
    private static final Set<BlockPos> failed = new HashSet<BlockPos>();
    private static EnumFacing[] facings;
    private static EnumFacing startFacing;
    private static final EnumFacing[] ORDER = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.DOWN};
    private static final EnumFacing[] ORDER_UP = {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN};
    private static final BlockPos.MutableBlockPos MUTABLE = new BlockPos.MutableBlockPos();
    private static float aimYaw, aimPitch;
    private static Float fromYaw, fromPitch;
    private static Float lastYaw, lastPitch;
    private static Float refYaw;
    private static int steps, step;
    private static boolean roundUp = true;

    public AutoBlockinModule(int key) {
        super("AutoBlockin", Category.player, key);
        instance = this;
        this.lite = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        delay = (SliderSetting) ModuleManager.setting(this, "Delay");
        disableInCreative = (ButtonSetting) ModuleManager.setting(this, "Disable in creative");
        disableObsidian = (ButtonSetting) ModuleManager.setting(this, "Disable obsidian");
        disableWhenComplete = (ButtonSetting) ModuleManager.setting(this, "Disable when complete");
        onlyWhileMiddleClicking = (ButtonSetting) ModuleManager.setting(this, "Only while middle clicking");
        prioritizeStrongerBlocks = (ButtonSetting) ModuleManager.setting(this, "Prioritize stronger blocks");
        showPercentage = (ButtonSetting) ModuleManager.setting(this, "Show percentage");
        silentAim = (ButtonSetting) ModuleManager.setting(this, "Silent aim");
        silentSwing = (ButtonSetting) ModuleManager.setting(this, "Silent swing");
        skipNearBed = (ButtonSetting) ModuleManager.setting(this, "Skip near bed");
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        Minecraft mc = mc();
        if (!Chat.inWorld() || !mc.inGameHasFocus || PlayerUtils.isAdventure() || mc.currentScreen != null && !Overlay.ownScreen() || !PacketUtils.noUseOrDig()
                || ScaffoldModule.active || disableInCreative.isToggled() && PlayerUtils.isCreative()) {
            if (middleHeld) {
                stop();
            } else if (active) {
                reset();
            }
            return null;
        }
        if (onlyWhileMiddleClicking.isToggled() && waitRelease) {
            if (Mouse.isButtonDown(2)) {
                return null;
            }
            waitRelease = false;
        }
        boolean run = !onlyWhileMiddleClicking.isToggled() || Mouse.isButtonDown(2);
        if (!run && middleHeld) {
            stop();
        }
        middleHeld = run;
        if (!run) {
            return null;
        }
        if (!active) {
            blockSlot = findSlot();
            if (blockSlot == -1) {
                return null;
            }
            previousSlot = mc.thePlayer.inventory.currentItem;
            if (AutoToolModule.previousSlot != -1) {
                previousSlot = AutoToolModule.previousSlot;
            }
            PlayerUtils.setSlot(blockSlot, false);
            active = true;
            startedAt = System.currentTimeMillis();
            resetSteps();
            resetAim();
            captureFacings();
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
        ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        if (active && !usable(itemStack)) {
            int n = findSlot();
            if (n == -1) {
                stop();
                return null;
            }
            blockSlot = n;
            PlayerUtils.setSlot(blockSlot, false);
        }
        failed.clear();
        RotationUtils.AimPoint last = null;
        for (int i = 0; i < 5; ++i) {
            if (!(hasTarget() || findTarget() || retryTarget())) {
                clearTarget();
                break;
            }
            RotationUtils.AimPoint aim = RotationUtils.aimAtFace(against, face);
            if (aim == null) {
                markFailed();
                clearTarget();
                continue;
            }
            if (!canPlace(aim, itemStack)) {
                markFailed();
                clearTarget();
                continue;
            }
            aimAt(aim);
            if (steps > 0 && step < steps) {
                float t = clamp01(((float) step + 1.0f) / (float) steps);
                float startYaw = fromYaw != null ? fromYaw : (lastYaw != null ? unwrap(lastYaw, refYaw != null ? refYaw : mc.thePlayer.rotationYaw) : mc.thePlayer.rotationYaw);
                float startPitch = fromPitch != null ? fromPitch : (lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch);
                float y = lerp(startYaw, aimYaw, t);
                float p = lerp(startPitch, aimPitch, t);
                ++step;
                return finish(y, p, yaw, pitch);
            }
            double reach = mc.playerController.getBlockReachDistance();
            float wrappedYaw = MathHelper.wrapAngleTo180_float(aimYaw);
            float aimP = aimPitch;
            MovingObjectPosition hit = PlayerUtils.rayTraceBlock(against, face, reach, wrappedYaw, aimP);
            if (hit != null && mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, against, face, aim.point)) {
                PlayerUtils.swing(silentSwing.isToggled(), true);
                if (path != null && !path.isEmpty() && path.get(0).equals(targetPos)) {
                    path.remove(0);
                }
                clearTarget();
                resetSteps();
                float y = lastYaw != null ? lastYaw : wrappedYaw;
                if (refYaw == null) {
                    refYaw = y;
                }
                float u = unwrap(y, refYaw);
                setFrom(u, lastPitch != null ? lastPitch : aimP);
                return finish(aimYaw, aimPitch, yaw, pitch);
            }
            last = aim;
            markFailed();
            clearTarget();
        }
        if (fromYaw != null || lastYaw != null) {
            int n = delay != null ? (int) delay.getInput() : 0;
            if (n > 0 && step < n) {
                float t = clamp01(((float) step + 1.0f) / (float) n);
                float startYaw = fromYaw != null ? fromYaw : (lastYaw != null ? unwrap(lastYaw, refYaw != null ? refYaw : mc.thePlayer.rotationYaw) : mc.thePlayer.rotationYaw);
                float startPitch = fromPitch != null ? fromPitch : (lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch);
                float y = lerp(startYaw, aimYaw, t);
                float p = lerp(startPitch, aimPitch, t);
                ++step;
                return finish(y, p, yaw, pitch);
            }
            return finish(aimYaw, aimPitch, yaw, pitch);
        }
        return last == null ? null : finish(last.yaw, last.pitch, yaw, pitch);
    }

    private static void aimAt(RotationUtils.AimPoint aim) {
        Minecraft mc = mc();
        boolean changed = aimedAgainst == null || !aimedAgainst.equals(against) || aimedFace != face;
        float dYaw = Math.abs(wrap(aim.yaw - aimYaw));
        float dPitch = Math.abs(aim.pitch - aimPitch);
        boolean moved = dYaw > 1.0f || dPitch > 1.0f;
        if (!changed && !moved && fromYaw != null) {
            float ref = refYaw != null ? refYaw : mc.thePlayer.rotationYaw;
            aimYaw = unwrap(aim.yaw, ref);
            aimPitch = aim.pitch;
        } else {
            float y = lastYaw != null ? lastYaw : mc.thePlayer.rotationYaw;
            float p = lastPitch != null ? lastPitch : mc.thePlayer.rotationPitch;
            if (refYaw == null) {
                refYaw = y;
            }
            float u = unwrap(y, refYaw);
            float target = unwrap(aim.yaw, refYaw);
            setFrom(u, p);
            aimYaw = target;
            aimPitch = aim.pitch;
            aimedAgainst = against;
            aimedFace = face;
        }
    }

    private static boolean ownVertical(BlockPos pos, EnumFacing facing) {
        BlockPos own = BlockUtils.playerPos();
        return pos.equals(own) && (facing == EnumFacing.UP || facing == EnumFacing.DOWN);
    }

    private static boolean retryTarget() {
        int n = retry.size();
        while (n-- > 0 && !retry.isEmpty()) {
            BlockPos pos = retry.pollFirst();
            if (pos == null) continue;
            if (skip(pos) || !BlockUtils.isReplaceable(pos)) {
                retrySet.remove(pos);
                continue;
            }
            if (findSupport(pos)) {
                retrySet.remove(pos);
                return true;
            }
            retry.addLast(pos);
        }
        return false;
    }

    private static boolean findTarget() {
        BlockPos own = BlockUtils.playerPos();
        BlockPos roof = own.up(2);
        if (path != null && !path.isEmpty()) {
            while (!path.isEmpty() && (retrySet.contains(path.get(0)) || BlockUtils.isPlayerBlock(path.get(0)))) {
                path.remove(0);
            }
            BlockPos next;
            if (!path.isEmpty() && !skip(next = path.get(0)) && findSupport(next)) {
                return true;
            }
        }
        if (BlockUtils.isReplaceable(roof) && BlockUtils.hasSupport(roof)) {
            if (!skip(roof) && findSupport(roof)) {
                return true;
            }
        }
        List<BlockPos> list;
        BlockPos next;
        if (BlockUtils.isReplaceable(roof) && (list = findPath(roof, own, 8, -2, 6)) != null && !list.isEmpty()) {
            path = list;
            while (!path.isEmpty() && BlockUtils.isPlayerBlock(path.get(0))) {
                path.remove(0);
            }
            if (!path.isEmpty() && !skip(next = path.get(0)) && findSupport(next)) {
                return true;
            }
        }
        for (EnumFacing facing : facings()) {
            BlockPos below = own.down().offset(facing);
            BlockPos feet = own.offset(facing);
            BlockPos head = feet.up();
            check:
            {
                if (BlockUtils.isReplaceable(below)) {
                    if (BlockUtils.hasSupport(below)) {
                        if (!skip(below)) {
                            if (!findSupportUp(below)) {
                                break check;
                            }
                            return true;
                        }
                    } else {
                        list = findPath(below, own, Math.max(3, 8 / 2), -6, 0);
                        if (list != null && !list.isEmpty()) {
                            path = list;
                            while (!path.isEmpty() && BlockUtils.isPlayerBlock(path.get(0))) {
                                path.remove(0);
                            }
                            if (!path.isEmpty() && !skip(next = path.get(0)) && findSupport(next)) {
                                return true;
                            }
                        }
                    }
                }
            }
            if (BlockUtils.isReplaceable(feet)) {
                if (!skip(feet) && findSupportBeside(feet, own, facing)) {
                    return true;
                }
                if (!BlockUtils.hasSupport(feet)) {
                    list = findPath(feet, own, Math.max(2, 8 / 2), -3, 1);
                    if (list != null && !list.isEmpty()) {
                        path = list;
                        while (!path.isEmpty() && BlockUtils.isPlayerBlock(path.get(0))) {
                            path.remove(0);
                        }
                        if (!path.isEmpty() && !skip(next = path.get(0)) && findSupport(next)) {
                            return true;
                        }
                    }
                } else if (!skip(feet) && findSupport(feet, ORDER)) {
                    return true;
                }
            }
            if (!BlockUtils.isReplaceable(head)) continue;
            if (BlockUtils.hasSupport(head)) {
                if (skip(head) || !findSupport(head, ORDER)) continue;
                return true;
            }
            list = findPath(head, own, Math.max(2, 8 / 2), -2, 2);
            if (list == null || list.isEmpty()) continue;
            path = list;
            while (!path.isEmpty() && BlockUtils.isPlayerBlock(path.get(0))) {
                path.remove(0);
            }
            if (path.isEmpty() || skip(next = path.get(0)) || !findSupport(next)) continue;
            return true;
        }
        return BlockUtils.isReplaceable(roof) && BlockUtils.hasSupport(roof) && !skip(roof) && findSupport(roof);
    }

    private static int score(Node n) {
        return n.score;
    }

    private static void captureFacings() {
        EnumFacing f = startFacing = mc().thePlayer.getHorizontalFacing();
        facings = new EnumFacing[]{f, f.rotateY(), f.getOpposite(), f.rotateYCCW()};
    }

    private static int findSlot() {
        int slot = -1;
        int most = 0;
        float best = -1.0f;
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = mc().thePlayer.inventory.getStackInSlot(i);
            if (!usable(itemStack)) continue;
            Block block = BlockUtils.blockOf(itemStack);
            int size = itemStack.stackSize;
            if (prioritizeStrongerBlocks.isToggled()) {
                float f = BlockUtils.breakTicks(block, null, true);
                if (f < 0.0f) continue;
                if (!(f > best || f == best && size > most)) continue;
                best = f;
                most = size;
                slot = i;
                continue;
            }
            if (size <= most) continue;
            most = size;
            slot = i;
        }
        return slot;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static boolean usable(ItemStack itemStack) {
        if (!BlockUtils.isBlockItem(itemStack)) {
            return false;
        }
        Block block = BlockUtils.blockOf(itemStack);
        if (!BlockUtils.isPlaceable(block)) {
            return false;
        }
        if (block == Blocks.pumpkin || disableObsidian.isToggled() && block == Blocks.obsidian) {
            return false;
        }
        return itemStack.stackSize > 0;
    }

    private static boolean findSupportBeside(BlockPos pos, BlockPos own, EnumFacing facing) {
        if (BlockUtils.isPlayerBlock(pos)) {
            return false;
        }
        if (pos.getY() == own.getY()) {
            if (!ownVertical(own, facing) && BlockUtils.isSupport(BlockUtils.getBlock(own))) {
                against = own;
                face = facing;
                targetPos = pos;
                return true;
            }
            BlockPos below = pos.down();
            if (!ownVertical(below, EnumFacing.UP) && BlockUtils.isSupport(BlockUtils.getBlock(below))) {
                against = below;
                face = EnumFacing.UP;
                targetPos = pos;
                return true;
            }
        }
        return findSupport(pos, ORDER);
    }

    private static boolean skip(BlockPos pos) {
        return failed.contains(pos) || BlockUtils.isPlayerBlock(pos) || skipNearBed.isToggled() && BlockUtils.bedAdjacent(pos);
    }

    private static void stop() {
        if (previousSlot >= 0 && previousSlot < 9) {
            PlayerUtils.setSlot(previousSlot, false);
        }
        reset();
    }

    private static boolean canPlace(RotationUtils.AimPoint aim, ItemStack itemStack) {
        if (!BlockUtils.isBlockItem(itemStack)) {
            return false;
        }
        if (against == null || face == null || targetPos == null) {
            return false;
        }
        double reach = mc().playerController.getBlockReachDistance();
        MovingObjectPosition hit = PlayerUtils.rayTraceBlock(against, face, reach, aim.yaw, aim.pitch);
        return hit != null && BlockUtils.canPlace(hit, itemStack);
    }

    private static List<BlockPos> findPath(BlockPos pos, BlockPos own, int r, int minY, int maxY) {
        HashSet<BlockPos> solids = new HashSet<BlockPos>();
        HashSet<BlockPos> open = new HashSet<BlockPos>();
        BlockUtils.scan(solids, open, retrySet, own, r, minY, maxY);
        if (open.isEmpty()) {
            return null;
        }
        for (EnumFacing f : EnumFacing.VALUES) {
            if (solids.contains(pos.offset(f.getOpposite()))) {
                return null;
            }
        }
        ArrayList<BlockPos> starts = new ArrayList<BlockPos>();
        BlockUtils.touching(starts, solids, open, own);
        if (starts.isEmpty()) {
            return null;
        }
        HashSet<BlockPos> goals = new HashSet<BlockPos>();
        for (EnumFacing f : EnumFacing.VALUES) {
            BlockPos p = pos.offset(f.getOpposite());
            if (open.contains(p) && !retrySet.contains(p)) {
                goals.add(p);
            }
        }
        if (goals.isEmpty()) {
            return null;
        }
        int size = open.size();
        int limit = Math.min(8000, Math.max(500, size * 2));
        PriorityQueue<Node> queue = new PriorityQueue<Node>(Comparator.comparingInt(AutoBlockinModule::score));
        HashMap<BlockPos, Integer> cost = new HashMap<BlockPos, Integer>(size);
        HashMap<BlockPos, BlockPos> parent = new HashMap<BlockPos, BlockPos>(size);
        for (BlockPos p : starts) {
            int h = BlockUtils.nearestDistance(p, goals);
            cost.put(p, 0);
            queue.add(new Node(p, h, 0));
            parent.put(p, null);
        }
        HashSet<BlockPos> closed = new HashSet<BlockPos>();
        int visited = 0;
        BlockPos found = null;
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            BlockPos p = node.pos;
            if (closed.contains(p)) continue;
            closed.add(p);
            if (goals.contains(p)) {
                found = p;
                break;
            }
            if (++visited > limit) {
                break;
            }
            int c = cost.get(p);
            for (EnumFacing f : EnumFacing.VALUES) {
                BlockPos n = p.offset(f);
                if (!open.contains(n) || closed.contains(n)) continue;
                int step = 1 + (p.getY() != n.getY() ? 1 : 0);
                int nc = c + step;
                Integer old = cost.get(n);
                if (old == null || nc < old) {
                    cost.put(n, nc);
                    parent.put(n, p);
                    int h = BlockUtils.nearestDistance(n, goals);
                    queue.add(new Node(n, nc + h, nc));
                }
            }
        }
        if (found == null) {
            return null;
        }
        ArrayList<BlockPos> route = new ArrayList<BlockPos>();
        for (BlockPos p = found; p != null; p = parent.get(p)) {
            route.add(p);
        }
        Collections.reverse(route);
        ArrayList<BlockPos> out = new ArrayList<BlockPos>();
        for (BlockPos p : route) {
            if (!BlockUtils.isSupport(BlockUtils.getBlock(p)) && !BlockUtils.isPlayerBlock(p)) {
                out.add(p);
            }
        }
        return out;
    }

    private static void markFailed() {
        if (targetPos != null) {
            failed.add(targetPos);
            if (path != null && !path.isEmpty() && targetPos.equals(path.get(0))) {
                if (!retrySet.contains(targetPos)) {
                    addRetry(targetPos);
                }
                path = null;
            }
            if (!retrySet.contains(targetPos)) {
                if (!BlockUtils.isPlayerBlock(targetPos)) {
                    addRetry(targetPos);
                }
            }
        }
    }

    private static void addRetry(BlockPos pos) {
        if (retry.size() >= 3) {
            BlockPos old = retry.pollFirst();
            if (old != null) {
                retrySet.remove(old);
            }
        }
        retry.addLast(pos);
        retrySet.add(pos);
    }

    private static void resetAim() {
        fromYaw = null;
        fromPitch = null;
        aimYaw = 0.0f;
        aimPitch = 0.0f;
        aimedAgainst = null;
        aimedFace = null;
    }

    private static boolean hasTarget() {
        return against != null && face != null && targetPos != null && !BlockUtils.isPlayerBlock(targetPos) && BlockUtils.isReplaceable(targetPos);
    }

    private static float wrap(float f) {
        f %= 360.0f;
        if (f >= 180.0f) {
            f -= 360.0f;
        }
        if (f < -180.0f) {
            f += 360.0f;
        }
        return f;
    }

    private static void reset() {
        if (active) {
            active = false;
            stoppedAt = System.currentTimeMillis();
        }
        middleHeld = false;
        roundUp = true;
        previousSlot = -1;
        blockSlot = -1;
        path = null;
        retry.clear();
        retrySet.clear();
        failed.clear();
        step = 0;
        resetAim();
        lastPitch = null;
        lastYaw = null;
        refYaw = null;
        clearTarget();
        startFacing = null;
        facings = null;
    }

    private static void setFrom(float yaw, float pitch) {
        fromYaw = yaw;
        fromPitch = pitch;
    }

    private static float[] finish(float yaw, float pitch, float prevYaw, float prevPitch) {
        float[] rot = RotationUtils.fixSensitivity(yaw, pitch, prevYaw, prevPitch);
        lastYaw = rot[0];
        lastPitch = rot[1];
        if (refYaw == null) {
            refYaw = lastYaw;
        }
        refYaw = unwrap(lastYaw, refYaw);
        return new float[]{lastYaw, lastPitch};
    }

    private static void clearTarget() {
        against = null;
        face = null;
        targetPos = null;
    }

    private static void resetSteps() {
        double d = delay.getInput();
        if (d % 1.0 == 0.5) {
            d = Math.floor(d);
            if (roundUp) {
                ++d;
            }
            roundUp = !roundUp;
        }
        steps = (int) d;
        step = 0;
    }

    private static boolean findSupportUp(BlockPos pos) {
        if (BlockUtils.isPlayerBlock(pos)) {
            return false;
        }
        return findSupport(pos, ORDER_UP);
    }

    private static float enclosure(int x, int y, int z) {
        int n = 0;
        if (solid(x, y + 2, z)) ++n;
        if (solid(x + 1, y, z)) ++n;
        if (solid(x - 1, y, z)) ++n;
        if (solid(x, y, z + 1)) ++n;
        if (solid(x, y, z - 1)) ++n;
        if (solid(x + 1, y + 1, z)) ++n;
        if (solid(x - 1, y + 1, z)) ++n;
        if (solid(x, y + 1, z + 1)) ++n;
        if (solid(x, y + 1, z - 1)) ++n;
        return (float) n / 9.0f;
    }

    @Override
    public void onUpdate() {
        Minecraft mc = mc();
        if (lastYaw != null && !silentAim.isToggled()) {
            mc.thePlayer.rotationYaw = lastYaw;
            mc.thePlayer.rotationPitch = lastPitch;
        }
        if (active) {
            int x = MathHelper.floor_double(mc.thePlayer.posX);
            int y = MathHelper.floor_double(mc.thePlayer.posY);
            int z = MathHelper.floor_double(mc.thePlayer.posZ);
            enclosed = enclosure(x, y, z);
            int percent = Math.round(enclosed * 100.0f);
            percentText = percent + "%";
            percentColor = percent != 100 ? -1 : Color.green.getRGB();
            percentHalfWidth = (float) mc.fontRendererObj.getStringWidth(percentText) / 2.0f - 2.0f;
            if (percent == 100 && disableWhenComplete.isToggled()) {
                if (!onlyWhileMiddleClicking.isToggled()) {
                    this.disable();
                } else {
                    stop();
                    waitRelease = true;
                }
            }
        }
    }

    private static EnumFacing[] facings() {
        return facings != null ? facings : new EnumFacing[]{EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH};
    }

    private static float unwrap(float yaw, float ref) {
        float d = wrap(yaw - ref);
        return ref + d;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.buttonstate && e.button != 0 && active) {
            e.setCanceled(true);
        }
    }

    private static boolean solid(int x, int y, int z) {
        MUTABLE.set(x, y, z);
        return BlockUtils.isSupport(BlockUtils.getBlock(MUTABLE));
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = mc();
        if (showPercentage.isToggled() && percentText != null && e.phase == TickEvent.Phase.END && Overlay.canRender()) {
            long now = System.currentTimeMillis();
            int a;
            long age;
            if (!active) {
                age = now - stoppedAt;
                if (age >= 125L) {
                    a = 0;
                    percentText = null;
                } else {
                    a = (int) ((float) 240 - (float) age / (float) 125L * (float) 240);
                }
            } else {
                age = now - startedAt;
                if (age >= 125L) {
                    a = 240;
                } else {
                    a = (int) ((float) age / (float) 125L * (float) 240);
                }
            }
            if (a > 0) {
                float x = (float) RenderUtils.scaledWidth / 2.0f - percentHalfWidth;
                float y = (float) RenderUtils.scaledHeight / 2.0f + 11.0f;
                if (BlinkModule.text != null) {
                    y += 10.0f;
                }
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                mc.fontRendererObj.drawString(percentText, x, y, RenderUtils.withAlpha(percentColor, RenderUtils.clampAlpha(a)), true);
                GlStateManager.disableBlend();
            }
        }
    }

    @Override
    public void onDisable() {
        reset();
        waitRelease = false;
    }

    private static boolean findSupport(BlockPos pos) {
        BlockPos own = BlockUtils.playerPos();
        if (!pos.equals(own.north()) && !pos.equals(own.south())) {
            BlockPos below = own.down();
            return !pos.equals(below.east()) && !pos.equals(below.west()) && !pos.equals(below.north()) && !pos.equals(below.south())
                    ? findSupport(pos, ORDER) : findSupportUp(pos);
        }
        return false;
    }

    private static boolean findSupport(BlockPos pos, EnumFacing[] order) {
        if (BlockUtils.isPlayerBlock(pos)) {
            return false;
        }
        for (EnumFacing f : order) {
            BlockPos n = pos.offset(f.getOpposite());
            if (ownVertical(n, f)) continue;
            Block block = BlockUtils.getBlock(n);
            if (BlockUtils.isSupport(block)) {
                against = n;
                face = f;
                targetPos = pos;
                return true;
            }
        }
        return false;
    }
}
