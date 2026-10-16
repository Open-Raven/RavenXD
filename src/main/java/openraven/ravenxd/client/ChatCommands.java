package openraven.ravenxd.client;

import openraven.ravenxd.gui.CategoryComponent;
import openraven.ravenxd.gui.ClickGui;
import openraven.ravenxd.gui.Component;
import openraven.ravenxd.gui.ModuleComponent;
import openraven.ravenxd.module.ChatCommandsModule;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.script.ScriptCrypto;
import openraven.ravenxd.script.ScriptEvent;
import openraven.ravenxd.script.ScriptManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class ChatCommands {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random RANDOM = new Random();

    public static long pingSentAt;
    public static String tracking;
    public static final HashMap<String, Double> speedBuilderScores = new HashMap<String, Double>();
    public static String fakeChatMessage = "&eThis is a fake chat message.";
    public static String nameHiderRaw = Account.NAME;
    public static String nameHider = Chat.color(Account.NAME);
    public static String ravenUser = "";

    private ChatCommands() {
    }

    public static String tag(String cmd, String s) {
        return "&7[&f" + cmd + "&7] &r" + s;
    }

    public static String lower(String s) {
        if (ChatCommandsModule.lowercase.isToggled()) {
            s = s.toLowerCase();
        }
        return s;
    }

    public static void line(String s) {
        Chat.print(lower(s));
    }

    public static void reply(String cmd, String s) {
        line(tag(cmd, s));
    }

    public static boolean handle(String s) {
        String prefix = ChatCommandsModule.prefix;
        if (!s.startsWith(prefix)) {
            return false;
        }
        String[] args = s.split(" ");
        int n = args[0].replace(prefix, "").length();
        if (n == 0 || n != args[0].length() - prefix.length()) {
            return false;
        }
        String cmd = args[0].substring(prefix.length()).toLowerCase();
        String rest = args.length > 1 ? s.substring(cmd.length() + prefix.length() + 1) : "";
        switch (cmd) {
            case "help":
            case "?": {
                help(cmd);
                break;
            }
            case "ask": {
                reply(cmd, "&cAssistants are currently out of service :(");
                break;
            }
            case "clear": {
                for (int i = 0; i < 30; ++i) {
                    Chat.raw("");
                }
                break;
            }
            case "ign":
            case "name": {
                ign(cmd);
                break;
            }
            case "enemy":
            case "e": {
                enemy(cmd, args);
                break;
            }
            case "fakechat": {
                if (!rest.isEmpty()) {
                    fakeChatMessage = rest;
                }
                ModuleManager.get("Fake Chat").enable();
                break;
            }
            case "fps": {
                int fps = Minecraft.getDebugFPS();
                String c = "&";
                c = fps < 60 ? c + "4" : (fps < 120 ? c + "c" : (fps < 300 ? c + "6" : (fps < 600 ? c + "e" : (fps < 1500 ? c + "a" : c + "2"))));
                reply(cmd, "&7FPS: " + c + fps);
                break;
            }
            case "friend":
            case "f": {
                friend(cmd, args);
                break;
            }
            case "ping": {
                if (mc.isSingleplayer()) {
                    reply(cmd, "&7Your ping: &20&7ms");
                    break;
                }
                if (pingSentAt == 0L) {
                    pingSentAt = -1L;
                    reply(cmd, "&7Estimating...");
                    break;
                }
                reply(cmd, "&cWait for the result!");
                break;
            }
            case "play":
            case "p": {
                play(cmd, args);
                break;
            }
            case "requeue":
            case "rq": {
                if (!Hypixel.onHypixel()) {
                    reply(cmd, "&cMust be on Hypixel!");
                    break;
                }
                if (Hypixel.lastMode == null) {
                    reply(cmd, "&cYou haven't queued any games yet.");
                    break;
                }
                mc.thePlayer.sendChatMessage("/play " + Hypixel.lastMode);
                break;
            }
            case "stats": {
                Chat.print("&b" + ravenUser + "&7's " + Account.NAME + " stats");
                Chat.print("&7 IRC Messages: &b" + commas(Irc.messages));
                Chat.print("&7 KillAura Hits: &b" + commas(Irc.killAuraHits));
                Chat.print("&7 Scaffold Blocks: &b" + commas(Irc.scaffoldBlocks));
                Chat.print("&7 Highest FPS: &b" + commas(Irc.highestFps));
                break;
            }
            case "time": {
                time(cmd, args);
                break;
            }
            case "track": {
                track(cmd, args);
                break;
            }
            case "decrypt": {
                decrypt(cmd, args);
                break;
            }
            case "rotate": {
                float yaw;
                float pitch;
                if (args.length != 3) {
                    reply(cmd, "&7Enter yaw/pitch.");
                    break;
                }
                try {
                    yaw = Float.parseFloat(args[1]);
                    pitch = Float.parseFloat(args[2]);
                } catch (NumberFormatException e) {
                    reply(cmd, "&7Enter valid yaw/pitch.");
                    break;
                }
                mc.thePlayer.rotationYaw = yaw;
                mc.thePlayer.rotationPitch = pitch;
                break;
            }
            case "cname": {
                if (rest.isEmpty()) {
                    reply(cmd, "&7Current name hider name: &b" + nameHider);
                    break;
                }
                nameHiderRaw = rest;
                nameHider = Chat.color(rest);
                Chat.print("&7Set name hider name to &b" + nameHider);
                break;
            }
            case "binds": {
                binds(cmd, args);
                break;
            }
            case "bind":
            case "b": {
                bind(cmd, args);
                break;
            }
            case "find": {
                find(cmd, rest);
                break;
            }
            case "hide": {
                hide(cmd, rest);
                break;
            }
            case "limbo": {
                ModuleManager.get("Limbo").enable();
                break;
            }
            case "panic": {
                int count = panic();
                if (count == 0) {
                    reply(cmd, "&cNo modules enabled.");
                    break;
                }
                Profiles.markModified();
                reply(cmd, "&7Disabled &b" + count + " &7enabled modules.");
                break;
            }
            case "renameprofile": {
                if (args.length != 2) {
                    reply(cmd, "&cInvalid syntax, enter new profile name.");
                } else {
                    renameProfile(cmd, args[1]);
                }
                break;
            }
            case "resetgui": {
                resetGui();
                reply(cmd, "&7Reset GUI positions.");
                break;
            }
            case "sb": {
                speedBuilders(cmd, rest);
                break;
            }
            case "toggle":
            case "t": {
                if (rest.isEmpty()) {
                    reply(cmd, "&cInvalid syntax, enter a module name.");
                    break;
                }
                Module m = find(rest);
                if (m == null) {
                    reply(cmd, "&cInvalid module.");
                    break;
                }
                if (m.noToggle) {
                    reply(cmd, "&cModule cannot be toggled.");
                    break;
                }
                m.toggle();
                reply(cmd, (m.isEnabled() ? "&a+" : "&c-") + " &b" + m.getName() + " &7" + (m.isEnabled() ? "enabled." : "disabled."));
                break;
            }
            default: {
                reply(cmd.length() > 8 ? cmd.substring(0, 8) + "..." : cmd, "&cInvalid command.");
            }
        }
        return true;
    }

    public static Module find(String name) {
        for (Module m : ModuleManager.modules) {
            if (m.getName().toLowerCase().equals(name.toLowerCase())) return m;
        }
        return null;
    }

    private static void renameProfile(String cmd, String name) {
        Profile current = Profiles.current;
        if (current == null) {
            reply(cmd, "&cNo profile selected.");
            return;
        }
        if (name.length() > 14 || !Profiles.validName(name)) {
            reply(cmd, "&cInvalid profile name.");
            return;
        }
        for (Profile p : Profiles.profiles.keySet()) {
            if (!p.name.equals(name)) continue;
            reply(cmd, "&cProfile already exists.");
            return;
        }
        java.io.File file = new java.io.File(current.path);
        if (!file.exists() || !file.isFile()) {
            reply(cmd, "&cError locating profile.");
            return;
        }
        java.io.File to = new java.io.File(file.getParent(), name + Profiles.EXT);
        if (!file.renameTo(to)) {
            reply(cmd, "&cError renaming profile.");
            return;
        }
        current.set(name, to.getAbsolutePath());
        openraven.ravenxd.module.Module m = Profiles.profiles.get(current);
        if (m != null) {
            m.name = name;
        }
        reply(cmd, "&7Renamed profile: &b" + name);
    }

    public static int panic() {
        int n = 0;
        for (Module m : ModuleManager.modules) {
            if (!m.isEnabled()) continue;
            if (m == ModuleManager.commandLine) continue;
            m.disable();
            ++n;
        }
        return n;
    }

    public static String commas(int n) {
        String s = String.valueOf(n);
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

    public static String distance(double d, boolean brackets) {
        char c = d <= 10.0 ? 'c' : (d <= 25.0 ? '6' : (d <= 50.0 ? 'e' : (d <= 100.0 ? 'a' : '2')));
        String s = "&" + c + (int) d + "m";
        if (brackets && ((ButtonSetting) ModuleManager.setting(ModuleManager.get("Settings"), "Add brackets to distance")).isToggled()) {
            s = "&7(" + s + "&7)";
        }
        return s;
    }

    private static boolean validName(String s) {
        if (s.length() > 16) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') continue;
            return false;
        }
        return true;
    }

    private static String keyName(int n) {
        if (n < 0) {
            return "MB" + (n + 100);
        }
        return Keyboard.getKeyName(n);
    }

    private static void enemy(String cmd, String[] args) {
        listOrEdit(cmd, args, Friends.enemies, " &7enemies added.", "&7&oEnemies are highlighted and targeted.", " &7enemies.",
                "&7Removed enemy: &b", "&7Added enemy: &b");
    }

    private static void friend(String cmd, String[] args) {
        listOrEdit(cmd, args, Friends.friends, " &7friends added.", "&7&oFriends are exempt from targeting.", " &7friends.",
                "&7Removed friend: &b", "&7Added friend: &b");
    }

    private static void listOrEdit(String cmd, String[] args, java.util.Set<String> set, String added, String note, String cleared,
                                   String removedMsg, String addedMsg) {
        if (args.length < 2) {
            reply(cmd, "&7You have &b" + set.size() + added);
            reply(cmd, note);
            if (!set.isEmpty()) {
                int n = 0;
                StringBuilder sb = new StringBuilder(" &b");
                for (String s : set) {
                    if (n > 0) {
                        sb.append("&7, &b");
                    }
                    sb.append(s);
                    if (++n < 3) continue;
                    reply(cmd, sb.toString());
                    n = 0;
                    sb.setLength(0);
                    sb.append(" &b");
                }
                if (n > 0) {
                    reply(cmd, sb.toString());
                }
            }
        } else {
            if (args[1].equals("-")) {
                reply(cmd, "&7Cleared &b" + set.size() + cleared);
                set.clear();
            } else {
                for (int i = 1; i < args.length; ++i) {
                    String s = args[i].toLowerCase();
                    if (set.contains(s)) {
                        set.remove(s);
                        reply(cmd, removedMsg + s + " &7[&c-&7]");
                        continue;
                    }
                    set.add(s);
                    reply(cmd, addedMsg + s + " &7[&a+&7]");
                }
            }
            ClientConfig.save();
        }
    }

    private static void ign(String cmd) {
        if (!Chat.inWorld()) {
            reply(cmd, "&cError getting username.");
            return;
        }
        String name = mc.thePlayer.getName();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(name), null);
        } catch (Exception ignored) {
        }
        ChatComponentText text = new ChatComponentText(Chat.color(Chat.PREFIX + lower(tag(cmd, "&7Your username: "))));
        ChatStyle style = new ChatStyle();
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color(lower("&7Copied to clipboard")))));
        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, name));
        String s = "&b" + (name.length() > 1 ? name.charAt(0) + "&b" + name.substring(1) : name);
        text.appendSibling(new ChatComponentText(Chat.color(s)).setChatStyle(style));
        mc.thePlayer.addChatMessage((IChatComponent) text);
    }

    // script decrypter cuz the file/folder layout of the repo is fucked
    private static void decrypt(String cmd, String[] args) {
        if (args.length != 2) {
            reply(cmd, "&7Enter script name.");
            return;
        }
        String name = args[1].endsWith(ScriptManager.EXTENSION) ? args[1].substring(0, args[1].length() - ScriptManager.EXTENSION.length()) : args[1];
        if (!name.matches("[A-Za-z0-9_-]+")) {
            reply(cmd, "&cInvalid script name.");
            return;
        }
        File dir = new File(ClientConfig.DIR, "scripts");
        File in = new File(dir, name + ScriptManager.EXTENSION);
        if (!in.isFile()) {
            reply(cmd, "&cScript &b" + name + "&c not found.");
            return;
        }
        String payload = null;
        try {
            for (String line : Files.readAllLines(in.toPath(), StandardCharsets.UTF_8)) {
                if (line.contains("decrypt - \"") && (payload = ScriptCrypto.decryptLine(line)) != null) {
                    break;
                }
            }
        } catch (IOException e) {
            reply(cmd, "&cCouldn't read script.");
            return;
        }
        String trailer = payload == null ? null : ScriptCrypto.trailer(payload);
        String source = payload == null ? null : ScriptCrypto.source(payload);
        if (source == null) {
            reply(cmd, "&cScript &b" + name + "&c isn't encrypted.");
            return;
        }
        for (ScriptEvent event : ScriptEvent.values()) {
            String derived = event.deriveEncryptedName(trailer);
            if (derived != null) {
                source = source.replace(derived, event.getNormalName());
            }
        }
        File out = new File(dir, name + "-decrypted" + ScriptManager.EXTENSION);
        try {
            Files.write(out.toPath(), source.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            reply(cmd, "&cCouldn't write &b" + out.getName() + "&c.");
            return;
        }
        reply(cmd, "&7Decrypted to &b" + out.getName() + "&7.");
    }

    private static void track(String cmd, String[] args) {
        if (args.length != 2) {
            if (tracking == null) {
                reply(cmd, "&cEnter player name.");
            } else {
                tracking = null;
                reply(cmd, "&7Cleared tracking target.");
            }
        } else if (!validName(args[1])) {
            reply(cmd, "&cInvalid username.");
        } else if (tracking != null && tracking.equalsIgnoreCase(args[1])) {
            reply(cmd, "&cAlready tracking target.");
        } else {
            EntityPlayer target = null;
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == mc.thePlayer || !p.getName().equalsIgnoreCase(args[1])) continue;
                tracking = p.getName();
                target = p;
                break;
            }
            if (target == null) {
                reply(cmd, "&cInvalid player.");
            } else {
                double d = mc.thePlayer.getDistanceToEntity(target);
                reply(cmd, "&7Tracking &b" + target.getName() + " " + distance(d, true));
            }
        }
    }

    private static void bind(String cmd, String[] args) {
        if (args.length != 2 && args.length != 3 && args.length != 4) {
            reply(cmd, "&cInvalid syntax, enter module name and keybind.");
            return;
        }
        String name;
        Module m = null;
        String key = null;
        if (args.length == 2) {
            name = args[1];
        } else if (args.length == 3) {
            name = args[1] + " " + args[2];
            m = find(name);
            if (m == null) {
                name = args[1];
                key = args[2];
            }
        } else {
            name = args[1] + " " + args[2];
            key = args[3];
        }
        if (m == null) {
            m = find(name);
        }
        if (m == null) {
            reply(cmd, "&cInvalid module.");
        } else if (!m.canBeBound()) {
            reply(cmd, "&cModule cannot be bound.");
        } else if (key == null) {
            if (m.getKeycode() == 0) {
                reply(cmd, "&b" + m.getName() + " &7has no keybind.");
            } else {
                reply(cmd, "&b" + m.getName() + " &7is set to &d" + keyName(m.getKeycode()) + "&7.");
            }
        } else if (key.equals("-")) {
            if (m.getKeycode() == 0) {
                reply(cmd, "&b" + m.getName() + " &7has no keybind.");
            } else {
                m.setBind(0);
                reply(cmd, "&b" + m.getName() + " &7has been unbound.");
            }
        } else {
            key = key.toUpperCase();
            int n = Keyboard.getKeyIndex(key);
            if (n == 0) {
                reply(cmd, "&cKeybind not supported as text, use GUI.");
            } else if (n == m.getKeycode()) {
                reply(cmd, "&b" + m.getName() + " &7is already set to &d" + key + "&7.");
            } else {
                m.setBind(n);
                reply(cmd, "&b" + m.getName() + " &7set to &d" + key + "&7.");
            }
        }
    }

    private static void binds(String cmd, String[] args) {
        if (args.length != 1 && args.length != 2) {
            reply(cmd, "&cInvalid syntax.");
            return;
        }
        String key = null;
        int n = 0;
        if (args.length == 2 && (n = Keyboard.getKeyIndex(key = args[1].toUpperCase())) == 0) {
            reply(cmd, "&cKeybind not supported as text.");
            return;
        }
        int count = 0;
        TreeMap<String, ArrayList<String>> map = new TreeMap<String, ArrayList<String>>();
        for (Module m : ModuleManager.modules) {
            if (m.getKeycode() == 0 || key != null && m.getKeycode() != n) continue;
            String k = keyName(m.getKeycode());
            ArrayList<String> list = map.get(k);
            if (list == null) {
                list = new ArrayList<String>();
            }
            ++count;
            list.add(m.getName());
            map.put(k, list);
        }
        if (key == null) {
            reply(cmd, "&b" + count + " &7module" + (count == 1 ? " has a keybind" : "s have keybinds") + ".");
        } else {
            reply(cmd, "&b" + count + " &7module" + (count == 1 ? " is" : "s are") + " bound to &d" + key + "&7.");
        }
        for (Map.Entry<String, ArrayList<String>> e : map.entrySet()) {
            StringBuilder sb = new StringBuilder(" &b" + e.getKey() + " &7");
            List<String> list = e.getValue();
            for (int i = 0; i < list.size(); ++i) {
                if (i > 0) {
                    sb.append(", &7");
                }
                sb.append(list.get(i));
            }
            line(sb.toString());
        }
    }

    private static void find(String cmd, String name) {
        if (name.isEmpty()) {
            reply(cmd, "&cInvalid syntax, enter a module name.");
            return;
        }
        Module m = find(name);
        if (m == null) {
            reply(cmd, "&cInvalid module.");
            return;
        }
        HashMap<String, List<String>> byCategory = new HashMap<String, List<String>>();
        for (Module x : ModuleManager.modules) {
            List<String> list = byCategory.get(x.moduleCategory().name());
            if (list == null) {
                byCategory.put(x.moduleCategory().name(), list = new ArrayList<String>());
            }
            list.add(x.getName());
        }
        for (Map.Entry<String, List<String>> e : byCategory.entrySet()) {
            List<String> list = e.getValue();
            for (int i = 0; i < list.size(); ++i) {
                String s = list.get(i);
                if (!s.equals(m.getName())) continue;
                String cat = e.getKey().substring(0, 1).toUpperCase() + e.getKey().substring(1);
                reply(cmd, "&b" + cat + " &7-> &b" + s + " &7(" + (i + 1) + "/" + list.size() + ")");
                return;
            }
        }
    }

    private static void hide(String cmd, String name) {
        if (name.isEmpty()) {
            reply(cmd, "&cInvalid syntax, enter a module name.");
            return;
        }
        Module m = find(name);
        if (m == null) {
            reply(cmd, "&cInvalid module.");
            return;
        }
        boolean was = m.isHidden();
        m.toggleHidden();
        if (was == m.isHidden()) {
            reply(cmd, "&cCannot hide this module.");
        } else if (was) {
            reply(cmd, "&b" + m.getName() + " &7is now &avisible &7in HUD.");
        } else {
            reply(cmd, "&b" + m.getName() + " &7is now &chidden &7from HUD.");
        }
    }

    public static void resetGui() {
        if (ClickGui.categories == null) {
            return;
        }
        float y = 5.0f;
        for (CategoryComponent c : ClickGui.categories) {
            for (Component comp : c.modules) {
                ModuleComponent mc = (ModuleComponent) comp;
                if (!mc.opened) continue;
                mc.toggleOpen();
            }
            c.x = 5.0f;
            c.y = y;
            c.setOpened(false);
            y += 20.0f;
        }
        openraven.ravenxd.gui.nplus.NPlusGui.instance.resetPositions();
    }

    private static void play(String cmd, String[] args) {
        if (!Hypixel.onHypixel()) {
            reply(cmd, "&cMust be on Hypixel!");
            return;
        }
        if (args.length != 2) {
            reply(cmd, "&7All modes");
            line(" &bbridge &7Bridge duel");
            line(" &bclassic &7Classic duel");
            line(" &bop &7OP duel");
            line(" &bsumo &7Sumo duel");
            line(" &buhc &7UHC duel");
            line(" &bbw[1-4,24,d,p] &7Bed Wars");
            line(" &bswn[1,2] &7Sky Wars Normal");
            line(" &bswi[1,2] &7Sky Wars Insane");
            line(" &bmm &7Murder Mystery Classic");
            line(" &bsb &7Speed Builders");
            return;
        }
        String mode = "";
        switch (args[1].toLowerCase()) {
            case "bridge": mode = "duels_bridge_duel"; break;
            case "classic": mode = "duels_classic_duel"; break;
            case "op": mode = "duels_op_duel"; break;
            case "sumo": mode = "duels_sumo_duel"; break;
            case "uhc": mode = "duels_uhc_duel"; break;
            case "bw1": mode = "bedwars_eight_one"; break;
            case "bw2": mode = "bedwars_eight_two"; break;
            case "bw3": mode = "bedwars_four_three"; break;
            case "bw4": mode = "bedwars_four_four"; break;
            case "bw24": mode = "bedwars_two_four"; break;
            case "bwd": mode = "bedwars_two_one_duels"; break;
            case "bwp": mode = "bedwars_practice"; break;
            case "swn1": mode = "solo_normal"; break;
            case "swn2": mode = "teams_normal"; break;
            case "swi1": mode = "solo_insane"; break;
            case "swi2": mode = "teams_insane"; break;
            case "mm": mode = "murder_classic"; break;
            case "sb": mode = "build_battle_speed_builders"; break;
        }
        if (mode.isEmpty()) {
            reply(cmd, "&cInvalid mode, use &b.play");
        } else {
            mc.thePlayer.sendChatMessage("/play " + mode);
        }
    }

    private static void time(String cmd, String[] args) {
        if (args.length != 2) {
            reply(cmd, "&cInvalid syntax, enter day/sunset/night/vanilla.");
            return;
        }
        Module weather = ModuleManager.get("Weather");
        SliderSetting time = (SliderSetting) ModuleManager.setting(weather, "Time");
        switch (args[1].toLowerCase()) {
            case "day": {
                time.setValue(14.1);
                weather.enable();
                reply(cmd, "&7Set time to &bday&7.");
                break;
            }
            case "sunset": {
                time.setValue(21.0);
                weather.enable();
                reply(cmd, "&7Set time to &bsunset&7.");
                break;
            }
            case "night": {
                time.setValue(2.1);
                weather.enable();
                reply(cmd, "&7Set time to &bnight&7.");
                break;
            }
            case "vanilla": {
                time.setValue(0.0);
                reply(cmd, "&7Set time to &bvanilla&7.");
                break;
            }
            default: {
                reply(cmd, "&cInvalid time, enter day/sunset/night/vanilla.");
            }
        }
    }

    private static void speedBuilders(String cmd, String theme) {
        if (speedBuilderScores.isEmpty()) {
            reply(cmd, "&cNo speed builder scores saved.");
            return;
        }
        if (theme.isEmpty()) {
            double total = 0.0;
            for (Map.Entry<String, Double> e : speedBuilderScores.entrySet()) {
                total += e.getValue();
            }
            reply(cmd, "&b" + SliderSetting.formatNumber(round(total / (double) speedBuilderScores.size(), 1)) + "s &7average time with &b"
                    + speedBuilderScores.size() + " &7builds");
            reply(cmd, "&7Fastest times:");
            speedBuilderScores.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(5L).forEach(e -> score(cmd, e));
            reply(cmd, "&7Slowest times:");
            speedBuilderScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(5L).forEach(e -> score(cmd, e));
        } else {
            String name = null;
            String difficulty = null;
            double d = 0.0;
            for (Map.Entry<String, Double> e : speedBuilderScores.entrySet()) {
                if (!e.getKey().toLowerCase().startsWith(theme.toLowerCase() + "-")) continue;
                String[] parts = e.getKey().split("-");
                name = parts[0];
                difficulty = parts[1];
                d = e.getValue();
            }
            if (name == null) {
                reply(cmd, "&cTheme not found.");
            } else {
                String c = difficulty.equals("Simple") ? "&2" : (difficulty.equals("Easy") ? "&a" : (difficulty.equals("Medium") ? "&6" : "&c"));
                reply(cmd, "&e" + name + " " + c + difficulty + " &7- &b" + d + "s");
            }
        }
    }

    private static void score(String cmd, Map.Entry<String, Double> e) {
        reply(cmd, " &e" + e.getKey().split("-")[0] + " &7- &b" + e.getValue() + "s");
    }

    private static double round(double d, int places) {
        double p = Math.pow(10.0, places);
        return Math.round(d * p) / p;
    }

    public static void tick() {
        if (pingSentAt == -1L) {
            pingSentAt = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            int n = RANDOM.nextInt(6 - 4 + 1) + 4;
            for (int i = 0; i < n; ++i) {
                sb.append('/');
            }
            mc.thePlayer.sendChatMessage(sb.toString());
        } else if (pingSentAt > 0L && System.currentTimeMillis() - pingSentAt > 3000L) {
            pingSentAt = 0L;
            reply("ping", "&cPing timed out.");
        }
    }

    public static void onChat(ClientChatReceivedEvent event, String s) {
        if (s.startsWith("Unknown command")) {
            event.setCanceled(true);
            long l = Math.max(System.currentTimeMillis() - pingSentAt - 20L, 0L);
            String c = "&";
            c = l < 30L ? c + "2" : (l < 80L ? c + "a" : (l < 130L ? c + "e" : (l < 180L ? c + "6" : (l < 230L ? c + "c" : c + "4"))));
            reply("ping", "&7Your ping: " + c + l + "&7ms");
            pingSentAt = 0L;
        }
    }

    private static void help(String cmd) {
        reply(cmd, "&7Commands - &dGeneral");
        line(" &b.ask [msg] &7Talk to " + Account.NAME + " AI.");
        line(" &b.clear &7Clears the chat.");
        line(" &b.ign/name &7Copy your username.");
        line(" &b.enemy/e (name/-) &7Manage enemies.");
        line(" &b.fakechat (text) &7Print fake messages.");
        line(" &b.fps &7Check your framerate.");
        line(" &b.friend/f (name/-) &7Manage friends.");
        line(" &b.ping &7Estimate your ping.");
        line(" &b.play/p (mode) &7Quick play Hypixel games.");
        line(" &b.requeue/rq &7Requeue last Hypixel gamemode.");
        line(" &b.stats &7View your " + Account.NAME + " stats.");
        line(" &b.time [time] &7Set world time.");
        line(" &b.track [name] &7Highlight a player.");
        line(" &b.decrypt [script] &7Decrypt a script to [script]-decrypted.");
        line(" &b.rotate [yaw] [pitch] &7Set rotations.");
        reply(cmd, "&7Commands - &dModules");
        line(" &b.cname [name] &7Set name hider name.");
        line(" &b.binds (key) &7List module binds.");
        line(" &b.bind/b [module] (key/-) &7Bind a module.");
        line(" &b.find [module] &7Find a module in GUI.");
        line(" &b.hide [module] &7Hide a module from HUD.");
        line(" &b.limbo &7Sends you to Hypixel limbo.");
        line(" &b.panic &7Disable all modules.");
        line(" &b.renameprofile [name] &7Rename profile.");
        line(" &b.resetgui &7Reset GUI positions.");
        line(" &b.sb (theme) &7View speed builder stats.");
        line(" &b.toggle/t [module] &7Toggle a module.");
    }
}
