package xyz.daikirai.module.minigames;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Strings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DuelsStatsModule extends Module {
    public static DuelsStatsModule instance;

    public enum Mode {
        Overall, Bridge, Classic, OP, SkyWars, Sumo, UHC
    }

    public static SliderSetting mode;
    public static ButtonSetting sendPartyChat, threatLevel;
    private static Mode current = Mode.Overall;
    private static final AtomicBoolean looking = new AtomicBoolean();
    private static final AtomicBoolean printed = new AtomicBoolean();
    private static final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();
    private static String partyMessage;
    private static final int[] NICKED = new int[0];
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final class Cached {
        final int[] stats;
        final long time;

        Cached(int[] stats, long time) {
            this.stats = stats;
            this.time = time;
        }
    }

    public DuelsStatsModule(int key) {
        super("Duels Stats", Category.minigames, key);
        instance = this;
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        sendPartyChat = (ButtonSetting) ModuleManager.setting(this, "Send party chat");
        threatLevel = (ButtonSetting) ModuleManager.setting(this, "Threat level");
    }

    @Override
    public void onPreMotion(xyz.daikirai.event.MotionEvent e) {
        if (partyMessage != null) {
            PlayerUtils.sendChatMessage("/pc " + partyMessage);
            partyMessage = null;
        }
    }

    private static String format(double d) {
        String s = d == Math.floor(d) ? Long.toString((long) d) : Double.toString(d);
        if (s.length() <= 3) {
            return s;
        }
        String[] parts = s.split("\\.");
        String re = "(\\d)(?=(\\d{3})+$)";
        if (parts.length != 2) {
            return s.replaceAll(re, "$1,");
        }
        if (parts[0].length() <= 3) {
            return s;
        }
        return parts[0].replaceAll(re, "$1,") + "." + parts[1];
    }

    private static void lookup(String name) {
        int[] stats;
        looking.set(true);
        Cached c = cache.get(name);
        String ago = "";
        if (c == null) {
            stats = fetch(name, current);
            if (stats != null) {
                cache.put(name, new Cached(stats, System.currentTimeMillis()));
            }
        } else {
            stats = c.stats;
            ago = " (" + age(c) + " ago)";
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ignored) {
            }
        }
        if (looking.getAndSet(false)) {
            Chat.raw("");
            if (stats == null) {
                Chat.print("&cError &7retrieving &b" + name + "&7's stats!");
            } else if (stats == NICKED) {
                Chat.print("&b" + name + " &7is &cnicked&7!" + ago);
                if (sendPartyChat.enabled) {
                    partyMessage = name + " is nicked";
                }
            } else {
                Chat.print("&b" + name + "&7's &b" + current.name().toLowerCase() + " &7stats" + ago);
                double d = stats[1] == 0 ? (double) stats[0] : (double) stats[0] / (double) stats[1];
                Chat.print("&7Wins: &d" + format(stats[0]) + "&7, Losses: &d" + format(stats[1]) + "&7, WLR: &d" + format(Strings.round(d, 1)));
                if (sendPartyChat.enabled) {
                    partyMessage = name + " - " + format(stats[0]) + " W " + format(stats[1]) + " L " + format(Strings.round(d, 1)) + " WLR";
                }
                if (threatLevel.enabled) {
                    Chat.print("&7Threat: &r" + threat(current, stats[0], stats[1], d));
                }
            }
        }
        printed.set(true);
    }

    private static int[] fetch(String name, Mode m) {
        int[] out = new int[]{0, 0};
        String page = get("https://plancke.io/hypixel/player/stats/" + name,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.5294.0 Safari/537.36");
        if (page.length() < 50) {
            return null;
        }
        if (!page.contains(" Duels ") || !page.contains(name)) {
            return NICKED;
        }
        String duels = page.split(" Duels ")[1];
        if (m == Mode.Overall) {
            String[] a = duels.split("Wins:</b> ");
            if (a.length >= 2) {
                try {
                    String wins = a[1].split("</li><li>")[0];
                    String losses = a[1].split("Losses:</b> ")[1].split("</li><li>")[0];
                    out[0] = Integer.parseInt(wins.replace(",", ""));
                    out[1] = Integer.parseInt(losses.replace(",", ""));
                } catch (Exception ignored) {
                }
            }
        } else {
            String[] a = duels.split(m.name() + " 1v1" + "</td><td>");
            if (a.length == 2) {
                try {
                    String[] cells = a[1].split("</td></tr>")[0].split("</td><td>");
                    out[0] = Integer.parseInt(cells[3].replace(",", ""));
                    out[1] = Integer.parseInt(cells[4].replace(",", ""));
                } catch (Exception ignored) {
                }
            }
        }
        return out;
    }

    private static String get(String url, String userAgent) {
        return "";
    }

    @Override
    public void guiUpdate() {
        current = Mode.values()[(int) mode.getInput()];
    }

    @Override
    public void onDisable() {
        reset();
        cache.clear();
    }

    private static void reset() {
        looking.set(false);
        printed.set(false);
        partyMessage = null;
    }

    private static String age(Cached c) {
        long l = (System.currentTimeMillis() - c.time) / 60000L;
        if (l < 1L) {
            l = 1L;
        }
        return l + "m";
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == Minecraft.getMinecraft().thePlayer) {
            reset();
        }
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (printed.compareAndSet(true, false)) {
            Chat.raw("");
        } else if (!s.isEmpty() && s.charAt(0) == ' ' && s.contains("  Opponent")) {
            String[] a = s.trim().split(" ");
            String name = a[a.length - 1];
            EXECUTOR.execute(() -> lookup(name));
        }
    }

    private static String threat(Mode m, int wins, int losses, double wlr) {
        int n = 0;
        if (wins + losses <= 13) {
            n += 2;
        }
        if (wlr >= 20.0) {
            n += 8;
        } else if (wlr >= 13.0) {
            n += 6;
        } else if (wlr >= 8.0) {
            n += 5;
        } else if (wlr >= 4.0) {
            n += 4;
        } else if (wlr >= 2.5) {
            n += 2;
        } else if (wlr >= 0.8) {
            ++n;
        }
        if (m == Mode.Overall) {
            if (wins >= 20000) {
                n += 4;
            } else if (wins >= 10000) {
                n += 3;
            } else if (wins >= 5000) {
                n += 2;
            } else if (wins >= 1000) {
                ++n;
            }
        } else if (wins >= 10000) {
            n += 4;
        } else if (wins >= 5000) {
            n += 3;
        } else if (wins >= 2500) {
            n += 2;
        } else if (wins >= 500) {
            ++n;
        }
        if (losses == 0) {
            n += wins == 0 ? 3 : 4;
        } else if (losses <= 10 && wlr >= 4.0) {
            n += 2;
        }
        return n == 0 ? "&2VERY LOW" : (n <= 3 ? "&aLOW" : (n <= 6 ? "&6MODERATE" : (n <= 10 ? "&cHIGH" : "&4VERY HIGH")));
    }
}
