package xyz.daikirai.module.network;

import xyz.daikirai.client.Chat;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class LimboModule extends Module {
    public static LimboModule instance;
    public static ButtonSetting lock;

    public LimboModule(int key) {
        super("Limbo", Category.network, key);
        instance = this;
    }

    public void link() {
        lock = (ButtonSetting) ModuleManager.setting(this, "Lock");
    }

    @Override
    public boolean canBeEnabled() {
        if (lock.enabled) {
            Chat.moduleMessage(this, "&cDisable 'Lock' to enable.");
            return false;
        }
        if (!Hypixel.onHypixel()) {
            Chat.moduleMessage(this, "&cMust be on Hypixel.");
            return false;
        }
        return true;
    }

    @Override
    public void onPreUpdate() {
        PacketUtils.sendThroughLag(new C08PacketPlayerBlockPlacement());
        instance.disable();
    }
}
