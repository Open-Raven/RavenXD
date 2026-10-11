package xyz.daikirai.module.minigames;

import xyz.daikirai.util.PlayerUtils;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.DescriptionSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SumoFencesModule extends Module {
    public static SliderSetting fenceHeight, blockType;
    public static DescriptionSetting modeDesc;
    private final List<String> maps = Arrays.asList("Sumo", "Space Mine", "White Crystal");
    private static final String SUMO_MODE = "Mode: Sumo Duel";
    private static final List<BlockPos> FENCE = Arrays.asList(new BlockPos(9, 65, -2), new BlockPos(9, 65, -1), new BlockPos(9, 65, 0),
            new BlockPos(9, 65, 1), new BlockPos(9, 65, 2), new BlockPos(9, 65, 3), new BlockPos(8, 65, 3), new BlockPos(8, 65, 4),
            new BlockPos(8, 65, 5), new BlockPos(7, 65, 5), new BlockPos(7, 65, 6), new BlockPos(7, 65, 7), new BlockPos(6, 65, 7),
            new BlockPos(5, 65, 7), new BlockPos(5, 65, 8), new BlockPos(4, 65, 8), new BlockPos(3, 65, 8), new BlockPos(3, 65, 9),
            new BlockPos(2, 65, 9), new BlockPos(1, 65, 9), new BlockPos(0, 65, 9), new BlockPos(-1, 65, 9), new BlockPos(-2, 65, 9),
            new BlockPos(-3, 65, 9), new BlockPos(-3, 65, 8), new BlockPos(-4, 65, 8), new BlockPos(-5, 65, 8), new BlockPos(-5, 65, 7),
            new BlockPos(-6, 65, 7), new BlockPos(-7, 65, 7), new BlockPos(-7, 65, 6), new BlockPos(-7, 65, 5), new BlockPos(-8, 65, 5),
            new BlockPos(-8, 65, 4), new BlockPos(-8, 65, 3), new BlockPos(-9, 65, 3), new BlockPos(-9, 65, 2), new BlockPos(-9, 65, 1),
            new BlockPos(-9, 65, 0), new BlockPos(-9, 65, -1), new BlockPos(-9, 65, -2), new BlockPos(-9, 65, -3), new BlockPos(-8, 65, -3),
            new BlockPos(-8, 65, -4), new BlockPos(-8, 65, -5), new BlockPos(-7, 65, -5), new BlockPos(-7, 65, -6), new BlockPos(-7, 65, -7),
            new BlockPos(-6, 65, -7), new BlockPos(-5, 65, -7), new BlockPos(-5, 65, -8), new BlockPos(-4, 65, -8), new BlockPos(-3, 65, -8),
            new BlockPos(-3, 65, -9), new BlockPos(-2, 65, -9), new BlockPos(-1, 65, -9), new BlockPos(0, 65, -9), new BlockPos(1, 65, -9),
            new BlockPos(2, 65, -9), new BlockPos(3, 65, -9), new BlockPos(3, 65, -8), new BlockPos(4, 65, -8), new BlockPos(5, 65, -8),
            new BlockPos(5, 65, -7), new BlockPos(6, 65, -7), new BlockPos(7, 65, -7), new BlockPos(7, 65, -6), new BlockPos(7, 65, -5),
            new BlockPos(8, 65, -5), new BlockPos(8, 65, -4), new BlockPos(8, 65, -3), new BlockPos(9, 65, -3));
    private IBlockState state = Blocks.oak_fence.getDefaultState();
    private Timer timer;

    public SumoFencesModule(int key) {
        super("Sumo Fences", Category.minigames, key);
    }

    public void link() {
        fenceHeight = (SliderSetting) ModuleManager.setting(this, "Fence height");
        blockType = (SliderSetting) ModuleManager.setting(this, "Block type");
        modeDesc = (DescriptionSetting) ModuleManager.setting(this, "Mode: Oak fence");
    }

    public static List<String> scoreLines() {
        Minecraft mc = Minecraft.getMinecraft();
        ArrayList<String> out = new ArrayList<>();
        if (mc.theWorld == null) {
            return out;
        }
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) {
            return out;
        }
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) {
            return out;
        }
        Collection<Score> all = sb.getSortedScores(obj);
        ArrayList<Score> filtered = new ArrayList<>();
        for (Score s : all) {
            if (s == null || s.getPlayerName() == null || s.getPlayerName().startsWith("#")) continue;
            filtered.add(s);
        }
        Collection<Score> shown = filtered.size() > 15 ? Lists.newArrayList(Iterables.skip(filtered, all.size() - 15)) : filtered;
        for (Score s : shown) {
            ScorePlayerTeam team = sb.getPlayersTeam(s.getPlayerName());
            out.add(ScorePlayerTeam.formatPlayerName(team, s.getPlayerName()));
        }
        return out;
    }

    private boolean inSumo() {
        if (Hypixel.onHypixel()) {
            for (String line : scoreLines()) {
                String s = Chat.strip(line);
                if (s.startsWith("Map:")) {
                    if (!this.maps.contains(s.substring(5))) continue;
                    return true;
                }
                if (!s.equals(SUMO_MODE)) continue;
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop;
        if (e.buttonstate && (e.button == 0 || e.button == 1) && Chat.inWorld() && this.inSumo() && (mop = mc.objectMouseOver) != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int x = mop.getBlockPos().getX();
            int z = mop.getBlockPos().getZ();
            for (BlockPos p : FENCE) {
                if (p.getX() != x || p.getZ() != z) continue;
                e.setCanceled(true);
                if (e.button == 0) {
                    PlayerUtils.restartSwing();
                }
                Mouse.poll();
                break;
            }
        }
    }

    @Override
    public void onEnable() {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                if (!SumoFencesModule.this.inSumo()) {
                    return;
                }
                outer:
                for (BlockPos p : FENCE) {
                    for (int n = 0; (double) n < fenceHeight.getInput(); ++n) {
                        if (mc.theWorld == null) break outer;
                        BlockPos q = new BlockPos(p.getX(), p.getY() + n, p.getZ());
                        if (mc.theWorld.getBlockState(q).getBlock() == Blocks.air) {
                            mc.theWorld.setBlockState(q, SumoFencesModule.this.state);
                        }
                    }
                }
            }
        }, 0L, 500L);
    }

    @Override
    public void guiUpdate() {
        switch ((int) blockType.getInput()) {
            case 1:
                this.state = Blocks.oak_fence.getDefaultState();
                modeDesc.setDesc("Mode: Oak fence");
                break;
            case 2:
                this.state = Blocks.leaves.getDefaultState();
                modeDesc.setDesc("Mode: Leaves");
                break;
            case 3:
                this.state = Blocks.glass.getDefaultState();
                modeDesc.setDesc("Mode: Glass");
                break;
            case 4:
                this.state = Blocks.barrier.getDefaultState();
                modeDesc.setDesc("Mode: Barrier");
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
        for (BlockPos p : FENCE) {
            for (int n = 0; (double) n < fenceHeight.getInput(); ++n) {
                BlockPos q = new BlockPos(p.getX(), p.getY() + n, p.getZ());
                if ((Object) mc.theWorld.getBlockState(q).getBlock() == this.state) {
                    mc.theWorld.setBlockState(q, Blocks.air.getDefaultState());
                }
            }
        }
    }
}
