package openraven.ravenxd.mixin;

import net.minecraft.network.play.client.C03PacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(C03PacketPlayer.class)
public interface IC03PacketPlayer {
    @Accessor("onGround")
    void setOnGround(boolean onGround);
}
