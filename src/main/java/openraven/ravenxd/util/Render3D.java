package openraven.ravenxd.util;

import openraven.ravenxd.module.render.ExtendCameraModule;
import openraven.ravenxd.module.render.NametagsModule;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.awt.Color;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class Render3D implements IMinecraft {

    public static final Frustum frustum = new Frustum();
    public static final double[] cameraPos = new double[3];
    private static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer screen = BufferUtils.createFloatBuffer(3);
    private static Method setupCameraTransform;

    private Render3D() {
    }

    public static void updateFrustum(float f) {
        Entity entity = mc.getRenderViewEntity();
        if (entity != null) {
            double d = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) f;
            double d2 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) f;
            double d3 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) f;
            frustum.setPosition(d, d2, d3);
        }
    }

    public static boolean isVisible(AxisAlignedBB bb) {
        return frustum.isBoundingBoxInFrustum(bb);
    }

    public static boolean isVisible(Entity e) {
        return isVisible(e.getEntityBoundingBox());
    }

    public static boolean isVisible(double x, double y, double z, double x2, double y2, double z2) {
        return frustum.isBoxInFrustum(x, y, z, x2, y2, z2);
    }

    public static float cameraYaw() {
        return (float) Math.atan2(ActiveRenderInfo.getRotationXY(), ActiveRenderInfo.getRotationX());
    }

    public static float cameraPitch() {
        return (float) Math.acos(ActiveRenderInfo.getRotationXZ());
    }

    public static void updateCameraPos() {
        RenderManager rm = mc.getRenderManager();
        double d = rm.viewerPosX;
        double d2 = rm.viewerPosY + (double) mc.thePlayer.getEyeHeight();
        double d3 = rm.viewerPosZ;
        if (mc.gameSettings.thirdPersonView != 0) {
            float f = cameraYaw();
            float f2 = cameraPitch();
            double d4 = ExtendCameraModule.distance();
            double d5 = Math.cos(f2) * d4;
            d -= -Math.sin(f) * d5;
            d2 -= -Math.sin(f2) * d4;
            d3 -= Math.cos(f) * d5;
        }
        cameraPos[0] = d;
        cameraPos[1] = d2;
        cameraPos[2] = d3;
    }

    public static void setupCamera(float f) {
        try {
            if (setupCameraTransform == null) {
                setupCameraTransform = ReflectionHelper.findMethod(EntityRenderer.class, mc.entityRenderer,
                        new String[]{"setupCameraTransform", "func_78479_a"}, float.class, int.class);
            }
            setupCameraTransform.invoke(mc.entityRenderer, f, 0);
        } catch (Exception ignored) {
        }
    }

    public static void setupOverlay() {
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0, RenderUtils.scaledWidthD, RenderUtils.scaledHeightD, 0.0, 1000.0, 3000.0);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0f, 0.0f, -2000.0f);
    }

    public static Vec3 project(double d, double d2, double d3, int scale, float f) {
        RenderManager rm = mc.getRenderManager();
        d -= rm.viewerPosX;
        d2 -= rm.viewerPosY;
        d3 -= rm.viewerPosZ;
        if (f != -1.0f) {
            setupCamera(f);
        }
        GL11.glGetFloat(2982, modelView);
        GL11.glGetFloat(2983, projection);
        GL11.glGetInteger(2978, viewport);
        Vec3 vec3 = null;
        if (Project.gluProject((float) d, (float) d2, (float) d3, modelView, projection, viewport, screen)) {
            vec3 = new Vec3(screen.get(0) / (float) scale, ((float) Display.getHeight() - screen.get(1)) / (float) scale, screen.get(2));
        }
        setupOverlay();
        return vec3;
    }

    public static float nametagScale(double d, double d2, double d3, double d4, double d5, double d6) {
        float f = (float) NametagsModule.scale.getInput();
        if (mc.currentScreen == null && Zoom.isZooming()) {
            f *= 2.0f;
        }
        if (NametagsModule.autoScale.isToggled()) {
            double d7 = d - d4;
            double d8 = d2 - d5;
            double d9 = d3 - d6;
            double d10 = d7 * d7 + d8 * d8 + d9 * d9;
            if (d10 < 49.0) {
                double d11 = Math.sqrt(d10);
                f = Math.max((float) ((double) f * (7.0 / d11)), f);
            }
        } else {
            double d12 = d - d4;
            double d13 = d2 - d5;
            double d14 = d3 - d6;
            double d15 = Math.sqrt(d12 * d12 + d13 * d13 + d14 * d14);
            f = (float) ((double) f + 1.3);
            f = (float) ((double) f * ((double) f / d15));
        }
        return f;
    }

    public static void filledBox(AxisAlignedBB a, float r, float g, float b, float alpha) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double[][] v = {
                {a.minX, a.minY, a.minZ}, {a.minX, a.maxY, a.minZ}, {a.maxX, a.maxY, a.minZ}, {a.maxX, a.minY, a.minZ},
                {a.minX, a.minY, a.maxZ}, {a.maxX, a.minY, a.maxZ}, {a.maxX, a.maxY, a.maxZ}, {a.minX, a.maxY, a.maxZ},
                {a.minX, a.minY, a.minZ}, {a.minX, a.minY, a.maxZ}, {a.minX, a.maxY, a.maxZ}, {a.minX, a.maxY, a.minZ},
                {a.maxX, a.minY, a.minZ}, {a.maxX, a.maxY, a.minZ}, {a.maxX, a.maxY, a.maxZ}, {a.maxX, a.minY, a.maxZ},
                {a.minX, a.minY, a.minZ}, {a.maxX, a.minY, a.minZ}, {a.maxX, a.minY, a.maxZ}, {a.minX, a.minY, a.maxZ},
                {a.minX, a.maxY, a.minZ}, {a.minX, a.maxY, a.maxZ}, {a.maxX, a.maxY, a.maxZ}, {a.maxX, a.maxY, a.minZ}};
        for (double[] p : v) {
            w.pos(p[0], p[1], p[2]).color(r, g, b, alpha).endVertex();
        }
        t.draw();
    }

    public static void entityBox(Entity entity, int n, int n2, float f) {
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        double d4 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) f - rm.viewerPosX;
        double d5 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) f - rm.viewerPosY;
        double d6 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) f - rm.viewerPosZ;
        GlStateManager.pushMatrix();
        if (n == 3) {
            GL11.glTranslated(d4, d5 - 0.2, d6);
            GL11.glRotated(-rm.playerViewY, 0.0, 1.0, 0.0);
            GlStateManager.disableDepth();
            GL11.glScalef(0.03f, 0.03f, 0.03f);
            float f2 = 0.5f;
            RenderUtils.beginRect();
            RenderUtils.rawRect(-20.0f - f2, 75.0f, 20.0f + f2, 75.0f + f2, -15987700);
            RenderUtils.rawRect(-20.0f - f2, -1.0f - f2, 20.0f + f2, -1.0f, -15987700);
            RenderUtils.rawRect(-20.0f - f2, -1.0f, -20.0f, 75.0f, -15987700);
            RenderUtils.rawRect(20.0f + f2, -1.0f, 20.0f, 75.0f, -15987700);
            RenderUtils.endRect();
            f2 = 1.5f;
            if (n2 == 0) {
                int n4 = RenderUtils.rainbow(2L, 0L);
                int n5 = RenderUtils.rainbow(2L, 1000L);
                RenderUtils.gradientRect(-20.0f, -1.0f + f2, -20.0f + f2, 75.0f - f2, n4, n5);
                RenderUtils.gradientRect(20.0f, -1.0f + f2, 20.0f - f2, 75.0f - f2, n4, n5);
                RenderUtils.rect(-20.0f, 75.0f, 20.0f, 75.0f - f2, n4);
                RenderUtils.rect(-20.0f, -1.0f, 20.0f, -1.0f + f2, n5);
            } else {
                RenderUtils.rect(-20.0f, 75.0f, 20.0f, 75.0f - f2, n2);
                RenderUtils.rect(-20.0f, -1.0f, 20.0f, -1.0f + f2, n2);
                RenderUtils.rect(-20.0f, -1.0f + f2, -20.0f + f2, 75.0f - f2, n2);
                RenderUtils.rect(20.0f, -1.0f + f2, 20.0f - f2, 75.0f - f2, n2);
            }
            GlStateManager.enableDepth();
        } else if (n == 4) {
            healthBar((EntityLivingBase) entity, d4, d5, d6);
        } else if (n == 6) {
            ring(d4, d5, d6, 0.7, 45, 1.5, n2, n2 == 0);
        } else {
            if (n2 == 0) {
                n2 = RenderUtils.rainbow(2L, 0L);
            }
            float a = (float) (n2 >> 24 & 0xFF) / 255.0f;
            float r = (float) (n2 >> 16 & 0xFF) / 255.0f;
            float g = (float) (n2 >> 8 & 0xFF) / 255.0f;
            float b = (float) (n2 & 0xFF) / 255.0f;
            if (n == 5) {
                GL11.glTranslated(d4, d5 - 0.2, d6);
                GL11.glRotated(-rm.playerViewY, 0.0, 1.0, 0.0);
                GlStateManager.disableDepth();
                GL11.glScalef(0.03f, 0.03f, 0.03f);
                circle(0.0, 95, 10.0, 3, Color.black.getRGB());
                for (int i = 0; i < 6; ++i) {
                    circle(0.0, 95 + (10 - i), 3.0, 4, Color.black.getRGB());
                }
                for (int i = 0; i < 7; ++i) {
                    circle(0.0, 95 + (10 - i), 2.0, 4, n2);
                }
                circle(0.0, 95, 8.0, 3, n2);
                GlStateManager.enableDepth();
            } else {
                AxisAlignedBB bb = entity.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
                AxisAlignedBB bb2 = new AxisAlignedBB(bb.minX - entity.posX + d4, bb.minY - entity.posY + d5, bb.minZ - entity.posZ + d6,
                        bb.maxX - entity.posX + d4, bb.maxY - entity.posY + d5, bb.maxZ - entity.posZ + d6);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(3042);
                GL11.glDisable(3553);
                GL11.glDisable(2929);
                GL11.glDepthMask(false);
                if (n == 1) {
                    GlStateManager.color(r, g, b, a);
                    GL11.glEnable(2848);
                    GL11.glLineWidth(2.0f);
                    RenderGlobal.drawSelectionBoundingBox(bb2);
                    GL11.glDisable(2848);
                } else if (n == 2) {
                    filledBox(bb2, r, g, b, 0.37f);
                }
                RenderUtils.resetColor();
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDepthMask(true);
                GL11.glDisable(3042);
            }
        }
        GlStateManager.popMatrix();
    }

    public static float barHeight(EntityLivingBase e) {
        if ((double) e.height < 1.1 || e.isChild()) {
            return 40.0f;
        }
        return 70.0f;
    }

    public static void healthBar(EntityLivingBase e, double d, double d2, double d3) {
        GL11.glTranslated(d, d2 - 0.1, d3);
        GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0, 1.0, 0.0);
        GlStateManager.disableDepth();
        GL11.glScalef(0.03f, 0.03f, 0.03f);
        double d4 = e.getHealth() / e.getMaxHealth();
        if (d4 > 1.0) {
            d4 = 1.0;
        }
        float f = barHeight(e);
        float f2 = (float) ((double) f * d4);
        int n = 21;
        if (e.isChild()) {
            n -= 5;
        }
        RenderUtils.beginRect();
        RenderUtils.rawRect(n, -1.0f, n + 4, f + 1.0f, -15987700);
        RenderUtils.rawRect(n + 1, f2, n + 3, f, -11974327);
        RenderUtils.rawRect(n + 1, 0.0f, n + 3, f2, Strings.healthColor(d4));
        RenderUtils.endRect();
        GlStateManager.enableDepth();
    }

    public static void shadedBox(EntityLivingBase e, int n, float f) {
        RenderManager rm = mc.getRenderManager();
        double d = e.lastTickPosX + (e.posX - e.lastTickPosX) * (double) f - rm.viewerPosX;
        double d2 = e.lastTickPosY + (e.posY - e.lastTickPosY) * (double) f - rm.viewerPosY;
        double d3 = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * (double) f - rm.viewerPosZ;
        AxisAlignedBB bb = e.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        AxisAlignedBB bb3 = new AxisAlignedBB(bb.minX - e.posX + d, bb.minY - e.posY + d2, bb.minZ - e.posZ + d3,
                bb.maxX - e.posX + d, bb.maxY - e.posY + d2, bb.maxZ - e.posZ + d3);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float a = (float) (n >> 24 & 0xFF) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        filledBox(bb3, r, g, b, a);
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    public static void tracer(Entity e, float width, int n, float f) {
        RenderManager rm = mc.getRenderManager();
        double d = e.lastTickPosX + (e.posX - e.lastTickPosX) * (double) f - rm.viewerPosX;
        double d2 = (double) e.getEyeHeight() - 0.4 + e.lastTickPosY + (e.posY - e.lastTickPosY) * (double) f - rm.viewerPosY;
        double d3 = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * (double) f - rm.viewerPosZ;
        float a = (float) (n >> 24 & 0xFF) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        GL11.glEnable(3042);
        GL11.glEnable(2848);
        GL11.glDisable(2929);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(width);
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(2);
        GL11.glVertex3d(0.0, mc.thePlayer.getEyeHeight(), 0.0);
        GL11.glVertex3d(d, d2, d3);
        GL11.glEnd();
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
    }

    public static void ring(double d, double d2, double d3, double d4, int n, double d5, int n2, boolean rainbow) {
        if (n < 3) {
            return;
        }
        float a = (float) (n2 >> 24 & 0xFF) / 255.0f;
        float r = (float) (n2 >> 16 & 0xFF) / 255.0f;
        float g = (float) (n2 >> 8 & 0xFF) / 255.0f;
        float b = (float) (n2 & 0xFF) / 255.0f;
        mc.entityRenderer.disableLightmap();
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glEnable(2848);
        GL11.glDepthMask(false);
        GL11.glLineWidth((float) d5);
        if (!rainbow) {
            GlStateManager.color(r, g, b, a);
        }
        GL11.glBegin(1);
        long l = 0L;
        long l2 = 15000L / (long) n;
        long l3 = l2 / 2L;
        for (int i = 0; i < n * 2; ++i) {
            if (rainbow) {
                if (i % 2 != 0) {
                    if (i == 47) {
                        l = l3;
                    }
                    l += l2;
                }
                int c = RenderUtils.rainbow(2L, l);
                GlStateManager.color((float) (c >> 16 & 0xFF) / 255.0f, (float) (c >> 8 & 0xFF) / 255.0f, (float) (c & 0xFF) / 255.0f);
            }
            double d7 = Math.PI * 2 * (double) i / (double) n + Math.toRadians(180.0);
            GL11.glVertex3d(d + Math.cos(d7) * d4, d2, d3 + Math.sin(d7) * d4);
        }
        GL11.glEnd();
        RenderUtils.resetColor();
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glEnable(3553);
        mc.entityRenderer.enableLightmap();
    }

    public static void circle(double d, double d2, double d3, int n, int n2) {
        if (n < 3) {
            return;
        }
        float a = (float) (n2 >> 24 & 0xFF) / 255.0f;
        float r = (float) (n2 >> 16 & 0xFF) / 255.0f;
        float g = (float) (n2 >> 8 & 0xFF) / 255.0f;
        float b = (float) (n2 & 0xFF) / 255.0f;
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        double d4 = Math.toRadians(180.0);
        w.begin(6, DefaultVertexFormats.POSITION);
        for (int i = 0; i < n; ++i) {
            double d5 = Math.PI * 2 * (double) i / (double) n + d4;
            w.pos(d + Math.sin(d5) * d3, d2 + Math.cos(d5) * d3, 0.0).endVertex();
        }
        t.draw();
        RenderUtils.resetColor();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void beam(double d, double d2, double d3, double d4, double d5, int n, float f) {
        RenderManager rm = mc.getRenderManager();
        d -= rm.viewerPosX;
        d2 -= rm.viewerPosY;
        d3 -= rm.viewerPosZ;
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        double d6 = 0.0;
        double d7 = d4;
        double d8 = d + 0.5 - d5;
        double d9 = d + 0.5 + d5;
        double d10 = d3 + 0.5 - d5;
        double d11 = d3 + 0.5 + d5;
        w.begin(7, DefaultVertexFormats.POSITION_COLOR);
        w.pos(d8, d2 + d7, d10).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d6, d10).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d6, d10).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d7, d10).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d7, d11).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d6, d11).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d6, d11).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d7, d11).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d7, d11).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d6, d11).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d6, d10).color(r, g, b, f).endVertex();
        w.pos(d8, d2 + d7, d10).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d7, d10).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d6, d10).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d6, d11).color(r, g, b, f).endVertex();
        w.pos(d9, d2 + d7, d11).color(r, g, b, f).endVertex();
        t.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    public static void blockBox(int n, int n2, int n3, float f, float f2, float f3, float f4, float f5, float f6, int n4, boolean outline, boolean shade) {
        RenderManager rm = mc.getRenderManager();
        double d = (double) n - rm.viewerPosX;
        double d2 = (double) n2 - rm.viewerPosY;
        double d3 = (double) n3 - rm.viewerPosZ;
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float a = (float) (n4 >> 24 & 0xFF) / 255.0f;
        float r = (float) (n4 >> 16 & 0xFF) / 255.0f;
        float g = (float) (n4 >> 8 & 0xFF) / 255.0f;
        float b = (float) (n4 & 0xFF) / 255.0f;
        AxisAlignedBB bb = new AxisAlignedBB(d + (double) f, d2 + (double) f2, d3 + (double) f3, d + (double) f4, d2 + (double) f5, d3 + (double) f6);
        if (outline) {
            GlStateManager.color(r, g, b, a);
            GL11.glEnable(2848);
            GL11.glLineWidth(2.0f);
            RenderGlobal.drawSelectionBoundingBox(bb);
            GL11.glDisable(2848);
        }
        if (shade) {
            filledBox(bb, r, g, b, 0.42f);
        }
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    public static void block(BlockPos pos, int n, boolean outline, boolean shade) {
        blockBox(pos.getX(), pos.getY(), pos.getZ(), 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, n, outline, shade);
    }

    public static void face(BlockPos pos, EnumFacing facing, int n, boolean outline, boolean shade) {
        float f = 0.0f, f2 = 0.0f, f3 = 0.0f, f4 = 1.0f, f5 = 1.0f, f6 = 1.0f;
        switch (facing.ordinal() + 1) {
            case 1: f5 = 0.0f; break;
            case 2: f2 = 1.0f; break;
            case 3: f6 = 0.0f; break;
            case 4: f3 = 1.0f; break;
            case 5: f4 = 0.0f; break;
            case 6: f = 1.0f;
        }
        blockBox(pos.getX(), pos.getY(), pos.getZ(), f, f2, f3, f4, f5, f6, n, outline, shade);
    }

    public static void color(int n) {
        RenderUtils.setColor(n);
    }
}
