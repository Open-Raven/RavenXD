package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.module.NPlusThemeModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

// nplus utils port
public final class NPlusRender {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<String, ResourceLocation> ICONS = new HashMap<String, ResourceLocation>();

    public static final Color clientColor = new Color(5, 145, 115);
    public static final int clientInt = clientColor.getRGB();
    public static final int clientIntDarker = clientColor.darker().getRGB();
    public static final int purple = new Color(154, 2, 255).getRGB();
    public static final int gray = new Color(104, 104, 104).getRGB();
    public static final int lightRed = new Color(209, 102, 102).getRGB();
    public static final int modComp4 = -12302777;
    public static final int modComp5 = -12829381;
    public static final int categoryFill = -552266475;
    public static final int command1 = -13813950;
    public static final int command3 = new Color(51, 51, 51).getRGB();
    public static final int babyBlue = new Color(79, 104, 158).getRGB();
    public static final int intMin = Integer.MIN_VALUE;
    public static final int intMinDarker = -1056964608;

    private NPlusRender() {
    }

    private static NPlusFont font() {
        return NPlusThemeModule.font() ? NPlusFont.get(20) : null;
    }

    public static FontRenderer mcFont() {
        return mc.fontRendererObj;
    }

    // comfortaa by default
    public static void text(String s, float x, float y, int color, boolean shadow) {
        NPlusFont f = font();
        if (f != null) {
            f.drawString(s, x, y, color, shadow);
        } else {
            mc.fontRendererObj.drawString(s, x, y, color, shadow);
        }
    }

    public static int width(String s) {
        NPlusFont f = font();
        return f != null ? (int) f.getStringWidth(s) : mc.fontRendererObj.getStringWidth(s);
    }

    public static int height() {
        NPlusFont f = font();
        return f != null ? (int) f.getHeight() : mc.fontRendererObj.FONT_HEIGHT;
    }

    public static void underlined(String s, int x, int y, int expand, int color) {
        text(s, x, y, color, true);
        Gui.drawRect(x - expand, y + mc.fontRendererObj.FONT_HEIGHT + 1, x + width(s) + expand, y + mc.fontRendererObj.FONT_HEIGHT, color);
    }

    public static int rainbow(long speed, long delay) {
        long time = System.currentTimeMillis() + delay;
        return Color.getHSBColor((float) (time % (15000L / speed)) / (15000.0f / (float) speed), 1.0f, 1.0f).getRGB();
    }

    public static int astolfo(int yOffset, int yTotal, float speed) {
        float hue = (float) (System.currentTimeMillis() % (long) (int) speed) + (float) ((yTotal - yOffset) * 9);
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5f) {
            hue = 0.5f - (hue - 0.5f);
        }
        return Color.HSBtoRGB(hue + 0.5f, 0.5f, 1.0f);
    }

    public static int astolfo(int yOffset, int yTotal) {
        return astolfo(yOffset, yTotal, 2900.0f);
    }

    public static int zeroDay(long index) {
        float s = 4000.0f;
        float hue = (float) ((System.currentTimeMillis() + index) % (long) s) / s;
        return Color.getHSBColor(hue, 0.8f, 0.8f).getRGB();
    }

    public static void color(int c) {
        GL11.glColor4f((c >> 16 & 0xFF) / 255.0f, (c >> 8 & 0xFF) / 255.0f, (c & 0xFF) / 255.0f, (c >> 24 & 0xFF) / 255.0f);
    }

    private static void corners(float x, float y, float xEnd, float yEnd, float r) {
        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2d(x + r + Math.sin(Math.toRadians(i)) * -r, y + r + Math.cos(Math.toRadians(i)) * -r);
        }
        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2d(x + r + Math.sin(Math.toRadians(i)) * -r, yEnd - r + Math.cos(Math.toRadians(i)) * -r);
        }
        for (int i = 0; i <= 90; i += 3) {
            GL11.glVertex2d(xEnd - r + Math.sin(Math.toRadians(i)) * r, yEnd - r + Math.cos(Math.toRadians(i)) * r);
        }
        for (int i = 90; i <= 180; i += 3) {
            GL11.glVertex2d(xEnd - r + Math.sin(Math.toRadians(i)) * r, y + r + Math.cos(Math.toRadians(i)) * r);
        }
    }

    public static void roundedRect(float x, float y, float xEnd, float yEnd, float radius, int c) {
        shape(x, y, xEnd, yEnd, radius, 0.0f, c, 9);
    }

    public static void roundedOutline(float x, float y, float xEnd, float yEnd, float radius, float lineWidth, int c) {
        shape(x, y, xEnd, yEnd, radius, lineWidth, c, 2);
    }

    private static void shape(float x, float y, float xEnd, float yEnd, float radius, float lineWidth, int c, int mode) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(2848);
        if (lineWidth > 0.0f) {
            GL11.glLineWidth(lineWidth);
        }
        GlStateManager.resetColor();
        color(c);
        GL11.glBegin(mode);
        corners(x * 2.0f, y * 2.0f, xEnd * 2.0f, yEnd * 2.0f, radius);
        GL11.glEnd();
        GL11.glDisable(2848);
        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
        GlStateManager.resetColor();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void borderedRect(float x, float y, float xEnd, float yEnd, float w, int c) {
        rect(x + w, y, xEnd - w, y + w, c);
        rect(x, y, x + w, yEnd, c);
        rect(xEnd - w, y, xEnd, yEnd, c);
        rect(x + w, yEnd - w, xEnd - w, yEnd, c);
    }

    public static void rect(float x, float y, float xEnd, float yEnd, int c) {
        openraven.ravenxd.util.RenderUtils.rect(x, y, xEnd, yEnd, c);
    }

    // gui theme gradient
    public static void moduleGradient(float x, float y, float xEnd, float yEnd, int top, int bottom) {
        int theme = (int) NPlusThemeModule.theme.getInput() + 1;
        if (theme != 1 && theme != 2) {
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(7425);
        GlStateManager.resetColor();
        GL11.glBegin(7);
        themeColor(top, theme);
        GL11.glVertex2f(x, yEnd);
        GL11.glVertex2f(xEnd, yEnd);
        themeColor(bottom, theme);
        GL11.glVertex2f(xEnd, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        GL11.glShadeModel(7424);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void themeColor(int h, int theme) {
        float a = (float) (h >> 14 & 0xFF) / 255.0f;
        float r, g;
        if (theme == 1) {
            r = (float) (h >> 5 & 0xFF) / 255.0f;
            g = (float) (h >> 5 & 0xFF) / 2155.0f;
        } else {
            r = (float) (h >> 5 & 0xFF) / 2155.0f;
            g = (float) (h >> 5 & 0xFF) / 255.0f;
        }
        GL11.glColor4f(r, g, Math.min(1.0f, (float) (h & 0xFF)), a);
    }

    public static void icon(String name, int x, int y, int w, int h, Color c) {
        ResourceLocation res = ICONS.get(name);
        if (res == null) {
            ICONS.put(name, res = new ResourceLocation("ravenxd", "nplus/icon/" + name + ".png"));
        }
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.color(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, c.getAlpha() / 255.0f);
        mc.getTextureManager().bindTexture(res);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0f, 0.0f, w, h, (float) w, (float) h);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // vape slider
    public static void rectWithCircle(int x, int y, int xEnd, int yEnd, int size, int c) {
        Color cl = new Color(c);
        if (xEnd - 5 > x) {
            roundedRect(x - 1, y - 0.4f, xEnd - 5, yEnd - 2.1f, 2.5f, cl.darker().darker().getRGB());
        }
        if (size == 4) {
            icon("circle", xEnd - 3, y - 1, size, size, cl);
        } else {
            icon("circle", xEnd - 4, (int) (y - 1.3f), size, size, cl);
        }
    }

    public static void checkBox(int x, int y, int s, boolean on) {
        icon("circle", x, y, s, s, on ? clientColor : new Color(lightRed));
    }
}
