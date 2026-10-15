package xyz.daikirai.gui;

import xyz.daikirai.client.Chat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ConsolePing {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String PROBE = "///";
    private static final String REPLY = "Unknown";

    public static boolean pending;
    public static ConsolePing listener;
    public static long sentAt;

    public static void start() {
        Console.print("&eChecking...", 1);
        if (pending) {
            Console.print("&cPlease wait.", 0);
            return;
        }
        listener = new ConsolePing();
        MinecraftForge.EVENT_BUS.register(listener);
        mc.thePlayer.sendChatMessage(PROBE);
        pending = true;
        sentAt = System.currentTimeMillis();
    }

    public void finish() {
        pending = false;
        if (listener != null) {
            MinecraftForge.EVENT_BUS.unregister(listener);
            listener = null;
        }
        int n = (int) (System.currentTimeMillis() - sentAt) - 20;
        if (n < 0) {
            n = 0;
        }
        Console.print("Your ping: " + n + "ms", 0);
        reset();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!pending || event.type == 2 || !Chat.inWorld()) {
            return;
        }
        String s = Chat.strip(event.message.getUnformattedText());
        if (s.isEmpty()) {
            return;
        }
        if (s.startsWith(REPLY)) {
            event.setCanceled(true);
            this.finish();
        }
    }

    public static void reset() {
        pending = false;
        sentAt = 0L;
        listener = null;
    }
}
