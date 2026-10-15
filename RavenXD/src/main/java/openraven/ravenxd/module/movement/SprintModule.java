package xyz.daikirai.module.movement;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SprintModule extends Module {
    public static SprintModule instance;
    public static ButtonSetting editPosition, disableBackwards, displayText, rainbow;
    public static String text = "[Sprint (Toggled)]";
    public static int posX = 5, posY = 5;
    private static boolean stopBackwards;
    public static boolean sprinting;

    public SprintModule(int key) {
        super("Sprint", Category.movement, key);
        instance = this;
    }

    public void link() {
        editPosition = (ButtonSetting) ModuleManager.setting(this, "Edit text position");
        disableBackwards = (ButtonSetting) ModuleManager.setting(this, "Disable backwards");
        displayText = (ButtonSetting) ModuleManager.setting(this, "Display text");
        rainbow = (ButtonSetting) ModuleManager.setting(this, "Rainbow");
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void guiButtonToggled(ButtonSetting b) {
        if (b == editPosition) {
            mc().displayGuiScreen(new xyz.daikirai.gui.SprintTextEditor());
        }
    }

    public static void setText(String s) {
        text = Chat.color(s);
    }

    public static String storedText() {
        return text.replace("§", "&");
    }

    private static void setSprintKey(boolean down) {
        KeyBinding.setKeyBindState(mc().gameSettings.keyBindSprint.getKeyCode(), down);
    }

    private static void stop() {
        setSprintKey(false);
        PlayerUtils.resetSprintToggleTimer();
        mc().thePlayer.setSprinting(false);
    }

    @Override
    public void onPreUpdate() {
        boolean sprint = true;
        if (stopBackwards) {
            stopBackwards = false;
            sprint = false;
        } else if (xyz.daikirai.module.combat.WTapModule.reset && !xyz.daikirai.module.combat.WTapModule.silent.enabled) {
            xyz.daikirai.module.combat.WTapModule.reset = false;
            sprint = false;
        } else if (xyz.daikirai.module.player.ScaffoldModule.active && !xyz.daikirai.module.player.TowerModule.vanillaTowering()
                && !xyz.daikirai.module.player.ScaffoldModule.fastScaffold.canSprint()) {
            sprint = false;
        } else if (xyz.daikirai.module.ModuleManager.enabled("NoSlow") != null && (NoSlowModule.tickSlow != -1.0f
                || NoSlowModule.disableSprint.isToggled() && NoSlowModule.useTicks > 0)) {
            sprint = false;
        } else if (xyz.daikirai.module.ModuleManager.enabled("InvMove") != null && (InvMoveModule.busy() || !InvMoveModule.allowSprinting.isToggled()
                && (InvMoveModule.inInventory() || InvMoveModule.inChestLike()))) {
            sprint = false;
        } else if (xyz.daikirai.module.combat.KillAuraModule.target != null && xyz.daikirai.module.combat.KillAuraModule.disableSprintMidair.isToggled()
                && !mc().thePlayer.onGround) {
            sprint = false;
        }
        if (sprint) {
            setSprintKey(true);
            if (PlayerUtils.isMoving() && NoSlowModule.useTicks > 0) {
                mc().thePlayer.setSprinting(true);
            }
        } else {
            stop();
        }
        sprinting = sprint;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        if (disableBackwards.enabled && (mc().thePlayer.rotationYaw != e.yaw || mc().thePlayer.movementInput.moveForward > 0.0f)
                && !xyz.daikirai.module.player.ScaffoldModule.active && PlayerUtils.direction(e).backward) {
            stopBackwards = true;
        }
    }

    @Override
    public void onDisable() {
        stopBackwards = false;
        if (!GameSettings.isKeyDown(mc().gameSettings.keyBindSprint)) {
            setSprintKey(false);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !displayText.enabled || !Overlay.canRender()) {
            return;
        }
        mc().fontRendererObj.drawStringWithShadow(text, (float) posX, (float) posY, rainbow.enabled ? RenderUtils.rainbow(2L, 0L) : -1);
    }
}
