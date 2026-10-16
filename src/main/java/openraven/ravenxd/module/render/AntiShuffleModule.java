package openraven.ravenxd.module.render;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;

public class AntiShuffleModule extends Module {
    public static AntiShuffleModule instance;
    public static final String OBFUSCATED = "§k";

    public AntiShuffleModule(int key) {
        super("AntiShuffle", Category.render, key);
        instance = this;
    }
}
