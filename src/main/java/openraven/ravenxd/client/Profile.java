package openraven.ravenxd.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import openraven.ravenxd.gui.InfoHudEditor;
import openraven.ravenxd.module.HudModule;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.ProfileManagerModule;
import openraven.ravenxd.module.ScriptManagerModule;
import openraven.ravenxd.module.SettingsModule;
import openraven.ravenxd.module.movement.SprintModule;
import openraven.ravenxd.module.render.TargetHudModule;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.Setting;
import openraven.ravenxd.module.setting.SliderSetting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class Profile {
    public String name;
    public String path;

    public Profile(String name, String path) {
        this.set(name, path);
    }

    public void set(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public boolean load() {
        File file = new File(this.path);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return false;
        }
        Profiles.current = null;
        Profiles.modified = false;
        ChatCommands.panic();
        try (FileReader r = new FileReader(file)) {
            JsonObject root = new JsonParser().parse(r).getAsJsonObject();
            JsonArray arr = ProfileMigration.migrateModules(root.getAsJsonArray("modules"),
                    openraven.ravenxd.module.combat.VelocityModule.Mode.Advanced.ordinal());
            arr = ProfileMigration.migrateDisabler(arr,
                    openraven.ravenxd.module.other.DisablerModule.Mode.Custom.ordinal());
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                String name = string(o, "name");
                for (Module m : new ArrayList<Module>(ModuleManager.modules)) {
                    if (!m.saveName().equals(name)) continue;
                    Boolean enabled = bool(o, "enabled");
                    m.setBind(integer(o, "keybind"));
                    Boolean hidden = bool(o, "hidden");
                    if (hidden != null) {
                        m.hidden = hidden;
                    }
                    if (m == ModuleManager.hud) {
                        Integer x = integerOrNull(o, "posX");
                        Integer y = integerOrNull(o, "posY");
                        if (x != null) HudModule.posX = x;
                        if (y != null) HudModule.posY = y;
                    } else if (m == ModuleManager.get("TargetHUD")) {
                        Integer x = integerOrNull(o, "posX");
                        Integer y = integerOrNull(o, "posY");
                        if (x != null) TargetHudModule.offsetX = x;
                        if (y != null) TargetHudModule.offsetY = y;
                    } else if (m == ModuleManager.settings) {
                        Integer x = integerOrNull(o, "posX");
                        Integer y = integerOrNull(o, "posY");
                        if (x != null) InfoHudEditor.hudX = x;
                        if (y != null) InfoHudEditor.hudY = y;
                    } else if (m == ModuleManager.get("Sprint")) {
                        Integer x = integerOrNull(o, "posX");
                        Integer y = integerOrNull(o, "posY");
                        if (x != null) SprintModule.posX = x;
                        if (y != null) SprintModule.posY = y;
                        String text = string(o, "text");
                        if (!text.isEmpty()) {
                            SprintModule.setText(text);
                        }
                    } else if (m == ModuleManager.get("Name Hider")) {
                        String cname = string(o, "cname");
                        if (!cname.isEmpty()) {
                            ChatCommands.nameHiderRaw = cname;
                            ChatCommands.nameHider = Chat.color(cname);
                        }
                    }
                    for (Setting s : m.getSettings()) {
                        if (s instanceof SliderSetting) {
                            Double d = number(o, s.getName());
                            if (d != null) {
                                SliderSetting slider = (SliderSetting) s;
                                if (!slider.isMode()) {
                                    slider.setValueRaw(d);
                                } else {
                                    slider.setValue(d);
                                }
                            }
                        } else if (s instanceof ButtonSetting) {
                            Boolean b = bool(o, s.getName());
                            if (b != null) {
                                ButtonSetting button = (ButtonSetting) s;
                                button.set(b);
                                m.guiButtonToggled(button);
                            }
                        }
                    }
                    m.guiUpdate();
                    if (enabled != null && enabled && !m.isEnabled() && !SettingsModule.disableModulesOnLoad.isToggled()
                            && m != ModuleManager.commandLine) {
                        m.enable();
                    }
                    break;
                }
            }
            Profiles.current = this;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int keybind() {
        int n = 0;
        File file = new File(this.path);
        if (file.exists() && file.isFile() && file.canRead()) {
            try (FileReader r = new FileReader(file)) {
                n = integer(new JsonParser().parse(r).getAsJsonObject(), "keybind");
            } catch (Exception ignored) {
            }
        }
        return n;
    }

    public boolean save() {
        try {
            File file = new File(this.path);
            if (!file.exists() || !file.isFile()) {
                file.createNewFile();
            }
            try (JsonWriter w = new JsonWriter(new FileWriter(file, false))) {
                w.setIndent("    ");
                w.beginObject();
                Module self = Profiles.profiles.get(this);
                w.name("keybind").value(self == null ? 0L : (long) self.getKeycode());
                w.name("modules");
                w.beginArray();
                for (Module m : ModuleManager.modules) {
                    if (m.isProfile() || m instanceof ProfileManagerModule || m instanceof ScriptManagerModule) continue;
                    w.beginObject();
                    w.name("name").value(m.saveName());
                    w.name("enabled").value(m.isEnabled());
                    w.name("hidden").value(m.isHidden());
                    w.name("keybind").value((long) m.getKeycode());
                    if (m == ModuleManager.hud) {
                        w.name("posX").value((long) HudModule.posX);
                        w.name("posY").value((long) HudModule.posY);
                    } else if (m == ModuleManager.get("TargetHUD")) {
                        w.name("posX").value((long) TargetHudModule.offsetX);
                        w.name("posY").value((long) TargetHudModule.offsetY);
                    } else if (m == ModuleManager.settings) {
                        w.name("posX").value((long) InfoHudEditor.hudX);
                        w.name("posY").value((long) InfoHudEditor.hudY);
                    } else if (m == ModuleManager.get("Sprint")) {
                        w.name("posX").value((long) SprintModule.posX);
                        w.name("posY").value((long) SprintModule.posY);
                        w.name("text").value(SprintModule.storedText());
                    } else if (m == ModuleManager.get("Name Hider")) {
                        w.name("cname").value(ChatCommands.nameHiderRaw);
                    }
                    for (Setting s : m.getSettings()) {
                        if (!s.saved) continue;
                        if (s instanceof SliderSetting) {
                            w.name(s.getName()).value(((SliderSetting) s).getInput());
                        } else if (s instanceof ButtonSetting) {
                            w.name(s.getName()).value(((ButtonSetting) s).isToggled());
                        }
                    }
                    w.endObject();
                }
                w.endArray();
                w.endObject().flush();
            }
            Profiles.current = this;
            Profiles.modified = false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String string(JsonObject o, String k) {
        try {
            return o.get(k).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Boolean bool(JsonObject o, String k) {
        try {
            return o.get(k).getAsBoolean();
        } catch (Exception e) {
            return null;
        }
    }

    private static int integer(JsonObject o, String k) {
        try {
            return o.get(k).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private static Integer integerOrNull(JsonObject o, String k) {
        try {
            return o.get(k).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static Double number(JsonObject o, String k) {
        try {
            return o.get(k).getAsDouble();
        } catch (Exception e) {
            return null;
        }
    }
}
