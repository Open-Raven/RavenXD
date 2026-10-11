package xyz.daikirai.module;

import xyz.daikirai.module.setting.SliderSetting;

public class AssistantModule extends Module {
    public enum Assistant {
        Daisy("&d", 2.5f), Jamal("&6", 0.8f), Simon("&3", 1.0f);
        public final String color;
        public final float pitch;

        Assistant(String color, float pitch) {
            this.color = color;
            this.pitch = pitch;
        }
    }

    public static Assistant assistant = Assistant.Daisy;
    public static SliderSetting assistantSetting;

    public AssistantModule(int key) {
        super("Assistant", Category.client, key);
    }

    public void link() {
        assistantSetting = (SliderSetting) ModuleManager.setting(this, "Assistant");
    }

    @Override
    public void guiUpdate() {
        assistant = Assistant.values()[(int) assistantSetting.getInput()];
    }
}
