package openraven.ravenxd.jade.compat;

import com.google.gson.JsonObject;
import openraven.ravenxd.module.setting.ButtonSetting;

public class BooleanSetting {
    public final String name;
    public boolean visible = true;
    private boolean value;
    private ButtonSetting backing;

    public BooleanSetting(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    public void bind(ButtonSetting setting) {
        this.backing = setting;
    }

    public boolean isToggled() {
        return this.backing != null ? this.backing.isToggled() : this.value;
    }

    public void enable() {
        this.value = true;
        if (this.backing != null) {
            this.backing.set(true);
        }
    }

    public void loadConfig(JsonObject json) {
    }

    public void setVisible(boolean visible, Module module) {
        this.visible = visible;
    }
}
