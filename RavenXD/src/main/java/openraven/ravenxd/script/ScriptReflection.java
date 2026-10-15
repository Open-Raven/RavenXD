package xyz.daikirai.script;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ScriptReflection {
    public static boolean failed;

    public enum ReflectedField {
        WINDOW_ID(C0DPacketCloseWindow.class, "field_149556_a", "windowId"),
        FLY_SPEED(C13PacketPlayerAbilities.class, "field_149497_e", "flySpeed"),
        WALK_SPEED(C13PacketPlayerAbilities.class, "field_149495_f", "walkSpeed"),
        TITLE(GuiIngame.class, "field_175201_x", ""),
        SUBTITLE(GuiIngame.class, "field_175200_y", ""),
        TITLE_TIMER(GuiIngame.class, "field_175195_w", ""),
        HEADER(GuiPlayerTabOverlay.class, "field_175256_i", "header"),
        FOOTER(GuiPlayerTabOverlay.class, "field_175255_h", "footer");

        final Class<?> owner;
        final String srgName;
        final String name;
        Field field;

        ReflectedField(Class<?> owner, String srgName, String name) {
            this.owner = owner;
            this.srgName = srgName;
            this.name = name;
        }

        public Field get() {
            return this.field;
        }
    }

    public enum ReflectedMethod {
        SETUP_CAMERA_TRANSFORM(EntityRenderer.class, "func_78479_a", "setupCameraTransform", float.class, int.class);

        final Class<?> owner;
        final String srgName;
        final String name;
        final Class<?>[] params;
        Method method;

        ReflectedMethod(Class<?> owner, String srgName, String name, Class<?>... params) {
            this.owner = owner;
            this.srgName = srgName;
            this.name = name;
            this.params = params;
        }

        public Method get() {
            return this.method;
        }
    }

    public static final ReflectedField WINDOW_ID = ReflectedField.WINDOW_ID;
    public static final ReflectedField FLY_SPEED = ReflectedField.FLY_SPEED;
    public static final ReflectedField WALK_SPEED = ReflectedField.WALK_SPEED;
    public static final ReflectedField TITLE = ReflectedField.TITLE;
    public static final ReflectedField SUBTITLE = ReflectedField.SUBTITLE;
    public static final ReflectedField TITLE_TIMER = ReflectedField.TITLE_TIMER;
    public static final ReflectedField HEADER = ReflectedField.HEADER;
    public static final ReflectedField FOOTER = ReflectedField.FOOTER;
    public static final ReflectedMethod SETUP_CAMERA_TRANSFORM = ReflectedMethod.SETUP_CAMERA_TRANSFORM;

    private ScriptReflection() {
    }

    public static void init() {
        for (ReflectedField f : ReflectedField.values()) {
            if (!f.srgName.isEmpty()) {
                try {
                    f.field = f.owner.getDeclaredField(f.srgName);
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (f.field == null && !f.name.isEmpty()) {
                try {
                    f.field = f.owner.getDeclaredField(f.name);
                } catch (NoSuchFieldException e) {
                    failed = true;
                    return;
                }
            }
            f.field.setAccessible(true);
        }
        for (ReflectedMethod m : ReflectedMethod.values()) {
            if (!m.srgName.isEmpty()) {
                try {
                    m.method = m.owner.getDeclaredMethod(m.srgName, m.params);
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (m.method == null) {
                if (m.name.isEmpty()) {
                    continue;
                }
                try {
                    m.method = m.owner.getDeclaredMethod(m.name, m.params);
                } catch (NoSuchMethodException e) {
                    failed = true;
                    return;
                }
            }
            m.method.setAccessible(true);
        }
    }
}
