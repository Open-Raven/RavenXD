package xyz.daikirai.client;

import xyz.daikirai.gui.InfoHudEditor;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public final class InfoHud {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final class Line {
        boolean columns;
        String left, right;
        ItemStack item;
        int leftWidth, rightWidth, width;

        void set(String left, String right, ItemStack item) {
            this.columns = true;
            this.left = Chat.color(left);
            this.right = Chat.color(right);
            this.item = item;
            this.leftWidth = mc.fontRendererObj.getStringWidth(this.left);
            this.rightWidth = mc.fontRendererObj.getStringWidth(this.right);
        }

        void set(String text) {
            this.columns = false;
            this.left = Chat.color(text);
            this.right = null;
            this.item = null;
            this.width = this.leftWidth = mc.fontRendererObj.getStringWidth(this.left);
        }
    }

    private static final List<Line> lines = new ArrayList<Line>();
    private static final List<Line> pool = new ArrayList<Line>();
    private static int poolUsed;
    private static int width, leftColumn, rightColumn, gap;
    private static boolean hasItems;
    private static int targetWidth = -1, targetLeft, targetRight;
    private static float curWidth, curLeft, curRight, fromWidth, fromLeft, fromRight;
    private static long animStart;

    private InfoHud() {
    }

    private static void clear() {
        lines.clear();
        rightColumn = 0;
        leftColumn = 0;
        poolUsed = 0;
        width = 0;
        hasItems = false;
    }

    public static void add(Line line) {
        if (line == null) {
            return;
        }
        if (line.columns) {
            gap = mc.fontRendererObj.getCharWidth(' ') * 2;
            leftColumn = Math.max(leftColumn, line.leftWidth);
            rightColumn = Math.max(rightColumn, line.rightWidth);
            if (line.item != null) {
                hasItems = true;
            }
            width = Math.max(width, leftColumn + gap + rightColumn + (hasItems ? gap + 10 : 0));
        } else if (line.width > width) {
            width = line.width;
        }
        lines.add(line);
    }

    private static void animate(int w, int left, int right, boolean animate) {
        long now = System.currentTimeMillis();
        if (!animate || targetWidth == -1) {
            targetWidth = w;
            curWidth = fromWidth = (float) targetWidth;
            targetLeft = left;
            curLeft = fromLeft = (float) targetLeft;
            targetRight = right;
            curRight = fromRight = (float) targetRight;
            animStart = now;
            return;
        }
        float f = Math.min(1.0f, (float) (now - animStart) / 100.0f);
        f = f * f * (3.0f - 2.0f * f);
        curWidth = fromWidth + ((float) targetWidth - fromWidth) * f;
        curLeft = fromLeft + ((float) targetLeft - fromLeft) * f;
        curRight = fromRight + ((float) targetRight - fromRight) * f;
        if (w != targetWidth || left != targetLeft || right != targetRight) {
            fromWidth = curWidth;
            fromLeft = curLeft;
            fromRight = curRight;
            targetWidth = w;
            targetLeft = left;
            targetRight = right;
            animStart = now;
        }
    }

    public static void render(boolean visible) {
        int n = lines.size();
        if (n == 0) {
            return;
        }
        if (visible) {
            boolean background = SettingsModule.background != SettingsModule.Background.None;
            animate(width, leftColumn, rightColumn, background);
            float w = curWidth;
            float left = curLeft;
            float right = curRight;
            int x = InfoHudEditor.hudX;
            int y = InfoHudEditor.defaultY();
            float x1 = x - 5;
            float x2 = (float) x + w + 5.0f - 1.0f;
            float y1 = y - 5;
            float y2 = y + (n - 1) * 12 + mc.fontRendererObj.FONT_HEIGHT + 5;
            boolean shadow = false;
            if (background) {
                if (SettingsModule.background == SettingsModule.Background.Rect) {
                    RenderUtils.rect(x1, y1, x2 - 0.5f, y2 - 1.5f, 0x7D000000);
                } else {
                    if (x1 <= 0.0f) {
                        x1 = -3.0f;
                    }
                    if (x2 > (float) RenderUtils.scaledWidth) {
                        x2 = RenderUtils.scaledWidth + 3;
                    }
                    RenderUtils.roundedRect(x1, y1, x2 - 0.5f, y2 - 1.5f, 8.0f, 0x7D000000);
                }
                scissor(x1, y1, x2 - 0.5f, y2 - 1.5f);
            } else {
                shadow = true;
            }
            for (int i = 0; i < n; ++i) {
                Line line = lines.get(i);
                if (line.columns) continue;
                mc.fontRendererObj.drawString(line.left, (float) x, (float) (y + i * 12), -1, shadow);
            }
            if (background) {
                scissor(x, y1, (float) x + left, y2 - 1.5f);
            }
            for (int i = 0; i < n; ++i) {
                Line line = lines.get(i);
                if (!line.columns) continue;
                mc.fontRendererObj.drawString(line.left, (float) x, (float) (y + i * 12), -1, shadow);
            }
            if (background) {
                scissor(x1, y1, x2 - 0.5f, y2 - 1.5f);
            }
            float rx = (float) x + left + (float) gap;
            for (int i = 0; i < n; ++i) {
                Line line = lines.get(i);
                if (!line.columns) continue;
                mc.fontRendererObj.drawString(line.right, rx + right - (float) line.rightWidth, (float) (y + i * 12), -1, shadow);
            }
            if (hasItems) {
                float ix = (rx + right + (float) gap) * 1.6f;
                GlStateManager.pushMatrix();
                GlStateManager.scale(0.625f, 0.625f, 0.625f);
                for (int i = 0; i < n; ++i) {
                    Line line = lines.get(i);
                    if (!line.columns || line.item == null) continue;
                    float iy = ((float) (y + i * 12) - 0.5f) * 1.6f;
                    GL11.glDepthFunc(519);
                    RenderUtils.drawItem(line.item, ix, iy);
                }
                GL11.glDepthFunc(515);
                GlStateManager.popMatrix();
            }
            if (background) {
                GL11.glDisable(3089);
            }
            RenderUtils.resetColor();
        }
        clear();
    }

    private static void scissor(float x1, float y1, float x2, float y2) {
        int scale = RenderUtils.scaleFactor;
        int a = Math.max(0, (int) Math.floor(x1 * (float) scale));
        int b = Math.min(mc.displayWidth, (int) Math.ceil(x2 * (float) scale));
        int c = Math.max(0, (int) Math.floor((float) mc.displayHeight - y2 * (float) scale));
        int d = Math.min(mc.displayHeight, (int) Math.ceil((float) mc.displayHeight - y1 * (float) scale));
        GL11.glEnable(3089);
        GL11.glScissor(a, c, Math.max(0, b - a), Math.max(0, d - c));
    }

    private static Line pooled() {
        Line line;
        if (poolUsed < pool.size()) {
            line = pool.get(poolUsed);
        } else {
            line = new Line();
            pool.add(line);
        }
        ++poolUsed;
        return line;
    }

    public static void add(String text) {
        Line line = pooled();
        line.set(text);
        add(line);
    }

    public static Line line(String left, String right, ItemStack item) {
        Line line = new Line();
        line.set(left, right, item);
        return line;
    }
}
