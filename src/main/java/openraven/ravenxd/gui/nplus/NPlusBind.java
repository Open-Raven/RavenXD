package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.gui.BindComponent;
import openraven.ravenxd.module.GuiModule;
import org.lwjgl.opengl.GL11;

public class NPlusBind extends NPlusComp {
    private final boolean cannotBind;
    private boolean binding;

    public NPlusBind(NPlusModule module, int o) {
        super(module, o);
        this.cannotBind = !module.mod.canBeBound();
    }

    @Override
    public void draw() {
        String s;
        if (this.cannotBind) {
            s = " Cannot be bound";
        } else if (this.binding) {
            s = " Bind - Key ";
        } else {
            s = " Bind - " + BindComponent.keyName(this.module.mod.getKeycode()).replace("NONE", "None");
        }
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        mc.fontRendererObj.drawStringWithShadow(s, (this.left() + 4) * 2, (this.top() + 3) * 2, this.module.mod.isHidden() ? NPlusRender.lightRed : NPlusRender.clientIntDarker);
        GL11.glPopMatrix();
    }

    @Override
    public void update(int mouseX, int mouseY) {
        super.update(mouseX, mouseY);
        if (!this.module.expanded) {
            this.setBinding(false);
        }
    }

    @Override
    public void mouseDown(int x, int y, int b) {
        if (this.cannotBind || !this.module.expanded) {
            return;
        }
        if (this.binding && b > 2) {
            this.module.mod.setBind(b - 100);
            this.setBinding(false);
            return;
        }
        if (!this.hovered(x, y)) {
            return;
        }
        if (b == 0) {
            this.setBinding(!this.binding);
        } else if (b == 1) {
            this.module.mod.toggleHidden();
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (!this.binding) {
            return;
        }
        if (k == 1) {
            if (!(this.module.mod instanceof GuiModule)) {
                this.module.mod.setBind(0);
            }
        } else {
            this.module.mod.setBind(k);
        }
        this.setBinding(false);
    }

    private void setBinding(boolean b) {
        if (this.binding != b) {
            this.binding = b;
            BindComponent.binding = b;
        }
    }
}
