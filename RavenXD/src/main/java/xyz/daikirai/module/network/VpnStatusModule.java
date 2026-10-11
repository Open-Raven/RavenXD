package xyz.daikirai.module.network;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;

public class VpnStatusModule extends Module {
    public VpnStatusModule(int key) {
        super("VPN Status", Category.network, key);
    }

    @Override
    public boolean canBeEnabled() {
        Chat.moduleMessage(this, "&cThere was an error, try relaunching.");
        return false;
    }
}
