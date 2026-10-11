package xyz.daikirai.gui;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.GuiModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.util.Animation;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class CategoryComponent {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public Category categoryName;
    public float iconScale = 0.5f;
    public float titleHeight;
    public float x;
    public boolean dragging;
    public float width;
    public float iconInverse = 1.0f / this.iconScale;
    public boolean closing;
    public float yOffset;
    public ArrayList<Component> modules = new ArrayList<Component>();
    public boolean opened;
    public float moduleY;
    public float targetScroll;
    public Animation scrollAnimation;
    public float scroll;
    public float xOffset;
    public float bigSettingsHeight;
    public float maxHeightRatio = 0.8f;
    public float y;
    public boolean opening;
    public Animation openAnimation;
    public long animationDuration = 400L;

    public CategoryComponent(Category category) {
        this.categoryName = category;
        this.width = 92.0f;
        this.setX(5.0f);
        this.titleHeight = 13.0f;
        this.moduleY = this.titleHeight + 3.0f;
        this.xOffset = 0.0f;
        for (Module mod : ModuleManager.modules) {
            if (mod.moduleCategory() != category) continue;
            this.addModule(mod);
        }
    }

    public void addModule(Module mod) {
        ModuleComponent c = new ModuleComponent(mod, this, this.moduleY);
        this.modules.add(c);
        if (mod.isProfile() || mod.isScript()) {
            ModuleManager.add(mod);
        }
        this.moduleY += c.height;
        this.updateHeight();
    }

    public void removeModule(Module mod, ModuleComponent c) {
        this.modules.remove(c);
        ModuleManager.remove(mod);
        this.moduleY -= c.height;
        this.updateHeight();
    }

    public void setOpened(boolean on) {
        if (on == this.opened) {
            return;
        }
        this.opened = on;
        if (on) {
            this.opening = true;
            this.closing = false;
        } else {
            this.opening = false;
            this.closing = true;
        }
        if (this.openAnimation == null) {
            this.openAnimation = new Animation(this.animationDuration);
        }
        this.openAnimation.start();
    }

    public void mouseReleased() {
        this.overTitle(false);
        for (Component c : this.modules) {
            c.onGuiClosed();
        }
    }

    public float getY() {
        return this.y;
    }

    public void render(FontRenderer renderer, int mouseX, int mouseY) {
        float animated;
        float target;
        this.width = 92.0f;
        boolean active = false;
        boolean lite = GuiModule.liteMode.isToggled();
        if ((this.opened || this.closing) && !this.modules.isEmpty()) {
            animated = 0.0f;
            float y = target = 2.0f;
            for (Component c : this.modules) {
                if (lite && !((ModuleComponent) c).mod.lite) continue;
                target += c.getHeight();
            }
            this.bigSettingsHeight = target;
            target = Math.min(target, (float) RenderUtils.scaledHeight * this.maxHeightRatio);
            if (this.openAnimation != null) {
                float v;
                if (this.opening) {
                    v = this.openAnimation.getValue(animated, target, 2);
                    if (v == target) {
                        this.opening = false;
                    }
                    animated = v;
                } else if (this.closing) {
                    v = target - this.openAnimation.getValue(animated, target, 2);
                    if (v == animated) {
                        this.closing = false;
                    }
                    animated = v;
                } else {
                    animated = target;
                }
            }
            float top = this.y + this.titleHeight + 3.0f;
            int bottom = (int) ((double) top + Math.floor(animated));
            RenderUtils.roundedRectOutlined(this.x - 2.0f, this.y, this.x + this.width + 2.0f, top + animated, 10.0f,
                    GuiModule.categoryFill, GuiModule.outlineLeft, GuiModule.outlineRight);
            GL11.glPushMatrix();
            GL11.glEnable(3089);
            GL11.glScissor((int) ((this.x - 1.0f) * (float) RenderUtils.scaleFactor), mc.displayHeight - bottom * RenderUtils.scaleFactor,
                    (int) ((this.width + 2.0f) * (float) RenderUtils.scaleFactor), (int) (((float) bottom - top) * (float) RenderUtils.scaleFactor));
            for (Component c : this.modules) {
                ModuleComponent m = (ModuleComponent) c;
                if (lite && !m.mod.lite) continue;
                m.render(mouseX, mouseY, y + m.height);
                y += m.getHeight();
            }
            GL11.glDisable(3089);
            GL11.glPopMatrix();
            float max = Math.min(this.bigSettingsHeight, (float) RenderUtils.scaledHeight * this.maxHeightRatio);
            this.targetScroll = Math.max(max - this.bigSettingsHeight, Math.min(0.0f, this.targetScroll));
            if (this.scrollAnimation != null && this.scroll != this.targetScroll) {
                this.scroll = this.scroll > this.targetScroll
                        ? (this.scroll -= this.scrollAnimation.getValue(0.0f, this.scroll - this.targetScroll, 0))
                        : (this.targetScroll == 0.0f
                        ? (this.scroll += this.scrollAnimation.getValue(this.targetScroll, -this.scroll, 0))
                        : this.scrollAnimation.getValue(this.scroll, this.targetScroll, 0));
            }
            this.updateHeight();
            active = true;
        } else if (this.overTitle(mouseX, mouseY)) {
            RenderUtils.roundedRectOutlined(this.x - 2.0f, this.y, this.x + this.width + 2.0f, this.y + this.titleHeight + 3.0f, 10.0f,
                    GuiModule.categoryFill, GuiModule.outlineLeft, GuiModule.outlineRight);
            active = true;
        } else {
            RenderUtils.roundedRectOutlined(this.x - 2.0f, this.y, this.x + this.width + 2.0f, this.y + this.titleHeight + 3.0f, 10.0f,
                    GuiModule.categoryFill, GuiModule.closedOutlineLeft, GuiModule.closedOutlineRight);
        }
        GL11.glPushMatrix();
        float nameX = 13.5f;
        if (this.openAnimation != null) {
            float center = this.getWidth() / 2.0f - (float) mc.fontRendererObj.getStringWidth(this.categoryName.name()) / 2.0f;
            if (this.opening) {
                nameX = this.openAnimation.getValue(nameX, center, 2);
            } else if (this.closing) {
                nameX = nameX + center - this.openAnimation.getValue(nameX, center, 2);
            } else if (this.opened) {
                nameX = center;
            }
        }
        renderer.drawString(this.categoryName.name(), this.x + nameX, this.y + 4.0f, GuiModule.categoryNameColor.rgb, false);
        RenderUtils.resetColor();
        GlStateManager.scale(this.iconScale, this.iconScale, this.iconScale);
        ItemStack icon = active ? this.categoryName.activeIcon : this.categoryName.icon;
        RenderUtils.drawItem(icon, (this.x + 2.0f) * this.iconInverse, (this.y + 4.0f) * this.iconInverse);
        GL11.glPopMatrix();
    }

    public boolean overCategoryIcon(int x, int y) {
        return (float) x >= this.x && (float) x <= this.x + 11.0f && (float) y >= this.y + 2.0f && (float) y <= this.y + this.titleHeight + 1.0f;
    }

    public float getX() {
        return this.x;
    }

    public void overTitle(boolean d) {
        this.dragging = d;
    }

    public void updateHeight() {
        boolean lite = GuiModule.liteMode != null && GuiModule.liteMode.isToggled();
        float y = this.titleHeight + this.scroll + 3.0f;
        this.yOffset = 2.0f;
        for (Component c : this.modules) {
            if (lite && !((ModuleComponent) c).mod.lite) continue;
            c.setOffset(y);
            y += c.getHeight();
            this.yOffset += c.getHeight();
        }
    }

    public void onScroll(int mouseX, int mouseY, int dir) {
        if (!this.isHovered(mouseX, mouseY)) {
            return;
        }
        if (this.scrollAnimation == null) {
            this.scrollAnimation = new Animation(1000.0f);
        }
        this.scrollAnimation.start();
        this.targetScroll = (float) ((double) this.targetScroll + (double) dir * GuiModule.scrollSpeed.getInput());
    }

    public void mousePosition(int mouseX, int mouseY) {
        if (this.dragging) {
            this.setX((float) mouseX - this.xOffset);
            this.setY((float) mouseY - this.dragY);
        }
    }

    public float dragY;

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return this.width;
    }

    public boolean isHovered(int x, int y) {
        return (float) x >= this.x && (float) x <= this.x + this.width && (float) y >= this.y + this.titleHeight && (float) y <= this.y + this.titleHeight + this.yOffset;
    }

    public void setX(float x) {
        this.x = x;
    }

    public boolean overTitle(int x, int y) {
        return (float) x >= this.x && (float) x <= this.x + this.width && (float) y >= this.y && (float) y <= this.y + this.titleHeight;
    }
}
