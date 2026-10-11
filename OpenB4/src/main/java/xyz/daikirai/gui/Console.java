package xyz.daikirai.gui;

import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.client.Friends;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class Console {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random RANDOM = new Random();

    public static List<String> lines = new ArrayList<String>();
    public static boolean firstOpen = true;
    public static List<Integer> backgrounds = Arrays.asList(new Color(170, 107, 148, 50).getRGB(), new Color(122, 158, 134, 50).getRGB(),
            new Color(16, 16, 16, 50).getRGB(), new Color(64, 114, 148, 50).getRGB());
    public static int lastBackground = -1;
    public static int background = 0;

    public static String nick = "";
    public static String sprintText = "";
    public static boolean bps;
    public static boolean cosmetics = true;

    private Console() {
    }

    public static void pickBackground() {
        int n = RANDOM.nextInt(backgrounds.size());
        if (n == lastBackground) {
            n += n == backgrounds.size() - 1 ? -(backgrounds.size() - 1) : 1;
        }
        lastBackground = n;
        background = backgrounds.get(n);
    }

    public static void render(int height, int width, int scale, FontRenderer fr) {
        int x = width - 195;
        int y = height - 130;
        int top = height - 360;
        GL11.glEnable(3089);
        int w = width * scale;
        GL11.glScissor(0, mc.displayHeight - (top + 245) * scale, w - (w < 2 ? 0 : 2), 245 * scale - 2);
        Gui.drawRect(0, 0, 1000, 1000, background == -1 ? 0x7D000000 : background);
        drawLines(lines, x, y, fr);
        GL11.glDisable(3089);
    }

    public static void drawLines(List<String> list, int x, int y, FontRenderer fr) {
        if (firstOpen) {
            firstOpen = false;
            print("Welcome,", 0);
            print("Use \"help\" for help.", 0);
        }
        if (list.isEmpty()) {
            return;
        }
        for (int i = list.size() - 1; i >= 0; --i) {
            String s = list.get(i);
            int c = -1;
            if (s.contains("&a")) {
                s = s.replace("&a", "");
                c = Color.green.getRGB();
            } else if (s.contains("&c")) {
                s = s.replace("&c", "");
                c = Color.red.getRGB();
            } else if (s.contains("&e")) {
                s = s.replace("&e", "");
                c = Color.yellow.getRGB();
            }
            fr.drawString(s, x, y, c);
            y -= fr.FONT_HEIGHT + 5;
        }
    }

    public static void print(String s, int spacing) {
        if (spacing == 1 || spacing == 2) {
            lines.add("");
        }
        lines.add(s);
        if (spacing == 2 || spacing == 3) {
            lines.add("");
        }
        while (lines.size() > 20) {
            lines.remove(0);
        }
    }

    private static void chat(String s) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(s.replace("&", "§")));
        }
    }

    public static void handle(String raw) {
        if (raw.isEmpty()) {
            return;
        }
        String cmd = raw.toLowerCase();
        boolean hasArgs = raw.contains(" ");
        String[] args = hasArgs ? raw.split(" ") : new String[]{cmd};
        if (args.length == 0) {
            return;
        }
        if (cmd.startsWith("nick")) {
            if (!hasArgs || args.length != 2) {
                print("&cInvalid syntax.", 1);
                return;
            }
            if (args[1].equalsIgnoreCase("reset")) {
                nick = "";
                print("&aNick reset.", 1);
                return;
            }
            nick = args[1];
            print("&aNick has been set to:", 1);
            print("\"" + nick + "\"", 0);
            return;
        }
        if (cmd.startsWith("friend") || cmd.startsWith("f ")) {
            listCommand(args, hasArgs, Friends.friends, "&aFriends reset.", "&aAdded friend:", "&aRemoved friend:");
            return;
        }
        if (cmd.startsWith("enemy") || cmd.startsWith("e ")) {
            listCommand(args, hasArgs, Friends.enemies, "&&aEnemies reset.", "&aAdded enemy:", "&aRemoved enemy:");
            return;
        }
        if (cmd.startsWith("cname")) {
            if (!hasArgs) {
                print("&cInvalid syntax.", 1);
                return;
            }
            if (args.length == 2) {
                ChatCommands.nameHider = args[1];
                print("&aName has been set to:", 1);
                print("\"" + ChatCommands.nameHider + "\"", 0);
                return;
            }
            print("&cInvalid syntax.", 1);
            return;
        }
        if (cmd.startsWith("fakechat")) {
            if (!hasArgs) {
                print("&cInvalid syntax.", 1);
                return;
            }
            String msg = raw.replaceFirst("fakechat", "").substring(1);
            if (!msg.isEmpty() && !msg.equals("\\n")) {
                ChatCommands.fakeChatMessage = msg;
                print("&aMessage set!", 1);
                return;
            }
            print("&cInvalid message.", 1);
            return;
        }
        if (cmd.equals("ping")) {
            ConsolePing.start();
            return;
        }
        if (cmd.startsWith("sprint")) {
            if (!hasArgs) {
                print("&cInvalid syntax.", 1);
                return;
            }
            String text = raw.replaceFirst("sprint", "").substring(1).trim();
            if (text.isEmpty()) {
                print("&cInvalid message.", 1);
                return;
            }
            xyz.daikirai.client.Profiles.markModified();
            xyz.daikirai.module.movement.SprintModule.setText(text);
            print("&aSprint text updated!", 1);
            return;
        }
        if (cmd.startsWith("setspeed")) {
            double d;
            if (hasArgs && args.length == 3) {
                try {
                    d = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    print("&cInvalid syntax.", 1);
                    return;
                }
            } else {
                print("&cInvalid syntax.", 1);
                return;
            }
            if (d > 100.0 || d < 0.0) {
                print("&cInvalid value. [0 - 100]", 1);
                return;
            }
            String[][] targets = {{"fly", "Fly", "Horizontal speed"}, {"bhop", "Bhop", "Speed"}, {"speed", "Speed", "Multiplier"}};
            for (String[] t : targets) {
                if (args[1].equalsIgnoreCase(t[0])) {
                    ((SliderSetting) ModuleManager.setting(ModuleManager.get(t[1]), t[2])).setValueRaw(d);
                    print("&aSet % speed to ".replace("%", t[0]) + d, 1);
                    return;
                }
            }
            print("&cInvalid syntax.", 1);
            return;
        }
        other(cmd, hasArgs, args);
    }

    private static void listCommand(String[] args, boolean hasArgs, Set<String> set, String reset, String added, String removed) {
        if (!hasArgs || args.length != 2) {
            print("&cInvalid syntax.", 1);
            return;
        }
        if (args[1].equalsIgnoreCase("reset")) {
            set.clear();
            print(reset, 1);
        } else {
            String name = args[1].toLowerCase();
            if (set.contains(name)) {
                set.remove(name);
                print(removed, 1);
            } else {
                set.add(name);
                print(added, 1);
            }
            print("\"" + args[1] + "\"", 0);
        }
        ClientConfig.save();
    }

    private static void other(String cmd, boolean hasArgs, String[] args) {
        if (cmd.startsWith("setvelocity")) {
            double d;
            if (!hasArgs || args.length != 3) {
                print("&cInvalid syntax.", 1);
                return;
            }
            try {
                d = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                print("&cInvalid syntax.", 1);
                return;
            }
            if (d > 300.0 || d < -100.0) {
                print("&cInvalid value. [-100 - 300]", 1);
                return;
            }
            Module velocity = ModuleManager.get("Velocity");
            if (args[1].equalsIgnoreCase("h")) {
                ((SliderSetting) ModuleManager.setting(velocity, "Horizontal")).setValueRaw(d);
                print("&aSet h velocity to " + d, 1);
                return;
            }
            if (args[1].equalsIgnoreCase("v")) {
                ((SliderSetting) ModuleManager.setting(velocity, "Vertical")).setValueRaw(d);
                print("&aSet v velocity to " + d, 1);
                return;
            }
            print("&cInvalid syntax.", 1);
            return;
        }
        if (args[0].equalsIgnoreCase("profiles") || args[0].equalsIgnoreCase("p")) {
            if (!hasArgs) {
                xyz.daikirai.client.Profiles.list();
                return;
            }
            if (args.length == 3) {
                xyz.daikirai.client.Profiles.rename(args);
                return;
            }
            print("&cInvalid syntax.", 1);
            return;
        }
        if (cmd.startsWith("clear")) {
            lines.clear();
            return;
        }
        if (cmd.startsWith("help") || cmd.startsWith("?")) {
            print("&eAvailable commands", 1);
            print("- nick [name/reset]", 0);
            print("- friend/enemy [name]", 0);
            print("- ping", 0);
            print("&eProfiles (p)", 0);
            print("- profiles", 0);
            print("- profiles rename [profile]", 0);
            print("&eModule-specific", 0);
            print("- cname [name]", 0);
            print("- fakechat [text]", 0);
            print("- setspeed [fly/bhop/speed] [value]", 0);
            print("- setvelocity [h/v] [value]", 0);
            print("- sprint [text]", 0);
            return;
        }
        if (cmd.equals("shoutout")) {
            print("&eshout out", 1);
            for (String n : new String[]{"charlie", "pug", "jc", "alan", "mic", "syuto", "des"}) {
                print("- " + n, 0);
            }
            return;
        }
        if (cmd.equals("bps")) {
            bps = !bps;
            print("BPS " + (bps ? "enabled" : "disabled"), 1);
            return;
        }
        if (cmd.equals("tcosm")) {
            cosmetics = !cosmetics;
            print("Cosmetics " + (cosmetics ? "enabled" : "disabled"), 1);
            return;
        }
        if (cmd.equals("entities") && mc.theWorld != null) {
            String s = "";
            List<Entity> list = mc.theWorld.loadedEntityList;
            for (int i = 0; i < list.size(); ++i) {
                Entity e = list.get(i);
                s = s + "&7" + e.getClass().getSimpleName() + ":" + e.getEntityId() + ":&r" + e.getName();
                if (i == list.size() - 1) continue;
                s = s + ", ";
            }
            if (!s.isEmpty()) {
                chat(s);
            }
            print("Printed " + list.size() + " entities.", 1);
            return;
        }
        if (cmd.startsWith("coords") && mc.thePlayer != null) {
            chat("&7pX: &d" + mc.thePlayer.posX + " &7pY: &d" + mc.thePlayer.posY + " &7pZ: &d" + mc.thePlayer.posZ);
            BlockPos p = mc.thePlayer.getPosition();
            chat("&7bX: &d" + p.getX() + " &7bY: &d" + p.getY() + " &7bZ: &d" + p.getZ());
            print("Printed coordinates.", 1);
            return;
        }
        print("&cInvalid command. (" + (cmd.length() > 5 ? cmd.substring(0, 5) + "..." : cmd) + ")", 1);
    }
}
