package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.module.NPlusThemeModule;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

public class NPlusSlider extends NPlusComp {
    private final SliderSetting slider;
    private boolean dragging;
    private double w;
    private int knob = 4;

    public NPlusSlider(NPlusModule module, SliderSetting slider, int o) {
        super(module, o);
        this.slider = slider;
    }

    @Override
    public int gap() {
        return MODULE_GAP;
    }

    @Override
    public void draw() {
        int dark = NPlusRender.clientIntDarker;
        int c = NPlusRender.clientInt;
        int x = this.left() + 4;
        int y = this.top() + 11;
        int xEnd = x + this.module.category.getWidth() - 8;
        int r = x + (int) this.w;
        NPlusThemeModule.SliderStyle style = NPlusThemeModule.sliderStyle();
        float round = (float) NPlusThemeModule.sliderRound.getInput();
        switch (style) {
            case Normal:
                Gui.drawRect(x, y, xEnd, y + 4, dark);
                Gui.drawRect(x, y, r, y + 4, c);
                break;
            case Round:
                NPlusRender.roundedOutline(x, y, xEnd, y + 4, round, 1.0f, dark);
                NPlusRender.roundedOutline(x, y, r, y + 4, round, 1.0f, c);
                break;
            case Outlines:
                NPlusRender.borderedRect(x, y, xEnd, y + 4, 1.0f, dark);
                NPlusRender.borderedRect(x, y, r, y + 4, 2.0f, c);
                break;
            case Vape:
                NPlusRender.rectWithCircle(x, y, r, y + 4, this.knob, c);
                break;
        }
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        mc.fontRendererObj.drawStringWithShadow(this.slider.getName(), x * 2, (this.top() + 3) * 2, c);
        String value = this.slider.getDisplay(false);
        mc.fontRendererObj.drawStringWithShadow(value, (this.left() + 85) * 2 - mc.fontRendererObj.getStringWidth(value) / 2.0f, (this.top() + 3) * 2, c);
        GL11.glPopMatrix();
    }

    @Override
    public void update(int mouseX, int mouseY) {
        super.update(mouseX, mouseY);
        this.knob = this.hovered(mouseX, mouseY) ? 5 : 4;
        int width = this.module.category.getWidth() - 8;
        double min = this.slider.getMin(), max = this.slider.getMax();
        this.w = width * (this.slider.getInput() - min) / (max - min);
        if (this.dragging) {
            double d = Math.min(width, Math.max(0, mouseX - this.x));
            this.slider.setValue(d == 0.0 ? min : d / width * (max - min) + min);
        }
    }

    @Override
    public void mouseDown(int x, int y, int b) {
        if (b == 0 && this.module.expanded && this.hovered(x, y)) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(int x, int y, int b) {
        this.dragging = false;
    }
}
