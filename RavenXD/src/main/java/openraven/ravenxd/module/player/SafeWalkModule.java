package xyz.daikirai.module.player;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class SafeWalkModule extends Module {
    public static SafeWalkModule instance;
    public static SliderSetting modeSlider, edgeTolerance, minimumPitch, unsneakDelay;
    public static ButtonSetting blocksOnly, disableOnForward, onlyRightClicking, onlyWhileSneaking;

    public enum Mode {
        EDGE, SNEAK_KEYBIND, SNEAK_STATE, SCAFFOLD
    }

    public enum Tolerance {
        LOW(0.05), MEDIUM(0.15), HIGH(0.25), VERY_HIGH(0.3);
        public final double value;

        Tolerance(double value) {
            this.value = value;
        }
    }

    public static Mode mode = Mode.EDGE;
    public static Tolerance tolerance = Tolerance.MEDIUM;
    public static boolean sneaking;
    private static long sneakStart;
    public static boolean scaffoldActive;
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    public SafeWalkModule(int key) {
        super("SafeWalk", Category.player, key);
        instance = this;
    }

    public void link() {
        modeSlider = (SliderSetting) ModuleManager.setting(this, "Mode");
        edgeTolerance = (SliderSetting) ModuleManager.setting(this, "Edge tolerance");
        minimumPitch = (SliderSetting) ModuleManager.setting(this, "Minimum pitch");
        unsneakDelay = (SliderSetting) ModuleManager.setting(this, "Unsneak delay");
        blocksOnly = (ButtonSetting) ModuleManager.setting(this, "Blocks only");
        disableOnForward = (ButtonSetting) ModuleManager.setting(this, "Disable on forward");
        onlyRightClicking = (ButtonSetting) ModuleManager.setting(this, "Only while right clicking");
        onlyWhileSneaking = (ButtonSetting) ModuleManager.setting(this, "Only while sneaking");
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static boolean enabled() {
        return instance != null && instance.isEnabled();
    }

    private static Module scaffold() {
        return ModuleManager.get("Scaffold");
    }

    @Override
    public void onPostPlayerInput() {
        if (mode != Mode.SNEAK_STATE) {
            return;
        }
        if (sneaking) {
            PlayerUtils.setSneak(true);
        }
    }

    @Override
    public void onDisable() {
        if (mode == Mode.SNEAK_KEYBIND && PlayerUtils.isAirBelow()) {
            setSneak(false);
        }
        if (scaffoldActive) {
            scaffoldActive = false;
            if (scaffold() != null) scaffold().disable();
        }
        sneaking = false;
    }

    public static boolean conditions(boolean needGround) {
        if (needGround && !mc().thePlayer.onGround) {
            return false;
        }
        if (!minimumPitch.isAtMin() && (double) mc().thePlayer.rotationPitch < minimumPitch.getInput()) {
            return false;
        }
        if (onlyRightClicking.enabled && !Mouse.isButtonDown(1)) {
            return false;
        }
        if (onlyWhileSneaking.enabled && !GameSettings.isKeyDown(mc().gameSettings.keyBindSneak)) {
            return false;
        }
        if (disableOnForward.enabled && GameSettings.isKeyDown(mc().gameSettings.keyBindForward)) {
            return false;
        }
        return !blocksOnly.enabled || PlayerUtils.holdingScaffoldBlock();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (mode != Mode.EDGE && e.gui == null) {
            sneaking = mc().thePlayer.isSneaking();
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate || e.button != 1) {
            return;
        }
        if (mode == Mode.SCAFFOLD && onlyRightClicking.enabled && conditions(false)) {
            e.setCanceled(true);
        }
    }

    private static void pressSneak(boolean b) {
        KeyBinding.setKeyBindState(mc().gameSettings.keyBindSneak.getKeyCode(), b);
    }

    @Override
    public boolean stopsOnEdge() {
        if (mode != Mode.EDGE) {
            return false;
        }
        return conditions(true);
    }

    public static boolean atEdge() {
        float f;
        double x = mc().thePlayer.posX;
        double y = mc().thePlayer.getEntityBoundingBox().minY - 0.01;
        double z = mc().thePlayer.posZ;
        double mx = mc().thePlayer.motionX;
        double mz = mc().thePlayer.motionZ;
        float forward = keyAxis(mc().gameSettings.keyBindForward, mc().gameSettings.keyBindBack) * 0.98f;
        float sq = forward * forward + (f = keyAxis(mc().gameSettings.keyBindLeft, mc().gameSettings.keyBindRight) * 0.98f) * f;
        if (sq >= 1.0E-4f) {
            float speed = mc().thePlayer.getAIMoveSpeed();
            float len = MathHelper.sqrt_float(sq);
            speed *= 0.9999998f;
            if (len >= 1.0f) {
                speed /= len;
            }
            float yaw = mc().thePlayer.rotationYaw * ((float) Math.PI / 180);
            float sin = MathHelper.sin(yaw);
            float cos = MathHelper.cos(yaw);
            mx += (double) (f * speed * cos - forward * speed * sin);
            mz += (double) (forward * speed * cos + f * speed * sin);
        }
        double nx = x + mx;
        double nz = z + mz;
        double t = tolerance.value;
        int by = MathHelper.floor_double(y);
        int x1 = MathHelper.floor_double(nx - t);
        int z1 = MathHelper.floor_double(nz - t);
        if (solidAt(x1, by, z1)) {
            return false;
        }
        int z2 = MathHelper.floor_double(nz + t);
        if (z1 != z2 && solidAt(x1, by, z2)) {
            return false;
        }
        int x2 = MathHelper.floor_double(nx + t);
        if (x1 != x2) {
            if (solidAt(x2, by, z1)) {
                return false;
            }
            if (z1 != z2 && solidAt(x2, by, z2)) {
                return false;
            }
        }
        return true;
    }

    private static float keyAxis(KeyBinding plus, KeyBinding minus) {
        float f = 0.0f;
        if (plus.isKeyDown()) f += 1.0f;
        if (minus.isKeyDown()) f -= 1.0f;
        return f;
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSlider.getInput()];
        tolerance = Tolerance.values()[(int) edgeTolerance.getInput()];
    }

    private static boolean unsneakReady() {
        return !unsneakDelay.isNotDefault() || (double) (System.currentTimeMillis() - sneakStart) > unsneakDelay.getInput() + 50.0 - 10.0;
    }

    @Override
    public void onPreUpdate() {
        if (!Chat.inWorld()) {
            return;
        }
        if (onlyWhileSneaking.enabled) {
            if (mode == Mode.SNEAK_KEYBIND || mode == Mode.SCAFFOLD) {
                pressSneak(sneaking);
            } else {
                pressSneak(false);
            }
        }
        if (mode == Mode.EDGE) {
            return;
        }
        if (mode == Mode.SCAFFOLD) {
            if (conditions(false)) {
                if (!scaffoldActive) {
                    scaffoldActive = true;
                    if (scaffold() != null) scaffold().enable();
                }
                if (mc().thePlayer.onGround && !GameSettings.isKeyDown(mc().gameSettings.keyBindJump) && atEdge()) {
                    setSneak(true);
                } else if (sneaking && unsneakReady()) {
                    setSneak(false);
                }
            } else if (scaffoldActive) {
                scaffoldActive = false;
                if (scaffold() != null) scaffold().disable();
                if (sneaking) {
                    setSneak(false);
                }
            }
            return;
        }
        if (!conditions(true)) {
            if (sneaking) {
                setSneak(false);
            }
            return;
        }
        if (atEdge()) {
            setSneak(true);
        } else if (sneaking && unsneakReady()) {
            setSneak(false);
        }
    }

    private static boolean solidAt(int x, int y, int z) {
        POS.set(x, y, z);
        return mc().theWorld.getBlockState(POS).getBlock().getMaterial().isSolid();
    }

    public static void setSneak(boolean b) {
        if (sneaking) {
            if (b) {
                return;
            }
        } else if (!b) {
            return;
        }
        if (!b && !onlyWhileSneaking.enabled && GameSettings.isKeyDown(mc().gameSettings.keyBindSneak)) {
            return;
        }
        sneaking = b;
        if (sneaking) {
            sneakStart = System.currentTimeMillis();
        }
        if (mode == Mode.SNEAK_KEYBIND || mode == Mode.SCAFFOLD) {
            pressSneak(b);
        }
    }
}
