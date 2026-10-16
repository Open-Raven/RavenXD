package openraven.ravenxd.jade.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class ClientUtils {
    private ClientUtils() {
    }

    public static boolean isInWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && mc.theWorld != null;
    }

    public static boolean xusXfhC(KeyBinding key) {
        int code = key.getKeyCode();
        return code < 0 ? Mouse.isButtonDown(code + 100) : code > 0 && Keyboard.isKeyDown(code);
    }
}
