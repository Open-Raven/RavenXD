package xyz.daikirai.module.network;

import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class FakeLagModule extends Module {
    public static FakeLagModule instance;
    public static SliderSetting outboundDelay, pulse;
    public static ButtonSetting disableOnAttack, showServerPosition;
    private static final Object lock = new Object();
    private static final ArrayDeque<Delayed> queue = new ArrayDeque<>();
    private static volatile long delay;
    private static volatile long passUntil;
    private static volatile long lastPulse;
    private static volatile boolean caughtUp;
    private static Timer timer;
    private static EntityOtherPlayerMP ghost;
    private static final AtomicReference<Vec3> serverPos = new AtomicReference<>();

    private static final class Delayed {
        final Packet<?> packet;
        final long time;

        Delayed(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    public FakeLagModule(int key) {
        super("Fake Lag", Category.network, key);
        instance = this;
    }

    public void link() {
        outboundDelay = (SliderSetting) ModuleManager.setting(this, "Outbound delay");
        pulse = (SliderSetting) ModuleManager.setting(this, "Pulse");
        disableOnAttack = (ButtonSetting) ModuleManager.setting(this, "Disable on attack");
        showServerPosition = (ButtonSetting) ModuleManager.setting(this, "Show server position");
    }

    @Override
    public String getInfo() {
        return delay + "ms";
    }

    @Override
    public void onEnable() {
        delay = (int) outboundDelay.getInput();
        if (showServerPosition.enabled && Chat.inWorld()) {
            spawnGhost();
        } else {
            ghost = null;
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (delay != (long) (int) outboundDelay.getInput()) {
                    instance.disable();
                    return;
                }
                if (pulse.isNotDefault()) {
                    long l = System.currentTimeMillis();
                    if (lastPulse == 0L) {
                        lastPulse = l;
                    } else if (l - lastPulse > (long) (int) pulse.getInput()) {
                        lastPulse = l;
                        release(true);
                        return;
                    }
                }
                release(false);
            }
        }, 0L, 10L);
    }

    @Override
    public void onDisable() {
        ghost = null;
        caughtUp = false;
        passUntil = 0L;
        lastPulse = 0L;
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        release(true);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (ghost != null) {
            ghost.lastTickPosX = ghost.posX;
            ghost.lastTickPosY = ghost.posY;
            ghost.lastTickPosZ = ghost.posZ;
            Vec3 v = serverPos.get();
            if (v != null) {
                ghost.setPosition(v.xCoord, v.yCoord, v.zCoord);
                caughtUp = Math.abs(ghost.posX - mc.thePlayer.posX) < 0.05 && Math.abs(ghost.posY - mc.thePlayer.posY) < 0.05
                        && Math.abs(ghost.posZ - mc.thePlayer.posZ) < 0.05;
            }
        }
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == showServerPosition) {
            if (b.enabled && Chat.inWorld()) {
                spawnGhost();
            } else {
                ghost = null;
            }
        }
    }

    private void spawnGhost() {
        Minecraft mc = Minecraft.getMinecraft();
        caughtUp = true;
        ghost = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        ghost.setEntityId(-170);
        ghost.copyLocationAndAnglesFrom(mc.thePlayer);
        serverPos.set(new Vec3(ghost.posX, ghost.posY, ghost.posZ));
    }

    public static void release(boolean all) {
        if (!Chat.inWorld()) {
            synchronized (lock) {
                queue.clear();
            }
            return;
        }
        ArrayList<Delayed> out = null;
        synchronized (lock) {
            if (queue.isEmpty()) {
                return;
            }
            if (all) {
                out = new ArrayList<>(queue.size());
                while (!queue.isEmpty()) {
                    out.add(queue.removeFirst());
                }
            } else {
                Delayed d;
                long l = System.currentTimeMillis();
                while (!queue.isEmpty() && (d = queue.peekFirst()) != null && d.time <= l) {
                    if (out == null) {
                        out = new ArrayList<>();
                    }
                    out.add(queue.removeFirst());
                }
            }
        }
        if (out != null) {
            for (Delayed d : out) {
                send(d.packet);
            }
        }
    }

    private static void send(Packet<?> p) {
        if (ghost != null && p instanceof C03PacketPlayer && ((C03PacketPlayer) p).isMoving()) {
            C03PacketPlayer c = (C03PacketPlayer) p;
            serverPos.set(new Vec3(c.getPositionX(), c.getPositionY(), c.getPositionZ()));
        }
        if (PacketQueue.active) {
            PacketQueue.hold(p);
            return;
        }
        PacketUtils.sendQueued(p);
    }

    @Override
    public boolean onSendPacket(Packet<?> p) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Chat.inWorld() || mc.isSingleplayer()) {
            return true;
        }
        if (!xyz.daikirai.hook.Hooks.finalSendCheck(p)) {
            return false;
        }
        long l = System.currentTimeMillis();
        if (passUntil != 0L) {
            if (l > passUntil) {
                passUntil = 0L;
            } else {
                return true;
            }
        }
        synchronized (lock) {
            queue.addLast(new Delayed(p, l + delay));
        }
        if (p instanceof C02PacketUseEntity && disableOnAttack.enabled && ((C02PacketUseEntity) p).getAction() == C02PacketUseEntity.Action.ATTACK) {
            instance.disable();
        }
        return false;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (ghost == null || BlinkModule.ghost != null || LagRangeModule.ghost != null || !Chat.inWorld() || mc.isSingleplayer()) {
            return;
        }
        if (SettingsModule.hideFirstPersonSelfEsp.enabled && mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        if (caughtUp && !PlayerUtils.isMoving()) {
            return;
        }
        Render3D.entityBox(ghost, 2, Theme.defaultTheme.getRGB(0.0), e.partialTicks);
    }
}
