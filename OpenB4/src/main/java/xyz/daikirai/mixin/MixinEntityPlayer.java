package xyz.daikirai.mixin;

import xyz.daikirai.hook.Hooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {
    @Shadow
    private int itemInUseCount;

    private static Entity clickgui$attackTarget;

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void clickgui$isBlocking(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Hooks.isBlocking((EntityPlayer) (Object) this));
    }

    @Inject(method = "getItemInUseCount", at = @At("HEAD"), cancellable = true)
    private void clickgui$getItemInUseCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Hooks.getItemInUseCount((EntityPlayer) (Object) this, this.itemInUseCount));
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"))
    private void clickgui$captureTarget(Entity target, CallbackInfo ci) {
        clickgui$attackTarget = target;
    }

    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;motionX:D", opcode = 181, ordinal = 0))
    private void clickgui$slowdownX(EntityPlayer player, double d) {
    }

    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;motionZ:D", opcode = 181, ordinal = 0))
    private void clickgui$slowdownZ(EntityPlayer player, double d) {
    }

    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"))
    private void clickgui$slowdownSprint(EntityPlayer player, boolean sprint) {
        Hooks.onAttackSlowdown(clickgui$attackTarget);
    }
}
