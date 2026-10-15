package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public final class Zoom {
    private static KeyBinding zoomKey;

    private Zoom() {
    }

    public static void find() {
        for (KeyBinding k : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (!k.getKeyDescription().equals("of.key.zoom")) continue;
            zoomKey = k;
        }
    }

    public static boolean isZooming() {
        return zoomKey != null && GameSettings.isKeyDown(zoomKey);
    }
}
