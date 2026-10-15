package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.GuiModule;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.NPlusThemeModule;
import openraven.ravenxd.util.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;
import java.util.ArrayList;

public class NPlusCategory {
    public static final int WIDTH = 92;
    public static final int TITLE_HEIGHT = 10;

    public final Category category;
    public final ArrayList<NPlusModule> modules = new ArrayList<NPlusModule>();
    public boolean dragging;
    public int dragX, dragY;
    private boolean opened, pinned;
    private int x = 5, y = 5, offset;

    public NPlusCategory(Category category) {
        this.category = category;
    }

    // refresh per open
    public void rebuild() {
        ArrayList<String> expanded = new ArrayList<String>();
        for (NPlusModule m : this.modules) {
            if (m.expanded) expanded.add(m.mod.getName());
        }
        this.modules.clear();
        boolean lite = GuiModule.liteMode.isToggled();
        int o = TITLE_HEIGHT + 3;
        for (Module mod : ModuleManager.modules) {
            if (mod.moduleCategory() != this.category || lite && !mod.lite) continue;
            NPlusModule m = new NPlusModule(mod, this, o);
            m.expanded = expanded.contains(mod.getName());
            this.modules.add(m);
            o += NPlusComp.MODULE_GAP;
        }
        this.layout();
    }

    public void layout() {
        int o = TITLE_HEIGHT + 3;
        for (NPlusModule m : this.modules) {
            m.setComponentStartAt(o);
            o += m.getHeight();
        }
    }

    public void draw() {
        int x = this.getX(), y = this.getY();
        if (this.opened) {
            int height = 0;
            for (NPlusModule m : this.modules) {
                height += m.getHeight();
            }
            int off = (int) NPlusThemeModule.categoryOffset.getInput();
            int bottom = y + TITLE_HEIGHT + height + off + 2;
            NPlusRender.roundedRect(x - off, y, x + WIDTH + off, bottom, 3.0f, NPlusRender.categoryFill);
            NPlusRender.roundedOutline(x - off, y, x + WIDTH + off, bottom, 0.0f, 2.62f, NPlusRender.clientIntDarker);
            Gui.drawRect(x - 1, y, x + WIDTH + 1, y + TITLE_HEIGHT + 4, NPlusRender.clientIntDarker);
        } else {
            NPlusRender.roundedRect(x, y, x + WIDTH, y + TITLE_HEIGHT + 4, 3.0f, NPlusRender.clientIntDarker);
            NPlusRender.roundedOutline(x, y, x + WIDTH, y + TITLE_HEIGHT + 4, 0.0f, 2.62f, NPlusRender.clientInt);
        }
        NPlusRender.text(this.category.name(), x + 15, y + 3, NPlusRender.clientInt, false);
        Color c = NPlusRender.clientColor;
        int arrowX = x + 80, arrowY = y + 2;
        NPlusRender.icon("pin", arrowX - 15, arrowY, 10, 10, this.pinned ? new Color(NPlusRender.clientIntDarker) : c);
        String icon = icon(this.category);
        if (icon != null) {
            NPlusRender.icon(icon, x + 2, y + 2, 10, 10, c);
        } else {
            float s = 0.625f;
            GlStateManager.pushMatrix();
            GlStateManager.scale(s, s, s);
            RenderUtils.drawItem(this.category.icon, (x + 2) / s, (y + 2) / s);
            GlStateManager.popMatrix();
        }
        NPlusRender.icon(this.opened ? "open" : "close", arrowX, arrowY, 10, 10, c);
        if (this.opened) {
            for (NPlusModule m : this.modules) {
                m.draw();
            }
        }
    }

    private static String icon(Category c) {
        switch (c) {
            case combat: return "combat";
            case movement: return "move";
            case player: return "player";
            case render: return "render";
            case client: return "client";
            default: return null;
        }
    }

    public void update(int mx, int my) {
        if (this.dragging) {
            this.setX(mx - this.dragX);
            this.setY(my - this.dragY);
        }
        if (this.opened) {
            for (NPlusModule m : this.modules) {
                m.update(mx, my);
            }
        }
    }

    public void scroll(float amount) {
        this.offset = (int) ((float) this.offset + amount);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y + this.offset;
    }

    public int getWidth() {
        return WIDTH;
    }

    public void setX(int x) {
        if (!this.pinned) this.x = x;
    }

    public void setY(int y) {
        if (!this.pinned) this.y = y - this.offset;
    }

    // Position without the scroll offset, for saving.
    public int rawY() {
        return this.y;
    }

    public void load(int x, int y, boolean pinned) {
        this.x = x;
        this.y = y;
        this.pinned = pinned;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public void setOpened(boolean o) {
        this.opened = o;
    }

    public boolean isPinned() {
        return this.pinned;
    }

    public void setPinned(boolean p) {
        this.pinned = p;
    }

    public boolean overArrow(int mx, int my) {
        return mx >= this.getX() + 77 && mx <= this.getX() + WIDTH && my >= this.getY() + 2 && my <= this.getY() + TITLE_HEIGHT + 1;
    }

    public boolean overPin(int mx, int my) {
        return mx >= this.getX() + 64 && mx < this.getX() + 77 && my >= this.getY() + 2 && my <= this.getY() + TITLE_HEIGHT + 1;
    }

    public boolean overTitle(int mx, int my) {
        return mx >= this.getX() && mx <= this.getX() + WIDTH && my >= this.getY() && my <= this.getY() + TITLE_HEIGHT;
    }
}
