package openraven.ravenxd.module.player;

import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.PlayerUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

// yuri antivoid
public class AntiVoidModule extends Module {
    public static SliderSetting mode, distance;
    private static final int holdticks = 30; // change ts for hold
    private final List<Packet<?>> held = new ArrayList<Packet<?>>();
    private boolean blinking, hasSafe;
    private double safeX, safeY, safeZ;
    private int holdTicks;

    public AntiVoidModule(int key) {
        super("AntiVoid", Category.player, key);
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        distance = (SliderSetting) ModuleManager.setting(this, "Distance");
    }

    @Override
    public String getInfo() {
        return mode.getDisplay(false);
    }

    @Override
    public void onEnable() {
        this.hasSafe = false;
        this.blinking = false;
        this.held.clear();
    }

    @Override
    public void onDisable() {
        this.release();
        this.hasSafe = false;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            this.held.clear();
            this.blinking = false;
            this.hasSafe = false;
        }
    }

    // todo: putt this in util
    private static boolean overVoid() {
        return PlayerUtils.heightAboveGround() == -1.0;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        EntityPlayerSP p = mc.thePlayer;
        if (mode.getInput() == 2.0) {
            this.blink(p, e);
            return;
        }
        if (p.fallDistance <= distance.getInput() || !overVoid()) {
            return;
        }
        if (mode.getInput() == 0.0) {
            PacketUtils.send(new C03PacketPlayer.C04PacketPlayerPosition());
        } else if (p.posY + p.motionY < Math.floor(p.posY)) {
            p.motionY = Math.floor(p.posY) - p.posY;
            if (p.motionY == 0.0) {
                p.onGround = true;
                e.onGround = true;
            }
        }
    }

    private void blink(EntityPlayerSP p, MotionEvent e) {
        if (p.onGround) {
            this.release();
            this.safeX = p.posX;
            this.safeY = p.posY;
            this.safeZ = p.posZ;
            this.hasSafe = true;
            return;
        }
        boolean overVoid = overVoid();
        if (!this.blinking) {
            if (overVoid && this.hasSafe) {
                this.blinking = true;
                this.holdTicks = 0;
            }
            return;
        }
        if (!overVoid || ++this.holdTicks > holdticks) {
            this.release();
        } else if (p.fallDistance > distance.getInput()) {
            this.held.clear();
            this.blinking = false;
            p.setPosition(this.safeX, this.safeY, this.safeZ);
            p.motionX = 0.0;
            p.motionY = 0.0;
            p.motionZ = 0.0;
            p.fallDistance = 0.0f;
            e.x = this.safeX;
            e.y = this.safeY;
            e.z = this.safeZ;
            e.onGround = true;
        }
    }

    private void release() {
        this.blinking = false;
        if (this.held.isEmpty()) {
            return;
        }
        for (Packet<?> packet : this.held) {
            PacketUtils.sendNoEvent(packet);
        }
        this.held.clear();
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (this.blinking && packet instanceof C03PacketPlayer) {
            this.held.add(packet);
            return false;
        }
        return true;
    }
}
