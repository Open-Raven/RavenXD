package openraven.ravenxd.module.movement;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

// pandaware damage vulcan highjump
public class HighJumpModule extends Module {
    public static HighJumpModule instance;
    public static SliderSetting mode, height;
    public static ButtonSetting onlyOnJump;
    private int ticks;
    private double groundY;
    private boolean damage;

    public HighJumpModule(int key) {
        super("High Jump", Category.movement, key);
        instance = this;
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        height = (SliderSetting) ModuleManager.setting(this, "Height");
        onlyOnJump = (ButtonSetting) ModuleManager.setting(this, "Only on jump");
    }

    private static boolean verus() {
        return mode.getInput() == 1.0;
    }

    public static double jumpMotion() {
        return instance != null && instance.isEnabled() && !verus() ? height.getInput() : -1.0;
    }

    @Override
    public void onEnable() {
        if (!verus()) {
            return;
        }
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        this.ticks = 0;
        this.groundY = p.posY;
        this.damage = true;
        if (p.posY % 0.015625 != 0.0) {
            // TODO: text formatting util or smhtn
            Chat.print("&cYou must be on the ground.");
            this.disable();
        }
    }

    @Override
    public void onDisable() {
        this.damage = false;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (!verus()) {
            if (!onlyOnJump.isToggled() && p.onGround) {
                p.motionY = height.getInput();
            }
            return;
        }
        if (this.damage) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            p.motionX = 0.0;
            p.motionZ = 0.0;
        }
        if (p.hurtTime == 9) {
            this.disable();
            if (PlayerUtils.isMoving()) {
                MoveUtils.strafe(0.5);
            }
            p.motionY = 1.0;
            return;
        }
        if (!this.damage) {
            return;
        }
        if (this.ticks < 17) {
            e.onGround = false;
            e.y = p.posY + 0.5;
        } else {
            if (this.ticks < 25) {
                e.onGround = true;
                e.y = this.groundY;
            }
            if (this.ticks > 25) {
                this.damage = false;
            }
        }
        ++this.ticks;
    }
}
