package openraven.ravenxd;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.client.ChatCommands;
import openraven.ravenxd.client.ClientConfig;
import openraven.ravenxd.client.Hypixel;
import openraven.ravenxd.client.Irc;
import openraven.ravenxd.client.MiddleClickFriends;
import openraven.ravenxd.client.Sounds;
import openraven.ravenxd.gui.ClickGui;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.client.RenderDispatcher;
import openraven.ravenxd.module.HudModule;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.util.Zoom;
import openraven.ravenxd.util.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod(modid = "ravenxd", name = openraven.ravenxd.client.Account.NAME, version = openraven.ravenxd.client.Account.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class ClickGuiMod {
    private static final Minecraft mc = Minecraft.getMinecraft();

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ModuleManager.init();
        openraven.ravenxd.script.ScriptReflection.init();
        openraven.ravenxd.script.ScriptEvents.bind(openraven.ravenxd.script.ScriptDelegated.class);
        ClickGui.instance = new ClickGui();
        openraven.ravenxd.gui.nplus.NPlusGui.instance = new openraven.ravenxd.gui.nplus.NPlusGui();
        ClientConfig.load();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RenderDispatcher());
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new openraven.ravenxd.client.KeystrokesGui.Command());
        MinecraftForge.EVENT_BUS.register(new openraven.ravenxd.client.Keystrokes());
        openraven.ravenxd.client.Keystrokes.instance = new openraven.ravenxd.client.Keystrokes();
        openraven.ravenxd.client.Keystrokes.load();
    }

    private static boolean isDown(int key) {
        return key != 0 && (key < 0 ? Mouse.isButtonDown(key + 100) : Keyboard.isKeyDown(key));
    }

    private final java.util.Map<Module, Boolean> held = new java.util.HashMap<Module, Boolean>();

    private boolean pollBind(Module m) {
        if (m.getKeycode() == 0 || m.getKeycode() == -1) {
            return false;
        }
        boolean wasHeld = Boolean.TRUE.equals(this.held.get(m));
        if (isDown(m.getKeycode())) {
            if (!wasHeld) {
                m.toggle();
                wasHeld = true;
            }
        } else {
            wasHeld = false;
        }
        this.held.put(m, wasHeld);
        return wasHeld;
    }

    private static boolean firstTick = true;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        if (openraven.ravenxd.util.PacketQueue.active) {
            openraven.ravenxd.util.PacketQueue.tick();
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (firstTick) {
            firstTick = false;
            openraven.ravenxd.script.ScriptManager.reload();
            openraven.ravenxd.client.Profiles.loadDefault();
            Zoom.find();
        }
        boolean profileHandled = false;
        for (Module m : ModuleManager.modules) {
            if (m.moduleCategory() == Category.profiles) {
                if (profileHandled || mc.currentScreen != null || !this.pollBind(m)) continue;
                profileHandled = true;
                continue;
            }
            if (mc.currentScreen == null) {
                this.pollBind(m);
            } else if (ClickGui.isOpen(mc.currentScreen)) {
                m.guiUpdate();
            }
            if (!m.isEnabled()) continue;
            m.onUpdate();
        }
        if (ModuleManager.hud.isEnabled()) {
            HudModule.sort();
        }
        Irc.tick();
        Sounds.flush();
        if (openraven.ravenxd.client.KeystrokesGui.open) {
            openraven.ravenxd.client.KeystrokesGui.open = false;
            mc.displayGuiScreen(new openraven.ravenxd.client.KeystrokesGui());
        }
        if (mc.thePlayer.isRiding()) {
            if (RotationManager.rotating) {
                RotationManager.updateBody(RotationManager.serverYaw);
            }
            RotationManager.wasRotating = RotationManager.rotating;
            RotationManager.setLast(RotationManager.serverYaw, RotationManager.serverPitch, RotationManager.lastPitch);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Chat.inWorld()) {
            return;
        }
        String s = Chat.strip(e.message.getUnformattedText());
        if (s.isEmpty()) {
            return;
        }
        if (ChatCommands.pingSentAt > 0L) {
            ChatCommands.onChat(e, s);
        }
        for (String name : new String[]{"AntiBot", "AutoWho", "AutoRequeue", "Duels Stats", "NoFall", "Sky Wars", "Speed Builders"}) {
            Module m = ModuleManager.enabled(name);
            if (m != null) {
                m.onChat(e, s);
            }
        }
        if (s.charAt(0) == ' ') {
            boolean bedWars = ModuleManager.enabled("Bed Wars") != null;
            boolean bedEsp = ModuleManager.enabled("BedESP") != null && openraven.ravenxd.module.render.BedESPModule.optimizeForBedWars.isToggled();
            boolean autoWho = ModuleManager.enabled("AutoWho") != null && openraven.ravenxd.module.minigames.AutoWhoModule.runBedWarsInGame.isToggled();
            if (bedWars || bedEsp || autoWho) {
                openraven.ravenxd.module.minigames.BedWarsModule.onGameStartChat(e, s, bedWars, bedEsp, autoWho);
            }
        }
                if (Hypixel.onChat(s)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onJoinQueue(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            openraven.ravenxd.util.PacketQueue.onJoinWorld();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            Hypixel.joinedAt = System.currentTimeMillis();
            Hypixel.pendingLocraw = true;
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        openraven.ravenxd.util.ClickUtils.onMouse(e);
    }
}
