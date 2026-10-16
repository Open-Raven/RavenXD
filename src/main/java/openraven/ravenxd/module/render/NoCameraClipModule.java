package openraven.ravenxd.module.render;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;

public class NoCameraClipModule extends Module {
    public static NoCameraClipModule instance;

    public NoCameraClipModule(int key) {
        super("NoCameraClip", Category.render, key);
        instance = this;
    }
}
