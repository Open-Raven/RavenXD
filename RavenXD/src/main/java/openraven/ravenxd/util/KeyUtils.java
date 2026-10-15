package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public final class KeyUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean simulatingMouse;

    private KeyUtils() {
    }

    public static boolean isDown(KeyBinding keyBinding) {
        return GameSettings.isKeyDown(keyBinding);
    }

    public static boolean isDown(int code) {
        return code != 0 && (code < 0 ? Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code));
    }

    public static void press(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), true);
        KeyBinding.onTick(keyBinding.getKeyCode());
    }

    public static void release(KeyBinding keyBinding) {
        KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
    }

    public static void click(KeyBinding keyBinding) {
        press(keyBinding);
        release(keyBinding);
    }

    public static int heldHotbarKey() {
        for (int i = 0; i < 9; ++i) {
            if (!isDown(mc.gameSettings.keyBindsHotbar[i])) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static Field eventButton, eventButtonState, mouseButtons;

    public static boolean simulateMouse(int button, boolean state) {
        boolean notCancelled = true;
        try {
            if (eventButton == null) {
                eventButton = ReflectionHelper.findField(MouseEvent.class, "button");
                eventButtonState = ReflectionHelper.findField(MouseEvent.class, "buttonstate");
                mouseButtons = ReflectionHelper.findField(Mouse.class, "buttons");
            }
            MouseEvent mouseEvent = new MouseEvent();
            eventButton.set(mouseEvent, button);
            eventButtonState.set(mouseEvent, state);
            simulatingMouse = true;
            notCancelled = !MinecraftForge.EVENT_BUS.post(mouseEvent);
            simulatingMouse = false;
            if (notCancelled) {
                ByteBuffer byteBuffer = (ByteBuffer) mouseButtons.get(null);
                if (button >= 0 && button < byteBuffer.limit()) {
                    byteBuffer.put(button, (byte) (state ? 1 : 0));
                }
            }
        } catch (IllegalAccessException illegalAccessException) {
        }
        return notCancelled;
    }
}
