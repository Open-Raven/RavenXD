package xyz.daikirai.client;

import xyz.daikirai.module.IrcModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class Irc {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final String PREFIX = "&7[&dIRC&7]&r ";

    public static volatile boolean connected;
    public static volatile boolean muted;
    private static long lastSent;
    public static int messages, killAuraHits, scaffoldBlocks, highestFps;
    private static int lastFps;
    private static boolean openedByKey, keyHeld;

    public static int fps;
    private static int frames;
    private static long fpsWindow;

    private Irc() {
    }

    public static void countFrame() {
        ++frames;
        long now = System.currentTimeMillis();
        if (fpsWindow == 0L || now - fpsWindow >= 1000L) {
            fps = frames;
            frames = 0;
            fpsWindow = now;
        }
    }

    public static void trackFps() {
        if (Hypixel.state == Hypixel.State.LIMBO) {
            return;
        }
        int n = fps;
        if (lastFps > 0 && lastFps != n && n - lastFps < 3000 && n > highestFps) {
            highestFps = n;
        }
        lastFps = n;
    }

    public static void onKillAuraHit() {
        if (!playing()) {
            return;
        }
        ++killAuraHits;
    }

    public static void onScaffoldPlace() {
        if (!playing()) {
            return;
        }
        ++scaffoldBlocks;
    }

    private static boolean playing() {
        return Hypixel.state != Hypixel.State.NOT_HYPIXEL && Hypixel.state != Hypixel.State.LOBBY && Hypixel.state != Hypixel.State.LIMBO
                && System.currentTimeMillis() - Math.max(xyz.daikirai.util.ClickUtils.lastLeftClick, xyz.daikirai.util.ClickUtils.lastRightClick) < 180000L;
    }

    public static void system(String s) {
        print(s, false, true);
    }

    public static void print(String s, boolean timestamped, boolean prefixed) {
        if (s == null) {
            return;
        }
        if (prefixed && !timestamped) {
            s = PREFIX + s;
        }
        if (Chat.inWorld()) {
            Chat.raw(s);
        }
    }

    private static String clean(String s) {
        String t = s.substring(1).replaceAll("[^\\x20-\\x7E]", "").trim();
        if (t.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(t.length());
        boolean skip = false;
        for (int i = 0; i < t.length(); ++i) {
            char c = t.charAt(i);
            if (skip) {
                skip = false;
                continue;
            }
            if (c == '&') {
                skip = true;
                continue;
            }
            if (c <= '\u0014' || c >= '\u007f') continue;
            sb.append(c);
        }
        return sb.toString();
    }

    public static boolean onChat(String s) {
        IrcModule.Prefix prefix = IrcModule.prefix;
        if (prefix == IrcModule.Prefix.None) {
            if (s.length() >= 1 && s.charAt(0) == '/') {
                return false;
            }
            s = ' ' + s;
        } else if (s.length() <= 1 || s.charAt(0) != prefix.c) {
            return false;
        }
        boolean fromChat = false;
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (!cls.equals("net.minecraft.client.gui.GuiChat") || !m.equals("func_73869_a") && !m.equals("keyTyped")) continue;
            fromChat = true;
            break;
        }
        if (!fromChat) {
            return false;
        }
        if (!IrcModule.receiveMessages.isToggled()) {
            system("&cEnable 'Receive messages' to use IRC.");
            return true;
        }
        if (!IrcModule.sendMessages.isToggled()) {
            system("&cEnable 'Send messages' to send IRC messages.");
            return true;
        }
        if (!connected) {
            system("&cYou aren't connected.");
            return true;
        }
        if (muted) {
            system("&cYou are muted.");
            return true;
        }
        long now = System.currentTimeMillis();
        long since = Math.abs(now - lastSent);
        if (since < 2500L) {
            double left = Math.round((double) (2500L - since) / 1000.0 * 10.0) / 10.0;
            system("&cYou are on cooldown for " + (left == Math.floor(left) ? Long.toString((long) left) : Double.toString(left)) + "s.");
            return true;
        }
        s = clean(s);
        int n = s.length();
        if (n == 0) {
            system("&cYour message is invalid.");
            return true;
        }
        if (n > 170) {
            system("&cYour message is too long.");
            return true;
        }
        String lower = s.toLowerCase();
        if (lower.contains("http://") || lower.contains("https://") || lower.contains("www.")) {
            boolean allowed = lower.contains("/discord.gg") || lower.contains("/youtube.com") || lower.contains("/youtu.be") || lower.contains("/x.com");
            if (!allowed) {
                system("&cYou cannot send unverified links.");
                return true;
            }
        }
        lastSent = now;
        ++messages;
        return true;
    }

    public static void tick() {
        IrcModule.Prefix prefix = IrcModule.prefix;
        if (prefix.key != 0) {
            boolean down = false;
            if (mc.currentScreen == null) {
                down = isDown(prefix.key);
                if (down && !keyHeld) {
                    mc.displayGuiScreen(new GuiChat(String.valueOf(prefix.c)));
                    openedByKey = true;
                } else if (openedByKey) {
                    openedByKey = false;
                }
            } else if (openedByKey && mc.currentScreen instanceof GuiChat && (down = isDown(prefix.key)) && !keyHeld) {
                mc.displayGuiScreen(null);
                openedByKey = false;
            }
            keyHeld = down;
        }
    }

    private static boolean isDown(int key) {
        return key != 0 && (key < 0 ? Mouse.isButtonDown(key + 100) : Keyboard.isKeyDown(key));
    }
}
