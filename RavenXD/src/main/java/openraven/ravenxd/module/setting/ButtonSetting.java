package xyz.daikirai.module.setting;

public class ButtonSetting extends Setting {
    public String label;
    public long enabledAt;
    public boolean enabled;
    public boolean isMethodButton;
    public long clickedAt;
    public long disabledAt;
    public String buttonName;

    public ButtonSetting(String name, boolean enabled) {
        super(name);
        this.buttonName = name;
        this.enabled = enabled;
        this.updateLabel();
    }

    public ButtonSetting(String name) {
        super(name);
        this.buttonName = name;
        this.isMethodButton = true;
        this.notSaved();
        this.updateLabel();
    }

    public void updateLabel() {
        this.label = (this.isMethodButton ? "[=]  " : (this.isToggled() ? "[+]  " : "[-]  ")) + this.getName();
    }

    public String getLabel() {
        return this.label;
    }

    public boolean isMethodButton() {
        return this.isMethodButton;
    }

    public boolean isToggled() {
        return this.enabled;
    }

    public void setEnabled(boolean b) {
        this.enabled = b;
    }

    public void toggle() {
        if (this.isMethodButton()) {
            this.clickedAt = System.currentTimeMillis();
        } else {
            this.setEnabled(!this.enabled);
            if (this.enabled) {
                this.enabledAt = System.currentTimeMillis();
            } else {
                this.disabledAt = System.currentTimeMillis();
            }
            this.updateLabel();
            xyz.daikirai.client.Profiles.markModified();
        }
    }

    public void set(boolean b) {
        this.setEnabled(b);
        this.updateLabel();
    }
}
