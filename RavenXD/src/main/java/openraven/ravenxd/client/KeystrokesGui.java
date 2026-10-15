package xyz.daikirai.client;

import xyz.daikirai.util.ClickUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.io.IOException;

public class KeystrokesGui extends GuiScreen {
    private static final String[] COLOR_NAMES = {"White", "Red", "Green", "Blue", "Yellow", "Purple", "Rainbow"};
    public static boolean open;

    private GuiButton modButton, colorButton, mouseButton, outlineButton;
    private boolean dragging = false;
    private int lastX, lastY;

    @Override
    public void initGui() {
        this.modButton = new GuiButton(0, this.width / 2 - 70, this.height / 2 - 28, 140, 20, "Mod: " + (Keystrokes.enabled ? "Enabled" : "Disabled"));
        this.buttonList.add(this.modButton);
        this.colorButton = new GuiButton(1, this.width / 2 - 70, this.height / 2 - 6, 140, 20, "Text color: " + COLOR_NAMES[Keystrokes.color]);
        this.buttonList.add(this.colorButton);
        this.mouseButton = new GuiButton(2, this.width / 2 - 70, this.height / 2 + 16, 140, 20, "Show mouse buttons: " + (Keystrokes.showMouseButtons ? "On" : "Off"));
        this.buttonList.add(this.mouseButton);
        this.outlineButton = new GuiButton(3, this.width / 2 - 70, this.height / 2 + 38, 140, 20, "Outline: " + (Keystrokes.outline ? "On" : "Off"));
        this.buttonList.add(this.outlineButton);
    }

    @Override
    public void drawScreen(int mx, int my, float p) {
        Keystrokes.instance.render();
        super.drawScreen(mx, my, p);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b == this.modButton) {
            Keystrokes.enabled = !Keystrokes.enabled;
            this.modButton.displayString = "Mod: " + (Keystrokes.enabled ? "Enabled" : "Disabled");
        } else if (b == this.colorButton) {
            Keystrokes.color = Keystrokes.color == 6 ? 0 : Keystrokes.color + 1;
            this.colorButton.displayString = "Text color: " + COLOR_NAMES[Keystrokes.color];
        } else if (b == this.mouseButton) {
            Keystrokes.showMouseButtons = !Keystrokes.showMouseButtons;
            this.mouseButton.displayString = "Show mouse buttons: " + (Keystrokes.showMouseButtons ? "On" : "Off");
        } else if (b == this.outlineButton) {
            Keystrokes.outline = !Keystrokes.outline;
            this.outlineButton.displayString = "Outline: " + (Keystrokes.outline ? "On" : "Off");
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button == 0) {
            ClickUtils.onLeftClick();
            int x1 = Keystrokes.x;
            int y1 = Keystrokes.y;
            int x2 = x1 + 74;
            int y2 = y1 + (Keystrokes.showMouseButtons ? 74 : 50);
            if (mx >= x1 && mx <= x2 && my >= y1 && my <= y2) {
                this.dragging = true;
                this.lastX = mx;
                this.lastY = my;
            }
        } else if (button == 1) {
            ClickUtils.onRightClick();
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        this.dragging = false;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        super.mouseClickMove(mx, my, button, time);
        if (!this.dragging) {
            return;
        }
        Keystrokes.x = Keystrokes.x + mx - this.lastX;
        Keystrokes.y = Keystrokes.y + my - this.lastY;
        this.lastX = mx;
        this.lastY = my;
    }

    @Override
    public void onGuiClosed() {
        Keystrokes.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class Command extends CommandBase {
        @Override
        public String getCommandName() {
            return "keystrokesmod";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/keystrokesmod";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            open = true;
        }
    }
}
