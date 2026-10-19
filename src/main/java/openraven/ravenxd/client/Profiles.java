package openraven.ravenxd.client;

import openraven.ravenxd.gui.CategoryComponent;
import openraven.ravenxd.gui.ClickGui;
import openraven.ravenxd.gui.Component;
import openraven.ravenxd.gui.Console;
import openraven.ravenxd.gui.ModuleComponent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public final class Profiles {
    public static Profile current;
    public static final HashMap<Profile, Module> profiles = new HashMap<Profile, Module>();
    public static boolean modified;
    public static final String MANAGER_NAME = "Manager";
    public static final String EXT = ".json";

    private Profiles() {
    }

    public static void markModified() {
        if (current != null) {
            modified = true;
        }
    }

    public static void list() {
        Console.print("&aAvailable profiles:", 1);
        if (profiles.isEmpty()) {
            Console.print("None", 0);
            return;
        }
        int n = 0;
        for (Profile p : profiles.keySet()) {
            Console.print(++n + ". " + p.name, 0);
        }
    }

    public static void loadAll() {
        current = null;
        modified = false;
        Iterator<Map.Entry<Profile, Module>> it = profiles.entrySet().iterator();
        while (it.hasNext()) {
            remove(it.next().getKey(), false, false);
            it.remove();
        }
        File dir = new File(ClientConfig.DIR, "profiles");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                HashSet<String> seen = new HashSet<String>();
                for (File f : files) {
                    if (!f.isFile() || !f.canRead() || seen.contains(f.getName())) continue;
                    seen.add(f.getName());
                    load(f);
                }
            }
        } else {
            dir.mkdirs();
        }
    }

    public static boolean validName(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != ' ' && c != '_' && c != '-') {
                return false;
            }
        }
        return true;
    }

    public static void loadDefault() {
        if (ClientConfig.defaultProfile == null) {
            return;
        }
        for (Profile p : profiles.keySet()) {
            if (p.name.equals(ClientConfig.defaultProfile)) {
                enable(p, true);
                return;
            }
        }
        ClientConfig.defaultProfile = null;
        ClientConfig.save();
    }

    private static CategoryComponent component() {
        for (CategoryComponent c : ClickGui.categories) {
            if (c.categoryName == Category.profiles) return c;
        }
        return null;
    }

    public static void add(Profile p, int keybind) {
        Module m = new openraven.ravenxd.module.ProfileModule(p, keybind);
        profiles.put(p, m);
        component().addModule(m);
    }

    public static void enable(Profile p, boolean isDefault) {
        if (!p.load()) {
            Chat.print("&cError loading " + (!isDefault ? "" : "default ") + "profile: &b" + p.name);
        } else {
            Chat.print("&7Enabled " + (!isDefault ? "" : "default ") + "profile: &b" + p.name);
        }
    }

    public static void remove(Profile p, boolean remove, boolean backup) {
        if (backup) {
            File file = new File(p.path);
            if (file.exists() && file.isFile()) {
                File dir = new File(ClientConfig.DIR + File.separator + "profiles", "backup");
                if (!dir.exists() || !dir.isDirectory()) {
                    dir.mkdirs();
                }
                File to = new File(dir, p.name + "-" + System.currentTimeMillis() + EXT);
                if (!file.renameTo(to)) {
                    file.delete();
                    Chat.print("&cError creating backup file!");
                }
            }
        }
        Module m = !remove ? profiles.get(p) : profiles.remove(p);
        if (m != null) {
            CategoryComponent cat = component();
            for (Component c : cat.modules) {
                if (((ModuleComponent) c).mod == m) {
                    cat.removeModule(m, (ModuleComponent) c);
                    break;
                }
            }
        }
    }

    public static String freeName() {
        outer:
        for (int i = 1; i <= 30; ++i) {
            String s = "profile-" + i;
            for (Profile p : profiles.keySet()) {
                if (p.name.equals(s)) continue outer;
            }
            return s;
        }
        return "";
    }

    public static boolean create(String name) {
        File file = new File(ClientConfig.DIR + File.separator + "profiles", name + EXT);
        Profile p = new Profile(name, file.getAbsolutePath());
        if (p.save()) {
            add(p, 0);
            return true;
        }
        return false;
    }

    public static void load(File file) {
        if (file.getName().startsWith("_") || !file.getName().endsWith(EXT)) {
            return;
        }
        String name = file.getName().replace(EXT, "");
        if (name.length() > 14) {
            name = name.substring(0, 14);
        }
        if (validName(name)) {
            Profile p = new Profile(name, file.getAbsolutePath());
            add(p, p.keybind());
        }
    }

    public static void rename(String[] args) {
        if (!args[1].equalsIgnoreCase("rename")) {
            return;
        }
        if (current == null) {
            Console.print("&cNo profile selected.", 1);
            return;
        }
        String name = args[2];
        if (name.length() > 14 || !validName(name)) {
            Console.print("&cInvalid profile name.", 1);
            return;
        }
        for (Profile p : profiles.keySet()) {
            if (p.name.equals(name)) {
                Console.print("&cProfile already exists.", 1);
                return;
            }
        }
        File file = new File(current.path);
        if (!file.exists() || !file.isFile()) {
            Console.print("&cError locating profile.", 1);
            return;
        }
        File to = new File(file.getParent(), name + EXT);
        if (!file.renameTo(to)) {
            Console.print("&cError renaming profile.", 1);
            return;
        }
        current.set(name, to.getAbsolutePath());
        Module m = profiles.get(current);
        if (m != null) {
            m.name = name;
        }
        Console.print("&aRenamed profile:", 1);
        Console.print(name, 0);
    }
}
