package openraven.ravenxd.mixin;

import openraven.ravenxd.module.render.CameraModules;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @ModifyConstant(method = "hurtCameraEffect", constant = @Constant(floatValue = 14.0F))
    private float clickgui$hurtCam(float f) {
        return CameraModules.hurtCam();
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;)Lnet/minecraft/util/MovingObjectPosition;"))
    private MovingObjectPosition clickgui$clip(WorldClient world, Vec3 from, Vec3 to) {
        return CameraModules.shouldCameraClip() ? world.rayTraceBlocks(from, to) : null;
    }
}
