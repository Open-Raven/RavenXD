package xyz.daikirai.module.minigames;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.InfoHud;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import xyz.daikirai.util.Theme;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHay;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SpeedBuildersModule extends Module {
    public static SliderSetting activation, autoPlaceSetting, placeDelay, autoSwapSetting, renderBlocksSetting, showStatsSetting;
    public static ButtonSetting infoHud, saveStats, showTime, strictPlacements;

    public enum AutoPlace {
        Disabled, Hover
    }

    public enum AutoSwap {
        Disabled, Incremental, Hover
    }

    public enum RenderBlocks {
        Disabled, All, Holding, Placeable
    }

    public enum ShowStats {
        Disabled, Chat, Hover
    }

    private static AutoPlace autoPlace = AutoPlace.Disabled;
    private static AutoSwap autoSwap = AutoSwap.Disabled;
    private static RenderBlocks renderBlocks = RenderBlocks.Holding;
    private static ShowStats showStats = ShowStats.Chat;

    private static int ticks, state, lastState;
    private static int judgedAt;
    private static BlockPos center;
    private static Map<BlockPos, IBlockState> build;
    private static List<BlockPos> waiting;
    private static String status, theme, themeKey;
    private static int timeTicks;
    private static long timeFadeAt;
    private static ItemStack lastItem;
    private static Map<BlockPos, Integer> highlights;
    private static Map<IBlockState, Integer> missing;
    private static boolean auto;
    private static int lastPlaceTick, placeWait;
    private static boolean halfTick = true;
    private static int totalBlocks, blocksLeft;
    private static final Map<String, Double> bestTimes = new HashMap<String, Double>();
    private static final File FILE = new File(ClientConfig.DIR, "speedbuilders.txt");

    public SpeedBuildersModule(int key) {
        super("Speed Builders", Category.minigames, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        activation = (SliderSetting) ModuleManager.setting(this, "Activation");
        autoPlaceSetting = (SliderSetting) ModuleManager.setting(this, "Auto place");
        placeDelay = (SliderSetting) ModuleManager.setting(this, "Place delay");
        autoSwapSetting = (SliderSetting) ModuleManager.setting(this, "Auto swap");
        renderBlocksSetting = (SliderSetting) ModuleManager.setting(this, "Render blocks");
        showStatsSetting = (SliderSetting) ModuleManager.setting(this, "Show stats");
        infoHud = (ButtonSetting) ModuleManager.setting(this, "Info HUD");
        saveStats = (ButtonSetting) ModuleManager.setting(this, "Save stats");
        showTime = (ButtonSetting) ModuleManager.setting(this, "Show time");
        strictPlacements = (ButtonSetting) ModuleManager.setting(this, "Strict placements");
    }

    private static int facingCase(EnumFacing f) {
        switch (f) {
            case DOWN: return 6;
            case UP: return 5;
            case NORTH: return 1;
            case SOUTH: return 2;
            case WEST: return 3;
            default: return 4;
        }
    }

    private static void findCenter() {
        Minecraft mc = mc();
        for (int i = -5; i <= 5; ++i) {
            next:
            for (int j = -5; j <= 5; ++j) {
                BlockPos pos = new BlockPos(mc.thePlayer.posX + (double) i, 71.0, mc.thePlayer.posZ + (double) j);
                IBlockState s = mc.theWorld.getBlockState(pos);
                if (s.getBlock() != Blocks.quartz_block || s.getBlock().getMetaFromState(s) != 1) continue;
                for (EnumFacing f : EnumFacing.HORIZONTALS) {
                    if (BlockUtils.getBlock(pos.offset(f)) != Blocks.grass) continue next;
                }
                center = pos;
                return;
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (state != 4 || renderBlocks == RenderBlocks.Disabled || highlights == null || !Chat.inWorld()) {
            return;
        }
        for (Map.Entry<BlockPos, Integer> entry : highlights.entrySet()) {
            this.renderBlock(entry.getKey(), entry.getValue());
        }
    }

    private static int pickSlot(ItemStack held, List<Map.Entry<IBlockState, Integer>> sorted) {
        Minecraft mc = mc();
        if (sorted.isEmpty() && held == null) {
            for (int i = 0; i < 9; ++i) {
                if (mc.thePlayer.inventory.getStackInSlot(i) == null) continue;
                return i;
            }
            return -1;
        }
        if (autoSwap == AutoSwap.Hover && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = BlockUtils.placedPos(mc.objectMouseOver);
            IBlockState expected = build != null ? build.get(pos) : null;
            if (expected != null) {
                for (int n = 0; n < 9; ++n) {
                    if (!matches(mc.thePlayer.inventory.getStackInSlot(n), expected)) continue;
                    return n;
                }
            }
        }
        if (lastState == 4 && held != null) {
            for (Map.Entry<IBlockState, Integer> entry : sorted) {
                if (!matches(held, entry.getKey())) continue;
                return -1;
            }
        }
        if (lastItem != null && held == null) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (!ItemUtils.sameItem(lastItem, stack) || lastItem.getItemDamage() != stack.getItemDamage()) continue;
                return i;
            }
        }
        for (Map.Entry<IBlockState, Integer> entry : sorted) {
            for (int n = 0; n < 9; ++n) {
                if (!matches(mc.thePlayer.inventory.getStackInSlot(n), entry.getKey())) continue;
                return n;
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            this.onDisable();
        }
    }

    @Override
    public void onPreMotion(MotionEvent ev) {
        Minecraft mc = mc();
        try {
            ++ticks;
            state = Hypixel.buildBattle();
            if (state == 0) {
                return;
            }
            if (state == 1 || state == 2 || state == 5) {
                if (build != null) {
                    build = null;
                }
                if (waiting != null) {
                    waiting = null;
                }
            }
            switch (state) {
                case 1: {
                    status = "Queueing";
                    break;
                }
                case 2: {
                    status = "Waiting";
                    if (ticks % 2 != 0 || center != null) break;
                    findCenter();
                    break;
                }
                case 3: {
                    List<String> lines;
                    status = "Showing";
                    if (ticks - judgedAt < 10 || ticks % 5 != 0) break;
                    if (center == null) {
                        findCenter();
                        if (center == null) break;
                    }
                    if (build == null) {
                        build = new HashMap<BlockPos, IBlockState>();
                    }
                    if ((lines = Hypixel.sidebar()) != null && lines.size() > 6) {
                        for (String s : lines) {
                            if (s.startsWith("Theme: ")) {
                                theme = Chat.strip(s).replace("Theme: ", "");
                                continue;
                            }
                            if (!s.startsWith("Difficulty: ")) continue;
                            themeKey = theme + "-" + Chat.strip(s).replace("Difficulty: ", "");
                            break;
                        }
                    }
                    for (int i = 1; i <= 13; ++i) {
                        for (int j = -3; j <= 3; ++j) {
                            for (int k = -3; k <= 3; ++k) {
                                BlockPos pos = new BlockPos(center.getX() + j, center.getY() + i, center.getZ() + k);
                                IBlockState s = build.get(pos);
                                if (s != null && s.getBlock() != Blocks.air) continue;
                                build.put(pos, BlockUtils.getState(pos));
                            }
                        }
                    }
                    break;
                }
                case 4: {
                    status = "Building";
                    if (lastState == 3) {
                        timeTicks = 1;
                    } else if (timeTicks > 0) {
                        ++timeTicks;
                    }
                    if (build == null) break;
                    if (waiting == null) {
                        waiting = new ArrayList<BlockPos>();
                        lastItem = null;
                    }
                    highlights = new HashMap<BlockPos, Integer>();
                    missing = new HashMap<IBlockState, Integer>();
                    ItemStack held = mc.thePlayer.getHeldItem();
                    for (Map.Entry<BlockPos, IBlockState> entry : build.entrySet()) {
                        IBlockState current = BlockUtils.getState(entry.getKey());
                        IBlockState wanted = entry.getValue();
                        Block now = current.getBlock();
                        Block block = wanted.getBlock();
                        if (block == Blocks.air) {
                            if (now == block) continue;
                            highlights.put(entry.getKey(), 3);
                            continue;
                        }
                        boolean halfOfDouble = now instanceof BlockSlab && !now.isFullBlock() && block instanceof BlockSlab && block.isFullBlock()
                                && now == BlockUtils.singleSlab(block) && now.getMetaFromState(current) == block.getMetaFromState(wanted);
                        if (now == Blocks.air || halfOfDouble) {
                            boolean placeable = canPlaceAt(entry.getKey(), wanted);
                            if (placeable) {
                                missing.merge(wanted, 1, Integer::sum);
                            }
                            if (renderBlocks == RenderBlocks.All) {
                                highlights.put(entry.getKey(), placeable ? 1 : 2);
                                continue;
                            }
                            if (renderBlocks != RenderBlocks.Holding && (!placeable || renderBlocks != RenderBlocks.Placeable)) continue;
                            if (held == null) {
                                highlights.put(entry.getKey(), 0);
                                continue;
                            }
                            if (!matches(held, wanted)) continue;
                            highlights.put(entry.getKey(), placeable ? 1 : 2);
                            continue;
                        }
                        if (now != block) {
                            highlights.put(entry.getKey(), 3);
                            continue;
                        }
                        if (now.getMetaFromState(current) == block.getMetaFromState(wanted) || block == Blocks.leaves || block == Blocks.double_plant
                                || block instanceof BlockRailBase) continue;
                        highlights.put(entry.getKey(), 4);
                        if (strictPlacements.isToggled()) continue;
                        boolean lenient = false;
                        if (block == Blocks.lever || block instanceof BlockButton || block instanceof BlockDaylightDetector || block == Blocks.flowing_lava) {
                            lenient = true;
                        } else if (block instanceof BlockStairs) {
                            BlockStairs.EnumHalf h1 = current.getValue(BlockStairs.HALF);
                            BlockStairs.EnumHalf h2 = wanted.getValue(BlockStairs.HALF);
                            EnumFacing f1;
                            EnumFacing f2;
                            if (h1 == h2 && ((f1 = current.getValue(BlockStairs.FACING)) == (f2 = wanted.getValue(BlockStairs.FACING)) || f1 == f2.rotateY()
                                    || f1 == f2.rotateYCCW())) {
                                lenient = true;
                            }
                        } else if (block instanceof BlockDoor && wanted.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER) {
                            lenient = true;
                        }
                        if (!lenient) continue;
                        highlights.remove(entry.getKey());
                    }
                    ArrayList<Map.Entry<IBlockState, Integer>> sorted = new ArrayList<Map.Entry<IBlockState, Integer>>(missing.entrySet());
                    sorted.sort(SpeedBuildersModule::compareMissing);
                    if (auto) {
                        MovingObjectPosition hit;
                        if (autoSwap != AutoSwap.Disabled) {
                            int n = pickSlot(held, sorted);
                            if (n >= 0) {
                                PlayerUtils.setSlot(n, false);
                                mc.thePlayer.inventory.currentItem = n;
                                lastItem = held;
                                held = mc.thePlayer.getHeldItem();
                            } else {
                                lastItem = held;
                            }
                        }
                        if (PacketUtils.noUseOrDig() && ticks - lastPlaceTick >= placeWait && autoPlace == AutoPlace.Hover && (hit = mc.objectMouseOver) != null
                                && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            BlockPos pos = BlockUtils.placedPos(hit);
                            IBlockState expected = build.get(pos);
                            boolean fullSlab = expected != null && expected.getBlock() instanceof BlockSlab && expected.getBlock().isFullBlock();
                            if (fullSlab) {
                                Chat.print("&efull slab expected");
                            }
                            if (BlockUtils.isPassable(pos) && !BlockUtils.opensOnClick(hit.getBlockPos())) {
                                IBlockState target = build.get(pos);
                                if (fullSlab) {
                                    Chat.print("&7 - can place at and no interact");
                                }
                                if (target != null && matches(held, target) && !ItemUtils.is(held, Items.water_bucket) && !ItemUtils.is(held, Items.lava_bucket)) {
                                    if (fullSlab) {
                                        Chat.print("&7 - stack target");
                                    }
                                    boolean canPlace = !(held.getItem() instanceof ItemBlock) || BlockUtils.canPlace(hit, held);
                                    if (waiting.contains(pos)) {
                                        if (canPlace) {
                                            waiting.remove(pos);
                                        }
                                    } else if (!canPlace) {
                                        waiting.add(pos);
                                    } else if (placesAs(target, hit, held)
                                            && mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, hit.getBlockPos(), hit.sideHit, hit.hitVec)) {
                                        if (fullSlab) {
                                            Chat.print("&7 - &aplaced");
                                        }
                                        PlayerUtils.swing(false, true);
                                        afterPlace();
                                    } else if (fullSlab) {
                                        Chat.print("&7 - &cplace failed &7" + placesAs(target, hit, held));
                                    }
                                } else if (fullSlab) {
                                    Chat.print("&7 - &cokay &7" + matches(held, target));
                                }
                            } else if (fullSlab) {
                                Chat.print("&7 - &cbruh &7" + BlockUtils.isPassable(pos) + " " + !BlockUtils.opensOnClick(hit.getBlockPos()));
                            }
                        }
                    }
                    int n = 0;
                    for (int i = 0; i < 9; ++i) {
                        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                        if (stack == null) continue;
                        n += stack.stackSize;
                    }
                    if (lastState == 3) {
                        totalBlocks = n;
                        break;
                    }
                    blocksLeft = n;
                    break;
                }
                case 5: {
                    status = "Judging";
                    judgedAt = ticks;
                }
            }
            lastState = state;
        } catch (Exception ignored) {
        }
    }

    private static Comparable<?> axisFor(MovingObjectPosition hit, IProperty<?> prop) {
        Class<?> c = prop.getValueClass();
        if (c == EnumFacing.Axis.class) {
            return hit.sideHit.getAxis();
        }
        if (c == BlockLog.EnumAxis.class) {
            if (hit.sideHit == EnumFacing.UP || hit.sideHit == EnumFacing.DOWN) {
                return BlockLog.EnumAxis.Y;
            }
            EnumFacing f = mc().thePlayer.getHorizontalFacing();
            switch (facingCase(f)) {
                case 1:
                case 2:
                    return BlockLog.EnumAxis.Z;
                case 3:
                case 4:
                    return BlockLog.EnumAxis.X;
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (state == 0 || e.button == -1) {
            return;
        }
        int n = (int) activation.getInput();
        if (n == 0) {
            if (e.button == 2 && e.buttonstate) {
                auto = !auto;
            }
        } else if (n == 1 && e.button == 1 && (auto = e.buttonstate) && (state != 4 || missing != null && !missing.isEmpty())) {
            e.setCanceled(true);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState with(IBlockState s, IProperty prop, Object value) {
        if (prop.getValueClass().isInstance(value)) {
            Comparable c = (Comparable) prop.getValueClass().cast(value);
            return s.withProperty(prop, c);
        }
        return s;
    }

    private static boolean canPlaceAt(BlockPos pos, IBlockState s) {
        try {
            EnumSet<EnumFacing> sides = EnumSet.allOf(EnumFacing.class);
            Block block = s.getBlock();
            if (block instanceof BlockSlab) {
                if (block.isFullBlock()) {
                    Block there = BlockUtils.getBlock(pos);
                    if (there instanceof BlockSlab && !there.isFullBlock()) {
                        return true;
                    }
                } else {
                    BlockSlab.EnumBlockHalf half = s.getValue(BlockSlab.HALF);
                    sides.remove(half == BlockSlab.EnumBlockHalf.TOP ? EnumFacing.DOWN : EnumFacing.UP);
                }
            } else if (block instanceof BlockStairs) {
                BlockStairs.EnumHalf half = s.getValue(BlockStairs.HALF);
                if (strictPlacements.isToggled()) {
                    sides.clear();
                    sides.add(half == BlockStairs.EnumHalf.TOP ? EnumFacing.UP : EnumFacing.DOWN);
                    sides.add(s.getValue(BlockStairs.FACING));
                } else {
                    sides.remove(half == BlockStairs.EnumHalf.TOP ? EnumFacing.DOWN : EnumFacing.UP);
                }
            } else if (block instanceof BlockTorch) {
                EnumFacing f = s.getValue(BlockTorch.FACING);
                sides.clear();
                sides.add(f.getOpposite());
            } else if (block instanceof BlockButton) {
                if (strictPlacements.isToggled()) {
                    EnumFacing f = s.getValue(BlockButton.FACING);
                    sides.clear();
                    sides.add(f.getOpposite());
                }
            } else if (block instanceof BlockTrapDoor) {
                EnumFacing f = s.getValue(BlockTrapDoor.FACING);
                sides.clear();
                sides.add(f.getOpposite());
            } else if (block instanceof BlockRailBase) {
                sides.clear();
                sides.add(EnumFacing.DOWN);
            }
            for (EnumFacing f : sides) {
                IBlockState other;
                BlockPos p = pos.offset(f);
                if (BlockUtils.isReplaceable(p)) continue;
                if (block instanceof BlockSlab && !block.isFullBlock() && (other = BlockUtils.getState(p)).getBlock() instanceof BlockSlab && !other.getBlock().isFullBlock()) {
                    BlockSlab.EnumBlockHalf h = other.getValue(BlockSlab.HALF);
                    BlockSlab.EnumBlockHalf mine = s.getValue(BlockSlab.HALF);
                    if (!(f == EnumFacing.UP ? h == BlockSlab.EnumBlockHalf.BOTTOM : (f == EnumFacing.DOWN ? h == BlockSlab.EnumBlockHalf.TOP : h == mine))) continue;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static Comparable<?> halfFor(MovingObjectPosition hit, IProperty<?> prop) {
        float f = (float) (hit.hitVec.yCoord - (double) hit.getBlockPos().getY());
        if (prop.getValueClass() == BlockStairs.EnumHalf.class) {
            if (hit.sideHit == EnumFacing.UP) {
                return BlockStairs.EnumHalf.BOTTOM;
            }
            if (hit.sideHit == EnumFacing.DOWN) {
                return BlockStairs.EnumHalf.TOP;
            }
            return (double) f > 0.57 ? BlockStairs.EnumHalf.TOP : BlockStairs.EnumHalf.BOTTOM;
        }
        if (prop.getValueClass() == BlockSlab.EnumBlockHalf.class) {
            if (hit.sideHit == EnumFacing.UP) {
                IBlockState s;
                if (BlockUtils.getBlock(hit.getBlockPos()) == Blocks.carpet && (s = BlockUtils.getState(BlockUtils.placedPos(hit))).getBlock() instanceof BlockSlab
                        && !s.getBlock().isFullBlock()) {
                    BlockSlab.EnumBlockHalf h = s.getValue(BlockSlab.HALF);
                    return h == BlockSlab.EnumBlockHalf.BOTTOM ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM;
                }
                return (double) f == 0.5 ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM;
            }
            if (hit.sideHit == EnumFacing.DOWN) {
                return (double) f == 0.5 ? BlockSlab.EnumBlockHalf.BOTTOM : BlockSlab.EnumBlockHalf.TOP;
            }
            return (double) f > 0.5 ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM;
        }
        if (prop.getValueClass() == BlockTrapDoor.DoorHalf.class) {
            return (double) f > 0.5 ? BlockTrapDoor.DoorHalf.TOP : BlockTrapDoor.DoorHalf.BOTTOM;
        }
        return null;
    }

    private static int compareMissing(Map.Entry<IBlockState, Integer> a, Map.Entry<IBlockState, Integer> b) {
        boolean la = a.getKey().getBlock() instanceof BlockLiquid;
        boolean lb = b.getKey().getBlock() instanceof BlockLiquid;
        if (la && !lb) {
            return 1;
        }
        if (!la && lb) {
            return -1;
        }
        int n = Integer.compare(b.getValue(), a.getValue());
        if (n != 0) {
            return n;
        }
        return a.getKey().toString().compareTo(b.getKey().toString());
    }

    private void renderBlock(BlockPos pos, int kind) {
        Minecraft mc = mc();
        IBlockState s;
        if (build == null || (s = build.get(pos)) == null) {
            return;
        }
        AxisAlignedBB bb;
        Block block = s.getBlock();
        double d = (double) pos.getX() - mc.getRenderManager().viewerPosX;
        double d2 = (double) pos.getY() - mc.getRenderManager().viewerPosY;
        double d3 = (double) pos.getZ() - mc.getRenderManager().viewerPosZ;
        if (block instanceof BlockStairs) {
            double d4 = 0.5;
            double d5 = 1.0 - d4;
            EnumFacing f = s.getValue(BlockStairs.FACING);
            boolean bottom = s.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.BOTTOM;
            switch (facingCase(f)) {
                case 1:
                    bb = bottom ? new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 0.5, d3 + d4) : new AxisAlignedBB(d, d2 + 0.5, d3, d + 1.0, d2 + 1.0, d3 + d4);
                    break;
                case 2:
                    bb = bottom ? new AxisAlignedBB(d, d2, d3 + d5, d + 1.0, d2 + 0.5, d3 + 1.0) : new AxisAlignedBB(d, d2 + 0.5, d3 + d5, d + 1.0, d2 + 1.0, d3 + 1.0);
                    break;
                case 3:
                    bb = bottom ? new AxisAlignedBB(d, d2, d3, d + d4, d2 + 0.5, d3 + 1.0) : new AxisAlignedBB(d, d2 + 0.5, d3, d + d4, d2 + 1.0, d3 + 1.0);
                    break;
                case 4:
                    bb = bottom ? new AxisAlignedBB(d + d5, d2, d3, d + 1.0, d2 + 0.5, d3 + 1.0) : new AxisAlignedBB(d + d5, d2 + 0.5, d3, d + 1.0, d2 + 1.0, d3 + 1.0);
                    break;
                default:
                    bb = new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0);
                    break;
            }
        } else if (block instanceof BlockSlab) {
            bb = block.isFullBlock() ? new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0)
                    : (s.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM
                    ? new AxisAlignedBB(d + block.getBlockBoundsMinX(), d2, d3 + block.getBlockBoundsMinZ(), d + block.getBlockBoundsMaxX(), d2 + 0.5, d3 + block.getBlockBoundsMaxZ())
                    : new AxisAlignedBB(d + block.getBlockBoundsMinX(), d2 + 0.5, d3 + block.getBlockBoundsMinZ(), d + block.getBlockBoundsMaxX(), d2 + 1.0, d3 + block.getBlockBoundsMaxZ()));
        } else if (block instanceof BlockButton) {
            EnumFacing f = s.getValue(BlockButton.FACING);
            boolean powered = s.getValue(BlockButton.POWERED);
            float t = (float) (powered ? 1 : 2) / 16.0f;
            switch (facingCase(f)) {
                case 4:
                    bb = new AxisAlignedBB(d, d2 + 0.375, d3 + 0.3125, d + (double) t, d2 + 0.625, d3 + 0.6875);
                    break;
                case 3:
                    bb = new AxisAlignedBB(d + 1.0 - (double) t, d2 + 0.375, d3 + 0.3125, d + 1.0, d2 + 0.625, d3 + 0.6875);
                    break;
                case 2:
                    bb = new AxisAlignedBB(d + 0.3125, d2 + 0.375, d3, d + 0.6875, d2 + 0.625, d3 + (double) t);
                    break;
                case 1:
                    bb = new AxisAlignedBB(d + 0.3125, d2 + 0.375, d3 + 1.0 - (double) t, d + 0.6875, d2 + 0.625, d3 + 1.0);
                    break;
                case 5:
                    bb = new AxisAlignedBB(d + 0.3125, d2, d3 + 0.375, d + 0.6875, d2 + (double) t, d3 + 0.625);
                    break;
                default:
                    bb = new AxisAlignedBB(d + 0.3125, d2 + 1.0 - (double) t, d3 + 0.375, d + 0.6875, d2 + 1.0, d3 + 0.625);
                    break;
            }
        } else if (block instanceof BlockTrapDoor) {
            EnumFacing f = s.getValue(BlockTrapDoor.FACING);
            boolean open = s.getValue(BlockTrapDoor.OPEN);
            BlockTrapDoor.DoorHalf half = s.getValue(BlockTrapDoor.HALF);
            double x1 = d;
            double y1 = d2;
            double z1 = d3;
            double x2 = d + 1.0;
            double y2 = d2 + 1.0;
            double z2 = d3 + 1.0;
            double thick = 0.1875;
            double t = open ? thick : 0.5;
            switch (facingCase(f)) {
                case 1:
                    z1 = d3 + 1.0 - t;
                    z2 = d3 + 1.0;
                    break;
                case 2:
                    z1 = d3;
                    z2 = d3 + t;
                    break;
                case 3:
                    x1 = d + 1.0 - t;
                    x2 = d + 1.0;
                    break;
                case 4:
                    x1 = d;
                    x2 = d + t;
            }
            if (half == BlockTrapDoor.DoorHalf.BOTTOM) {
                y2 = open ? d2 + 0.5 : d2 + thick;
            } else {
                y1 = open ? d2 + 0.5 : d2 + 1.0 - thick;
            }
            bb = new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
        } else if (block instanceof BlockFenceGate) {
            EnumFacing f = s.getValue(BlockFenceGate.FACING);
            double x1 = d;
            double y1 = d2 + 0.3125;
            double z1 = d3;
            double x2 = d + 1.0;
            double y2 = d2 + 1.0;
            double z2 = d3 + 1.0;
            double w = 0.4375;
            switch (facingCase(f)) {
                case 1:
                case 2:
                    z1 = d3 + w;
                    z2 = d3 + 1.0 - w;
                    break;
                case 3:
                case 4:
                    x1 = d + w;
                    x2 = d + 1.0 - w;
            }
            bb = new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
        } else if (block instanceof BlockFence) {
            double w = 0.125;
            bb = new AxisAlignedBB(d + 0.5 - w, d2, d3 + 0.5 - w, d + 0.5 + w, d2 + 1.0, d3 + 0.5 + w);
        } else if (block == Blocks.lever) {
            BlockLever.EnumOrientation o = s.getValue(BlockLever.FACING);
            double a = 0.1875;
            double b = 1.0 - a;
            double c = 0.3125;
            double e = 0.6875;
            double g = 0.25;
            double h = 0.75;
            bb = o == BlockLever.EnumOrientation.UP_Z ? new AxisAlignedBB(d + c, d2, d3 + g, d + e, d2 + a, d3 + h)
                    : (o == BlockLever.EnumOrientation.UP_X ? new AxisAlignedBB(d + g, d2, d3 + c, d + h, d2 + a, d3 + e)
                    : (o == BlockLever.EnumOrientation.DOWN_Z ? new AxisAlignedBB(d + c, d2 + b, d3 + g, d + e, d2 + 1.0, d3 + h)
                    : (o == BlockLever.EnumOrientation.DOWN_X ? new AxisAlignedBB(d + g, d2 + b, d3 + c, d + h, d2 + 1.0, d3 + e)
                    : (o == BlockLever.EnumOrientation.NORTH ? new AxisAlignedBB(d + c, d2 + g, d3 + (1.0 - a), d + e, d2 + h, d3 + 1.0)
                    : (o == BlockLever.EnumOrientation.SOUTH ? new AxisAlignedBB(d + c, d2 + g, d3, d + e, d2 + h, d3 + a)
                    : (o == BlockLever.EnumOrientation.EAST ? new AxisAlignedBB(d, d2 + g, d3 + c, d + a, d2 + h, d3 + e)
                    : (o == BlockLever.EnumOrientation.WEST ? new AxisAlignedBB(d + (1.0 - a), d2 + g, d3 + c, d + 1.0, d2 + h, d3 + e)
                    : new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0))))))));
        } else if (block instanceof BlockTripWireHook) {
            EnumFacing f = s.getValue(BlockTripWireHook.FACING);
            bb = f == EnumFacing.EAST ? new AxisAlignedBB(d, d2 + 0.064, d3 + 0.375, d + 0.124, d2 + 0.56125, d3 + 0.625)
                    : (f == EnumFacing.WEST ? new AxisAlignedBB(d + 0.876, d2 + 0.064, d3 + 0.375, d + 1.0, d2 + 0.56125, d3 + 0.625)
                    : (f == EnumFacing.SOUTH ? new AxisAlignedBB(d + 0.375, d2 + 0.064, d3, d + 0.625, d2 + 0.56125, d3 + 0.124)
                    : (f == EnumFacing.NORTH ? new AxisAlignedBB(d + 0.375, d2 + 0.064, d3 + 0.876, d + 0.625, d2 + 0.56125, d3 + 1.0)
                    : new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0))));
        } else if (block instanceof BlockDoor) {
            double t = 0.1875;
            EnumFacing f = s.getValue(BlockDoor.FACING);
            boolean open = s.getValue(BlockDoor.OPEN);
            bb = f == EnumFacing.NORTH ? (open ? new AxisAlignedBB(d, d2, d3, d + t, d2 + 1.0, d3 + 1.0) : new AxisAlignedBB(d, d2, d3 + 1.0 - t, d + 1.0, d2 + 1.0, d3 + 1.0))
                    : (f == EnumFacing.SOUTH ? (open ? new AxisAlignedBB(d + 1.0 - t, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0) : new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + t))
                    : (f == EnumFacing.EAST ? (open ? new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + t) : new AxisAlignedBB(d, d2, d3, d + t, d2 + 1.0, d3 + 1.0))
                    : (f == EnumFacing.WEST ? (open ? new AxisAlignedBB(d, d2, d3 + 1.0 - t, d + 1.0, d2 + 1.0, d3 + 1.0) : new AxisAlignedBB(d + 1.0 - t, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0))
                    : new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0))));
        } else if (block == Blocks.hay_block || block instanceof BlockLog) {
            String axis = block == Blocks.hay_block ? s.getValue(BlockHay.AXIS).name() : s.getValue(BlockLog.LOG_AXIS).name();
            double w = 0.25;
            switch (axis) {
                case "X":
                    bb = new AxisAlignedBB(d, d2 + 0.5 - w, d3 + 0.5 - w, d + 1.0, d2 + 0.5 + w, d3 + 0.5 + w);
                    break;
                case "Y":
                    bb = new AxisAlignedBB(d + 0.5 - w, d2, d3 + 0.5 - w, d + 0.5 + w, d2 + 1.0, d3 + 0.5 + w);
                    break;
                default:
                    bb = new AxisAlignedBB(d + 0.5 - w, d2 + 0.5 - w, d3, d + 0.5 + w, d2 + 0.5 + w, d3 + 1.0);
                    break;
            }
        } else if (block == Blocks.ladder) {
            EnumFacing f = s.getValue(BlockLadder.FACING);
            double t = 0.125;
            switch (facingCase(f)) {
                case 1:
                    bb = new AxisAlignedBB(d, d2, d3 + 1.0 - t, d + 1.0, d2 + 1.0, d3 + 1.0);
                    break;
                case 2:
                    bb = new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + t);
                    break;
                case 3:
                    bb = new AxisAlignedBB(d + 1.0 - t, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0);
                    break;
                default:
                    bb = new AxisAlignedBB(d, d2, d3, d + t, d2 + 1.0, d3 + 1.0);
                    break;
            }
        } else if (block == Blocks.wall_sign) {
            EnumFacing f = s.getValue(BlockWallSign.FACING);
            double t = 0.125;
            double y1 = d2 + 0.25;
            double y2 = d2 + 0.8;
            switch (facingCase(f)) {
                case 1:
                    bb = new AxisAlignedBB(d, y1, d3 + 1.0 - t, d + 1.0, y2, d3 + 1.0);
                    break;
                case 2:
                    bb = new AxisAlignedBB(d, y1, d3, d + 1.0, y2, d3 + t);
                    break;
                case 3:
                    bb = new AxisAlignedBB(d + 1.0 - t, y1, d3, d + 1.0, y2, d3 + 1.0);
                    break;
                default:
                    bb = new AxisAlignedBB(d, y1, d3, d + t, y2, d3 + 1.0);
                    break;
            }
        } else if (block == Blocks.end_portal_frame) {
            EnumFacing f = s.getValue(BlockEndPortalFrame.FACING);
            double w = 0.25;
            double h = 0.8125;
            switch (facingCase(f)) {
                case 1:
                case 2:
                    bb = new AxisAlignedBB(d + 0.5 - w, d2 + 0.5 - w, d3, d + 0.5 + w, d2 + h, d3 + 1.0);
                    break;
                default:
                    bb = new AxisAlignedBB(d, d2, d3 + 0.5 - w, d + 1.0, d2 + h, d3 + 0.5 + w);
                    break;
            }
        } else {
            bb = block instanceof BlockPane ? new AxisAlignedBB(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0)
                    : new AxisAlignedBB(d + block.getBlockBoundsMinX(), d2 + block.getBlockBoundsMinY(), d3 + block.getBlockBoundsMinZ(),
                    d + block.getBlockBoundsMaxX(), d2 + block.getBlockBoundsMaxY(), d3 + block.getBlockBoundsMaxZ());
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        int color;
        float alpha;
        if (kind == 0) {
            color = Theme.GOLD.getRGB(0.0);
            alpha = 0.23f;
        } else if (kind == 1) {
            color = Theme.VINE.first.getRGB();
            alpha = 0.28f;
        } else if (kind == 2) {
            color = Theme.VINE.second.getRGB();
            alpha = 0.23f;
        } else if (kind == 3) {
            color = Color.red.getRGB();
            alpha = 0.36f;
        } else {
            color = Color.orange.getRGB();
            alpha = 0.36f;
        }
        float r = (float) (color >> 16 & 0xFF) / 255.0f;
        float g = (float) (color >> 8 & 0xFF) / 255.0f;
        float b = (float) (color & 0xFF) / 255.0f;
        Render3D.filledBox(bb, r, g, b, alpha);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GlStateManager.disableBlend();
    }

    @Override
    public void guiUpdate() {
        autoPlace = AutoPlace.values()[(int) autoPlaceSetting.getInput()];
        autoSwap = AutoSwap.values()[(int) autoSwapSetting.getInput()];
        renderBlocks = RenderBlocks.values()[(int) renderBlocksSetting.getInput()];
        showStats = ShowStats.values()[(int) showStatsSetting.getInput()];
        if (activation.getInput() == 1.0) {
            auto = false;
        }
    }

    public static void render() {
        Minecraft mc = mc();
        if (state == 0) {
            return;
        }
        if (infoHud.isToggled()) {
            InfoHud.add("&6Speed Builders");
            InfoHud.add("&7Status: &b" + status);
            if (state >= 3) {
                if (themeKey != null) {
                    Double d = bestTimes.get(themeKey);
                    InfoHud.add("&7Theme: &b" + theme + (d != null ? " &7~ &b" + SliderSetting.formatNumber(d) + "s" : ""));
                }
                if (state >= 4) {
                    InfoHud.add("&7Progress: &b" + (totalBlocks - blocksLeft) + "&7/&b" + totalBlocks + " "
                            + SliderSetting.formatNumber(Strings.round((double) (totalBlocks - blocksLeft) / (double) totalBlocks * 100.0, 0)) + "%");
                }
            }
            if (autoPlace != AutoPlace.Disabled || autoSwap != AutoSwap.Disabled) {
                InfoHud.add("&7Auto: " + (auto ? "&aENABLED" : "&cDISABLED"));
            }
        }
        if (showTime.isToggled() && Overlay.canRender()) {
            int alpha;
            long now = System.currentTimeMillis();
            if (state == 4 && timeTicks > 0) {
                timeFadeAt = -1L;
                alpha = 240;
            } else {
                if (timeFadeAt == -1L) {
                    timeFadeAt = now;
                }
                long l = now - timeFadeAt;
                alpha = l < 125L ? (int) (240.0f - (float) l / 125.0f * 240.0f) : 0;
            }
            if (alpha > 0) {
                String s = "§b" + Strings.round((double) Math.abs(timeTicks) / 20.0, 1) + "s";
                int x = RenderUtils.scaledWidth / 2 - mc.fontRendererObj.getStringWidth(s) / 2;
                int y = RenderUtils.scaledHeight / 2 + 12;
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                mc.fontRendererObj.drawString(s, (float) x, (float) y, RenderUtils.withAlpha(-1, RenderUtils.clampAlpha(alpha)), true);
                GlStateManager.disableBlend();
            }
        }
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (state == 4 && s.startsWith(PlayerUtils.username())) {
            double d;
            timeTicks = -timeTicks;
            Module longJump = ModuleManager.get("Long Jump");
            try {
                d = Double.parseDouble(s.split(" ")[6].replace("s!", ""));
            } catch (Exception ex) {
                Chat.moduleMessage(longJump, "&cError parsing time!");
                return;
            }
            if (themeKey == null) {
                Chat.moduleMessage(longJump, "&cError parsing theme!");
                return;
            }
            Double best = bestTimes.get(themeKey);
            if (saveStats.isToggled() && (best == null || d < best)) {
                bestTimes.put(themeKey, d);
                saveTimes();
            }
            if (showStats != ShowStats.Disabled) {
                String line;
                e.setCanceled(true);
                if (best == null) {
                    line = "&7first attempt: &b" + SliderSetting.formatNumber(d) + "s";
                } else {
                    line = "&7best: &b" + SliderSetting.formatNumber(best) + "s &7(" + (d < best ? "&a-" + SliderSetting.formatNumber(Strings.round(best - d, 1)) + "s &6PB&7)"
                            : "&c+" + SliderSetting.formatNumber(Strings.round(d - best, 1)) + "s&7)");
                }
                String blocks = "&7blocks: &b" + totalBlocks + " &7~ &b" + SliderSetting.formatNumber(Strings.round((double) totalBlocks / d, 1)) + " &7bps";
                if (showStats == ShowStats.Chat) {
                    Chat.raw(e.message.getFormattedText());
                    Chat.print(line);
                    Chat.print(blocks);
                } else if (showStats == ShowStats.Hover) {
                    ChatStyle style = new ChatStyle();
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(Chat.color("&7theme: &b" + theme) + "\n" + Chat.color(line) + "\n" + Chat.color(blocks))));
                    mc().thePlayer.addChatMessage(e.message.setChatStyle(style));
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes"})
    private static boolean placesAs(IBlockState wanted, MovingObjectPosition hit, ItemStack held) {
        Minecraft mc = mc();
        Block block = wanted.getBlock();
        if (block == Blocks.double_plant || block instanceof BlockRailBase) {
            return true;
        }
        if (!strictPlacements.isToggled() && (block == Blocks.lever || block instanceof BlockButton)) {
            return true;
        }
        IBlockState result = block.getDefaultState();
        Collection<IProperty> props = result.getPropertyNames();
        for (IProperty prop : props) {
            String name = prop.getName();
            int which = -1;
            switch (name) {
                case "facing":
                    which = 0;
                    break;
                case "axis":
                    which = 1;
                    break;
                case "half":
                    which = 2;
                    break;
                case "variant":
                    which = 3;
                    break;
            }
            switch (which) {
                case 0: {
                    EnumFacing facing = null;
                    if (!(block instanceof BlockStairs)) {
                        boolean piston = block == Blocks.dispenser || block == Blocks.dropper || block instanceof BlockPistonBase;
                        if (piston) {
                            facing = BlockPistonBase.getFacingFromEntity(mc.theWorld, hit.getBlockPos().offset(hit.sideHit), mc.thePlayer);
                        } else if (block instanceof BlockTorch) {
                            if (hit.sideHit != EnumFacing.DOWN) {
                                facing = hit.sideHit;
                            } else {
                                BlockPos at = hit.getBlockPos().offset(hit.sideHit);
                                for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                                    if (mc.theWorld.isSideSolid(at.offset(f.getOpposite()), f, true)) {
                                        facing = f;
                                        at = null;
                                        break;
                                    }
                                }
                                if (at != null) {
                                    return false;
                                }
                            }
                        } else if (block == Blocks.skull) {
                            if (hit.sideHit == EnumFacing.DOWN) {
                                return false;
                            }
                            if (hit.sideHit == EnumFacing.UP && wanted.getValue(BlockSkull.FACING) == EnumFacing.UP) {
                                float yaw = (mc.thePlayer.rotationYaw % 360.0f + 360.0f) % 360.0f;
                                boolean straight = false;
                                for (float a : new float[]{0.0f, 90.0f, 180.0f, 270.0f}) {
                                    float diff = Math.abs(yaw - a);
                                    diff = Math.min(diff, 360.0f - diff);
                                    if (diff <= 8.0f) {
                                        straight = true;
                                        break;
                                    }
                                }
                                if (!straight) {
                                    return false;
                                }
                            }
                            facing = hit.sideHit;
                        } else if (block instanceof BlockButton) {
                            facing = hit.sideHit;
                        } else if (block == Blocks.tripwire_hook || block == Blocks.wall_sign) {
                            if (hit.sideHit == EnumFacing.UP || hit.sideHit == EnumFacing.DOWN) {
                                return false;
                            }
                            facing = hit.sideHit;
                        } else if (block == Blocks.hopper) {
                            if (hit.sideHit != EnumFacing.DOWN) {
                                facing = hit.sideHit.getOpposite();
                            }
                        } else if (block instanceof BlockFenceGate) {
                            facing = mc.thePlayer.getHorizontalFacing();
                        } else if (block == Blocks.lever) {
                            EnumFacing player = mc.thePlayer.getHorizontalFacing();
                            BlockLever.EnumOrientation o;
                            switch (facingCase(hit.sideHit)) {
                                case 1:
                                    o = BlockLever.EnumOrientation.NORTH;
                                    break;
                                case 2:
                                    o = BlockLever.EnumOrientation.SOUTH;
                                    break;
                                case 4:
                                    o = BlockLever.EnumOrientation.EAST;
                                    break;
                                case 5:
                                    o = player.getAxis() != EnumFacing.Axis.X ? BlockLever.EnumOrientation.UP_Z : BlockLever.EnumOrientation.UP_X;
                                    break;
                                case 6:
                                    o = player.getAxis() != EnumFacing.Axis.X ? BlockLever.EnumOrientation.DOWN_Z : BlockLever.EnumOrientation.DOWN_X;
                                    break;
                                case 3:
                                default:
                                    o = BlockLever.EnumOrientation.WEST;
                                    break;
                            }
                            result = with(result, BlockLever.FACING, o);
                            break;
                        } else {
                            facing = mc.thePlayer.getHorizontalFacing().getOpposite();
                        }
                    } else {
                        facing = mc.thePlayer.getHorizontalFacing();
                        if (!strictPlacements.isToggled()) {
                            EnumFacing f = wanted.getValue(BlockStairs.FACING);
                            if (f == facing || f == facing.rotateY() || f == facing.rotateYCCW()) {
                                facing = f;
                            }
                        }
                    }
                    if (facing != null) {
                        result = with(result, prop, facing);
                    }
                    break;
                }
                case 1: {
                    Comparable<?> c = axisFor(hit, prop);
                    if (c != null) {
                        result = with(result, prop, c);
                    }
                    break;
                }
                case 2: {
                    Comparable<?> c = halfFor(hit, prop);
                    if (c != null) {
                        result = with(result, prop, c);
                    }
                    break;
                }
                case 3: {
                    result = with(result, prop, variantFor(hit, prop, wanted));
                    break;
                }
                default:
                    result = with(result, prop, wanted.getValue(prop));
            }
        }
        return result.equals(wanted);
    }

    private static void afterPlace() {
        PlayerUtils.clearEmptyStacks();
        lastPlaceTick = ticks;
        placeWait = (int) Math.floor(placeDelay.getInput()) + 1;
        if (placeDelay.getInput() % 1.0 == 0.5) {
            if (halfTick) {
                ++placeWait;
            }
            halfTick = !halfTick;
        }
    }

    public static void loadTimes() {
        bestTimes.clear();
        if (FILE.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(FILE))) {
                String line;
                bestTimes.clear();
                while ((line = r.readLine()) != null) {
                    String[] parts = line.split(": ");
                    if (parts.length != 2) continue;
                    try {
                        double d = Double.parseDouble(parts[1]);
                        if (!(d >= 0.0)) continue;
                        bestTimes.put(parts[0], d);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void onDisable() {
        timeTicks = 0;
        placeWait = 0;
        lastPlaceTick = 0;
        judgedAt = 0;
        lastState = 0;
        state = 0;
        ticks = 0;
        timeFadeAt = 0;
        status = null;
        lastItem = null;
        center = null;
        build = null;
        missing = null;
        highlights = null;
        waiting = null;
    }

    private static void saveTimes() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE))) {
            for (Map.Entry<String, Double> entry : bestTimes.entrySet()) {
                w.write(entry.getKey() + ": " + entry.getValue());
                w.newLine();
            }
        } catch (IOException e) {
            Chat.moduleMessage(ModuleManager.get("Long Jump"), "&cError saving scores to file!");
        }
    }

    @Override
    public void onEnable() {
        auto = activation.getInput() == 0.0;
    }

    private static boolean matches(ItemStack stack, IBlockState s) {
        try {
            ItemBlock itemBlock = ItemUtils.isBlock(stack) ? (ItemBlock) stack.getItem() : null;
            Block block = s.getBlock();
            if (itemBlock == null) {
                if (stack != null) {
                    if (block == Blocks.bed) {
                        return ItemUtils.is(stack, Items.bed);
                    }
                    if (block == Blocks.brewing_stand) {
                        return ItemUtils.is(stack, Items.brewing_stand);
                    }
                    if (block == Blocks.cauldron) {
                        return ItemUtils.is(stack, Items.cauldron);
                    }
                    if (block == Blocks.flower_pot) {
                        return ItemUtils.is(stack, Items.flower_pot);
                    }
                    if (block == Blocks.skull) {
                        return ItemUtils.is(stack, Items.skull);
                    }
                    if (block == Blocks.flowing_water || block == Blocks.water) {
                        return ItemUtils.is(stack, Items.water_bucket);
                    }
                    if (block == Blocks.lava || block == Blocks.flowing_lava) {
                        return ItemUtils.is(stack, Items.lava_bucket);
                    }
                    if (block instanceof BlockSign) {
                        return ItemUtils.is(stack, Items.sign);
                    }
                    if (block instanceof BlockDoor) {
                        return stack.getItem() instanceof ItemDoor;
                    }
                }
                return false;
            }
            Block placed = itemBlock.getBlock();
            if (!(placed == block || placed instanceof BlockSlab && block instanceof BlockSlab)) {
                return false;
            }
            boolean byMeta = block == Blocks.wool || block == Blocks.stained_hardened_clay || block == Blocks.planks || block == Blocks.stained_glass
                    || block == Blocks.stained_glass_pane || block == Blocks.stone || block instanceof BlockSlab;
            if (!byMeta) {
                return true;
            }
            int n = stack.getItemDamage();
            int n2 = block.getMetaFromState(s);
            if (block instanceof BlockSlab) {
                n &= 7;
                n2 &= 7;
            }
            return n == n2;
        } catch (Exception e) {
            return false;
        }
    }

    private static Comparable<?> variantFor(MovingObjectPosition hit, IProperty<?> prop, IBlockState s) {
        BlockQuartz.EnumType type;
        if (s.getBlock() == Blocks.quartz_block && (type = s.getValue(BlockQuartz.VARIANT)) != BlockQuartz.EnumType.DEFAULT && type != BlockQuartz.EnumType.CHISELED) {
            switch (facingCase(hit.sideHit)) {
                case 1:
                case 2:
                    return BlockQuartz.EnumType.LINES_Z;
                case 3:
                case 4:
                    return BlockQuartz.EnumType.LINES_X;
                case 5:
                case 6:
                    return BlockQuartz.EnumType.LINES_Y;
            }
        }
        return s.getValue(prop);
    }
}
