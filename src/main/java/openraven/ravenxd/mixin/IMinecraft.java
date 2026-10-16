package openraven.ravenxd.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraft {
    @Accessor("timer")
    Timer getTimer();

    @Invoker("clickMouse")
    void invokeClickMouse();

    @Invoker("rightClickMouse")
    void invokeRightClickMouse();
}
