package openraven.ravenxd.module.other;

import openraven.ravenxd.module.setting.ButtonSetting;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CustomPacketFilterTest {
    private final Map<String, ButtonSetting> settings = new HashMap<>();
    private final CustomPacketFilter filter = new CustomPacketFilter(name -> {
        ButtonSetting setting = new ButtonSetting(name, false);
        settings.put(name, setting);
        return setting;
    });

    @Test void allPacketsPassWithDefaultSettings() {
        assertTrue(filter.allows(new C00PacketKeepAlive()));
        assertTrue(filter.allows(new C03PacketPlayer()));
        assertTrue(filter.allows(new C03PacketPlayer.C04PacketPlayerPosition()));
    }

    @Test void baseMovementToggleDoesNotCancelItsVariants() {
        settings.get("C03PacketPlayer").setEnabled(true);
        assertFalse(filter.allows(new C03PacketPlayer()));
        assertTrue(filter.allows(new C03PacketPlayer.C04PacketPlayerPosition()));
        assertTrue(filter.allows(new C03PacketPlayer.C05PacketPlayerLook()));
        assertTrue(filter.allows(new C03PacketPlayer.C06PacketPlayerPosLook()));
    }

    @Test void variantAndKeepaliveTogglesAreIndependentAndLive() {
        settings.get("C04PacketPlayerPosition").setEnabled(true);
        settings.get("C00PacketKeepAlive").setEnabled(true);
        assertFalse(filter.allows(new C03PacketPlayer.C04PacketPlayerPosition()));
        assertFalse(filter.allows(new C00PacketKeepAlive()));
        assertTrue(filter.allows(new C03PacketPlayer()));
        assertTrue(filter.allows(new C03PacketPlayer.C06PacketPlayerPosLook()));
        assertTrue(filter.allows(new C01PacketChatMessage("hello")));
        settings.get("C04PacketPlayerPosition").setEnabled(false);
        assertTrue(filter.allows(new C03PacketPlayer.C04PacketPlayerPosition()));
    }
}
