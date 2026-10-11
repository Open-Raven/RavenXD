package xyz.daikirai.module.movement;

import xyz.daikirai.client.Chat;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.MovingObjectPosition;

public class LongJumpModule extends Module {
    public static LongJumpModule instance;
    public static SliderSetting modeSetting, horizontalBoost, verticalMotion, motionTicks;
    public static ButtonSetting addMotion, allowStrafe, invertYaw, jump, stopMotionOnDisable;

    public enum Mode {
        Vanilla(false), Fireball_Auto(true);

        public final boolean waitsForExplosion;

        Mode(boolean waitsForExplosion) {
            this.waitsForExplosion = waitsForExplosion;
        }
    }

    public static Mode mode = Mode.Fireball_Auto;
    public static int throwTicks;
    public static int stage = -1;
    public static long fireballAt;
    public static int previousSlot = -1;
    private static int jumpTicks;

    public LongJumpModule(int key) {
        super("Long Jump", Category.movement, key);
        instance = this;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        horizontalBoost = (SliderSetting) ModuleManager.setting(this, "Horizontal boost");
        verticalMotion = (SliderSetting) ModuleManager.setting(this, "Vertical motion");
        motionTicks = (SliderSetting) ModuleManager.setting(this, "Motion ticks");
        addMotion = (ButtonSetting) ModuleManager.setting(this, "Add motion");
        allowStrafe = (ButtonSetting) ModuleManager.setting(this, "Allow strafe");
        invertYaw = (ButtonSetting) ModuleManager.setting(this, "Invert yaw");
        jump = (ButtonSetting) ModuleManager.setting(this, "Jump");
        stopMotionOnDisable = (ButtonSetting) ModuleManager.setting(this, "Stop motion on disable");
    }

    @Override
    public int spoofedSlot() {
        return previousSlot;
    }

    private static void boost() {
        if (!horizontalBoost.isAtMin()) {
            double d = horizontalBoost.getInput() - MathUtils.randomTimes(1.0E-4);
            MoveUtils.strafe(d);
        }
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (!(packet instanceof S27PacketExplosion)) {
            return true;
        }
        S27PacketExplosion p = (S27PacketExplosion) packet;
        if (fireballAt == 0L || mc().thePlayer.getDistanceSq(p.getX(), p.getY(), p.getZ()) > 9.0) {
            return true;
        }
        fireballAt = 0L;
        restoreSlot();
        stage = 0;
        return false;
    }

    @Override
    public void onPreUpdate() {
        if (stage == -1) {
            if (!mode.waitsForExplosion) {
                stage = 1;
            }
        } else if ((double) (++stage) > motionTicks.getInput()) {
            this.disable();
            return;
        }
        if (mode == Mode.Fireball_Auto) {
            if (fireballAt > 0L && Math.abs(System.currentTimeMillis() - fireballAt) > 750L) {
                Chat.moduleMessage(this, "&cFireball timed out.");
                this.disable();
                return;
            }
            if (stage > 0) {
                vertical();
                if (allowStrafe.isToggled()) {
                    MoveUtils.strafe(MoveUtils.speed());
                }
            }
        }
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
        if (this.isEnabled()) {
            this.disable();
        }
    }

    private static void restoreSlot() {
        if (previousSlot != -1) {
            mc().thePlayer.inventory.currentItem = previousSlot;
            previousSlot = -1;
        }
    }

    private static void vertical() {
        if (!verticalMotion.isAtMin()) {
            double d = verticalMotion.getInput();
            mc().thePlayer.motionY = addMotion.isToggled() ? (mc().thePlayer.motionY += d) : d;
        }
    }

    @Override
    public void onPostPlayerInput() {
        if (jumpTicks > 0 && --jumpTicks == 0) {
            PlayerUtils.setJump(true);
        }
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (throwTicks <= 0) {
            return null;
        }
        return throwRotation();
    }

    private static float[] throwRotation() {
        float y;
        float p;
        if (invertYaw.isToggled()) {
            y = mc().thePlayer.rotationYaw - 180.0f + RotationUtils.randomOffset();
            p = 85.0f;
        } else {
            y = mc().thePlayer.rotationYaw;
            p = 90.0f;
        }
        return new float[]{y, p};
    }

    @Override
    public void onEnable() {
        int n;
        if (mode == Mode.Fireball_Auto && (n = findThrowable(true)) != -1) {
            previousSlot = mc().thePlayer.inventory.currentItem;
            mc().thePlayer.inventory.currentItem = n;
            throwTicks = 1;
        }
    }

    @Override
    public void onDisable() {
        if (stopMotionOnDisable.isToggled()) {
            MoveUtils.strafe(0.0);
        }
        jumpTicks = 0;
        throwTicks = 0;
        fireballAt = 0;
        stage = -1;
        restoreSlot();
    }

    private static int fireballSlot() {
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            ItemStack itemStack = mc().thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null || itemStack.getItem() != Items.fire_charge) continue;
            return i;
        }
        return -1;
    }

    private static int findThrowable(boolean onEnable) {
        int n = fireballSlot();
        if (n == -1) {
            Chat.moduleMessage(instance, "&cFireball not found.");
            instance.disable();
        } else if (mc().thePlayer.getHealth() < 3.0f || onEnable && PlayerUtils.heightAboveGround() > 3.0 || !onEnable && !PacketUtils.noInteraction()) {
            Chat.moduleMessage(instance, "&cCan't throw fireball right now.");
            instance.disable();
            n = -1;
        }
        return n;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        switch (mode) {
            case Vanilla: {
                boost();
                vertical();
                break;
            }
            case Fireball_Auto: {
                if (throwTicks > 0 && ++throwTicks >= 3) {
                    throwTicks = 0;
                    int n = findThrowable(false);
                    if (n == -1) break;
                    mc.thePlayer.inventory.currentItem = n;
                    float[] rot = throwRotation();
                    MovingObjectPosition hit = PlayerUtils.rayTraceBlocks(3.0, rot[0], rot[1]);
                    MovingObjectPosition saved = null;
                    if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        saved = mc.objectMouseOver;
                        mc.objectMouseOver = hit;
                    }
                    fireballAt = System.currentTimeMillis();
                    PlayerUtils.rightClickMouse();
                    if (saved != null) {
                        mc.objectMouseOver = saved;
                    }
                    if (!jump.isToggled()) break;
                    jumpTicks = 2;
                    break;
                }
                if (stage != 1 || horizontalBoost.isAtMin()) break;
                if (invertYaw.isToggled()) {
                    mc.thePlayer.motionY += 0.025;
                }
                boost();
            }
        }
    }
}
