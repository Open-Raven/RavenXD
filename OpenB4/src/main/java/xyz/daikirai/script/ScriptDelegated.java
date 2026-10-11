package xyz.daikirai.script;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;

public class ScriptDelegated extends ScriptDefaults {
    public static int m4fd81ab6c73e92;

    public static boolean m63b8ed2f901ca4(Packet packet) {
        ScriptDefaults.SPacket sPacket = ScriptPackets.wrapReceived(packet);
        if (sPacket != null) {
            if (ScriptProtected.invokeBoolean("onPacketReceived", sPacket) == 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean mf2946c1e83ab75(Packet packet) {
        ScriptDefaults.CPacket cPacket = ScriptPackets.wrapSent(packet);
        if (cPacket != null) {
            if (ScriptProtected.invokeBoolean("onPacketSent", cPacket) == 0) {
                return false;
            }
        }
        return true;
    }

    public static Object[] mc5810fa29d73e4(Object[] objectArray) {
        ScriptDefaults.PlayerState playerState = new ScriptDefaults.PlayerState(objectArray);
        ScriptProtected.invoke("onPreMotion", playerState);
        return playerState.asArray();
    }

    public static void m7a3f19c8e42bd6() {
        if (++m4fd81ab6c73e92 >= 800) {
            m4fd81ab6c73e92 = 0;
            ScriptDelegated.protected_resetCache(true);
        }
    }

    public static void m9e2c74a1fd5038(String string, EntityPlayer entityPlayer) {
        ScriptProtected.invoke("onBedWarsAlert", string, ScriptDefaults.Entity.protected_of((net.minecraft.entity.Entity) entityPlayer));
    }

    public static void ma047d95c3e68bf(String string, EntityPlayer entityPlayer) {
        ScriptProtected.invoke("onAntiCheatFlag", string, ScriptDefaults.Entity.protected_of((net.minecraft.entity.Entity) entityPlayer));
    }

    public static void m18de7b40ac926f(net.minecraft.entity.Entity entity) {
        if (entity == ScriptDelegated.mc.thePlayer) {
            ScriptDelegated.protected_resetCache(false);
        }
        ScriptProtected.invoke("onWorldJoin", ScriptDefaults.Entity.protected_of(entity));
    }
}
