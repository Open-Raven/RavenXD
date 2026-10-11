package xyz.daikirai.gui;

import xyz.daikirai.module.Module;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.RenderUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class ButtonComponent extends Component {
    public float o;
    public float x;
    public Module mod;
    public double clickFadeMs = 175.0;
    public ModuleComponent p;
    public Color color = new Color(20, 255, 0);
    public ButtonSetting buttonSetting;
    public double toggleFadeMs = 75.0;
    public float y;

    public ButtonComponent(Module mod, ButtonSetting op, ModuleComponent b, float o) {
        this.mod = mod;
        this.buttonSetting = op;
        this.p = b;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.offset;
        this.o = o;
    }

    @Override
    public void setOffset(float n) {
        this.o = n;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        int c;
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        float tx = (this.p.categoryComponent.getX() + 4.0f) * 2.0f;
        float ty = (this.p.categoryComponent.getY() + this.o + 4.0f) * 2.0f;
        if (this.buttonSetting.isMethodButton()) {
            if (this.buttonSetting.clickedAt > 0L) {
                double d = 1.0 - Math.min(1.0, (double) (System.currentTimeMillis() - this.buttonSetting.clickedAt) / this.clickFadeMs);
                if (d == 0.0) {
                    this.buttonSetting.clickedAt = 0L;
                }
                c = RenderUtils.blend(this.color, Color.WHITE, d);
            } else {
                c = -1;
            }
        } else if (this.buttonSetting.isToggled()) {
            if (this.buttonSetting.enabledAt > 0L) {
                double d = 1.0 - Math.min(1.0, (double) (System.currentTimeMillis() - this.buttonSetting.enabledAt) / this.toggleFadeMs);
                if (d == 0.0) {
                    this.buttonSetting.enabledAt = 0L;
                }
                c = RenderUtils.blend(Color.WHITE, this.color, d);
            } else {
                c = this.color.getRGB();
            }
        } else if (this.buttonSetting.disabledAt > 0L) {
            double d = 1.0 - Math.min(1.0, (double) (System.currentTimeMillis() - this.buttonSetting.disabledAt) / this.toggleFadeMs);
            if (d == 0.0) {
                this.buttonSetting.disabledAt = 0L;
            }
            c = RenderUtils.blend(this.color, Color.WHITE, d);
        } else {
            c = -1;
        }
        mc.fontRendererObj.drawString(this.buttonSetting.getLabel(), tx, ty, c, false);
        GL11.glPopMatrix();
    }

    public boolean isHovered(int x, int y) {
        return (float) x > this.x && (float) x < this.x + this.p.categoryComponent.getWidth() && (float) y > this.y && (float) y < this.y + 11.0f;
    }

    @Override
    public void updatePosition(int x, int y) {
        this.x = this.p.categoryComponent.getX();
        this.y = this.p.categoryComponent.getY() + this.o;
    }

    @Override
    public float getHeight() {
        return 12.0f;
    }

    @Override
    public boolean mouseClicked(int x, int y, int b) {
        if (!this.p.opened) {
            return false;
        }
        if (b != 0) {
            return false;
        }
        if (!this.isHovered(x, y)) {
            return false;
        }
        this.buttonSetting.toggle();
        this.mod.guiButtonToggled(this.buttonSetting);
        return true;
    }
}
