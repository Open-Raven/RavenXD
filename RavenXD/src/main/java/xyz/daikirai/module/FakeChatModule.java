package xyz.daikirai.module;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;

public class FakeChatModule extends Module {
    public FakeChatModule(Category category, int key) {
        super("Fake Chat", category, key);
    }

    @Override
    public void onEnable() {
        String s = ChatCommands.fakeChatMessage;
        if (s.contains("\\n")) {
            for (String line : s.split("\\\\n")) {
                Chat.raw(line);
            }
        } else {
            Chat.raw(s);
        }
        this.disable();
    }
}
