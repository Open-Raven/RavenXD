package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Render3D;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class XrayModule extends Module {
    public enum Ore {
        COAL("Coal", -16777216, Blocks.coal_ore, Blocks.coal_block),
        DIAMOND("Diamond", -16711681, Blocks.diamond_ore, Blocks.diamond_block),
        EMERALD("Emerald", -16711936, Blocks.emerald_ore, Blocks.emerald_block),
        GOLD("Gold", -256, Blocks.gold_ore, Blocks.gold_block),
        IRON("Iron", -1, Blocks.iron_ore, Blocks.iron_block),
        LAPIS("Lapis", -16776961, Blocks.lapis_ore, Blocks.lapis_block),
        OBSIDIAN("Obsidian", -7273744, Blocks.obsidian, null),
        REDSTONE("Redstone", -65536, Blocks.redstone_ore, Blocks.redstone_block),
        SPAWNER("Spawner", -43213, Blocks.mob_spawner, null);

        public final String label;
        public final int color;
        private final Block a;
        private final Block b;
        public ButtonSetting setting;

        Ore(String label, int color, Block a, Block b) {
            this.label = label;
            this.color = color;
            this.a = a;
            this.b = b;
        }

        public boolean isBlock(Block block) {
            return block == this.a || this.b != null && block == this.b;
        }
    }

    public static SliderSetting range, rate;
    private double lastRate;
    private double lastRange;
    private long interval;
    private double rangeSq;
    private Timer timer;
    private final List<BlockPos> found = Collections.synchronizedList(new ArrayList<BlockPos>());

    public XrayModule(int key) {
        super("Xray", Category.render, key);
    }

    public void link() {
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        rate = (SliderSetting) ModuleManager.setting(this, "Rate");
        for (Ore o : Ore.values()) {
            o.setting = (ButtonSetting) ModuleManager.setting(this, o.label);
        }
    }

    private boolean stale(BlockPos pos) {
        if (!this.inRange(pos)) {
            return true;
        }
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block == null || !this.wanted(block);
    }

    @Override
    public void onEnable() {
        long period = (long) (rate.getMin() * 1000.0);
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new Scan(), 0L, period);
    }

    @Override
    public void onUpdate() {
        if (this.found.isEmpty()) {
            return;
        }
        synchronized (this.found) {
            this.found.removeIf(this::stale);
        }
    }

    @Override
    public void guiUpdate() {
        if (this.lastRange != range.getInput()) {
            this.lastRange = range.getInput();
            this.rangeSq = this.lastRange * this.lastRange + 2.0;
        }
        if (this.lastRate != rate.getInput()) {
            this.lastRate = rate.getInput();
            this.interval = (long) (this.lastRate * 1000.0 - 10.0);
        }
    }

    private int color(Block block) {
        for (Ore o : Ore.values()) {
            if (!o.isBlock(block)) continue;
            return o.color;
        }
        return -1;
    }

    private boolean inRange(BlockPos pos) {
        return mc.thePlayer.getDistanceSq(pos) <= this.rangeSq;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.found.isEmpty() || !Chat.inWorld()) {
            return;
        }
        synchronized (this.found) {
            for (BlockPos pos : this.found) {
                Block block = mc.theWorld.getBlockState(pos).getBlock();
                Render3D.block(pos, this.color(block), false, true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
        this.found.clear();
    }

    private void scan() {
        int n = (int) range.getInput();
        outer:
        for (int i = n; i >= -n; --i) {
            for (int j = -n; j <= n; ++j) {
                next:
                for (int k = -n; k <= n; ++k) {
                    if (!Chat.inWorld()) break outer;
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + (double) j, mc.thePlayer.posY + (double) i, mc.thePlayer.posZ + (double) k);
                    if (!this.inRange(pos)) continue;
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (block == null || !this.wanted(block)) continue;
                    synchronized (this.found) {
                        for (BlockPos p : this.found) {
                            if (pos == p || pos.getX() == p.getX() && pos.getY() == p.getY() && pos.getZ() == p.getZ()) continue next;
                        }
                        this.found.add(pos);
                    }
                }
            }
        }
    }

    private boolean wanted(Block block) {
        for (Ore o : Ore.values()) {
            if (!o.isBlock(block)) continue;
            return o.setting.isToggled();
        }
        return false;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            this.found.clear();
        }
    }

    private final class Scan extends TimerTask {
        private long last;

        @Override
        public void run() {
            if (!Chat.inWorld()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (Math.abs(now - this.last) < XrayModule.this.interval) {
                return;
            }
            XrayModule.this.scan();
            this.last = now;
        }
    }
}
