package xyz.daikirai.module.setting;

public class Setting {
    public String name;
    public boolean saved = true;

    public Setting(String name) {
        this.name = name;
    }

    public Setting notSaved() {
        this.saved = false;
        return this;
    }

    public String getName() {
        return this.name;
    }
}
