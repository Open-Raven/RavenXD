package xyz.daikirai.module.combat;

import xyz.daikirai.util.ClickUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.awt.AWTException;
import java.awt.Robot;

public class ClickAssistModule extends Module {
    public static SliderSetting chanceLeft, chanceRight;
    public static ButtonSetting disableInCreative, disableInReplays, leftClick, weaponOnly, onlyWhileTargeting, rightClick, blocksOnly, above5Cps;
    private Robot robot;
    private boolean robotLeft, robotRight;

    public ClickAssistModule(int key) {
        super("ClickAssist", Category.combat, key);
    }

    public void link() {
        disableInCreative = (ButtonSetting) ModuleManager.setting(this, "Disable in creative");
        disableInReplays = (ButtonSetting) ModuleManager.setting(this, "Disable in replays");
        leftClick = (ButtonSetting) ModuleManager.setting(this, "Left click");
        chanceLeft = (SliderSetting) ModuleManager.setting(this, "Chance left");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
        onlyWhileTargeting = (ButtonSetting) ModuleManager.setting(this, "Only while targeting");
        rightClick = (ButtonSetting) ModuleManager.setting(this, "Right click");
        chanceRight = (SliderSetting) ModuleManager.setting(this, "Chance right");
        blocksOnly = (ButtonSetting) ModuleManager.setting(this, "Blocks only");
        above5Cps = (ButtonSetting) ModuleManager.setting(this, "Above 5 cps");
    }

    private void release(int button) {
        if (button == 0) {
            if (this.robotLeft && !Mouse.isButtonDown(0)) {
                this.robot.mouseRelease(16);
            }
        } else if (button == 1 && this.robotRight && !Mouse.isButtonDown(1)) {
            this.robot.mouseRelease(4);
        }
    }

    @Override
    public void onDisable() {
        this.robotRight = false;
        this.robotLeft = false;
        this.robot = null;
    }

    @Override
    public void onEnable() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            this.disable();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(MouseEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.button < 0 || !e.buttonstate || !Chat.inWorld() || AutoClickerModule.clicking()) {
            return;
        }
        if (disableInCreative.enabled && mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        if (disableInReplays.enabled && Hypixel.state == Hypixel.State.REPLAY) {
            return;
        }
        if (mc.currentScreen != null || mc.thePlayer.isEating()) {
            this.release(0);
            this.release(1);
            return;
        }
        if (e.button == 0 && leftClick.enabled) {
            if (this.robotLeft) {
                this.robotLeft = false;
            } else {
                double d;
                if (weaponOnly.enabled && !PlayerUtils.holdingWeapon()) {
                    return;
                }
                if (onlyWhileTargeting.enabled && (mc.objectMouseOver == null || !(mc.objectMouseOver.entityHit instanceof EntityLivingBase))) {
                    return;
                }
                if (chanceLeft.getInput() != 100.0 && (d = Math.random()) >= chanceLeft.getInput() / 100.0) {
                    this.release(0);
                    return;
                }
                this.robot.mouseRelease(16);
                this.robot.mousePress(16);
                this.robotLeft = true;
            }
        } else if (e.button == 1 && rightClick.enabled) {
            if (this.robotRight) {
                this.robotRight = false;
            } else {
                double d;
                if (blocksOnly.enabled) {
                    ItemStack s = mc.thePlayer.getHeldItem();
                    if (s == null || !(s.getItem() instanceof ItemBlock)) {
                        this.release(1);
                        return;
                    }
                }
                if (above5Cps.enabled && ClickUtils.rightCps() <= 5) {
                    this.release(1);
                    return;
                }
                if (chanceRight.getInput() != 100.0 && (d = Math.random()) >= chanceRight.getInput() / 100.0) {
                    this.release(1);
                    return;
                }
                this.robot.mouseRelease(4);
                this.robot.mousePress(4);
                this.robotRight = true;
            }
        }
        this.release(0);
        this.release(1);
    }
}
