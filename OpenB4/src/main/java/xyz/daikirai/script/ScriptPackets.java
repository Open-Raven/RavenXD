package xyz.daikirai.script;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;

public class ScriptPackets extends ScriptDefaults {

    public static ScriptDefaults.CPacket wrapSent(Packet packet) {
        ScriptDefaults.CPacket cPacket;
        try {
            cPacket = packet instanceof C0APacketAnimation ? new ScriptDefaults.C0A((byte) 0, (C0APacketAnimation) packet)
                    : (packet instanceof C0BPacketEntityAction ? new ScriptDefaults.C0B((byte) 0, (C0BPacketEntityAction) packet)
                    : (packet instanceof C0DPacketCloseWindow ? new ScriptDefaults.C0D((byte) 0, (C0DPacketCloseWindow) packet)
                    : (packet instanceof C0EPacketClickWindow ? new ScriptDefaults.C0E((byte) 0, (C0EPacketClickWindow) packet)
                    : (packet instanceof C0FPacketConfirmTransaction ? new ScriptDefaults.C0F((byte) 0, (C0FPacketConfirmTransaction) packet)
                    : (packet instanceof C01PacketChatMessage ? new ScriptDefaults.C01((byte) 0, (C01PacketChatMessage) packet)
                    : (packet instanceof C02PacketUseEntity ? new ScriptDefaults.C02((byte) 0, (C02PacketUseEntity) packet)
                    : (packet instanceof C03PacketPlayer ? new ScriptDefaults.C03((byte) 0, (C03PacketPlayer) packet)
                    : (packet instanceof C07PacketPlayerDigging ? new ScriptDefaults.C07((byte) 0, (C07PacketPlayerDigging) packet)
                    : (packet instanceof C08PacketPlayerBlockPlacement ? new ScriptDefaults.C08((byte) 0, (C08PacketPlayerBlockPlacement) packet)
                    : (packet instanceof C09PacketHeldItemChange ? new ScriptDefaults.C09((byte) 0, (C09PacketHeldItemChange) packet)
                    : (packet instanceof C10PacketCreativeInventoryAction ? new ScriptDefaults.C10((byte) 0, (C10PacketCreativeInventoryAction) packet)
                    : (packet instanceof C13PacketPlayerAbilities ? new ScriptDefaults.C13((byte) 0, (C13PacketPlayerAbilities) packet)
                    : (packet instanceof C16PacketClientStatus ? new ScriptDefaults.C16((byte) 0, (C16PacketClientStatus) packet)
                    : new ScriptDefaults.CPacket(packet))))))))))))));
        } catch (Exception exception) {
            cPacket = null;
        }
        return cPacket;
    }

    public static ScriptDefaults.SPacket wrapReceived(Packet packet) {
        ScriptDefaults.SPacket sPacket;
        try {
            sPacket = packet instanceof S06PacketUpdateHealth ? new ScriptDefaults.S06((S06PacketUpdateHealth) packet)
                    : (packet instanceof S08PacketPlayerPosLook ? new ScriptDefaults.S08((S08PacketPlayerPosLook) packet)
                    : (packet instanceof S09PacketHeldItemChange ? new ScriptDefaults.S09((S09PacketHeldItemChange) packet)
                    : (packet instanceof S12PacketEntityVelocity ? new ScriptDefaults.S12((S12PacketEntityVelocity) packet)
                    : (packet instanceof S23PacketBlockChange ? new ScriptDefaults.S23((S23PacketBlockChange) packet)
                    : (packet instanceof S27PacketExplosion ? new ScriptDefaults.S27((S27PacketExplosion) packet)
                    : (packet instanceof S29PacketSoundEffect ? new ScriptDefaults.S29((S29PacketSoundEffect) packet)
                    : (packet instanceof S2APacketParticles ? new ScriptDefaults.S2A((S2APacketParticles) packet)
                    : (packet instanceof S3EPacketTeams ? new ScriptDefaults.S3E((S3EPacketTeams) packet)
                    : new ScriptDefaults.SPacket(packet)))))))));
        } catch (Exception exception) {
            sPacket = null;
        }
        return sPacket;
    }
}
