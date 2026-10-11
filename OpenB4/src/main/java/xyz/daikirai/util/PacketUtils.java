package xyz.daikirai.util;

import xyz.daikirai.hook.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public final class PacketUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Packet<?> sending;
    public static Packet<?> sendingNoEvent;
    public static Packet<?> sendingQueued;
    public static Packet<?> sendingSkipScripts;

    public static volatile Packet<?> reprocessing;
    public static boolean sentCloseWindow;
    public static boolean sentOpenInventory;
    public static boolean sentClickWindow;
    public static boolean sentDigging;
    public static boolean sentAnyDigging;
    public static boolean sentBlockPlace;
    public static boolean sentAnyPlacement;
    public static boolean sentInteractAt;
    public static boolean sentUseEntity;
    public static boolean sentAttack;
    public static boolean sentSwing;
    public static int slotChanges;
    public static int serverSlot;
    public static int previousServerSlot;
    public static int lastUse = -1;

    public static boolean sentPlacementPost;
    public static boolean sentUseEntityPost;
    public static int slotPost;
    public static int previousSlotPost;
    public static int slotChangesPost;

    public static long lastPosLook;

    public static long lastExplosion;
    public static long lastVelocity;

    private PacketUtils() {
    }

    public static void send(Packet<?> packet) {
        sending = packet;
        mc.getNetHandler().addToSendQueue(sending);
        sending = null;
    }

    public static void sendNoEvent(Packet<?> packet) {
        sendingNoEvent = packet;
        send(sendingNoEvent);
        sendingNoEvent = null;
    }

    public static void sendQueued(Packet<?> packet) {
        sendingQueued = packet;
        sendNoEvent(sendingQueued);
        sendingQueued = null;
    }

    public static void sendSkipScripts(Packet<?> packet) {
        sendingSkipScripts = packet;
        send(sendingSkipScripts);
        sendingSkipScripts = null;
    }

    public static void sendThroughLag(Packet<?> packet) {
        if (!Hooks.lagPassesOutbound(packet)) {
            return;
        }
        sendNoEvent(packet);
    }

    public static void receive(Packet<?> packet) {
        Hooks.onReceive(packet, mc.getNetHandler().getNetworkManager().getNetHandler());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void receiveNoEvent(Packet packet) {
        try {
            packet.processPacket(mc.getNetHandler().getNetworkManager().getNetHandler());
        } catch (Exception exception) {
        }
    }

    public static void resetLastUse() {
        slotChangesPost = 0;
        lastUse = -1;
    }

    public static void track(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            sentCloseWindow = false;
            sentOpenInventory = false;
            sentClickWindow = false;
            sentDigging = false;
            sentAnyDigging = false;
            sentBlockPlace = false;
            sentAnyPlacement = false;
            sentInteractAt = false;
            sentUseEntity = false;
            sentAttack = false;
            sentSwing = false;
            slotChanges = 0;
        } else if (packet instanceof C0APacketAnimation) {
            sentSwing = true;
        } else if (packet instanceof C02PacketUseEntity) {
            sentUseEntity = true;
            C02PacketUseEntity.Action action = ((C02PacketUseEntity) packet).getAction();
            if (action == C02PacketUseEntity.Action.ATTACK) {
                sentAttack = true;
            } else if (action == C02PacketUseEntity.Action.INTERACT_AT) {
                sentInteractAt = true;
            }
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            sentAnyPlacement = true;
            if (((C08PacketPlayerBlockPlacement) packet).getPlacedBlockDirection() == 255) {
                lastUse = 1;
            } else {
                sentBlockPlace = true;
            }
        } else if (packet instanceof C07PacketPlayerDigging) {
            sentAnyDigging = true;
            C07PacketPlayerDigging.Action action = ((C07PacketPlayerDigging) packet).getStatus();
            if (action == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                lastUse = 2;
            } else if (action == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK || action == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK || action == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
                sentDigging = true;
            }
        } else if (packet instanceof C09PacketHeldItemChange) {
            previousServerSlot = serverSlot;
            serverSlot = ((C09PacketHeldItemChange) packet).getSlotId();
            lastUse = 3;
            ++slotChanges;
            xyz.daikirai.module.dev.DebugAcModule.updateHeldItem(serverSlot);
        } else if (packet instanceof C0DPacketCloseWindow) {
            sentCloseWindow = true;
        } else if (packet instanceof C0EPacketClickWindow) {
            sentClickWindow = true;
        } else if (packet instanceof C16PacketClientStatus && ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
            sentOpenInventory = true;
        }
    }

    public static void trackPost(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            sentPlacementPost = false;
            sentUseEntityPost = false;
        } else if (packet instanceof C02PacketUseEntity) {
            sentUseEntityPost = true;
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            sentPlacementPost = true;
        } else if (packet instanceof C09PacketHeldItemChange) {
            previousSlotPost = slotPost;
            slotPost = ((C09PacketHeldItemChange) packet).getSlotId();
            ++slotChangesPost;
        }
    }

    public static void trackReceived(Packet<?> packet) {
        if (packet instanceof S09PacketHeldItemChange) {
            previousServerSlot = serverSlot;
            serverSlot = ((S09PacketHeldItemChange) packet).getHeldItemHotbarIndex();
        } else if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion p = (S27PacketExplosion) packet;
            if (mc.thePlayer.getDistanceSq(p.getX(), p.getY(), p.getZ()) <= 25.0) {
                lastExplosion = System.currentTimeMillis();
            }
        }
    }

    public static boolean noUseOrDig() {
        return !sentUseEntity && !sentAnyDigging;
    }

    public static boolean noInteraction() {
        return !(sentUseEntity || sentAnyPlacement || sentAnyDigging);
    }

    public static boolean noInteractionOrBreak() {
        return !(sentUseEntity || sentAnyPlacement || sentDigging);
    }

    public static boolean noAttackPlaceOrDig() {
        return !(sentAttack || sentAnyPlacement || sentAnyDigging);
    }

    public static boolean isKnockback(S12PacketEntityVelocity p) {
        return p.getMotionY() > 0 && p.getMotionY() < 4500;
    }
}
