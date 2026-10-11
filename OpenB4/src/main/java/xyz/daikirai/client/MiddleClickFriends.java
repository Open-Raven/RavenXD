package xyz.daikirai.client;

import xyz.daikirai.module.SettingsModule;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;

public final class MiddleClickFriends {
    private MiddleClickFriends() {
    }

    public static void onMouse(MouseEvent event) {
        if (!event.buttonstate || event.button != 2 || !SettingsModule.middleClickFriends.isToggled()
                || xyz.daikirai.module.ModuleManager.enabled("TargetStrafe") != null && xyz.daikirai.module.movement.TargetStrafeModule.active()) {
            return;
        }
        EntityLivingBase e = BotCheck.lookingAt(100.0);
        if (BotCheck.isBot(e) || Friends.addFriend(e.getName())) {
            return;
        }
        Friends.removeFriend(e.getName());
    }
}
