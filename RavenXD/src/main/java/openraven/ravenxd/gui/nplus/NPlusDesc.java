package xyz.daikirai.gui.nplus;

import xyz.daikirai.module.setting.DescriptionSetting;
import org.lwjgl.opengl.GL11;

public class NPlusDesc extends NPlusComp {
    private final DescriptionSetting desc;

    public NPlusDesc(NPlusModule module, DescriptionSetting desc, int o) {
        super(module, o);
        this.desc = desc;
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        mc.fontRendererObj.drawString(this.desc.getDesc(), (this.left() + 4) * 2, this.top() * 2 + 8, NPlusRender.clientInt, true);
        GL11.glPopMatrix();
    }
}
