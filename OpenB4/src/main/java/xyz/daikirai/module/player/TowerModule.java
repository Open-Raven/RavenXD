package xyz.daikirai.module.player;

import xyz.daikirai.client.Hypixel;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

public class TowerModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public enum Mode {
        Vanilla, Normal, Fly, _2_tick, _2__3_tick, _2__5_tick, _4_tick, _5_tick, _6_tick
    }

    public static SliderSetting modeSetting, speed, diagonalSpeed, speedBoost, slowedSpeed, slowedTicks;
    public static ButtonSetting disableInLiquid, disableWhileCollided, disableSafewalk, disableWhileHurt, onlyWhileRightClicking,
            setbackRecovery, skipSlowWithFastScaffold;

    public static Mode mode = Mode.Vanilla;
    public static boolean towering;
    public static boolean enabledByScaffold;
    private static int ticks;
    private static int diagonalTicks;
    private static int slowedLeft;
    private static boolean diagonal;
    private static int stage;

    public TowerModule(int key) {
        super("Tower", Category.player, key);
    }

    @Override
    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        speed = (SliderSetting) ModuleManager.setting(this, "Speed");
        diagonalSpeed = (SliderSetting) ModuleManager.setting(this, "Diagonal speed");
        speedBoost = (SliderSetting) ModuleManager.setting(this, "Speed boost");
        slowedSpeed = (SliderSetting) ModuleManager.setting(this, "Slowed speed");
        slowedTicks = (SliderSetting) ModuleManager.setting(this, "Slowed ticks");
        disableInLiquid = (ButtonSetting) ModuleManager.setting(this, "Disable in liquid");
        disableWhileCollided = (ButtonSetting) ModuleManager.setting(this, "Disable while collided");
        disableSafewalk = (ButtonSetting) ModuleManager.setting(this, "Disable safewalk");
        disableWhileHurt = (ButtonSetting) ModuleManager.setting(this, "Disable while hurt");
        onlyWhileRightClicking = (ButtonSetting) ModuleManager.setting(this, "Only while right clicking");
        setbackRecovery = (ButtonSetting) ModuleManager.setting(this, "Setback recovery");
        skipSlowWithFastScaffold = (ButtonSetting) ModuleManager.setting(this, "Skip slow with fast scaffold");
    }

    @Override
    public void onPreMotionAlways(MotionEvent e) {
        if (enabledByScaffold) {
            preMotion(e);
        }
    }

    @Override
    public void onPostPlayerInputAlways() {
        if (enabledByScaffold) {
            postPlayerInput();
        }
    }

    @Override
    public boolean allowsSafeWalk() {
        return !towering || !disableSafewalk.isToggled();
    }

    @Override
    public boolean isTowering() {
        return towering;
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
    }

    private static boolean canTower() {
        if (mc.thePlayer.capabilities.isFlying || mc.currentScreen != null) {
            return false;
        }
        if (ModuleManager.enabled("Scaffold") == null || !ScaffoldModule.towerAllowed()) {
            return false;
        }
        if (onlyWhileRightClicking.isToggled() && !Mouse.isButtonDown(1)) {
            return false;
        }
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        if (itemStack == null || !(itemStack.getItem() instanceof ItemBlock)) {
            return false;
        }
        if (!PlayerUtils.isMoving() || mc.thePlayer.isSneaking()) {
            return false;
        }
        if (!(PlayerUtils.jumpKeyDown() || stage != 0 && ScaffoldModule.legitTicks != 0)) {
            return false;
        }
        if (disableInLiquid.isToggled() && PlayerUtils.inLiquid()) {
            return false;
        }
        if (disableWhileCollided.isToggled() && mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        if (disableWhileHurt.isToggled() && mc.thePlayer.hurtTime > 0) {
            return false;
        }
        if (diagonalSpeed.isAtMin() && PlayerUtils.isDiagonal()) {
            return false;
        }
        if (setbackRecovery.isToggled() && NoRotateModule.sinceSetback(2000L)) {
            PlayerUtils.setJump(false);
            return false;
        }
        return true;
    }

    private static void preMotion(MotionEvent e) {
        if (!towering) {
            ticks = 0;
            return;
        }
        ++ticks;
        diagonal = PlayerUtils.isDiagonal();
        if (diagonal) {
            diagonalTicks = 1;
        } else if (diagonalTicks > 0 && ++diagonalTicks < 8) {
            diagonal = true;
        }
        if (mode == Mode.Vanilla) {
            if (ScaffoldModule.jumpFacingForward.isToggled()) {
                if (stage == 0) {
                    if (mc.thePlayer.onGround) {
                        if (ScaffoldModule.legitTicks == 0) {
                            stage = 1;
                            ScaffoldModule.legitTicks = ScaffoldModule.legitMotion.isToggled() ? 5 : 1;
                        } else {
                            stage = 2;
                        }
                    }
                } else if (stage == 1) {
                    ScaffoldModule.delay = ScaffoldModule.legitTicks;
                    stage = 2;
                }
            }
            return;
        }
        boolean faceBack = false;
        mc.thePlayer.setSprinting(false);
        switch (mode) {
            case Normal: {
                if (!mc.thePlayer.onGround) break;
                double d = speed(diagonal);
                PlayerUtils.jump(d);
                if (!(d > 0.17)) break;
                faceBack = true;
                break;
            }
            case Fly: {
                if (mc.thePlayer.ticksExisted % 2 != 0) break;
                mc.thePlayer.motionY = 0.21 + speed.getInput() * 0.0525;
                double d = Math.min(0.17 + speed.getInput() * 0.015, 0.27) + speedBonus() + 1.0E-4 * Math.random();
                MoveUtils.strafe(d);
                break;
            }
            case _2_tick:
            case _2__3_tick:
            case _2__5_tick: {
                switch (PlayerUtils.posYDecimals()) {
                    case 0: {
                        if (stage <= 2) {
                            double d;
                            if (ticks == 1) {
                                d = 0.124;
                            } else if (mode == Mode._2__5_tick) {
                                if (stage == 0) {
                                    d = speed(diagonal);
                                    if (d > 0.17) {
                                        faceBack = true;
                                    }
                                } else {
                                    d = ticks == 4 ? 0.14 : MoveUtils.speed();
                                }
                            } else {
                                d = 0.14;
                            }
                            if (d != MoveUtils.speed()) {
                                mc.thePlayer.onGround = true;
                            }
                            e.onGround = true;
                            PlayerUtils.jump(d);
                            if (mode == Mode._2__3_tick) {
                                ++stage;
                                break;
                            }
                            if (mode != Mode._2__5_tick) break;
                            stage += 2;
                            break;
                        }
                        if (Hypixel.onHypixel()) {
                            e.y += Double.parseDouble("0.0003" + PlayerUtils.idOffset());
                        } else {
                            e.onGround = true;
                        }
                        mc.thePlayer.motionY = 0.0;
                        stage = 0;
                        break;
                    }
                    case 4200: {
                        mc.thePlayer.motionY = 0.33f;
                        if (mode != Mode._2__5_tick) {
                            MoveUtils.strafe(speed(diagonal));
                            break;
                        }
                        if (ticks != 2 && ticks != 5) break;
                        MoveUtils.strafe(0.28 + speedBonus());
                        break;
                    }
                    case 7500: {
                        mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                    }
                }
                break;
            }
            case _4_tick:
            case _5_tick: {
                switch (PlayerUtils.posYDecimals()) {
                    case 0: {
                        PlayerUtils.jump(speed(diagonal));
                        break;
                    }
                    case 13:
                    case 1661:
                    case 2492: {
                        e.y = Math.floor(e.y);
                        if (Hypixel.onHypixel()) {
                            e.y += Double.parseDouble("0.0003" + PlayerUtils.idOffset());
                        } else {
                            e.onGround = true;
                        }
                        if (PlayerUtils.posYDecimals() != (mode == Mode._4_tick ? 1661 : 2492)) break;
                        mc.thePlayer.motionY -= 1.0;
                    }
                }
                break;
            }
            case _6_tick: {
                if (mc.thePlayer.onGround) {
                    double d = speed(diagonal);
                    PlayerUtils.jump(d);
                    if (d > 0.17) {
                        faceBack = true;
                    }
                    if (Hypixel.onHypixel()) {
                        mc.thePlayer.motionY -= 9.0E-4 - Double.parseDouble("0.00000" + PlayerUtils.idOffset());
                    }
                    stage = 1;
                    break;
                }
                if (stage == 1) {
                    int n = PlayerUtils.posYDecimals();
                    if (n == 4191) {
                        mc.thePlayer.motionY = 0.3275;
                        break;
                    }
                    if (n <= 2200 || n >= 2500) break;
                    stage = 2;
                    break;
                }
                if (stage != 2) break;
                mc.thePlayer.motionY -= 1.0;
                stage = 0;
                break;
            }
            default:
                break;
        }
        if (faceBack && ScaffoldModule.jumpFacingForward.isToggled()) {
            e.yaw = PlayerUtils.movementAngle() - 180.0f + RotationUtils.randomOffset();
            ScaffoldModule.delay = 2;
        }
    }

    private static void reset() {
        diagonal = false;
        towering = false;
        enabledByScaffold = false;
        stage = 0;
        slowedLeft = 0;
        diagonalTicks = 0;
        ticks = 0;
    }

    private static double speed(boolean diagonal, double d) {
        double d2 = ((diagonal ? diagonalSpeed.getInput() : speed.getInput()) + d) / 10.0 + 1.0E-4 * Math.random();
        return (d2 += speedBonus()) > 0.0 ? d2 : 0.0;
    }

    private static double speed(boolean diagonal) {
        return speed(diagonal, 0.0);
    }

    public static void stop() {
        towering = false;
        if (ticks > 1 && (mode == Mode._2__3_tick || mode == Mode._2__5_tick) && stage >= 2) {
            if (mc.thePlayer.motionY == (double) 0.42f) {
                mc.thePlayer.motionY = 0.16659951058455766;
            }
        }
        diagonalTicks = 0;
        stage = 0;
    }

    public static boolean vanillaTowering() {
        return towering && mode == Mode.Vanilla;
    }

    private static double speedBonus() {
        int n = PlayerUtils.speedLevel();
        switch (n) {
            case 0:
                return 0.0;
            case 1:
                return 0.05;
            case 2:
                return speedBoost.getInput() / 10.0;
        }
        return 0.06 * (double) n;
    }

    public static void setFromScaffold(ButtonSetting tower) {
        if (tower.isToggled()) {
            if (!enabledByScaffold) {
                enabledByScaffold = true;
            }
        } else if (enabledByScaffold) {
            reset();
        }
    }

    private static void postPlayerInput() {
        if (canTower()) {
            towering = true;
            slowedLeft = 0;
            if (mode == Mode.Vanilla) {
                if (ScaffoldModule.jumpFacingForward.isToggled()) {
                    PlayerUtils.setJump(true);
                    if (stage == 2) {
                        stage = 0;
                        PlayerUtils.setJump(true);
                    } else {
                        PlayerUtils.setJump(false);
                    }
                    if (ScaffoldModule.legitMotion.isToggled() && diagonal && PlayerUtils.airTicks == 1L) {
                        PlayerUtils.setForward(0.0f);
                        PlayerUtils.setStrafe(0.0f);
                    }
                }
            } else {
                PlayerUtils.setJump(false);
            }
        } else if (towering) {
            towering = false;
            if (slowedLeft == 0) {
                slowedLeft = (int) slowedTicks.getInput();
            }
            stage = 0;
            ScaffoldModule.resetKeepY();
        }
        if (slowedLeft > 0) {
            --slowedLeft;
            if (PlayerUtils.isMoving()) {
                boolean slow = false;
                if (ScaffoldModule.fastScaffold.jump) {
                    if (PlayerUtils.perSpeedLevel(1.0) == 0.0 && ScaffoldModule.fastScaffold.canFastScaffold()) {
                        PlayerUtils.multiplyMotion(0.4);
                    } else {
                        slow = true;
                    }
                } else if (!skipSlowWithFastScaffold.isToggled() || !ScaffoldModule.fastScaffold.canFastScaffold()) {
                    slow = true;
                }
                if (slow) {
                    MoveUtils.setSpeed(slowedSpeed.getInput() / 10.0);
                }
            }
        }
    }
}
