package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public final class EspShaders {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Shader fill;
    private static Shader roundRect;
    private static Shader outline;

    private EspShaders() {
    }

    public static Shader fill() {
        if (fill == null) {
            fill = new Shader(ShaderSources.OUTLINE_MASK, ShaderSources.OUTLINE_CAPTURE_VERTEX);
        }
        return fill;
    }

    public static Shader roundRect() {
        if (roundRect == null) {
            roundRect = new Shader(ShaderSources.ELLIPSE);
        }
        return roundRect;
    }

    public static Shader outline() {
        if (outline == null) {
            outline = new Shader(ShaderSources.OUTLINE);
        }
        return outline;
    }

    public static boolean valid(Shader s) {
        return s.program > 0;
    }

    public static void useFill(int n) {
        Shader s = fill();
        s.use();
        s.uniformi("u_texture", 0);
        s.uniformf("u_color", (float) (n >> 16 & 0xFF) / 255.0f, (float) (n >> 8 & 0xFF) / 255.0f, (float) (n & 0xFF) / 255.0f);
        s.uniformf("u_alpha_threshold", 0.001f);
    }

    public static void roundRect(float x, float y, float x2, float y2, float radius, int n) {
        Shader s = roundRect();
        if (s.program == -1) {
            return;
        }
        s.use();
        float w = x2 - x;
        float h = y2 - y;
        s.uniformf("u_size", w, h);
        s.uniformf("u_radius", radius);
        float a = (float) (n >> 24 & 0xFF) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        s.uniformf("u_color", r, g, b, a);
        GlStateManager.color(r, g, b, a);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.0f);
        RenderUtils.quad(x, y, w, h);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        s.stop();
    }

    private static void fullscreenQuad() {
        ScaledResolution sr = new ScaledResolution(mc);
        double w = sr.getScaledWidth_double();
        double h = sr.getScaledHeight_double();
        GL11.glBegin(7);
        GL11.glTexCoord2d(0.0, 1.0);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2d(0.0, 0.0);
        GL11.glVertex2d(0.0, h);
        GL11.glTexCoord2d(1.0, 0.0);
        GL11.glVertex2d(w, h);
        GL11.glTexCoord2d(1.0, 1.0);
        GL11.glVertex2d(w, 0.0);
        GL11.glEnd();
    }

    public static void outline(Framebuffer fb, float radius, float threshold) {
        Shader s = outline();
        if (!valid(s)) {
            return;
        }
        s.use();
        s.uniformi("u_texture", 0);
        s.uniformf("u_texel_size", 1.0f / (float) fb.framebufferWidth, 1.0f / (float) fb.framebufferHeight);
        s.uniformf("u_radius", radius);
        s.uniformf("u_alpha_threshold", threshold);
        GlStateManager.bindTexture(fb.framebufferTexture);
        fullscreenQuad();
        GlStateManager.bindTexture(0);
        s.stop();
    }
}
