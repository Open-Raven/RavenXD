package xyz.daikirai.module;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.DescriptionSetting;
import xyz.daikirai.module.setting.SliderSetting;

public class ChatCommandsModule extends Module {
    public enum Prefix {
        Period("."), Comma(","), Semicolon(";");
        public final String str;

        Prefix(String str) {
            this.str = str;
        }
    }

    public static int prefixIndex;
    public static Prefix prefixEnum = Prefix.Period;
    public static String prefix = Prefix.Period.str;
    public static DescriptionSetting desc;
    public static SliderSetting prefixSetting;
    public static ButtonSetting lowercase;

    public ChatCommandsModule(int key) {
        super("Chat Commands", Category.client, key);
    }

    public void link() {
        desc = (DescriptionSetting) this.getSettings().get(0);
        prefixSetting = (SliderSetting) ModuleManager.setting(this, "Prefix");
        lowercase = (ButtonSetting) ModuleManager.setting(this, "Lowercase");
    }

    @Override
    public void guiUpdate() {
        if (prefixIndex != (int) prefixSetting.getInput()) {
            prefixIndex = (int) prefixSetting.getInput();
            prefixEnum = Prefix.values()[prefixIndex];
            prefix = prefixEnum.str;
            desc.setDesc(Chat.color("Use &e" + prefix + "help&r for help."));
        }
    }

    @Override
    public void onDisable() {
        ChatCommands.pingSentAt = 0L;
        ChatCommands.tracking = null;
    }
}
