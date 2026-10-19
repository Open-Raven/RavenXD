package openraven.ravenxd.module.combat;

import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MovingObjectPosition;

import java.util.concurrent.ThreadLocalRandom;

//todo: make adding folders for modes possible + easier:
//https://github.com/Open-Raven/RavenAPlus/tree/master/src/main/java/keystrokesmod/module/impl/combat/criticals
// or smthn

//best pandaware skid?
public class CriticalsModule extends Module {
    public enum Mode {
        Verus, Vulcan, Packet
    }

    public static SliderSetting mode, vulcanMode;
    private int stage;
    private Mode lastMode;

    public CriticalsModule(int key) {
        super("Criticals", Category.combat, key);
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        vulcanMode = (SliderSetting) ModuleManager.setting(this, "Vulcan mode");
    }

    @Override
    public void onEnable() {
        this.stage = 0;
    }

    @Override
    public void onDisable() {
        this.stage = 0;
    }

    private static boolean attacking(Minecraft mc) {
        if (KillAuraModule.hasTarget()) {
            return true;
        }
        MovingObjectPosition hit = mc.objectMouseOver;
        return mc.gameSettings.keyBindAttack.isKeyDown() && hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
    }

    private static boolean onGround(Minecraft mc) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, -0.1, 0.0)).isEmpty();
    }

    private static boolean blockAbove(Minecraft mc) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().addCoord(0.0, 1.0, 0.0)).isEmpty();
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        Mode m = Mode.values()[(int) mode.getInput()];
        if (m != this.lastMode) {
            this.lastMode = m;
            this.stage = 0;
        }
        if (ModuleManager.enabled("Fly") != null) {
            return;
        }
        if (!attacking(mc)) {
            // finish
            if (m != Mode.Packet && this.stage != 0) {
                ++this.stage;
            }
            return;
        }
        switch (m) {
            case Verus:
                this.verus(mc, p, e);
                break;
            case Vulcan:
                this.vulcan(mc, p, e);
                break;
            case Packet:
                this.packet(p);
                break;
        }
    }

    private void verus(Minecraft mc, EntityPlayerSP p, MotionEvent e) {
        double yGround = p.posY % 0.015625;
        if (onGround(mc) && yGround >= 0.0 && yGround < 0.1 && !blockAbove(mc)) {
            switch (this.stage++) {
                case 2:
                case 1:
                    e.onGround = false;
                    e.y += 0.419999986886978;
                    break;
                case 4:
                case 3:
                    e.onGround = false;
                    e.y += 0.341599985361099;
                    break;
                case 6:
                case 5:
                    e.onGround = false;
                    e.y += 0.186367980844497;
                    break;
            }
            ++this.stage;
            if (this.stage >= 9) {
                this.stage = 0;
            }
        } else {
            this.stage = 0;
        }
    }

    private void vulcan(Minecraft mc, EntityPlayerSP p, MotionEvent e) {
        double yGround = p.posY % 0.015625;
        if (!onGround(mc) || yGround < 0.0 || yGround >= 0.1 || blockAbove(mc)) {
            return;
        }
        if (vulcanMode.getInput() == 0.0) {
            this.vulcanNew(e);
        } else {
            this.vulcanOld(e);
        }
        if (!PlayerUtils.isMoving() && p.ticksExisted % 2 == 0) {
            double yaw = Math.toRadians(p.rotationYaw);
            e.x += -Math.sin(yaw) * 0.1;
            e.z += Math.cos(yaw) * 0.1;
        }
    }

    private void vulcanNew(MotionEvent e) {
        switch (this.stage++) {
            case 2:
                e.y += 0.42f;
                e.onGround = false;
                break;
            case 3:
            case 4:
                e.y += 0.007531999805212;
                e.onGround = false;
                break;
            case 16:
                this.stage = 0;
                break;
        }
    }

    private void vulcanOld(MotionEvent e) {
        switch (this.stage++) {
            case 3:
            case 2:
                e.y += 0.42f;
                e.onGround = false;
                break;
            case 4:
            case 5:
                e.y += 0.007531999805212;
                e.onGround = false;
                break;
            case 6:
            case 7:
                e.y += 0.00133597911214;
                e.onGround = false;
                break;
            case 8:
            case 9:
                e.y += 0.16610926093895;
                e.onGround = false;
                break;
            case 26:
                this.stage = 0;
                break;
        }
    }

    private void packet(EntityPlayerSP p) {
        if (p.onGround && p.posY % 0.015625 == 0.0 && p.ticksExisted % 2 == 0) {
            double value = ThreadLocalRandom.current().nextDouble(0.0615, 0.0645);
            PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(p.posX, p.posY + value, p.posZ, false));
            PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(p.posX, p.posY, p.posZ, false));
        }
    }
}
