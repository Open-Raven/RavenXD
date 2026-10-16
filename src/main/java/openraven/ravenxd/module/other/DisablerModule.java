package openraven.ravenxd.module.other;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C18PacketSpectate;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

// skidded from rise + sum liquidbounce script + sum vibe client on github
public class DisablerModule extends Module {
    public enum Mode {
        Verus, Vulcan, OldMatrix
    }

    public static Mode current = Mode.Verus;
    public static SliderSetting mode;
    public static ButtonSetting verusMovement, verusSprint, verusDamage;
    public static ButtonSetting vulcanSprint, vulcanAutoClicker, vulcanReach, vulcanMovement, vulcanFastUse;
    public static ButtonSetting matrixC17, matrixC05, matrixC06, matrixC04, matrixC18, matrixC03, matrixS08;

    private final ConcurrentLinkedQueue<Packet<?>> transactions = new ConcurrentLinkedQueue<Packet<?>>();
    private final ConcurrentLinkedQueue<Packet<?>> keepAlives = new ConcurrentLinkedQueue<Packet<?>>();
    private final ConcurrentLinkedQueue<Packet<?>> blinked = new ConcurrentLinkedQueue<Packet<?>>();
    private volatile boolean teleported;
    private long stopWatch;
    private int lastHurtTime;

    public DisablerModule(int key) {
        super("Disabler", Category.other, key);
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        verusMovement = (ButtonSetting) ModuleManager.setting(this, "Movement");
        verusSprint = (ButtonSetting) ModuleManager.setting(this, "Sprint");
        verusDamage = (ButtonSetting) ModuleManager.setting(this, "Damage");
        vulcanSprint = (ButtonSetting) ModuleManager.setting(this, "Omni-Sprint");
        vulcanAutoClicker = (ButtonSetting) ModuleManager.setting(this, "Auto clicker");
        vulcanReach = (ButtonSetting) ModuleManager.setting(this, "Reach");
        vulcanMovement = (ButtonSetting) ModuleManager.setting(this, "Strafe and jump");
        vulcanFastUse = (ButtonSetting) ModuleManager.setting(this, "Fast use");
        matrixC17 = (ButtonSetting) ModuleManager.setting(this, "Cancel C17 custom payload");
        matrixC05 = (ButtonSetting) ModuleManager.setting(this, "Cancel C05 look");
        matrixC06 = (ButtonSetting) ModuleManager.setting(this, "Cancel C06 pos look");
        matrixC04 = (ButtonSetting) ModuleManager.setting(this, "Cancel C04 position");
        matrixC18 = (ButtonSetting) ModuleManager.setting(this, "Cancel C18 spectate");
        matrixC03 = (ButtonSetting) ModuleManager.setting(this, "Cancel all C03");
        matrixS08 = (ButtonSetting) ModuleManager.setting(this, "Cancel S08 setbacks");
    }

    @Override
    public void guiUpdate() {
        Mode m = Mode.values()[(int) mode.getInput()];
        if (m != current) {
            this.flush();
        }
        current = m;
    }

    @Override
    public void onEnable() {
        this.stopWatch = System.currentTimeMillis();
        this.lastHurtTime = 0;
        if (current == Mode.Verus && verusMovement.isToggled()) {
            Chat.print("&7[Disabler] Verus movement: wait for damage from the disabler or otherwise.");
        }
    }

    @Override
    public void onDisable() {
        this.flush();
    }

    private void flush() {
        Packet<?> p;
        while ((p = this.blinked.poll()) != null) PacketUtils.sendNoEvent(p);
        while ((p = this.transactions.poll()) != null) PacketUtils.sendNoEvent(p);
        while ((p = this.keepAlives.poll()) != null) PacketUtils.sendNoEvent(p);
        this.teleported = false;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity != Minecraft.getMinecraft().thePlayer) {
            return;
        }
        this.transactions.clear();
        this.keepAlives.clear();
        this.blinked.clear();
        this.teleported = false;
        if (current == Mode.Verus && verusMovement.isToggled()) {
            Chat.print("&7[Disabler] Verus movement: wait for damage from the disabler or otherwise.");
        }
    }

    private boolean elapsed(long ms) {
        return System.currentTimeMillis() - this.stopWatch >= ms;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (current == Mode.Verus) {
            this.verus(p);
        } else if (current == Mode.Vulcan) {
            this.vulcan(p);
        }
    }

    private void verus(EntityPlayerSP p) {
        if (verusMovement.isToggled()) {
            ++this.lastHurtTime;
            if (p.ticksExisted % 100 == 0) {
                PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(p.posX, p.posY, p.posZ, p.onGround));
                PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(p.posX, -0.015625, p.posZ, false));
                PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(p.posX, p.posY, p.posZ, p.onGround));
                this.teleported = true;
            }
            if (this.elapsed(500L) && !this.transactions.isEmpty()) {
                PacketUtils.sendNoEvent(this.transactions.poll());
                this.stopWatch = System.currentTimeMillis();
            }
            if (verusDamage.isToggled()) {
                if (p.ticksExisted % 200 == 0 && this.lastHurtTime >= 100) {
                    PlayerUtils.damagePlayerVerus(3.42, 1, false, false);
                }
                if (this.lastHurtTime > 101) {
                    Chat.print("&7[Disabler] Verus movement: you must get damaged now.");
                }
            }
        }
        if (p.hurtTime > 0) {
            this.lastHurtTime = 0;
        }
        if (this.elapsed(400L) && !this.keepAlives.isEmpty()) {
            PacketUtils.sendNoEvent(this.keepAlives.poll());
            this.stopWatch = System.currentTimeMillis();
        }
        if (verusSprint.isToggled() && PlayerUtils.isMoving()) {
            PacketUtils.sendNoEvent(new C0BPacketEntityAction(p, p.ticksExisted % 2 == 0
                    ? C0BPacketEntityAction.Action.STOP_SPRINTING : C0BPacketEntityAction.Action.START_SPRINTING));
        }
    }

    private void vulcan(EntityPlayerSP p) {
        if (p.ticksExisted < 20) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (vulcanSprint.isToggled()) {
            PacketUtils.send(new C0BPacketEntityAction(p, C0BPacketEntityAction.Action.START_SPRINTING));
            PacketUtils.send(new C0BPacketEntityAction(p, C0BPacketEntityAction.Action.STOP_SPRINTING));
        }
        if (vulcanMovement.isToggled() && p.ticksExisted % 5 == 0) {
            PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, new BlockPos(p), EnumFacing.UP));
        }
        if (vulcanAutoClicker.isToggled() && p.ticksExisted % 100 == 0 && mc.currentScreen == null) {
            PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, new BlockPos(p), EnumFacing.UP));
        }
        if (vulcanFastUse.isToggled() && p.ticksExisted % 7 == 0) {
            PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
        if (vulcanReach.isToggled() && p.ticksExisted % 2 == 0) {
            Packet<?> q;
            while ((q = this.blinked.poll()) != null) PacketUtils.sendNoEvent(q);
        }
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        switch (current) {
            case Verus:
                if (verusSprint.isToggled() && packet instanceof C0BPacketEntityAction) {
                    C0BPacketEntityAction.Action a = ((C0BPacketEntityAction) packet).getAction();
                    if (a == C0BPacketEntityAction.Action.START_SPRINTING || a == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                        return false;
                    }
                }
                if (verusMovement.isToggled()) {
                    if (packet instanceof C0FPacketConfirmTransaction) {
                        this.transactions.add(packet);
                        if (this.transactions.size() > 300) {
                            PacketUtils.sendNoEvent(this.transactions.poll());
                        }
                        return false;
                    }
                    if (packet instanceof C00PacketKeepAlive) {
                        this.keepAlives.add(new C00PacketKeepAlive(((C00PacketKeepAlive) packet).getKey() - 30));
                        return false;
                    }
                }
                return true;
            case Vulcan:
                if (vulcanReach.isToggled() && !(packet instanceof C00PacketKeepAlive)) {
                    this.blinked.add(packet);
                    return false;
                }
                return true;
            case OldMatrix:
                return !(matrixC17.isToggled() && packet instanceof C17PacketCustomPayload
                        || matrixC05.isToggled() && packet instanceof C03PacketPlayer.C05PacketPlayerLook
                        || matrixC06.isToggled() && packet instanceof C03PacketPlayer.C06PacketPlayerPosLook
                        || matrixC04.isToggled() && packet instanceof C03PacketPlayer.C04PacketPlayerPosition
                        || matrixC18.isToggled() && packet instanceof C18PacketSpectate
                        || matrixC03.isToggled() && packet instanceof C03PacketPlayer);
            default:
                return true;
        }
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (!(packet instanceof S08PacketPlayerPosLook)) {
            return true;
        }
        if (current == Mode.Verus && verusMovement.isToggled()) {
            boolean cancel = this.teleported;
            this.teleported = false;
            return !cancel;
        }
        return !(current == Mode.OldMatrix && matrixS08.isToggled());
    }
}
