package xyz.daikirai.module.combat;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Irc;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.player.AutoBlockinModule;
import xyz.daikirai.module.player.AutoLadderModule;
import xyz.daikirai.module.player.BedAuraModule;
import xyz.daikirai.module.player.ScaffoldModule;
import xyz.daikirai.module.render.DamageTagsModule;
import xyz.daikirai.module.render.TargetHudModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.module.world.AntiBotModule;
import xyz.daikirai.util.Animation;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ClickUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.KeyUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.util.Strings;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class KillAuraModule extends Module {
    public static KillAuraModule instance;
    public static SliderSetting aps, attackOnDamage, autoBlockSetting, autoBlockAps, autoBlockLag, fov, attackRangeSetting, swingRangeSetting,
            blockRangeSetting, rotationSetting, rotationSmoothing, sortSetting, switchDelay, targets;
    public static ButtonSetting aimInvis, attackFireballs, attackMobs, attackPlayers, disableInInventory, disableWhileBlocking, disableWhileMining,
            disableSprintMidair, hitThroughBlocks, ignoreTeammates, manualBlock, prioritizeEnemies, requireMouseDown, silentSwingWhileBlocking,
            weaponOnly;

    public enum AutoBlock {
        Manual(false, false, false), Vanilla(false, false, false), Damage(false, false, false), Fake(false, false, false),
        Interact_A(true, true, true), Interact_B(true, true, true), Interact_C(true, true, true), Interact_D(true, true, true),
        Swap(false, true, true), Dual(true, true, false);

        public final boolean interacts;
        public final boolean packet;
        public final boolean lags;

        AutoBlock(boolean interacts, boolean packet, boolean lags) {
            this.interacts = interacts;
            this.packet = packet;
            this.lags = lags;
        }
    }

    public enum Rotation {
        Silent, Lock_view, None
    }

    public enum Sort {
        Distance, Health, Hurt_time, Yaw
    }

    public static class Target {
        public final Entity entity;
        public final boolean living;

        Target(Entity entity) {
            this.entity = entity;
            this.living = entity instanceof EntityLivingBase;
        }

        public float maxHealth() {
            return this.living ? ((EntityLivingBase) this.entity).getMaxHealth() : 20.0f;
        }

        public float health() {
            return this.living ? Strings.health((EntityLivingBase) this.entity) : 20.0f;
        }

        public boolean exists() {
            return this.entity != null && Minecraft.getMinecraft().theWorld.getEntityByID(this.entity.getEntityId()) != null;
        }
    }

    private static class Timer {
        long last = 0L;

        void reset() {
            this.last = System.currentTimeMillis();
        }

        boolean passed(long ms) {
            return System.currentTimeMillis() - this.last >= ms;
        }
    }

    public static AutoBlock autoBlock = AutoBlock.Manual;
    public static Rotation rotation = Rotation.Silent;
    public static Sort sort = Sort.Distance;
    public static Target target;
    private static Target previousTarget;
    public static boolean fakeBlocking;
    public static boolean serverBlocking;
    public static boolean resyncSlot;
    public static boolean slowedThisTick;
    public static boolean disabled = true;
    public static int blockState = -1;
    public static double attackRange, swingRange, blockRange, scanRangeSq;
    public static double currentAps;
    private static final Timer attackTimer = new Timer();
    private static long attackDelay;
    private static boolean attackQueued;
    private static int forcedAttacks;
    private static int noAttackTicks;
    private static int badSwapTicks;
    private static boolean inSwingRange;
    private static float[] rotations;
    private static float[] interactRotations;
    private static boolean skipSmoothing;
    private static boolean returning;
    private static float lastClientYaw, lastClientPitch;
    private static Vec3 aimPoint;
    private static final HashMap<Integer, Vec3> aimPoints = new HashMap<Integer, Vec3>();
    private static final HashMap<UUID, Long> switchTimes = new HashMap<UUID, Long>();
    private static final List<Packet<?>> held = Collections.synchronizedList(new ArrayList<Packet<?>>());
    private static boolean lagging;
    private static int lagAccumulator;
    private static double dualAccumulator;
    private static int dualSwaps;
    private static boolean attackedThisTick;
    private static boolean attacking;
    private static boolean interacting;
    private static boolean releasing;
    private static boolean droppedDig;
    private static boolean passedRightClick;
    private static long damageBlockAt;
    private static int enabledTick;
    private static int unused107101;
    private static boolean unused107108;
    public static boolean bedAuraRotated;
    private static long hudSince = -1L;
    private static Animation hudFade;
    private static String hudName = "";
    private static float hudHealth;
    private static double hudRatio;
    private static final long HUD_HOLD = 250L, HUD_FADE = 400L;

    public KillAuraModule(int key) {
        super("KillAura", Category.combat, key);
        instance = this;
        this.queueAfterDisable = true;
        this.lite = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        aps = (SliderSetting) ModuleManager.setting(this, "APS");
        attackOnDamage = (SliderSetting) ModuleManager.setting(this, "Attack on damage");
        autoBlockSetting = (SliderSetting) ModuleManager.setting(this, "AutoBlock");
        autoBlockAps = (SliderSetting) ModuleManager.setting(this, "AutoBlock APS");
        autoBlockLag = (SliderSetting) ModuleManager.setting(this, "AutoBlock lag");
        fov = (SliderSetting) ModuleManager.setting(this, "FOV");
        attackRangeSetting = (SliderSetting) ModuleManager.setting(this, "Range (attack)");
        swingRangeSetting = (SliderSetting) ModuleManager.setting(this, "Range (swing)");
        blockRangeSetting = (SliderSetting) ModuleManager.setting(this, "Range (block)");
        rotationSetting = (SliderSetting) ModuleManager.setting(this, "Rotation mode");
        rotationSmoothing = (SliderSetting) ModuleManager.setting(this, "Rotation smoothing");
        sortSetting = (SliderSetting) ModuleManager.setting(this, "Sort mode");
        switchDelay = (SliderSetting) ModuleManager.setting(this, "Switch delay");
        targets = (SliderSetting) ModuleManager.setting(this, "Targets");
        aimInvis = (ButtonSetting) ModuleManager.setting(this, "Aim invis");
        attackFireballs = (ButtonSetting) ModuleManager.setting(this, "Attack fireballs");
        attackMobs = (ButtonSetting) ModuleManager.setting(this, "Attack mobs");
        attackPlayers = (ButtonSetting) ModuleManager.setting(this, "Attack players");
        disableInInventory = (ButtonSetting) ModuleManager.setting(this, "Disable in inventory");
        disableWhileBlocking = (ButtonSetting) ModuleManager.setting(this, "Disable while blocking");
        disableWhileMining = (ButtonSetting) ModuleManager.setting(this, "Disable while mining");
        disableSprintMidair = (ButtonSetting) ModuleManager.setting(this, "Disable sprint midair");
        hitThroughBlocks = (ButtonSetting) ModuleManager.setting(this, "Hit through blocks");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        manualBlock = (ButtonSetting) ModuleManager.setting(this, "Manual block");
        prioritizeEnemies = (ButtonSetting) ModuleManager.setting(this, "Prioritize enemies");
        requireMouseDown = (ButtonSetting) ModuleManager.setting(this, "Require mouse down");
        silentSwingWhileBlocking = (ButtonSetting) ModuleManager.setting(this, "Silent swing while blocking");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
        guiUpdate();
    }

    @Override
    public Entity getTarget() {
        return target != null ? target.entity : null;
    }

    @Override
    public boolean isFakeBlocking() {
        return fakeBlocking;
    }

    @Override
    public boolean consumeSlotResync() {
        if (resyncSlot) {
            resyncSlot = false;
            return true;
        }
        return false;
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (rotation == Rotation.None) {
            return null;
        }
        return takeRotations();
    }

    public static boolean hasTarget() {
        return exists(target);
    }

    private static boolean exists(Target t) {
        return t != null && t.exists();
    }

    private static boolean smoothReturn() {
        Minecraft mc = mc();
        if (!rotationSmoothing.isAtMin() && (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS)
                && !ScaffoldModule.active && !AutoBlockinModule.active && AutoLadderModule.target == null && BedAuraModule.target == null) {
            ItemStack itemStack = mc.thePlayer.getHeldItem();
            Item item = itemStack == null ? null : itemStack.getItem();
            if (item != null) {
                if (item == Items.fishing_rod || item == Items.bow) {
                    return false;
                }
                if (ItemUtils.isThrowable(itemStack)) {
                    return false;
                }
            }
            float lastYaw = RotationManager.lastYaw;
            float lastPitch = RotationManager.lastPitch;
            float yaw = mc.thePlayer.rotationYaw;
            float pitch = mc.thePlayer.rotationPitch;
            float dYaw = Math.abs(MathHelper.wrapAngleTo180_float(yaw - lastClientYaw));
            float dPitch = Math.abs(pitch - lastClientPitch);
            lastClientYaw = yaw;
            lastClientPitch = pitch;
            if (dYaw > 20.0f || dPitch > 8.0f) {
                return false;
            }
            rotations = new float[]{yaw, pitch};
            smooth(rotations, lastYaw, lastPitch, 1.5f);
            boolean still = Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw)) > 3.0f
                    || Math.abs(rotations[1] - mc.thePlayer.rotationPitch) > 3.0f;
            rotations = RotationUtils.fixSensitivity(rotations[0], rotations[1], lastYaw, lastPitch);
            return still;
        }
        return false;
    }

    public static void onRenderTick() {
        Minecraft mc = mc();
        if (instance != null && instance.isEnabled() && currentAps > 0.0 && (attackTimer.passed(attackDelay) || previousTarget == null && target != null)) {
            boolean hold = false;
            if (target != null) {
                if (disableSprintMidair.isToggled() && !mc.thePlayer.onGround && mc.thePlayer.isSprinting()) {
                    hold = true;
                } else if (autoBlock == AutoBlock.Dual && blockAllowed()) {
                    hold = true;
                }
            }
            if (!hold) {
                attackTimer.reset();
                setDelay(false);
                if (target != null) {
                    attackQueued = true;
                }
            }
        }
        Module targetHud = ModuleManager.enabled("TargetHUD");
        if (targetHud != null && TargetHudModule.mode != TargetHudModule.Mode.Disabled && hudSince != -1L) {
            double ratio;
            long age = System.currentTimeMillis() - hudSince;
            if (age >= HUD_HOLD + HUD_FADE) {
                hudFade = null;
                hudSince = -1L;
                return;
            }
            if (age >= HUD_HOLD && hudFade == null) {
                hudFade = new Animation(400L);
                hudFade.start();
            }
            if (!Overlay.canRender()) {
                return;
            }
            if (target == null) {
                ratio = hudRatio;
            } else {
                hudName = target.entity.getDisplayName().getUnformattedText();
                hudHealth = target.exists() ? target.health() : 0.0f;
                hudRatio = ratio = (double) Math.min(hudHealth / target.maxHealth(), 1.0f);
            }
            TargetHudModule.render(hudFade, hudName, hudHealth, ratio);
        }
    }

    private static void release(boolean all) {
        if (held.isEmpty()) {
            return;
        }
        synchronized (held) {
            for (Packet<?> packet : held) {
                if (packet instanceof C02PacketUseEntity) {
                    if (all) continue;
                    if (ModuleManager.enabled("Hit Select") != null) {
                        HitSelectModule.onUseEntity((C02PacketUseEntity) packet);
                    }
                } else if (all) {
                    if (packet instanceof C0APacketAnimation) continue;
                    if (packet instanceof C07PacketPlayerDigging && ((C07PacketPlayerDigging) packet).getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                        droppedDig = true;
                        continue;
                    }
                }
                PacketUtils.sendThroughLag(packet);
            }
        }
        held.clear();
    }

    private static void setDelay(boolean now) {
        attackDelay = now ? 0L : (long) (1000.0 / currentAps + (double) MathUtils.randomInt(-4, 4));
    }

    @Override
    public void onPostPlayerInput() {
        if (autoBlock.packet && serverBlocking) {
            boolean slow = true;
            if (autoBlock == AutoBlock.Dual && blockState != -1 && dualAccumulator + currentAps >= 20.0) {
                slow = false;
            }
            if (slow) {
                xyz.daikirai.module.movement.NoSlowModule.slowdown();
            }
        }
        if (slowedThisTick) {
            slowedThisTick = false;
        }
    }

    private static void smooth(float[] rot, float lastYaw, float lastPitch, float extra) {
        float maxYaw = 7.0f;
        float maxPitch = 3.0f;
        float keep = (float) (1.0 - Math.min(9.9, rotationSmoothing.getInput() + (double) extra) / 10.0);
        float dYaw = MathHelper.wrapAngleTo180_float(rot[0] - lastYaw);
        float aYaw = Math.abs(dYaw);
        float dPitch = rot[1] - lastPitch;
        float aPitch = Math.abs(dPitch);
        if (aYaw > maxYaw) {
            dYaw = Math.signum(dYaw) * ((aYaw - maxYaw) * keep + maxYaw);
        }
        if (aPitch > maxPitch) {
            dPitch = Math.signum(dPitch) * ((aPitch - maxPitch) * keep + maxPitch);
        }
        rot[0] = lastYaw + dYaw;
        rot[1] = lastPitch + dPitch;
    }

    public static void setBlocking(boolean block, boolean packet) {
        Minecraft mc = mc();
        if (packet) {
            ItemStack itemStack;
            if ((block || autoBlock != AutoBlock.Interact_C && autoBlock != AutoBlock.Interact_D)
                    && ((itemStack = mc.thePlayer.inventory.getStackInSlot(PacketUtils.serverSlot)) == null || !(itemStack.getItem() instanceof ItemSword))) {
                return;
            }
        } else if (block && !PlayerUtils.holdingSword()) {
            return;
        }
        if (packet) {
            if (block) {
                PacketUtils.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            } else {
                releasing = true;
                PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                releasing = false;
            }
            serverBlocking = fakeBlocking = block;
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), block);
        }
    }

    private static void releaseFlow(int state) {
        Minecraft mc = mc();
        boolean swap = false;
        boolean proceed = true;
        boolean unblock = false;
        if (state == 0 && autoBlock.lags) {
            if (autoBlock == AutoBlock.Interact_B || autoBlock == AutoBlock.Interact_C) {
                swap = true;
            } else {
                proceed = false;
            }
        }
        if ((autoBlock == AutoBlock.Interact_B || autoBlock == AutoBlock.Interact_C) && !PlayerUtils.holdingSword()) {
            if (!swap) {
                unblock = true;
                swap = true;
            }
        }
        boolean inScreen = disableInInventory.isToggled() && mc.currentScreen != null;
        if (swap) {
            if (inScreen) {
                PlayerUtils.setSlot(PacketUtils.serverSlot, false);
            } else {
                PlayerUtils.sendSlot(mc.thePlayer.inventory.currentItem);
                if (unblock) {
                    setBlocking(false, true);
                }
            }
        } else if (proceed) {
            if (autoBlock == AutoBlock.Interact_B || autoBlock == AutoBlock.Interact_C) {
                if (inScreen) {
                    setBlocking(false, true);
                } else {
                    PlayerUtils.sendSlot(freeSlot());
                    if (autoBlock == AutoBlock.Interact_C) {
                        setBlocking(false, true);
                    }
                    resyncSlot = true;
                }
            } else {
                if (autoBlock == AutoBlock.Interact_D) {
                    swapAway();
                } else if (autoBlock == AutoBlock.Dual) {
                    int slot = PlayerUtils.currentSlot();
                    if (slot == PacketUtils.serverSlot) {
                        PlayerUtils.setSlot((slot + 1) % 9, true);
                        PlayerUtils.setSlot(slot, false);
                    } else {
                        PlayerUtils.sendSlot(slot);
                    }
                }
                setBlocking(false, true);
            }
        }
        slowedThisTick = true;
    }

    private static boolean dropSetSlot(S2FPacketSetSlot p) {
        Minecraft mc = mc();
        return target != null && Hypixel.skywars(2) && !Overlay.screenOpen() && mc.thePlayer.inventory.currentItem == p.func_149173_d() - 36 && PlayerUtils.holdingSword();
    }

    public static void forceAttacks(int n) {
        if (forcedAttacks < n) {
            forcedAttacks = n;
        }
    }

    @Override
    public void afterDisable() {
        if (blockState != -1) {
            releaseFlow(blockState);
        }
        blockState = -1;
        slowedThisTick = false;
        fakeBlocking = false;
        serverBlocking = false;
        disabled = true;
    }

    private static double hurtTime(EntityLivingBase e) {
        return e.hurtTime;
    }

    @Override
    public void onEnable() {
        disabled = false;
        hudFade = null;
        hudSince = -1L;
        enabledTick = mc().thePlayer.ticksExisted;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            setTarget(null);
            held.clear();
        }
    }

    public static boolean stopBlocking(boolean blocking) {
        if (blockState != -1) {
            releaseFlow(blockState);
            blockState = -1;
            serverBlocking = fakeBlocking = blocking;
            return true;
        }
        return false;
    }

    @Override
    public void onPostMotion() {
        Minecraft mc = mc();
        if (target != null) {
            if (inSwingRange && rotation == Rotation.Lock_view) {
                RotationUtils.setRotation(RotationManager.serverYaw, RotationManager.serverPitch);
            }
            if (blockAllowed() && !autoBlock.packet) {
                switch (autoBlock) {
                    case Vanilla: {
                        setBlocking(true, false);
                        break;
                    }
                    case Damage: {
                        if (System.currentTimeMillis() - damageBlockAt < 800L) {
                            setBlocking(true, false);
                            break;
                        }
                        if (rightDown()) break;
                        setBlocking(false, false);
                        break;
                    }
                    case Fake: {
                        fakeBlocking = true;
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        if (autoBlock.packet && !passedRightClick && target == null && mc.currentScreen == null && mc.objectMouseOver != null && PlayerUtils.holdingSword() && rightDown()) {
            boolean pass = mc.objectMouseOver.entityHit != null && BotCheck.isBot(mc.objectMouseOver.entityHit);
            BlockPos pos;
            if (!pass && (pos = mc.objectMouseOver.getBlockPos()) != null && BlockUtils.isInteractable(BlockUtils.getBlock(pos))) {
                pass = true;
            }
            if (pass) {
                int key = mc.gameSettings.keyBindUseItem.getKeyCode();
                KeyBinding.setKeyBindState(key, true);
                KeyBinding.onTick(key);
                KeyBinding.setKeyBindState(key, false);
                passedRightClick = true;
            }
        }
        attackedThisTick = false;
    }

    private static void updateRotations() {
        float lastYaw = RotationManager.lastYaw;
        float lastPitch = RotationManager.lastPitch;
        if (interactRotations == null) {
            rotations = aim();
            if (!rotationSmoothing.isAtMin()) {
                if (skipSmoothing) {
                    skipSmoothing = false;
                } else {
                    smooth(rotations, lastYaw, lastPitch, 0.0f);
                }
            }
        } else {
            rotations = interactRotations;
            interactRotations = null;
        }
        rotations = RotationUtils.fixSensitivity(rotations[0], rotations[1], lastYaw, lastPitch);
    }

    private static double yawDifference(EntityLivingBase e) {
        return RotationUtils.absYawDifference(e, false);
    }

    public static void setTarget(Entity entity) {
        if (entity == null) {
            switchTimes.clear();
            if (target != null) {
                if (autoBlock.packet && (!manualBlock.isToggled() || blockState != -1)) {
                    releaseFlow(blockState);
                }
                fakeBlocking = false;
            } else if (autoBlock != AutoBlock.Manual && !Mouse.isButtonDown(1)) {
                setBlocking(false, false);
            }
            serverBlocking = false;
            inSwingRange = false;
            if (blockState != -1) {
                blockState = -1;
            }
            if (!hitThroughBlocks.isToggled()) {
                aimPoint = null;
                aimPoints.clear();
            }
        } else if (!hitThroughBlocks.isToggled()) {
            aimPoint = aimPoints.get(entity.getEntityId());
        }
        previousTarget = target;
        if (target != null && target.entity == entity) {
            return;
        }
        target = entity != null ? new Target(entity) : null;
    }

    @Override
    public String getInfo() {
        if (rotation == Rotation.None) {
            return fov.getDisplay(false);
        }
        return rotationSetting.getDisplay(false);
    }

    @Override
    public void onDisable() {
        if (autoBlock != AutoBlock.Manual && !rightDown() && mc().thePlayer.isUsingItem()) {
            setBlocking(false, false);
        }
        returning = false;
        attackedThisTick = false;
        droppedDig = false;
        skipSmoothing = false;
        unused107108 = false;
        bedAuraRotated = false;
        passedRightClick = false;
        inSwingRange = false;
        attackQueued = false;
        previousTarget = null;
        target = null;
        interactRotations = null;
        rotations = null;
        aimPoint = null;
        aimPoints.clear();
        switchTimes.clear();
        dualSwaps = 0;
        forcedAttacks = 0;
        unused107101 = 0;
        badSwapTicks = 0;
        noAttackTicks = 0;
        dualAccumulator = 0;
        damageBlockAt = 0L;
        attackDelay = 0L;
        release(true);
    }

    private static void updateAps() {
        double d = blockAllowed() ? autoBlockAps.getInput() : aps.getInput();
        if (target != null && autoBlock == AutoBlock.Dual) {
            if (blockState == -1) {
                dualAccumulator = 20.0 - d;
            }
            if ((dualAccumulator += d) >= 20.0) {
                dualAccumulator -= 20.0;
                attackQueued = true;
            }
        }
        if (currentAps != d) {
            setDelay(true);
            currentAps = d;
        }
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        Minecraft mc = mc();
        if (packet instanceof C09PacketHeldItemChange) {
            if (autoBlock.packet && !PlayerUtils.sendingSlot) {
                C09PacketHeldItemChange p = (C09PacketHeldItemChange) packet;
                if (ItemUtils.isSword(mc.thePlayer.inventory.getStackInSlot(p.getSlotId()))) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    badSwapTicks = 1;
                } else if (rightDown() && !ScaffoldModule.active) {
                    ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(PacketUtils.previousServerSlot);
                    if (itemStack != null && itemStack.getItem() instanceof ItemSword) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                    }
                }
            }
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            if (badSwapTicks > 0) {
                PacketUtils.lastUse = 3;
                return false;
            }
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
            if (ItemUtils.isSword(p.getStack())) {
                if (p.getPlacedBlockDirection() == 255) {
                    if (target != null && autoBlock == AutoBlock.Manual && target.exists()) {
                        if (PacketUtils.sentAttack) {
                            return false;
                        }
                        Entity entity = target.entity;
                        boolean ok = true;
                        if (AntiKnockbackModule.disableAttackWhileDelay.isToggled() && AntiKnockbackModule.delayUntil.get() != 0L) {
                            ok = false;
                        } else if (HitSelectModule.holdForKillAura(entity)) {
                            ok = false;
                        } else if (!hitThroughBlocks.isToggled() && (aimPoint = RotationUtils.visiblePoint(entity)) == null) {
                            ok = false;
                        }
                        if (ok) {
                            float[] rot = aim();
                            MovingObjectPosition hit = BotCheck.rayTrace(entity, attackRange, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, rot[0], rot[1]);
                            if (hit != null) {
                                attackedThisTick = true;
                                attacking = true;
                                PlayerUtils.attack(entity, false);
                                attacking = false;
                                Irc.onKillAuraHit();
                                Vec3 vec3 = new Vec3(hit.hitVec.xCoord - entity.posX, hit.hitVec.yCoord - entity.posY, hit.hitVec.zCoord - entity.posZ);
                                interacting = true;
                                PacketUtils.send(new C02PacketUseEntity(entity, vec3));
                                PacketUtils.send(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT));
                                interacting = false;
                                interactRotations = rot;
                            } else if (PlayerUtils.rayHits(entity, rot[0], rot[1], swingRange)) {
                                PlayerUtils.swing(false, false);
                                ClickUtils.onAttackClick();
                            }
                        }
                    }
                } else {
                    Block block = BlockUtils.getBlock(p.getPosition());
                    if (!BlockUtils.isInteractable(block)) {
                        return false;
                    }
                }
            }
        } else if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging p = (C07PacketPlayerDigging) packet;
            if (p.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                if (!releasing && autoBlock.packet && PlayerUtils.holdingSword() && mc.thePlayer.ticksExisted != enabledTick) {
                    PacketUtils.lastUse = 0;
                    return false;
                }
            } else if (droppedDig && (p.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK || p.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                droppedDig = false;
                return false;
            }
        } else if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity p = (C02PacketUseEntity) packet;
            if (target == null) {
                if (autoBlock.packet && !interacting && blockState == -1 && badSwapTicks > 0 && p.getAction() != C02PacketUseEntity.Action.ATTACK) {
                    return false;
                }
            } else if (autoBlock.packet) {
                if (p.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    if (!attacking) {
                        return false;
                    }
                } else if (!autoBlock.interacts) {
                    badSwapTicks = 1;
                    return false;
                }
            } else if (!(autoBlock != AutoBlock.Manual || interacting || p.getAction() != C02PacketUseEntity.Action.INTERACT_AT && p.getAction() != C02PacketUseEntity.Action.INTERACT)) {
                return false;
            }
        }
        if (!(!autoBlock.lags || mc.isSingleplayer() || !lagging && held.isEmpty() || blockState != -1 && packet instanceof C08PacketPlayerBlockPlacement && packet != PacketUtils.sending)) {
            switch (blockState) {
                case -1: {
                    if (packet instanceof C03PacketPlayer) {
                        release(true);
                        lagging = false;
                    } else if (!held.isEmpty()) {
                        held.add(packet);
                        return false;
                    }
                    return true;
                }
                case 1: {
                    held.add(packet);
                    if (packet instanceof C03PacketPlayer) {
                        release(false);
                    }
                    return false;
                }
                default:
                    break;
            }
            held.add(packet);
            if (!lagging && packet instanceof C03PacketPlayer) {
                release(true);
            }
            return false;
        }
        return true;
    }

    private static void swapAway() {
        PlayerUtils.sendSlot(freeSlot());
        PlayerUtils.sendSlot(PlayerUtils.currentSlot());
    }

    private static float[] aim() {
        if (aimPoint != null) {
            return RotationUtils.toVec(aimPoint);
        }
        return RotationUtils.toEntityBox(target.entity);
    }

    @Override
    public boolean allowHeldItemPacket() {
        return !(autoBlock.packet && PacketUtils.slotChangesPost > 1 && PacketUtils.slotPost == PacketUtils.previousSlotPost);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = mc();
        if (autoBlock.lags && blockState != -1 && (mc.theWorld == null || mc.currentScreen instanceof GuiGameOver)) {
            blockState = -1;
            held.clear();
        }
    }

    private static void findTarget() {
        Minecraft mc = mc();
        if (disableInInventory.isToggled() && (PacketUtils.sentCloseWindow || Overlay.screenOpen())) {
            setTarget(null);
            fakeBlocking = false;
            return;
        }
        if (weaponOnly.isToggled() && !PlayerUtils.holdingWeapon()) {
            setTarget(null);
            fakeBlocking = false;
            return;
        }
        if (requireMouseDown.isToggled() && (!Mouse.isButtonDown(0) || Overlay.ownScreen() || mc.currentScreen instanceof GuiChat)) {
            setTarget(null);
            return;
        }
        if (disableWhileMining.isToggled() && Mouse.isButtonDown(0) && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            setTarget(null);
            return;
        }
        if (AutoBlockinModule.active || AutoLadderModule.target != null) {
            setTarget(null);
            return;
        }
        if (ScaffoldModule.active && ScaffoldModule.autoSwap.isToggled() && PacketUtils.slotPost != mc.thePlayer.inventory.currentItem) {
            setTarget(null);
            return;
        }
        boolean enemiesOnly = false;
        boolean fireballs = attackFireballs.isToggled() && Math.abs(System.currentTimeMillis() - PlayerUtils.lastFireball) > 700L;
        double nearest = -1.0;
        double nearestFireball = -1.0;
        EntityLivingBase nearestEntity = null;
        List<EntityLivingBase> list = new ArrayList<EntityLivingBase>();
        HashSet<UUID> seen = new HashSet<UUID>();
        Entity fireball = null;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                double d;
                if (!fireballs || !(entity instanceof EntityLargeFireball) || (d = mc.thePlayer.getDistanceSqToEntity(entity)) > scanRangeSq) continue;
                if (fov.isNotDefault() && !PlayerUtils.inFov((float) fov.getInput(), entity)) continue;
                if (!PlayerUtils.rayHits(entity, swingRange)) continue;
                if (!RotationUtils.canSee(entity)) continue;
                if (nearestFireball != -1.0 && !(nearestFireball > d)) continue;
                nearestFireball = d;
                fireball = entity;
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living == mc.thePlayer || living.deathTime != 0) continue;
            if (!aimInvis.isToggled() && PlayerUtils.isInvisibleUnarmoured(living)) continue;
            if (AntiBotModule.ignored(living)) continue;
            boolean player = living instanceof EntityPlayer;
            if (!(player ? attackPlayers.isToggled() : attackMobs.isToggled() && DamageTagsModule.isMob(living) && !Hypixel.sameTeam(living))) continue;
            if (seen.contains(living.getUniqueID())) continue;
            double d = mc.thePlayer.getDistanceSqToEntity(living);
            if (d > scanRangeSq) continue;
            if (fov.isNotDefault() && !PlayerUtils.inFov((float) fov.getInput(), living)) continue;
            if (player) {
                EntityPlayer p = (EntityPlayer) living;
                if (PlayerUtils.isFriend(p)) continue;
                if (BotCheck.isBot(p)) continue;
                if (ignoreTeammates.isToggled() && Hypixel.sameTeam(p)) continue;
            }
            if (!hitThroughBlocks.isToggled()) {
                Vec3 point = RotationUtils.visiblePoint(living);
                if (point == null) continue;
                aimPoints.put(living.getEntityId(), point);
            }
            seen.add(living.getUniqueID());
            if (player && prioritizeEnemies.isToggled()) {
                if (PlayerUtils.isEnemy((EntityPlayer) living)) {
                    if (!enemiesOnly) {
                        enemiesOnly = true;
                        list.clear();
                    }
                } else if (enemiesOnly) continue;
            }
            if (nearest == -1.0 || nearest > d) {
                nearest = d;
                nearestEntity = living;
            }
            list.add(living);
        }
        List<EntityLivingBase> inRange = null;
        boolean sameRange = swingRange == attackRange;
        Iterator<EntityLivingBase> it = list.iterator();
        while (it.hasNext()) {
            EntityLivingBase e = it.next();
            if (inRange == null) {
                if (!PlayerUtils.rayHits(e, swingRange)) {
                    it.remove();
                    continue;
                }
                if (!sameRange && !PlayerUtils.rayHits(e, attackRange)) continue;
                inRange = new ArrayList<EntityLivingBase>();
                inRange.add(e);
                continue;
            }
            if (!PlayerUtils.rayHits(e, attackRange)) continue;
            inRange.add(e);
        }
        if (inRange != null) {
            list = inRange;
        }
        if (list.isEmpty()) {
            if (fireballs && fireball != null) {
                inSwingRange = true;
                setTarget(fireball);
            } else if (nearestEntity != null && blockRange > swingRange && PlayerUtils.rayHits(nearestEntity, blockRange)) {
                inSwingRange = false;
                setTarget(nearestEntity);
            } else {
                setTarget(null);
            }
        } else {
            Comparator<EntityLivingBase> comparator = null;
            switch (sort) {
                case Distance: {
                    comparator = Comparator.comparingDouble(KillAuraModule::distanceSq);
                    break;
                }
                case Health: {
                    comparator = Comparator.comparingDouble(EntityLivingBase::getHealth);
                    break;
                }
                case Hurt_time: {
                    comparator = Comparator.comparingDouble(KillAuraModule::hurtTime);
                    break;
                }
                case Yaw: {
                    comparator = Comparator.comparingDouble(KillAuraModule::yawDifference);
                }
            }
            list.sort(comparator);
            long delay = switchDelay.getInput() < 100.0 && noAttackTicks > 0 ? 100L : (long) switchDelay.getInput();
            long now = System.currentTimeMillis();
            long cycle = (long) Math.min((double) list.size(), targets.getInput()) * delay;
            Entity chosen = null;
            Iterator<EntityLivingBase> it2 = list.iterator();
            while (it2.hasNext()) {
                EntityLivingBase e = it2.next();
                UUID uuid = e.getUniqueID();
                Long l = switchTimes.get(uuid);
                if (l == null) continue;
                if (l > 0L) {
                    if (Math.abs(now - l) < delay) {
                        chosen = e;
                        break;
                    }
                    if (cycle != delay) {
                        switchTimes.put(uuid, -l.longValue());
                        continue;
                    }
                    switchTimes.remove(uuid);
                    continue;
                }
                if (Math.abs(now - -l.longValue()) < cycle) continue;
                switchTimes.remove(uuid);
            }
            if (chosen == null) {
                it2 = list.iterator();
                while (it2.hasNext()) {
                    EntityLivingBase e = it2.next();
                    if (switchTimes.get(e.getUniqueID()) != null) continue;
                    switchTimes.put(e.getUniqueID(), now);
                    chosen = e;
                    break;
                }
                if (chosen == null && exists(previousTarget)) {
                    chosen = previousTarget.entity;
                }
            }
            inSwingRange = true;
            setTarget(chosen);
        }
    }

    private static boolean slotFree(boolean attack) {
        return ScaffoldModule.pauseForKillAura() && BedAuraModule.allowsKillAura(attack);
    }

    private static boolean dropWindowItems(S30PacketWindowItems p) {
        Minecraft mc = mc();
        return target != null && Hypixel.skywars(2) && !Overlay.screenOpen() && p.getItemStacks().length == 45 && PlayerUtils.holdingSword()
                && ItemUtils.sameItem(p.getItemStacks()[mc.thePlayer.inventory.currentItem + 36], mc.thePlayer.getHeldItem());
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (packet instanceof S2FPacketSetSlot) {
            return !dropSetSlot((S2FPacketSetSlot) packet);
        }
        if (packet instanceof S30PacketWindowItems) {
            return !dropWindowItems((S30PacketWindowItems) packet);
        }
        return true;
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = mc();
        if (interactRotations != null) {
            updateRotations();
            return;
        }
        findTarget();
        updateAps();
        boolean canAttack = true;
        if (noAttackTicks > 0) {
            --noAttackTicks;
        }
        if (badSwapTicks > 0) {
            --badSwapTicks;
        }
        if (!exists(target)) {
            canAttack = false;
        } else if (disableInInventory.isToggled() && (PacketUtils.sentCloseWindow || Overlay.screenOpen())) {
            canAttack = false;
        }
        if (target != null) {
            if (inSwingRange) {
                hudFade = null;
                hudSince = System.currentTimeMillis();
                updateRotations();
                if (!rotationSmoothing.isAtMin()) {
                    lastClientYaw = mc.thePlayer.rotationYaw;
                    lastClientPitch = mc.thePlayer.rotationPitch;
                    returning = true;
                }
            }
            if (autoBlock.packet) {
                if (!blockAllowed() || !PlayerUtils.holdingSword()) {
                    if (blockState != -1) {
                        releaseFlow(blockState);
                        blockState = -1;
                        fakeBlocking = false;
                        serverBlocking = false;
                        canAttack = false;
                    }
                } else {
                    if (blockState == 0) {
                        setBlocking(false, false);
                    }
                    switch (autoBlock) {
                        case Swap: {
                            if (slotFree(false)) {
                                switch (blockState) {
                                    case 0: {
                                        setBlocking(true, true);
                                        ++blockState;
                                        break;
                                    }
                                    case -1:
                                    case 1: {
                                        if (blockState == -1 || currentAps >= 10.0) {
                                            attackQueued = true;
                                        }
                                        rollLag();
                                        blockState = 0;
                                        swapAway();
                                        serverBlocking = false;
                                        if (lagging) break;
                                        fakeBlocking = false;
                                    }
                                }
                                break;
                            }
                            if (BedAuraModule.allowKillAura.isToggled()) {
                                canAttack = false;
                            }
                            blockState = -1;
                            break;
                        }
                        case Dual: {
                            boolean free = slotFree(false);
                            boolean noSecond = false;
                            if (free) {
                                int best = -1;
                                int second = -1;
                                double bestDamage = -1.0;
                                double secondDamage = -1.0;
                                for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
                                    ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);
                                    if (!ItemUtils.isSword(itemStack)) continue;
                                    double damage = ItemUtils.attackDamage(itemStack);
                                    if (damage > bestDamage) {
                                        secondDamage = bestDamage;
                                        second = best;
                                        bestDamage = damage;
                                        best = i;
                                        continue;
                                    }
                                    if (!(damage > secondDamage)) continue;
                                    secondDamage = damage;
                                    second = i;
                                }
                                noSecond = second == -1;
                                if (noSecond) {
                                    free = false;
                                    if (PacketUtils.lastUse == 1) {
                                        setBlocking(false, true);
                                        setBlocking(false, false);
                                    }
                                } else if (attackQueued) {
                                    blockState = 1;
                                    int slot = (dualSwaps++ & 1) == 0 ? best : second;
                                    if (slot == PacketUtils.serverSlot) {
                                        slot = slot == best ? second : best;
                                    }
                                    PlayerUtils.sendSlot(slot);
                                } else {
                                    blockState = 0;
                                }
                            }
                            if (free) break;
                            if (BedAuraModule.allowKillAura.isToggled()) {
                                canAttack = false;
                            }
                            blockState = noSecond ? -2 : -1;
                            break;
                        }
                        case Interact_A:
                        case Interact_B:
                        case Interact_C:
                        case Interact_D: {
                            if (slotFree(false)) {
                                switch (blockState) {
                                    case 0: {
                                        if (currentAps >= 10.0) {
                                            attackQueued = true;
                                        }
                                        if (!attackQueued) break;
                                        ++blockState;
                                        if (autoBlock != AutoBlock.Interact_B && autoBlock != AutoBlock.Interact_C || PacketUtils.lastUse == -1) break;
                                        PlayerUtils.sendSlot(mc.thePlayer.inventory.currentItem);
                                        break;
                                    }
                                    case -1:
                                    case 1: {
                                        rollLag();
                                        if (blockState == 1 || PacketUtils.lastUse == 1) {
                                            if (autoBlock == AutoBlock.Interact_B || autoBlock == AutoBlock.Interact_C) {
                                                PlayerUtils.sendSlot(freeSlot());
                                                if (autoBlock == AutoBlock.Interact_C) {
                                                    setBlocking(false, true);
                                                    if (lagging) {
                                                        fakeBlocking = true;
                                                    }
                                                } else {
                                                    serverBlocking = false;
                                                    if (!lagging) {
                                                        fakeBlocking = false;
                                                    }
                                                }
                                            } else if (serverBlocking || PacketUtils.lastUse == 1) {
                                                if (autoBlock == AutoBlock.Interact_D) {
                                                    swapAway();
                                                }
                                                setBlocking(false, true);
                                                if (lagging) {
                                                    fakeBlocking = true;
                                                }
                                            }
                                        }
                                        blockState = 0;
                                        canAttack = false;
                                    }
                                }
                                break;
                            }
                            if (BedAuraModule.allowKillAura.isToggled()) {
                                canAttack = false;
                            }
                            blockState = -1;
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }
        if (!inSwingRange && returning) {
            returning = smoothReturn();
        }
        if (!slotFree(true)) {
            canAttack = false;
        }
        boolean forced = false;
        if (forcedAttacks > 0) {
            --forcedAttacks;
            if (autoBlock != AutoBlock.Dual || blockState == -1 || blockState == -2) {
                forced = true;
                attackQueued = true;
            }
        }
        if (attackQueued && PacketUtils.noAttackPlaceOrDig()) {
            attackQueued = false;
            if (canAttack) {
                Entity entity = target.entity;
                boolean swinging = inSwingRange;
                boolean inReach = false;
                if (swinging) {
                    boolean sameRange = attackRange == swingRange;
                    inReach = sameRange && rotationSmoothing.isAtMin() || PlayerUtils.rayHits(entity, rotations[0], rotations[1], attackRange);
                }
                if (swinging) {
                    boolean attack = true;
                    boolean swing = true;
                    boolean silent = silentSwingWhileBlocking.isToggled() && PlayerUtils.isBlocking();
                    if (noAttackTicks > 0) {
                        attack = false;
                    } else if (!inReach) {
                        attack = false;
                    }
                    if (disableWhileBlocking.isToggled() && PlayerUtils.isBlocking()) {
                        swing = false;
                        attack = false;
                    } else if (AntiKnockbackModule.disableAttackWhileDelay.isToggled() && AntiKnockbackModule.delayUntil.get() != 0L) {
                        swing = false;
                        attack = false;
                    } else if (!forced && HitSelectModule.holdForKillAura(entity)) {
                        swing = false;
                        attack = false;
                    } else if (ModuleManager.enabled("Displace") != null && DisplaceModule.killAura.isToggled()) {
                        if (DisplaceModule.queued) {
                            attack = false;
                        } else if (DisplaceModule.lastAt > 0L && Math.abs(System.currentTimeMillis() - DisplaceModule.lastAt) < 60L) {
                            DisplaceModule.rearm();
                        } else if (!forced) {
                            if (DisplaceModule.holding) {
                                swing = false;
                                attack = false;
                            } else if (DisplaceModule.overrideAttacks.isToggled() && DisplaceModule.canDisplace(entity, false, false)) {
                                swing = false;
                                attack = false;
                            }
                        }
                    }
                    if (attack) {
                        attackedThisTick = true;
                        attacking = true;
                        PlayerUtils.attack(entity, silent);
                        attacking = false;
                        Irc.onKillAuraHit();
                    } else if (swing) {
                        PlayerUtils.swing(silent, false);
                        ClickUtils.onAttackClick();
                    } else {
                        setDelay(true);
                    }
                }
                if (blockAllowed() && blockState == 1 && autoBlock.interacts) {
                    boolean interact = true;
                    boolean block = true;
                    if (disableWhileBlocking.isToggled() && PlayerUtils.isBlocking()) {
                        interact = false;
                    }
                    if (inReach && interact) {
                        MovingObjectPosition hit = BotCheck.rayTrace(entity, attackRange + 0.3, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, rotations[0], rotations[1]);
                        if (hit != null) {
                            Vec3 vec3 = new Vec3(hit.hitVec.xCoord - entity.posX, hit.hitVec.yCoord - entity.posY, hit.hitVec.zCoord - entity.posZ);
                            PacketUtils.send(new C02PacketUseEntity(entity, vec3));
                            PacketUtils.send(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT));
                        } else {
                            block = false;
                        }
                    }
                    if (!ItemUtils.isSword(mc.thePlayer.inventory.getStackInSlot(PacketUtils.serverSlot))) {
                        block = false;
                    }
                    if (block) {
                        setBlocking(true, true);
                    }
                }
            }
        }
    }

    private static boolean rightDown() {
        return Mouse.isButtonDown(1);
    }

    private static void rollLag() {
        if ((lagAccumulator += (int) autoBlockLag.getInput()) >= 100) {
            lagAccumulator -= 100;
            lagging = true;
        } else {
            lagging = false;
        }
    }

    @Override
    public void onKnockback(S12PacketEntityVelocity packet, long now) {
        if (!attackOnDamage.isAtMin() && now - PacketUtils.lastVelocity > 250L) {
            forceAttacks((int) attackOnDamage.getInput());
        }
    }

    private static double distanceSq(EntityLivingBase e) {
        return mc().thePlayer.getDistanceSqToEntity(e);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        Minecraft mc = mc();
        if (e.button == 0) {
            if (e.buttonstate && target != null) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    e.setCanceled(true);
                } else if (autoBlock.packet && blockAllowed()) {
                    e.setCanceled(true);
                }
            }
        } else if (e.button == 1) {
            if (!e.buttonstate) {
                passedRightClick = false;
            }
            if (autoBlock.packet) {
                if (PlayerUtils.holdingSword()) {
                    int n;
                    if (target != null && manualBlock.isToggled() && blockState == -1 && (n = KeyUtils.heldHotbarKey()) != -1
                            && !ItemUtils.isSword(mc.thePlayer.inventory.getStackInSlot(n))) {
                        return;
                    }
                    if (target == null && mc.objectMouseOver != null) {
                        if (mc.objectMouseOver.entityHit != null) {
                            if (BotCheck.isBot(mc.objectMouseOver.entityHit)) {
                                fakeBlocking = false;
                                return;
                            }
                        }
                        BlockPos pos = mc.objectMouseOver.getBlockPos();
                        if (pos != null && BlockUtils.isInteractable(BlockUtils.getBlock(pos))) {
                            fakeBlocking = false;
                            return;
                        }
                    }
                    if (autoBlock == AutoBlock.Dual) {
                        if (e.buttonstate) {
                            if (target == null) {
                                fakeBlocking = true;
                            }
                        } else {
                            fakeBlocking = false;
                        }
                    } else if (e.buttonstate) {
                        fakeBlocking = true;
                    } else if (target == null) {
                        fakeBlocking = false;
                    }
                    e.setCanceled(true);
                }
            } else if (autoBlock == AutoBlock.Manual && e.buttonstate && target != null && !weaponOnly.isToggled() && !PlayerUtils.holdingSword()
                    && mc.objectMouseOver.entityHit == null) {
                e.setCanceled(true);
            }
        }
    }

    public static boolean blockAllowed() {
        return !manualBlock.isToggled() || rightDown();
    }

    @Override
    public void guiUpdate() {
        SliderSetting.order(attackRangeSetting, swingRangeSetting);
        SliderSetting.order(swingRangeSetting, blockRangeSetting);
        attackRange = attackRangeSetting.getInput();
        swingRange = swingRangeSetting.getInput();
        blockRange = blockRangeSetting.getInput();
        double d = Math.max(Math.max(attackRange, swingRange), blockRange) + 1.2;
        scanRangeSq = d * d;
        autoBlock = AutoBlock.values()[(int) autoBlockSetting.getInput()];
        rotation = Rotation.values()[(int) rotationSetting.getInput()];
        sort = Sort.values()[(int) sortSetting.getInput()];
    }

    private static int freeSlot() {
        int current = PlayerUtils.currentSlot();
        InventoryPlayer inventory = mc().thePlayer.inventory;
        for (int i = 0; i < 9; ++i) {
            if (i == current) continue;
            if (inventory.getStackInSlot(i) != null) continue;
            return i;
        }
        for (int i = 0; i < 9; ++i) {
            if (i == current) continue;
            Item item = inventory.getStackInSlot(i).getItem();
            if (item == FREE_SKIP[0] || item == FREE_SKIP[1] || item == FREE_SKIP[2]) continue;
            return i;
        }
        return (current + 1) % 9;
    }

    private static float[] takeRotations() {
        float[] rot = rotations;
        rotations = null;
        if (DisplaceModule.queued) {
            DisplaceModule.queued = false;
            if (!(rot == null || target == null || attackedThisTick || disableWhileBlocking.isToggled() && PlayerUtils.isBlocking()
                    || !PlayerUtils.rayHits(target.entity, rot[0], rot[1], attackRange))) {
                rot[0] = rot[0] + DisplaceModule.offset();
                ++forcedAttacks;
                skipSmoothing = true;
                DisplaceModule.startBlink();
            } else {
                DisplaceModule.clear();
            }
        }
        return rot;
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == hitThroughBlocks && hitThroughBlocks.isToggled()) {
            aimPoint = null;
            aimPoints.clear();
        }
    }

    private static final Item[] FREE_SKIP = {Items.compass, Items.skull, Items.snowball};
}
