package openraven.ravenxd.module.dev;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.util.ItemUtils;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.Strings;
import net.minecraft.entity.Entity;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewPacketsModule extends Module {
    public static ButtonSetting includeCancelled, logsOnly, roundValues, sent, ignoreC00, ignoreC03, compactC03, ignoreC08, ignoreC0A,
            ignoreC0F, received;

    private static final Logger LOGGER = LogManager.getLogger();
    public static Packet<?> last;
    public static long ticks;

    public ViewPacketsModule(int key) {
        super("View Packets", Category.other, key);
    }

    @Override
    public void link() {
        includeCancelled = (ButtonSetting) ModuleManager.setting(this, "Include cancelled");
        logsOnly = (ButtonSetting) ModuleManager.setting(this, "Logs only");
        roundValues = (ButtonSetting) ModuleManager.setting(this, "Round values");
        sent = (ButtonSetting) ModuleManager.setting(this, "Sent");
        ignoreC00 = (ButtonSetting) ModuleManager.setting(this, "Ignore C00");
        ignoreC03 = (ButtonSetting) ModuleManager.setting(this, "Ignore C03");
        compactC03 = (ButtonSetting) ModuleManager.setting(this, "Compact C03");
        ignoreC08 = (ButtonSetting) ModuleManager.setting(this, "Ignore C08");
        ignoreC0A = (ButtonSetting) ModuleManager.setting(this, "Ignore C0A");
        ignoreC0F = (ButtonSetting) ModuleManager.setting(this, "Ignore C0F");
        received = (ButtonSetting) ModuleManager.setting(this, "Received");
    }

    public static void onSent(Packet<?> packet) {
        if (mc.isSingleplayer()) {
            if (packet.getClass().getSimpleName().charAt(0) == 'S') {
                return;
            }
            if (packet instanceof FMLProxyPacket) {
                return;
            }
        }
        if (ignoreC00.isToggled() && packet instanceof C00PacketKeepAlive) {
            return;
        }
        if (ignoreC0A.isToggled() && packet instanceof C0APacketAnimation) {
            return;
        }
        if (ignoreC08.isToggled() && packet instanceof C08PacketPlayerBlockPlacement) {
            return;
        }
        if (ignoreC0F.isToggled() && packet instanceof C0FPacketConfirmTransaction) {
            return;
        }
        if (packet instanceof C03PacketPlayer) {
            if (ignoreC03.isToggled()) {
                return;
            }
            if (compactC03.isToggled() && (last == null || last instanceof C03PacketPlayer)) {
                return;
            }
        }
        last = packet;
        show(last, true);
    }

    public static void onReceived(Packet<?> packet) {
        if (mc.isSingleplayer() && packet.getClass().getSimpleName().charAt(0) == 'C') {
            return;
        }
        show(packet, false);
    }

    public static double round(double d) {
        return roundValues.isToggled() ? Strings.round(d, 3) : d;
    }

    private static String bool(boolean b) {
        return (b ? "&atrue" : "&cfalse") + "&r";
    }

    private static String pos(BlockPos pos) {
        return pos == null ? "null" : pos.getX() + "&7, &b" + pos.getY() + "&7, &b" + pos.getZ();
    }

    public static String receivedInfo(Packet<?> packet) {
        String s = "&a" + packet.getClass().getSimpleName();
        if (packet instanceof S0BPacketAnimation) {
            Entity entity = mc.theWorld.getEntityByID(((S0BPacketAnimation) packet).getEntityID());
            s = s + "\n&7Target: &b" + (entity == null ? "null" : entity.getName());
            s = s + "\n&7Animation: &b" + ((S0BPacketAnimation) packet).getAnimationType();
        }
        return s;
    }

    public static String sentInfo(Packet<?> packet) {
        String s = (PacketUtils.sending == packet ? "&c" : "&a") + packet.getClass().getSimpleName();
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging p = (C07PacketPlayerDigging) packet;
            s = s + "\n&7Status: &b" + (p.getStatus() == null ? "null" : p.getStatus().name());
            s = s + "\n&7Facing: &b" + (p.getFacing() == null ? "null" : p.getFacing().ordinal() + " " + p.getFacing().name().toLowerCase());
            s = s + "\n&7Position: &b" + pos(p.getPosition());
        } else if (packet instanceof C09PacketHeldItemChange) {
            s = s + "\n&7Swap to slot: &b" + ((C09PacketHeldItemChange) packet).getSlotId();
        } else if (packet instanceof C0BPacketEntityAction) {
            s = s + "\n&7Action: &b" + ((C0BPacketEntityAction) packet).getAction().name();
            s = s + "\n&7Aux data: &b" + ((C0BPacketEntityAction) packet).getAuxData();
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
            s = s + "\n&7Item: &b" + ItemUtils.name(p.getStack());
            int n = p.getPlacedBlockDirection();
            s = s + "\n&7Direction: &b" + n + (n != 255 ? " " + EnumFacing.getFront(n) : "");
            s = s + "\n&7Position: &b" + pos(p.getPosition());
            s = s + "\n&7Offset: &b" + round(p.getPlacedBlockOffsetX()) + "&7, &b" + round(p.getPlacedBlockOffsetY()) + "&7, &b" + round(p.getPlacedBlockOffsetZ());
        } else if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity p = (C02PacketUseEntity) packet;
            s = s + "\n&7Action: &b" + p.getAction().name();
            Entity entity = p.getEntityFromWorld(mc.theWorld);
            s = s + "\n&7Target: &b" + (entity == null ? "null" : entity.getName() + " " + entity.getEntityId());
            Vec3 v = p.getHitVec();
            s = v == null ? s + "\n&7Hit vec: &bnull" : s + "\n&7Hit vec: &b" + round(v.xCoord) + "&7, &b" + round(v.yCoord) + "&7, &b" + round(v.zCoord);
        } else if (packet instanceof C01PacketChatMessage) {
            s = s + "\n&7Length: &b" + ((C01PacketChatMessage) packet).getMessage().length();
        } else if (packet instanceof C14PacketTabComplete) {
            s = s + "\n&7Message: &b" + ((C14PacketTabComplete) packet).getMessage();
            s = s + "\n&7Target block: &b" + pos(((C14PacketTabComplete) packet).getTargetBlock());
        } else if (packet instanceof C17PacketCustomPayload) {
            s = s + "\n&7Channel: &b" + ((C17PacketCustomPayload) packet).getChannelName();
        } else if (packet instanceof C15PacketClientSettings) {
            s = s + "\n&7Language: &b" + ((C15PacketClientSettings) packet).getLang();
            s = s + "\n&7Chat visibility: &b" + ((C15PacketClientSettings) packet).getChatVisibility().name();
        } else if (packet instanceof C00PacketKeepAlive) {
            s = s + "\n&7Key: &b" + ((C00PacketKeepAlive) packet).getKey();
        } else if (packet instanceof C16PacketClientStatus) {
            s = s + "\n&7Status: &b" + ((C16PacketClientStatus) packet).getStatus().name();
        } else if (packet instanceof C10PacketCreativeInventoryAction) {
            s = s + "\n&7Slot: &b" + ((C10PacketCreativeInventoryAction) packet).getSlotId();
            s = s + "\n&7Item: &b" + ItemUtils.name(((C10PacketCreativeInventoryAction) packet).getStack());
        } else if (packet instanceof C0DPacketCloseWindow) {
            String window = "error";
            try {
                window = String.valueOf((int) (Integer) ReflectionHelper.getPrivateValue(C0DPacketCloseWindow.class, (C0DPacketCloseWindow) packet, "windowId", "field_149556_a"));
            } catch (Exception ignored) {
            }
            s = s + "\n&7Window: &b" + window;
        } else if (packet instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow p = (C0EPacketClickWindow) packet;
            s = s + "\n&7Window: &b" + p.getWindowId();
            s = s + "\n&7Slot: &b" + p.getSlotId();
            s = s + "\n&7Button: &b" + p.getUsedButton();
            s = s + "\n&7Mode: &b" + p.getMode();
            s = s + "\n&7Action: &b" + p.getActionNumber();
            s = s + "\n&7Item: &b" + ItemUtils.name(p.getClickedItem());
        } else if (packet instanceof C0FPacketConfirmTransaction) {
            s = s + "\n&7Window: &b" + ((C0FPacketConfirmTransaction) packet).getWindowId();
            s = s + "\n&7Uid: &b" + ((C0FPacketConfirmTransaction) packet).getUid();
        } else if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer p = (C03PacketPlayer) packet;
            s = s + "\n&7X: &b" + p.getPositionX();
            s = s + "\n&7Y: &b" + p.getPositionY();
            s = s + "\n&7Z: &b" + p.getPositionZ();
            s = s + "\n&7Yaw: &b" + p.getYaw();
            s = s + "\n&7Pitch: &b" + p.getPitch();
            s = s + "\n&7Ground: " + bool(p.isOnGround());
            s = s + "\n&7Moving: " + bool(p.isMoving());
            s = s + "\n&7Rotating: " + bool(p.getRotating());
        }
        s = s + "\n&7Client tick: &e" + ticks + " " + System.currentTimeMillis() % 1000000L;
        return s;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            ++ticks;
        }
    }

    public static void show(Packet<?> packet, boolean isSent) {
        String head = "&7" + (isSent ? "Sent" : "Received") + " packet (t:&b" + ticks + " " + (int) (System.currentTimeMillis() % 10000L) + "&7): ";
        String details = isSent ? sentInfo(packet) : receivedInfo(packet);
        String name = (packet instanceof C03PacketPlayer ? "&6" : "&d") + packet.getClass().getSimpleName();
        ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + head));
        if (!Chat.inWorld() || logsOnly.isToggled()) {
            String[] lines = details.split("\n");
            for (int i = 0; i < lines.length; ++i) {
                String l = Chat.strip(Chat.color(lines[i]));
                if (i == 0) {
                    LOGGER.info(Chat.strip(Chat.color(head)) + l);
                    continue;
                }
                if (i == lines.length - 1) {
                    l = l + " " + (PacketUtils.sending == packet ? "Client" : "Vanilla");
                }
                LOGGER.info("  " + l);
            }
            return;
        }
        ChatStyle style = new ChatStyle();
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color(details))));
        line.appendSibling(new ChatComponentText(Chat.color(name)).setChatStyle(style));
        mc.thePlayer.addChatMessage(line);
    }

    @Override
    public void onDisable() {
        last = null;
        ticks = 0L;
    }
}
