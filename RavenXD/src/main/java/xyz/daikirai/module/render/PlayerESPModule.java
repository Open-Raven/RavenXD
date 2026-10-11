package xyz.daikirai.module.render;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.EspShaders;
import xyz.daikirai.util.GlowShader;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Theme;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class PlayerESPModule extends Module {
    public enum Type {
        None(0), Outline(-1), Outline_Light_Glow(-2), Outline_Heavy_Glow(-3), Box(1), Shaded(2), _2D(3), Ring(6);

        public final int mode;

        Type(int mode) {
            this.mode = mode;
        }
    }

    public static PlayerESPModule instance;
    public static SliderSetting typeSetting, red, green, blue;
    public static ButtonSetting disableInLobby, rainbow, healthBar, redOnDamage, renderSelf, teamColor, showInvis;
    public static Type type = Type.Outline;
    public static int color;
    public static boolean shaderPass;
    private static Framebuffer framebuffer;
    private int lastR, lastG, lastB;

    public PlayerESPModule(int key) {
        super("PlayerESP", Category.render, key);
        instance = this;
    }

    public void link() {
        typeSetting = (SliderSetting) ModuleManager.setting(this, "Type");
        red = (SliderSetting) ModuleManager.setting(this, "Red");
        green = (SliderSetting) ModuleManager.setting(this, "Green");
        blue = (SliderSetting) ModuleManager.setting(this, "Blue");
        disableInLobby = (ButtonSetting) ModuleManager.setting(this, "Disable in lobby");
        rainbow = (ButtonSetting) ModuleManager.setting(this, "Rainbow");
        healthBar = (ButtonSetting) ModuleManager.setting(this, "Health bar");
        redOnDamage = (ButtonSetting) ModuleManager.setting(this, "Red on damage");
        renderSelf = (ButtonSetting) ModuleManager.setting(this, "Render self");
        teamColor = (ButtonSetting) ModuleManager.setting(this, "Team color");
        showInvis = (ButtonSetting) ModuleManager.setting(this, "Show invis");
    }

    @Override
    public void guiUpdate() {
        type = Type.values()[(int) typeSetting.getInput()];
        int r = (int) red.getInput();
        int g = (int) green.getInput();
        int b = (int) blue.getInput();
        if (r != this.lastR || g != this.lastG || b != this.lastB) {
            this.lastR = r;
            this.lastG = g;
            this.lastB = b;
            color = new Color(this.lastR, this.lastG, this.lastB).getRGB();
        }
    }

    public static boolean shouldRender(Entity e) {
        return !(!renderSelf.isToggled() && e == mc.thePlayer || BotCheck.aboveSpawn(e) || BotCheck.isBot(e, true));
    }

    public static int color(EntityLivingBase e, float pt) {
        int n = teamColor.isToggled() && e instanceof EntityPlayer ? teamColor(e) : (rainbow.isToggled() ? 0 : color);
        if (redOnDamage.isToggled() && e.hurtTime > 1) {
            if (n == 0) {
                n = RenderUtils.rainbow(2L, 0L);
            }
            float f2 = (float) e.hurtTime - pt;
            double d = Math.max(0.0, Math.min(1.0, (double) (f2 - 1.0f) / 7.0));
            n = RenderUtils.blendInt(Theme.FLARE.first.getRGB(), n, d);
        }
        return n;
    }

    public static int teamColor(EntityLivingBase e) {
        String s;
        ScorePlayerTeam team;
        int n = 0xFFFFFF;
        if (e instanceof EntityPlayer && (team = (ScorePlayerTeam) e.getTeam()) != null && (s = FontRenderer.getFormatFromString(team.getColorPrefix())).length() >= 2) {
            n = mc.fontRendererObj.getColorCode(s.charAt(1));
        }
        return 0xFF000000 | n & 0xFFFFFF;
    }

    public static boolean isShader() {
        return type.mode < 0;
    }

    public static void render(EntityLivingBase e, float pt) {
        int n = color(e, pt);
        if (healthBar.isToggled()) {
            Render3D.entityBox(e, 4, n, pt);
        }
        if (type != Type.None && !isShader()) {
            Render3D.entityBox(e, type.mode, n, pt);
        }
    }

    private static Framebuffer framebuffer() {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            framebuffer.setFramebufferFilter(9728);
        }
        return framebuffer;
    }

    public static void renderShader(float pt) {
        if (!EspShaders.valid(EspShaders.outline()) || !EspShaders.valid(EspShaders.fill())) {
            return;
        }
        Framebuffer fb = framebuffer();
        RenderManager rm = mc.getRenderManager();
        boolean shadow = rm.isRenderShadow();
        boolean drew = false;
        int mode = GL11.glGetInteger(2976);
        GL11.glMatrixMode(5889);
        GL11.glPushMatrix();
        GL11.glMatrixMode(5888);
        GL11.glPushMatrix();
        try {
            fb.framebufferClear();
            fb.bindFramebuffer(false);
            Render3D.setupCamera(pt);
            rm.setRenderShadow(false);
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.001f);
            GlStateManager.depthMask(false);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            shaderPass = true;
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p.deathTime != 0 || p == mc.thePlayer && (!renderSelf.isToggled() || mc.gameSettings.thirdPersonView == 0 || p.isPlayerSleeping())
                        || !showInvis.isToggled() && p.isInvisible() || !shouldRender(p)) continue;
                int n = color(p, pt);
                if (n == 0) {
                    n = RenderUtils.rainbow(2L, 0L);
                }
                EspShaders.useFill(n);
                rm.renderEntityStatic(p, pt, true);
                EspShaders.fill().stop();
                drew = true;
            }
            shaderPass = false;
            mc.entityRenderer.disableLightmap();
            mc.getFramebuffer().bindFramebuffer(false);
            Render3D.setupOverlay();
            if (drew) {
                GlStateManager.disableDepth();
                GlStateManager.depthMask(false);
                GlStateManager.disableLighting();
                GlStateManager.disableCull();
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                if (type != Type.Outline) {
                    GlowShader.get().render(fb, 2, type == Type.Outline_Light_Glow ? 1.5f : 6.5f);
                }
                EspShaders.outline(fb, 2.0f, 0.1f);
            }
        } finally {
            EspShaders.fill().stop();
            shaderPass = false;
            rm.setRenderShadow(shadow);
            mc.getFramebuffer().bindFramebuffer(false);
            GL11.glMatrixMode(5888);
            GL11.glPopMatrix();
            GL11.glMatrixMode(5889);
            GL11.glPopMatrix();
            GL11.glMatrixMode(mode);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1f);
            GlStateManager.disableBlend();
        }
    }
}
