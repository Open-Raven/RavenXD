package xyz.daikirai.module.combat;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class JumpResetModule extends Module {
    public static JumpResetModule instance;
    public static SliderSetting chance;
    public static ButtonSetting minimizeVertical, onlyWhileTargeting;
    public static boolean pending;
    private static boolean explosion;
    private static double kbX, kbZ;
    private static int kbY;

    public JumpResetModule(int key) {
        super("Jump Reset", Category.combat, key);
        instance = this;
    }

    public void link() {
        chance = (SliderSetting) ModuleManager.setting(this, "Chance");
        minimizeVertical = (ButtonSetting) ModuleManager.setting(this, "Minimize vertical");
        onlyWhileTargeting = (ButtonSetting) ModuleManager.setting(this, "Only while targeting");
    }

    public static boolean on() {
        return instance != null && instance.isEnabled();
    }

    @Override
    public boolean onReceivePacket(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        if (!(packet instanceof S27PacketExplosion)) {
            return true;
        }
        S27PacketExplosion p = (S27PacketExplosion) packet;
        if (Minecraft.getMinecraft().thePlayer.getDistanceSq(p.getX(), p.getY(), p.getZ()) > 36.0) {
            return true;
        }
        explosion = true;
        return true;
    }

    @Override
    public void onDisable() {
        explosion = false;
        pending = false;
    }

    @Override
    public void onKnockback(S12PacketEntityVelocity p, long now) {
        if (explosion) {
            return;
        }
        if (minimizeVertical.enabled && (long) p.getMotionY() < 3000L) {
            return;
        }
        kbX = (double) p.getMotionX() / 8000.0;
        kbY = p.getMotionY();
        kbZ = (double) p.getMotionZ() / 8000.0;
        pending = true;
    }

    @Override
    public void onUpdateEntityActionState() {
        Minecraft mc = Minecraft.getMinecraft();
        if (pending) {
            pending = false;
            if (!mc.thePlayer.onGround || !mc.thePlayer.isSprinting()) {
                return;
            }
            if (PlayerUtils.inLiquid() || mc.thePlayer.isRiding() || mc.thePlayer.isPotionActive(Potion.jump)) {
                return;
            }
            if (onlyWhileTargeting.enabled && !PlayerUtils.targetMatches(1)) {
                return;
            }
            if (chance.getInput() != 100.0 && !(Math.random() < chance.getInput() / 100.0)) {
                return;
            }
            if (!helps()) {
                return;
            }
            mc.thePlayer.setJumping(true);
        }
    }

    private static boolean helps() {
        if (kbX == 0.0 && kbZ == 0.0) {
            return false;
        }
        double kb = Math.sqrt(kbX * kbX + kbZ * kbZ);
        float yaw = MathHelper.wrapAngleTo180_float(RotationManager.movementYaw());
        double after = withJump(kbX, kbZ, yaw);
        double less = (kb - after) / kb;
        return less >= 0.03;
    }

    @Override
    public void onUpdate() {
        explosion = false;
    }

    private static double withJump(double x, double z, float yaw) {
        double r = Math.toRadians(yaw);
        double nx = x + -Math.sin(r) * 0.2;
        double nz = z + Math.cos(r) * 0.2;
        return Math.sqrt(nx * nx + nz * nz);
    }
}
