package xyz.daikirai.module.combat;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.hook.Hooks;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.movement.LongJumpModule;
import xyz.daikirai.module.network.BacktrackModule;
import xyz.daikirai.module.player.NoRotateModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AntiKnockbackModule extends Module {
    public static AntiKnockbackModule instance;
    public static SliderSetting horizontal, vertical, cancelInterval, damageBoost, delay;
    public static ButtonSetting disableInLobby, onlyWhileMoving, cancelBurning, cancelExplosion, cancelGround, cancelOffGround, cancelWhileFalling,
            boostWithLmb, delayIfLanding, delayOnlyMidair, delayOnlyTargeting, delaySimultaneously, delayUntilGround, disableAttackWhileDelay;

    private static final Field MOTION_X = ReflectionHelper.findField(S12PacketEntityVelocity.class, "motionX", "field_149415_b");
    private static final Field MOTION_Y = ReflectionHelper.findField(S12PacketEntityVelocity.class, "motionY", "field_149416_c");
    private static final Field MOTION_Z = ReflectionHelper.findField(S12PacketEntityVelocity.class, "motionZ", "field_149414_d");

    public static final TreeMap<Long, List<Packet<?>>> delayed = new TreeMap<Long, List<Packet<?>>>();
    private static final Object LOCK = new Object();
    private static final Map<Integer, Long> delayedVelocities = new ConcurrentHashMap<Integer, Long>();
    private static final AtomicLong tick = new AtomicLong();
    public static final AtomicLong delayUntil = new AtomicLong();
    private static final AtomicLong delayTicks = new AtomicLong();
    private static final AtomicBoolean startedMidair = new AtomicBoolean();
    private static volatile Packet<?> processing;
    private static long boostStage;
    private static long cancelCount;
    private static long explosionAt;
    private static long fireballStage;
    private static long fireballAt;

    public AntiKnockbackModule(int key) {
        super("AntiKnockback", Category.combat, key);
        instance = this;
        this.lite = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        horizontal = (SliderSetting) ModuleManager.setting(this, "Horizontal");
        vertical = (SliderSetting) ModuleManager.setting(this, "Vertical");
        disableInLobby = (ButtonSetting) ModuleManager.setting(this, "Disable in lobby");
        onlyWhileMoving = (ButtonSetting) ModuleManager.setting(this, "Only while moving");
        cancelBurning = (ButtonSetting) ModuleManager.setting(this, "Cancel burning");
        cancelExplosion = (ButtonSetting) ModuleManager.setting(this, "Cancel explosion");
        cancelGround = (ButtonSetting) ModuleManager.setting(this, "Cancel ground");
        cancelOffGround = (ButtonSetting) ModuleManager.setting(this, "Cancel off ground");
        cancelWhileFalling = (ButtonSetting) ModuleManager.setting(this, "Cancel while falling");
        cancelInterval = (SliderSetting) ModuleManager.setting(this, "Cancel interval");
        damageBoost = (SliderSetting) ModuleManager.setting(this, "Damage boost");
        boostWithLmb = (ButtonSetting) ModuleManager.setting(this, "Boost with LMB");
        delay = (SliderSetting) ModuleManager.setting(this, "Delay");
        delayIfLanding = (ButtonSetting) ModuleManager.setting(this, "Delay if landing");
        delayOnlyMidair = (ButtonSetting) ModuleManager.setting(this, "Delay only midair");
        delayOnlyTargeting = (ButtonSetting) ModuleManager.setting(this, "Delay only targeting");
        delaySimultaneously = (ButtonSetting) ModuleManager.setting(this, "Delay simultaneously");
        delayUntilGround = (ButtonSetting) ModuleManager.setting(this, "Delay until ground");
        disableAttackWhileDelay = (ButtonSetting) ModuleManager.setting(this, "Disable attack while delay");
    }

    private static boolean hasVelocity(List<Packet<?>> list) {
        for (Packet<?> packet : list) {
            if (packet instanceof S12PacketEntityVelocity && isOwnVelocity((S12PacketEntityVelocity) packet)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOwnVelocity(S12PacketEntityVelocity p) {
        return p.getEntityID() == mc().thePlayer.getEntityId() && p.getMotionY() != -627 && p.getMotionY() < 4500 && System.currentTimeMillis() != PacketUtils.lastExplosion;
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        Minecraft mc = mc();
        if (packet == processing) {
            return true;
        }
        if (packet == PacketUtils.reprocessing) {
            return filter(packet);
        }
        if (delayTicks.get() != 0L) {
            if (handler instanceof INetHandlerPlayClient && !mc.isSingleplayer() && active()) {
                List<Packet<?>> list = null;
                synchronized (LOCK) {
                    if (delayUntil.get() == 0L && packet instanceof S12PacketEntityVelocity && isOwnVelocity((S12PacketEntityVelocity) packet)) {
                        boolean start = true;
                        if (delayOnlyMidair.isToggled() && mc.thePlayer.onGround || delayOnlyTargeting.isToggled() && !PlayerUtils.targetMatches(1)
                                || delayIfLanding.isToggled() && !PlayerUtils.landsWithin(delayTicks.intValue())) {
                            start = false;
                        }
                        if (start) {
                            if (BacktrackModule.holding) {
                                BacktrackModule.flush();
                            }
                            delayUntil.set(tick.get());
                            startedMidair.set(!mc.thePlayer.onGround);
                        }
                    }
                    if (delayUntil.get() != 0L && !passesThrough(packet)) {
                        boolean hold = true;
                        if (!delaySimultaneously.isToggled() && packet instanceof S12PacketEntityVelocity && isOwnVelocity((S12PacketEntityVelocity) packet) && anyVelocityDelayed()) {
                            list = drain();
                            delayedVelocities.clear();
                            delayUntil.set(0L);
                            hold = false;
                        }
                        if (hold) {
                            delay(packet);
                            return false;
                        }
                    }
                }
                if (list != null) {
                    process(list);
                }
            } else if (delayUntil.get() != 0L) {
                synchronized (LOCK) {
                    delayed.clear();
                    delayedVelocities.clear();
                    delayUntil.set(0L);
                }
            }
        }
        return filter(packet);
    }

    private static boolean process(List<Packet<?>> list) {
        if (list.isEmpty()) {
            return false;
        }
        boolean velocity = false;
        for (Packet<?> packet : list) {
            if (!velocity && packet instanceof S12PacketEntityVelocity && isOwnVelocity((S12PacketEntityVelocity) packet)) {
                velocity = true;
            }
            if (!filter(packet)) continue;
            try {
                processing = packet;
                PacketUtils.reprocessing = processing;
                Hooks.onReceive(packet, mc().getNetHandler().getNetworkManager().getNetHandler());
            } catch (Throwable throwable) {
                Chat.print(MathUtils.timeDigits() + " &cdelay kb e4 " + packet.getClass().getSimpleName() + " " + throwable.getClass().getSimpleName());
            } finally {
                processing = null;
                PacketUtils.reprocessing = null;
            }
        }
        return velocity;
    }

    @Override
    public String getDisplayName() {
        return this.displayName("Velocity");
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            tick.set(0L);
            boostStage = 0L;
        }
    }

    public static void reset() {
        release(true);
        delayUntil.set(0L);
        startedMidair.set(false);
        delayedVelocities.clear();
    }

    @Override
    public void onEnable() {
        delayTicks.set((int) delay.getInput() / 50);
    }

    public static void release(boolean all) {
        List<Packet<?>> list = null;
        synchronized (LOCK) {
            if (delayed.isEmpty()) {
                return;
            }
            if (!Chat.inWorld()) {
                delayed.clear();
                delayedVelocities.clear();
                delayUntil.set(0L);
                return;
            }
            if (all) {
                list = drain();
                delayUntil.set(0L);
            } else {
                boolean velocity = false;
                java.util.Iterator<Map.Entry<Long, List<Packet<?>>>> it = delayed.entrySet().iterator();
                Map.Entry<Long, List<Packet<?>>> entry;
                while (it.hasNext() && (entry = it.next()).getKey() <= tick.get()) {
                    List<Packet<?>> packets = entry.getValue();
                    if (!packets.isEmpty()) {
                        if (!velocity && hasVelocity(packets)) {
                            velocity = true;
                        }
                        if (list == null) {
                            list = new ArrayList<Packet<?>>();
                        }
                        list.addAll(packets);
                        packets.clear();
                    }
                    it.remove();
                }
                if (velocity && !anyVelocityDelayed()) {
                    List<Packet<?>> rest = drain();
                    delayUntil.set(0L);
                    if (rest != null && !rest.isEmpty()) {
                        list.addAll(rest);
                    }
                }
            }
        }
        if (list != null) {
            process(list);
        }
    }

    private static List<Packet<?>> drain() {
        ArrayList<Packet<?>> list = null;
        if (!delayed.isEmpty()) {
            for (List<Packet<?>> packets : delayed.values()) {
                if (packets.isEmpty()) continue;
                if (list == null) {
                    list = new ArrayList<Packet<?>>();
                }
                list.addAll(packets);
                packets.clear();
            }
            delayed.clear();
        }
        return list;
    }

    private static boolean canBoost() {
        Minecraft mc = mc();
        return !(mc.thePlayer.onGround || !PlayerUtils.isMoving() || mc.thePlayer.isBurning() || PlayerUtils.inLiquid() || mc.currentScreen != null || NoRotateModule.sinceSetback(3000L));
    }

    @Override
    public void onDisable() {
        boostStage = 0L;
        cancelCount = 0L;
        explosionAt = 0L;
        resetFireball();
        reset();
    }

    private static void resetFireball() {
        fireballStage = 0L;
        fireballAt = 0L;
    }

    private static boolean anyVelocityDelayed() {
        for (List<Packet<?>> list : delayed.values()) {
            if (list.isEmpty() || !hasVelocity(list)) continue;
            return true;
        }
        return false;
    }

    @Override
    public String getInfo() {
        if (delay.isNotDefault() && !horizontal.isNotDefault() && !vertical.isNotDefault()) {
            return delay.getDisplay(false);
        }
        return horizontal.getDisplay(false) + " " + vertical.getDisplay(false);
    }

    private static boolean passesThrough(Packet<?> packet) {
        return packet instanceof S02PacketChat || packet instanceof S06PacketUpdateHealth || packet instanceof S14PacketEntity || packet instanceof S19PacketEntityStatus
                || packet instanceof S22PacketMultiBlockChange || packet instanceof S23PacketBlockChange || packet instanceof S29PacketSoundEffect
                || packet instanceof S2APacketParticles || packet instanceof S3BPacketScoreboardObjective || packet instanceof S3CPacketUpdateScore
                || packet instanceof S3DPacketDisplayScoreboard || packet instanceof S3EPacketTeams;
    }

    public static boolean delayable(Packet<?> packet) {
        return !passesThrough(packet);
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (!(packet instanceof C08PacketPlayerBlockPlacement)) {
            return true;
        }
        C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
        if (p.getPlacedBlockDirection() == 255) {
            return true;
        }
        if (p.getStack() == null || p.getStack().getItem() != Items.fire_charge) {
            return true;
        }
        if (ModuleManager.enabled("Long Jump") != null && LongJumpModule.fireballAt > 0L) {
            return true;
        }
        fireballAt = System.currentTimeMillis();
        fireballStage = 1L;
        return true;
    }

    @Override
    public void onUpdate() {
        Minecraft mc = mc();
        int n = (int) delay.getInput() / 50;
        if (delayTicks.get() != (long) n && delayUntil.get() != 0L) {
            release(true);
        }
        delayTicks.set(n);
        if (fireballStage > 0L && Math.abs(System.currentTimeMillis() - fireballAt) > 750L) {
            resetFireball();
        }
        if (boostStage > 0L) {
            double d = MoveUtils.speed();
            if (boostStage == 1L) {
                double d2 = d * damageBoost.getInput();
                d2 = Math.max(d2, 0.35);
                MoveUtils.strafe(d2 -= MathUtils.randomTimes(0.001));
            } else if (boostStage == 2L) {
                MoveUtils.strafe(0.35 - MathUtils.randomTimes(0.001));
            }
            boostStage = 0L;
        }
        if (delayUntil.get() != 0L) {
            if (delayUntilGround.isToggled() && startedMidair.get() && mc.thePlayer.onGround) {
                release(true);
            } else {
                release(false);
            }
        }
        tick.incrementAndGet();
    }

    private static void delay(Packet<?> packet) {
        long l = tick.get() + delayTicks.get();
        if (packet instanceof S12PacketEntityVelocity && isOwnVelocity((S12PacketEntityVelocity) packet)) {
            delayedVelocities.put(packet.hashCode(), System.currentTimeMillis());
        }
        List<Packet<?>> list = delayed.computeIfAbsent(l, k -> new ArrayList<Packet<?>>());
        list.add(packet);
    }

    private static boolean scale(S12PacketEntityVelocity p) {
        double h = horizontal.getInput();
        double v = vertical.getInput();
        if (h == 0.0 && v == 0.0) {
            return false;
        }
        try {
            if (h == 0.0) {
                mc().thePlayer.motionY = (double) MOTION_Y.getInt(p) / 8000.0 * (v / 100.0);
                return false;
            }
            if (h != 100.0) {
                double d = h / 100.0;
                int x = (int) ((double) MOTION_X.getInt(p) * d);
                int z = (int) ((double) MOTION_Z.getInt(p) * d);
                MOTION_X.setInt(p, x);
                MOTION_Z.setInt(p, z);
            }
            if (v != 100.0) {
                int y = (int) ((double) MOTION_Y.getInt(p) * (v / 100.0));
                MOTION_Y.setInt(p, y);
            }
        } catch (IllegalAccessException illegalAccessException) {
        }
        return true;
    }

    private static boolean filter(Packet<?> packet) {
        Minecraft mc = mc();
        if (active()) {
            if (packet instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) packet;
                if (p.getEntityID() != mc.thePlayer.getEntityId()) {
                    return true;
                }
                if (fireballStage == 2L) {
                    resetFireball();
                    return true;
                }
                if (ModuleManager.enabled("Long Jump") != null && LongJumpModule.stage == 0) {
                    return true;
                }
                if (explosionAt > 0L) {
                    if (Math.abs(System.currentTimeMillis() - explosionAt) < 10L) {
                        explosionAt = 0L;
                        return false;
                    }
                    explosionAt = 0L;
                }
                if (!mc.thePlayer.onGround) {
                    if (damageBoost.isNotDefault() && (!boostWithLmb.isToggled() || Mouse.isButtonDown(0)) && canBoost()) {
                        boostStage = 1L;
                        return true;
                    }
                    if (cancelOffGround.isToggled()) {
                        return false;
                    }
                    if (cancelWhileFalling.isToggled() && (mc.thePlayer.motionY < -0.55 || mc.thePlayer.motionY < -0.08 && PlayerUtils.heightAboveGround() > 5.0)) {
                        return false;
                    }
                } else if (cancelGround.isToggled()) {
                    return false;
                }
                if (cancelBurning.isToggled() && mc.thePlayer.isBurning() && Math.abs(p.getMotionX()) < 2010 && Math.abs(p.getMotionZ()) < 2010) {
                    return false;
                }
                if (cancelInterval.isNotDefault() && (double) (++cancelCount) >= cancelInterval.getInput() && !mc.thePlayer.onGround) {
                    cancelCount = 0L;
                    return false;
                }
                return scale(p);
            }
            if (packet instanceof S27PacketExplosion) {
                S27PacketExplosion p = (S27PacketExplosion) packet;
                double d = mc.thePlayer.getDistanceSq(p.getX(), p.getY(), p.getZ());
                if (fireballStage == 1L && d <= 6.0) {
                    fireballAt = System.currentTimeMillis();
                    fireballStage = 2L;
                    return true;
                }
                if (cancelExplosion.isToggled() && d <= 25.0) {
                    explosionAt = System.currentTimeMillis();
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean active() {
        if (Chat.inWorld()) {
            if (onlyWhileMoving.isToggled() && !PlayerUtils.isMoving()) {
                return false;
            }
            return !disableInLobby.isToggled() || Hypixel.state != Hypixel.State.LOBBY;
        }
        return false;
    }
}
