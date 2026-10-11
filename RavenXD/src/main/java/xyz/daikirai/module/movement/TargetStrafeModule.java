package xyz.daikirai.module.movement;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.combat.KillAuraModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.util.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Mouse;

public class TargetStrafeModule extends Module {
    public static SliderSetting radius, range, yawOffset;
    public static ButtonSetting onlyWhileMiddleClicking;
    private static int direction, pressed, flippedAt;

    public TargetStrafeModule(int key) {
        super("TargetStrafe", Category.movement, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        radius = (SliderSetting) ModuleManager.setting(this, "Radius");
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        yawOffset = (SliderSetting) ModuleManager.setting(this, "Yaw offset");
        onlyWhileMiddleClicking = (ButtonSetting) ModuleManager.setting(this, "Only while middle clicking");
    }

    public static boolean active() {
        Minecraft mc = mc();
        if (KillAuraModule.target == null || mc.thePlayer.movementInput.moveForward < 1.0f
                || onlyWhileMiddleClicking.isToggled() && !Mouse.isButtonDown(2) || RotationManager.movementFix()) {
            return false;
        }
        if (!mc.thePlayer.capabilities.isFlying && LongJumpModule.stage == -1) {
            if (PlayerUtils.heightAboveGround() >= 3.0) {
                return false;
            }
            for (int i = -1; i <= 1; i += 2) {
                if (PlayerUtils.heightAboveGround(mc.thePlayer.posX + (double) i, mc.thePlayer.posY, mc.thePlayer.posZ) >= 3.0) {
                    return false;
                }
                if (PlayerUtils.heightAboveGround(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ + (double) i) >= 3.0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        direction = -1;
        pressed = 0;
        flippedAt = 0;
    }

    @Override
    public Float getMovementYaw() {
        if (!active()) {
            return null;
        }
        Minecraft mc = mc();
        Entity entity = KillAuraModule.target.entity;
        double d = PlayerUtils.horizontalDistanceSq(entity.posX, entity.posZ);
        if (d > range.getInput() * range.getInput() + 2.0) {
            return null;
        }
        float f = RotationUtils.toEntity(entity)[0];
        if (d > radius.getInput() * radius.getInput() + 2.0) {
            return f;
        }
        if (mc.thePlayer.ticksExisted - flippedAt > 4 && mc.thePlayer.isCollidedHorizontally) {
            direction *= -1;
            flippedAt = mc.thePlayer.ticksExisted;
        }
        if (pressed != 0) {
            direction = pressed * -1;
            pressed = 0;
        }
        return f + (float) ((int) yawOffset.getInput() * direction);
    }

    @Override
    public void onPostPlayerInput() {
        if (!active()) {
            return;
        }
        Minecraft mc = mc();
        Entity entity = KillAuraModule.target.entity;
        if (PlayerUtils.horizontalDistanceSq(entity.posX, entity.posZ) > range.getInput() * range.getInput() + 2.0) {
            return;
        }
        if (mc.thePlayer.movementInput.moveStrafe != 0.0f) {
            pressed = (int) Math.signum(mc.thePlayer.movementInput.moveStrafe);
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
        }
    }
}
