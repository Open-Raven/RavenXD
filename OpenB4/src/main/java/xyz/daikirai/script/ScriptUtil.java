package xyz.daikirai.script;

import com.google.gson.JsonObject;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;

public final class ScriptUtil implements Defaults {
    public static final Logger LOGGER = LogManager.getLogger();

    private ScriptUtil() {
    }

    public static String stripNamespace(String string) {
        return string.replaceFirst("minecraft:", "");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T enumByName(Class<T> clazz, String string) {
        Enum<?>[] values = (Enum<?>[]) clazz.getEnumConstants();
        for (Enum<?> e : values) {
            if (!e.name().equals(string)) continue;
            return (T) e;
        }
        return null;
    }

    public static Vec3 lookVec(float f, float f2) {
        float f3 = MathHelper.cos((f *= -((float) Math.PI / 180)) - (float) Math.PI);
        float f4 = MathHelper.sin(f - (float) Math.PI);
        float f5 = -MathHelper.cos(f2 *= -((float) Math.PI / 180));
        float f6 = MathHelper.sin(f2);
        return new Vec3((double) (f4 * f5), (double) f6, (double) (f3 * f5));
    }

    public static boolean inGame() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void dedupe(ArrayList arrayList) {
        HashSet hashSet = new HashSet(arrayList);
        arrayList.clear();
        arrayList.addAll(hashSet);
    }

    public static int randomInt(int n, int n2) {
        return (int) (Math.random() * (double) (n2 - n + 1)) + n;
    }

    public static double round(double d, int n) {
        if (n == 0) {
            return Math.round(d);
        }
        double d2 = Math.pow(10.0, n);
        return (double) Math.round(d * d2) / d2;
    }

    public static double randomDouble(double d, double d2) {
        return d + (d2 - d) * Math.random();
    }

    public static String jsonString(JsonObject jsonObject, String string) {
        try {
            return jsonObject.get(string).getAsString();
        } catch (Exception exception) {
            return "";
        }
    }
}
