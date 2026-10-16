package openraven.ravenxd.client;

import openraven.ravenxd.module.SettingsModule;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;

public final class MiddleClickFriends {
    private MiddleClickFriends() {
    }

    public static void onMouse(MouseEvent event) {
        if (!event.buttonstate || event.button != 2 || !SettingsModule.middleClickFriends.isToggled()
                || openraven.ravenxd.module.ModuleManager.enabled("TargetStrafe") != null && openraven.ravenxd.module.movement.TargetStrafeModule.active()) {
            return;
        }
        EntityLivingBase e = BotCheck.lookingAt(100.0);
        if (BotCheck.isBot(e) || Friends.addFriend(e.getName())) {
            return;
        }
        Friends.removeFriend(e.getName());
    }
}
