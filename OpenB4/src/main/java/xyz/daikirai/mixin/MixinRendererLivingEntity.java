package xyz.daikirai.mixin;

import xyz.daikirai.hook.Hooks;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    private static float clickgui$partialTicks;

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private void clickgui$capture(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        clickgui$partialTicks = partialTicks;
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLivingBase;prevRotationPitch:F", opcode = Opcodes.GETFIELD))
    private float clickgui$prevPitch(EntityLivingBase entity) {
        return Hooks.getRotationPitchHead(entity, clickgui$partialTicks);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/EntityLivingBase;rotationPitch:F", opcode = Opcodes.GETFIELD))
    private float clickgui$pitch(EntityLivingBase entity) {
        return Hooks.getRotationPitchHead(entity, clickgui$partialTicks);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(method = "renderLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/LayerRenderer;doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V"))
    private void clickgui$doRenderLayer(LayerRenderer layer, EntityLivingBase e, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        Hooks.doRenderLayer((RendererLivingEntity) (Object) this, layer, e, f, f2, f3, f4, f5, f6, f7);
    }
}
