package xyz.daikirai.mixin;

import xyz.daikirai.hook.Hooks;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyVariable(method = "moveEntity", at = @At("STORE"), index = 19)
    private boolean clickgui$stopOnEdge(boolean flag) {
        return Hooks.stopOnEdge((Entity) (Object) this);
    }

    @Redirect(method = "moveFlying", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F", opcode = 180))
    private float clickgui$movementYaw(Entity entity) {
        return Hooks.getMovementYaw(entity);
    }
}
