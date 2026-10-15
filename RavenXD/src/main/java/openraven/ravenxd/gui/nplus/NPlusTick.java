package xyz.daikirai.gui.nplus;

import xyz.daikirai.module.setting.ButtonSetting;
import org.lwjgl.opengl.GL11;

public class NPlusTick extends NPlusComp {
    private final ButtonSetting tick;

    public NPlusTick(NPlusModule module, ButtonSetting tick, int o) {
        super(module, o);
        this.tick = tick;
    }

    @Override
    public void draw() {
        boolean on = this.tick.isMethodButton() || this.tick.isToggled();
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        NPlusRender.checkBox((this.left() + 4) * 2, (this.top() + 2) * 2, 12, on);
        mc.fontRendererObj.drawString("   - " + this.tick.getName(), (this.left() + 6) * 2, (this.top() + 3) * 2, on ? NPlusRender.clientInt : NPlusRender.lightRed, false);
        GL11.glPopMatrix();
    }

    @Override
    public void mouseDown(int x, int y, int b) {
        if (b == 0 && this.module.expanded && this.hovered(x, y)) {
            this.tick.toggle();
            this.module.mod.guiButtonToggled(this.tick);
        }
    }
}
