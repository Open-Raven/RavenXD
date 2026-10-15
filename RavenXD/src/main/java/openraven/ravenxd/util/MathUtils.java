package xyz.daikirai.util;

import xyz.daikirai.module.setting.SliderSetting;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public final class MathUtils {
    public static final Random RANDOM = new Random();

    private MathUtils() {
    }

    public static int randomInt(int min, int max) {
        return RANDOM.nextInt(max - min + 1) + min;
    }

    public static float randomFloat(float min, float max) {
        return (float) ((double) min + (double) (max - min) * Math.random());
    }

    public static int randomRange(int min, int max) {
        return min + (int) ((double) (max - min + 1) * Math.random());
    }

    public static int timeDigits() {
        return (int) (System.currentTimeMillis() % 10000L);
    }

    public static double randomTimes(double d) {
        return d * Math.random();
    }

    public static int randomInt(int n) {
        return (int) ((double) (n + 1) * Math.random());
    }

    public static double randomBetween(double min, double max) {
        return min + (max - min) * Math.random();
    }

    public static double randomBetween(SliderSetting min, SliderSetting max) {
        return min.getInput() == max.getInput() ? min.getInput() : min.getInput() + RANDOM.nextDouble() * (max.getInput() - min.getInput());
    }

    public static double pingPong(double period) {
        double half = period / 2.0;
        return Math.abs((double) System.currentTimeMillis() % period - half) / half;
    }

    public static int wrapPrintable(int n) {
        if (n > 126) {
            n = 32 + (n - 126) - 1;
        } else if (n < 32) {
            n = 126 - (32 - n) + 1;
        }
        return n;
    }

    public static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 65 + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 97 + 10;
        }
        return 0;
    }

    public static char hexDigit(int n) {
        if (n < 10) {
            return (char) (48 + n);
        }
        return (char) (65 + (n - 10));
    }

    public static <T> void dedupe(List<T> list) {
        HashSet<T> set = new HashSet<T>(list);
        list.clear();
        list.addAll(set);
    }

    public static String randomString(int n) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            sb.append(chars[RANDOM.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    public static String stripDashes(String s) {
        return s.replace("-", "");
    }

    public static String stripNamespace(String s) {
        return s.replaceFirst("minecraft:", "");
    }

    public static String bool(boolean b) {
        return b ? "&aTRUE" : "&cFALSE";
    }
}
