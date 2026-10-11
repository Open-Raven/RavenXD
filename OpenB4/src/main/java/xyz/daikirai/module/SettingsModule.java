package xyz.daikirai.module;

import xyz.daikirai.gui.InfoHudEditor;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Theme;
import net.minecraft.util.ResourceLocation;

public class SettingsModule extends Module {
    public enum Cape {
        None(null),
        Purple("rvn_purple"),
        Red("rvn_red"),
        Green("rvn_green"),
        Yellow("rvn_yellow"),
        Aqua("rvn_aqua"),
        Anime("anime"),
        Jewstein("jewstein");

        public final ResourceLocation texture;

        Cape(String file) {
            this.texture = file == null ? null : new ResourceLocation("keystrokesmod", "textures/capes/" + file + ".png");
        }
    }

    public enum Background {
        None, Rect, Round
    }

    public static Cape cape = Cape.values()[1 + (int) ((double) (Cape.values().length - 1) * Math.random())];
    public static Background background = Background.Round;

    public static SliderSetting customCape, defaultTheme, infoHudBackground;
    public static SliderSetting themeOffset;
    public static ButtonSetting addBracketsToDistance, middleClickFriends, editPosition, healthAsHearts, heartSymbol, chatAsInventory;
    public static ButtonSetting movementFix, rotateBody, fullBody;
    public static ButtonSetting disableModulesOnLoad;
    public static SliderSetting jitterYaw, aimClosestPoint, hittableHurtTime;
    public static ButtonSetting hideFirstPersonSelfEsp;
    public static ButtonSetting weaponAxe, weaponEnchanted, weaponFist, weaponHoe, weaponRod, weaponShovel, weaponStick;

    public SettingsModule(int key) {
        super("Settings", Category.client, key);
    }

    public void link() {
        customCape = (SliderSetting) ModuleManager.setting(this, "Custom cape");
        disableModulesOnLoad = (ButtonSetting) ModuleManager.setting(this, "Disable modules on load");
        disableModulesOnLoad.notSaved();
        defaultTheme = (SliderSetting) ModuleManager.setting(this, "Default theme");
        infoHudBackground = (SliderSetting) ModuleManager.setting(this, "Background");
        addBracketsToDistance = (ButtonSetting) ModuleManager.setting(this, "Add brackets to distance");
        middleClickFriends = (ButtonSetting) ModuleManager.setting(this, "Middle click friends");
        editPosition = (ButtonSetting) ModuleManager.setting(this, "Edit position");
        healthAsHearts = (ButtonSetting) ModuleManager.setting(this, "Show health as hearts");
        heartSymbol = (ButtonSetting) ModuleManager.setting(this, "Show heart symbol");
        chatAsInventory = (ButtonSetting) ModuleManager.setting(this, "Set chat as inventory");
        themeOffset = (SliderSetting) ModuleManager.setting(this, "Offset");
        movementFix = (ButtonSetting) ModuleManager.setting(this, "Movement fix");
        rotateBody = (ButtonSetting) ModuleManager.setting(this, "Rotate body");
        fullBody = (ButtonSetting) ModuleManager.setting(this, "Full body");
        jitterYaw = (SliderSetting) ModuleManager.setting(this, "Jitter yaw");
        aimClosestPoint = (SliderSetting) ModuleManager.setting(this, "Aim closest point");
        hittableHurtTime = (SliderSetting) ModuleManager.setting(this, "Hittable hurt time");
        hideFirstPersonSelfEsp = (ButtonSetting) ModuleManager.setting(this, "Hide first person self ESP");
        weaponAxe = (ButtonSetting) ModuleManager.setting(this, "Axe");
        weaponEnchanted = (ButtonSetting) ModuleManager.setting(this, "Enchanted");
        weaponFist = (ButtonSetting) ModuleManager.setting(this, "Fist");
        weaponHoe = (ButtonSetting) ModuleManager.setting(this, "Hoe");
        weaponRod = (ButtonSetting) ModuleManager.setting(this, "Rod");
        weaponShovel = (ButtonSetting) ModuleManager.setting(this, "Shovel");
        weaponStick = (ButtonSetting) ModuleManager.setting(this, "Stick");
        Theme.timeMultiplier = (SliderSetting) ModuleManager.setting(this, "Time multiplier");
        customCape.setValueRaw(cape.ordinal());
        defaultTheme.setValueRaw(Theme.defaultTheme.ordinal() - 1);
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == editPosition) {
            mc.displayGuiScreen(new InfoHudEditor());
        }
    }

    @Override
    public void guiUpdate() {
        cape = Cape.values()[(int) customCape.getInput()];
        Theme.defaultTheme = Theme.SELECTABLE[(int) defaultTheme.getInput()];
        background = Background.values()[(int) infoHudBackground.getInput()];
    }
}
