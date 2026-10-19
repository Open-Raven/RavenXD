package openraven.ravenxd.module.movement;

import openraven.ravenxd.jade.Stasis;
import openraven.ravenxd.jade.compat.BooleanSetting;
import openraven.ravenxd.jade.compat.EventBus;
import openraven.ravenxd.jade.compat.LoadWorldEvent;
import openraven.ravenxd.jade.compat.MoveInputEvent;
import openraven.ravenxd.jade.compat.PacketReceiveEvent;
import openraven.ravenxd.jade.compat.PacketSendEvent;
import openraven.ravenxd.jade.compat.PostUpdateEvent;
import openraven.ravenxd.jade.compat.PreUpdateEvent;
import openraven.ravenxd.jade.compat.RightClickEvent;
import openraven.ravenxd.jade.compat.TickStartEvent;
import openraven.ravenxd.jade.compat.VelocityEvent;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.MovementInput;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

//wrapper around jade stasis
public class StasisModule extends Module {
    public static StasisModule instance;
    private final Stasis stasis = new Stasis();

    public StasisModule(int key) {
        super("Stasis", Category.movement, key);
        instance = this;
        EventBus.register(this.stasis);
    }

    public void link() {
        for (BooleanSetting s : this.stasis.settings) {
            s.bind((ButtonSetting) ModuleManager.setting(this, s.name));
        }
    }

    @Override
    public void onEnable() {
        this.stasis.enabled = true;
        this.stasis.onEnable();
    }

    @Override
    public void onDisable() {
        this.stasis.onDisable();
        this.stasis.enabled = false;
    }

    @Override
    public void guiUpdate() {
        this.stasis.guiUpdate();
    }

    @Override
    public void onPreUpdate() {
        EventBus.post(new PreUpdateEvent());
    }

    public void postUpdate() {
        EventBus.post(new PostUpdateEvent());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            EventBus.post(new TickStartEvent());
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            EventBus.post(new LoadWorldEvent());
        }
    }

    // input lock no matter what state
    @Override
    public void onPostPlayerInputAlways() {
        MovementInput input = mc.thePlayer.movementInput;
        MoveInputEvent e = EventBus.post(new MoveInputEvent(input.moveForward, input.moveStrafe, input.jump, input.sneak));
        input.moveForward = e.getMoveForward();
        input.moveStrafe = e.getMoveStrafe();
        input.jump = e.isJumping();
        input.sneak = e.isSneaking();
    }

    public boolean rightClick() {
        return EventBus.post(new RightClickEvent()).isCanceled();
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (EventBus.post(new PacketReceiveEvent(packet)).isCanceled()) {
            return false;
        }
        if (packet instanceof S12PacketEntityVelocity) {
            EventBus.post(new VelocityEvent((S12PacketEntityVelocity) packet));
        }
        return true;
    }

    @Override
    public boolean onSendPacketAlways(Packet<?> packet) {
        return !EventBus.post(new PacketSendEvent(packet)).isCanceled();
    }
}
