package xyz.daikirai.module;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.script.ScriptManager;
import xyz.daikirai.util.Strings;

import java.awt.Desktop;
import java.io.File;

public class ScriptManagerModule extends Module {
    public long lastLoad;
    public long cooldown = 1000L;
    public static ButtonSetting loadScripts, openFolder, viewDocs;

    public ScriptManagerModule(Category category, int key) {
        super(ScriptManager.MANAGER_NAME, category, key);
    }

    @Override
    public void link() {
        loadScripts = (ButtonSetting) ModuleManager.setting(this, "Load scripts");
        openFolder = (ButtonSetting) ModuleManager.setting(this, "Open folder");
        viewDocs = (ButtonSetting) ModuleManager.setting(this, "View documentation");
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == loadScripts) {
            if (ScriptManager.compiler == null) {
                Chat.print("&cCompiler error, JDK not found!");
            } else if (ScriptManager.loading) {
                Chat.print("&cScripts are still loading...");
            } else {
                long l = System.currentTimeMillis();
                long l2 = Math.abs(l - this.lastLoad);
                if (l2 < this.cooldown) {
                    Chat.print("&cYou are on cooldown for " + SliderSetting.formatNumber(Strings.round((double) (this.cooldown - l2) / 1000.0, 1)) + "s.");
                } else {
                    this.lastLoad = l;
                    ScriptManager.reload();
                    if (ScriptManager.scripts.isEmpty()) {
                        Chat.print("&7No scripts found.");
                    } else {
                        int n = ScriptManager.loadedCount;
                        l2 = Math.abs(System.currentTimeMillis() - this.lastLoad);
                        String string = "&b";
                        string = l2 < 100L ? string + l2 + "&7ms" : string + SliderSetting.formatNumber(Strings.round((double) l2 / 1000.0, 1)) + "&7s";
                        Chat.print("&7Loaded &b" + n + " &7script" + (n == 1 ? "" : "s") + " in " + string + ".");
                    }
                }
            }
        } else if (b == openFolder) {
            File file = new File(ClientConfig.DIR, "scripts");
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception exception) {
                file.mkdirs();
                Chat.print("&cError locating scripts folder, recreated.");
            }
        } else if (b == viewDocs) {
            Chat.print("&cError retrieving API docs!");
        }
    }
}
