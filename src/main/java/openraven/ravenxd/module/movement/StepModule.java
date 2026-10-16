package openraven.ravenxd.module.movement;

import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;

// pandaware skid
// todo: matrix + jump
public class StepModule extends Module {
    public enum Mode {
        Vanilla, Verus, Vulcan, NCP
    }

    public static SliderSetting mode, stepHeight, stepTimer, timer10, timer15, timer20;
    public static ButtonSetting differentTimers;

    private static final float[] VERUS_10 = {0.5f};
    private static final float[] VERUS_15 = {0.5f, 1.0f};
    private static final float[] VULCAN_10 = {0.41999998688698f, 0.753f};
    private static final float[] VULCAN_15 = {0.41999998688698f, 0.753f, 1.0f};
    private static final float[] NCP_10 = {0.41999998688698f, 0.753f};
    private static final float[] NCP_15 = {0.41999998688698f, 0.75f, 1.0f, 1.16f};
    private static final float[] VULCAN_20 = {0.41999998688698f, 0.78f, 0.63f, 0.51f, 0.9f, 1.21f, 1.45f, 1.43f};

    private float defaultStep = 0.6f;
    private long lastPacket, lastStep;
    private boolean stepping;

    public StepModule(int key) {
        super("Step", Category.movement, key);
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        stepHeight = (SliderSetting) ModuleManager.setting(this, "Step height");
        stepTimer = (SliderSetting) ModuleManager.setting(this, "Step timer");
        differentTimers = (ButtonSetting) ModuleManager.setting(this, "Different timers");
        timer10 = (SliderSetting) ModuleManager.setting(this, "Timer 1.0");
        timer15 = (SliderSetting) ModuleManager.setting(this, "Timer 1.5");
        timer20 = (SliderSetting) ModuleManager.setting(this, "Timer 2.0");
    }

    @Override
    public void onEnable() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p != null) {
            this.defaultStep = p.stepHeight;
        }
        this.stepping = false;
        this.lastPacket = 0L;
    }

    @Override
    public void onDisable() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p != null) {
            p.stepHeight = this.defaultStep;
        }
        if (this.stepping) {
            MoveUtils.resetTimer();
        }
        this.stepping = false;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        if (this.stepping && System.currentTimeMillis() - this.lastStep >= 50L) {
            this.stepping = false;
            MoveUtils.resetTimer();
        }
    }

    private static boolean blocked(Minecraft mc, EntityPlayerSP p, double height) {
        return !mc.theWorld.getCollidingBoundingBoxes(p, p.getEntityBoundingBox().offset(p.motionX, height, p.motionZ)).isEmpty();
    }

    private static float neededHeight(Minecraft mc, EntityPlayerSP p, Mode m) {
        if (!blocked(mc, p, 1.01)) {
            return 1.0f;
        }
        if (m == Mode.Verus || !blocked(mc, p, 1.51)) {
            return 1.5f;
        }
        return 2.0f;
    }

    private static float[] offsets(Mode m, float height) {
        if (m == Mode.Verus) {
            return height == 1.0f ? VERUS_10 : height == 1.5f ? VERUS_15 : null;
        }
        if (m == Mode.NCP) {
            return height == 1.0f ? NCP_10 : height == 1.5f ? NCP_15 : height == 2.0f ? VULCAN_20 : null;
        }
        return height == 1.0f ? VULCAN_10 : height == 1.5f ? VULCAN_15 : height == 2.0f ? VULCAN_20 : null;
    }

    public void beforeMove() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        p.stepHeight = this.defaultStep;
        if (mc.theWorld == null || !p.onGround || ModuleManager.enabled("Speed") != null) {
            return;
        }
        Mode m = Mode.values()[(int) mode.getInput()];
        if (m == Mode.Vanilla) {
            p.stepHeight = (float) stepHeight.getInput();
            return;
        }
        if (mc.theWorld.isAirBlock(new BlockPos(p.posX, p.posY - 1.0, p.posZ))) {
            return;
        }
        if (!p.isCollidedHorizontally || PlayerUtils.inLiquid() || p.isOnLadder()) {
            return;
        }
        float needed = neededHeight(mc, p, m);
        if (needed > stepHeight.getInput()) {
            return;
        }
        if (needed == 1.0f && !blocked(mc, p, 0.6) || blocked(mc, p, needed + 0.01)) {
            return;
        }
        float timer = (float) stepTimer.getInput();
        if (needed >= 1.0f) {
            timer -= Math.abs(1.0f - needed) * (timer * 0.6f);
        }
        if (differentTimers.isToggled()) {
            timer = (float) (needed == 1.0f ? timer10.getInput() : needed == 1.5f ? timer15.getInput() : timer20.getInput());
        }
        MoveUtils.setTimer(timer);
        p.stepHeight = needed;
        long now = System.currentTimeMillis();
        long delay = needed > 1.0f ? (m == Mode.Vulcan ? 50L : 355L) : 100L;
        if (now - this.lastPacket > delay) {
            float[] offsets = offsets(m, needed);
            if (offsets != null) {
                for (float o : offsets) {
                    PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(p.posX, p.posY + o, p.posZ, true));
                }
                this.lastPacket = now;
            }
        }
        this.stepping = true;
        this.lastStep = now;
    }
}
