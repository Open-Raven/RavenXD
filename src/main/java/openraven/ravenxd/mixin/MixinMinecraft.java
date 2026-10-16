package openraven.ravenxd.mixin;

import openraven.ravenxd.hook.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Unique
    private long clickgui$lastFrame = System.nanoTime();

    // renderDeltaTime (not rlly useful rn cuz i deleted the anims)
    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void clickgui$updateDeltaTime(CallbackInfo ci) {
        long now = System.nanoTime();
        openraven.ravenxd.util.RenderUtils.renderDeltaTime = (now - this.clickgui$lastFrame) / 1_000_000f;
        this.clickgui$lastFrame = now;
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;getMouseOver(F)V"))
    private void clickgui$getMouseOver(EntityRenderer renderer, float partialTicks) {
        Hooks.getMouseOver();
    }
}
