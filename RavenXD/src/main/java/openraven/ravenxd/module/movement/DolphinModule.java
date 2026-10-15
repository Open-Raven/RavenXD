package openraven.ravenxd.module.movement;

import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

public class DolphinModule extends Module {
    public static DolphinModule instance;
    public static SliderSetting horizontalSpeed, verticalSpeed;
    public static ButtonSetting buoyant, disableWhileUsing, disableVerticalWhileMoving, forwardOnly;

    public DolphinModule(int key) {
        super("Dolphin", Category.movement, key);
        instance = this;
    }

    public void link() {
        horizontalSpeed = (SliderSetting) ModuleManager.setting(this, "Horizontal speed");
        verticalSpeed = (SliderSetting) ModuleManager.setting(this, "Vertical speed");
        buoyant = (ButtonSetting) ModuleManager.setting(this, "Buoyant");
        disableWhileUsing = (ButtonSetting) ModuleManager.setting(this, "Disable while using");
        disableVerticalWhileMoving = (ButtonSetting) ModuleManager.setting(this, "Disable vertical while moving");
        forwardOnly = (ButtonSetting) ModuleManager.setting(this, "Forward only");
    }

    @Override
    public void onPostPlayerInput() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.thePlayer.isInWater() || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (disableWhileUsing.enabled && mc.thePlayer.isUsingItem()) {
            return;
        }
        if (forwardOnly.enabled && !GameSettings.isKeyDown(mc.gameSettings.keyBindForward)) {
            return;
        }
        if (buoyant.enabled) {
            mc.thePlayer.motionY = 0.0;
        }
        if (PlayerUtils.isMoving()) {
            double d = 0.078 * horizontalSpeed.getInput();
            MoveUtils.strafe(d);
            if (disableVerticalWhileMoving.enabled) {
                return;
            }
        }
        if (GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
            mc.thePlayer.motionY = 0.02 + 0.04 * verticalSpeed.getInput();
        } else if (!mc.thePlayer.onGround && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.thePlayer.movementInput.sneak = false;
            mc.thePlayer.motionY = -0.1 - 0.03 * verticalSpeed.getInput();
        } else if (buoyant.enabled) {
            mc.thePlayer.motionY = 0.0;
        }
    }
}
