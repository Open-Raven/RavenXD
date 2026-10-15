package xyz.daikirai.module.render;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;

public class NoCameraClipModule extends Module {
    public static NoCameraClipModule instance;

    public NoCameraClipModule(int key) {
        super("NoCameraClip", Category.render, key);
        instance = this;
    }
}
