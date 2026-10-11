package xyz.daikirai.gui;

import xyz.daikirai.module.movement.SprintModule;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class SprintTextEditor extends HudEditor {
    private static final String HELP = "Edit the HUD position by dragging.";

    private int x, y, x2, y2;
    private int targetX = 5, targetY = 5;
    private boolean dragging;
    private int dragStartX, dragStartY, dragFromX, dragFromY;
    private GuiButtonExt resetButton;

    @Override
    public void onGuiClosed() {
        this.closed(SprintModule.posX, SprintModule.posY);
    }

    @Override
    public void drawScreen(int mx, int my, float p) {
        super.drawScreen(mx, my, p);
        int w = this.x + this.mc.fontRendererObj.getStringWidth(SprintModule.text);
        int h = this.y + this.mc.fontRendererObj.FONT_HEIGHT;
        this.mc.fontRendererObj.drawStringWithShadow(SprintModule.text, (float) this.x, (float) this.y, -1);
        this.x = this.targetX;
        this.y = this.targetY;
        this.x2 = w;
        this.y2 = h;
        SprintModule.posX = this.x;
        SprintModule.posY = this.y;
        ScaledResolution sr = new ScaledResolution(this.mc);
        InfoHudEditor.rainbowText(HELP, '-', sr.getScaledWidth() / 2 - 84, sr.getScaledHeight() / 2 - 20, 2L, 0L, true, this.mc.fontRendererObj);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == this.resetButton) {
            SprintModule.posX = 5;
            this.targetX = 5;
            SprintModule.posY = 5;
            this.targetY = 5;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int button) {
        super.mouseReleased(mx, my, button);
        if (button == 0) {
            this.dragging = false;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.resetButton = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position");
        this.buttonList.add(this.resetButton);
        this.targetX = SprintModule.posX;
        this.targetY = SprintModule.posY;
        this.initPos(this.targetX, this.targetY);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        super.mouseClickMove(mx, my, button, time);
        if (button != 0) {
            return;
        }
        if (this.dragging) {
            this.targetX = this.dragFromX + (mx - this.dragStartX);
            this.targetY = this.dragFromY + (my - this.dragStartY);
        } else if (mx > this.x && mx < this.x2 && my > this.y && my < this.y2) {
            this.dragging = true;
            this.dragStartX = mx;
            this.dragStartY = my;
            this.dragFromX = this.targetX;
            this.dragFromY = this.targetY;
        }
    }
}
