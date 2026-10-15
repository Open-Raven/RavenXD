package xyz.daikirai.gui.nplus;

import net.minecraft.client.Minecraft;

public abstract class NPlusComp {
    public static final int MODULE_GAP = 16;
    public static final int SETTING_GAP = 12;
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public final NPlusModule module;
    public int o;
    public int x;
    public int y;

    protected NPlusComp(NPlusModule module, int o) {
        this.module = module;
        this.o = o;
    }

    public void draw() {
    }

    public void update(int mouseX, int mouseY) {
        this.x = this.module.category.getX();
        this.y = this.module.category.getY() + this.o;
    }

    public void mouseDown(int x, int y, int b) {
    }

    public void mouseReleased(int x, int y, int b) {
    }

    public void keyTyped(char t, int k) {
    }

    public int gap() {
        return SETTING_GAP;
    }

    protected boolean hovered(int mx, int my) {
        return mx > this.x && mx < this.x + this.module.category.getWidth() && my > this.y && my < this.y + this.gap();
    }

    protected int left() {
        return this.module.category.getX();
    }

    protected int top() {
        return this.module.category.getY() + this.o;
    }
}
