package xyz.daikirai.gui.nplus;

import xyz.daikirai.module.HudModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NPlusArrayList {
    public enum Colors {
        RavenB, Astolfo, Astolfo2, ZeroDay
    }

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<Module, Float> widths = new HashMap<Module, Float>();
    private static long lastRender;

    private NPlusArrayList() {
    }

    public static void render(boolean zeroDay) {
        List<Module> mods = new ArrayList<Module>();
        for (Module m : HudModule.listed()) {
            if (m.isEnabled()) {
                mods.add(m);
            } else {
                widths.remove(m);
            }
        }
        long now = System.currentTimeMillis();
        float step = Math.min(1.0f, (now - lastRender) / 100.0f);
        lastRender = now;
        float sc = (float) HudModule.scale.getInput();
        GlStateManager.pushMatrix();
        GlStateManager.scale(sc, sc, 1.0f);
        if (zeroDay) {
            zeroDay(mods, sc, step);
        } else {
            nplus(mods, sc, step);
        }
        GlStateManager.popMatrix();
        RenderUtils.resetColor();
    }

    private static float animate(Module m, float target, float step) {
        if (!HudModule.animate.isToggled()) {
            widths.put(m, target);
            return target;
        }
        Float w = widths.get(m);
        float v = w == null ? 0.0f : w + (target - w) * step;
        if (Math.abs(target - v) < 0.5f) {
            v = target;
        }
        widths.put(m, v);
        return v;
    }

    private static int color(int i, boolean zeroDay) {
        switch (Colors.values()[(int) HudModule.nplusColors.getInput()]) {
            case Astolfo: return NPlusRender.astolfo(10, 14);
            case Astolfo2: return NPlusRender.astolfo(10, zeroDay ? i * 160 : -120 * i);
            case ZeroDay: return NPlusRender.zeroDay((long) i * 160L);
            default: return NPlusRender.rainbow(2L, -120L * i);
        }
    }

    private static void nplus(List<Module> mods, float sc, float step) {
        int x0 = (int) (HudModule.posX / sc);
        int y = (int) (HudModule.posY / sc);
        int h = NPlusRender.height();
        boolean right = HudModule.alignRight.isToggled();
        for (int i = 0; i < mods.size(); ++i) {
            Module m = mods.get(i);
            String s = m.getDisplayName();
            int tw = NPlusRender.width(s);
            float w = animate(m, tw, step);
            int x = right ? x0 - (int) w : x0;
            if (HudModule.drawBackground.isToggled()) {
                Gui.drawRect(x - 2, y - 1, x + 1 + (int) w, y + h + 1, NPlusRender.intMin);
            }
            float tx = right ? x : x + w - tw;
            NPlusRender.text(s, tx, y + 1, color(i, false), true);
            y += h + 2;
        }
    }

    private static void zeroDay(List<Module> mods, float sc, float step) {
        int screenWidth = (int) (new ScaledResolution(mc).getScaledWidth() / sc);
        int h = NPlusRender.height();
        int pad = 4;
        for (int i = 0; i < mods.size(); ++i) {
            Module m = mods.get(i);
            String s = m.getDisplayName();
            int tw = NPlusRender.width(s);
            float w = animate(m, tw, step);
            int y = i * (h + 6);
            int c = color(i, true);
            if (HudModule.drawBackground.isToggled()) {
                Gui.drawRect(screenWidth - (int) w - pad * 2, y, screenWidth, y + h + 6, NPlusRender.intMinDarker);
                if (w >= tw) {
                    Gui.drawRect(screenWidth - 2, y, screenWidth, y + h + 6, c);
                }
            }
            NPlusRender.text(s, screenWidth - w - pad, y + pad, c, true);
        }
    }
}
