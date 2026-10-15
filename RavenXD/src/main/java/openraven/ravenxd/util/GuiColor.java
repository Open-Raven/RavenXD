package xyz.daikirai.util;

import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;

public class GuiColor {
    public int r, g, b, a;
    public float rf, gf, bf, af;
    public Color color;
    public int rgb;
    public boolean rainbow;
    public long speed;
    public long offset;

    public GuiColor(int r, int g, int b, int a) {
        this.r = r;
        this.rf = r / 255.0f;
        this.g = g;
        this.gf = g / 255.0f;
        this.b = b;
        this.bf = b / 255.0f;
        this.a = a;
        this.af = a / 255.0f;
        this.color = new Color(r, g, b, a);
        this.rgb = this.color.getRGB();
    }

    public GuiColor(long speed, long offset, int alpha) {
        this.rainbow = true;
        this.speed = speed;
        this.offset = offset;
        this.af = alpha / 255.0f;
    }

    public static void apply(GuiColor c) {
        if (c.rainbow) {
            float[] f = c.rainbowComponents();
            GlStateManager.color(f[0], f[1], f[2], f[3]);
        } else {
            GlStateManager.color(c.rf, c.gf, c.bf, c.af);
        }
    }

    public float[] rainbowComponents() {
        int n = this.offset == 0L ? RenderUtils.rainbow(this.speed) : RenderUtils.rainbow(this.speed, this.offset);
        return new float[]{(n >> 16 & 0xFF) / 255.0f, (n >> 8 & 0xFF) / 255.0f, (n & 0xFF) / 255.0f, this.af};
    }
}
