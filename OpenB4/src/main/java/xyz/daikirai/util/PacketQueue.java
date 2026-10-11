package xyz.daikirai.util;

import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public final class PacketQueue {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean active;
    private static int holders;
    private static final List<Packet<?>> held = new ArrayList<Packet<?>>();
    public static int ticks;

    private PacketQueue() {
    }

    public static void flush() {
        if (!held.isEmpty()) {
            for (Packet<?> p : held) {
                PacketUtils.sendQueued(p);
            }
            held.clear();
        }
    }

    public static void open() {
        if (!active) {
            active = true;
            ticks = 0;
        }
        ++holders;
    }

    public static void close() {
        if (!active) {
            return;
        }
        if (--holders > 0) {
            return;
        }
        if (blinking()) {
            holders = 1;
            return;
        }
        active = false;
        flush();
    }

    public static void closeAll() {
        if (!active) {
            return;
        }
        holders = 1;
        close();
        Module blink = ModuleManager.get("Blink");
        if (blinking()) {
            blink.disable();
        }
    }

    public static void onJoinWorld() {
        held.clear();
        closeAll();
    }

    public static void tick() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen instanceof GuiGameOver) {
            held.clear();
            closeAll();
            return;
        }
        ++ticks;
        Module blink = ModuleManager.get("Blink");
        if (blinking()) {
            blink.onPacketQueueTick();
        }
    }

    public static boolean hold(Packet<?> packet) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.isSingleplayer()) {
            return true;
        }
        if (!xyz.daikirai.hook.Hooks.finalSendCheck(packet)) {
            return false;
        }
        held.add(packet);
        Module blink = ModuleManager.get("Blink");
        if (blinking()) {
            blink.onPacketHeld(packet);
        }
        return false;
    }

    private static boolean blinking() {
        Module blink = ModuleManager.get("Blink");
        return blink != null && blink.isBlinking();
    }
}
