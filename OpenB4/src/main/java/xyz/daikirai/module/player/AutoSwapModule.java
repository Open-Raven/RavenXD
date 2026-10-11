package xyz.daikirai.module.player;

import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.SwapSlot;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class AutoSwapModule extends Module {
    public static SliderSetting switchAt;
    public static ButtonSetting onlySameType, spoofItem, swapToGreater;
    public static boolean swapped;

    public AutoSwapModule(int key) {
        super("AutoSwap", Category.player, key);
    }

    public void link() {
        switchAt = (SliderSetting) ModuleManager.setting(this, "Switch at");
        onlySameType = (ButtonSetting) ModuleManager.setting(this, "Only same type");
        spoofItem = (ButtonSetting) ModuleManager.setting(this, "Spoof item");
        swapToGreater = (ButtonSetting) ModuleManager.setting(this, "Swap to greater stack");
    }

    public static void swap(SwapSlot origin) {
        Minecraft mc = Minecraft.getMinecraft();
        swapped = false;
        if (Overlay.screenOpen()) {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        boolean isBlock = ItemUtils.isScaffoldBlock(held);
        if (!swapToGreater.enabled && isBlock && (double) held.stackSize > switchAt.getInput()) {
            return;
        }
        PlayerUtils.clearEmptyStacks();
        int best = -1;
        int slot = -1;
        for (int i = InventoryPlayer.getHotbarSize() - 1; i >= 0; --i) {
            if (i == mc.thePlayer.inventory.currentItem) continue;
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s == null || !(s.getItem() instanceof ItemBlock)) continue;
            if (best >= s.stackSize) continue;
            if (isBlock) {
                if (held.stackSize >= s.stackSize) continue;
                if (onlySameType.enabled && held.getItem() != s.getItem()) continue;
            }
            if (!ItemUtils.isScaffoldBlock(s)) continue;
            best = s.stackSize;
            slot = i;
        }
        if (slot != -1 && mc.thePlayer.inventory.currentItem != slot) {
            PlayerUtils.setSlot(slot, false);
            swapped = true;
        }
    }

    public static void restore(SwapSlot origin, boolean swapBack) {
        swapped = false;
        if (origin.slot == -1) {
            return;
        }
        int n = origin.slot;
        origin.slot = -1;
        if (!swapBack || Overlay.screenOpen()) {
            return;
        }
        Minecraft.getMinecraft().thePlayer.inventory.currentItem = n;
    }

    public static int countBlocks() {
        int n = 0;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            ItemStack s = Minecraft.getMinecraft().thePlayer.inventory.getStackInSlot(i);
            if (!ItemUtils.isScaffoldBlock(s)) continue;
            n += s.stackSize;
        }
        return n;
    }

    public static void remember(SwapSlot origin) {
        origin.slot = Minecraft.getMinecraft().thePlayer.inventory.currentItem;
        if (AutoToolModule.previousSlot != -1) {
            origin.slot = AutoToolModule.previousSlot;
        }
    }
}
