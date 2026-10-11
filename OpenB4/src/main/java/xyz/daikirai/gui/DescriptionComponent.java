package xyz.daikirai.gui;

import xyz.daikirai.module.setting.DescriptionSetting;
import xyz.daikirai.util.Theme;
import org.lwjgl.opengl.GL11;

public class DescriptionComponent extends Component {
    public DescriptionSetting desc;
    public ModuleComponent p;
    public float o;

    public DescriptionComponent(DescriptionSetting desc, ModuleComponent b, float o) {
        this.desc = desc;
        this.p = b;
        this.o = o;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        mc.fontRendererObj.drawString(this.desc.getDesc(), (this.p.categoryComponent.getX() + 4.0f) * 2.0f,
                (this.p.categoryComponent.getY() + this.o + 4.0f) * 2.0f, Theme.SKY.getRGB(0.0), true);
        GL11.glPopMatrix();
    }

    @Override
    public void setOffset(float n) {
        this.o = n;
    }

    @Override
    public float getHeight() {
        return 12.0f;
    }
}
