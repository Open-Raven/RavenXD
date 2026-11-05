package openraven.ravenxd.client;

import openraven.ravenxd.gui.CategoryComponent;
import openraven.ravenxd.gui.ClickGui;
import openraven.ravenxd.gui.nplus.NPlusCategory;
import openraven.ravenxd.gui.nplus.NPlusGui;
import openraven.ravenxd.module.GuiModule;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public final class ClientConfig {
    public static final String DIR = Minecraft.getMinecraft().mcDataDir + File.separator + "keystrokes";
    public static String defaultProfile;

    private ClientConfig() {
    }

    private static void write(Writer w, String key, Object value) throws Exception {
        w.write(key + ": " + value + "\n");
    }

    public static void save() {
        // Explicit UTF-8, writer always closed (try-with-resources):
        // previously a FileWriter in the platform default charset that
        // was only closed on the success path -- an exception mid-save
        // leaked the handle and could leave a truncated config behind.
        // The parent directory MUST be created before opening the stream.
        try {
            File file = new File(DIR, "config.txt");
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8)) {
                write(w, "default_profile", defaultProfile == null ? "" : defaultProfile);
                write(w, "save_categories", GuiModule.saveCategoryPositions.isToggled());
                for (CategoryComponent c : ClickGui.categories) {
                    write(w, "category-" + c.categoryName.name(), c.getX() + "," + c.getY());
                }
                for (NPlusCategory c : NPlusGui.instance.categories) {
                    write(w, "nplus-" + c.category.name(), c.getX() + "," + c.rawY() + "," + c.isPinned() + "," + c.isOpened());
                }
                write(w, "waifu", openraven.ravenxd.gui.Waifu.x + "," + openraven.ravenxd.gui.Waifu.y);
                write(w, "friends", String.join(",", Friends.friends));
                write(w, "enemies", String.join(",", Friends.enemies));
            }
        } catch (Exception ignored) {
        }
    }

    public static void load() {
        Friends.friends.clear();
        Friends.enemies.clear();
        File file = new File(DIR, "config.txt");
        boolean loaded = false;
        if (file.exists()) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    // Split on the FIRST ": " only: split(": ") dropped
                    // everything after a second separator in the value
                    // (e.g. a profile name or friend entry containing
                    // ": " was silently truncated on load).
                    int sep = line.indexOf(": ");
                    if (sep < 0) continue;
                    String key = line.substring(0, sep), value = line.substring(sep + 2);
                    if (key.equals("default_profile")) {
                        defaultProfile = value;
                    } else if (key.equals("save_categories")) {
                        GuiModule.saveCategoryPositions.setEnabled(value.equals("true"));
                        GuiModule.saveCategoryPositions.updateLabel();
                    } else if (key.startsWith("category-") && GuiModule.saveCategoryPositions.isToggled()) {
                        String name = key.substring("category-".length());
                        String[] xy = value.split(",");
                        if (xy.length == 2) {
                            try {
                                float x = Float.parseFloat(xy[0]);
                                float y = Float.parseFloat(xy[1]);
                                for (CategoryComponent c : ClickGui.categories) {
                                    if (c.categoryName.name().equals(name)) {
                                        c.setX(x);
                                        c.setY(y);
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } else if (key.startsWith("nplus-") && GuiModule.saveCategoryPositions.isToggled()) {
                        String[] v = value.split(",");
                        NPlusCategory c = NPlusGui.instance.get(key.substring("nplus-".length()));
                        if (c != null && v.length == 4) {
                            try {
                                c.load(Integer.parseInt(v[0]), Integer.parseInt(v[1]), Boolean.parseBoolean(v[2]));
                                c.setOpened(Boolean.parseBoolean(v[3]));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else if (key.equals("waifu")) {
                        // ts so dumb
                        String[] xy = value.split(",");
                        if (xy.length == 2) {
                            try {
                                openraven.ravenxd.gui.Waifu.x = Integer.parseInt(xy[0].trim());
                                openraven.ravenxd.gui.Waifu.y = Integer.parseInt(xy[1].trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    } else if (key.equals("friends")) {
                        for (String s : value.split(",")) {
                            if (!s.isEmpty()) Friends.friends.add(s);
                        }
                    } else if (key.equals("enemies")) {
                        for (String s : value.split(",")) {
                            if (!s.isEmpty()) Friends.enemies.add(s);
                        }
                    }
                }
                loaded = true;
            } catch (Exception ignored) {
            }
        }
        if (!loaded) {
            save();
        }
        Profiles.loadAll();
        openraven.ravenxd.module.minigames.SpeedBuildersModule.loadTimes();
    }
}
