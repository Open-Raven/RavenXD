package xyz.daikirai.module;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.client.Profiles;
import xyz.daikirai.module.setting.ButtonSetting;

import java.awt.Desktop;
import java.io.File;

public class ProfileManagerModule extends Module {
    private long lastPress;
    public static ButtonSetting createProfile, loadProfiles, openFolder, removeDefault;

    public ProfileManagerModule(Category category, int key) {
        super(Profiles.MANAGER_NAME, category, key);
    }

    @Override
    public void link() {
        createProfile = (ButtonSetting) ModuleManager.setting(this, "Create profile");
        loadProfiles = (ButtonSetting) ModuleManager.setting(this, "Load profiles");
        openFolder = (ButtonSetting) ModuleManager.setting(this, "Open folder");
        removeDefault = (ButtonSetting) ModuleManager.setting(this, "Remove default profile");
    }

    private boolean cooldown() {
        long l = System.currentTimeMillis();
        if (Math.abs(this.lastPress - l) < 500L) {
            Chat.print("&cYou are on cooldown.");
            return false;
        }
        this.lastPress = l;
        return true;
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == createProfile) {
            if (this.cooldown()) {
                String name = Profiles.freeName();
                if (name.isEmpty()) {
                    Chat.print("&cCannot override existing profile!");
                } else if (Profiles.create(name)) {
                    Chat.print("&7Created profile: &b" + name);
                } else {
                    Chat.print("&cError creating profile: &b" + name);
                }
            }
        } else if (b == loadProfiles) {
            if (this.cooldown()) {
                Profiles.loadAll();
                if (Profiles.profiles.isEmpty()) {
                    Chat.print("&7No profiles found.");
                } else {
                    int n = Profiles.profiles.size();
                    Chat.print("&7Loaded &b" + n + " &7profile" + (n == 1 ? "." : "s."));
                }
            }
        } else if (b == openFolder) {
            File file = new File(ClientConfig.DIR, "profiles");
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception exception) {
                file.mkdirs();
                Chat.print("&cError locating profiles folder, recreated.");
            }
        } else if (b == removeDefault) {
            if (ClientConfig.defaultProfile == null) {
                Chat.print("&7No default profile set.");
            } else {
                Chat.print("&7Removed default profile: &b" + ClientConfig.defaultProfile);
                ClientConfig.defaultProfile = null;
                ClientConfig.save();
            }
        }
    }
}
