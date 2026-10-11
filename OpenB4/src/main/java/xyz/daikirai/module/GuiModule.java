package xyz.daikirai.module;

import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.gui.CategoryComponent;
import xyz.daikirai.gui.ClickGui;
import xyz.daikirai.gui.Component;
import xyz.daikirai.gui.ModuleComponent;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.GuiColor;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    public enum Scale {
        Small(1), Normal(2), Large(3);
        public final int guiScale;

        Scale(int guiScale) {
            this.guiScale = guiScale;
        }
    }

    public static GuiColor watermarkColor = new GuiColor(57, 146, 229, 255);
    public static GuiColor closedOutlineLeft = new GuiColor(81, 99, 149, 255);
    public static GuiColor closedOutlineRight = new GuiColor(97, 67, 133, 255);
    public static GuiColor categoryNameColor = new GuiColor(220, 220, 220, 255);
    public static GuiColor red = new GuiColor(250, 95, 85, 255);
    public static GuiColor green = new GuiColor(135, 238, 144, 255);
    public static GuiColor enabledColor = new GuiColor(24, 154, 255, 255);
    public static GuiColor disabledColor = new GuiColor(192, 192, 192, 255);
    public static GuiColor scriptColor = new GuiColor(116, 202, 255, 255);
    public static GuiColor errorColor = new GuiColor(255, 80, 80, 255);
    public static GuiColor darkFill = new GuiColor(0, 0, 0, 110);
    public static GuiColor lightFill = new GuiColor(0, 0, 0, 130);
    public static GuiColor categoryFill = darkFill;
    public static GuiColor rainbowLeft = new GuiColor(2L, 0L, 130);
    public static GuiColor rainbowRight = new GuiColor(2L, 700L, 130);
    public static GuiColor whiteOutline = new GuiColor(255, 255, 255, 150);
    public static GuiColor outlineLeft = rainbowLeft;
    public static GuiColor outlineRight = rainbowRight;
    public static Scale guiScale = Scale.Normal;
    public static float blurStrength = 1.7f;
    public static int previousGuiScale;
    public static boolean skipRestore;
    public static boolean movementActive;

    public static SliderSetting scale, backgroundBlur, scrollSpeed;
    public static ButtonSetting allowMovement, allowRotating, darkBackground, hidePlayerModel, hideWatermark, liteMode,
            rainbowOutlines, resetPositions, saveCategoryPositions;

    public GuiModule(int key) {
        super("Gui", Category.client, key);
    }

    public void link() {
        scale = (SliderSetting) ModuleManager.setting(this, "Scale");
        backgroundBlur = (SliderSetting) ModuleManager.setting(this, "Background blur");
        scrollSpeed = (SliderSetting) ModuleManager.setting(this, "Scroll speed");
        allowMovement = (ButtonSetting) ModuleManager.setting(this, "Allow movement");
        allowRotating = (ButtonSetting) ModuleManager.setting(this, "Allow rotating");
        darkBackground = (ButtonSetting) ModuleManager.setting(this, "Dark background");
        hidePlayerModel = (ButtonSetting) ModuleManager.setting(this, "Hide player model");
        hideWatermark = (ButtonSetting) ModuleManager.setting(this, "Hide watermark");
        liteMode = (ButtonSetting) ModuleManager.setting(this, "Lite mode");
        rainbowOutlines = (ButtonSetting) ModuleManager.setting(this, "Rainbow outlines");
        resetPositions = (ButtonSetting) ModuleManager.setting(this, "Reset positions");
        saveCategoryPositions = (ButtonSetting) ModuleManager.setting(this, "Save category positions");
        saveCategoryPositions.notSaved();
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == rainbowOutlines) {
            if (rainbowOutlines.isToggled()) {
                outlineLeft = rainbowLeft;
                outlineRight = rainbowRight;
                return;
            }
            outlineLeft = outlineRight = whiteOutline;
        } else if (b == darkBackground) {
            categoryFill = darkBackground.isToggled() ? darkFill : lightFill;
        } else if (b == resetPositions) {
            resetPositions();
        } else if (b == saveCategoryPositions) {
            xyz.daikirai.client.ClientConfig.save();
        }
    }

    public static void restoreScale() {
        if (skipRestore) {
            skipRestore = false;
            return;
        }
        mc.gameSettings.guiScale = previousGuiScale;
        RenderUtils.updateResolution();
    }

    @Override
    public boolean canBeEnabled() {
        if (mc.currentScreen == ClickGui.instance) {
            return false;
        }
        if (xyz.daikirai.module.movement.TimerModule.slowed() && xyz.daikirai.module.movement.TimerModule.instance.getKeycode() != 0) {
            Chat.moduleMessage(this, "&cDisable timer to open GUI!");
            return false;
        }
        return true;
    }

    @Override
    public void guiUpdate() {
        guiScale = Scale.values()[(int) scale.getInput()];
        blurStrength = (float) (3.4 * (backgroundBlur.getInput() / 100.0));
        updateMovement();
    }

    public static void updateMovement() {
        if (!(mc.currentScreen instanceof ClickGui)) {
            movementActive = false;
            return;
        }
        boolean consoleClosed = !CommandLineModule.open;
        boolean move = allowMovement.isToggled() && consoleClosed;
        boolean rotate = allowRotating.isToggled() && consoleClosed;
        if (move) {
            setMoveKeys(false);
            if (ModuleManager.enabled("AntiAFK") != null) {
                xyz.daikirai.module.player.AntiAFKModule.pressKeys();
            }
        } else if (movementActive) {
            setMoveKeys(true);
        }
        if (rotate) {
            rotate();
        }
        movementActive = move;
    }

    private static void setMoveKeys(boolean release) {
        KeyBinding[] keys = {mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSneak};
        for (KeyBinding k : keys) {
            KeyBinding.setKeyBindState(k.getKeyCode(), !release && GameSettings.isKeyDown(k));
        }
    }

    private static void rotate() {
        float yaw = 0.0f, pitch = 0.0f;
        if (Keyboard.isKeyDown(205)) yaw += 8.0f;
        if (Keyboard.isKeyDown(203)) yaw -= 8.0f;
        if (Keyboard.isKeyDown(200)) pitch -= 5.0f;
        if (Keyboard.isKeyDown(208)) pitch += 5.0f;
        if (yaw != 0.0f) {
            yaw = (float) ((double) yaw + (Math.random() - 0.5));
            mc.thePlayer.rotationYaw += yaw;
        }
        if (pitch != 0.0f) {
            pitch = (float) ((double) pitch + (Math.random() - 0.5));
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitch, -90.0f, 90.0f);
        }
    }

    public static void applyScale() {
        previousGuiScale = mc.gameSettings.guiScale;
        if (previousGuiScale != guiScale.guiScale) {
            mc.gameSettings.guiScale = guiScale.guiScale;
        }
        RenderUtils.updateResolution();
    }

    public static void resetPositions() {
        float y = 5.0f;
        for (CategoryComponent c : ClickGui.categories) {
            for (Component comp : c.modules) {
                ModuleComponent m = (ModuleComponent) comp;
                if (!m.opened) continue;
                m.toggleOpen();
            }
            c.setX(5.0f);
            c.setY(y);
            c.setOpened(false);
            y += 20.0f;
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null && mc.theWorld != null && mc.currentScreen != ClickGui.instance) {
            if (xyz.daikirai.module.movement.TimerModule.speed > 0.0f) {
                MoveUtils.resetTimer();
            }
            applyScale();
            mc.displayGuiScreen(ClickGui.instance);
            ClickGui.instance.initMain();
        }
        this.disable();
    }
}
