package openraven.ravenxd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.List;

public final class Hypixel {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public enum State {
        NOT_HYPIXEL, UNKNOWN, LIMBO, LOBBY, REPLAY, BRIDGE_PRACTICE, PIT, MURDER_MYSTERY_GAME, BEDWARS_PREGAME,
        BEDWARS_GAME_BED, BEDWARS_GAME_NO_BED, SKYWARS_QUEUE, SKYWARS_GAME_SOLO, SKYWARS_GAME_TEAM, DUELS_QUEUE, DUELS_GAME
    }

    public static State state = State.UNKNOWN;
    public static String lastMode;
    public static boolean pendingLocraw;
    public static boolean awaitingLocraw;
    public static long joinedAt;
    private static final String HOST = "hypixel.net";
    private static final String LOBBY_MARK = "  " + Chat.SECTION + "8L";
    private static final char BED_MARK = '✓';

    private Hypixel() {
    }

    public static boolean sameTeam(net.minecraft.entity.EntityLivingBase e) {
        if (!onHypixel()) {
            return false;
        }
        if (state == State.PIT) {
            return false;
        }
        if (e instanceof net.minecraft.entity.monster.EntitySilverfish) {
            String own;
            if (e.getName().length() > 5 && (own = mc.thePlayer.getDisplayName().getUnformattedText()).length() > 5
                    && e.getName().substring(0, 5).equals(own.substring(0, 5))) {
                return true;
            }
        } else if (e instanceof net.minecraft.entity.monster.EntityIronGolem) {
            Entity tag = mc.theWorld.getEntityByID(e.getEntityId() + 1);
            String own;
            if (tag != null && tag.getName().length() > 2 && (own = mc.thePlayer.getDisplayName().getUnformattedText()).length() > 2
                    && tag.getName().substring(0, 2).equals(own.substring(0, 2))) {
                return true;
            }
        } else if (!(e instanceof net.minecraft.entity.player.EntityPlayer)) {
            return !(e instanceof net.minecraft.entity.passive.EntityHorse) && (state == State.BEDWARS_GAME_BED || state == State.BEDWARS_GAME_NO_BED);
        }
        net.minecraft.scoreboard.ScorePlayerTeam team = (net.minecraft.scoreboard.ScorePlayerTeam) e.getTeam();
        if (team == null) {
            return false;
        }
        net.minecraft.scoreboard.ScorePlayerTeam own = ownTeam();
        if (own == null) {
            return false;
        }
        return team.getColorPrefix().equals(own.getColorPrefix());
    }

    public static net.minecraft.scoreboard.ScorePlayerTeam ownTeam() {
        String nick = openraven.ravenxd.gui.Console.nick;
        if (!nick.isEmpty() && mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (!info.getGameProfile().getName().equalsIgnoreCase(nick)) continue;
                return info.getPlayerTeam();
            }
        }
        return (net.minecraft.scoreboard.ScorePlayerTeam) mc.thePlayer.getTeam();
    }

    public static double positionHash(double d, double d2, double d3) {
        d2 = d2 < 1.0 ? 1.0 : Math.floor(d2);
        return 0.0174 * (double) Double.hashCode(d) + 0.023 * (double) Double.hashCode(d2) + 0.039 * (double) Double.hashCode(d3);
    }

    public static boolean onHypixel() {
        return state != State.NOT_HYPIXEL;
    }

    public static boolean duels(int n) {
        switch (n) {
            case 0: return state == State.DUELS_QUEUE || state == State.DUELS_GAME;
            case 1: return state == State.DUELS_QUEUE;
            case 2: return state == State.DUELS_GAME;
        }
        return false;
    }

    public static boolean skywars(int n) {
        switch (n) {
            case 0: return state == State.SKYWARS_QUEUE || state == State.SKYWARS_GAME_SOLO || state == State.SKYWARS_GAME_TEAM;
            case 1: return state == State.SKYWARS_QUEUE;
            case 2: return state == State.SKYWARS_GAME_SOLO || state == State.SKYWARS_GAME_TEAM;
            case 3: return state == State.SKYWARS_GAME_SOLO;
            case 4: return state == State.SKYWARS_GAME_TEAM;
        }
        return false;
    }

    public static boolean bedwars(int n) {
        switch (n) {
            case 0: return state == State.BEDWARS_PREGAME || state == State.BEDWARS_GAME_BED || state == State.BEDWARS_GAME_NO_BED;
            case 1: return state == State.BEDWARS_PREGAME;
            case 2: return state == State.BEDWARS_GAME_BED || state == State.BEDWARS_GAME_NO_BED;
        }
        return false;
    }

    public static int buildBattle() {
        List<String> list;
        if (onHypixel() && (list = sidebar()) != null && list.size() > 6 && Chat.strip(list.get(0)).contains("BUILD BATTLE")) {
            boolean startsIn = false;
            boolean timeLeft = false;
            boolean theme = false;
            boolean judging = false;
            for (String s : list) {
                if (s.contains("Mode:") && s.contains("Speed Bu")) {
                    return 1;
                }
                if (s.contains("Starts In:")) {
                    startsIn = true;
                    continue;
                }
                if (s.contains("Time Left:")) {
                    timeLeft = true;
                    continue;
                }
                if (s.contains("Judging:")) {
                    judging = true;
                    continue;
                }
                if (s.contains("Theme:")) {
                    theme = true;
                    continue;
                }
                if (!s.contains("Perfect Builds:")) continue;
                if (theme) {
                    if (startsIn) {
                        boolean items = false;
                        for (int i = 0; i < 9; ++i) {
                            if (net.minecraft.client.Minecraft.getMinecraft().thePlayer.inventory.getStackInSlot(i) == null) continue;
                            items = true;
                            break;
                        }
                        return items ? 4 : 3;
                    }
                    if (timeLeft) {
                        return 4;
                    }
                    if (!judging) continue;
                    return 5;
                }
                return 2;
            }
        }
        return 0;
    }

    public static boolean isFakeEntity(Entity e) {
        return e.getEntityId() == -170 || e == openraven.ravenxd.module.player.FreecamModule.camera || e == openraven.ravenxd.module.player.FreecamModule.body;
    }

    public static NetworkPlayerInfo info(EntityPlayer p) {
        return p == null || mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfo(p.getUniqueID());
    }

    public static String tabHeader() {
        try {
            Object o = ReflectionHelper.getPrivateValue(GuiPlayerTabOverlay.class, mc.ingameGUI.getTabList(), "header", "field_175256_i");
            if (o != null) {
                return ((IChatComponent) o).getUnformattedText();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public static boolean isHypixelServer() {
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null) {
            return false;
        }
        if (mc.getCurrentServerData().serverIP.toLowerCase().endsWith(HOST)) {
            return true;
        }
        return tabHeader().toLowerCase().contains(HOST);
    }

    public static List<String> sidebar() {
        if (mc.theWorld == null) return null;
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return null;
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return null;
        ArrayList<Score> scores = new ArrayList<Score>(sb.getSortedScores(obj));
        ArrayList<String> out = new ArrayList<String>(10);
        out.add(obj.getDisplayName());
        int n = 0;
        for (int i = scores.size() - 1; i >= 0 && n < 15; --i) {
            Score s = scores.get(i);
            String name;
            if (s == null || (name = s.getPlayerName()) == null || name.startsWith("#")) continue;
            ScorePlayerTeam team = sb.getPlayersTeam(name);
            out.add(ScorePlayerTeam.formatPlayerName(team, name));
            ++n;
        }
        return out;
    }

    public static List<String> sidebarLines() {
        ArrayList<String> out = new ArrayList<String>();
        if (mc.theWorld == null) return out;
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return out;
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return out;
        java.util.Collection<Score> scores = sb.getSortedScores(obj);
        ArrayList<Score> kept = new ArrayList<Score>();
        for (Score s : scores) {
            if (s == null || s.getPlayerName() == null || s.getPlayerName().startsWith("#")) continue;
            kept.add(s);
        }
        java.util.List<Score> shown = kept.size() > 15 ? com.google.common.collect.Lists.newArrayList(com.google.common.collect.Iterables.skip(kept, scores.size() - 15)) : kept;
        for (Score s : shown) {
            ScorePlayerTeam team = sb.getPlayersTeam(s.getPlayerName());
            out.add(ScorePlayerTeam.formatPlayerName(team, s.getPlayerName()));
        }
        return out;
    }

    public static State detect() {
        if (!isHypixelServer()) return State.NOT_HYPIXEL;
        List<String> lines = sidebar();
        if (lines == null) {
            if (mc.theWorld.provider.getDimensionId() == 1) return State.LIMBO;
            return State.UNKNOWN;
        }
        boolean bedwars = false, skywars = false, skyPlayers = false, duels = false, murder = false;
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (i == 0) {
                String t = Chat.strip(line);
                if (t.equals("REPLAY")) return State.REPLAY;
                if (t.equals("BED WARS PRACTICE")) return State.BRIDGE_PRACTICE;
                if (t.equals("THE HYPIXEL PIT")) return State.PIT;
                if (t.equals("BED WARS")) bedwars = true;
                else if (t.equals("SKYWARS")) skywars = true;
                else if (t.equals("DUELS")) duels = true;
                else if (t.equals("MURDER MYSTERY")) murder = true;
                continue;
            }
            if (i == 1 && line.contains(LOBBY_MARK)) return State.LOBBY;
            if (bedwars) {
                if (i >= 5) {
                    if (i == 8 && line.startsWith("Mode")) return State.BEDWARS_PREGAME;
                    if (line.endsWith(" YOU")) {
                        return line.indexOf(BED_MARK) != -1 ? State.BEDWARS_GAME_BED : State.BEDWARS_GAME_NO_BED;
                    }
                }
            } else if (skywars) {
                if (i == 3) {
                    if (line.startsWith("Player")) return State.SKYWARS_QUEUE;
                } else if (i == 6) {
                    if (line.startsWith("Player")) skyPlayers = true;
                } else if (i == 7 && skyPlayers) {
                    return !line.startsWith("Team") ? State.SKYWARS_GAME_SOLO : State.SKYWARS_GAME_TEAM;
                }
            } else if (murder) {
                if (i == 3 && line.startsWith("Role")) return State.MURDER_MYSTERY_GAME;
            } else if (duels) {
                if (i == 3) {
                    if (line.startsWith("Time")) return State.DUELS_GAME;
                } else if (i == 6) {
                    if (line.startsWith("Waiting.")) return State.DUELS_QUEUE;
                    if (line.startsWith("Starting ")) return State.DUELS_QUEUE;
                }
            }
        }
        return State.UNKNOWN;
    }

    public static void tick() {
        state = detect();
        if (pendingLocraw && Math.abs(System.currentTimeMillis() - joinedAt) > 500L) {
            pendingLocraw = false;
            if (onHypixel()) {
                mc.thePlayer.sendChatMessage("/locraw");
                awaitingLocraw = true;
            }
        }
        if (mc.thePlayer.ticksExisted % 60 == 0 && mc.getNetHandler() != null && onHypixel()) {
            Irc.trackFps();
            NetworkPlayerInfo info = info(mc.thePlayer);
            String name;
            openraven.ravenxd.gui.Console.nick = info == null ? "" : ((name = info.getGameProfile().getName()).equals(mc.thePlayer.getName()) ? "" : name);
        }
    }

    public static boolean onChat(String s) {
        if (awaitingLocraw && s.charAt(0) == '{') {
            String key = "\"mode\":\"";
            int i = s.indexOf(key);
            int j;
            if (i != -1 && (j = s.indexOf('"', i += key.length())) != -1) {
                String mode = s.substring(i, j).toLowerCase();
                if (!mode.equals("base") && !mode.equals("bedwars_practice")) {
                    lastMode = mode;
                }
            }
            if (openraven.ravenxd.script.ScriptManager.events != null) {
                openraven.ravenxd.script.ScriptEvents.locraw(s);
            }
            awaitingLocraw = false;
            return true;
        }
        return false;
    }
}
