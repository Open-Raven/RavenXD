package xyz.daikirai.module.render;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;

public class AntiShuffleModule extends Module {
    public static AntiShuffleModule instance;
    public static final String OBFUSCATED = "§k";

    public AntiShuffleModule(int key) {
        super("AntiShuffle", Category.render, key);
        instance = this;
    }
}
