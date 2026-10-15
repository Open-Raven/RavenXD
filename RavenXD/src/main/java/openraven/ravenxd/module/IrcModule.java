package xyz.daikirai.module;

import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;

public class IrcModule extends Module {
    public enum Prefix {
        Backslash('\\', 43), Equals('=', 13), Hashtag('#', 40), Hyphen('-', 12), None(' ', 0);
        public final char c;
        public final int key;

        Prefix(char c, int key) {
            this.c = c;
            this.key = key;
        }
    }

    public static Prefix prefix = Prefix.Backslash;
    public static SliderSetting prefixSetting;
    public static ButtonSetting anonymous, filterBadWords, mentionSound, messageSound, receiveMessages, sendMessages;

    public IrcModule(int key) {
        super("IRC", Category.client, key);
    }

    public void link() {
        prefixSetting = (SliderSetting) ModuleManager.setting(this, "Prefix");
        anonymous = (ButtonSetting) ModuleManager.setting(this, "Anonymous");
        filterBadWords = (ButtonSetting) ModuleManager.setting(this, "Filter bad words");
        mentionSound = (ButtonSetting) ModuleManager.setting(this, "Play mention sound");
        messageSound = (ButtonSetting) ModuleManager.setting(this, "Play message sound");
        receiveMessages = (ButtonSetting) ModuleManager.setting(this, "Receive messages");
        sendMessages = (ButtonSetting) ModuleManager.setting(this, "Send messages");
    }

    @Override
    public void guiUpdate() {
        prefix = Prefix.values()[(int) prefixSetting.getInput()];
    }
}
