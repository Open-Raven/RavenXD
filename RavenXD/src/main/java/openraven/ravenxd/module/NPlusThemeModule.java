package openraven.ravenxd.module;

import openraven.ravenxd.gui.nplus.NPlusCategory;
import openraven.ravenxd.gui.nplus.NPlusGui;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;

public class NPlusThemeModule extends Module {
    public enum SliderStyle {
        Normal, Round, Outlines, Vape
    }

    public static final String[] SOUNDS = {"random.click", "random.bowhit", "game.player.hurt", "random.chestopen", "random.explode"};

    public static SliderSetting theme, categoryOffset, soundVolume, soundPitch, soundMode, underlineExpand, sliderStyle, sliderRound;
    public static ButtonSetting customFont, background, player, logo, blur, clock, date, commandPrompt, clean, sounds, lowercase, underline;

    public NPlusThemeModule(int key) {
        super("N+ Theme", Category.client, key);
    }

    public void link() {
        customFont = (ButtonSetting) ModuleManager.setting(this, "Custom font");
        theme = (SliderSetting) ModuleManager.setting(this, "Theme");
        background = (ButtonSetting) ModuleManager.setting(this, "Background");
        player = (ButtonSetting) ModuleManager.setting(this, "Player");
        logo = (ButtonSetting) ModuleManager.setting(this, "Logo");
        blur = (ButtonSetting) ModuleManager.setting(this, "Blur");
        clock = (ButtonSetting) ModuleManager.setting(this, "Clock");
        date = (ButtonSetting) ModuleManager.setting(this, "Date");
        commandPrompt = (ButtonSetting) ModuleManager.setting(this, "Command prompt");
        clean = (ButtonSetting) ModuleManager.setting(this, "Clean");
        categoryOffset = (SliderSetting) ModuleManager.setting(this, "Category BG offset");
        sounds = (ButtonSetting) ModuleManager.setting(this, "Sounds");
        soundVolume = (SliderSetting) ModuleManager.setting(this, "Sound volume");
        soundPitch = (SliderSetting) ModuleManager.setting(this, "Sound pitch");
        soundMode = (SliderSetting) ModuleManager.setting(this, "Sound mode");
        lowercase = (ButtonSetting) ModuleManager.setting(this, "Lowercase");
        underline = (ButtonSetting) ModuleManager.setting(this, "Underline");
        underlineExpand = (SliderSetting) ModuleManager.setting(this, "Underline expand");
        sliderStyle = (SliderSetting) ModuleManager.setting(this, "Slider style");
        sliderRound = (SliderSetting) ModuleManager.setting(this, "Slider round range");
    }

    public static SliderStyle sliderStyle() {
        return SliderStyle.values()[(int) sliderStyle.getInput()];
    }

    public static boolean font() {
        return customFont.isToggled();
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == clean && NPlusGui.instance != null) {
            int l = 50, j = l / 2;
            for (NPlusCategory c : NPlusGui.instance.categories) {
                c.setX(c.getX() / l * l + (c.getX() % l > j ? l : 0));
                c.setY(c.getY() / l * l + (c.getY() % l > j ? l : 0));
            }
        }
    }
}
