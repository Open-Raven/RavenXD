package xyz.daikirai.module.setting;

public class DescriptionSetting extends Setting {
    public String desc;

    public DescriptionSetting(String desc) {
        super(desc);
        this.desc = desc;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
