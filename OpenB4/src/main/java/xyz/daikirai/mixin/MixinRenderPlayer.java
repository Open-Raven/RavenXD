package xyz.daikirai.mixin;

import xyz.daikirai.hook.Hooks;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    @Inject(method = "setModelVisibilities", at = @At("RETURN"))
    private void clickgui$updateModelVisibilities(AbstractClientPlayer player, CallbackInfo ci) {
        Hooks.updateModelVisibilities(player, ((RenderPlayer) (Object) this).getMainModel());
    }
}
