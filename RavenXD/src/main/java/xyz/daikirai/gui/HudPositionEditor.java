package xyz.daikirai.gui;

import xyz.daikirai.module.HudModule;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Theme;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class HudPositionEditor extends HudEditor {
    private static final String HELP = "Edit the HUD position by dragging.";

    private final String[] example = new String[]{"This is an", "example", "HUD"};
    private int x = 5;
    private int y = 70;
    private int left;
    private int top;
    private int right;
    private int bottom;
    private boolean dragging;
    private int dragStartX;
    private int dragStartY;
    private int dragFromX;
    private int dragFromY;
    private GuiButtonExt resetButton;
    private GuiButtonExt alignButton;

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        super.mouseReleased(mx, my, button);
        if (button == 0) {
            this.dragging = false;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        super.mouseClickMove(mx, my, button, time);
        if (HudModule.alignRight.isToggled()) {
            mx += 50;
        }
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
    public void initGui() {
        super.initGui();
        this.resetButton = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position");
        this.buttonList.add(this.resetButton);
        this.alignButton = new GuiButtonExt(2, this.width - 90, this.height - 47, 85, 20, "Align: " + (HudModule.alignRight.isToggled() ? "Right" : "Left"));
        this.buttonList.add(this.alignButton);
        this.x = HudModule.posX;
        this.y = HudModule.posY;
        this.initPos(this.x, this.y);
    }

    @Override
    public void drawScreen(int mx, int my, float p) {
        super.drawScreen(mx, my, p);
        this.drawExample(this.example);
        this.left = this.x;
        this.top = this.y;
        HudModule.posX = this.left;
        HudModule.posY = this.top;
        int tx = RenderUtils.scaledWidth / 2 - 84;
        int ty = RenderUtils.scaledHeight / 2 - 20;
        InfoHudEditor.rainbowText(HELP, '-', tx, ty, 2L, 0L, true, this.mc.fontRendererObj);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == this.resetButton) {
            if (HudModule.alignRight.isToggled()) {
                this.x = HudModule.posX = this.width - 5;
            } else {
                HudModule.posX = 5;
                this.x = 5;
            }
            HudModule.posY = 70;
            this.y = 70;
        } else if (b == this.alignButton) {
            HudModule.alignRight.toggle();
            this.alignButton.displayString = "Align: " + (HudModule.alignRight.isToggled() ? "Right" : "Left");
        }
    }

    @Override
    public void onGuiClosed() {
        this.closed(HudModule.posX, HudModule.posY);
    }

    private void drawExample(String[] lines) {
        FontRenderer fr = this.mc.fontRendererObj;
        float sc = (float) HudModule.scale.getInput();
        int lh = Math.round((float) (fr.FONT_HEIGHT + 2) * sc);
        int w = 0;
        for (String s : lines) {
            int n = Math.round((float) fr.getStringWidth(s) * sc);
            if (n <= w) continue;
            w = n;
        }
        this.right = this.left + w;
        this.bottom = this.top + lh * lines.length;
        float fx = (float) this.left / sc;
        float fy = (float) this.top / sc;
        GlStateManager.pushMatrix();
        if (sc != 1.0f) {
            GlStateManager.scale(sc, sc, 1.0f);
        }
        float ly = fy;
        float offset = 0.0f;
        for (String s : lines) {
            float lx = fx;
            if (HudModule.alignRight.isToggled()) {
                lx -= (float) fr.getStringWidth(s);
            }
            int rgb = HudModule.theme.getRGB(offset);
            offset = HudModule.theme == Theme.RAINBOW ? (float) ((double) offset - 240.0 * SettingsModule.themeOffset.getInput())
                    : (float) ((double) offset - SettingsModule.themeOffset.getInput());
            fr.drawString(s, lx, ly, rgb, true);
            RenderUtils.resetColor();
            ly += (float) (fr.FONT_HEIGHT + 2);
        }
        GlStateManager.popMatrix();
    }
}
