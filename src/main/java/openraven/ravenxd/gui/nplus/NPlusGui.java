package openraven.ravenxd.gui.nplus;

import openraven.ravenxd.client.Account;
import openraven.ravenxd.client.Chat;
import openraven.ravenxd.client.Profiles;
import openraven.ravenxd.client.Sounds;
import openraven.ravenxd.gui.BindComponent;
import openraven.ravenxd.gui.Waifu;
import openraven.ravenxd.gui.ClickGui;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.GuiModule;
import openraven.ravenxd.module.HudModule;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.GuiModule;
import openraven.ravenxd.util.Animation;
import openraven.ravenxd.util.Blur;
import openraven.ravenxd.util.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;

public class NPlusGui extends GuiScreen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static NPlusGui instance;

    public final ArrayList<NPlusCategory> categories = new ArrayList<NPlusCategory>();
    public final NPlusConsole console = new NPlusConsole();
    private Animation backgroundAnimation;

    public NPlusGui() {
        int y = 5;
        for (Category c : Category.values()) {
            NPlusCategory cat = new NPlusCategory(c);
            cat.load(5, y, false);
            this.categories.add(cat);
            y += 20;
        }
        this.console.y = y;
    }

    public NPlusCategory get(String name) {
        for (NPlusCategory c : this.categories) {
            if (c.category.name().equals(name)) return c;
        }
        return null;
    }

    public void resetPositions() {
        int y = 5;
        for (NPlusCategory c : this.categories) {
            c.load(5, y, false);
            c.setOpened(false);
            for (NPlusModule m : c.modules) {
                m.expanded = false;
            }
            c.layout();
            y += 20;
        }
        this.console.x = 5;
        this.console.y = y;
    }

    public void initMain() {
        ClickGui.openedAt = System.currentTimeMillis();
        (this.backgroundAnimation = new Animation(500.0f)).start();
        for (NPlusCategory c : this.categories) {
            c.rebuild();
        }
    }

    @Override
    public void drawScreen(int x, int y, float p) {
        if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            HudModule.render();
        }
        float fade = this.backgroundAnimation.getValue(0.0f, 0.7f, 2);
        if (GuiModule.backgroundBlur.getInput() > 0.0) {
            Blur.get().startStencil();
            Gui.drawRect(0, 0, RenderUtils.scaledWidth, RenderUtils.scaledHeight, -1);
            Blur.get().render(2, GuiModule.blurStrength * fade / 0.7f);
        }
        if (GuiModule.darkBackground.isToggled()) {
            drawRect(0, 0, this.width, this.height, (int) (fade * 255.0f) << 24);
        }
        String info = "[ " + Account.NAME + (Profiles.current != null ? " | Profile: " + Profiles.current.name : "") + " ]";
        NPlusRender.text(info, 4, this.height - 3 - this.fontRendererObj.FONT_HEIGHT, NPlusRender.astolfo(10, 14, 4890.0f), !GuiModule.font());
        int rainbow = NPlusRender.rainbow(2L, 1350L);
        if (GuiModule.nplusLogo.isToggled()) {
            int sw = this.width / 2, sh = this.height / 4;
            NPlusRender.icon("logo", sw - 95, sh + 35, 200, 60, Color.white.darker());
            NPlusRender.icon("feather", sw + 80, sh + 70, 35, 35, new Color(rainbow).darker());
        }
        // The Raven XD title, like the default style's watermark.
        if (!GuiModule.hideWatermark.isToggled() && !GuiModule.nplusLogo.isToggled()) {
            ClickGui.instance.drawTitle(this.fontRendererObj, this.width / 2.0f, this.height / 4.0f);
        }
        for (NPlusCategory c : this.categories) {
            c.draw();
            c.update(x, y);
        }
        Waifu.render(x, y, this.width, this.height);
        String time = time();
        if (GuiModule.clock.isToggled()) {
            NPlusRender.text(time, 8, 5, rainbow, true);
        }
        if (GuiModule.date.isToggled()) {
            boolean clock = GuiModule.clock.isToggled();
            String date = (clock ? " | " : "") + DATE.format(LocalDateTime.now());
            NPlusRender.text(date, clock ? 8 + NPlusRender.width(time) : 5, 5, rainbow, true);
        }
        RenderUtils.resetColor();
        if (!GuiModule.hidePlayerModel.isToggled() && this.mc.thePlayer != null) {
            GuiInventory.drawEntityOnScreen(this.width + 15 - this.backgroundAnimation.getValueInt(0, 40, 2), this.height - 19 - this.fontRendererObj.FONT_HEIGHT, 40,
                    (float) (this.width - 25 - x), (float) (this.height - 50 - y), this.mc.thePlayer);
        }
        if (GuiModule.commandPrompt.isToggled()) {
            this.console.update(x, y);
            this.console.draw();
        }
    }

    private static String time() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
    }

    @Override
    public void mouseClicked(int x, int y, int b) throws IOException {
        if (Waifu.mouseClicked(x, y, b, this.width, this.height)) {
            return;
        }
        if (GuiModule.commandPrompt.isToggled()) {
            this.console.mouseDown(x, y, b);
            if (this.console.over(x, y)) {
                return;
            }
        }
        for (NPlusCategory c : this.categories) {
            if (b == 0) {
                if (c.overArrow(x, y)) {
                    c.setOpened(!c.isOpened());
                    GuiModule.playCategorySound(c.isOpened());
                    return;
                }
                if (c.overPin(x, y)) {
                    c.setPinned(!c.isPinned());
                    return;
                }
                if (c.overTitle(x, y)) {
                    c.dragging = true;
                    c.dragX = x - c.getX();
                    c.dragY = y - c.getY();
                    return;
                }
            }
            if (!c.isOpened()) continue;
            for (NPlusModule m : c.modules) {
                m.mouseDown(x, y, b);
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int b) {
        Waifu.mouseReleased();
        this.console.mouseReleased();
        if (b != 0) {
            return;
        }
        for (NPlusCategory c : this.categories) {
            c.dragging = false;
            if (!c.isOpened()) continue;
            for (NPlusModule m : c.modules) {
                m.mouseReleased(x, y, b);
            }
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (k == 1 && !BindComponent.binding) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (GuiModule.commandPrompt.isToggled() && this.console.keyTyped(t, k)) {
            return;
        }
        if (k == ModuleManager.gui.getKeycode() && !BindComponent.binding) {
            this.mc.displayGuiScreen(null);
            return;
        }
        for (NPlusCategory c : this.categories) {
            if (!c.isOpened()) continue;
            for (NPlusModule m : c.modules) {
                m.keyTyped(t, k);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        float amount = (float) Mouse.getEventDWheel() * 0.2f;
        if (amount != 0.0f) {
            for (NPlusCategory c : this.categories) {
                c.scroll(amount);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        GuiModule.restoreScale();
        BindComponent.binding = false;
        for (NPlusCategory c : this.categories) {
            c.dragging = false;
        }
        this.console.mouseReleased();
        for (Module m : ModuleManager.modules) {
            m.guiUpdate();
        }
        if (GuiModule.saveCategoryPositions.isToggled()) {
            openraven.ravenxd.client.ClientConfig.save();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
