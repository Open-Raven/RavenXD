package openraven.ravenxd.module.movement;

import openraven.ravenxd.util.MoveDirection;
import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.client.Hypixel;
import openraven.ravenxd.client.Overlay;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.player.NoRotateModule;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

public class SpeedModule extends Module {
    public static SpeedModule instance;
    public static SliderSetting mode, multiplier, hopSpeed, timer;
    public static ButtonSetting disableInInventory, onlyForward, onlyStrafe, setbackRecovery;

    public enum Mode {
        VANILLA, FLOAT, HOP, VERUS, MATRIX, AGC, VERUS_FLAG, VERUS2, VERUS_LOW
    }

    public static Mode current = Mode.VANILLA;
    private static boolean waiting;
    private static int ticks;

    public SpeedModule(int key) {
        super("Speed", Category.movement, key);
        instance = this;
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Speed");
        multiplier = (SliderSetting) ModuleManager.setting(this, "Multiplier");
        hopSpeed = (SliderSetting) ModuleManager.setting(this, "Hop speed");
        timer = (SliderSetting) ModuleManager.setting(this, "Timer");
        disableInInventory = (ButtonSetting) ModuleManager.setting(this, "Disable in inventory");
        onlyForward = (ButtonSetting) ModuleManager.setting(this, "Only forward");
        onlyStrafe = (ButtonSetting) ModuleManager.setting(this, "Only strafe");
        setbackRecovery = (ButtonSetting) ModuleManager.setting(this, "Setback recovery");
    }

    @Override
    public void onEnable() {
        waiting = true;
        ticks = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (current.ordinal() >= Mode.HOP.ordinal()) {
            MoveUtils.resetTimer();
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
        mc.thePlayer.jumpMovementFactor = 0.02f;
    }

    @Override
    public void guiUpdate() {
        Mode m = Mode.values()[(int) mode.getInput()];
        if (m != current && current.ordinal() >= Mode.HOP.ordinal() && this.isEnabled()) {
            MoveUtils.resetTimer();
        }
        current = m;
    }

    private static void boost(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.thePlayer.onGround || mc.thePlayer.isSneaking() || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (!PlayerUtils.isMoving() || GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
            return;
        }
        if (disableInInventory.enabled && Overlay.screenOpen()) {
            return;
        }
        if (setbackRecovery.enabled && NoRotateModule.sinceSetback(3000L)) {
            return;
        }
        double current = MoveUtils.speed();
        if (current == 0.0) {
            return;
        }
        MoveDirection dir = PlayerUtils.direction(e.yaw);
        if (onlyForward.enabled && !dir.forward || onlyStrafe.enabled && !dir.diagonal) {
            return;
        }
        double d = 0.153 * multiplier.getInput() + (double) PlayerUtils.speedLevel() * 0.04;
        MoveUtils.strafe(Math.max(d, current));
    }

    private static boolean edgeStairs(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        IBlockState st = mc.theWorld.getBlockState(pos);
        if (st.getBlock() instanceof BlockStairs) {
            if (st.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP) {
                return false;
            }
            for (int i = -1; i <= 1; i += 2) {
                if (mc.theWorld.getBlockState(pos.add(i, 0, 0)).getBlock() == Blocks.air) {
                    return true;
                }
                if (mc.theWorld.getBlockState(pos.add(0, 0, i)).getBlock() != Blocks.air) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (current == Mode.VANILLA) {
            boost(e);
        } else if (current == Mode.FLOAT) {
            if (openraven.ravenxd.module.player.ScaffoldModule.active) {
                waiting = true;
                return;
            }
            BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.5, mc.thePlayer.posZ);
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (edgeStairs(pos) || block instanceof BlockSlab && block.getClass().getSimpleName().contains("Half")) {
                waiting = true;
            } else {
                if (waiting && PlayerUtils.airTicks >= 3L) {
                    waiting = false;
                }
                if (!waiting && mc.thePlayer.onGround && e.y % 1.0 == 0.0) {
                    if (Hypixel.onHypixel()) {
                        e.y += 1.0E-13;
                    }
                    boost(e);
                }
            }
        } else {
            MoveUtils.setTimer((float) timer.getInput());
            switch (current) {
                case HOP: hop(); break;
                case VERUS: verus(); break;
                case MATRIX: matrix(); break;
                case AGC: agc(); break;
                case VERUS_FLAG: verusFlag(e); break;
                case VERUS2: verus2(); break;
                case VERUS_LOW: verusLow(e); break;
            }
        }
    }

    // srt client

    private static void hop() {
        Minecraft mc = Minecraft.getMinecraft();
        if (PlayerUtils.isMoving()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround);
            MoveUtils.strafe(hopSpeed.getInput());
        } else {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    private static void verus() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer.onGround) {
            if (PlayerUtils.isMoving()) {
                MoveUtils.strafe(0.36);
                mc.thePlayer.jump();
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.motionY, mc.thePlayer.posZ);
                mc.thePlayer.motionY = 0.0;
            }
            ticks = 0;
        } else {
            ++ticks;
            MoveUtils.strafe(MoveUtils.speed());
        }
        if (mc.thePlayer.hurtTime != 0) {
            MoveUtils.strafe(0.45);
        }
    }

    // Boosts on the ground for 40 ticks after taking damage.
    private static void matrix() {
        Minecraft mc = Minecraft.getMinecraft();
        if (ticks != 0) {
            ++ticks;
        }
        if (mc.thePlayer.hurtTime == 9) {
            ticks = 1;
        }
        if (mc.thePlayer.onGround && ticks != 0 && ticks <= 40) {
            MoveUtils.strafe(0.975);
        }
    }

    private static void agc() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!PlayerUtils.isMoving()) {
            return;
        }
        if (mc.thePlayer.hurtTime != 0) {
            MoveUtils.strafe(hopSpeed.getInput());
            return;
        }
        if (mc.thePlayer.onGround) {
            ticks = 0;
            mc.thePlayer.jump();
        }
        MoveUtils.strafe(MoveUtils.speed());
    }

    // azura verus 2 verus low
    private static double baseSpeed() {
        double s = 0.2873;
        int level = PlayerUtils.speedLevel();
        return level > 0 ? s * (1.0 + 0.2 * level) : s;
    }

    private static void verus2() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (!PlayerUtils.isMoving()) {
            MoveUtils.strafe(0.0);
        } else if (p.onGround) {
            p.motionY = 0.42f;
            MoveUtils.strafe(baseSpeed() + 0.17);
        } else {
            MoveUtils.strafe(baseSpeed() + 0.02 * 0.991);
        }
    }

    // lowhop
    private static void verusLow(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (!PlayerUtils.isMoving()) {
            MoveUtils.strafe(0.0);
        } else if (p.onGround) {
            p.motionY = 0.42f;
            MoveUtils.strafe(baseSpeed() + 0.1);
        } else {
            MoveUtils.strafe(baseSpeed() + 0.02 * 0.991);
        }
        double dir = Math.toRadians(MoveUtils.moveYaw(p.rotationYaw));
        boolean scaffold = ModuleManager.enabled("Scaffold") != null;
        boolean blocked = false;
        for (double d = -0.5; d < 2.0; d += 0.1) {
            double x = -Math.sin(dir) * d;
            double z = Math.cos(dir) * d;
            for (int i = 0; i >= -100 && !scaffold; --i) {
                blocked = true;
                if (!mc.theWorld.isAirBlock(new BlockPos(p.posX + x, p.posY + i, p.posZ + z))) {
                    blocked = false;
                    break;
                }
            }
            if (!mc.theWorld.getCollidingBoundingBoxes(p, p.getEntityBoundingBox().offset(x, 0.0, z)).isEmpty()) {
                blocked = true;
            }
        }
        if (p.motionY > 0.0 && p.motionY < 0.4 && !GameSettings.isKeyDown(mc.gameSettings.keyBindJump) && !blocked) {
            e.onGround = true;
            p.motionY = 0.0;
        }
    }

    private static void verusFlag(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer.onGround) {
            MoveUtils.strafe(hopSpeed.getInput());
            if (mc.thePlayer.ticksExisted % 19 == 0) {
                ++ticks;
                e.y -= 14.36;
            }
        }
    }
}
