package openraven.ravenxd.jade.compat;

import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class VelocityEvent extends Event {
    public final S12PacketEntityVelocity s12PacketEntityVelocity;

    public VelocityEvent(S12PacketEntityVelocity packet) {
        this.s12PacketEntityVelocity = packet;
    }
}
