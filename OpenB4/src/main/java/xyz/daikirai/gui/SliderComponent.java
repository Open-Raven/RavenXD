package xyz.daikirai.gui;

import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.RenderUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    public boolean heldDown;
    public float o;
    public float width;
    public SliderSetting sliderSetting;
    public float y;
    public float x;
    public int maxBar = 84;
    public ModuleComponent p;

    public SliderComponent(SliderSetting sliderSetting, ModuleComponent b, float o) {
        this.sliderSetting = sliderSetting;
        this.p = b;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.offset;
        this.o = o;
    }

    @Override
    public float getHeight() {
        return 17.0f;
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
        this.heldDown = false;
    }

    @Override
    public void updatePosition(int mousePosX, int mousePosY) {
        float w = this.p.categoryComponent.getWidth();
        this.x = this.p.categoryComponent.getX();
        this.y = this.p.categoryComponent.getY() + this.o;
        double min = this.sliderSetting.getMin(), max = this.sliderSetting.getMax();
        if (this.heldDown) {
            double d = Math.min(w - 8.0f, Math.max(0.0f, (float) (mousePosX - 2) - this.x));
            double n = d / (double) (w - 8.0f) * (max - min) + min;
            this.width = (float) ((double) (w - 8.0f) * (n - min) / (max - min));
            this.sliderSetting.setValue(roundToPrecision(n, 2));
            this.sliderSetting.setVisualInput(n);
        } else {
            double v = this.sliderSetting.getVisualInput();
            this.width = (float) ((double) (w - 8.0f) * (v - min) / (max - min));
        }
    }

    @Override
    public boolean mouseClicked(int x, int y, int b) {
        if (!this.p.opened) {
            return false;
        }
        if (b != 0) {
            return false;
        }
        if (!this.isLeftHalf(x, y) && !this.isRightHalf(x, y)) {
            return false;
        }
        this.heldDown = true;
        return true;
    }

    public static double roundToPrecision(double v, int p) {
        if (p < 0) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(p, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public void setOffset(float n) {
        this.o = n;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        CategoryComponent c = this.p.categoryComponent;
        RenderUtils.roundedRect(c.getX() + 4.0f, c.getY() + this.o + 11.0f, c.getX() + c.getWidth() - 4.0f, c.getY() + this.o + 15.0f, 4.0f, -12302777);
        float l = c.getX() + 4.0f;
        float r = c.getX() + 4.0f + this.width;
        if (r - l > (float) this.maxBar) {
            r = l + (float) this.maxBar;
        } else if (r < l) {
            r = l;
        }
        RenderUtils.roundedRect(l, c.getY() + this.o + 11.0f, r, c.getY() + this.o + 15.0f, 4.0f,
                Color.getHSBColor((float) (System.currentTimeMillis() % 11000L) / 11000.0f, 0.75f, 0.9f).getRGB());
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        mc.fontRendererObj.drawStringWithShadow(this.sliderSetting.getName() + ": " + this.sliderSetting.getDisplay(true),
                (float) ((int) ((c.getX() + 4.0f) * 2.0f)), (float) ((int) ((c.getY() + this.o + 3.0f) * 2.0f)), -1);
        GL11.glPopMatrix();
    }

    public boolean isLeftHalf(int x, int y) {
        float w = this.p.categoryComponent.getWidth();
        return (float) x > this.x && (float) x < this.x + (w / 2.0f + 1.0f) && (float) y > this.y && (float) y < this.y + 16.0f;
    }

    public boolean isRightHalf(int x, int y) {
        float w = this.p.categoryComponent.getWidth();
        return (float) x > this.x + w / 2.0f && (float) x < this.x + w && (float) y > this.y && (float) y < this.y + 16.0f;
    }

    @Override
    public void onGuiClosed() {
        this.heldDown = false;
    }
}
