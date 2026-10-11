package xyz.daikirai.mixin;

import xyz.daikirai.hook.Hooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {
    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onUpdate()V"))
    private void clickgui$onUpdate(CallbackInfo ci) {
        Hooks.onUpdate();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void clickgui$onUpdateWalkingPlayer(CallbackInfo ci) {
        Hooks.onUpdateWalkingPlayer();
        ci.cancel();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;moveStrafe:F", opcode = 181, ordinal = 0))
    private void clickgui$slowdownStrafe(MovementInput input, float f) {
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;moveForward:F", opcode = 181, ordinal = 0))
    private void clickgui$slowdownForward(MovementInput input, float f) {
        Hooks.onPlayerSlowdown();
    }

    @Inject(method = "updateEntityActionState", at = @At("RETURN"))
    private void clickgui$updateEntityActionState(CallbackInfo ci) {
        Hooks.onUpdateEntityActionState();
    }
}
