package xyz.daikirai.module.movement;

import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.gui.ClickGui;
import xyz.daikirai.gui.HudEditor;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.util.Timer;
import java.util.TimerTask;

public class TimerModule extends Module {
    public static TimerModule instance;
    public static SliderSetting multiplierSlider, sliderA, sliderB, sliderC;
    public static ButtonSetting disableChatMenu, disableOnTeleport, resetOnInput, strafeOnly;
    public static float speed;
    private static int slider;
    private Timer timer;
    private boolean resetByInput;

    public TimerModule(int key) {
        super("Timer", Category.movement, key);
        instance = this;
    }

    public void link() {
        multiplierSlider = (SliderSetting) ModuleManager.setting(this, "Multiplier slider");
        sliderA = (SliderSetting) ModuleManager.setting(this, "Slider A");
        sliderB = (SliderSetting) ModuleManager.setting(this, "Slider B");
        sliderC = (SliderSetting) ModuleManager.setting(this, "Slider C");
        disableChatMenu = (ButtonSetting) ModuleManager.setting(this, "Disable chat & menu");
        disableOnTeleport = (ButtonSetting) ModuleManager.setting(this, "Disable on teleport");
        resetOnInput = (ButtonSetting) ModuleManager.setting(this, "Reset on input");
        strafeOnly = (ButtonSetting) ModuleManager.setting(this, "Strafe only");
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static SliderSetting activeSlider() {
        return slider == 1 ? sliderB : slider == 2 ? sliderC : sliderA;
    }

    private static boolean inClientGui() {
        return mc().currentScreen instanceof ClickGui || mc().currentScreen instanceof HudEditor;
    }

    public static boolean slowed() {
        return speed > 0.0f && (double) speed < 0.25;
    }

    @Override
    public boolean canBeEnabled() {
        if (inClientGui()) {
            Chat.moduleMessage(this, "&cCannot be enabled from GUI.");
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                TimerModule t = TimerModule.this;
                if (!Chat.inWorld() || slowed() && mc().currentScreen != null && !inClientGui()) {
                    t.disable();
                    return;
                }
                t.pollBind();
                if (!t.isEnabled()) {
                    return;
                }
                if (slowed()) {
                    Module scaffold = ModuleManager.get("Scaffold");
                    Module stasis = ModuleManager.get("Stasis");
                    if (scaffold != null) scaffold.pollBind();
                    if (stasis != null) stasis.pollBind();
                }
                if (resetOnInput.enabled) {
                    boolean input = false;
                    if (Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
                        input = true;
                    } else {
                        int held = -1;
                        for (int i = 0; i < 9; ++i) {
                            if (GameSettings.isKeyDown(mc().gameSettings.keyBindsHotbar[i])) {
                                held = i;
                                break;
                            }
                        }
                        if (held != -1 && held != mc().thePlayer.inventory.currentItem) {
                            input = true;
                        }
                    }
                    if (input) {
                        MoveUtils.resetTimer();
                        t.resetByInput = true;
                    }
                }
            }
        }, 0L, 50L);
    }

    @Override
    public void onDisable() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
        MoveUtils.resetTimer();
        speed = 0.0f;
        this.resetByInput = false;
    }

    @Override
    public void onUpdate() {
        if (this.resetByInput) {
            this.resetByInput = false;
            MoveUtils.resetTimer();
        } else if (strafeOnly.enabled && mc().thePlayer.moveStrafing == 0.0f) {
            MoveUtils.resetTimer();
        } else {
            double d = activeSlider().getInput();
            if (d == 0.0) {
                d = 5.0E-4;
            }
            speed = (float) d;
            MoveUtils.setTimer(speed);
        }
    }

    @Override
    public void guiUpdate() {
        slider = (int) multiplierSlider.getInput();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (disableChatMenu.enabled && (e.gui instanceof GuiChat || e.gui instanceof GuiIngameMenu)) {
            Chat.moduleMessage(ModuleManager.gui, "&cCannot open " + (e.gui instanceof GuiChat ? "chat" : "menu") + ".");
            e.setCanceled(true);
        }
    }

    public static void onS08() {
        if (speed > 0.0f && disableOnTeleport.enabled) {
            instance.disable();
        }
    }
}
