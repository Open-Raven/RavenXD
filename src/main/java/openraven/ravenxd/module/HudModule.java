package openraven.ravenxd.module;

import openraven.ravenxd.client.Overlay;
import openraven.ravenxd.gui.HudPositionEditor;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.RenderUtils;
import openraven.ravenxd.util.Theme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HudModule extends Module {
    public enum Outline {
        None(false, false, false),
        Full(false, false, false),
        Left(true, false, false),
        Bottom_left(true, false, true),
        Right(false, true, false),
        Bottom_right(false, true, true);

        public final boolean left;
        public final boolean right;
        public final boolean bottom;

        Outline(boolean left, boolean right, boolean bottom) {
            this.left = left;
            this.right = right;
            this.bottom = bottom;
        }
    }

    public static final String INFO_SEPARATOR = "§7 ";
    public static final int ANIM_MS = 135;
    public static final float SLIDE = 12.0f;

    public static Outline outline = Outline.Left;
    public static Theme theme = Theme.DEFAULT;
    public static int posX = 5, posY = 70;
    public static int fontHeight;
    public static int lineHeight;

    public static SliderSetting outlineSetting, scale, themeSetting, style, nplusColors;
    public static ButtonSetting editPosition, alignRight, alphabeticalSort, animate, drawBackground, lowercase, removeClosetModules,
            removeRenderModules, removeScripts, showModuleInfo;

    public HudModule(int key) {
        super("HUD", Category.render, key);
        fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        lineHeight = fontHeight + 1;
    }

    public void link() {
        outlineSetting = (SliderSetting) ModuleManager.setting(this, "Outline");
        scale = (SliderSetting) ModuleManager.setting(this, "Scale");
        themeSetting = (SliderSetting) ModuleManager.setting(this, "Theme");
        style = (SliderSetting) ModuleManager.setting(this, "Style");
        nplusColors = (SliderSetting) ModuleManager.setting(this, "N+ colors");
        editPosition = (ButtonSetting) ModuleManager.setting(this, "Edit position");
        alignRight = (ButtonSetting) ModuleManager.setting(this, "Align right");
        alphabeticalSort = (ButtonSetting) ModuleManager.setting(this, "Alphabetical sort");
        animate = (ButtonSetting) ModuleManager.setting(this, "Animate");
        drawBackground = (ButtonSetting) ModuleManager.setting(this, "Draw background");
        lowercase = (ButtonSetting) ModuleManager.setting(this, "Lowercase");
        removeClosetModules = (ButtonSetting) ModuleManager.setting(this, "Remove closet modules");
        removeRenderModules = (ButtonSetting) ModuleManager.setting(this, "Remove render modules");
        removeScripts = (ButtonSetting) ModuleManager.setting(this, "Remove scripts");
        showModuleInfo = (ButtonSetting) ModuleManager.setting(this, "Show module info");
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == editPosition) {
            mc.displayGuiScreen(new HudPositionEditor());
        }
    }

    @Override
    public void guiUpdate() {
        theme = Theme.values()[(int) themeSetting.getInput()];
        outline = Outline.values()[(int) outlineSetting.getInput()];
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Overlay.canRender()) {
            return;
        }
        render();
    }

    public static float slide(Module m, long now) {
        if (!animate.isToggled()) {
            return 0.0f;
        }
        float f = !alignRight.isToggled() ? -SLIDE : SLIDE;
        if (!m.isEnabled()) {
            long l = now - m.hudDisabledAt;
            return l >= (long) ANIM_MS ? f : f * ((float) l / (float) ANIM_MS);
        }
        long l = now - m.hudEnabledAt;
        return l >= (long) ANIM_MS ? 0.0f : f * (1.0f - (float) l / (float) ANIM_MS);
    }

    public static float height(Module m, long now) {
        if (!animate.isToggled()) {
            return m.isEnabled() ? (float) lineHeight : 0.0f;
        }
        if (m.isEnabled()) {
            long l = now - m.hudEnabledAt;
            if (l < (long) ANIM_MS) {
                return (float) lineHeight * ((float) l / (float) ANIM_MS);
            }
            return lineHeight;
        }
        long l = now - m.hudDisabledAt;
        if (l < (long) ANIM_MS) {
            return (float) lineHeight * (1.0f - (float) l / (float) ANIM_MS);
        }
        return 0.0f;
    }

    public static float visibility(Module m, long now) {
        if (animate == null || !animate.isToggled()) {
            return m.isEnabled() ? 1.0f : 0.0f;
        }
        if (m.isEnabled()) {
            long l = now - m.hudEnabledAt;
            if (l < (long) ANIM_MS) {
                return 0.1f + 0.9f * ((float) l / (float) ANIM_MS);
            }
            return 1.0f;
        }
        long l = now - m.hudDisabledAt;
        if (l < (long) ANIM_MS) {
            return 1.0f - 0.9f * ((float) l / (float) ANIM_MS);
        }
        return 0.0f;
    }

    public static List<Module> listed() {
        ArrayList<Module> list = new ArrayList<Module>(ModuleManager.hudOrder);
        Iterator<Module> it = list.iterator();
        while (it.hasNext()) {
            Module m = it.next();
            if (m.isHidden() || m == ModuleManager.hud || m == ModuleManager.gui || m == ModuleManager.commandLine) {
                it.remove();
                continue;
            }
            if (removeClosetModules.isToggled() && m.isCloset()) {
                it.remove();
                continue;
            }
            if (removeRenderModules.isToggled() && m.moduleCategory() == Category.render) {
                it.remove();
                continue;
            }
            if (removeScripts.isToggled() && m.isScript()) {
                it.remove();
                continue;
            }
        }
        return list;
    }

    public static void sort() {
        if (alphabeticalSort.isToggled()) {
            ModuleManager.hudOrder.sort((a, b) -> a.getName().compareTo(b.getName()));
        } else {
            ModuleManager.hudOrder.sort((a, b) -> mc.fontRendererObj.getStringWidth(b.getDisplayName()) - mc.fontRendererObj.getStringWidth(a.getDisplayName()));
        }
    }

    public static void render() {
        if (style.getInput() > 0.0) {
            openraven.ravenxd.gui.nplus.NPlusArrayList.render(style.getInput() == 2.0);
            return;
        }
        List<Module> all = listed();
        long now = System.currentTimeMillis();
        ArrayList<Module> mods = new ArrayList<Module>();
        ArrayList<Float> heights = new ArrayList<Float>();
        ArrayList<Float> alphas = new ArrayList<Float>();
        for (Module m : all) {
            float h = height(m, now);
            if (h <= 0.0f) continue;
            float a = visibility(m, now);
            if ((double) a <= 0.1) continue;
            mods.add(m);
            heights.add(h);
            alphas.add(a);
        }
        float sc = (float) scale.getInput();
        float x0 = (float) posX / sc;
        float y = (float) posY / sc;
        float offset = 0.0f;
        float lastWidth = 0.0f;
        int last = mods.size() - 1;
        for (int i = 0; i < mods.size(); ++i) {
            Module m = mods.get(i);
            float h = heights.get(i);
            float a = alphas.get(i);
            float slide = slide(m, now);
            int rgb = theme.getRGB((double) offset);
            if (theme != Theme.RAINBOW) {
                offset = (float) ((double) offset - SettingsModule.themeOffset.getInput());
            } else {
                offset = (float) ((double) offset - 240.0 * SettingsModule.themeOffset.getInput());
            }
            int alpha = (int) (a * 255.0f) & 0xFF;
            int color = RenderUtils.withAlpha(rgb, alpha);
            String s = m.getDisplayName();
            float w = mc.fontRendererObj.getStringWidth(s);
            float x = !alignRight.isToggled() ? x0 : x0 - w;
            float ty = y + (h - (float) fontHeight) / 2.0f;
            GlStateManager.pushMatrix();
            if (sc != 1.0f) {
                GlStateManager.scale(sc, sc, 1.0f);
            }
            GlStateManager.translate(slide / sc, 0.0f, 0.0f);
            float bgA = (a - 0.1f) / 0.9f;
            int bg = ((int) (26.0f + bgA * 64.0f) & 0xFF) << 24;
            if (drawBackground.isToggled()) {
                float l = x - 1.0f;
                float r = x + w;
                if (outline.left) {
                    l -= 0.5f;
                } else if (outline.right) {
                    r += 0.5f;
                } else if (outline == Outline.Full) {
                    r += 0.5f;
                    l -= 0.5f;
                }
                rect(l, ty + (float) fontHeight, r, ty - (float) 1, bg);
            }
            if (outline != Outline.None) {
                if (outline == Outline.Full) {
                    fullOutline(i, x, ty, w, lastWidth, color, bg, last);
                } else {
                    if (outline.left) {
                        leftOutline(i, x, ty, w, lastWidth, color, bg);
                    } else if (outline.right) {
                        rightOutline(i, x, ty, w, lastWidth, color, bg);
                    }
                    if (outline.bottom && i == last) {
                        float l = x - 2.5f;
                        float r = x + w + 1.5f;
                        if (outline == Outline.Bottom_left) {
                            r -= 1.5f;
                        } else {
                            l += 1.5f;
                        }
                        bottomLine(l, r, ty, color);
                    }
                }
            }
            RenderUtils.shadowText(s, x, ty, color);
            GlStateManager.popMatrix();
            y += h;
            lastWidth = w;
        }
    }

    private static void rect(float f, float f2, float f3, float f4, int n) {
        RenderUtils.beginRect();
        RenderUtils.rawRect(f, f2, f3, f4, n);
        RenderUtils.endRect();
    }

    private static void fullOutline(int n, float f, float f2, float f3, float f4, int n2, int n3, int n4) {
        RenderUtils.beginRect();
        RenderUtils.rawRect(f - 2.5f, f2 + (float) fontHeight, f - 1.5f, f2 - (float) 1, n2);
        RenderUtils.rawRect(f + f3 + 0.5f, f2 + (float) fontHeight, f + f3 + 1.5f, f2 - (float) 1, n2);
        if (!drawBackground.isToggled()) {
            RenderUtils.rawRect(f + f3 + 1.5f, f2 + (float) fontHeight, f + f3 + 2.0f, f2 - (float) 1, n3);
            RenderUtils.rawRect(f - 1.5f, f2 + (float) fontHeight, f - 1.0f, f2 - (float) 1, n3);
        }
        if (n == 0) {
            RenderUtils.rawRect(f - 2.5f, f2 - (float) 1, f + f3 + 1.5f, f2 - (float) 1 - 1.0f, n2);
        } else if (alignRight.isToggled()) {
            RenderUtils.rawRect(f - 2.5f - (f4 - f3), f2 - (float) 1 + 1.0f, f - 2.5f, f2 - (float) 1, n2);
        } else {
            RenderUtils.rawRect(f + f3 + 1.5f + (f4 - f3), f2 - (float) 1 + 1.0f, f + f3 + 1.5f, f2 - (float) 1, n2);
        }
        RenderUtils.endRect();
        if (n == n4) {
            bottomLine(f - 2.5f, f + f3 + 1.5f, f2, n2);
        }
    }

    private static void leftOutline(int n, float f, float f2, float f3, float f4, int n2, int n3) {
        RenderUtils.beginRect();
        RenderUtils.rawRect(f - 2.5f, f2 + (float) fontHeight, f - 1.5f, f2 - (float) 1, n2);
        if (!drawBackground.isToggled()) {
            RenderUtils.rawRect(f - 1.5f, f2 + (float) fontHeight, f - 1.0f, f2 - (float) 1, n3);
        }
        if (n > 0 && alignRight.isToggled()) {
            RenderUtils.rawRect(f - 2.5f - (f4 - f3), f2 - (float) 1 + 1.0f, f - 2.5f, f2 - (float) 1, n2);
        }
        RenderUtils.endRect();
    }

    private static void rightOutline(int n, float f, float f2, float f3, float f4, int n2, int n3) {
        RenderUtils.beginRect();
        RenderUtils.rawRect(f + f3 + 0.5f, f2 + (float) fontHeight, f + f3 + 1.5f, f2 - (float) 1, n2);
        if (!drawBackground.isToggled()) {
            RenderUtils.rawRect(f + f3 + 1.5f, f2 + (float) fontHeight, f + f3 + 2.0f, f2 - (float) 1, n3);
        }
        if (n > 0 && !alignRight.isToggled()) {
            RenderUtils.rawRect(f + f3 + 1.5f + (f4 - f3), f2 - (float) 1 + 1.0f, f + f3 + 1.5f, f2 - (float) 1, n2);
        }
        RenderUtils.endRect();
    }

    private static void bottomLine(float f, float f2, float f3, int n) {
        rect(f, f3 + (float) fontHeight, f2, f3 + (float) fontHeight + 1.0f, n);
    }
}
