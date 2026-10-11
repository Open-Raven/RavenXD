package xyz.daikirai.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public final class Chat {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final String COLOR = "§";
    public static final String PREFIX = "&7[&dR&7]&r ";
    public static final char SECTION = '§';

    private Chat() {
    }

    public static boolean inWorld() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    public static String color(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return s.replace("&", "§");
    }

    public static String strip(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean skip = false;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (skip) {
                skip = false;
                continue;
            }
            if (c == SECTION) {
                skip = true;
                continue;
            }
            if (c <= '\u0014' || c >= '\u007f') continue;
            sb.append(c);
        }
        return sb.toString();
    }

    public static void print(String s) {
        if (!inWorld()) {
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(color(PREFIX + s)));
    }

    public static void moduleMessage(xyz.daikirai.module.Module m, String s) {
        print("&b" + m.getName() + "&7: &r" + s);
    }

    public static void raw(String s) {
        if (!inWorld()) {
            return;
        }
        mc.thePlayer.addChatMessage(new ChatComponentText(color(s)));
    }
}
