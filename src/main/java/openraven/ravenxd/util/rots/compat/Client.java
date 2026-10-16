package openraven.ravenxd.util.rots.compat;

import openraven.ravenxd.util.Rots;
import openraven.ravenxd.util.rots.RotationManager;

// i like skidding
public final class Client {
    private static final Client INSTANCE = new Client();
    private final MethodEventShim shim = new MethodEventShim();

    public static Client getInstance() {
        return INSTANCE;
    }

    public RotationManager getRotman() {
        return Rots.manager;
    }

    public MethodEventShim getShim() {
        return this.shim;
    }
}
