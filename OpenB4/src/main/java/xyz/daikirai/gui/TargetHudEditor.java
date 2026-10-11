package xyz.daikirai.gui;

import xyz.daikirai.module.render.TargetHudModule;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.client.config.GuiButtonExt;

public class TargetHudEditor extends HudEditor {
    private static final String HELP = "Edit the HUD position by dragging.";

    private String text = "";
    private double ratio;
    private boolean dragging;
    private int dragStartX;
    private int dragStartY;
    private int dragFromX;
    private int dragFromY;
    private GuiButtonExt resetButton;

    @Override
    public void onGuiClosed() {
        this.closed(TargetHudModule.offsetX, TargetHudModule.offsetY);
    }

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
        if (button != 0) {
            return;
        }
        int w = this.mc.fontRendererObj.getStringWidth(this.text) + 8;
        int x = RenderUtils.scaledWidth / 2 - w / 2 + TargetHudModule.offsetX;
        int y = RenderUtils.scaledHeight / 2 + TargetHudModule.offsetY;
        int x1 = x - 8;
        int y1 = y - 8;
        int x2 = x + w;
        int y2 = y + this.mc.fontRendererObj.FONT_HEIGHT + 12 + 8;
        if (this.dragging) {
            TargetHudModule.offsetX = this.dragFromX + (mx - this.dragStartX);
            TargetHudModule.offsetY = this.dragFromY + (my - this.dragStartY);
        } else if (mx > x1 && mx < x2 && my > y1 && my < y2) {
            this.dragging = true;
            this.dragStartX = mx;
            this.dragStartY = my;
            this.dragFromX = TargetHudModule.offsetX;
            this.dragFromY = TargetHudModule.offsetY;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.resetButton = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position");
        this.buttonList.add(this.resetButton);
        this.initPos(TargetHudModule.offsetX, TargetHudModule.offsetY);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == this.resetButton) {
            TargetHudModule.offsetX = 70;
            TargetHudModule.offsetY = 40;
        }
    }

    @Override
    public void updateScreen() {
        EntityLivingBase e = xyz.daikirai.module.combat.KillAuraModule.hasTarget() && xyz.daikirai.module.combat.KillAuraModule.target.living
                ? (EntityLivingBase) xyz.daikirai.module.combat.KillAuraModule.target.entity : this.mc.thePlayer;
        this.ratio = Math.min(e.getHealth() / e.getMaxHealth(), 1.0f);
        this.text = TargetHudModule.text(e.getDisplayName().getUnformattedText(), e.getHealth(), this.ratio);
        super.updateScreen();
    }

    @Override
    public void drawScreen(int mx, int my, float p) {
        super.drawScreen(mx, my, p);
        TargetHudModule.mode.render(this.text, this.ratio);
        int tx = RenderUtils.scaledWidth / 2 - 84;
        int ty = RenderUtils.scaledHeight / 2 - 20;
        InfoHudEditor.rainbowText(HELP, '-', tx, ty, 2L, 0L, true, this.mc.fontRendererObj);
    }
}
