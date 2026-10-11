package xyz.daikirai.gui;

import xyz.daikirai.module.GuiModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.DescriptionSetting;
import xyz.daikirai.module.setting.Setting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Animation;
import xyz.daikirai.util.RenderUtils;

import java.awt.Color;
import java.util.ArrayList;

public class ModuleComponent extends Component {
    public long hoverEndedAt;
    public CategoryComponent categoryComponent;
    public boolean opening;
    public boolean wasHovered;
    public Animation animation;
    public long animationDuration = 200L;
    public double hoverFadeMs = 150.0;
    public boolean opened;
    public boolean closing;
    public float height = 16.0f;
    public ArrayList<Component> settings;
    public float yPos;
    public float offset;
    public boolean hovered;
    public double colorFadeMs = 75.0;
    public Module mod;

    public ModuleComponent(Module mod, CategoryComponent p, float o) {
        this.mod = mod;
        this.categoryComponent = p;
        this.offset = o;
        this.settings = new ArrayList<Component>();
        float y = o + 12.0f;
        if (!mod.getSettings().isEmpty()) {
            for (Setting v : mod.getSettings()) {
                Component c = null;
                if (v instanceof SliderSetting) {
                    c = new SliderComponent((SliderSetting) v, this, y);
                } else if (v instanceof ButtonSetting) {
                    c = new ButtonComponent(mod, (ButtonSetting) v, this, y);
                } else if (v instanceof DescriptionSetting) {
                    c = new DescriptionComponent((DescriptionSetting) v, this, y);
                }
                if (c == null) continue;
                y += c.getHeight();
                this.settings.add(c);
            }
        }
        this.settings.add(new BindComponent(this, mod, y));
    }

    public float top() {
        return this.categoryComponent.getY() + this.offset - 1.0f;
    }

    public float bottom() {
        return this.categoryComponent.getY() + 16.0f + this.offset;
    }

    public float left() {
        return this.categoryComponent.getX() - 1.0f;
    }

    public float right() {
        return this.categoryComponent.getX() + this.categoryComponent.getWidth() + 1.0f;
    }

    @Override
    public void setOffset(float n) {
        this.offset = this.yPos = n;
        float y = this.offset + 16.0f;
        for (Component c : this.settings) {
            c.setOffset(y);
            y += c.getHeight();
        }
    }

    @Override
    public void onGuiClosed() {
        if (!this.opened && !this.closing) {
            return;
        }
        for (Component c : this.settings) {
            c.onGuiClosed();
        }
    }

    public void toggleOpen() {
        this.opened = !this.opened;
        if (this.opened) {
            this.opening = true;
            this.closing = false;
        } else {
            this.opening = false;
            this.closing = true;
        }
        if (this.animation == null) {
            this.animation = new Animation(this.animationDuration);
        }
        this.animation.start();
        this.categoryComponent.updateHeight();
    }

    @Override
    public void mouseReleased(int x, int y, int m) {
        if (!this.opened && !this.closing) {
            return;
        }
        for (Component c : this.settings) {
            c.mouseReleased(x, y, m);
        }
    }

    public boolean isHovered(int x, int y) {
        return (float) x > this.left() && (float) x < this.right() && (float) y > this.top() && (float) y < this.bottom();
    }

    @Override
    public float getHeight() {
        float h = this.height;
        if (this.opened || this.closing) {
            for (Component c : this.settings) {
                h += c.getHeight();
            }
            if (this.animation != null) {
                if (this.opening) {
                    float v = this.animation.getValue(this.height, h, 2);
                    if (v == h) {
                        this.opening = false;
                    }
                    h = v;
                } else if (this.closing) {
                    float v = this.height + (h - this.animation.getValue(this.height, h, 2));
                    if (v == this.height) {
                        this.closing = false;
                    }
                    h = v;
                }
            }
        }
        return h;
    }

    @Override
    public boolean mouseClicked(int x, int y, int b) {
        if (this.categoryComponent.opening || this.categoryComponent.closing) {
            return false;
        }
        if (this.hovered) {
            if (b == 0) {
                this.mod.toggle();
                return true;
            }
            if (b == 1) {
                this.toggleOpen();
                return true;
            }
        }
        if (this.opening || this.closing) {
            return false;
        }
        for (Component c : this.settings) {
            if (!c.mouseClicked(x, y, b)) continue;
            return true;
        }
        return false;
    }

    @Override
    public void updatePosition(int x, int y) {
        for (Component c : this.settings) {
            c.updatePosition(x, y);
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (!this.opened && !this.closing) {
            return;
        }
        for (Component c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    public void render(int mouseX, int mouseY, float y) {
        int color;
        double d;
        if (this.mod.isEnabled()) {
            if (this.mod.enabledAt > 0L) {
                d = 1.0 - Math.min(1.0, (double) (System.currentTimeMillis() - this.mod.enabledAt) / this.colorFadeMs);
                if (d == 0.0) {
                    this.mod.enabledAt = 0L;
                }
                color = RenderUtils.blend(GuiModule.disabledColor.color, GuiModule.enabledColor.color, d);
            } else {
                color = GuiModule.enabledColor.rgb;
            }
        } else if (this.mod.isProfile() && this.mod.profile == xyz.daikirai.client.Profiles.current) {
            color = xyz.daikirai.client.Profiles.modified ? GuiModule.scriptColor.rgb : GuiModule.enabledColor.rgb;
        } else if (this.mod.isScript() && this.mod.script.failed || this.mod.getName().equals("IRC")) {
            color = GuiModule.errorColor.rgb;
        } else if (this.mod.disabledAt > 0L) {
            d = 1.0 - Math.min(1.0, (double) (System.currentTimeMillis() - this.mod.disabledAt) / this.colorFadeMs);
            if (d == 0.0) {
                this.mod.disabledAt = 0L;
            }
            color = RenderUtils.blend(GuiModule.enabledColor.color, GuiModule.disabledColor.color, d);
        } else {
            color = GuiModule.disabledColor.rgb;
        }
        this.wasHovered = this.hovered;
        this.hovered = this.isHovered(mouseX, mouseY);
        int alpha;
        if (this.hovered) {
            alpha = 90;
        } else {
            if (this.wasHovered) {
                this.hoverEndedAt = System.currentTimeMillis();
            }
            if (this.hoverEndedAt > 0L) {
                double f = Math.max(0.0, 1.0 - (double) (System.currentTimeMillis() - this.hoverEndedAt) / this.hoverFadeMs);
                if (f == 0.0) {
                    this.hoverEndedAt = 0L;
                }
                alpha = RenderUtils.clampAlpha((int) (f * (double) 90));
            } else {
                alpha = 0;
            }
        }
        if (alpha > 0) {
            int bg = RenderUtils.withAlpha(Color.black.getRGB(), alpha);
            RenderUtils.rect(this.left(), this.top() + 1.0f, this.right(), this.bottom(), bg);
        }
        float x = this.categoryComponent.getX() + this.categoryComponent.getWidth() / 2.0f - (float) mc.fontRendererObj.getStringWidth(this.mod.getName()) / 2.0f;
        float ty = this.categoryComponent.getY() + this.yPos + 4.0f;
        mc.fontRendererObj.drawStringWithShadow(this.mod.getName(), x, ty, color);
        if ((this.opened || this.closing) && !this.settings.isEmpty()) {
            float end = y - this.height + this.getHeight();
            for (Component c : this.settings) {
                if (y + c.getHeight() - 4.0f > end) break;
                c.render(mouseX, mouseY);
                y += c.getHeight();
            }
        }
    }
}
