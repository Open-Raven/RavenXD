package openraven.ravenxd.mixin;

import openraven.ravenxd.hook.Hooks;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.logging.log4j.LogManager;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    // No logger field here on purpose: un-@Unique fields in a @Mixin class
    // can fail mixin validation on Mixin 0.7, so look the logger up inline.

    @Redirect(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Packet;processPacket(Lnet/minecraft/network/INetHandler;)V"))
    @SuppressWarnings("rawtypes")
    private void clickgui$receive(Packet packet, INetHandler handler) {
        // Hooks.onReceive is already fail-safe; this is a last-resort guard
        // so a bug in the hook itself can never kill the Netty channel.
        try {
            Hooks.onReceive(packet, handler);
        } catch (Throwable t) {
            LogManager.getLogger("RavenXD-Hooks").error("RavenXD: Hooks.onReceive escaped its own guard for " + (packet == null ? "null" : packet.getClass().getName()), t);
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void clickgui$send(Packet packet, CallbackInfo ci) {
        // Hooks.onSend is already fail-open; this guard only ensures a
        // packet is never silently dropped if the hook itself misbehaves.
        try {
            if (!Hooks.onSend(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
            LogManager.getLogger("RavenXD-Hooks").error("RavenXD: Hooks.onSend escaped its own guard for " + (packet == null ? "null" : packet.getClass().getName()) + "; sending packet anyway", t);
        }
    }
}
