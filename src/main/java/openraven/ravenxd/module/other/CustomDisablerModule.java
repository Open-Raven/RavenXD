package openraven.ravenxd.module.other;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;

import java.util.HashMap;
import java.util.Map;

// raven bplusplusplus disabler but less fucked
public class CustomDisablerModule extends Module {
    private static final Object[][] PACKETS = {
            {"C00PacketKeepAlive", C00PacketKeepAlive.class},
            {"C02PacketUseEntity", C02PacketUseEntity.class},
            {"C03PacketPlayer", C03PacketPlayer.class},
            {"C04PacketPlayerPosition", C03PacketPlayer.C04PacketPlayerPosition.class},
            {"C05PacketPlayerLook", C03PacketPlayer.C05PacketPlayerLook.class},
            {"C06PacketPlayerPosLook", C03PacketPlayer.C06PacketPlayerPosLook.class},
            {"C07PacketPlayerDigging", C07PacketPlayerDigging.class},
            {"C08PlayerBlockPlacement", C08PacketPlayerBlockPlacement.class},
            {"C09PacketHeldItemChange", C09PacketHeldItemChange.class},
            {"C0APacketAnimation", C0APacketAnimation.class},
            {"C0BPacketEntityAction", C0BPacketEntityAction.class},
            {"C0CPacketInput", C0CPacketInput.class},
            {"C0DPacketCloseWindow", C0DPacketCloseWindow.class},
            {"C0EPacketClickWindow", C0EPacketClickWindow.class},
            {"C0FConfirmTransaction", C0FPacketConfirmTransaction.class},
            {"C10PCreativeInventory", C10PacketCreativeInventoryAction.class},
            {"C11PacketEnchantItem", C11PacketEnchantItem.class},
            {"C12PacketUpdateSign", C12PacketUpdateSign.class},
            {"C13PacketPlayerAbilities", C13PacketPlayerAbilities.class},
            {"C14PacketTabComplete", C14PacketTabComplete.class},
            {"C15PacketClientSettings", C15PacketClientSettings.class},
            {"C16PacketClientStatus", C16PacketClientStatus.class},
            {"C17PacketCustomPayload", C17PacketCustomPayload.class},
            {"C18PacketSpectate", C18PacketSpectate.class},
            {"C19PacketPackStatus", C19PacketResourcePackStatus.class},
    };

    private final Map<Class<?>, ButtonSetting> toggles = new HashMap<Class<?>, ButtonSetting>();

    public CustomDisablerModule(int key) {
        super("Custom Disabler", Category.other, key);
    }

    public void link() {
        for (Object[] p : PACKETS) {
            this.toggles.put((Class<?>) p[1], (ButtonSetting) ModuleManager.setting(this, (String) p[0]));
        }
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        // fix c03 doing c04-c06 too
        ButtonSetting b = this.toggles.get(packet.getClass());
        return b == null || !b.isToggled();
    }
}
