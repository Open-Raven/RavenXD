package xyz.daikirai.gui;

import xyz.daikirai.module.minigames.BridgeInfoModule;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class BridgeInfoEditor extends HudEditor {
    private static final String HELP = "Edit the HUD position by dragging.";
    private static final String EXAMPLE = "Enemy: Player123-Distance to goal: 17.2-Enemy distance to goal: 16.3-Blocks: 98";
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

    @Override
    public void initGui() {
        super.initGui();
        this.resetButton = new GuiButtonExt(1, this.width - 90, 5, 85, 20, "Reset position");
        this.buttonList.add(this.resetButton);
        this.x = BridgeInfoModule.posX;
        this.y = BridgeInfoModule.posY;
        this.initPos(this.x, this.y);
    }

    @Override
    public void drawScreen(int mx, int my, float p) {
        super.drawScreen(mx, my, p);
        int x = this.x;
        int y = this.y;
        int r = x + 140;
        int b = y + 41;
        this.drawExample(this.mc.fontRendererObj, EXAMPLE);
        this.left = x;
        this.top = y;
        this.right = r;
        this.bottom = b;
        BridgeInfoModule.posX = x;
        BridgeInfoModule.posY = y;
        int tx = RenderUtils.scaledWidth / 2 - 84;
        int ty = RenderUtils.scaledHeight / 2 - 20;
        InfoHudEditor.rainbowText(HELP, '-', tx, ty, 2L, 0L, true, this.mc.fontRendererObj);
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
    protected void mouseReleased(int mx, int my, int button) {
        super.mouseReleased(mx, my, button);
        if (button == 0) {
            this.dragging = false;
        }
    }

    private void drawExample(FontRenderer fr, String s) {
        int x = this.left;
        int y = this.top;
        for (String line : s.split("-")) {
            fr.drawString(line, (float) x, (float) y, BridgeInfoModule.COLOR, true);
            y += fr.FONT_HEIGHT + 2;
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == this.resetButton) {
            this.x = BridgeInfoModule.posX = 5;
            this.y = BridgeInfoModule.posY = 70;
        }
    }

    @Override
    public void onGuiClosed() {
        this.closed(BridgeInfoModule.posX, BridgeInfoModule.posY);
    }
}
