package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class RenderUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static int scaledWidth, scaledWidthD, scaledHeight, scaledHeightD, scaleFactor = 1;

    public static final double[] SIN_0_90 = new double[31], COS_0_90 = new double[31];
    public static final double[] SIN_90_180 = new double[31], COS_90_180 = new double[31];

    static {
        for (int n = 0; n <= 90; n += 3) {
            double d = (float) n * ((float) Math.PI / 180);
            SIN_0_90[n / 3] = Math.sin(d);
            COS_0_90[n / 3] = Math.cos(d);
        }
        for (int n = 90; n <= 180; n += 3) {
            double d = (float) n * ((float) Math.PI / 180);
            SIN_90_180[(n - 90) / 3] = Math.sin(d);
            COS_90_180[(n - 90) / 3] = Math.cos(d);
        }
    }

    private RenderUtils() {
    }

    public static void updateResolution() {
        ScaledResolution sr = new ScaledResolution(mc);
        scaledWidth = sr.getScaledWidth();
        scaledWidthD = (int) sr.getScaledWidth_double();
        scaledHeight = sr.getScaledHeight();
        scaledHeightD = (int) sr.getScaledHeight_double();
        scaleFactor = sr.getScaleFactor();
    }

    public static int rainbow(long speed, long... offset) {
        long l2 = System.currentTimeMillis() + (offset.length > 0 ? offset[0] : 0L);
        return Color.getHSBColor((float) (l2 % (15000L / speed)) / (15000.0f / (float) speed), 0.75f, 1.0f).getRGB();
    }

    public static int blend(Color color, Color color2, double d) {
        int n = (int) (d * 255.0);
        int n2 = 255 - n;
        int r = color.getRed() * n + color2.getRed() * n2 >> 8;
        int g = color.getGreen() * n + color2.getGreen() * n2 >> 8;
        int b = color.getBlue() * n + color2.getBlue() * n2 >> 8;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int rgb, int alpha) {
        return rgb & 0xFFFFFF | alpha << 24;
    }

    public static int clampAlpha(int n) {
        if (n > 255) return 255;
        if (n < 4) return 4;
        return n;
    }

    public static String dots(long period) {
        StringBuilder sb = new StringBuilder();
        int n = (int) (System.currentTimeMillis() / period % 4L);
        for (int i = 0; i < n; ++i) sb.append('.');
        return sb.toString();
    }

    public static void resetColor() {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void setColor(int n) {
        float a = (float) (n >> 24 & 0xFF) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
    }

    public static void roundedRectOutlined(float x, float y, float x2, float y2, float r, GuiColor fill, GuiColor c2, GuiColor c3) {
        int n;
        x *= 2.0f;
        y *= 2.0f;
        x2 *= 2.0f;
        y2 *= 2.0f;
        GL11.glPushAttrib(1);
        GL11.glScaled(0.5, 0.5, 0.5);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBegin(9);
        GuiColor.apply(fill);
        float f6 = x + r, f7 = y + r, f8 = x2 - r, f9 = y2 - r;
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f6 + SIN_0_90[n] * -r, f7 + COS_0_90[n] * -r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f6 + SIN_90_180[n] * -r, f9 + COS_90_180[n] * -r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f8 + SIN_0_90[n] * r, f9 + COS_0_90[n] * r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f8 + SIN_90_180[n] * r, f7 + COS_90_180[n] * r);
        GL11.glEnd();
        GL11.glPushMatrix();
        GL11.glShadeModel(7425);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(2);
        if (c2 != null) GuiColor.apply(c2);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f6 + SIN_0_90[n] * -r, f7 + COS_0_90[n] * -r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f6 + SIN_90_180[n] * -r, f9 + COS_90_180[n] * -r);
        if (c3 != null) GuiColor.apply(c3);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f8 + SIN_0_90[n] * r, f9 + COS_0_90[n] * r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f8 + SIN_90_180[n] * r, f7 + COS_90_180[n] * r);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        GL11.glLineWidth(1.0f);
        GL11.glShadeModel(7424);
        resetColor();
    }

    public static void roundedRect(float x, float y, float x2, float y2, float r, int color) {
        int n;
        r = Math.min(r, Math.min(((x2 *= 2.0f) - (x *= 2.0f)) / 2.0f, ((y2 *= 2.0f) - (y *= 2.0f)) / 2.0f));
        float f6 = x + r, f7 = y + r, f8 = x2 - r, f9 = y2 - r;
        GL11.glPushAttrib(0);
        GL11.glScaled(0.5, 0.5, 0.5);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBegin(9);
        setColor(color);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f6 + SIN_0_90[n] * -r, f7 + COS_0_90[n] * -r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f6 + SIN_90_180[n] * -r, f9 + COS_90_180[n] * -r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f8 + SIN_0_90[n] * r, f9 + COS_0_90[n] * r);
        for (n = 0; n <= 30; ++n) GL11.glVertex2d(f8 + SIN_90_180[n] * r, f7 + COS_90_180[n] * r);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        resetColor();
    }

    public static void rect(float x, float y, float x2, float y2, int color) {
        beginRect();
        rawRect(x, y, x2, y2, color);
        endRect();
    }

    public static void beginRect() {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
    }

    public static void rawRect(float f, float f2, float f3, float f4, int n) {
        float t;
        if (f < f3) { t = f; f = f3; f3 = t; }
        if (f2 < f4) { t = f2; f2 = f4; f4 = t; }
        float a = (float) (n >>> 24) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION);
        wr.pos(f, f4, 0.0).endVertex();
        wr.pos(f3, f4, 0.0).endVertex();
        wr.pos(f3, f2, 0.0).endVertex();
        wr.pos(f, f2, 0.0).endVertex();
        tessellator.draw();
    }

    public static void endRect() {
        resetColor();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void quad(double x, double y, double w, double h) {
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2d(x, y + h);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2d(x + w, y + h);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2d(x + w, y);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
    }

    public static void screenQuad() {
        quad(0.0, 0.0, scaledWidth, scaledHeight);
    }

    public static void drawItem(ItemStack stack, float x, float y) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        RenderHelper.enableGUIStandardItemLighting();
        RenderItem renderItem = mc.getRenderItem();
        renderItem.zLevel = -100.0f;
        IBakedModel model = renderItem.getItemModelMesher().getItemModel(stack);
        GlStateManager.pushMatrix();
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        resetColor();
        GlStateManager.translate(x, y, 100.0f + renderItem.zLevel);
        GlStateManager.translate(8.0f, 8.0f, 0.0f);
        GlStateManager.scale(1.0f, 1.0f, -1.0f);
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        if (model.isGui3d()) {
            GlStateManager.scale(40.0f, 40.0f, 40.0f);
            GlStateManager.rotate(210.0f, 1.0f, 0.0f, 0.0f);
            GlStateManager.rotate(-135.0f, 0.0f, 1.0f, 0.0f);
            GlStateManager.enableLighting();
        } else {
            GlStateManager.scale(64.0f, 64.0f, 64.0f);
            GlStateManager.rotate(180.0f, 1.0f, 0.0f, 0.0f);
            GlStateManager.disableLighting();
        }
        model = ForgeHooksClient.handleCameraTransforms(model, ItemCameraTransforms.TransformType.GUI);
        renderItem.renderItem(stack, model);
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        renderItem.zLevel = 0.0f;
        RenderHelper.disableStandardItemLighting();
    }

    public static int blendInt(int n, int n2, double d) {
        int n3 = (int) (d * 255.0);
        int n4 = 255 - n3;
        int a = (n >> 24 & 0xFF) * n3 + (n2 >> 24 & 0xFF) * n4 >> 8;
        int r = (n >> 16 & 0xFF) * n3 + (n2 >> 16 & 0xFF) * n4 >> 8;
        int g = (n >> 8 & 0xFF) * n3 + (n2 >> 8 & 0xFF) * n4 >> 8;
        int b = (n & 0xFF) * n3 + (n2 & 0xFF) * n4 >> 8;
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void gradientRect(float f, float f2, float f3, float f4, int n, int n2) {
        float t;
        if (f < f3) { t = f; f = f3; f3 = t; }
        if (f2 < f4) { t = f2; f2 = f4; f4 = t; }
        float a = (float) (n >> 24 & 0xFF) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        float a2 = (float) (n2 >> 24 & 0xFF) / 255.0f;
        float r2 = (float) (n2 >> 16 & 0xFF) / 255.0f;
        float g2 = (float) (n2 >> 8 & 0xFF) / 255.0f;
        float b2 = (float) (n2 & 0xFF) / 255.0f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(f3, f2, 0.0).color(r, g, b, a).endVertex();
        wr.pos(f, f2, 0.0).color(r, g, b, a).endVertex();
        wr.pos(f, f4, 0.0).color(r2, g2, b2, a2).endVertex();
        wr.pos(f3, f4, 0.0).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void roundedRectBorder(float f, float f2, float f3, float f4, float f5, int n, int n2, int n3) {
        int i;
        f *= 2.0f;
        f2 *= 2.0f;
        f3 *= 2.0f;
        f4 *= 2.0f;
        GL11.glPushAttrib(1);
        GL11.glScaled(0.5, 0.5, 0.5);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBegin(9);
        setColor(n);
        float f6 = f + f5;
        float f7 = f2 + f5;
        float f8 = f3 - f5;
        float f9 = f4 - f5;
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f6 + SIN_0_90[i] * (double) (-f5), (double) f7 + COS_0_90[i] * (double) (-f5));
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f6 + SIN_90_180[i] * (double) (-f5), (double) f9 + COS_90_180[i] * (double) (-f5));
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f8 + SIN_0_90[i] * (double) f5, (double) f9 + COS_0_90[i] * (double) f5);
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f8 + SIN_90_180[i] * (double) f5, (double) f7 + COS_90_180[i] * (double) f5);
        GL11.glEnd();
        GL11.glPushMatrix();
        GL11.glShadeModel(7425);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(2);
        if (n2 != 0) {
            setColor(n2);
        }
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f6 + SIN_0_90[i] * (double) (-f5), (double) f7 + COS_0_90[i] * (double) (-f5));
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f6 + SIN_90_180[i] * (double) (-f5), (double) f9 + COS_90_180[i] * (double) (-f5));
        if (n3 != 0) {
            setColor(n3);
        }
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f8 + SIN_0_90[i] * (double) f5, (double) f9 + COS_0_90[i] * (double) f5);
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f8 + SIN_90_180[i] * (double) f5, (double) f7 + COS_90_180[i] * (double) f5);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        GL11.glLineWidth(1.0f);
        GL11.glShadeModel(7424);
        resetColor();
    }

    public static void roundedRectGradient(float f, float f2, float f3, float f4, float f5, int n, int n2, int n3, int n4) {
        int i;
        f5 = Math.min(f5, Math.min(((f3 *= 2.0f) - (f *= 2.0f)) / 2.0f, ((f4 *= 2.0f) - (f2 *= 2.0f)) / 2.0f));
        float f6 = f + f5;
        float f7 = f2 + f5;
        float f8 = f3 - f5;
        float f9 = f4 - f5;
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glShadeModel(7425);
        GL11.glPushAttrib(0);
        GL11.glScaled(0.5, 0.5, 0.5);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glShadeModel(7425);
        GL11.glBegin(9);
        setColor(n);
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f6 + SIN_0_90[i] * (double) (-f5), (double) f7 + COS_0_90[i] * (double) (-f5));
        setColor(n2);
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f6 + SIN_90_180[i] * (double) (-f5), (double) f9 + COS_90_180[i] * (double) (-f5));
        setColor(n3);
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f8 + SIN_0_90[i] * (double) f5, (double) f9 + COS_0_90[i] * (double) f5);
        setColor(n4);
        for (i = 0; i <= 30; ++i) GL11.glVertex2d((double) f8 + SIN_90_180[i] * (double) f5, (double) f7 + COS_90_180[i] * (double) f5);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        GL11.glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glShadeModel(7424);
        resetColor();
    }

    public static void shadowText(String s, float x, float y, int color) {
        GlStateManager.enableBlend();
        mc.fontRendererObj.drawString(s, x, y, color, true);
        GlStateManager.disableBlend();
        resetColor();
    }
}
