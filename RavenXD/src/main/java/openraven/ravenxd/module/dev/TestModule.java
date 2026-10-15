package xyz.daikirai.module.dev;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockBreaker;
import xyz.daikirai.util.Executor;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.util.Strings;
import xyz.daikirai.util.TeleportPath;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestModule extends Module {
    public static ButtonSetting blockBreakTest, breakTime, limboPacket, printTicks, bedwarsTp, bedwarsLobbyTp, killCounter, killCounterCheck,
            antiDcbot, entityInfo, rejoin;
    public static SliderSetting limboType, bedwarsTpDelay;

    public static String dcName = "";
    public static long breakStart;
    public static BlockPos breaking;
    public static BlockPos selected;
    public static BlockPos tpTarget;
    public static long lastKillMessage;

    public TestModule(int key) {
        super("Test", Category.other, key);
    }

    @Override
    public void link() {
        blockBreakTest = (ButtonSetting) ModuleManager.setting(this, "Block break test");
        breakTime = (ButtonSetting) ModuleManager.setting(this, "Break time");
        limboPacket = (ButtonSetting) ModuleManager.setting(this, "Limbo packet");
        limboType = (SliderSetting) ModuleManager.setting(this, "Limbo type");
        printTicks = (ButtonSetting) ModuleManager.setting(this, "Print ticks");
        bedwarsTp = (ButtonSetting) ModuleManager.setting(this, "Bedwars TP");
        bedwarsTpDelay = (SliderSetting) ModuleManager.setting(this, "Bedwars TP delay");
        bedwarsLobbyTp = (ButtonSetting) ModuleManager.setting(this, "Bedwars lobby TP");
        killCounter = (ButtonSetting) ModuleManager.setting(this, "Kill counter");
        killCounterCheck = (ButtonSetting) ModuleManager.setting(this, "Kill counter check");
        antiDcbot = (ButtonSetting) ModuleManager.setting(this, "Anti dcbot");
        entityInfo = (ButtonSetting) ModuleManager.setting(this, "Entity info");
        rejoin = (ButtonSetting) ModuleManager.setting(this, "Rejoin");
    }

    public static void onDig(C07PacketPlayerDigging p) {
        if (breakTime.isToggled() && p.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
            breakStart = System.currentTimeMillis();
            breaking = p.getPosition();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Chat.inWorld()) {
            return;
        }
        String s = Chat.strip(e.message.getUnformattedText());
        if (s.isEmpty()) {
            return;
        }
        if (bedwarsTp.isToggled()) {
            if (s.equals("You have respawned!") || tpTarget != null && s.charAt(0) == ' ' && s.contains("Protect your bed")) {
                Executor.POOL.execute(this::tpDown);
            } else if (tpTarget != null && s.equals("You will respawn because you still have a bed!")) {
                RotationUtils.setRotation(RotationUtils.toBlock(tpTarget));
                Chat.print("&7updated rotations, distance: &d" + Strings.round(mc.thePlayer.getDistance(tpTarget.getX(), tpTarget.getY(), tpTarget.getZ()), 1));
            }
        }
        if (bedwarsLobbyTp.isToggled() && s.startsWith(mc.thePlayer.getName() + " has joined")) {
            Executor.POOL.execute(TestModule::lobbyTp);
        }
        if (killCounterCheck.isToggled() && s.contains("current kills: ")) {
            lastKillMessage = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate || !Chat.inWorld()) {
            return;
        }
        if (blockBreakTest.isToggled()) {
            if (e.button == 0) {
                MovingObjectPosition hit = BotCheck.rayTraceBlocks(25.0);
                if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                    return;
                }
                e.setCanceled(true);
                if (selected != null) {
                    BlockBreaker.abort(selected);
                }
                selected = hit.getBlockPos();
                BlockBreaker.start(selected);
            } else if (e.button == 1) {
                if (selected != null) {
                    e.setCanceled(true);
                    BlockBreaker.stop(selected);
                    selected = null;
                } else {
                    Chat.print("&cno block selected");
                }
            }
        } else if (bedwarsTp.isToggled() && e.button == 1) {
            MovingObjectPosition hit = BotCheck.rayTraceBlocks(150.0);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return;
            }
            tpTarget = hit.getBlockPos();
            Chat.print("&7set target block: &d" + tpTarget.getX() + " " + tpTarget.getY() + " " + tpTarget.getZ());
        }
    }

    @Override
    public void onDisable() {
        tpTarget = null;
        breaking = null;
        selected = null;
        lastKillMessage = 0L;
        breakStart = 0L;
    }

    @Override
    public void onUpdate() {
        if (breakTime.isToggled() && breaking != null) {
            long now = System.currentTimeMillis();
            if (mc.theWorld.getBlockState(breaking).getBlock() == Blocks.air) {
                Chat.print("&7block break took &b" + (now - breakStart) + "ms");
                breaking = null;
            } else if (Math.abs(now - breakStart) >= 10000L) {
                Chat.print("&7timed out, break took too long");
                breaking = null;
            }
        }
        if (printTicks.isToggled()) {
            Chat.print("&7ticks existed: &b" + mc.thePlayer.ticksExisted);
        }
        if (killCounter.isToggled() && mc.thePlayer.ticksExisted % 60 == 0) {
            String kills = "unknown";
            for (String line : Hypixel.sidebarLines()) {
                String s = Chat.strip(line);
                if (!s.startsWith("Kills: ")) continue;
                try {
                    kills = s.split("Kills: ")[1];
                } catch (Exception ex) {
                    kills = "error";
                }
            }
            mc.thePlayer.sendChatMessage("/pc current kills: " + kills + " (" + Math.round(1000.0 * Math.random()) + ")");
        }
        if (killCounterCheck.isToggled() && lastKillMessage != 0L) {
            if (Hypixel.bedwars(2)) {
                long l = Math.abs(System.currentTimeMillis() - lastKillMessage);
                if (l >= 5000L) {
                    Chat.print("&d" + l + "&7ms since last message, sending win message");
                    mc.thePlayer.sendChatMessage("/shout gg! " + MathUtils.randomString(10));
                    lastKillMessage = 0L;
                }
            } else {
                lastKillMessage = 0L;
                Chat.print("&7not in game, reset last received kill count");
            }
        }
    }

    @Override
    public void onEnable() {
        if (rejoin.isToggled()) {
            ServerData serverData = new ServerData("hypixel", "hypixel.net", false);
            mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, serverData));
            this.disable();
            return;
        }
        if (limboPacket.isToggled()) {
            switch ((int) limboType.getInput()) {
                case 0:
                    PacketUtils.sendThroughLag(new C09PacketHeldItemChange(-1));
                    break;
                case 1:
                    PacketUtils.sendThroughLag(new C07PacketPlayerDigging());
                    break;
                case 2:
                    PacketUtils.sendThroughLag(new C03PacketPlayer.C04PacketPlayerPosition(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !mc.thePlayer.onGround));
                    break;
            }
            this.disable();
        }
        if (antiDcbot.isToggled()) {
            if (dcName.isEmpty()) {
                Chat.print("&7dcname not set, use &ddcname [name]");
            } else {
                EntityPlayer target = null;
                for (EntityPlayer p : mc.theWorld.playerEntities) {
                    if (p == mc.thePlayer || !p.getName().equalsIgnoreCase(dcName)) continue;
                    target = p;
                    break;
                }
                if (target == null) {
                    Chat.print("&7target not found: &d" + dcName);
                } else {
                    Chat.print("&7players within &b20 &7blocks of &d" + dcName);
                    for (EntityPlayer p : mc.theWorld.playerEntities) {
                        if (p == mc.thePlayer || p.getName().equalsIgnoreCase(dcName) || target.getDistanceSqToEntity(p) > 400.0) continue;
                        ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + " &7- "));
                        ChatStyle style = new ChatStyle();
                        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7click to paste username"))));
                        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, p.getName()));
                        line.appendSibling(new ChatComponentText(Chat.color("&d" + p.getName())).setChatStyle(style));
                        mc.thePlayer.addChatMessage(line);
                    }
                }
            }
            this.disable();
        }
        if (entityInfo.isToggled()) {
            Chat.print("entity players");
            List<EntityPlayer> players = mc.theWorld.playerEntities;
            for (int i = 0; i < players.size(); ++i) {
                EntityPlayer p = players.get(i);
                ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + "&7" + p.getName()));
                ChatStyle style = new ChatStyle();
                style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7uuid: &r" + p.getUniqueID()))));
                line.setChatStyle(style);
                mc.thePlayer.addChatMessage(line);
            }
            Chat.print("network players");
            ArrayList<NetworkPlayerInfo> infos = new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
            MathUtils.dedupe(infos);
            for (NetworkPlayerInfo info : infos) {
                ScorePlayerTeam team = info.getPlayerTeam();
                ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + "&7" + info.getGameProfile().getName()));
                ChatStyle style = new ChatStyle();
                style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7uuid: &r" + info.getGameProfile().getId()
                        + (team == null ? "&7no team found" : "\n&7team name: &r" + team.getTeamName() + "\n&7registered: &r" + team.getRegisteredName())))));
                line.setChatStyle(style);
                mc.thePlayer.addChatMessage(line);
            }
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard != null) {
                Chat.print("scoreboard (score teams)");
                for (ScorePlayerTeam team : scoreboard.getTeams()) {
                    String members = "&emembers";
                    Collection<String> names = team.getMembershipCollection();
                    for (String name : names) {
                        members = members + "\n&r" + name;
                    }
                    ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + "&7" + team.getTeamName()));
                    ChatStyle style = new ChatStyle();
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color(members))));
                    line.setChatStyle(style);
                    mc.thePlayer.addChatMessage(line);
                }
            }
            this.disable();
        }
    }

    public void tpDown() {
        if (bedwarsTpDelay.getInput() != 0.0) {
            try {
                Thread.sleep((long) bedwarsTpDelay.getInput());
            } catch (Exception ex) {
                return;
            }
        }
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer(false));
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY - 20.0, mc.thePlayer.posZ, true));
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround));
        Chat.print("&7teleported, disabling");
        this.disable();
    }

    public static void lobbyTp() {
        try {
            Thread.sleep(200L);
            TeleportPath.teleport(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 50.0, mc.thePlayer.posZ), true);
        } catch (Exception ex) {
            Chat.print("&7error teleporting");
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        if (blockBreakTest.isToggled() && selected != null) {
            Render3D.block(selected, Color.orange.getRGB(), false, true);
        }
        if (bedwarsTp.isToggled() && tpTarget != null) {
            for (int i = 0; i < 10; ++i) {
                Render3D.block(tpTarget.up(i), i == 0 ? Color.orange.getRGB() : Color.yellow.getRGB(), false, true);
            }
        }
    }
}
