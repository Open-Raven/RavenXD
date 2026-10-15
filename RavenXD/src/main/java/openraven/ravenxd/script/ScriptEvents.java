package xyz.daikirai.script;

import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.util.PlayerUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ScriptEvents {
    static Runnable preUpdate;
    static Function<Object[], Object[]> preMotion;
    static Predicate<Packet> packetSent;
    static Predicate<Packet> packetReceived;
    static Consumer<Entity> worldJoin;
    static BiConsumer<String, EntityPlayer> antiCheatFlag;
    static BiConsumer<String, EntityPlayer> bedWarsAlert;
    static GuiScreen openedScreen;

    public static boolean bind(Class<?> clazz) {
        preUpdate = ScriptInvoker.runnable(clazz, "m7a3f19c8e42bd6");
        preMotion = ScriptInvoker.function(clazz, "mc5810fa29d73e4", Object[].class, Object[].class);
        packetSent = ScriptInvoker.predicate(clazz, "mf2946c1e83ab75", Packet.class);
        packetReceived = ScriptInvoker.predicate(clazz, "m63b8ed2f901ca4", Packet.class);
        worldJoin = ScriptInvoker.consumer(clazz, "m18de7b40ac926f", Entity.class);
        antiCheatFlag = ScriptInvoker.biConsumer(clazz, "ma047d95c3e68bf", String.class, EntityPlayer.class);
        bedWarsAlert = ScriptInvoker.biConsumer(clazz, "m9e2c74a1fd5038", String.class, EntityPlayer.class);
        return preUpdate != null && preMotion != null && packetSent != null && packetReceived != null && worldJoin != null && antiCheatFlag != null && bedWarsAlert != null;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent mouseEvent) {
        if (mouseEvent.button == -1 && mouseEvent.dwheel == 0) {
            return;
        }
        boolean bl = mouseEvent.buttonstate;
        if (mouseEvent.dwheel > 0) {
            bl = true;
        }
        if (ScriptInvoker.dispatchBoolean(ScriptEvent.MOUSE, mouseEvent.button, bl, mouseEvent.x, mouseEvent.y) == 0) {
            mouseEvent.setCanceled(true);
        }
    }

    public static void antiCheatFlag(String string, EntityPlayer entityPlayer) {
        antiCheatFlag.accept(string, entityPlayer);
    }

    public static void postMotion() {
        ScriptInvoker.dispatch(ScriptEvent.POST_MOTION);
    }

    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Post post) {
        int n = Keyboard.getEventKey();
        ScriptInvoker.dispatch(ScriptEvent.KEY, Keyboard.getKeyName(n), n, Keyboard.getEventKeyState(), true);
    }

    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre pre) {
        int n = Mouse.getEventButton();
        int n2 = Mouse.getEventDWheel();
        if (n == -1 && n2 == 0) {
            return;
        }
        boolean bl = Mouse.getEventButtonState();
        if (n2 > 0) {
            bl = true;
        }
        if (ScriptInvoker.dispatchBoolean(ScriptEvent.MOUSE, n, bl, Mouse.getEventX(), Mouse.getEventY()) == 0) {
            pre.setCanceled(true);
        }
    }

    public static Float movementYaw(float f) {
        return ScriptInvoker.dispatchFloat(ScriptEvent.GET_MOVEMENT_YAW, f);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!ScriptUtil.inGame()) {
            return;
        }
        if (ScriptInvoker.dispatchBoolean(ScriptEvent.CHAT, event.message.getFormattedText(), event.type) == 0) {
            event.setCanceled(true);
        }
    }

    public static boolean ircMessage(String string) {
        return ScriptInvoker.dispatchBoolean(ScriptEvent.IRC_MESSAGE, string) != 0;
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        int n = Keyboard.getEventKey();
        ScriptInvoker.dispatch(ScriptEvent.KEY, Keyboard.getKeyName(n), n, Keyboard.getEventKeyState(), false);
    }

    public static float[] rotations(float f, float f2) {
        return ScriptInvoker.dispatchFloatArray(ScriptEvent.GET_ROTATIONS, f, f2);
    }

    public static void bedWarsAlert(String string, EntityPlayer entityPlayer) {
        bedWarsAlert.accept(string, entityPlayer);
    }

    public static void preUpdate() {
        preUpdate.run();
        if (openedScreen != null) {
            ScriptInvoker.dispatch(ScriptEvent.GUI_UPDATE, PlayerUtils.screenName(openedScreen), true);
            openedScreen = null;
        }
        ScriptInvoker.dispatch(ScriptEvent.PRE_UPDATE);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ScriptUtil.inGame()) {
            return;
        }
        ScriptInvoker.dispatch(ScriptEvent.RENDER_TICK, event.renderTickTime);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!ScriptUtil.inGame()) {
            return;
        }
        ScriptInvoker.dispatch(ScriptEvent.RENDER_WORLD, event.partialTicks);
    }

    public static void preMotion(MotionEvent e) {
        Object[] objectArray = preMotion.apply(e.toArray());
        e.set(objectArray);
    }

    public static void postPlayerInput() {
        ScriptInvoker.dispatch(ScriptEvent.POST_PLAYER_INPUT);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote) {
            worldJoin.accept(event.entity);
        }
    }

    public static boolean packetSent(Packet packet) {
        return packetSent.test(packet);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null) {
            openedScreen = event.gui;
        } else if (Defaults.mc.currentScreen != null) {
            openedScreen = null;
            ScriptInvoker.dispatch(ScriptEvent.GUI_UPDATE, PlayerUtils.screenName(Defaults.mc.currentScreen), false);
        }
    }

    public static boolean packetReceived(Packet packet) {
        return packetReceived.test(packet);
    }

    public static void locraw(String string) {
        ScriptInvoker.dispatch(ScriptEvent.LOCRAW, string);
    }
}
