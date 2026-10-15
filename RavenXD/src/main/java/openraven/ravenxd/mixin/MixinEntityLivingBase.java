package openraven.ravenxd.mixin;

import openraven.ravenxd.hook.Hooks;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void clickgui$onJump(CallbackInfo ci) {
        if (Hooks.onJump((EntityLivingBase) (Object) this)) {
            ci.cancel();
        }
    }
}
