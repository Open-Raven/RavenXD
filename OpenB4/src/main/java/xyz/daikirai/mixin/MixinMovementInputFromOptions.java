package xyz.daikirai.mixin;

import xyz.daikirai.hook.Hooks;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions {
    @Inject(method = "updatePlayerMoveState", at = @At("RETURN"))
    private void clickgui$postPlayerInput(CallbackInfo ci) {
        Hooks.postPlayerInput();
    }
}
