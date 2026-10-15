package xyz.daikirai.util;

import xyz.daikirai.module.setting.SliderSetting;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public enum Theme {
    DEFAULT("Default", true, false),
    RAINBOW("Rainbow", false, true),
    AURORA("Aurora", new Color(120, 0, 200), new Color(0, 255, 180)),
    CHERRY("Cherry", new Color(255, 200, 200), new Color(243, 58, 106)),
    COTTON_CANDY("Cotton candy", new Color(99, 249, 255), new Color(255, 104, 204)),
    FLARE("Flare", new Color(241, 39, 17), new Color(245, 175, 25)),
    FLOWER("Flower", new Color(215, 166, 231), new Color(211, 90, 232)),
    FOREST("Forest", new Color(34, 139, 34), new Color(154, 205, 50)),
    FROST("Frost", new Color(253, 253, 253), new Color(210, 225, 230)),
    GOLD("Gold", new Color(255, 250, 0), new Color(255, 253, 195)),
    GRAY("Grayscale", new Color(240, 240, 240), new Color(110, 110, 110)),
    INFERNO("Inferno", new Color(255, 51, 0), new Color(50, 0, 0)),
    ROYAL("Royal", new Color(125, 204, 241), new Color(30, 71, 170)),
    SANDSTORM("Sandstorm", new Color(194, 178, 128), new Color(255, 236, 179)),
    SKY("Sky", new Color(140, 254, 250), new Color(14, 210, 247)),
    VINE("Vine", new Color(0, 255, 25), new Color(180, 255, 180));

    public static SliderSetting timeMultiplier;

    public static Theme defaultTheme = FROST;

    public static final Theme[] SELECTABLE = Arrays.stream(values()).filter(t -> t != DEFAULT).toArray(Theme[]::new);
    public static final String[] NAMES = Arrays.stream(values()).map(t -> t.name).toArray(String[]::new);
    public static final String[] SELECTABLE_NAMES = Arrays.stream(SELECTABLE).map(t -> t.name).toArray(String[]::new);
    public static final Map<String, Theme> BY_NAME = new LinkedHashMap<>();

    static {
        for (Theme t : values()) BY_NAME.put(t.name, t);
    }

    public final String name;
    public final boolean followsDefault;
    public final boolean rainbow;
    public final Color first;
    public final Color second;
    private final int[] rgbArr;

    Theme(String name, boolean followsDefault, boolean rainbow) {
        this.name = name;
        this.followsDefault = followsDefault;
        this.rainbow = rainbow;
        this.first = null;
        this.second = null;
        this.rgbArr = null;
    }

    Theme(String name, Color first, Color second) {
        this.name = name;
        this.followsDefault = false;
        this.rainbow = false;
        this.first = first;
        this.second = second;
        this.rgbArr = new int[]{first.getRGB(), second.getRGB()};
    }

    public int getRGB(double d) {
        if (this.followsDefault) return defaultTheme.getRGB(d);
        return gradient(this.rainbow, this.first, this.second, d);
    }

    public int[] getRGBArr() {
        if (this.followsDefault) return defaultTheme.getRGBArr();
        if (this.rainbow) {
            int n = RenderUtils.rainbow(2L);
            return new int[]{n, n};
        }
        return this.rgbArr;
    }

    public static int gradient(boolean rainbow, Color color, Color color2, double d) {
        double mult = timeMultiplier == null ? 0.5 : timeMultiplier.getInput();
        if (rainbow) {
            long l = (long) Math.max(2.0 * mult, 1.0);
            return RenderUtils.rainbow(l, (long) d);
        }
        double d3 = (double) System.currentTimeMillis() / 1.0E8 * mult * 400000.0;
        double d4 = (Math.sin(d3 + d * (double) 0.55f) + 1.0) * 0.5;
        return RenderUtils.blend(color, color2, d4);
    }
}
