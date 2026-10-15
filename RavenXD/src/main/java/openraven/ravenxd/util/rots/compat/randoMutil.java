package openraven.ravenxd.util.rots.compat;

import java.util.concurrent.ThreadLocalRandom;

public final class randoMutil {
    private randoMutil() {
    }

    public static float nextFloat(float min, float max) {
        return min == max ? min : (float) ThreadLocalRandom.current().nextDouble(Math.min(min, max), Math.max(min, max));
    }
}
