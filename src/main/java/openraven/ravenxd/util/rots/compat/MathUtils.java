package openraven.ravenxd.util.rots.compat;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
    public static final double TO_DEGREES = 180.0 / Math.PI;
    public static final double RISETO_DEGREES = 180.0 / Math.PI;

    private MathUtils() {
    }

    public static double getRandom(double min, double max) {
        return min == max ? min : ThreadLocalRandom.current().nextDouble(Math.min(min, max), Math.max(min, max));
    }

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }
}
