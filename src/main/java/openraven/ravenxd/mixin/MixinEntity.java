package openraven.ravenxd.mixin;

import openraven.ravenxd.hook.Hooks;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyVariable(method = "moveEntity", at = @At("STORE"), index = 19)
    private boolean clickgui$stopOnEdge(boolean flag) {
        return Hooks.stopOnEdge((Entity) (Object) this);
    }

    @Inject(method = "moveEntity", at = @At("HEAD"))
    private void clickgui$step(double x, double y, double z, CallbackInfo ci) {
        Hooks.onStep((Entity) (Object) this);
    }

    @Redirect(method = "moveFlying", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F", opcode = 180))
    private float clickgui$movementYaw(Entity entity) {
        return Hooks.getMovementYaw(entity);
    }
}
