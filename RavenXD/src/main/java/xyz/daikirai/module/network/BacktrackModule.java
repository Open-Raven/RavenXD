package xyz.daikirai.module.network;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.hook.Hooks;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.combat.AimAssistModule;
import xyz.daikirai.module.combat.AntiKnockbackModule;
import xyz.daikirai.module.combat.KillAuraModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.module.world.AntiBotModule;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BacktrackModule extends Module {
    public static BacktrackModule instance;
    public static SliderSetting delay, distanceMin, distanceMax, cooldown, fov;
    public static ButtonSetting disableAdventure, disableWhileChasing, flushOnDamage, ignoreTeammates, showServerPosition, onlyHittable, onlyMovement,
            weaponOnly;

    private static final TreeMap<Integer, List<Packet<?>>> held = new TreeMap<Integer, List<Packet<?>>>();
    private static final Object LOCK = new Object();
    private static int tick;
    private static volatile int delayTicks;
    public static volatile boolean holding;
    private static volatile boolean ready;
    public static volatile EntityLivingBase target;
    private static volatile Vec3 serverPos;
    public static volatile EntityLivingBase ghost;
    private static volatile Packet<?> processing;
    private static volatile long lastFlush;
    private static double minRange, maxRange, maxRangeSq, scanRangeSq;

    public BacktrackModule(int key) {
        super("Backtrack", Category.network, key);
        instance = this;
        this.lite = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        delay = (SliderSetting) ModuleManager.setting(this, "Delay");
        distanceMin = (SliderSetting) ModuleManager.setting(this, "Distance (min)");
        distanceMax = (SliderSetting) ModuleManager.setting(this, "Distance (max)");
        cooldown = (SliderSetting) ModuleManager.setting(this, "Cooldown");
        fov = (SliderSetting) ModuleManager.setting(this, "FOV");
        disableAdventure = (ButtonSetting) ModuleManager.setting(this, "Disable adventure");
        disableWhileChasing = (ButtonSetting) ModuleManager.setting(this, "Disable while chasing");
        flushOnDamage = (ButtonSetting) ModuleManager.setting(this, "Flush on damage");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        showServerPosition = (ButtonSetting) ModuleManager.setting(this, "Show server position");
        onlyHittable = (ButtonSetting) ModuleManager.setting(this, "Only hittable");
        onlyMovement = (ButtonSetting) ModuleManager.setting(this, "Only movement");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
        guiUpdate();
    }

    @Override
    public Entity getTarget() {
        return target;
    }

    private static boolean canStart() {
        if (FreezeModule.frozen || AntiKnockbackModule.delayUntil.get() != 0L) {
            return false;
        }
        return !cooldown.isNotDefault() || (double) Math.abs(System.currentTimeMillis() - lastFlush) > cooldown.getInput() * 1000.0 - 20.0;
    }

    private static void start() {
        delayTicks = Math.max(1, (int) delay.getInput() / 50);
        updateServerPos();
        holding = true;
        if (showServerPosition.isToggled()) {
            makeGhost(target);
        }
    }

    @Override
    public void onDisable() {
        flush();
    }

    @Override
    public String getInfo() {
        return delay.getDisplay(false);
    }

    private static boolean passesThrough(Packet<?> packet) {
        return packet instanceof S02PacketChat || packet instanceof S06PacketUpdateHealth || packet instanceof S14PacketEntity.S16PacketEntityLook
                || packet instanceof S19PacketEntityStatus || packet instanceof S22PacketMultiBlockChange || packet instanceof S23PacketBlockChange
                || packet instanceof S29PacketSoundEffect || packet instanceof S2APacketParticles || packet instanceof S3BPacketScoreboardObjective
                || packet instanceof S3CPacketUpdateScore || packet instanceof S3DPacketDisplayScoreboard || packet instanceof S3EPacketTeams;
    }

    public static void flush() {
        ArrayList<Packet<?>> list = null;
        synchronized (LOCK) {
            if (!held.isEmpty()) {
                int n = held.size();
                for (List<Packet<?>> packets : held.values()) {
                    if (packets.isEmpty()) continue;
                    if (list == null) {
                        list = new ArrayList<Packet<?>>();
                    }
                    list.addAll(packets);
                    packets.clear();
                }
                if (held.size() > n) {
                    Chat.print(MathUtils.timeDigits() + " &cbacktrack e1 t=" + tick + " s1=" + n + " s2= " + held.size());
                }
                for (Map.Entry<Integer, List<Packet<?>>> entry : held.entrySet()) {
                    List<Packet<?>> packets = entry.getValue();
                    if (packets.isEmpty()) continue;
                    Packet<?> packet = packets.get(0);
                    Chat.print(MathUtils.timeDigits() + " &cbacktrack e2 t=" + tick + " s=" + held.size() + " p=" + packets.size() + " f=" + packet.getClass().getSimpleName());
                }
                held.clear();
            }
            reset();
        }
        if (list != null) {
            process(list);
        }
    }

    private static boolean valid(EntityLivingBase e) {
        if (onlyHittable.isToggled() && (double) e.hurtTime > SettingsModule.hittableHurtTime.getInput()) {
            return false;
        }
        return !disableWhileChasing.isToggled() || !PlayerUtils.awayFromPlayer(e);
    }

    @Override
    public void guiUpdate() {
        SliderSetting.order(distanceMin, distanceMax);
        minRange = distanceMin.getInput();
        maxRange = distanceMax.getInput();
        double d = maxRange + 1.2;
        double d2 = maxRange + 2.5;
        maxRangeSq = d * d;
        scanRangeSq = d2 * d2;
    }

    private static void reset() {
        ready = false;
        holding = false;
        ghost = null;
        target = null;
        tick = 0;
        lastFlush = System.currentTimeMillis();
    }

    private static boolean targetValid() {
        Minecraft mc = mc();
        if (!Chat.inWorld() || mc.isSingleplayer()) {
            return false;
        }
        if (!(target != null && mc.theWorld.getEntityByID(target.getEntityId()) != null) || target.deathTime != 0) {
            return false;
        }
        if (mc.currentScreen != null && !Overlay.ownScreen()) {
            return false;
        }
        if (mc.thePlayer.capabilities.allowFlying || disableAdventure.isToggled() && PlayerUtils.isAdventure()) {
            return false;
        }
        return !weaponOnly.isToggled() || PlayerUtils.holdingWeapon();
    }

    private static void updateServerPos() {
        serverPos = new Vec3((double) target.serverPosX / 32.0, (double) target.serverPosY / 32.0, (double) target.serverPosZ / 32.0);
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        Minecraft mc = mc();
        try {
            if (packet == processing || packet == PacketUtils.reprocessing) {
                return true;
            }
            if (!(handler instanceof INetHandlerPlayClient) || !Chat.inWorld()) {
                if (holding) {
                    synchronized (LOCK) {
                        held.clear();
                        reset();
                    }
                }
                return true;
            }
            check:
            {
                if (holding) {
                    boolean flush = packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect;
                    if (!flush && flushOnDamage.isToggled() && packet instanceof S12PacketEntityVelocity
                            && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                        flush = true;
                    }
                    if (!flush) break check;
                    flush();
                    return true;
                }
                if (!ready) {
                    return true;
                }
            }
            Vec3 pos = serverPos;
            double x = pos.xCoord;
            double y = pos.yCoord;
            double z = pos.zCoord;
            boolean moves = false;
            boolean teleport = false;
            if (packet instanceof S14PacketEntity.S15PacketEntityRelMove || packet instanceof S14PacketEntity.S17PacketEntityLookMove) {
                S14PacketEntity p = (S14PacketEntity) packet;
                Entity entity = p.getEntity(mc.theWorld);
                if (entity != null && target != null && entity.getEntityId() == target.getEntityId()) {
                    moves = true;
                    x += (double) p.func_149062_c() / 32.0;
                    y += (double) p.func_149061_d() / 32.0;
                    z += (double) p.func_149064_e() / 32.0;
                }
            } else if (packet instanceof S18PacketEntityTeleport) {
                S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
                if (target != null && p.getEntityId() == target.getEntityId()) {
                    teleport = true;
                    moves = true;
                    x = (double) p.getX() / 32.0;
                    y = (double) p.getY() / 32.0;
                    z = (double) p.getZ() / 32.0;
                }
            }
            if (moves) {
                boolean flush = true;
                double distSq = mc.thePlayer.getDistanceSq(x, y, z);
                AxisAlignedBB box;
                AxisAlignedBB newBox;
                float[] rot;
                if (distSq <= maxRangeSq && !PlayerUtils.rayHits(target, newBox = (box = target.getEntityBoundingBox()).offset(x - target.posX, y - target.posY, z - target.posZ),
                        (rot = RotationUtils.toBox(target, newBox))[0], rot[1], minRange)) {
                    boolean inRange = PlayerUtils.rayHits(target, newBox, rot[0], rot[1], maxRange);
                    if (inRange) {
                        AxisAlignedBB oldBox = box.offset(pos.xCoord - target.posX, pos.yCoord - target.posY, pos.zCoord - target.posZ);
                        float[] oldRot = RotationUtils.toBox(target, oldBox);
                        double newDist = PlayerUtils.rayDistanceSq(target, newBox, rot[0], rot[1], maxRange);
                        double oldDist = PlayerUtils.rayDistanceSq(target, oldBox, oldRot[0], oldRot[1], maxRange + 2.0);
                        if (oldDist >= 0.0 && newDist > oldDist && (!teleport || newDist - oldDist < 64.0)) {
                            if (!holding && canStart()) {
                                start();
                            }
                            serverPos = new Vec3(x, y, z);
                            EntityLivingBase g = ghost;
                            if (g instanceof EntityOtherPlayerMP) {
                                g.setPositionAndRotation2(x, y, z, 0.0f, 0.0f, 3, teleport);
                            }
                            flush = false;
                        }
                    }
                }
                if (holding && flush) {
                    flush();
                    return true;
                }
            }
            if (holding && !(onlyMovement.isToggled() ? !moves : passesThrough(packet))) {
                hold(packet);
                return false;
            }
            return true;
        } catch (Throwable throwable) {
            Chat.print(MathUtils.timeDigits() + " &cbacktrack e3 " + throwable.getClass().getSimpleName());
            return true;
        }
    }

    private static void hold(Packet<?> packet) {
        synchronized (LOCK) {
            int n = tick + delayTicks;
            List<Packet<?>> list = held.computeIfAbsent(n, k -> new ArrayList<Packet<?>>());
            list.add(packet);
        }
    }

    private static void process(List<Packet<?>> list) {
        if (list.isEmpty()) {
            return;
        }
        for (Packet<?> packet : list) {
            try {
                processing = packet;
                PacketUtils.reprocessing = processing;
                Hooks.onReceive(packet, mc().getNetHandler().getNetworkManager().getNetHandler());
            } catch (Throwable throwable) {
                Chat.print(MathUtils.timeDigits() + " &cbacktrack e4 " + packet.getClass().getSimpleName() + " " + throwable.getClass().getSimpleName());
            } finally {
                processing = null;
                PacketUtils.reprocessing = null;
            }
        }
    }

    private static void releaseDue() {
        ArrayList<Packet<?>> list = null;
        synchronized (LOCK) {
            if (!held.isEmpty()) {
                Map.Entry<Integer, List<Packet<?>>> entry;
                Iterator<Map.Entry<Integer, List<Packet<?>>>> it = held.entrySet().iterator();
                while (it.hasNext() && tick >= (entry = it.next()).getKey()) {
                    List<Packet<?>> packets = entry.getValue();
                    if (!packets.isEmpty()) {
                        if (list == null) {
                            list = new ArrayList<Packet<?>>();
                        }
                        list.addAll(packets);
                        packets.clear();
                    }
                    it.remove();
                }
            }
            ++tick;
        }
        if (list != null) {
            process(list);
        }
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == onlyMovement) {
            flush();
        } else if (b == showServerPosition) {
            if (!b.isToggled()) {
                ghost = null;
            } else if (holding) {
                makeGhost(target);
            }
        }
    }

    private static void renderGhost(EntityLivingBase e, float partialTicks) {
        RenderManager rm = mc().getRenderManager();
        double lx = e.lastTickPosX;
        double ly = e.lastTickPosY;
        double lz = e.lastTickPosZ;
        double x = lx + (e.posX - lx) * (double) partialTicks - rm.viewerPosX;
        double y = ly + (e.posY - ly) * (double) partialTicks - rm.viewerPosY;
        double z = lz + (e.posZ - lz) * (double) partialTicks - rm.viewerPosZ;
        AxisAlignedBB bb = e.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        AxisAlignedBB box = new AxisAlignedBB(bb.minX - e.posX + x, bb.minY - e.posY + y, bb.minZ - e.posZ + z, bb.maxX - e.posX + x, bb.maxY - e.posY + y, bb.maxZ - e.posZ + z);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        int n = Theme.defaultTheme.getRGB(0.0);
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        Render3D.filledBox(box, r, g, b, 0.37f);
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
    }

    private static EntityLivingBase findTarget() {
        Minecraft mc = mc();
        if (KillAuraModule.hasTarget() && KillAuraModule.target.living && valid((EntityLivingBase) KillAuraModule.target.entity)) {
            return (EntityLivingBase) KillAuraModule.target.entity;
        }
        if (AimAssistModule.target != null && valid(AimAssistModule.target)) {
            return AimAssistModule.target;
        }
        double half = fov.getInput() / 2.0;
        double best = 999.0;
        EntityPlayer found = null;
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p.deathTime != 0) continue;
            if (!valid(p)) continue;
            if (AntiBotModule.ignored(p)) continue;
            double d = mc.thePlayer.getDistanceSqToEntity(p);
            if (d > scanRangeSq) continue;
            double yaw = RotationUtils.absYawDifference(p, false);
            if (yaw > half) continue;
            if (PlayerUtils.isFriend(p)) continue;
            if (BotCheck.isBot(p)) continue;
            if (ignoreTeammates.isToggled() && Hypixel.sameTeam(p)) continue;
            if (best != 999.0 && !(yaw < best)) continue;
            best = yaw;
            found = p;
        }
        return found;
    }

    private static void makeGhost(EntityLivingBase e) {
        Minecraft mc = mc();
        EntityOtherPlayerMP copy = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        copy.copyLocationAndAnglesFrom(e);
        Vec3 pos = serverPos;
        copy.setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
        copy.setPositionAndRotation2(pos.xCoord, pos.yCoord, pos.zCoord, e.rotationYaw, e.rotationPitch, 0, false);
        ghost = copy;
    }

    @Override
    public void onUpdate() {
        EntityLivingBase e = findTarget();
        if (holding) {
            if (e != target || !targetValid()) {
                flush();
                return;
            }
            EntityLivingBase g = ghost;
            if (g instanceof EntityOtherPlayerMP) {
                g.prevPosX = g.lastTickPosX = g.posX;
                g.prevPosY = g.lastTickPosY = g.posY;
                g.prevPosZ = g.lastTickPosZ = g.posZ;
                g.onLivingUpdate();
            }
            releaseDue();
        } else {
            if (e != null) {
                target = e;
                updateServerPos();
            }
            ready = target != null && targetValid();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        EntityLivingBase g = ghost;
        if (g == null) {
            return;
        }
        renderGhost(g, e.partialTicks);
    }
}
