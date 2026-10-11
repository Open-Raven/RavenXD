package xyz.daikirai.module;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.client.Profile;
import xyz.daikirai.client.Profiles;
import xyz.daikirai.module.setting.ButtonSetting;

import java.util.Iterator;
import java.util.Map;

public class ProfileModule extends Module {
    public ProfileModule(Profile profile, int keycode) {
        super(profile, keycode);
        this.registerSetting(new SaveButton(profile));
        this.registerSetting(new DefaultButton(profile));
        this.registerSetting(new RemoveButton(profile));
    }

    static final class SaveButton extends ButtonSetting {
        private final Profile profile;

        SaveButton(Profile profile) {
            super("Save profile");
            this.profile = profile;
        }

        @Override
        public void toggle() {
            super.toggle();
            for (Map.Entry<Profile, Module> e : Profiles.profiles.entrySet()) {
                Profile p = e.getKey();
                if (p != this.profile) continue;
                if (p.save()) {
                    Chat.print("&7Saved profile: &b" + p.name);
                    break;
                }
                Chat.print("&cError saving profile: &b" + p.name);
                break;
            }
        }
    }

    static final class DefaultButton extends ButtonSetting {
        private final Profile profile;

        DefaultButton(Profile profile) {
            super("Set default");
            this.profile = profile;
        }

        @Override
        public void toggle() {
            super.toggle();
            if (this.profile.name.equals(ClientConfig.defaultProfile)) {
                Chat.print("&b" + this.profile.name + " &7is already your default profile.");
            } else {
                ClientConfig.defaultProfile = this.profile.name;
                Chat.print("&7Set default profile: &b" + this.profile.name);
                ClientConfig.save();
            }
        }
    }

    static final class RemoveButton extends ButtonSetting {
        private final Profile profile;

        RemoveButton(Profile profile) {
            super("Remove profile");
            this.profile = profile;
        }

        @Override
        public void toggle() {
            Iterator<Map.Entry<Profile, Module>> it = Profiles.profiles.entrySet().iterator();
            while (it.hasNext()) {
                Profile p = it.next().getKey();
                if (p != this.profile) continue;
                Profiles.remove(p, false, true);
                it.remove();
                if (Profiles.current == p) {
                    Profiles.current = null;
                }
                Chat.print("&7Removed profile: &b" + p.name);
                break;
            }
        }
    }
}
