package xyz.daikirai.module;

import xyz.daikirai.gui.CategoryComponent;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public enum Category {
    combat(new ItemStack(Items.diamond_sword)),
    movement(new ItemStack(Items.diamond_boots)),
    player(new ItemStack(Items.golden_apple, 1, 1), new ItemStack(Items.golden_apple, 1, 0)),
    world(new ItemStack((Item) Items.filled_map)),
    render(new ItemStack(Items.ender_eye)),
    network(new ItemStack(Items.comparator)),
    minigames(new ItemStack(Items.gold_ingot)),
    other(new ItemStack(Items.clock)),
    client(new ItemStack(Items.compass)),
    scripts(new ItemStack(Items.redstone)),
    profiles(new ItemStack(Items.book));

    public final ItemStack activeIcon;
    public final ItemStack icon;
    public CategoryComponent manager;

    Category(ItemStack icon) {
        this(icon, icon);
    }

    Category(ItemStack activeIcon, ItemStack icon) {
        this.activeIcon = activeIcon;
        this.icon = icon;
    }

    public void setManager(CategoryComponent c) {
        this.manager = c;
    }
}
