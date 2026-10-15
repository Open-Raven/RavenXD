package xyz.daikirai.module.other;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.client.Friends;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.script.ScriptEvents;
import xyz.daikirai.script.ScriptManager;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Strings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class AntiCheatModule extends Module {
    public static SliderSetting flagInterval;
    public static ButtonSetting addCheatersAsEnemy, autoReport, disableOutsideMinigames, hideChatAlerts, ignoreTeammates, sendPartyChat, shouldPing;

    public enum Check {
        AutoBlock("AutoBlock", false), LegitScaffold("Legit Scaffold", false), NoSlow("NoSlow", false), Nuker("Nuker", false),
        BedNuker("Bed Nuker", false), Scaffold("Scaffold", false), AimAssist("AimAssist", true), AutoClicker("AutoClicker", true),
        Blink("Blink", true), FastMine("FastMine", true), NoFall("NoFall", true), Protocol("Protocol", true);

        public final String displayName;
        public final boolean experimental;
        ButtonSetting button;

        Check(String displayName, boolean experimental) {
            this.displayName = displayName;
            this.experimental = experimental;
        }

        public boolean enabled() {
            return this.button.isToggled();
        }
    }

    static class Tracker {
        Block belowBlock;
        int quickSwings, lastPitchTick, lastSwing, lastMoveTick, yawTicks, restTicks, sprintTicks, lastRestTicks, lastSprintTicks, lastStopUsingTick,
                lastFallingTicks, lastDeltaYTick, stillTicks, lastBreakAnim = -1, lastSneakTick, swingBlockTicks, useSprintTicks, lastHorizontalMoveTick,
                perfectSneaks, fallingTicks, scaffoldTicks, horizontalTicks;
        boolean wasUsing, wasSneaking;
        double maxHorizontal;
        final double[] motionX = new double[1], motionY = new double[1], motionZ = new double[1];
        Vec3 fallStart;

        void update(EntityPlayer p) {
            int n = p.ticksExisted;
            this.updateMotion(p);
            below.set(MathHelper.floor_double(p.posX), MathHelper.floor_double(p.posY - 1.0), MathHelper.floor_double(p.posZ));
            this.belowBlock = BlockUtils.getBlock(below);
            this.maxHorizontal = Math.max(Math.abs(this.motionX[0]), Math.abs(this.motionZ[0]));
            if (this.maxHorizontal == 0.0 && this.motionY[0] == 0.0) {
                ++this.restTicks;
            } else {
                this.restTicks = 0;
                this.lastMoveTick = n;
            }
            if (this.maxHorizontal >= 0.07) {
                ++this.horizontalTicks;
                this.lastHorizontalMoveTick = n;
            } else {
                this.horizontalTicks = 0;
            }
            if (Math.abs(this.motionY[0]) >= 0.1) {
                this.lastDeltaYTick = n;
            }
            this.fallingTicks = this.motionY[0] < 0.0 && this.belowBlock == Blocks.air ? ++this.fallingTicks : 0;
            if (p.isSneaking()) {
                this.lastSneakTick = n;
            }
            if (p.rotationPitch != p.prevRotationPitch) {
                this.lastPitchTick = n;
            }
            if (!p.isUsingItem() && this.wasUsing) {
                this.lastStopUsingTick = n;
            }
            this.yawTicks = Math.abs(p.rotationYaw - p.prevRotationYaw) >= 1.0f ? ++this.yawTicks : 0;
            this.swingBlockTicks = p.isSwingInProgress && p.isBlocking() ? ++this.swingBlockTicks : 0;
            this.sprintTicks = p.isSprinting() ? ++this.sprintTicks : 0;
            this.useSprintTicks = p.isSprinting() && p.isUsingItem() && !p.isRiding() && !p.isInWater() ? ++this.useSprintTicks : 0;
            if (p.isSwingInProgress && p.rotationPitch >= 65.0f && ItemUtils.isBlock(p.getHeldItem())) {
                this.scaffoldTicks = this.horizontalTicks >= 15 ? ++this.scaffoldTicks : 0;
                if (p.swingProgressInt == 1) {
                    this.perfectSneaks = !this.wasSneaking && p.isSneaking() ? ++this.perfectSneaks : 0;
                }
            } else {
                this.perfectSneaks = 0;
                this.scaffoldTicks = 0;
            }
            if (p.isSwingInProgress && (this.lastBreakAnim == -1 || this.lastBreakAnim == 255)) {
                if (p.swingProgressInt == 0) {
                    this.quickSwings = this.lastSwing == 3 ? ++this.quickSwings : 0;
                }
            } else {
                this.quickSwings = 0;
            }
            this.stillTicks = !p.isSneaking() && !p.isSwingInProgress && this.maxHorizontal == 0.0 && this.motionY[0] == 0.0
                    && p.rotationYaw == p.prevRotationYaw && p.rotationPitch == p.prevRotationPitch ? ++this.stillTicks : 0;
        }

        void endTick(EntityPlayer p) {
            this.lastRestTicks = this.restTicks;
            this.lastSprintTicks = this.sprintTicks;
            this.lastFallingTicks = this.fallingTicks;
            this.wasSneaking = p.isSneaking();
            this.lastSwing = p.swingProgressInt;
            this.wasUsing = p.isUsingItem();
        }

        void onBreakAnim(EntityPlayer p, S25PacketBlockBreakAnim anim) {
            this.lastBreakAnim = anim.getProgress();
            if (Check.FastMine.enabled()) {
                S25PacketBlockBreakAnim previous = breakingPlayers.get(p.getUniqueID());
                if (previous != null && anim.getProgress() == 255) {
                    breakingBlocks.put(anim.getPosition(), previous);
                }
                breakingPlayers.put(p.getUniqueID(), anim);
            }
        }

        void updateMotion(EntityPlayer p) {
            this.motionX[0] = p.posX - p.lastTickPosX;
            this.motionY[0] = p.posY - p.lastTickPosY;
            this.motionZ[0] = p.posZ - p.lastTickPosZ;
        }
    }

    private static final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
    private static final HashMap<UUID, Tracker> trackers = new HashMap<UUID, Tracker>();
    private static final HashMap<UUID, HashMap<Check, Long>> lastFlags = new HashMap<UUID, HashMap<Check, Long>>();
    private static final HashMap<UUID, S25PacketBlockBreakAnim> breakingPlayers = new HashMap<UUID, S25PacketBlockBreakAnim>();
    private static final HashMap<BlockPos, S25PacketBlockBreakAnim> breakingBlocks = new HashMap<BlockPos, S25PacketBlockBreakAnim>();
    private static final HashSet<String> reported = new HashSet<String>();
    private static String partyMessage, reportName;
    private static long lastPing;

    public AntiCheatModule(int key) {
        super("AntiCheat", Category.other, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        flagInterval = (SliderSetting) ModuleManager.setting(this, "Flag interval");
        addCheatersAsEnemy = (ButtonSetting) ModuleManager.setting(this, "Add cheaters as enemy");
        autoReport = (ButtonSetting) ModuleManager.setting(this, "Auto report");
        disableOutsideMinigames = (ButtonSetting) ModuleManager.setting(this, "Disable outside minigames");
        hideChatAlerts = (ButtonSetting) ModuleManager.setting(this, "Hide chat alerts");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        sendPartyChat = (ButtonSetting) ModuleManager.setting(this, "Send party chat");
        shouldPing = (ButtonSetting) ModuleManager.setting(this, "Should ping");
        for (Check c : Check.values()) {
            if (c != Check.BedNuker) {
                c.button = (ButtonSetting) ModuleManager.setting(this, c.displayName);
            }
        }
    }

    private static void check(EntityPlayer p, Tracker t) {
        Minecraft mc = mc();
        ItemStack held;
        int n = p.ticksExisted;
        boolean adventure = PlayerUtils.isAdventure();
        if (Check.AimAssist.enabled() && p.rotationPitch > -80.0f && p.rotationPitch < 80.0f && t.yawTicks >= 25 && n - t.lastPitchTick >= 25) {
            flag(p, Check.AimAssist, "&b" + t.yawTicks + " " + (n - t.lastPitchTick));
            return;
        }
        if (Check.AutoClicker.enabled() && !adventure && t.quickSwings >= 10 && mc.thePlayer.getDistanceSqToEntity(p) < 700.0
                && ((held = p.getHeldItem()) == null || held.getItem() != Items.fishing_rod)) {
            flag(p, Check.AutoClicker, "&b" + t.quickSwings);
            return;
        }
        if (Check.AutoBlock.enabled() && t.swingBlockTicks >= 15) {
            flag(p, Check.AutoBlock, "&b" + t.swingBlockTicks);
            return;
        }
        if (Check.Blink.enabled() && !adventure && t.lastSprintTicks >= 2 && t.lastRestTicks >= 6 && t.maxHorizontal > 0.5 && t.maxHorizontal < 3.0
                && System.currentTimeMillis() - LatencyAlertsModule.lastPacket < 100L) {
            flag(p, Check.Blink, "&b" + t.lastSprintTicks + " " + t.lastRestTicks + " " + t.maxHorizontal);
            return;
        }
        if (Check.LegitScaffold.enabled() && t.perfectSneaks >= 3) {
            flag(p, Check.LegitScaffold, "&b" + t.perfectSneaks);
            return;
        }
        if (Check.NoFall.enabled()) {
            if (t.stillTicks == 10 && t.belowBlock == Blocks.air) {
                t.fallStart = new Vec3(p.posX, p.posY, p.posZ);
            }
            if (t.fallStart != null) {
                if (p.hurtTime != 0) {
                    t.fallStart = null;
                } else if (t.lastFallingTicks > 0 && t.lastFallingTicks < 6 && t.belowBlock != Blocks.air
                        && !ItemUtils.is(p.getHeldItem(), Item.getItemFromBlock(Blocks.ladder))) {
                    double dx = p.posX - t.fallStart.xCoord;
                    double dz = p.posZ - t.fallStart.zCoord;
                    double h = dx * dx + dz * dz;
                    double dy = p.posY - t.fallStart.yCoord;
                    t.fallStart = null;
                    if (h < 144.0 && dy <= -5.0 && Hypixel.state != Hypixel.State.LOBBY && Hypixel.state != Hypixel.State.PIT
                            && Hypixel.state != Hypixel.State.REPLAY && !Hypixel.skywars(0) && Hypixel.buildBattle() == 0) {
                        flag(p, Check.NoFall, "&b" + dy + "m");
                        return;
                    }
                }
            }
        }
        if (Check.NoSlow.enabled() && t.useSprintTicks >= 11 && t.maxHorizontal >= 0.08 && n - t.lastStopUsingTick >= 35) {
            flag(p, Check.NoSlow, "&b" + t.useSprintTicks + " " + (n - t.lastStopUsingTick));
            return;
        }
        if (Check.Scaffold.enabled() && t.scaffoldTicks > 0 && n - t.lastSneakTick >= 25 && (n - t.lastDeltaYTick >= 20 || t.scaffoldTicks > 25)
                && Hypixel.buildBattle() == 0) {
            boolean air = true;
            BlockPos pos = p.getPosition().down(2);
            for (int i = 0; i < 4; ++i) {
                if (!(BlockUtils.getBlock(pos) instanceof BlockAir)) {
                    air = false;
                    break;
                }
                pos = pos.down();
            }
            if (air) {
                flag(p, Check.Scaffold, "&b" + t.scaffoldTicks + " " + (n - t.lastSneakTick));
                return;
            }
        }
        if (Check.Protocol.enabled() && Math.abs(p.rotationPitch) >= 92.0f) {
            flag(p, Check.Protocol, "&bp:" + p.rotationPitch);
        }
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        if (!active()) {
            return;
        }
        if (partyMessage != null) {
            PlayerUtils.sendChatMessage("/pc " + partyMessage);
            partyMessage = null;
        } else if (reportName != null) {
            PlayerUtils.sendChatMessage("/report " + reportName + " cheating");
            reportName = null;
        }
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (ignored(p)) continue;
            Tracker t = trackers.get(p.getUniqueID());
            if (t == null) {
                t = new Tracker();
            }
            t.update(p);
            check(p, t);
            t.endTick(p);
            trackers.put(p.getUniqueID(), t);
        }
    }

    @Override
    public void onDisable() {
        trackers.clear();
        lastFlags.clear();
        breakingPlayers.clear();
        breakingBlocks.clear();
        reported.clear();
        partyMessage = null;
        reportName = null;
        lastPing = 0L;
    }

    private static boolean ignored(EntityPlayer p) {
        return p == mc().thePlayer || p.deathTime != 0 || BotCheck.isBot(p, false);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            this.onDisable();
        }
    }

    private static boolean active() {
        return Chat.inWorld() && !mc().isSingleplayer() && (!disableOutsideMinigames.isToggled() || Hypixel.onHypixel() && Hypixel.state != Hypixel.State.LOBBY);
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        Minecraft mc = mc();
        if (!active()) {
            return true;
        }
        Entity entity;
        if (packet instanceof S25PacketBlockBreakAnim && (Check.AutoClicker.enabled() || Check.FastMine.enabled())
                && (entity = mc.theWorld.getEntityByID(((S25PacketBlockBreakAnim) packet).getBreakerId())) instanceof EntityPlayer) {
            Tracker t = trackers.get(entity.getUniqueID());
            if (t != null) {
                t.onBreakAnim((EntityPlayer) entity, (S25PacketBlockBreakAnim) packet);
            }
        }
        if (packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange p = (S23PacketBlockChange) packet;
            if (p.getBlockState().getBlock() instanceof BlockAir) {
                BlockPos pos = p.getBlockPosition();
                Block block = BlockUtils.getBlock(pos);
                if (block == Blocks.air || block == Blocks.bed || block == Blocks.ladder || block instanceof BlockBush || block instanceof BlockLeaves) {
                    return true;
                }
                EntityPlayer breaker = null;
                S25PacketBlockBreakAnim anim = breakingBlocks.get(pos);
                if (anim != null) {
                    Entity e = mc.theWorld.getEntityByID(anim.getBreakerId());
                    if (e instanceof EntityPlayer) {
                        breaker = (EntityPlayer) e;
                        if (anim.getProgress() > 4 && anim.getProgress() < 10) {
                            flag(breaker, Check.FastMine, "&b" + anim.getProgress());
                        }
                    }
                    breakingBlocks.remove(anim);
                }
                if (Check.Nuker.enabled()) {
                    if (BlockUtils.exposed(pos)) {
                        return true;
                    }
                    double d = -1.0;
                    if (!(breaker != null || ModuleManager.enabled("BedAura") != null && mc.thePlayer.getDistanceSq(pos) <= 36.0)) {
                        for (EntityPlayer other : mc.theWorld.playerEntities) {
                            if (ignored(other)) continue;
                            double d2 = other.getDistanceSq(pos);
                            if (d != -1.0 && !(d2 < d)) continue;
                            d = d2;
                            breaker = other;
                        }
                    }
                    if (breaker != null) {
                        flag(breaker, Check.Nuker, "&b" + block.getRegistryName() + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + Strings.round(d, 3));
                    }
                }
            }
        } else if (packet instanceof S22PacketMultiBlockChange && Check.Nuker.enabled()) {
            S22PacketMultiBlockChange p = (S22PacketMultiBlockChange) packet;
            if (p.getChangedBlocks().length != 2) {
                return true;
            }
            if (!(p.getChangedBlocks()[0].getBlockState().getBlock() instanceof BlockAir)) {
                return true;
            }
            BlockPos a = p.getChangedBlocks()[0].getPos();
            BlockPos b = p.getChangedBlocks()[1].getPos();
            if (BlockUtils.exposed(a) || BlockUtils.exposed(b)) {
                return true;
            }
            EntityPlayer breaker = null;
            for (EntityPlayer other : mc.theWorld.playerEntities) {
                if (other == mc.thePlayer && ModuleManager.enabled("BedAura") != null && (other.getDistanceSq(a) <= 36.0 || other.getDistanceSq(b) <= 36.0)) {
                    break;
                }
                if (ignored(other) || other.getDistanceSq(a) > 36.0 && other.getDistanceSq(b) > 36.0) continue;
                breaker = other;
                break;
            }
            if (breaker != null) {
                flag(breaker, Check.BedNuker, "");
            }
        }
        return true;
    }

    private static void flag(EntityPlayer p, Check check, String info) {
        Minecraft mc = mc();
        if (PlayerUtils.isFriend(p) || ignoreTeammates.isToggled() && Hypixel.sameTeam(p)) {
            return;
        }
        if (addCheatersAsEnemy.isToggled()) {
            Friends.addEnemy(p.getName());
        }
        long now = System.currentTimeMillis();
        if (flagInterval.getInput() > 0.0) {
            HashMap<Check, Long> flags = lastFlags.get(p.getUniqueID());
            if (flags == null) {
                flags = new HashMap<Check, Long>();
            } else {
                Long last = flags.get(check);
                if (last != null && (double) Math.abs(now - last) <= flagInterval.getInput() * 1000.0) {
                    return;
                }
            }
            flags.put(check, now);
            lastFlags.put(p.getUniqueID(), flags);
        }
        boolean hypixel = Hypixel.onHypixel();
        String name = p.getName();
        if (hypixel) {
            if (name.endsWith("§r")) {
                name = Chat.strip(p.getDisplayName().getUnformattedText()).trim();
            }
            if (autoReport.isToggled() && !reported.contains(name) && !Overlay.screenOpen()) {
                reportName = name;
                reported.add(reportName);
            }
        }
        double d = mc.thePlayer.getDistanceToEntity(p);
        String s = p.getDisplayName().getUnformattedText() + " &7detected for &d" + check.displayName + " " + ChatCommands.distance(d, true);
        ChatComponentText text = new ChatComponentText(Chat.color(Chat.PREFIX + s));
        if (hypixel && sendPartyChat.isToggled()) {
            partyMessage = Chat.strip(text.getUnformattedText());
        }
        if (!hideChatAlerts.isToggled()) {
            if (hypixel) {
                ChatStyle style = new ChatStyle();
                style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7/report &c" + name + " &7cheating"))));
                style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report " + name + " cheating"));
                text.appendSibling(new ChatComponentText(Chat.color(" &7[&cWDR&7]")).setChatStyle(style));
            }
            mc.thePlayer.addChatMessage((IChatComponent) text);
        }
        if (shouldPing.isToggled() && now - lastPing > 1500L) {
            PlayerUtils.ping();
            lastPing = now;
        }
        if (ScriptManager.events != null) {
            ScriptEvents.antiCheatFlag(check.displayName, p);
        }
    }
}
