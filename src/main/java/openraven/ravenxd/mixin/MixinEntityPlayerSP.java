package openraven.ravenxd.mixin;

import openraven.ravenxd.hook.Hooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.logging.log4j.LogManager;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {
    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onUpdate()V"))
    private void clickgui$onUpdate(CallbackInfo ci) {
        Hooks.onUpdate();
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void clickgui$onPostUpdate(CallbackInfo ci) {
        Hooks.onPostUpdate();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void clickgui$onUpdateWalkingPlayer(CallbackInfo ci) {
        // Only cancel vanilla when the RavenXD reimplementation actually
        // completed. On failure (logged in Hooks) vanilla runs as a fallback
        // instead of the player silently freezing. The try/catch here is
        // defence in depth: Hooks already contains its own failures, so
        // this only fires if Hooks itself fails to even run (e.g. class
        // initialisation) -- in which case vanilla must still run.
        try {
            if (Hooks.onUpdateWalkingPlayer()) {
                ci.cancel();
            }
        } catch (Throwable t) {
            LogManager.getLogger("RavenXD-Hooks").error("RavenXD: Hooks.onUpdateWalkingPlayer escaped its own guard; running vanilla movement update", t);
        }
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
