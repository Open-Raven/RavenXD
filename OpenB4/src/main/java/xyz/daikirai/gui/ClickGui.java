package xyz.daikirai.gui;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.CommandLineModule;
import xyz.daikirai.module.GuiModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.util.Animation;
import xyz.daikirai.util.Blur;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ClickGui extends GuiScreen {
    public static ClickGui instance;
    public static long openedAt;
    public static ArrayList<CategoryComponent> categories;

    public Timer timer;
    public Animation watermarkAnimation;
    public Animation backgroundAnimation;
    public Animation unusedAnimation;
    public Animation delayedAnimation;
    public ScaledResolution sr;
    public GuiButtonExt commandSend;
    public GuiTextField commandLineInput;

    public ClickGui() {
        categories = new ArrayList<CategoryComponent>();
        float y = 5.0f;
        for (Category c : Category.values()) {
            CategoryComponent f = new CategoryComponent(c);
            f.setY(y);
            c.setManager(f);
            categories.add(f);
            y += 20.0f;
        }
    }

    public void initMain() {
        openedAt = System.currentTimeMillis();
        (this.backgroundAnimation = new Animation(500.0f)).start();
        (this.unusedAnimation = new Animation(1000.0f)).start();
        (this.watermarkAnimation = new Animation(500.0f)).start();
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ClickGui.this.delayedAnimation = new Animation(650.0f);
                ClickGui.this.delayedAnimation.start();
                ClickGui.this.timer.cancel();
                ClickGui.this.timer.purge();
                ClickGui.this.timer = null;
            }
        }, 650L);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.sr = new ScaledResolution(this.mc);
        (this.commandLineInput = new GuiTextField(1, this.mc.fontRendererObj, 22, this.height - 100, 150, 20)).setMaxStringLength(256);
        this.buttonList.add(this.commandSend = new GuiButtonExt(2, 22, this.height - 70, 150, 20, "Send"));
        this.commandSend.visible = CommandLineModule.open;
    }

    @Override
    public void drawScreen(int x, int y, float p) {
        int bg;
        Module hud = ModuleManager.get("HUD");
        if (hud != null && hud.isEnabled()) {
            xyz.daikirai.module.HudModule.render();
        }
        int alpha = (int) (this.backgroundAnimation.getValue(0.0f, 0.75f, 2) * 255.0f);
        if (GuiModule.darkBackground.isToggled()) {
            bg = 0x101010 | alpha << 24;
            drawRect(0, 0, this.width, this.height, bg);
        }
        if (GuiModule.backgroundBlur.getInput() > 0.0) {
            Blur.get().startStencil();
            Gui.drawRect(0, 0, RenderUtils.scaledWidth, RenderUtils.scaledHeight, -1);
            Blur.get().render(2, GuiModule.blurStrength * ((float) alpha / 191.0f));
        }
        if (!GuiModule.hideWatermark.isToggled()) {
            this.drawTitle((float) this.width / 2.0f, (float) this.height / 4.0f);
        }
        for (CategoryComponent c : categories) {
            c.render(this.fontRendererObj, x, y);
            c.mousePosition(x, y);
            for (Component m : c.modules) {
                m.updatePosition(x, y);
            }
        }
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        if (!GuiModule.hidePlayerModel.isToggled() && this.mc.thePlayer != null) {
            GuiInventory.drawEntityOnScreen(this.width + 15 - this.backgroundAnimation.getValueInt(0, 40, 2), this.height - 10, 40,
                    (float) (this.width - 25 - x), (float) (this.height - 50 - y), this.mc.thePlayer);
        }
        if (CommandLineModule.open) {
            if (!this.commandSend.visible) {
                this.commandSend.visible = true;
            }
            int w = CommandLineModule.animate.isToggled() && CommandLineModule.animation != null ? CommandLineModule.animation.getValueInt(0, 200, 2) : 200;
            if (CommandLineModule.closing && (w = 200 - w) == 0) {
                this.commandSend.visible = false;
                CommandLineModule.open = false;
                CommandLineModule.closing = false;
            }
            drawRect(0, 0, w, this.height, 0x7D000000);
            this.drawHorizontalLine(0, w - 1, this.height - 360, -1);
            this.drawHorizontalLine(0, w - 1, this.height - 115, -1);
            drawRect(w - 1, 0, w, this.height, -1);
            Console.render(this.height, w, this.sr.getScaleFactor(), this.fontRendererObj);
            int fx = w - 178;
            this.commandLineInput.xPosition = fx;
            this.commandSend.xPosition = fx;
            this.commandLineInput.drawTextBox();
            super.drawScreen(x, y, p);
        } else if (CommandLineModule.closing) {
            CommandLineModule.closing = false;
        }
        int wy = this.height + 18 - this.watermarkAnimation.getValueInt(0, 30, 2);
        Watermark.render(4, wy, true);
    }

    public void drawTitle(float cx, float cy) {
        long now = System.currentTimeMillis();
        float since = now - openedAt;
        String title = "raven";
        String tag = "b4";
        float intro = this.clamp(since / 950.0f);
        float base = 2.5f;
        float pulse = 1.0f + (float) Math.sin((double) now * 0.0017) * 0.012f;
        float scale = base * pulse;
        float spacing = 0.8f;
        float width = 0.0f;
        for (int i = 0; i < title.length(); ++i) {
            width += ((float) this.fontRendererObj.getCharWidth(title.charAt(i)) + spacing) * scale;
        }
        float startX = cx - (width -= spacing * scale) / 2.0f;
        float bob = (float) Math.sin((double) now * 0.0021) * 1.2f;
        GL11.glEnable(3042);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        this.drawSparkles(cx, cy + 3.0f, width * 0.58f, 20.0f, intro, now);
        float px = startX;
        for (int i = 0; i < title.length(); ++i) {
            char c = title.charAt(i);
            float cw = this.fontRendererObj.getCharWidth(c);
            float t = this.clamp((since - (float) i * 70.0f) / 760.0f);
            if (t <= 0.0f) {
                px += (cw + spacing) * scale;
                continue;
            }
            float e = this.easeOutBack(t);
            float ang = -1.3f + (float) i * 0.48f;
            float dist = (1.0f - e) * (36.0f + (float) i * 3.5f);
            float bx = px + cw * scale / 2.0f;
            float by = cy + bob;
            float lx = bx + (float) Math.cos(ang) * dist;
            float ly = by + (float) Math.sin(ang) * dist * 0.55f + (1.0f - e) * 14.0f;
            float rot = (1.0f - e) * ((i & 1) == 0 ? -86.0f : 86.0f);
            float s = scale * (0.72f + 0.28f * e);
            int color = RenderUtils.rainbow(2L, (long) (-i) * 320L);
            this.drawGlowText(String.valueOf(c), lx, ly, s, rot, color, (int) (255.0f * t));
            px += (cw + spacing) * scale;
        }
        float tagT = this.clamp((since - 420.0f) / 850.0f);
        if (tagT > 0.0f) {
            float e = this.easeOutCubic(tagT);
            float s = 1.05f * (0.82f + 0.18f * e);
            float tw = (float) this.fontRendererObj.getStringWidth(tag) * s;
            float sway = (float) Math.sin((double) now * 0.00115) * 2.2f;
            float tbob = (float) Math.sin((double) now * 0.0021) * 0.9f;
            float phase = (float) (now % 4200L) / 4200.0f;
            float rock = (float) Math.sin((double) phase * Math.PI * 2.0) * 7.0f;
            float tx = cx + width * 0.28f + sway + 6.0f;
            float ty = cy + 17.0f + (1.0f - e) * 10.0f + tbob;
            float rot = (1.0f - e) * 8.0f + rock;
            this.drawGlowText(tag, tx + tw / 2.0f, ty, s, rot, RenderUtils.rainbow(2L, -1700L), (int) (235.0f * tagT));
        }
        RenderUtils.resetColor();
        GL11.glDisable(3042);
    }

    public void drawSparkles(float cx, float cy, float rx, float ry, float intro, long now) {
        char[] chars = new char[]{'.', '+', 'x', '*', '.', '+'};
        for (int i = 0; i < chars.length; ++i) {
            float scale;
            boolean even = (i & 1) == 0;
            double ang = (double) i * (Math.PI * 2 / (double) chars.length) + (even ? 0.0 : Math.PI / (double) chars.length);
            double speed = even ? 4.2E-4 : -3.1E-4;
            double a = ang + (double) now * speed;
            float ex = rx * (even ? 1.02f : 0.78f);
            float ey = ry * (even ? 1.0f : 0.72f);
            float sx = cx + (float) Math.cos(a) * ex;
            float sy = cy + (float) Math.sin(a) * ey;
            float twinkle = 0.65f + 0.35f * (float) Math.sin((double) now * 0.0042 + (double) i * 1.37);
            int alpha = (int) (120.0f * intro * twinkle);
            if (alpha <= 3) continue;
            alpha = RenderUtils.clampAlpha(alpha);
            int color = RenderUtils.withAlpha(RenderUtils.rainbow(2L, (long) i * 430L), alpha);
            char c = chars[i];
            float rot = 0.0f;
            switch (c) {
                case '.':
                    scale = 0.85f;
                    break;
                case '+':
                    scale = 0.72f;
                    rot = (float) (((double) now * 0.045 + (double) (i * 37)) % 360.0);
                    break;
                case 'x':
                    scale = 0.72f;
                    rot = (float) (((double) now * 0.04 + (double) (i * 53)) % 360.0);
                    break;
                default:
                    scale = 0.78f;
                    rot = (float) (((double) now * 0.03 + (double) (i * 29)) % 360.0);
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(sx, sy, 0.0f);
            if (rot != 0.0f) {
                GlStateManager.rotate(rot, 0.0f, 0.0f, 1.0f);
            }
            GlStateManager.scale(scale, scale, 1.0f);
            String s = String.valueOf(c);
            this.fontRendererObj.drawString(s, (float) (-this.fontRendererObj.getStringWidth(s)) / 2.0f, (float) (-this.fontRendererObj.FONT_HEIGHT) / 2.0f, color, true);
            GlStateManager.popMatrix();
        }
    }

    public void drawGlowText(String s, float x, float y, float scale, float rot, int color, int alpha) {
        if (alpha <= 3) {
            return;
        }
        alpha = RenderUtils.clampAlpha(alpha);
        int w = this.fontRendererObj.getStringWidth(s);
        int h = this.fontRendererObj.FONT_HEIGHT;
        float ox = (float) (-w) / 2.0f;
        float oy = (float) (-h) / 2.0f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0f);
        GlStateManager.rotate(rot, 0.0f, 0.0f, 1.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        for (int i = 2; i >= 1; --i) {
            float gs = 1.0f + (float) i * 0.055f;
            int ga = (int) ((float) alpha * (0.1f + (float) i * 0.035f));
            if (ga < 4) continue;
            ga = RenderUtils.clampAlpha(ga);
            GlStateManager.pushMatrix();
            GlStateManager.scale(gs, gs, 1.0f);
            this.fontRendererObj.drawString(s, ox, oy, RenderUtils.withAlpha(color, ga), false);
            GlStateManager.popMatrix();
        }
        this.fontRendererObj.drawString(s, ox, oy, RenderUtils.withAlpha(color, alpha), true);
        GlStateManager.popMatrix();
    }

    public float easeOutCubic(float f) {
        float t = 1.0f - f;
        return 1.0f - t * t * t;
    }

    public float clamp(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    public float easeOutBack(float f) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float t = f - 1.0f;
        return 1.0f + c3 * t * t * t + c1 * t * t;
    }

    @Override
    public void mouseClicked(int x, int y, int m) throws IOException {
        for (CategoryComponent c : categories) {
            if (m == 0) {
                if (c.overCategoryIcon(x, y)) {
                    c.setOpened(!c.opened);
                    return;
                }
                if (c.overTitle(x, y)) {
                    c.overTitle(true);
                    c.xOffset = (float) x - c.getX();
                    c.dragY = (float) y - c.getY();
                    return;
                }
            } else if ((m == 1 || m == 2) && c.overTitle(x, y)) {
                c.setOpened(!c.opened);
                return;
            }
            if (!c.opened || c.modules.isEmpty()) continue;
            for (Component comp : c.modules) {
                if (!comp.mouseClicked(x, y, m)) continue;
                return;
            }
        }
        if (CommandLineModule.open) {
            this.commandLineInput.mouseClicked(x, y, m);
            super.mouseClicked(x, y, m);
        }
    }

    @Override
    public void mouseReleased(int x, int y, int s) {
        if (s == 0) {
            for (CategoryComponent c : categories) {
                c.overTitle(false);
            }
            for (CategoryComponent c : categories) {
                if (!c.opened || c.modules.isEmpty()) continue;
                for (Component comp : c.modules) {
                    comp.mouseReleased(x, y, s);
                }
            }
        }
    }

    @Override
    public void keyTyped(char t, int k) {
        if (k == ModuleManager.gui.getKeycode() && !ModuleManager.commandLine.isEnabled() || k == 1 && !BindComponent.binding) {
            this.mc.displayGuiScreen(null);
            return;
        }
        for (CategoryComponent c : categories) {
            if (!c.opened || c.modules.isEmpty()) continue;
            for (Component comp : c.modules) {
                comp.keyTyped(t, k);
            }
        }
        if (CommandLineModule.open) {
            String cm = this.commandLineInput.getText();
            if (k == 28 && !cm.isEmpty()) {
                Console.handle(cm);
                this.commandLineInput.setText("");
            } else {
                this.commandLineInput.textboxKeyTyped(t, k);
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton b) {
        if (b == this.commandSend) {
            Console.handle(this.commandLineInput.getText());
            this.commandLineInput.setText("");
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            dWheel = dWheel >> 31 | -dWheel >>> 31;
            boolean computed = false;
            int mx = 0, my = 0;
            for (CategoryComponent c : categories) {
                if (!c.opened || c.opening) continue;
                if (!computed) {
                    mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
                    my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
                    computed = true;
                }
                c.onScroll(mx, my, dWheel);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        GuiModule.restoreScale();
        this.watermarkAnimation = null;
        this.delayedAnimation = null;
        this.unusedAnimation = null;
        this.backgroundAnimation = null;
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
        for (CategoryComponent c : categories) {
            if (!c.opened || c.opening) continue;
            c.mouseReleased();
        }
        for (Module m : ModuleManager.modules) {
            m.guiUpdate();
        }
        if (xyz.daikirai.module.GuiModule.saveCategoryPositions.isToggled()) {
            xyz.daikirai.client.ClientConfig.save();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
