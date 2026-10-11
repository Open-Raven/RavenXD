package xyz.daikirai.module.player;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

public class AntiAFKModule extends Module {
    public static AntiAFKModule instance;

    public enum Afk {
        None, Shuffle, Forward, Backward, Wander
    }

    public enum Spin {
        None, Random, Right, Left
    }

    public static SliderSetting afkMode, spinMode, minDelay, maxDelay;
    public static ButtonSetting jump, jumpOnlyWhenCollided, randomClicks, swapItem, randomizeDelta, randomizePitch;
    public static Afk afk = Afk.Shuffle;
    public static Spin spin = Spin.None;
    public static boolean flip;
    private int ticks;

    public AntiAFKModule(int key) {
        super("AntiAFK", Category.player, key);
        instance = this;
    }

    public void link() {
        afkMode = (SliderSetting) ModuleManager.setting(this, "AFK");
        jump = (ButtonSetting) ModuleManager.setting(this, "Jump");
        jumpOnlyWhenCollided = (ButtonSetting) ModuleManager.setting(this, "Jump only when collided");
        randomClicks = (ButtonSetting) ModuleManager.setting(this, "Random clicks");
        swapItem = (ButtonSetting) ModuleManager.setting(this, "Swap item");
        spinMode = (SliderSetting) ModuleManager.setting(this, "Spin");
        randomizeDelta = (ButtonSetting) ModuleManager.setting(this, "Randomize delta");
        randomizePitch = (ButtonSetting) ModuleManager.setting(this, "Randomize pitch");
        minDelay = (SliderSetting) ModuleManager.setting(this, "Minimum delay ticks");
        maxDelay = (SliderSetting) ModuleManager.setting(this, "Maximum delay ticks");
    }

    public static void pressKeys() {
        Minecraft mc = Minecraft.getMinecraft();
        switch (afk) {
            case Shuffle:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), flip);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), !flip);
                break;
            case Forward:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                break;
            case Backward:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                break;
            case Wander:
                if (flip) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), MathUtils.RANDOM.nextBoolean());
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), MathUtils.RANDOM.nextBoolean());
                    break;
                }
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), MathUtils.RANDOM.nextBoolean());
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), MathUtils.RANDOM.nextBoolean());
                break;
            default:
                break;
        }
    }

    private double yawDelta(boolean positive) {
        int n = positive ? 1 : -1;
        if (!randomizeDelta.enabled) {
            return 3 * n;
        }
        double d = (double) MathUtils.randomInt(100, 1000) / 100.0;
        if (d % 1.0 == 0.0) {
            d += (double) MathUtils.randomInt(1, 10) / 10.0 * (double) n;
        }
        return d * (double) n;
    }

    private double pitchDelta() {
        int n = MathUtils.RANDOM.nextBoolean() ? 1 : -1;
        if (!randomizeDelta.enabled) {
            return 2 * n;
        }
        double d = (double) MathUtils.randomInt(100, 500) / 100.0;
        if (d % 1.0 == 0.0) {
            d += (double) MathUtils.randomInt(1, 10) / 10.0 * (double) n;
        }
        return d * (double) n;
    }

    @Override
    public void onPostPlayerInput() {
        Minecraft mc = Minecraft.getMinecraft();
        if (jump.enabled && (!jumpOnlyWhenCollided.enabled || mc.thePlayer.isCollidedHorizontally)) {
            mc.thePlayer.movementInput.jump = true;
        }
    }

    private void releaseKeys(Afk a) {
        Minecraft mc = Minecraft.getMinecraft();
        switch (a) {
            case Shuffle:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                break;
            case Forward:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                break;
            case Backward:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                break;
            case Wander:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onEnable() {
        this.ticks = this.nextDelay();
        flip = MathUtils.RANDOM.nextBoolean();
    }

    private int nextDelay() {
        if (minDelay.getInput() == maxDelay.getInput()) {
            return (int) minDelay.getInput();
        }
        return MathUtils.randomInt((int) minDelay.getInput(), (int) maxDelay.getInput());
    }

    @Override
    public String getInfo() {
        if (afk == Afk.None) {
            if (spin != Spin.None) {
                return spinMode.getDisplay(false);
            }
            if (jump.enabled) {
                return "Jump";
            }
            if (randomClicks.enabled) {
                return "Click";
            }
            if (swapItem.enabled) {
                return "Swap";
            }
        }
        return afkMode.getDisplay(false);
    }

    @Override
    public void onDisable() {
        this.releaseKeys(afk);
    }

    private void spinPitch() {
        Minecraft mc = Minecraft.getMinecraft();
        if (randomizePitch.enabled) {
            mc.thePlayer.rotationPitch = MathHelper.clamp_float((float) ((double) mc.thePlayer.rotationPitch + this.pitchDelta()), -90.0f, 90.0f);
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (Overlay.screenOpen()) {
            return;
        }
        --this.ticks;
        pressKeys();
        switch (spin) {
            case Random:
                mc.thePlayer.rotationYaw = (float) ((double) mc.thePlayer.rotationYaw + this.yawDelta(flip));
                this.spinPitch();
                break;
            case Right:
                mc.thePlayer.rotationYaw = (float) ((double) mc.thePlayer.rotationYaw + this.yawDelta(true));
                this.spinPitch();
                break;
            case Left:
                mc.thePlayer.rotationYaw = (float) ((double) mc.thePlayer.rotationYaw + this.yawDelta(false));
                this.spinPitch();
                break;
            default:
                break;
        }
        if (this.ticks == 0) {
            if (swapItem.enabled) {
                mc.thePlayer.inventory.currentItem = MathUtils.randomInt(0, 8);
            }
            if (randomClicks.enabled) {
                PlayerUtils.clickMouse();
            }
            this.ticks = this.nextDelay();
            if (flip) {
                flip = false;
            }
        }
    }

    @Override
    public void guiUpdate() {
        SliderSetting.order(minDelay, maxDelay);
        if ((double) afk.ordinal() != afkMode.getInput()) {
            if (this.isEnabled()) {
                this.releaseKeys(afk);
            }
            afk = Afk.values()[(int) afkMode.getInput()];
        }
        spin = Spin.values()[(int) spinMode.getInput()];
    }
}
