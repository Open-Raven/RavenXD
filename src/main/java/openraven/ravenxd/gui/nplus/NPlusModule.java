package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.GuiModule;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.DescriptionSetting;
import openraven.ravenxd.module.setting.Setting;
import openraven.ravenxd.module.setting.SliderSetting;

import java.util.ArrayList;

public class NPlusModule {
    public final Module mod;
    public final NPlusCategory category;
    public final ArrayList<NPlusComp> settings = new ArrayList<NPlusComp>();
    public boolean expanded;
    public int o;

    public NPlusModule(Module mod, NPlusCategory category, int o) {
        this.mod = mod;
        this.category = category;
        this.o = o;
        int y = o + NPlusComp.SETTING_GAP;
        for (Setting s : mod.getSettings()) {
            NPlusComp c;
            if (s instanceof SliderSetting) {
                SliderSetting slider = (SliderSetting) s;
                c = slider.isMode() ? new NPlusMode(this, slider, y) : new NPlusSlider(this, slider, y);
            } else if (s instanceof ButtonSetting) {
                c = new NPlusTick(this, (ButtonSetting) s, y);
            } else if (s instanceof DescriptionSetting) {
                c = new NPlusDesc(this, (DescriptionSetting) s, y);
            } else {
                continue;
            }
            this.settings.add(c);
            y += c.gap();
        }
        this.settings.add(new NPlusBind(this, y));
    }

    public void setComponentStartAt(int n) {
        this.o = n;
        int y = n + NPlusComp.MODULE_GAP;
        for (NPlusComp c : this.settings) {
            c.o = y;
            y += c.gap();
        }
    }

    public int getHeight() {
        int h = NPlusComp.MODULE_GAP;
        if (this.expanded) {
            for (NPlusComp c : this.settings) {
                h += c.gap();
            }
        }
        return h;
    }

    public int getY() {
        return this.category.getY() + this.o;
    }

    public void draw() {
        int x = this.category.getX();
        int y = this.getY();
        boolean on = this.mod.isEnabled();
        NPlusRender.moduleGradient(x, y, x + this.category.getWidth(), y + 15, on ? NPlusRender.purple : NPlusRender.modComp5, on ? NPlusRender.purple : NPlusRender.modComp4);
        int color = on ? NPlusRender.clientInt : NPlusRender.gray;
        String name = GuiModule.lowercase.isToggled() ? this.mod.getName().toLowerCase() : this.mod.getName();
        int tx = x + this.category.getWidth() / 2 - NPlusRender.width(name) / 2;
        if (GuiModule.underline.isToggled()) {
            NPlusRender.underlined(name, tx, y + 4, (int) GuiModule.underlineExpand.getInput(), color);
        } else {
            NPlusRender.text(name, tx, y + 4, color, true);
        }
        if (this.expanded) {
            for (NPlusComp c : this.settings) {
                c.draw();
            }
        }
    }

    public void update(int mx, int my) {
        for (NPlusComp c : this.settings) {
            c.update(mx, my);
        }
    }

    public void mouseDown(int x, int y, int b) {
        if (this.hovered(x, y)) {
            if (b == 0) {
                this.mod.toggle();
                return;
            }
            if (b == 1) {
                this.expanded = !this.expanded;
                this.category.layout();
                return;
            }
        }
        if (!this.expanded) {
            return;
        }
        for (NPlusComp c : this.settings) {
            c.mouseDown(x, y, b);
        }
    }

    public void mouseReleased(int x, int y, int b) {
        for (NPlusComp c : this.settings) {
            c.mouseReleased(x, y, b);
        }
    }

    public void keyTyped(char t, int k) {
        for (NPlusComp c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    public boolean hovered(int x, int y) {
        int cy = this.getY();
        return x > this.category.getX() && x < this.category.getX() + this.category.getWidth() && y > cy && y < cy + NPlusComp.MODULE_GAP;
    }
}
