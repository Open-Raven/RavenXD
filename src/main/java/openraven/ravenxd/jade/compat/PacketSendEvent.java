package openraven.ravenxd.jade.compat;

import net.minecraft.network.Packet;

public class PacketSendEvent extends Event {
    private final Packet<?> packet;

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }
    public Packet<?> ys98() {
        return this.packet;
    }
}
