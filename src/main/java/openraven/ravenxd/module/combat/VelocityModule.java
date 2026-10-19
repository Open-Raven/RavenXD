package openraven.ravenxd.module.combat;


//creds to miau minus
import openraven.ravenxd.client.Friends;
import openraven.ravenxd.client.BotCheck;
import openraven.ravenxd.util.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MathHelper;
import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.client.Hypixel;
import openraven.ravenxd.hook.Hooks;
import openraven.ravenxd.module.movement.LongJumpModule;
import openraven.ravenxd.module.network.BacktrackModule;
import openraven.ravenxd.module.player.NoRotateModule;
import openraven.ravenxd.util.MathUtils;
import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PacketUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
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

public class VelocityModule extends Module {
    public enum Mode {
        Normal, Standard, Jump, AAC5, AACZero, Buffer, Cancel, GhostBlock, Glitch, Karhu, Matrix1, Matrix2, Matrix3, S32, Reverse, Intave, Advanced, Airpush
    }

    public static Mode current = Mode.Normal;
    public static SliderSetting mode, horizontal, vertical, chance;
    public static ButtonSetting onlyWhileTargeting, disableWhileHoldingS;
    public static SliderSetting jumpChance, jumpCooldown, ticksUntilJump, hitsUntilJump;
    public static ButtonSetting jumpOnlyOnSwing;
    public static SliderSetting bufferPackets, bufferHorizontal, bufferVertical;
    public static ButtonSetting bufferDebug;
    public static ButtonSetting cancelHorizontal, cancelVertical, cancelVerticalOnlyInAir;
    public static SliderSetting minHurtTime, maxHurtTime;
    public static ButtonSetting boostAfterReduce;
    public static SliderSetting boostFactor, boostCooldown;
    public static SliderSetting reverseStrength, reverseRange, maxAngleDifference;
    public static ButtonSetting reverseOnLook;
    public static SliderSetting duration, reduceTicks;

    private volatile boolean received;
    private volatile long receivedAt;
    private volatile long suppressUntil;
    private AntiKnockback antiKnockback;
    private int bufferAmount;
    private int limitUntilJump;
    private long boostTimer;

    public VelocityModule(int key) {
        super("Velocity", Category.combat, key);
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        horizontal = (SliderSetting) ModuleManager.setting(this, "Horizontal");
        vertical = (SliderSetting) ModuleManager.setting(this, "Vertical");
        chance = (SliderSetting) ModuleManager.setting(this, "Chance");
        onlyWhileTargeting = (ButtonSetting) ModuleManager.setting(this, "Only while targeting");
        disableWhileHoldingS = (ButtonSetting) ModuleManager.setting(this, "Disable while holding S");
        jumpChance = (SliderSetting) ModuleManager.setting(this, "Jump chance");
        jumpCooldown = (SliderSetting) ModuleManager.setting(this, "Jump cooldown");
        ticksUntilJump = (SliderSetting) ModuleManager.setting(this, "Ticks until jump");
        hitsUntilJump = (SliderSetting) ModuleManager.setting(this, "Hits until jump");
        jumpOnlyOnSwing = (ButtonSetting) ModuleManager.setting(this, "Jump only on swing");
        bufferPackets = (SliderSetting) ModuleManager.setting(this, "Buffer packets");
        bufferHorizontal = (SliderSetting) ModuleManager.setting(this, "Buffer horizontal");
        bufferVertical = (SliderSetting) ModuleManager.setting(this, "Buffer vertical");
        bufferDebug = (ButtonSetting) ModuleManager.setting(this, "Buffer debug");
        cancelHorizontal = (ButtonSetting) ModuleManager.setting(this, "Cancel horizontal");
        cancelVertical = (ButtonSetting) ModuleManager.setting(this, "Cancel vertical");
        cancelVerticalOnlyInAir = (ButtonSetting) ModuleManager.setting(this, "Cancel vertical only in air");
        minHurtTime = (SliderSetting) ModuleManager.setting(this, "Min hurt time");
        maxHurtTime = (SliderSetting) ModuleManager.setting(this, "Max hurt time");
        boostAfterReduce = (ButtonSetting) ModuleManager.setting(this, "Boost after reduce");
        boostFactor = (SliderSetting) ModuleManager.setting(this, "Boost factor");
        boostCooldown = (SliderSetting) ModuleManager.setting(this, "Boost cooldown");
        reverseStrength = (SliderSetting) ModuleManager.setting(this, "Reverse strength");
        reverseOnLook = (ButtonSetting) ModuleManager.setting(this, "Reverse on look");
        reverseRange = (SliderSetting) ModuleManager.setting(this, "Reverse range");
        maxAngleDifference = (SliderSetting) ModuleManager.setting(this, "Max angle difference");
        duration = (SliderSetting) ModuleManager.setting(this, "Duration");
        reduceTicks = (SliderSetting) ModuleManager.setting(this, "Reduce ticks");
        this.antiKnockback = new AntiKnockback(this);
        this.antiKnockback.link();
    }

    @Override
    public void guiUpdate() {
        Mode m = Mode.values()[(int) mode.getInput()];
        if (m != current) {
            this.reset();
        }
        current = m;
        this.syncAntiKnockback();
    }

    private void syncAntiKnockback() {
        boolean want = this.isEnabled() && current == Mode.Advanced;
        if (want == this.antiKnockback.enabled) {
            return;
        }
        this.antiKnockback.enabled = want;
        if (want) {
            MinecraftForge.EVENT_BUS.register(this.antiKnockback);
            this.antiKnockback.onEnable();
        } else {
            MinecraftForge.EVENT_BUS.unregister(this.antiKnockback);
            this.antiKnockback.onDisable();
        }
    }

    @Override
    public void onUpdate() {
        current = Mode.values()[(int) mode.getInput()];
        this.syncAntiKnockback();
        if (this.antiKnockback.enabled) {
            this.antiKnockback.onUpdate();
        }
    }

    @Override
    public void onEnable() {
        this.reset();
        this.syncAntiKnockback();
    }

    @Override
    public void onDisable() {
        this.reset();
        this.syncAntiKnockback();
    }

    private void reset() {
        this.received = false;
        this.suppressUntil = 0L;
        this.bufferAmount = 0;
        this.limitUntilJump = 0;
        this.boostTimer = 0L;
        this.reduceTicksCounter = -1;
        this.lastAttackTick = -1;
        this.shouldJump = false;
        this.cancellingAction = false;
        this.pendingAttack = null;
        this.useItemTicks = 0;
        this.rotateTarget = null;
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p != null) {
            p.noClip = false;
        }
    }

    private static boolean gated() {
        Minecraft mc = Minecraft.getMinecraft();
        if (onlyWhileTargeting.enabled && !PlayerUtils.targetMatches(1)) {
            return true;
        }
        if (disableWhileHoldingS.enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindBack)) {
            return true;
        }
        return chance.getInput() != 100.0 && !(Math.random() < chance.getInput() / 100.0);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (current != Mode.Normal || e.entity != mc.thePlayer || !Chat.inWorld()) {
            return;
        }
        if (mc.thePlayer.maxHurtTime <= 0 || mc.thePlayer.hurtTime != mc.thePlayer.maxHurtTime) {
            return;
        }
        if (Math.abs(System.currentTimeMillis() - PlayerUtils.lastFireballOnBlock) < 400L) {
            return;
        }
        if (gated()) {
            return;
        }
        if (horizontal.getInput() != 100.0) {
            double d = horizontal.getInput() / 100.0;
            mc.thePlayer.motionX *= d;
            mc.thePlayer.motionZ *= d;
        }
        if (vertical.getInput() != 100.0) {
            mc.thePlayer.motionY *= vertical.getInput() / 100.0;
        }
    }

    private static boolean replace(S12PacketEntityVelocity p, INetHandler handler, double h, double v) {
        S12PacketEntityVelocity n = new S12PacketEntityVelocity(p.getEntityID(), p.getMotionX() / 8000.0 * h, p.getMotionY() / 8000.0 * v, p.getMotionZ() / 8000.0 * h);
        try {
            n.processPacket((INetHandlerPlayClient) handler);
        } catch (ThreadQuickExitException ignored) {
        }
        return false;
    }

    private static void schedule(Runnable r) {
        Minecraft.getMinecraft().addScheduledTask(r);
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) {
            return true;
        }
        if (packet instanceof S32PacketConfirmTransaction) {
            if (current == Mode.S32 && this.received) {
                this.received = false;
                return false;
            }
            return true;
        }
        if (!(packet instanceof S12PacketEntityVelocity)) {
            return true;
        }
        S12PacketEntityVelocity p = (S12PacketEntityVelocity) packet;
        switch (current) {
            case Standard:
                if (gated()) return true;
                return replace(p, handler, horizontal.getInput() / 100.0, vertical.getInput() / 100.0);
            case Jump:
                return this.jumpVelocity(p);
            case Buffer:
                if (this.bufferAmount < (int) bufferPackets.getInput()) {
                    ++this.bufferAmount;
                    if (bufferDebug.isToggled()) {
                        schedule(() -> Chat.print("&7[Buffer] Cancelled packet " + this.bufferAmount + "/" + (int) bufferPackets.getInput()));
                    }
                    return false;
                }
                this.bufferAmount = 0;
                if (bufferDebug.isToggled()) {
                    schedule(() -> Chat.print("&7[Buffer] Applied reduction: H=" + bufferHorizontal.getInput() + "%, V=" + bufferVertical.getInput() + "%"));
                }
                return replace(p, handler, bufferHorizontal.getInput() / 100.0, bufferVertical.getInput() / 100.0);
            case Cancel: {
                boolean h = cancelHorizontal.isToggled(), v = cancelVertical.isToggled(), air = cancelVerticalOnlyInAir.isToggled();
                if (h && v && !air) {
                    return false;
                }
                schedule(() -> {
                    if (!h) {
                        player.motionX = p.getMotionX() / 8000.0;
                        player.motionZ = p.getMotionZ() / 8000.0;
                    }
                    if (!(v || air && !player.onGround)) {
                        player.motionY = p.getMotionY() / 8000.0;
                    }
                });
                return false;
            }
            case Glitch:
                if (!player.onGround) return true;
                this.received = true;
                return false;
            case Matrix1: {
                double f = player.onGround ? 0.86 : 0.33;
                return replace(p, handler, f, 1.0);
            }
            case Matrix3:
                schedule(() -> this.matrix3(p));
                return false;
            case S32:
                this.received = true;
                return false;
            case Intave:
                schedule(() -> {
                    player.motionX = 0.0;
                    player.motionY = 0.0;
                    player.motionZ = 0.0;
                });
                this.suppressUntil = System.currentTimeMillis() + (long) duration.getInput();
                return false;
            case Airpush:
                if (p.getMotionY() > 0) {
                    this.reduceTicksCounter = 0;
                    if (player.onGround && player.isSprinting()) {
                        this.shouldJump = true;
                    }
                }
                return true;
            case AACZero:
            case GhostBlock:
            case Karhu:
            case Matrix2:
            case Reverse:
                this.received = true;
                this.receivedAt = System.currentTimeMillis();
                return true;
            default:
                return true;
        }
    }

    private boolean jumpVelocity(S12PacketEntityVelocity p) {
        double packetDir = Math.floorMod((int) Math.toDegrees(Math.atan2(p.getMotionX(), p.getMotionZ())), 360);
        double angle = Math.floorMod((int) Math.abs(packetDir + direction()), 360);
        if (angle >= 120.0 && angle <= 240.0) {
            this.received = true;
        }
        return true;
    }

    private void matrix3(S12PacketEntityVelocity p) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        double y = p.getMotionY() / 8000.0;
        if (Math.abs(y) < 0.1) {
            return;
        }
        player.motionY = y;
        double kx = p.getMotionX() / 8000.0, kz = p.getMotionZ() / 8000.0;
        double kb = Math.hypot(kx, kz);
        if (!moving()) {
            double reduced = Math.max(kb * 0.1, Math.hypot(player.motionX, player.motionZ));
            if (kb > 0.0) {
                player.motionX = kx / kb * reduced;
                player.motionZ = kz / kb * reduced;
            }
        } else if (boostAfterReduce.isToggled() && System.currentTimeMillis() - this.boostTimer >= (long) boostCooldown.getInput()) {
            double f = boostFactor.getInput() + 1.0;
            player.motionX *= f;
            player.motionZ *= f;
            this.boostTimer = System.currentTimeMillis();
        }
    }

    @Override
    public void onPreUpdate() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        switch (current) {
            case Jump: {
                boolean hit = player.hurtTime == player.maxHurtTime && player.maxHurtTime > 0;
                if (jumpCooldown.getInput() == 0.0 || hit) {
                    ++this.limitUntilJump;
                }
                if (this.received) {
                    boolean ready = this.limitUntilJump >= (int) (jumpCooldown.getInput() == 0.0 ? ticksUntilJump.getInput() : hitsUntilJump.getInput());
                    boolean swinging = !jumpOnlyOnSwing.isToggled() || player.isSwingInProgress;
                    if (Math.random() * 100.0 <= jumpChance.getInput() && ready && swinging && player.onGround && hit && player.isSprinting()) {
                        if (!GameSettings.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindJump)) {
                            player.jump();
                        }
                        this.limitUntilJump = 0;
                    }
                    this.received = false;
                }
                break;
            }
            case AAC5:
                if (player.hurtTime > 1) {
                    player.motionX *= 0.81;
                    player.motionZ *= 0.81;
                }
                break;
            case AACZero:
                if (player.hurtTime > 0) {
                    if (!this.received || player.onGround || player.fallDistance > 2.0f) break;
                    player.motionY -= 1.0;
                    player.isAirBorne = true;
                    player.onGround = true;
                } else {
                    this.received = false;
                }
                break;
            case GhostBlock:
                if (this.received && player.hurtTime >= (int) minHurtTime.getInput() && player.hurtTime <= (int) maxHurtTime.getInput()) {
                    player.noClip = true;
                } else if (player.hurtTime == 0) {
                    this.received = false;
                    player.noClip = false;
                }
                break;
            case Glitch:
                player.noClip = this.received;
                if (player.hurtTime == 7) {
                    player.motionY = 0.4;
                }
                this.received = false;
                break;
            case Karhu:
                if (player.hurtTime > 0) {
                    player.noClip = this.received;
                } else {
                    this.received = false;
                    player.noClip = false;
                }
                break;
            case Matrix2:
                if (this.received && player.hurtTime >= 9) {
                    boolean busy = player.isBlocking() || player.isSneaking() || player.isEating() || !player.onGround;
                    double f = moving() && !busy ? 0.0 : 0.2;
                    player.motionX *= f;
                    player.motionZ *= f;
                    this.received = false;
                }
                break;
            case Reverse:
                this.reverse(player);
                break;
            case Airpush:
                this.airpush(player);
                break;
        }
    }

    // yuri "legit" as airpush (airpush port to 1.8 + jump reset)
    private volatile int reduceTicksCounter = -1;
    private volatile boolean shouldJump;
    private int lastAttackTick = -1;
    private boolean cancellingAction;
    private Entity pendingAttack;
    private int useItemTicks;
    private Entity rotateTarget;
    private int rotateTick;

    private void airpush(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();
        this.useItemTicks = player.isUsingItem() ? 0 : this.useItemTicks + 1;
        if (this.pendingAttack != null && this.useItemTicks >= 1) {
            Entity target = this.pendingAttack;
            this.pendingAttack = null;
            if (mc.currentScreen == null && validTarget(target) && player.getDistanceToEntity(target) <= 3.0f) {
                this.rotateTo(target);
                this.attack(target);
            }
        } else if (mc.currentScreen == null && this.reduceTicksCounter >= 0 && this.reduceTicksCounter < (int) reduceTicks.getInput()) {
            ++this.reduceTicksCounter;
            if (this.lastAttackTick != player.ticksExisted) {
                EntityLivingBase target = furthestTarget(player, 3.0);
                if (target != null) {
                    this.rotateTo(target);
                    if (player.isUsingItem()) {
                        this.pendingAttack = target;
                    } else {
                        this.attack(target);
                    }
                }
            }
        }
        if (this.shouldJump) {
            this.shouldJump = false;
            if (player.onGround && player.isSprinting() && !GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
                player.jump();
            }
        }
    }

    private void rotateTo(Entity target) {
        this.rotateTarget = target;
        this.rotateTick = Minecraft.getMinecraft().thePlayer.ticksExisted;
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (current != Mode.Airpush || this.rotateTarget == null || this.rotateTarget.isDead || player.ticksExisted - this.rotateTick > 1) {
            return null;
        }
        return RotationUtils.toEntityFixed(this.rotateTarget, yaw, pitch);
    }

    private void attack(Entity target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (KillAuraModule.target != null || ModuleManager.enabled("Scaffold") != null || ModuleManager.enabled("BedAura") != null) {
            return;
        }
        this.cancellingAction = true;
        PacketUtils.send(new C0APacketAnimation());
        mc.playerController.attackEntity(mc.thePlayer, target);
        this.cancellingAction = false;
    }

    private static EntityLivingBase furthestTarget(EntityPlayerSP player, double range) {
        EntityLivingBase best = null;
        double bestDistance = 0.0;
        for (Entity e : Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (!validTarget(e)) continue;
            double d = player.getDistanceToEntity(e);
            if (d <= range && (best == null || d > bestDistance)) {
                best = (EntityLivingBase) e;
                bestDistance = d;
            }
        }
        return best;
    }

    private static boolean validTarget(Entity e) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (e == player || e.isDead || !(e instanceof EntityLivingBase) || e instanceof EntityArmorStand) {
            return false;
        }
        EntityLivingBase living = (EntityLivingBase) e;
        if (living.getHealth() <= 0.0f) {
            return false;
        }
        if (e instanceof EntityPlayer && (Friends.isFriend(e.getName()) || BotCheck.isBot(e) || Hypixel.sameTeam(living))) {
            return false;
        }
        return player.canEntityBeSeen(e);
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (current != Mode.Airpush) {
            return true;
        }
        if (packet instanceof C02PacketUseEntity && ((C02PacketUseEntity) packet).getAction() == C02PacketUseEntity.Action.ATTACK) {
            this.lastAttackTick = Minecraft.getMinecraft().thePlayer.ticksExisted;
        }
        return !(this.cancellingAction && packet instanceof C07PacketPlayerDigging);
    }

    private void reverse(EntityPlayerSP player) {
        if (!this.received) {
            return;
        }
        EntityPlayer nearby = null;
        double best = reverseRange.getInput();
        for (EntityPlayer e : Minecraft.getMinecraft().theWorld.playerEntities) {
            if (e == player) continue;
            double d = player.getDistanceToEntity(e);
            if (d <= best) {
                best = d;
                nearby = e;
            }
        }
        if (nearby == null) {
            return;
        }
        if (player.onGround) {
            if (System.currentTimeMillis() - this.receivedAt >= 80L) {
                this.received = false;
            }
            return;
        }
        if (reverseOnLook.isToggled() && !looking(player, nearby, maxAngleDifference.getInput())) {
            return;
        }
        double s = reverseStrength.getInput();
        double speed = Math.hypot(player.motionX, player.motionZ);
        if (speed > 0.0) {
            double yaw = Math.toRadians(Math.atan2(player.motionZ, player.motionX) * 180.0 / Math.PI - 90.0);
            player.motionX = -Math.sin(yaw) * speed * s;
            player.motionZ = Math.cos(yaw) * speed * s;
        }
    }

    @Override
    public void onPostPlayerInput() {
        if (current == Mode.Intave && System.currentTimeMillis() < this.suppressUntil) {
            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            player.movementInput.moveForward = 0.0f;
            player.movementInput.moveStrafe = 0.0f;
        }
    }

    private static boolean moving() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        return p.moveForward != 0.0f || p.moveStrafing != 0.0f;
    }

    private static float normalize(float a) {
        return (a % 360.0f + 360.0f) % 360.0f;
    }

    private static boolean looking(EntityPlayer player, EntityPlayer target, double maxAngle) {
        float yaw = normalize((float) Math.toDegrees(Math.atan2(target.posZ - player.posZ, target.posX - player.posX)) - 90.0f);
        float diff = Math.abs(yaw - normalize(player.rotationYaw));
        return Math.min(diff, 360.0f - diff) <= maxAngle;
    }

    private static double direction() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        float yaw = p.rotationYaw;
        if (p.moveForward != 0.0f && p.moveStrafing == 0.0f) {
            yaw += p.moveForward > 0.0f ? 0.0f : 180.0f;
        } else if (p.moveForward != 0.0f) {
            yaw += p.moveForward > 0.0f ? (p.moveStrafing > 0.0f ? -45.0f : 45.0f) : (p.moveStrafing > 0.0f ? 45.0f : -45.0f) + 180.0f;
        } else if (p.moveStrafing != 0.0f) {
            yaw += p.moveStrafing > 0.0f ? -90.0f : 90.0f;
        }
        return Math.floorMod((int) yaw, 360);
    }

    // shove antiknockback in here
    public static class AntiKnockback extends Module {
        public static AntiKnockback instance;
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

        private final Module owner;

        public AntiKnockback(Module owner) {
            super("Advanced", Category.combat, 0);
            this.owner = owner;
            instance = this;
        }

        public static Module running() {
            return instance != null && instance.enabled ? instance : null;
        }

        private static Minecraft mc() {
            return Minecraft.getMinecraft();
        }

        public void link() {
            horizontal = (SliderSetting) ModuleManager.setting(this.owner, "Horizontal");
            vertical = (SliderSetting) ModuleManager.setting(this.owner, "Vertical");
            disableInLobby = (ButtonSetting) ModuleManager.setting(this.owner, "Disable in lobby");
            onlyWhileMoving = (ButtonSetting) ModuleManager.setting(this.owner, "Only while moving");
            cancelBurning = (ButtonSetting) ModuleManager.setting(this.owner, "Cancel burning");
            cancelExplosion = (ButtonSetting) ModuleManager.setting(this.owner, "Cancel explosion");
            cancelGround = (ButtonSetting) ModuleManager.setting(this.owner, "Cancel ground");
            cancelOffGround = (ButtonSetting) ModuleManager.setting(this.owner, "Cancel off ground");
            cancelWhileFalling = (ButtonSetting) ModuleManager.setting(this.owner, "Cancel while falling");
            cancelInterval = (SliderSetting) ModuleManager.setting(this.owner, "Cancel interval");
            damageBoost = (SliderSetting) ModuleManager.setting(this.owner, "Damage boost");
            boostWithLmb = (ButtonSetting) ModuleManager.setting(this.owner, "Boost with LMB");
            delay = (SliderSetting) ModuleManager.setting(this.owner, "Delay");
            delayIfLanding = (ButtonSetting) ModuleManager.setting(this.owner, "Delay if landing");
            delayOnlyMidair = (ButtonSetting) ModuleManager.setting(this.owner, "Delay only midair");
            delayOnlyTargeting = (ButtonSetting) ModuleManager.setting(this.owner, "Delay only targeting");
            delaySimultaneously = (ButtonSetting) ModuleManager.setting(this.owner, "Delay simultaneously");
            delayUntilGround = (ButtonSetting) ModuleManager.setting(this.owner, "Delay until ground");
            disableAttackWhileDelay = (ButtonSetting) ModuleManager.setting(this.owner, "Disable attack while delay");
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
}
