package xyz.daikirai.module.render;

import xyz.daikirai.gui.TargetHudEditor;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Blur;
import xyz.daikirai.util.EspShaders;
import xyz.daikirai.util.GlowShader;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import xyz.daikirai.util.Theme;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class TargetHudModule extends Module {
    public enum Mode {
        Disabled, Modern, Legacy;

        public void render(String s, double d) {
            render(null, s, d);
        }

        public void render(xyz.daikirai.util.Animation fade, String s, double d) {
            if (this == Legacy) {
                legacy(fade, s, d);
            } else {
                modern(fade, s, d);
            }
        }
    }

    private static final int TEXT = new Color(220, 220, 220, 255).getRGB();

    public static SliderSetting modeSetting, themeSetting;
    public static ButtonSetting editPosition, renderEsp, showDifference, showHealth, showWinLoss, traditionalHealthColor;
    public static Mode mode = Mode.Modern;
    public static Theme theme = Theme.DEFAULT;
    public static int offsetX = 70, offsetY = 40;
    private static double shown;
    private static double target = -1.0;
    private static double from = -1.0;
    private static long changedAt = -1L;

    public TargetHudModule(int key) {
        super("TargetHUD", Category.render, key);
    }

    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        themeSetting = (SliderSetting) ModuleManager.setting(this, "Theme");
        editPosition = (ButtonSetting) ModuleManager.setting(this, "Edit position");
        renderEsp = (ButtonSetting) ModuleManager.setting(this, "Render ESP");
        showDifference = (ButtonSetting) ModuleManager.setting(this, "Show difference");
        showHealth = (ButtonSetting) ModuleManager.setting(this, "Show health");
        showWinLoss = (ButtonSetting) ModuleManager.setting(this, "Show win/loss");
        traditionalHealthColor = (ButtonSetting) ModuleManager.setting(this, "Traditional health color");
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == editPosition) {
            mc.displayGuiScreen(new TargetHudEditor());
        }
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
        theme = Theme.values()[(int) themeSetting.getInput()];
    }

    @Override
    public void onDisable() {
        changedAt = -1L;
        from = target = (double) -1L;
    }

    public static String text(String name, float health, double ratio) {
        StringBuilder sb = new StringBuilder(name);
        if (showHealth.isToggled()) {
            sb.append(' ').append(Strings.health(ratio, health));
        }
        float own = Strings.health((net.minecraft.entity.EntityLivingBase) mc.thePlayer);
        if (showDifference.isToggled()) {
            sb.append(' ').append(Strings.healthDifference(own - health, !showHealth.isToggled()));
        }
        if (showWinLoss.isToggled()) {
            boolean win = ratio <= (double) (own / mc.thePlayer.getMaxHealth());
            sb.append(' ').append(Strings.SECTION).append(win ? "aW" : "cL");
        }
        String s = sb.toString();
        int n = s.length();
        if (n < 14) {
            int pad = (16 - n + 1) / 2;
            String sp = "          ".substring(0, pad);
            s = sp + s + sp;
        }
        return s;
    }

    public static void render(xyz.daikirai.util.Animation fade, String name, float health, double ratio) {
        mode.render(fade, text(name, health, ratio), ratio);
    }

    public static void legacy(xyz.daikirai.util.Animation fade, String s, double d) {
        int w = mc.fontRendererObj.getStringWidth(s) + 8;
        int x = RenderUtils.scaledWidth / 2 - w / 2 + offsetX;
        int y = RenderUtils.scaledHeight / 2 + offsetY;
        int x1 = x - 8;
        int y1 = y - 8;
        int x2 = x + w;
        int y2 = y + mc.fontRendererObj.FONT_HEIGHT - 1 + 8;
        int a = fade == null ? 255 : 255 - fade.getValueInt(0, 255, 1);
        if (a <= 0) {
            return;
        }
        int bgA = Math.min(a, 100);
        int barA = Math.min(a, 210);
        int[] rgb = theme.getRGBArr();
        int bx1 = x1 + 6;
        int bx2 = x2 - 6;
        int by = y2;
        if (target != -1.0) {
            long now = System.currentTimeMillis();
            long age = now - changedAt;
            long dur = 125L;
            if (d != target) {
                from = age > dur ? target : shown;
                target = d;
                changedAt = now;
            } else if (age <= dur) {
                double t = Math.min(1.0, (double) age / (double) dur);
                shown = from + (target - from) * t;
            } else {
                shown = d;
            }
        } else {
            target = d;
        }
        d = shown;
        int c1;
        int c2;
        if (traditionalHealthColor.isToggled()) {
            c1 = c2 = RenderUtils.withAlpha(Strings.healthColor(d), barA);
        } else {
            c1 = RenderUtils.withAlpha(rgb[0], barA);
            c2 = RenderUtils.withAlpha(RenderUtils.blendInt(rgb[0], rgb[1], 1.0 - d), barA);
        }
        RenderUtils.roundedRectBorder(x1, y1, x2, y2 + 13, 10.0f, RenderUtils.withAlpha(Color.black.getRGB(), bgA), RenderUtils.withAlpha(rgb[0], a),
                RenderUtils.withAlpha(rgb[1], a));
        if (d < 1.0) {
            RenderUtils.roundedRect(bx1, by, bx2, by + 5, 4.0f, RenderUtils.withAlpha(Color.black.getRGB(), bgA));
        }
        RenderUtils.roundedRectGradient(bx1, by, (float) ((double) bx2 + (double) (bx1 - bx2) * (1.0 - d)), by + 5, 4.0f, c1, c1, c2, c2);
        GL11.glEnable(3042);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.fontRendererObj.drawString(s, (float) x, (float) y, RenderUtils.withAlpha(TEXT, RenderUtils.clampAlpha(a)), true);
        GL11.glDisable(3042);
        RenderUtils.resetColor();
    }

    public static void modern(xyz.daikirai.util.Animation fade, String s, double d) {
        int w = mc.fontRendererObj.getStringWidth(s) + 8;
        int x = RenderUtils.scaledWidth / 2 - w / 2 + offsetX;
        int y = RenderUtils.scaledHeight / 2 + offsetY;
        int x1 = x - 8;
        int y1 = y - 8;
        int x2 = x + w;
        int y2 = y + mc.fontRendererObj.FONT_HEIGHT - 1 + 8;
        int a = fade == null ? 255 : 255 - fade.getValueInt(0, 255, 1);
        if (a <= 0) {
            return;
        }
        int bgA = Math.min(a, 100);
        int barA = Math.min(a, 210);
        int[] rgb = theme.getRGBArr();
        int bx1 = x1 + 6;
        int bx2 = x2 - 6;
        boolean falling = target < from;
        if (target == -1.0) {
            target = d;
        } else {
            long now = System.currentTimeMillis();
            long age = now - changedAt;
            long dur = !falling ? 500L : 800L;
            if (d == target) {
                if (age > dur) {
                    shown = d;
                } else {
                    double t = Math.min(1.0, (double) age / (double) dur);
                    t = (double) (1.0f - (float) Math.pow(1.0 - t, 5.0));
                    shown = from + (target - from) * t;
                }
            } else {
                if (age <= dur) {
                    from = shown;
                } else {
                    from = target;
                }
                target = d;
                changedAt = now;
            }
        }
        d = shown;
        int c1;
        int c2;
        int t1;
        int t2;
        if (!traditionalHealthColor.isToggled()) {
            double front = !falling ? d : target;
            c1 = RenderUtils.withAlpha(rgb[0], barA);
            c2 = RenderUtils.withAlpha(RenderUtils.blendInt(rgb[0], rgb[1], 1.0 - front), barA);
            t1 = RenderUtils.withAlpha(rgb[0], bgA);
            t2 = RenderUtils.withAlpha(RenderUtils.blendInt(rgb[0], rgb[1], 1.0 - d), bgA);
        } else {
            int hc = Strings.healthColor(d);
            c1 = c2 = RenderUtils.withAlpha(hc, barA);
            t1 = t2 = RenderUtils.withAlpha(hc, bgA);
        }
        float f = (float) barA / 210.0f;
        GlowShader.get().startCapture();
        EspShaders.roundRect((float) x1, (float) y1, (float) x2, (float) (y2 + 13), 5.0f, RenderUtils.withAlpha(Color.black.getRGB(), barA));
        GlowShader.get().render(2, 1.3f * f);
        Blur.get().startStencil();
        EspShaders.roundRect((float) x1, (float) y1, (float) x2, (float) (y2 + 13), 5.0f, -1);
        Blur.get().render(2, 2.0f * f);
        if (d < 1.0) {
            RenderUtils.roundedRect((float) bx1, (float) y2, (float) bx2, (float) (y2 + 5), 4.0f, RenderUtils.withAlpha(Color.black.getRGB(), bgA));
        }
        if (!falling) {
            RenderUtils.roundedRectGradient((float) bx1, (float) y2, (float) ((double) bx2 + (double) (bx1 - bx2) * (1.0 - d)), (float) (y2 + 5), 4.0f, c1, c1, c2, c2);
        } else {
            if (target < 1.0) {
                RenderUtils.roundedRectGradient((float) bx1, (float) y2, (float) ((double) bx2 + (double) (bx1 - bx2) * (1.0 - d)), (float) (y2 + 5), 4.0f, t1, t1, t2, t2);
            }
            RenderUtils.roundedRectGradient((float) bx1, (float) y2, (float) ((double) bx2 + (double) (bx1 - bx2) * (1.0 - target)), (float) (y2 + 5), 4.0f, c1, c1, c2, c2);
        }
        GL11.glEnable(3042);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.fontRendererObj.drawString(s, (float) x, (float) y, RenderUtils.withAlpha(TEXT, RenderUtils.clampAlpha(a)), true);
        GL11.glDisable(3042);
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onRenderWorld(net.minecraftforge.client.event.RenderWorldLastEvent e) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (!renderEsp.isToggled() || mc.thePlayer == null || mc.theWorld == null || xyz.daikirai.module.network.BacktrackModule.ghost != null) {
            return;
        }
        if (xyz.daikirai.module.combat.KillAuraModule.target != null) {
            xyz.daikirai.util.Render3D.entityBox(xyz.daikirai.module.combat.KillAuraModule.target.entity, 2, theme.getRGB(0.0), e.partialTicks);
        }
    }
}
