package openraven.ravenxd.module;

import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.client.Chat;
import openraven.ravenxd.gui.CategoryComponent;
import openraven.ravenxd.gui.ClickGui;
import openraven.ravenxd.gui.Component;
import openraven.ravenxd.gui.ModuleComponent;
import openraven.ravenxd.gui.nplus.NPlusGui;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.GuiColor;
import openraven.ravenxd.util.RenderUtils;
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

    public static SliderSetting style, scale, backgroundBlur, scrollSpeed;
    public static ButtonSetting allowMovement, allowRotating, darkBackground, hidePlayerModel, hideWatermark, liteMode,
            rainbowOutlines, resetPositions, saveCategoryPositions;
    // extras
    public static SliderSetting waifu, soundVolume, soundPitch, soundMode;
    public static ButtonSetting clock, date, sounds;
    // haru
    public static SliderSetting haruColorMode, haruHue, haruSaturation, haruBrightness;
    // n+lite
    public static SliderSetting nplusTheme, categoryOffset, underlineExpand, sliderStyle, sliderRound;
    public static ButtonSetting customFont, nplusLogo, commandPrompt, clean, lowercase, underline;

    public enum Style {
        RavenXD, NPlus, Haru
    }

    public enum SliderStyle {
        Normal, Round, Outlines, Vape
    }

    public static final String[] SOUNDS = {"random.click", "random.bowhit", "game.player.hurt", "random.chestopen", "random.explode"};
    private static final GuiColor DEFAULT_CLOSED_LEFT = closedOutlineLeft, DEFAULT_CLOSED_RIGHT = closedOutlineRight,
            DEFAULT_NAME = categoryNameColor, DEFAULT_ENABLED = enabledColor;
    public static final GuiColor haruColor = new GuiColor(255, 190, 190, 255);

    public GuiModule(int key) {
        super("Gui", Category.client, key);
    }

    public void link() {
        style = (SliderSetting) ModuleManager.setting(this, "Style");
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
        waifu = (SliderSetting) ModuleManager.setting(this, "Waifu");
        clock = (ButtonSetting) ModuleManager.setting(this, "Clock");
        date = (ButtonSetting) ModuleManager.setting(this, "Date");
        sounds = (ButtonSetting) ModuleManager.setting(this, "Sounds");
        soundVolume = (SliderSetting) ModuleManager.setting(this, "Sound volume");
        soundPitch = (SliderSetting) ModuleManager.setting(this, "Sound pitch");
        soundMode = (SliderSetting) ModuleManager.setting(this, "Sound mode");
        haruColorMode = (SliderSetting) ModuleManager.setting(this, "Haru color");
        haruHue = (SliderSetting) ModuleManager.setting(this, "Haru hue");
        haruSaturation = (SliderSetting) ModuleManager.setting(this, "Haru saturation");
        haruBrightness = (SliderSetting) ModuleManager.setting(this, "Haru brightness");
        customFont = (ButtonSetting) ModuleManager.setting(this, "Custom font");
        nplusTheme = (SliderSetting) ModuleManager.setting(this, "N+ theme");
        nplusLogo = (ButtonSetting) ModuleManager.setting(this, "N+ logo");
        commandPrompt = (ButtonSetting) ModuleManager.setting(this, "Command prompt");
        clean = (ButtonSetting) ModuleManager.setting(this, "Clean");
        categoryOffset = (SliderSetting) ModuleManager.setting(this, "Category BG offset");
        lowercase = (ButtonSetting) ModuleManager.setting(this, "Lowercase");
        underline = (ButtonSetting) ModuleManager.setting(this, "Underline");
        underlineExpand = (SliderSetting) ModuleManager.setting(this, "Underline expand");
        sliderStyle = (SliderSetting) ModuleManager.setting(this, "Slider style");
        sliderRound = (SliderSetting) ModuleManager.setting(this, "Slider round range");
    }

    public static Style style() {
        return Style.values()[(int) style.getInput()];
    }

    public static SliderStyle sliderStyle() {
        return SliderStyle.values()[(int) sliderStyle.getInput()];
    }

    // nplus font
    public static boolean font() {
        return customFont.isToggled();
    }

    // category sound
    // TODO: add sigma remap sounds optionally
    public static void playCategorySound(boolean opened) {
        if (sounds == null || !sounds.isToggled() || !Chat.inWorld()) {
            return;
        }
        int mode = (int) soundMode.getInput();
        String s = mode == 3 && !opened ? "random.chestclosed" : SOUNDS[mode];
        openraven.ravenxd.client.Sounds.play(s, (float) soundVolume.getInput(), (float) soundPitch.getInput());
    }

    // haru colors
    public static int haruRgb() {
        switch ((int) haruColorMode.getInput()) {
            case 0: return gradient(new java.awt.Color(255, 190, 190), new java.awt.Color(255, 190, 255), 2, false);
            case 2: return java.awt.Color.getHSBColor((float) (System.currentTimeMillis() % 5000L) / 5000.0f, 1.0f, 1.0f).getRGB();
            case 3: return gradient3(new java.awt.Color(255, 0, 255), new java.awt.Color(255, 255, 0), new java.awt.Color(255, 0, 158), 2);
            case 4: return gradient3(new java.awt.Color(76, 56, 108), new java.awt.Color(255, 51, 51), new java.awt.Color(76, 56, 108), 5);
            case 5: return gradient(new java.awt.Color(255, 0, 0), new java.awt.Color(0, 0, 255), 7, true);
            default: return java.awt.Color.getHSBColor((float) (haruHue.getInput() % 360.0) / 360.0f, (float) haruSaturation.getInput(), (float) haruBrightness.getInput()).getRGB();
        }
    }

    // haru skid helpers
    private static int gradient(java.awt.Color a, java.awt.Color b, int y, boolean cosine) {
        double t = System.currentTimeMillis() / 600.0 - y * 0.06;
        double p = cosine ? Math.cos(t) * 0.5 + 0.5 : Math.sin(t) * 0.5 + 0.5;
        int green = cosine ? (int) (b.getGreen() + (a.getGreen() - b.getGreen()) * p) : (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * p);
        return new java.awt.Color((int) (a.getRed() + (b.getRed() - a.getRed()) * p), green, (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * p)).getRGB();
    }
    private static int gradient3(java.awt.Color a, java.awt.Color b, java.awt.Color c, int y) {
        double p = Math.sin(System.currentTimeMillis() / 600.0 - y * 0.06) * 0.5 + 0.5;
        return new java.awt.Color((int) (a.getRed() + (b.getRed() - a.getRed()) * p), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * p), c.getBlue()).getRGB();
    }
    public static void applyStyleColors() {
        if (style() == Style.Haru) {
            haruColor.set(haruRgb());
            outlineLeft = outlineRight = closedOutlineLeft = closedOutlineRight = haruColor;
            categoryNameColor = enabledColor = haruColor;
            return;
        }
        outlineLeft = rainbowOutlines.isToggled() ? rainbowLeft : whiteOutline;
        outlineRight = rainbowOutlines.isToggled() ? rainbowRight : whiteOutline;
        closedOutlineLeft = DEFAULT_CLOSED_LEFT;
        closedOutlineRight = DEFAULT_CLOSED_RIGHT;
        categoryNameColor = DEFAULT_NAME;
        enabledColor = DEFAULT_ENABLED;
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
            openraven.ravenxd.client.ClientConfig.save();
        } else if (b == clean && NPlusGui.instance != null) {
            int l = 50, j = l / 2;
            for (openraven.ravenxd.gui.nplus.NPlusCategory c : NPlusGui.instance.categories) {
                c.setX(c.getX() / l * l + (c.getX() % l > j ? l : 0));
                c.setY(c.getY() / l * l + (c.getY() % l > j ? l : 0));
            }
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
        if (ClickGui.isOpen(mc.currentScreen)) {
            return false;
        }
        if (openraven.ravenxd.module.movement.TimerModule.slowed() && openraven.ravenxd.module.movement.TimerModule.instance.getKeycode() != 0) {
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
        if (!ClickGui.isOpen(mc.currentScreen)) {
            movementActive = false;
            return;
        }
        boolean consoleClosed = !CommandLineModule.open;
        boolean move = allowMovement.isToggled() && consoleClosed;
        boolean rotate = allowRotating.isToggled() && consoleClosed;
        if (move) {
            setMoveKeys(false);
            if (ModuleManager.enabled("AntiAFK") != null) {
                openraven.ravenxd.module.player.AntiAFKModule.pressKeys();
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
        NPlusGui.instance.resetPositions();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null && mc.theWorld != null && !ClickGui.isOpen(mc.currentScreen)) {
            if (openraven.ravenxd.module.movement.TimerModule.speed > 0.0f) {
                MoveUtils.resetTimer();
            }
            applyScale();
            if (style() == Style.NPlus) {
                mc.displayGuiScreen(NPlusGui.instance);
                NPlusGui.instance.initMain();
            } else {
                mc.displayGuiScreen(ClickGui.instance);
                ClickGui.instance.initMain();
            }
        }
        this.disable();
    }
}
