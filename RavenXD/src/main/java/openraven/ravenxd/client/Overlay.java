package xyz.daikirai.client;

import xyz.daikirai.gui.ClickGui;
import xyz.daikirai.gui.HudEditor;
import xyz.daikirai.module.SettingsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

public final class Overlay {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private Overlay() {
    }

    public static boolean canRender() {
        return Chat.inWorld() && !mc.gameSettings.showDebugInfo &&(mc.currentScreen == null || !SettingsModule.chatAsInventory.isToggled() && mc.currentScreen instanceof GuiChat);
    }

    public static boolean screenOpen() {
        return !(mc.currentScreen == null || ownScreen() || !SettingsModule.chatAsInventory.isToggled() && mc.currentScreen instanceof GuiChat);
    }

    public static boolean ownScreen() {
        return ClickGui.isOpen(mc.currentScreen) || mc.currentScreen instanceof HudEditor;
    }
}
