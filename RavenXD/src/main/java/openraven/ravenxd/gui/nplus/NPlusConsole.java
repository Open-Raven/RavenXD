package xyz.daikirai.gui.nplus;

import xyz.daikirai.gui.Console;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

// nplus "command prompt"
public class NPlusConsole {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int BAR_HEIGHT = 13, BORDER = 1, RESIZE = 6, MIN_WIDTH = 92, MIN_HEIGHT = 37;
    private static final int TEXT_COLOR = 0xFF20C20E;

    public int x = 5, y, width = 138, height = 103;
    public boolean opened;
    private boolean focused, dragging, resizing;
    private int dragStartX, dragStartY, windowStartX, windowStartY;
    private String input = "";
    private int cursorBack;

    public void draw() {
        FontRenderer fr = mc.fontRendererObj;
        double textSize = BAR_HEIGHT * 0.65;
        int margin = (int) ((BAR_HEIGHT - textSize) * 0.8);
        float sc = (float) (textSize / fr.FONT_HEIGHT);
        Gui.drawRect(this.x, this.y, this.x + this.width, this.y + BAR_HEIGHT, NPlusRender.command1);
        if (this.opened) {
            Gui.drawRect(this.x, this.y + BAR_HEIGHT, this.x + this.width, this.y + this.height, NPlusRender.command3);
            Gui.drawRect(this.x, this.y + BAR_HEIGHT, this.x + BORDER, this.y + this.height, NPlusRender.command1);
            Gui.drawRect(this.x + this.width, this.y + BAR_HEIGHT, this.x + this.width - BORDER, this.y + this.height, NPlusRender.command1);
            Gui.drawRect(this.x, this.y + this.height - BORDER, this.x + this.width, this.y + this.height, NPlusRender.command1);
            Gui.drawRect(this.x + this.width - RESIZE, this.y + this.height - RESIZE - 1, this.x + this.width - 1, this.y + this.height - 1, NPlusRender.babyBlue);
        }
        GL11.glPushMatrix();
        GL11.glScalef(sc, sc, sc);
        fr.drawString("Command Prompt", (int) ((this.x + margin) / sc), (int) ((this.y + margin) / sc), -16220160);
        String button = this.opened ? "-" : "+";
        fr.drawString(button, (int) ((this.x + this.width - margin) / sc) - fr.getStringWidth(button), (int) ((this.y + margin) / sc), -1);
        if (this.opened) {
            if (Console.firstOpen) {
                Console.firstOpen = false;
                Console.print("Welcome,", 0);
                Console.print("Use \"help\" for help.", 0);
            }
            int textWidth = (int) ((this.width - 2 * (margin + BORDER)) / sc);
            int lineHeight = (int) ((textSize + margin) / sc);
            int maxLines = Math.max(1, (this.height - BAR_HEIGHT - 2 * margin - BORDER) / (int) (textSize + margin));
            List<String> lines = new ArrayList<String>();
            for (String s : new ArrayList<String>(Console.lines)) {
                lines.addAll(fr.listFormattedStringToWidth(s.replace('&', '§'), textWidth));
            }
            String typed = "/ " + this.input.substring(0, this.input.length() - this.cursorBack);
            List<String> inputLines = fr.listFormattedStringToWidth("/ " + this.input, textWidth);
            lines.addAll(inputLines);
            int start = Math.max(0, lines.size() - maxLines);
            int tx = (int) ((this.x + margin + BORDER) / sc);
            int ty = (int) ((this.y + BAR_HEIGHT + margin) / sc);
            for (int i = start; i < lines.size(); ++i) {
                fr.drawString(lines.get(i), tx, ty + (i - start) * lineHeight, TEXT_COLOR);
            }
            if (this.focused && System.currentTimeMillis() / 500L % 2L == 0L) {
                List<String> typedLines = fr.listFormattedStringToWidth(typed, textWidth);
                int cx = tx + fr.getStringWidth(typedLines.isEmpty() ? "" : typedLines.get(typedLines.size() - 1));
                int cy = ty + (lines.size() - inputLines.size() + Math.max(0, typedLines.size() - 1) - start) * lineHeight;
                Gui.drawRect(cx, cy, cx + 1, cy + fr.FONT_HEIGHT, TEXT_COLOR);
            }
        }
        GL11.glPopMatrix();
    }

    public void update(int mx, int my) {
        if (this.dragging) {
            this.x = this.windowStartX + mx - this.dragStartX;
            this.y = this.windowStartY + my - this.dragStartY;
        } else if (this.resizing) {
            this.width = Math.max(mx, this.x + MIN_WIDTH) - this.x;
            this.height = Math.max(my, this.y + MIN_HEIGHT) - this.y;
        }
    }

    public void mouseDown(int mx, int my, int b) {
        this.focused = false;
        if (this.overToggle(mx, my) && b == 0) {
            this.opened = !this.opened;
        } else if (this.overBar(mx, my)) {
            if (b == 0) {
                this.dragging = true;
                this.dragStartX = mx;
                this.dragStartY = my;
                this.windowStartX = this.x;
                this.windowStartY = this.y;
            } else if (b == 1) {
                this.opened = !this.opened;
            }
        } else if (this.opened && this.overResize(mx, my) && b == 0) {
            this.resizing = true;
        } else if (this.overWindow(mx, my) && b == 0) {
            this.focused = true;
        }
    }

    public void mouseReleased() {
        this.dragging = false;
        this.resizing = false;
    }

    public boolean keyTyped(char t, int k) {
        if (!this.focused || !this.opened) {
            return false;
        }
        int split = this.input.length() - this.cursorBack;
        String before = this.input.substring(0, split), after = this.input.substring(split);
        if (k == 28) {
            if (!this.input.isEmpty()) {
                Console.print("/ " + this.input, 0);
                Console.handle(this.input);
            }
            this.input = "";
            this.cursorBack = 0;
        } else if (k == 14) {
            if (!before.isEmpty()) {
                this.input = before.substring(0, before.length() - 1) + after;
            }
        } else if (k == 211) {
            if (!after.isEmpty()) {
                this.input = before + after.substring(1);
                --this.cursorBack;
            }
        } else if (k == 203) {
            if (this.cursorBack < this.input.length()) ++this.cursorBack;
        } else if (k == 205) {
            if (this.cursorBack > 0) --this.cursorBack;
        } else if (ChatAllowedCharacters.isAllowedCharacter(t)) {
            this.input = before + t + after;
        }
        return true;
    }

    public boolean over(int mx, int my) {
        return this.opened ? this.overWindow(mx, my) : this.overBar(mx, my);
    }

    private boolean overBar(int mx, int my) {
        return mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + BAR_HEIGHT;
    }

    private boolean overWindow(int mx, int my) {
        return this.opened && mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + this.height;
    }

    private boolean overResize(int mx, int my) {
        return mx >= this.x + this.width - RESIZE && mx <= this.x + this.width && my >= this.y + this.height - RESIZE && my <= this.y + this.height;
    }

    private boolean overToggle(int mx, int my) {
        return mx >= this.x + this.width - BAR_HEIGHT && mx <= this.x + this.width && my >= this.y && my <= this.y + BAR_HEIGHT;
    }
}
