package openraven.ravenxd.module.network;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;

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
