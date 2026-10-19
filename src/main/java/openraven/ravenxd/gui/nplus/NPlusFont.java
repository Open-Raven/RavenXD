package openraven.ravenxd.gui.nplus;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

// skidded ttf renderer
public class NPlusFont {
    private static final Map<Integer, NPlusFont> FONTS = new HashMap<Integer, NPlusFont>();
    private static Font base;
    private static boolean failed;

    private final float[] widths = new float[256];
    private final float[] heights = new float[256];
    private final int[] textures = new int[256];

    private NPlusFont(Font font) {
        BufferedImage util = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ug = (Graphics2D) util.getGraphics();
        ug.setFont(font);
        FontMetrics fm = ug.getFontMetrics();
        for (int i = 0; i < 256; ++i) {
            String s = String.valueOf((char) i);
            Rectangle2D b = fm.getStringBounds(s, ug);
            BufferedImage img = new BufferedImage((int) Math.ceil(b.getWidth() + 8.0), Math.max(1, (int) Math.ceil(b.getHeight())), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) img.getGraphics();
            g.setFont(font);
            g.setColor(new Color(255, 255, 255, 0));
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.setColor(Color.WHITE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.drawString(s, 4, fm.getAscent());
            this.widths[i] = img.getWidth();
            this.heights[i] = img.getHeight();
            this.textures[i] = upload(img);
        }
    }

    private static int upload(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int p : pixels) {
            buf.put((byte) (p >> 16 & 0xFF)).put((byte) (p >> 8 & 0xFF)).put((byte) (p & 0xFF)).put((byte) (p >> 24 & 0xFF));
        }
        buf.flip();
        int id = GL11.glGenTextures();
        GlStateManager.bindTexture(id);
        GL11.glTexParameteri(3553, 10241, 9728);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexImage2D(3553, 0, 6408, w, h, 0, 6408, 5121, buf);
        return id;
    }

    // Must be called from the render thread. Returns null if Comfortaa could not be loaded.
    public static NPlusFont get(int size) {
        if (failed) {
            return null;
        }
        NPlusFont f = FONTS.get(size);
        if (f != null) {
            return f;
        }
        try {
            if (base == null) {
                InputStream in = NPlusFont.class.getResourceAsStream("/assets/ravenxd/nplus/fonts/comfortaa.ttf");
                base = Font.createFont(Font.TRUETYPE_FONT, in);
                in.close();
            }
            f = new NPlusFont(base.deriveFont(Font.PLAIN, (float) size));
        } catch (Exception e) {
            failed = true;
            return null;
        }
        FONTS.put(size, f);
        return f;
    }

    public void drawString(String text, float x, float y, int color, boolean shadow) {
        if (shadow) {
            GL11.glTranslated(0.5, 0.5, 0.0);
            this.render(text, x, y, color, true);
            GL11.glTranslated(-0.5, -0.5, 0.0);
        }
        this.render(text, x, y, color, false);
    }

    private void render(String text, float x, float y, int color, boolean shadow) {
        x = (Math.round(x * 10.0f) / 10.0f - 1.5f) * 2.0f;
        y = (Math.round(y * 10.0f) / 10.0f - 1.5f) * 2.0f;
        GL11.glPushMatrix();
        GlStateManager.scale(0.5, 0.5, 1.0);
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(770, 771);
        double m = 255.0 * (shadow ? 4 : 1);
        GL11.glColor4d((color >> 16 & 0xFF) / m, (color >> 8 & 0xFF) / m, (color & 0xFF) / m, (color >> 24 & 0xFF) / 255.0);
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                ++i;
                continue;
            }
            if (c >= 256) continue;
            float w = this.widths[c], h = this.heights[c];
            GlStateManager.bindTexture(this.textures[c]);
            GL11.glBegin(6);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2d(x, y);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2d(x, y + h);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2d(x + w, y + h);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2d(x + w, y);
            GL11.glEnd();
            x += w - 8.0f;
        }
        GL11.glPopMatrix();
        GlStateManager.bindTexture(0);
        GlStateManager.resetColor();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public float getStringWidth(String text) {
        float w = 0.0f;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                ++i;
                continue;
            }
            w += ((c < 256 ? this.widths[c] : this.widths['A']) - 8.0f) / 2.0f;
        }
        return w + 2.0f;
    }

    public float getHeight() {
        return this.heights['I'] / 2.0f - 2.0f;
    }
}
