package xyz.daikirai.gui;

import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class InfoHudEditor extends HudEditor {
    public static int hudX = 4;
    public static int hudY = -1;

    private static final String HELP = "Edit the HUD position by dragging.";

    private final String[] example = new String[]{"This is an", "example", "HUD"};
    private GuiButtonExt resetButton;
    private int x;
    private int y;
    private boolean dragging;
    private int dragStartX;
    private int dragStartY;
    private int dragFromX;
    private int dragFromY;
    private int left;
    private int right;
    private int top;
    private int bottom;

    public static int defaultY() {
        if (hudY == -1) {
            hudY = RenderUtils.scaledHeight / 3 - 30;
        }
        return hudY;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.resetButton = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position");
        this.buttonList.add(this.resetButton);
        this.x = hudX;
        this.y = hudY < 0 ? defaultY() : hudY;
        this.initPos(this.x, this.y);
    }

    @Override
    public void drawScreen(int mx, int my, float p) {
        super.drawScreen(mx, my, p);
        this.drawExample(this.example);
        hudX = this.x;
        hudY = this.y;
        int tx = RenderUtils.scaledWidth / 2 - 84;
        int ty = RenderUtils.scaledHeight / 2 - 20;
        rainbowText(HELP, '-', tx, ty, 2L, 0L, true, this.mc.fontRendererObj);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == this.resetButton) {
            this.x = 4;
            hudY = -1;
            this.y = defaultY();
            hudX = this.x;
            hudY = this.y;
        }
    }

    private void drawExample(String[] lines) {
        FontRenderer fr = this.mc.fontRendererObj;
        int w = 0;
        for (String s : lines) {
            int n = fr.getStringWidth(s);
            if (n <= w) continue;
            w = n;
        }
        int x1 = this.x - 5;
        int x2 = this.x + w + 5;
        int y1 = this.y - 5;
        int y2 = this.y + (lines.length - 1) * 12 + fr.FONT_HEIGHT + 5;
        this.left = x1;
        this.top = y1;
        this.right = x2;
        this.bottom = y2;
        boolean shadow = false;
        if (SettingsModule.background == SettingsModule.Background.Rect) {
            RenderUtils.rect(x1, y1, (float) x2 - 0.5f, (float) y2 - 1.5f, 0x7D000000);
        } else if (SettingsModule.background == SettingsModule.Background.Round) {
            if (x1 <= 0) {
                x1 = -3;
            }
            if (x2 > RenderUtils.scaledWidth) {
                x2 = RenderUtils.scaledWidth + 3;
            }
            RenderUtils.roundedRect(x1, y1, (float) x2 - 0.5f, (float) y2 - 1.5f, 8.0f, 0x7D000000);
        } else {
            shadow = true;
        }
        int oy = 0;
        for (String s : lines) {
            fr.drawString(s, (float) this.x, (float) (this.y + oy), -1, shadow);
            oy += 12;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        super.mouseClickMove(mx, my, button, time);
        if (button != 0) {
            return;
        }
        if (this.dragging) {
            this.x = this.dragFromX + (mx - this.dragStartX);
            this.y = this.dragFromY + (my - this.dragStartY);
        } else if (mx > this.left && mx < this.right && my > this.top && my < this.bottom) {
            this.dragging = true;
            this.dragStartX = mx;
            this.dragStartY = my;
            this.dragFromX = this.x;
            this.dragFromY = this.y;
        }
    }

    @Override
    public void onGuiClosed() {
        this.closed(hudX, hudY);
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        super.mouseReleased(mx, my, button);
        if (button == 0) {
            this.dragging = false;
        }
    }

    public static void rainbowText(String s, char newline, int x, int y, long speed, long lineOffset, boolean shadow, FontRenderer fr) {
        int startX = x;
        int line = 0;
        long offset = 0L;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == newline) {
                x = startX;
                y += fr.FONT_HEIGHT + 5;
                offset = lineOffset * (long) (++line);
                continue;
            }
            fr.drawString(String.valueOf(c), (float) x, (float) y, RenderUtils.rainbow(speed, offset), shadow);
            x += fr.getCharWidth(c);
            if (c == ' ') continue;
            offset -= 90L;
        }
    }
}
