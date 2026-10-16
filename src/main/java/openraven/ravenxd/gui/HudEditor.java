package openraven.ravenxd.gui;

import openraven.ravenxd.module.GuiModule;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.util.Blur;
import openraven.ravenxd.util.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class HudEditor extends GuiScreen {
    private int startX;
    private int startY;

    public void closed(int x, int y) {
        if (x != this.startX || y != this.startY) {
            openraven.ravenxd.client.Profiles.markModified();
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == 1 || key == ModuleManager.gui.getKeycode()) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int x, int y, float p) {
        if (GuiModule.darkBackground.isToggled()) {
            drawRect(0, 0, this.width, this.height, -1089466352);
        }
        if (GuiModule.backgroundBlur.getInput() > 0.0) {
            Blur.get().startStencil();
            Gui.drawRect(0, 0, RenderUtils.scaledWidth, RenderUtils.scaledHeight, -1);
            Blur.get().render(2, GuiModule.blurStrength);
        }
        try {
            this.handleInput();
        } catch (IOException ignored) {
        }
        int wy = this.height + 18 - 30;
        Watermark.render(4, wy, false);
        super.drawScreen(x, y, p);
    }

    public void initPos(int x, int y) {
        this.startX = x;
        this.startY = y;
    }

    @Override
    public void updateScreen() {
        GuiModule.updateMovement();
    }
}
