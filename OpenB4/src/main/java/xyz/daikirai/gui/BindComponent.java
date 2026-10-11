package xyz.daikirai.gui;

import xyz.daikirai.module.GuiModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Theme;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class BindComponent extends Component {
    public Module mod;
    public ModuleComponent p;
    public float o;
    public boolean isBinding;
    public float y;
    public float x;
    public static boolean binding;
    public boolean cannotBind;

    public BindComponent(ModuleComponent b, Module mod, float o) {
        this.p = b;
        this.mod = mod;
        this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
        this.y = b.categoryComponent.getY() + b.offset;
        this.o = o;
        this.cannotBind = !mod.canBeBound();
    }

    public boolean overSetting(int x, int y) {
        return (float) x > this.x && (float) x < this.x + this.p.categoryComponent.getWidth() && (float) y > this.y - 1.0f && (float) y < this.y + 12.0f;
    }

    @Override
    public void setOffset(float n) {
        this.o = n;
    }

    @Override
    public void updatePosition(int x, int y) {
        this.y = this.p.categoryComponent.getY() + this.o;
        this.x = this.p.categoryComponent.getX();
    }

    @Override
    public boolean mouseClicked(int x, int y, int button) {
        if (this.cannotBind) {
            return false;
        }
        if (!this.p.opened) {
            return false;
        }
        if (button > 2 && this.isBinding) {
            this.setBind(button - 100);
            this.isBinding = false;
            binding = false;
            return false;
        }
        if (!this.overSetting(x, y)) {
            return false;
        }
        if (button == 0) {
            this.isBinding = !this.isBinding;
            binding = this.isBinding;
        } else if (button == 1) {
            this.mod.toggleHidden();
        } else {
            return false;
        }
        return true;
    }

    public void drawText(String s, String key) {
        float tx = (this.p.categoryComponent.getX() + 4.0f) * 2.0f;
        float ty = (this.p.categoryComponent.getY() + this.o + 3.0f) * 2.0f;
        if (key != null) {
            s = s + "'" + "§" + "e" + key + "§" + "r'";
        }
        mc.fontRendererObj.drawStringWithShadow(s, tx, ty, this.mod.isHidden() ? Theme.CHERRY.second.getRGB() : Theme.SKY.getRGB(0.0));
    }

    @Override
    public void render(int mouseX, int mouseY) {
        String s;
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        String key = null;
        if (this.cannotBind) {
            s = "Module cannot be bound.";
        } else if (this.isBinding) {
            s = "Press a key" + RenderUtils.dots(300L);
        } else {
            s = "Current bind: ";
            key = keyName(this.mod.getKeycode());
        }
        this.drawText(s, key);
        GL11.glPopMatrix();
    }

    public static String keyName(int n) {
        if (n < 0) {
            return "MB" + (n + 100);
        }
        return Keyboard.getKeyName(n);
    }

    @Override
    public void keyTyped(char t, int keybind) {
        if (this.cannotBind || !this.isBinding) {
            return;
        }
        this.setBind(keybind);
        this.isBinding = false;
        binding = false;
    }

    @Override
    public float getHeight() {
        return 12.0f;
    }

    public void setBind(int keybind) {
        if (keybind == 1) {
            if (!(this.mod instanceof GuiModule)) {
                this.mod.setBind(0);
            }
        } else {
            this.mod.setBind(keybind);
        }
    }
}
