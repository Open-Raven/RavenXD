package xyz.daikirai.module.movement;

import xyz.daikirai.client.Overlay;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.player.BedAuraModule;
import xyz.daikirai.module.player.NoRotateModule;
import xyz.daikirai.module.player.ScaffoldModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.MoveDirection;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;

public class BhopModule extends Module {
    public static SliderSetting modeSetting, speed, speedBoost, offset, strafeDecay;
    public static ButtonSetting autoJump, disableInInventory, disableInLiquid, disableWithJumpPotion, disableWhileCollided, disableWhileSneaking,
            maximizeStrafeTicks, stopMotionOnDisable;

    public enum Mode {
        Vanilla, Strafe, Ground, Glide, _7_tick, _8_tick_strafe, _9_tick, _9_tick_strafe, Hurt_time;

        public void onDisable() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mode == _7_tick || mode == _8_tick_strafe || mode == _9_tick_strafe) {
                if (mc.thePlayer.motionY == 0.38999999363422366) {
                    mc.thePlayer.motionY -= 0.0568;
                } else if (PlayerUtils.airTicks <= 2L) {
                    PlayerUtils.bhopJumpTick = mc.thePlayer.ticksExisted;
                }
            }
        }
    }

    public static Mode mode = Mode.Ground;
    private static boolean falling;
    private static boolean active;
    private static int setback;
    private static int jumps;

    public BhopModule(int key) {
        super("Bhop", Category.movement, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        speed = (SliderSetting) ModuleManager.setting(this, "Speed");
        speedBoost = (SliderSetting) ModuleManager.setting(this, "Speed boost");
        offset = (SliderSetting) ModuleManager.setting(this, "Offset");
        strafeDecay = (SliderSetting) ModuleManager.setting(this, "Strafe decay");
        autoJump = (ButtonSetting) ModuleManager.setting(this, "Auto jump");
        disableInInventory = (ButtonSetting) ModuleManager.setting(this, "Disable in inventory");
        disableInLiquid = (ButtonSetting) ModuleManager.setting(this, "Disable in liquid");
        disableWithJumpPotion = (ButtonSetting) ModuleManager.setting(this, "Disable with jump potion");
        disableWhileCollided = (ButtonSetting) ModuleManager.setting(this, "Disable while collided");
        disableWhileSneaking = (ButtonSetting) ModuleManager.setting(this, "Disable while sneaking");
        maximizeStrafeTicks = (ButtonSetting) ModuleManager.setting(this, "Maximize strafe ticks");
        stopMotionOnDisable = (ButtonSetting) ModuleManager.setting(this, "Stop motion on disable");
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
    }

    public static boolean canBhop() {
        Minecraft mc = mc();
        if (mc.thePlayer.capabilities.isFlying || ModuleManager.enabled("Fly") != null) {
            return false;
        }
        if (!(!ScaffoldModule.active || ScaffoldModule.fastScaffold.keepY && ScaffoldModule.fastScaffold.canFastScaffold())) {
            return false;
        }
        if (BedAuraModule.disableBhop.isToggled() && BedAuraModule.target != null) {
            return false;
        }
        if (disableWhileSneaking.isToggled() && mc.thePlayer.isSneaking()) {
            return false;
        }
        if (disableWithJumpPotion.isToggled() && mc.thePlayer.isPotionActive(Potion.jump)) {
            return false;
        }
        if (disableInLiquid.isToggled() && PlayerUtils.inLiquid()) {
            return false;
        }
        if (disableInInventory.isToggled() && Overlay.screenOpen()) {
            return false;
        }
        return !NoSlowModule.sneaking && (NoSlowModule.useTicks <= 0 || NoSlowModule.current != NoSlowModule.Mode.GROUND && NoSlowModule.current != NoSlowModule.Mode.JUMP);
    }

    private static boolean bhop(MotionEvent e, boolean moving) {
        Minecraft mc = mc();
        if (mode == Mode.Vanilla) {
            return true;
        }
        if (NoRotateModule.sinceSetback(500L)) {
            setback = -1;
        } else if (setback == -1) {
            setback = 0;
        }
        if (mc.thePlayer.onGround) {
            falling = false;
            if (mc.thePlayer.motionY == -0.0784000015258789 && !ScaffoldModule.active) {
                PlayerUtils.jump();
                if (moving) {
                    ++jumps;
                    double boost = potionBoost();
                    double current = MoveUtils.speed();
                    double d = (0.48475 + boost) * speed.getInput() + offset.getInput() / 100.0 - MathUtils.randomTimes(1.0E-4);
                    MoveDirection dir = PlayerUtils.direction(e);
                    if (!dir.forward) {
                        d = SprintModule.disableBackwards.isToggled() && dir.backward ? current : (d += 0.03);
                    }
                    if (setback == -1) {
                        d = current;
                    } else if (jumps == 1) {
                        d *= 0.95;
                    }
                    MoveUtils.strafe(d);
                }
            }
            if (setback == -1) {
                --setback;
            } else if (setback == -2) {
                setback = 0;
            }
            return true;
        }
        if (disableWhileCollided.isToggled() && mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        if (!moving || setback < 0) {
            return false;
        }
        if (ScaffoldModule.active) {
            falling = true;
        } else {
            double h = PlayerUtils.heightAboveGround();
            if (h > 1.25 || h == -1.0) {
                falling = true;
            }
        }
        double d = MoveUtils.speed();
        switch (mode) {
            case Ground:
            case Hurt_time: {
                if (mode == Mode.Hurt_time && Math.abs(System.currentTimeMillis() - PacketUtils.lastVelocity) < 500L) {
                    MoveUtils.strafe(d);
                    break;
                }
                if (!maximizeStrafeTicks.isToggled()) break;
                switch (PlayerUtils.motionYDecimals()) {
                    case 3332: {
                        MoveUtils.strafe(d);
                        break;
                    }
                    case -4475: {
                        if (falling) break;
                        MoveUtils.strafe(d);
                    }
                }
                break;
            }
            case Strafe: {
                MoveUtils.strafe(d);
                break;
            }
            case Glide: {
                double motionY = 0.0;
                double s = 0.0;
                switch (PlayerUtils.motionYDecimals()) {
                    case 3332: {
                        s = d;
                        break;
                    }
                    case -3766: {
                        motionY = -0.15 + Double.parseDouble("0.00000" + PlayerUtils.idOffset());
                        s = 0.275;
                        break;
                    }
                    case -2254: {
                        motionY = -0.1;
                        s = 0.27;
                        break;
                    }
                    case -1764: {
                        motionY = -0.07;
                        s = 0.27;
                        break;
                    }
                    case -2225:
                    case -1470: {
                        s = 0.27;
                    }
                }
                if (falling) break;
                if (motionY != 0.0) {
                    mc.thePlayer.motionY = motionY;
                }
                if (s == 0.0) break;
                s = Math.max(d, s);
                if (PlayerUtils.airTicks > 1L) {
                    s *= 1.0 - strafeDecay.getInput();
                }
                MoveUtils.strafe(s);
                break;
            }
            case _7_tick: {
                switch (PlayerUtils.motionYDecimals()) {
                    case 3332: {
                        mc.thePlayer.motionY += 0.0568;
                        if (!maximizeStrafeTicks.isToggled()) break;
                        MoveUtils.strafe(d);
                        break;
                    }
                    case 2193: {
                        mc.thePlayer.motionY -= 0.13;
                        break;
                    }
                    case 91: {
                        mc.thePlayer.motionY -= 0.2 + Double.parseDouble("0.000000" + PlayerUtils.idOffset());
                        break;
                    }
                    case -4102: {
                        if (!maximizeStrafeTicks.isToggled() || falling) break;
                        MoveUtils.strafe(d);
                    }
                }
                break;
            }
            case _8_tick_strafe: {
                int n = PlayerUtils.motionYDecimals();
                switch (n) {
                    case 3332: {
                        mc.thePlayer.motionY += 0.0568;
                        MoveUtils.strafe(d);
                        break;
                    }
                    case 2193: {
                        mc.thePlayer.motionY -= 0.13;
                        break;
                    }
                    case 91: {
                        mc.thePlayer.motionY -= 0.2 + Double.parseDouble("0.000000" + PlayerUtils.idOffset());
                    }
                }
                if (falling) break;
                switch (n) {
                    case -3385: {
                        mc.thePlayer.motionY += 0.08;
                        MoveUtils.strafe(d * (1.0 - strafeDecay.getInput()));
                        break;
                    }
                    case -4035:
                    case -3318: {
                        MoveUtils.strafe(d);
                    }
                }
                break;
            }
            case _9_tick: {
                switch (PlayerUtils.motionYDecimals()) {
                    case 3332: {
                        if (!maximizeStrafeTicks.isToggled()) break;
                        MoveUtils.strafe(d);
                        break;
                    }
                    case 1648: {
                        mc.thePlayer.motionY -= 0.014 + Double.parseDouble("0.000000" + PlayerUtils.idOffset());
                        break;
                    }
                    case -886: {
                        mc.thePlayer.motionY -= 0.12;
                        break;
                    }
                    case -4269: {
                        if (!maximizeStrafeTicks.isToggled() || falling) break;
                        MoveUtils.strafe(d);
                    }
                }
                break;
            }
            case _9_tick_strafe: {
                int n = PlayerUtils.motionYDecimals();
                switch (n) {
                    case 3332: {
                        mc.thePlayer.motionY += 0.0568;
                        MoveUtils.strafe(d);
                        break;
                    }
                    case 2193: {
                        mc.thePlayer.motionY -= 0.13;
                        break;
                    }
                    case 91: {
                        mc.thePlayer.motionY -= 0.2 + Double.parseDouble("0.000000" + PlayerUtils.idOffset());
                    }
                }
                if (falling) break;
                switch (n) {
                    case -3385: {
                        mc.thePlayer.motionY += 0.14;
                        MoveUtils.strafe(d * (1.0 - strafeDecay.getInput()));
                        break;
                    }
                    case -2730: {
                        mc.thePlayer.motionY += 0.1;
                        MoveUtils.strafe(d);
                        break;
                    }
                    case -3213:
                    case -2479: {
                        MoveUtils.strafe(d);
                    }
                }
                break;
            }
            default:
                break;
        }
        return true;
    }

    @Override
    public void onDisable() {
        if (stopMotionOnDisable.isToggled()) {
            MoveUtils.strafe(0.0);
        }
        if (active) {
            mode.onDisable();
        }
        jumps = 0;
        setback = 0;
        falling = false;
        active = false;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        boolean was = active;
        active = false;
        if (canBhop()) {
            boolean moving = PlayerUtils.isMoving();
            if (moving || autoJump.isToggled()) {
                active = bhop(e, moving);
            } else if (stopMotionOnDisable.isToggled()) {
                MoveUtils.strafe(0.0);
            }
        }
        if (was && !active) {
            mode.onDisable();
        }
    }

    @Override
    public String getInfo() {
        if (mode == Mode._7_tick || mode == Mode._9_tick) {
            return "Low";
        }
        if (mode == Mode._8_tick_strafe || mode == Mode._9_tick_strafe) {
            return "Semi";
        }
        return modeSetting.getDisplay(false);
    }

    private static double potionBoost() {
        int n = PlayerUtils.speedLevel();
        switch (n) {
            case 0: {
                return 0.0;
            }
            case 1: {
                return 0.03;
            }
            case 2: {
                return speedBoost.getInput() / 10.0;
            }
        }
        return 0.06 * (double) n;
    }

    @Override
    public void onPostPlayerInput() {
        if (canBhop()) {
            PlayerUtils.setJump(mode == Mode.Vanilla);
        }
    }
}
