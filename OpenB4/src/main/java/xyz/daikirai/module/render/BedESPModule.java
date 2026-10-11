package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Theme;
import xyz.daikirai.util.Zoom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class BedESPModule extends Module {
    public static SliderSetting themeSetting, range, rate, coveringBlocks;
    public static ButtonSetting disableInLobby, drawBeam, onlyRenderFirstBed, optimizeForBedWars, renderFullBlock, showToolsOnly,
            showWhitelistedCovering;

    public static Theme theme = Theme.DEFAULT;
    public static boolean active;
    private static boolean inBedWars;
    private static long interval;
    private static double lastRate;
    private static double rangeSq;
    private static BlockPos[] first;
    private static final List<BlockPos[]> beds = Collections.synchronizedList(new ArrayList<BlockPos[]>());
    private static final Map<BlockPos[], AxisAlignedBB> boxes = new ConcurrentHashMap<BlockPos[], AxisAlignedBB>();
    private static final Map<BlockPos[], List<ItemStack>> covering = new ConcurrentHashMap<BlockPos[], List<ItemStack>>();
    private static final Set<BlockPos[]> exposed = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos[], Long> broken = new ConcurrentHashMap<BlockPos[], Long>();
    private static final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
    private Timer timer;

    public BedESPModule(int key) {
        super("BedESP", Category.render, key);
    }

    public void link() {
        themeSetting = (SliderSetting) ModuleManager.setting(this, "Theme");
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        rate = (SliderSetting) ModuleManager.setting(this, "Rate");
        disableInLobby = (ButtonSetting) ModuleManager.setting(this, "Disable in lobby");
        drawBeam = (ButtonSetting) ModuleManager.setting(this, "Draw beam");
        onlyRenderFirstBed = (ButtonSetting) ModuleManager.setting(this, "Only render first bed");
        optimizeForBedWars = (ButtonSetting) ModuleManager.setting(this, "Optimize for Bed Wars");
        renderFullBlock = (ButtonSetting) ModuleManager.setting(this, "Render full block");
        coveringBlocks = (SliderSetting) ModuleManager.setting(this, "Covering blocks");
        showToolsOnly = (ButtonSetting) ModuleManager.setting(this, "Show tools only");
        showWhitelistedCovering = (ButtonSetting) ModuleManager.setting(this, "Show whitelisted covering");
    }

    private static void render(BlockPos[] bed, float fade, float pt) {
        AxisAlignedBB bb = boxes.get(bed);
        if (bb == null) {
            return;
        }
        boolean own = xyz.daikirai.module.minigames.BedWarsModule.isOwnBed(bed[0]);
        if (drawBeam.isToggled() && !own) {
            BlockPos a = bed[0];
            BlockPos b = bed[1];
            double x = (double) (a.getX() + b.getX()) * 0.5;
            double z = (double) (a.getZ() + b.getZ()) * 0.5;
            double y = a.getY();
            double h = 75.0;
            double w = 0.1;
            if (Render3D.isVisible(x + 0.5 - w, y, z + 0.5 - w, x + 0.5 + w, y + h, z + 0.5 + w)) {
                Render3D.beam(x, y, z, h, w, Theme.FROST.getRGB(0.0), (fade != -1.0f ? fade : 1.0f) * 0.3f);
            }
        }
        if (!Render3D.isVisible(bb)) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        bb = bb.offset(-rm.viewerPosX, -rm.viewerPosY, -rm.viewerPosZ);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0f);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        int n = !own ? theme.getRGB(0.0) : Theme.VINE.getRGB(0.0);
        float alpha = fade != -1.0f ? fade : 0.42f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float bl = (float) (n & 0xFF) / 255.0f;
        Render3D.filledBox(bb, r, g, bl, alpha);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        if (coveringBlocks.isNotDefault() && (!own || showWhitelistedCovering.isToggled())) {
            renderCovering(bed, fade, pt);
        }
    }

    private boolean isBed(BlockPos[] bed) {
        return mc.theWorld.getBlockState(bed[0]).getBlock() == Blocks.bed;
    }

    @Override
    public void onEnable() {
        long period = (long) (rate.getMin() * 1000.0);
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new Scan(), 0L, period);
    }

    private static void renderCovering(BlockPos[] bed, float fade, float pt) {
        List<ItemStack> list = covering.get(bed);
        if (list == null || list.isEmpty()) {
            return;
        }
        double sc = coveringBlocks.getInput();
        if (mc.currentScreen == null && Zoom.isZooming()) {
            sc *= 2.0;
        }
        if (fade != -1.0f) {
            double k = 1.3;
            double t = (double) fade / 0.95;
            if (!(t > 0.55)) {
                sc = k * Math.exp(-5.0 * (1.0 - t));
            } else {
                sc *= k * (1.0 + 0.3 * Math.sin((t - 0.55) * Math.PI / 0.3));
            }
        }
        BlockPos a = bed[0];
        BlockPos b = bed[1];
        double x = (double) (a.getX() + b.getX()) * 0.5 + 0.5;
        double y = a.getY() + 1;
        double z = (double) (a.getZ() + b.getZ()) * 0.5 + 0.5;
        double dx = Render3D.cameraPos[0] - x;
        double dy = Render3D.cameraPos[1] - y;
        double dz = Render3D.cameraPos[2] - z;
        double d = dx * dx + dy * dy + dz * dz;
        if (d < 81.0) {
            sc = Math.max(sc * (9.0 / Math.sqrt(d)), sc);
        }
        Vec3 v = Render3D.project(x, y, z, RenderUtils.scaleFactor, -1.0f);
        if (v != null && v.zCoord < 1.0003684) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(sc, sc, sc);
            float off = (float) (-8 * list.size());
            float ix = (float) (v.xCoord / sc + (double) off);
            float iy = (float) ((v.yCoord - 8.0) / sc);
            GL11.glDepthFunc(519);
            int border = exposed.contains(bed) && !broken.containsKey(bed) ? -8327298 : 0x5A000000;
            RenderUtils.roundedRectBorder(ix - 1.5f, iy - 1.5f, ix + (float) (16 * list.size()) + 1.5f, iy + 16.0f + 1.5f, 8.0f, 0x5A000000, border, border);
            for (ItemStack s : list) {
                RenderUtils.drawItem(s, ix, iy);
                ix += 16.0f;
            }
            GL11.glDepthFunc(515);
            GlStateManager.popMatrix();
        }
        Render3D.setupCamera(pt);
    }

    private static void clear() {
        first = null;
        beds.clear();
        boxes.clear();
        covering.clear();
        exposed.clear();
        broken.clear();
    }

    private void scan() {
        try {
            int py;
            int n2 = (int) range.getInput();
            int n3 = n2;
            int px = MathHelper.floor_double(mc.thePlayer.posX);
            int pz = MathHelper.floor_double(mc.thePlayer.posZ);
            if (optimizeForBedWars.isToggled() && inBedWars && xyz.daikirai.module.minigames.BedWarsModule.ownBed != null) {
                py = xyz.daikirai.module.minigames.BedWarsModule.ownBed.getY();
                n2 = (int) range.getMax();
                n3 = 4;
            } else {
                py = MathHelper.floor_double(mc.thePlayer.posY);
            }
            rangeSq = n2 * n2 + 2;
            for (int i = n3; i >= -n3; --i) {
                for (int j = -n2; j <= n2; ++j) {
                    next:
                    for (int k = -n2; k <= n2; ++k) {
                        if (!Chat.inWorld()) return;
                        scanPos.set(px + j, py + i, pz + k);
                        IBlockState state = mc.theWorld.getBlockState(scanPos);
                        if (state.getBlock() != Blocks.bed || state.getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT) continue;
                        BlockPos pos;
                        BlockPos[] bed;
                        if (onlyRenderFirstBed.isToggled()) {
                            if (first != null && samePos(scanPos, first[0])) {
                                return;
                            }
                            pos = new BlockPos(scanPos);
                            bed = new BlockPos[]{pos, pos.offset(state.getValue(BlockBed.FACING))};
                            if (!this.inRange(bed)) continue;
                            first = bed;
                            this.box(bed);
                            return;
                        }
                        pos = new BlockPos(scanPos);
                        bed = new BlockPos[]{pos, pos.offset(state.getValue(BlockBed.FACING))};
                        if (!this.inRange(bed)) continue;
                        synchronized (beds) {
                            for (BlockPos[] other : beds) {
                                if (samePos(scanPos, other[0])) continue next;
                            }
                            beds.add(bed);
                        }
                        this.box(bed);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean samePos(BlockPos a, BlockPos b) {
        return a == b || a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    @Override
    public void onDisable() {
        active = false;
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
        clear();
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            clear();
        }
    }

    public static void render(float pt) {
        if (!onlyRenderFirstBed.isToggled()) {
            if (!beds.isEmpty()) {
                synchronized (beds) {
                    for (BlockPos[] bed : beds) {
                        render(bed, -1.0f, pt);
                    }
                }
            }
        } else if (first != null) {
            render(first, -1.0f, pt);
        }
        if (!broken.isEmpty()) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<BlockPos[], Long>> it = broken.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos[], Long> e = it.next();
                long age = now - Math.abs(e.getValue());
                float t = (float) age / (float) 250L;
                if (!(t >= 1.0f)) {
                    float fade = (float) (0.95 - Math.min((double) t * 0.95, 0.95));
                    render(e.getKey(), fade, pt);
                } else {
                    it.remove();
                    boxes.remove(e.getKey());
                    covering.remove(e.getKey());
                    exposed.remove(e.getKey());
                }
            }
        }
    }

    private void box(BlockPos[] bed) {
        AxisAlignedBB bb;
        double x = bed[0].getX();
        double y = bed[0].getY();
        double z = bed[0].getZ();
        double h = renderFullBlock.isToggled() ? 1.0 : (double) 0.5625f;
        if (bed[0].getX() != bed[1].getX()) {
            bb = bed[0].getX() > bed[1].getX() ? new AxisAlignedBB(x - 1.0, y, z, x + 1.0, y + h, z + 1.0) : new AxisAlignedBB(x, y, z, x + 2.0, y + h, z + 1.0);
        } else if (bed[0].getZ() > bed[1].getZ()) {
            bb = new AxisAlignedBB(x, y, z - 1.0, x + 1.0, y + h, z + 1.0);
        } else {
            bb = new AxisAlignedBB(x, y, z, x + 1.0, y + h, z + 2.0);
        }
        boxes.put(bed, bb);
    }

    private void updateCovering(BlockPos[] bed) {
        BlockPos pos = bed[0].up();
        IBlockState state = mc.theWorld.getBlockState(pos);
        boolean open;
        if (state.getBlock() != Blocks.air) {
            open = BlockUtils.exposed(bed[0]) || BlockUtils.exposed(bed[1]);
        } else {
            pos = bed[1].up();
            state = mc.theWorld.getBlockState(pos);
            open = true;
        }
        if (!open) {
            exposed.remove(bed);
        } else {
            exposed.add(bed);
        }
        if (state.getBlock() == Blocks.air) {
            covering.remove(bed);
            return;
        }
        HashSet<Block> seen = new HashSet<Block>();
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (int i = 0; i < 4; ++i) {
            Block block;
            if (i != 0) {
                pos = pos.up();
                state = mc.theWorld.getBlockState(pos);
                block = state.getBlock();
                if (block == Blocks.air) {
                    break;
                }
                if (seen.contains(block)) {
                    continue;
                }
            } else {
                block = state.getBlock();
            }
            ItemStack stack = null;
            if (!showToolsOnly.isToggled()) {
                stack = BlockUtils.itemOf(state);
            } else {
                Item tool = null;
                if (block == Blocks.wool) {
                    tool = Items.shears;
                } else if (block == Blocks.planks || block == Blocks.log || block == Blocks.log2) {
                    tool = Items.iron_axe;
                } else if (block == Blocks.end_stone || block == Blocks.hardened_clay || block == Blocks.stained_hardened_clay) {
                    tool = Items.iron_pickaxe;
                } else if (block == Blocks.obsidian) {
                    tool = Items.diamond_pickaxe;
                }
                if (tool != null) {
                    stack = new ItemStack(tool);
                }
            }
            if (stack != null) {
                seen.add(block);
                items.add(0, stack);
            }
        }
        covering.put(bed, items);
    }

    @Override
    public void onUpdate() {
        inBedWars = Hypixel.state == Hypixel.State.BEDWARS_GAME_BED || Hypixel.state == Hypixel.State.BEDWARS_GAME_NO_BED;
        active = inBedWars || !Hypixel.onHypixel() || !disableInLobby.isToggled()
                || Hypixel.state != Hypixel.State.LOBBY && Hypixel.state != Hypixel.State.BRIDGE_PRACTICE;
        if (first != null) {
            if (this.isBed(first)) {
                if (this.inRange(first)) {
                    if (coveringBlocks.isNotDefault()) {
                        this.updateCovering(first);
                    }
                } else {
                    boxes.remove(first);
                    covering.remove(first);
                    exposed.remove(first);
                    first = null;
                }
            } else {
                broken.put(first, System.currentTimeMillis());
                first = null;
            }
        }
        if (!beds.isEmpty()) {
            for (int i = 0; i < beds.size(); ++i) {
                BlockPos[] bed = beds.get(i);
                if (this.isBed(bed)) {
                    if (this.inRange(bed)) {
                        if (coveringBlocks.isNotDefault()) {
                            this.updateCovering(beds.get(i));
                        }
                    } else {
                        boxes.remove(bed);
                        covering.remove(bed);
                        exposed.remove(bed);
                        beds.remove(i--);
                    }
                } else {
                    broken.put(bed, System.currentTimeMillis());
                    beds.remove(i--);
                }
            }
        }
    }

    private boolean inRange(BlockPos[] bed) {
        double x = mc.thePlayer.posX - 0.5;
        double y = mc.thePlayer.posY - 0.5;
        double z = mc.thePlayer.posZ - 0.5;
        double dx = x - (double) bed[0].getX();
        double dy = y - (double) bed[0].getY();
        double dz = z - (double) bed[0].getZ();
        double d = dx * dx + dy * dy + dz * dz;
        if (d <= rangeSq) {
            return true;
        }
        if (d > rangeSq + 25.0) {
            return false;
        }
        dx = x - (double) bed[1].getX();
        dy = y - (double) bed[1].getY();
        dz = z - (double) bed[1].getZ();
        return dx * dx + dy * dy + dz * dz <= rangeSq;
    }

    @Override
    public void guiUpdate() {
        if (lastRate != rate.getInput()) {
            lastRate = rate.getInput();
            interval = (long) (lastRate * 1000.0 - 10.0);
        }
        theme = Theme.values()[(int) themeSetting.getInput()];
    }

    private final class Scan extends TimerTask {
        private long last;

        @Override
        public void run() {
            if (!Chat.inWorld()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (Math.abs(now - this.last) >= interval) {
                BedESPModule.this.scan();
                this.last = now;
            }
        }
    }
}
