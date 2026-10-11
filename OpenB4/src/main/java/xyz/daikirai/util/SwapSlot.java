package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public enum SwapSlot {
    FastPlace,
    Scaffold;

    public int slot;

    public ItemStack getItemStack() {
        return Minecraft.getMinecraft().thePlayer.inventory.getStackInSlot(this.slot);
    }
}
