package xyz.daikirai.module.network;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.hook.Hooks;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.combat.KillAuraModule;
import xyz.daikirai.module.player.BedAuraModule;
import xyz.daikirai.module.player.ScaffoldModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.module.world.AntiBotModule;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.KeyUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.Theme;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

public class LagRangeModule extends Module {
    public static LagRangeModule instance;
    public static SliderSetting delayClose, delayFar, maxLagTime, startRange;
    public static ButtonSetting blockSwordOnLag, disableAdventure, ignoreTeammates, invisiblePlayers, onlyEnemies, randomLag, showServerPosition, weaponOnly;
    public static ButtonSetting holdingAnyBlock, holdingConsumable, holdingBow, holdingBuckets, holdingFireball, holdingFishingRod, holdingLadders,
            onInteractItem;

    private static final ConcurrentSkipListMap<Long, List<Packet<?>>> delayed = new ConcurrentSkipListMap<Long, List<Packet<?>>>();
    public static EntityPlayer target;
    private static EntityPlayer pending;
    private static double pendingDistSq;
    public static EntityOtherPlayerMP ghost;
    private static final AtomicReference<Vec3> ghostPos = new AtomicReference<Vec3>();
    private static boolean caughtUp;
    private static long delay;
    private static long startedAt;
    private static long maxTime;
    private static int randomPause;
    private static boolean flushNext;
    private static int missing;
    private static int movingAway;
    private static double range, rangeSq, keepRangeSq, randomRangeSq;
    private static Timer timer;

    public LagRangeModule(int key) {
        super("Lag Range", Category.network, key);
        instance = this;
        this.lite = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        delayClose = (SliderSetting) ModuleManager.setting(this, "Packet delay (close)");
        delayFar = (SliderSetting) ModuleManager.setting(this, "Packet delay (far)");
        maxLagTime = (SliderSetting) ModuleManager.setting(this, "Max lag time");
        startRange = (SliderSetting) ModuleManager.setting(this, "Start range");
        blockSwordOnLag = (ButtonSetting) ModuleManager.setting(this, "Block sword on lag");
        disableAdventure = (ButtonSetting) ModuleManager.setting(this, "Disable adventure");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        invisiblePlayers = (ButtonSetting) ModuleManager.setting(this, "Invisible players");
        onlyEnemies = (ButtonSetting) ModuleManager.setting(this, "Only enemies");
        randomLag = (ButtonSetting) ModuleManager.setting(this, "Random lag");
        showServerPosition = (ButtonSetting) ModuleManager.setting(this, "Show server position");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
        holdingAnyBlock = (ButtonSetting) ModuleManager.setting(this, "Holding any block");
        holdingConsumable = (ButtonSetting) ModuleManager.setting(this, "Holding consumable");
        holdingBow = (ButtonSetting) ModuleManager.setting(this, "Holding bow");
        holdingBuckets = (ButtonSetting) ModuleManager.setting(this, "Holding buckets");
        holdingFireball = (ButtonSetting) ModuleManager.setting(this, "Holding fireball");
        holdingFishingRod = (ButtonSetting) ModuleManager.setting(this, "Holding fishing rod");
        holdingLadders = (ButtonSetting) ModuleManager.setting(this, "Holding ladders");
        onInteractItem = (ButtonSetting) ModuleManager.setting(this, "On interact item");
    }

    @Override
    public Entity getTarget() {
        return target;
    }

    private static void stop() {
        ghost = null;
        pending = null;
        target = null;
        movingAway = 0;
        missing = 0;
        release(true);
        if (randomLag.isToggled()) {
            randomPause = MathUtils.randomRange(6000, 20000);
        }
    }

    private static void start(EntityPlayer player, double distSq) {
        if (player == null) {
            target = mc().thePlayer;
            delay = (long) (int) delayClose.getInput();
        } else {
            if (distSq > 42.25) {
                delay = delayFar.isAtMin() ? (long) (int) delayClose.getInput() : (long) (int) delayFar.getInput();
            } else {
                if (delayClose.isAtMin()) {
                    return;
                }
                delay = (long) (int) delayClose.getInput();
            }
            target = player;
        }
        delay += (long) MathUtils.randomRange(-12, 12);
        startedAt = System.currentTimeMillis();
        maxTime = (long) (maxLagTime.getInput() * 1000.0);
        int n = (int) (maxTime / 11L);
        maxTime += (long) MathUtils.randomRange(-n, n);
        if (showServerPosition.isToggled()) {
            makeGhost();
        }
    }

    private static void release(boolean all) {
        if (delayed.isEmpty()) {
            return;
        }
        if (!Chat.inWorld()) {
            delayed.clear();
            if (!all) {
                stop();
            }
            return;
        }
        if (all) {
            for (Map.Entry<Long, List<Packet<?>>> entry : delayed.entrySet()) {
                send(entry.getValue());
            }
            delayed.clear();
        } else {
            Map.Entry<Long, List<Packet<?>>> entry;
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Long, List<Packet<?>>>> it = delayed.entrySet().iterator();
            while (it.hasNext() && now >= (entry = it.next()).getKey()) {
                send(entry.getValue());
                it.remove();
            }
        }
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        Minecraft mc = mc();
        if (target == null) {
            return true;
        }
        if (!Hooks.finalSendCheck(packet)) {
            return false;
        }
        boolean flush = false;
        if (packet instanceof C01PacketChatMessage) {
            flushNext = true;
        } else if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity p = (C02PacketUseEntity) packet;
            Entity entity;
            if (p.getAction() == C02PacketUseEntity.Action.ATTACK && ((entity = p.getEntityFromWorld(mc.theWorld)) != target || target.hurtTime <= 2)) {
                flushNext = true;
            }
        } else if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging.Action action = ((C07PacketPlayerDigging) packet).getStatus();
            if (action == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK || action == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
                    || action == C07PacketPlayerDigging.Action.DROP_ITEM || action == C07PacketPlayerDigging.Action.DROP_ALL_ITEMS) {
                flushNext = true;
            }
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
            if (onInteractItem.isToggled() && !ItemUtils.isSword(p.getStack())) {
                flushNext = true;
            }
        } else if (flushNext && packet instanceof C03PacketPlayer) {
            flushNext = false;
            flush = true;
        }
        long l = System.currentTimeMillis() + delay;
        List<Packet<?>> list = delayed.computeIfAbsent(l, k -> Collections.synchronizedList(new ArrayList<Packet<?>>()));
        list.add(packet);
        delayed.put(l, list);
        if (flush) {
            stop();
        }
        return false;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (ghost == null || BlinkModule.ghost != null || !Chat.inWorld()) {
            return;
        }
        if (SettingsModule.hideFirstPersonSelfEsp.isToggled() && mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        if (caughtUp && !PlayerUtils.isMoving()) {
            return;
        }
        Render3D.entityBox(ghost, 2, Theme.defaultTheme.getRGB(0.0), e.partialTicks);
    }

    private static boolean allowed(boolean lagging) {
        Minecraft mc = mc();
        if (!Chat.inWorld() || mc.isSingleplayer()) {
            return false;
        }
        if (delayClose.isAtMin() && delayFar.isAtMin()) {
            return false;
        }
        if (ScaffoldModule.active) {
            return false;
        }
        if (mc.thePlayer.capabilities.allowFlying || disableAdventure.isToggled() && PlayerUtils.isAdventure()) {
            return false;
        }
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        if (weaponOnly.isToggled() && !ItemUtils.isWeapon(itemStack)) {
            return false;
        }
        if (itemStack != null) {
            if (holdingAnyBlock.isToggled() && itemStack.getItem() instanceof ItemBlock || holdingConsumable.isToggled() && ItemUtils.isConsumable(itemStack)
                    || holdingBuckets.isToggled() && itemStack.getItem() instanceof ItemBucket || holdingBow.isToggled() && itemStack.getItem() == Items.bow
                    || holdingFireball.isToggled() && itemStack.getItem() == Items.fire_charge || holdingFishingRod.isToggled() && itemStack.getItem() == Items.fishing_rod) {
                return false;
            }
        }
        if (!lagging) {
            if (!mc.thePlayer.onGround && mc.thePlayer.hurtTime > 0 && mc.thePlayer.motionY > 0.0) {
                return false;
            }
            if (!PlayerUtils.isMoving()) {
                return false;
            }
            if (PlayerUtils.direction().backward) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == showServerPosition && !b.isToggled()) {
            ghost = null;
        }
    }

    @Override
    public void onDisable() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        stop();
        flushNext = false;
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = mc();
        if (ghost != null) {
            ghost.lastTickPosX = ghost.posX;
            ghost.lastTickPosY = ghost.posY;
            ghost.lastTickPosZ = ghost.posZ;
            Vec3 pos = ghostPos.get();
            ghost.setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
            caughtUp = Math.abs(ghost.posX - mc.thePlayer.posX) < 0.05 && Math.abs(ghost.posY - mc.thePlayer.posY) < 0.05 && Math.abs(ghost.posZ - mc.thePlayer.posZ) < 0.05;
        }
        updateRanges();
        long now = System.currentTimeMillis();
        if (target != null) {
            if (now - startedAt > maxTime) {
                stop();
                return;
            }
            if (!allowed(true)) {
                stop();
                return;
            }
            if (randomLag.isToggled() && target == mc.thePlayer) {
                if (!lookingAtNothing()) {
                    stop();
                    return;
                }
                boolean near = false;
                for (EntityPlayer p : mc.theWorld.playerEntities) {
                    if (p == mc.thePlayer || p.deathTime != 0) continue;
                    if (PlayerUtils.isFriend(p) || ignoreTeammates.isToggled() && Hypixel.sameTeam(p)) continue;
                    double d = mc.thePlayer.getDistanceSqToEntity(p);
                    if (d < randomRangeSq) {
                        near = true;
                        break;
                    }
                }
                if (near) {
                    stop();
                }
                return;
            }
            if (target != null && mc.theWorld.getEntityByID(target.getEntityId()) != null) {
                missing = 0;
                double d = mc.thePlayer.getDistanceSqToEntity(target);
                if (d > keepRangeSq) {
                    stop();
                    return;
                }
                double[] m = PlayerUtils.motionTowards(target);
                boolean moving = m[0] >= 0.02;
                boolean away = moving && m[1] <= -0.15;
                movingAway = away ? ++movingAway : 0;
                if (movingAway >= 4) {
                    stop();
                }
            } else if (++missing >= 3) {
                stop();
            }
        } else if (allowed(false)) {
            if (pending != null) {
                start(pending, pendingDistSq);
                pending = null;
                return;
            }
            double nearest = -1.0;
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == mc.thePlayer || p.deathTime != 0) continue;
                if (!invisiblePlayers.isToggled() && p.isInvisible()) continue;
                if (AntiBotModule.ignored(p)) continue;
                if (onlyEnemies.isToggled() && !PlayerUtils.isEnemy(p)) continue;
                double d = mc.thePlayer.getDistanceSqToEntity(p);
                if (nearest == -1.0 || nearest > d) {
                    nearest = d;
                }
                if (d > rangeSq) continue;
                if (PlayerUtils.isFriend(p) || BotCheck.isBot(p)) continue;
                if (ignoreTeammates.isToggled() && Hypixel.sameTeam(p)) continue;
                double[] m = PlayerUtils.motionTowards(p);
                boolean moving = m[0] >= 0.02;
                boolean towards = moving && m[1] >= 0.15;
                if (!towards) continue;
                if (blockSwordOnLag.isToggled() && !delayClose.isAtMin() && d <= 42.25 && PlayerUtils.holdingSword() && !PlayerUtils.isBlocking()
                        && !PlayerUtils.lookingAtInteractable() && (KillAuraModule.disabled || !KillAuraModule.autoBlock.packet) && BedAuraModule.target == null) {
                    KeyUtils.click(mc.gameSettings.keyBindUseItem);
                    pending = p;
                    pendingDistSq = d;
                } else {
                    start(p, d);
                }
                return;
            }
            if (target == null && randomLag.isToggled() && nearest > randomRangeSq && startedAt != 0L && now > startedAt + randomPause && lookingAtNothing()) {
                start(null, 0.0);
            }
        }
    }

    private static void makeGhost() {
        Minecraft mc = mc();
        caughtUp = true;
        ghost = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        ghost.setEntityId(-170);
        ghost.copyLocationAndAnglesFrom(mc.thePlayer);
        ghostPos.set(new Vec3(ghost.posX, ghost.posY, ghost.posZ));
    }

    private static void send(List<Packet<?>> list) {
        synchronized (list) {
            for (Packet<?> packet : list) {
                C03PacketPlayer c03;
                if (ghost != null && packet instanceof C03PacketPlayer && (c03 = (C03PacketPlayer) packet).isMoving()) {
                    ghostPos.set(new Vec3(c03.getPositionX(), c03.getPositionY(), c03.getPositionZ()));
                }
                Module fakeLag = ModuleManager.enabled("Fake Lag");
                if (fakeLag != null) {
                    fakeLag.onSendPacket(packet);
                    continue;
                }
                if (PacketQueue.active) {
                    PacketQueue.hold(packet);
                    continue;
                }
                PacketUtils.sendQueued(packet);
            }
        }
    }

    private static boolean lookingAtNothing() {
        Minecraft mc = mc();
        if (mc.thePlayer.rotationPitch > 70.0f && PlayerUtils.holdingBlock()) {
            return false;
        }
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block = BlockUtils.getBlock(mc.objectMouseOver.getBlockPos());
            return !BlockUtils.isInteractable(block);
        }
        return true;
    }

    private static void updateRanges() {
        range = startRange.getInput() + PlayerUtils.perSpeedLevel(1.0);
        rangeSq = Math.pow(range, 2.0);
        keepRangeSq = Math.pow(range + 1.0, 2.0);
        randomRangeSq = Math.pow(range + 5.0, 2.0);
    }

    @Override
    public void onEnable() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (target != null) {
                    release(false);
                }
            }
        }, 0L, 5L);
    }

    @Override
    public String getInfo() {
        if (delayClose.isAtMin()) {
            return delayFar.isAtMin() ? null : delayFar.getDisplay(false);
        }
        return delayClose.getDisplay(false);
    }
}
