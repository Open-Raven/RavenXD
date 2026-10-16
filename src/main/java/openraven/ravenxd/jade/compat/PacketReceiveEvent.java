package openraven.ravenxd.jade.compat;

import net.minecraft.network.Packet;

public class PacketReceiveEvent extends Event {
    private final Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> ys98() {
        return this.packet;
    }
}
