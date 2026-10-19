package openraven.ravenxd.jade.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public final class PacketUtils {
    private PacketUtils() {
    }

    public static void sendSilently(Packet<?> packet) {
        openraven.ravenxd.util.PacketUtils.sendNoEvent(packet);
    }

    @SuppressWarnings("unchecked")
    public static void processInboundPacket(Packet<?> packet) {
        if (packet instanceof S12PacketEntityVelocity) {
            EventBus.post(new VelocityEvent((S12PacketEntityVelocity) packet));
        }
        try {
            ((Packet<INetHandlerPlayClient>) packet).processPacket(Minecraft.getMinecraft().getNetHandler());
        } catch (ThreadQuickExitException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
