package xyz.daikirai.client;

import xyz.daikirai.module.SettingsModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public final class CapeLayer {

    private CapeLayer() {
    }

    public static void render(RenderPlayer renderPlayer, AbstractClientPlayer player, float partialTicks) {
        if (!player.hasPlayerInfo() || player.isInvisible()) {
            return;
        }
        ResourceLocation rl = null;
        if (player.isUser()) {
            if (SettingsModule.cape != SettingsModule.Cape.None) {
                rl = SettingsModule.cape.texture;
            }
        }
        if (!(rl != null || player.isWearing(EnumPlayerModelParts.CAPE) && (rl = player.getLocationCape()) != null)) {
            return;
        }
        draw(rl, renderPlayer, player, partialTicks);
    }

    public static void draw(ResourceLocation rl, RenderPlayer renderPlayer, AbstractClientPlayer p, float f) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        renderPlayer.bindTexture(rl);
        GlStateManager.pushMatrix();
        boolean sneaking = p.isSneaking();
        if (sneaking) {
            GlStateManager.translate(0.0f, 0.145f, 0.11f);
        } else {
            GlStateManager.translate(0.0f, 0.0f, 0.125f);
        }
        GlStateManager.disableLighting();
        double d = p.prevChasingPosX + (p.chasingPosX - p.prevChasingPosX) * (double) f - (p.prevPosX + (p.posX - p.prevPosX) * (double) f);
        double d2 = p.prevChasingPosY + (p.chasingPosY - p.prevChasingPosY) * (double) f - (p.prevPosY + (p.posY - p.prevPosY) * (double) f);
        double d3 = p.prevChasingPosZ + (p.chasingPosZ - p.prevChasingPosZ) * (double) f - (p.prevPosZ + (p.posZ - p.prevPosZ) * (double) f);
        float yaw = xyz.daikirai.util.RotationManager.rotating ? p.renderYawOffset : p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * f;
        double d4 = MathHelper.sin(yaw *= (float) Math.PI / 180);
        double d5 = -MathHelper.cos(yaw);
        float f3 = MathHelper.clamp_float((float) d2 * 10.0f, -6.0f, 32.0f);
        float f4 = (float) (d * d4 + d3 * d5) * 100.0f;
        float f5 = (float) (d * d5 - d3 * d4) * 100.0f;
        if (f4 < 0.0f) {
            f4 = 0.0f;
        } else if (f4 > 165.0f) {
            f4 = 165.0f;
        }
        float f6 = p.prevCameraYaw + (p.cameraYaw - p.prevCameraYaw) * f;
        f3 += MathHelper.sin((p.prevDistanceWalkedModified + (p.distanceWalkedModified - p.prevDistanceWalkedModified) * f) * 6.0f) * 32.0f * f6;
        if (sneaking) {
            f3 += 25.0f;
        }
        GlStateManager.rotate(6.0f + f4 / 2.0f + f3, 1.0f, 0.0f, 0.0f);
        GlStateManager.rotate(f5 / 2.0f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-f5 / 2.0f, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
        renderPlayer.getMainModel().renderCape(0.0625f);
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
