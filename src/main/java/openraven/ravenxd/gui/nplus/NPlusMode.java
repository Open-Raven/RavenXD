package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.module.setting.SliderSetting;
import org.lwjgl.opengl.GL11;

// left click back, right click next
public class NPlusMode extends NPlusComp {
    private final SliderSetting mode;

    public NPlusMode(NPlusModule module, SliderSetting mode, int o) {
        super(module, o);
        this.mode = mode;
    }

    @Override
    public void draw() {
        String name = this.mode.getName() + ": ";
        int nameWidth = mc.fontRendererObj.getStringWidth(name) / 2;
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        mc.fontRendererObj.drawString(name, (this.left() + 4) * 2, (this.top() + 4) * 2, NPlusRender.clientInt, true);
        mc.fontRendererObj.drawString(this.mode.getDisplay(false), (this.left() + nameWidth + 4) * 2, (this.top() + 4) * 2, NPlusRender.clientIntDarker, true);
        GL11.glPopMatrix();
    }

    @Override
    public void mouseDown(int x, int y, int b) {
        if (!this.module.expanded || !this.hovered(x, y) || b > 1) {
            return;
        }
        int count = this.mode.options.length;
        int next = (int) this.mode.getInput() + (b == 1 ? 1 : -1);
        this.mode.setValue((next % count + count) % count);
    }
}
