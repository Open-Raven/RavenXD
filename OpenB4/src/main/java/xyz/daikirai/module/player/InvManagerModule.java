package xyz.daikirai.module.player;

import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.script.ScriptProtected;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PacketUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class InvManagerModule extends Module {
    public static SliderSetting autoArmor, autoSort, chestStealer, inventoryCleaner;
    public static SliderSetting swordSlot, bowSlot, blocksSlot, goldenAppleSlot, projectileSlot, speedPotionSlot, enderPearlSlot, waterBucketSlot, lavaBucketSlot;
    public static ButtonSetting closeChest, closeInventory, disableInLobby, stealFromCustomChests, middleClickToClean, rodOverProjectile, onlyWithKnockback;

    enum Action {
        AUTOARMOR, SORT, STEALER, CLEANER;

        long getDelay() {
            switch (this) {
                case AUTOARMOR: return (long) autoArmor.getInput();
                case SORT: return (long) autoSort.getInput();
                case STEALER: return (long) chestStealer.getInput();
                default: return (long) inventoryCleaner.getInput();
            }
        }
    }

    enum ArmorSlot {
        BOOTS(0, 8), LEGGINGS(1, 7), CHESTPLATE(2, 6), HELMET(3, 5);

        final int index, slot;
        double value;
        int duplicates;

        ArmorSlot(int index, int slot) {
            this.index = index;
            this.slot = slot;
        }

        ItemStack getItemStack() {
            return Minecraft.getMinecraft().thePlayer.inventory.armorItemInSlot(this.index);
        }
    }

    private static final ArmorSlot[] armor = ArmorSlot.values();
    private static int bestSwordSlot = -1;
    private static double bestSword = -1.0;
    private static int swordDuplicates;
    private static int bestBowSlot = -1;
    private static double bestBow = -1.0;
    private static int bowDuplicates;
    private static long delay, waited, clicks, chestTicks;

    public InvManagerModule(int key) {
        super("InvManager", Category.player, key);
    }

    public void link() {
        closeChest = (ButtonSetting) ModuleManager.setting(this, "Close chest");
        closeInventory = (ButtonSetting) ModuleManager.setting(this, "Close inventory");
        disableInLobby = (ButtonSetting) ModuleManager.setting(this, "Disable in lobby");
        autoArmor = (SliderSetting) ModuleManager.setting(this, "Auto armor");
        autoSort = (SliderSetting) ModuleManager.setting(this, "Auto sort");
        chestStealer = (SliderSetting) ModuleManager.setting(this, "Chest stealer");
        stealFromCustomChests = (ButtonSetting) ModuleManager.setting(this, "Steal from custom chests");
        inventoryCleaner = (SliderSetting) ModuleManager.setting(this, "Inventory cleaner");
        middleClickToClean = (ButtonSetting) ModuleManager.setting(this, "Middle click to clean");
        swordSlot = (SliderSetting) ModuleManager.setting(this, "Sword");
        bowSlot = (SliderSetting) ModuleManager.setting(this, "Bow");
        blocksSlot = (SliderSetting) ModuleManager.setting(this, "Blocks");
        goldenAppleSlot = (SliderSetting) ModuleManager.setting(this, "Golden apple");
        projectileSlot = (SliderSetting) ModuleManager.setting(this, "Projectile/Rod");
        speedPotionSlot = (SliderSetting) ModuleManager.setting(this, "Speed potion");
        enderPearlSlot = (SliderSetting) ModuleManager.setting(this, "Ender pearl");
        waterBucketSlot = (SliderSetting) ModuleManager.setting(this, "Water bucket");
        lavaBucketSlot = (SliderSetting) ModuleManager.setting(this, "Lava bucket");
        rodOverProjectile = (ButtonSetting) ModuleManager.setting(this, "Rod over projectile");
        onlyWithKnockback = (ButtonSetting) ModuleManager.setting(this, "^ Only with knockback");
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    static class Scan {
        final IInventory inv;
        final int size;
        final double[] sword = {-1.0, -1.0};
        final double[] bow = {-1.0, -1.0};
        final double[][] armor = {{-1.0, -1.0, -1.0, -1.0}, {-1.0, -1.0, -1.0, -1.0}};
        int emptyMain, filled;
        boolean water, lava;

        Scan(IInventory inv, boolean player) {
            this.inv = inv;
            this.size = !player ? inv.getSizeInventory() : inv.getSizeInventory() - 4;
            int sw = (int) (swordSlot.getInput() - 1.0) + (!player ? (this.size != 54 ? 54 : 81) : 0);
            int bw = (int) (bowSlot.getInput() - 1.0) + (!player ? (this.size != 54 ? 54 : 81) : 0);
            for (int i = 0; i < this.size; ++i) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack != null) {
                    ++this.filled;
                    Item item = stack.getItem();
                    this.water |= item == Items.water_bucket;
                    this.lava |= item == Items.lava_bucket;
                } else if (i >= 9) {
                    ++this.emptyMain;
                }
                if (i == sw) {
                    swordDuplicates = 0;
                    if (stack != null && isSword(stack) && durable(stack)) {
                        bestSwordSlot = i;
                        bestSword = ItemUtils.attackDamage(stack);
                        continue;
                    }
                    bestSwordSlot = -1;
                    bestSword = -1;
                } else if (i == bw) {
                    bowDuplicates = 0;
                    if (stack != null && isBow(stack) && durable(stack)) {
                        bestBowSlot = i;
                        bestBow = ItemUtils.bowEnchantLevels(stack);
                        continue;
                    }
                    bestBowSlot = -1;
                    bestBow = -1;
                }
                if (stack == null || !durable(stack)) continue;
                if (isSword(stack)) {
                    double d = ItemUtils.attackDamage(stack);
                    if (d > this.sword[1]) {
                        this.sword[1] = d;
                        this.sword[0] = i;
                    }
                } else if (isBow(stack)) {
                    double d = ItemUtils.bowEnchantLevels(stack);
                    if (d > this.bow[1]) {
                        this.bow[1] = d;
                        this.bow[0] = i;
                    }
                } else if (isArmor(stack)) {
                    int type = 3 - ((ItemArmor) stack.getItem()).armorType;
                    double d = ItemUtils.armorValue(stack);
                    if (d > this.armor[1][type]) {
                        this.armor[1][type] = d;
                        this.armor[0][type] = i;
                    }
                }
            }
        }

        void dropWorse(Scan player) {
            if (player.sword[1] > this.sword[1]) {
                this.sword[1] = -1.0;
            }
            if (player.bow[1] > this.bow[1]) {
                this.bow[1] = -1.0;
            }
            for (int i = 0; i < this.armor[0].length; ++i) {
                if (player.armor[1][i] > this.armor[1][i]) {
                    this.armor[1][i] = -1.0;
                }
            }
        }
    }

    private static boolean sortBow(Scan s, boolean player) {
        if (s.bow[1] != -1.0) {
            if (bowSlot.isNotDefault()) {
                if (s.bow[1] > bestBow) {
                    int n = (int) s.bow[0];
                    if (player) {
                        n = n < 9 ? n + 36 : n;
                    }
                    return click(n, (int) (bowSlot.getInput() - 1.0), 2, Action.SORT);
                }
            } else if (!player) {
                return click((int) s.bow[0], 0, 1, Action.SORT);
            }
        }
        return false;
    }

    private static boolean sortSword(Scan s, boolean player) {
        if (s.sword[1] != -1.0) {
            if (swordSlot.isNotDefault()) {
                if (s.sword[1] > bestSword) {
                    int n = (int) s.sword[0];
                    if (player) {
                        n = n < 9 ? n + 36 : n;
                    }
                    return click(n, (int) (swordSlot.getInput() - 1.0), 2, Action.SORT);
                }
            } else if (!player) {
                return click((int) s.sword[0], 0, 1, Action.SORT);
            }
        }
        return false;
    }

    private static boolean preferredRod(ItemStack stack) {
        return rodOverProjectile.isToggled() && stack != null && stack.getItem() == Items.fishing_rod
                && (!onlyWithKnockback.isToggled() || EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack) > 0);
    }

    public static boolean pausedInLobby() {
        return disableInLobby.isToggled() && (Hypixel.state == Hypixel.State.LOBBY || Hypixel.skywars(1));
    }

    private static boolean isUtilityBlock(Item item) {
        return item == Item.getItemFromBlock(Blocks.chest) || item == Item.getItemFromBlock(Blocks.crafting_table)
                || item == Item.getItemFromBlock(Blocks.enchanting_table) || item == Item.getItemFromBlock(Blocks.anvil);
    }

    private static boolean durable(ItemStack stack) {
        return ItemUtils.hasDurability(stack, 0.1);
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = mc();
        if (mc.currentScreen == null || pausedInLobby()) {
            delay = 0L;
            chestTicks = 0L;
            return;
        }
        if (PacketUtils.sentClickWindow || PacketUtils.sentOpenInventory && (ModuleManager.enabled("InvMove") == null
                || xyz.daikirai.module.movement.InvMoveModule.inventory != xyz.daikirai.module.movement.InvMoveModule.Inventory.Close)) {
            return;
        }
        if (++waited < delay) {
            return;
        }
        clicks = 0L;
        waited = 0L;
        if (mc.currentScreen instanceof GuiInventory) {
            if (autoArmor.isNotDefault() || autoSort.isNotDefault() || inventoryCleaner.isNotDefault()) {
                updateArmor();
                Scan s = new Scan(mc.thePlayer.inventory, true);
                if (autoArmor.isNotDefault()) {
                    for (int n = 0; n < s.armor[0].length; ++n) {
                        if (s.armor[1][n] == -1.0) continue;
                        ArmorSlot a = armor[n];
                        if (!(s.armor[1][n] > a.value)) continue;
                        if (a.getItemStack() != null) {
                            click(a.slot, 0, 4, Action.AUTOARMOR);
                        }
                        int n2 = (int) s.armor[0][n];
                        int n3 = n2 < 9 ? n2 + 36 : n2;
                        if (!click(n3, 0, 1, Action.AUTOARMOR)) continue;
                        return;
                    }
                }
                if (autoSort.isNotDefault()) {
                    if (sortSword(s, true)) {
                        return;
                    }
                    if (sortBow(s, true)) {
                        return;
                    }
                    for (int n = 0; n < s.size; ++n) {
                        ItemStack stack = s.inv.getStackInSlot(n);
                        if (stack == null || isSword(stack) || isBow(stack) || isArmor(stack)) continue;
                        Item item = stack.getItem();
                        int n4 = n < 9 ? n + 36 : n;
                        if (isBlock(item)) {
                            int n5 = (int) (blocksSlot.getInput() - 1.0);
                            if (n5 == -1 || n == n5) continue;
                            ItemStack other = s.inv.getStackInSlot(n5);
                            if (other == null || !isBlock(other.getItem())) {
                                if (!click(n4, n5, 2, Action.SORT)) continue;
                                return;
                            }
                            if (other.stackSize >= 64) continue;
                            if (item == other.getItem() && stack.getItemDamage() == other.getItemDamage()) {
                                if (stack.stackSize == 64) {
                                    if (!click(n4, n5, 2, Action.SORT)) continue;
                                    return;
                                }
                                if (s.emptyMain == 0 && n != n4 || !click(n4, 0, 1, Action.SORT)) continue;
                                return;
                            }
                            if (stack.stackSize <= other.stackSize || !click(n4, n5, 2, Action.SORT)) continue;
                            return;
                        }
                        if (item == Items.golden_apple) {
                            int n5 = (int) (goldenAppleSlot.getInput() - 1.0);
                            if (n5 == -1 || n == n5) continue;
                            ItemStack other = s.inv.getStackInSlot(n5);
                            if (other != null && other.getItem() == Items.golden_apple) continue;
                            if (!click(n4, n5, 2, Action.SORT)) continue;
                            return;
                        }
                        if (item == Items.snowball || item == Items.egg || item == Items.fishing_rod) {
                            int n5 = (int) (projectileSlot.getInput() - 1.0);
                            if (n5 == -1 || n == n5) continue;
                            ItemStack other = s.inv.getStackInSlot(n5);
                            if (other == null || other.getItem() != Items.snowball && other.getItem() != Items.egg && other.getItem() != Items.fishing_rod) {
                                if (!click(n4, n5, 2, Action.SORT)) continue;
                                return;
                            }
                            if (preferredRod(other)) continue;
                            if (preferredRod(stack)) {
                                if (!click(n4, n5, 2, Action.SORT)) continue;
                                return;
                            }
                            if (item == Items.fishing_rod) continue;
                            if (stack.stackSize >= 16) {
                                if (stack.stackSize <= other.stackSize || !click(n4, n5, 2, Action.SORT)) continue;
                                return;
                            }
                            if (other.stackSize >= 16) continue;
                            if (item == other.getItem()) {
                                if (s.emptyMain == 0 && n != n4 || !click(n4, 0, 1, Action.SORT)) continue;
                                return;
                            }
                            if (stack.stackSize <= other.stackSize || !click(n4, n5, 2, Action.SORT)) continue;
                            return;
                        }
                        if (item == Items.water_bucket || item == Items.lava_bucket) {
                            int n5 = (int) ((item == Items.water_bucket ? waterBucketSlot : lavaBucketSlot).getInput() - 1.0);
                            if (n5 == -1 || n == n5) continue;
                            ItemStack other = s.inv.getStackInSlot(n5);
                            if (other != null && other.getItem() == item) continue;
                            if (!click(n4, n5, 2, Action.SORT)) continue;
                            return;
                        }
                        if (item == Items.potionitem) {
                            int n5 = (int) (speedPotionSlot.getInput() - 1.0);
                            if (n5 == -1 || n == n5) continue;
                            if (!isSpeedPotion(stack) || isSpeedPotion(s.inv.getStackInSlot(n5))) continue;
                            if (!click(n4, n5, 2, Action.SORT)) continue;
                            return;
                        }
                        int n5;
                        if (item != Items.ender_pearl || (n5 = (int) (enderPearlSlot.getInput() - 1.0)) == -1 || n == n5) continue;
                        ItemStack other = s.inv.getStackInSlot(n5);
                        if (other != null && other.getItem() == Items.ender_pearl) continue;
                        if (!click(n4, n5, 2, Action.SORT)) continue;
                        return;
                    }
                }
                if (inventoryCleaner.isNotDefault() && (!middleClickToClean.isToggled() || Mouse.isButtonDown(2))) {
                    ArrayList<Integer> seen = new ArrayList<Integer>();
                    for (int i = 0; i < s.size; ++i) {
                        ItemStack stack = s.inv.getStackInSlot(i);
                        if (stack == null) continue;
                        Item item = stack.getItem();
                        int n12 = i < 9 ? i + 36 : i;
                        if (isSword(stack)) {
                            if (durable(stack)) {
                                double d = ItemUtils.attackDamage(stack);
                                if (bestSword < d) continue;
                                if (bestSword == d && ++swordDuplicates <= 2) continue;
                            }
                        } else if (item == Items.bow) {
                            if (durable(stack)) {
                                double d = ItemUtils.bowEnchantLevels(stack);
                                if (bestBow < d) continue;
                                if (bestBow == d && ++bowDuplicates <= 2) continue;
                            }
                        } else if (isArmor(stack)) {
                            if (durable(stack)) {
                                int type = 3 - ((ItemArmor) item).armorType;
                                double d = armor[type].value;
                                double d2 = ItemUtils.armorValue(stack);
                                if (d < d2) continue;
                                if (d == d2 && ++armor[type].duplicates <= 1) continue;
                            }
                        } else {
                            if (isBlock(item) || item == Items.golden_apple || item == Items.snowball || item == Items.egg || item == Items.ender_pearl
                                    || item == Items.arrow || item == Items.spawn_egg || item == Items.clock || item == Items.diamond
                                    || item == Items.emerald || item == Items.gold_ingot || item == Items.iron_ingot || isUtilityBlock(item)) continue;
                            if (item == Items.potionitem) {
                                if (!isBadPotion(stack)) continue;
                            } else {
                                int id;
                                if (stack.getMaxStackSize() == 1 && !seen.contains(id = Item.getIdFromItem(item))) {
                                    seen.add(id);
                                    continue;
                                }
                            }
                        }
                        if (!click(n12, 1, 4, Action.CLEANER)) continue;
                        return;
                    }
                }
                if (closeInventory.isToggled()) {
                    mc.thePlayer.closeScreen();
                }
            }
        } else if (mc.currentScreen instanceof GuiChest && chestStealer.isNotDefault() && ++chestTicks > 1L) {
            IInventory chest = ((ContainerChest) mc.thePlayer.openContainer).getLowerChestInventory();
            if (!(stealFromCustomChests.isToggled() || chest.getName().isEmpty() || chest.getName().contains("Chest"))) {
                return;
            }
            updateArmor();
            Scan c = new Scan(chest, false);
            Scan p = new Scan(mc.thePlayer.inventory, true);
            c.dropWorse(p);
            if (p.size != p.filled) {
                if (sortSword(c, false)) {
                    return;
                }
                if (sortBow(c, false)) {
                    return;
                }
                for (int n = 0; n < c.armor[0].length; ++n) {
                    if (c.armor[1][n] == -1.0 || !(c.armor[1][n] > armor[n].value) || !click((int) c.armor[0][n], 0, 1, Action.STEALER)) continue;
                    return;
                }
                for (int n = 0; n < c.size; ++n) {
                    ItemStack stack = c.inv.getStackInSlot(n);
                    if (stack == null || isSword(stack) || isBow(stack) || isArmor(stack)) continue;
                    Item item = stack.getItem();
                    int n16;
                    ItemStack other;
                    if (isBlock(item)) {
                        n16 = (int) (blocksSlot.getInput() - 1.0);
                        if (n16 == -1) {
                            if (!click(n, 0, 1, Action.STEALER)) continue;
                            return;
                        }
                        other = p.inv.getStackInSlot(n16);
                        if ((other == null || !isBlock(other.getItem()) || ((ItemBlock) item).getBlock() != ((ItemBlock) other.getItem()).getBlock()
                                && stack.stackSize > other.stackSize) && click(n, n16, 2, Action.STEALER)) {
                            return;
                        }
                    } else if (item == Items.golden_apple) {
                        n16 = (int) (goldenAppleSlot.getInput() - 1.0);
                        if (n16 == -1) {
                            if (!click(n, 0, 1, Action.STEALER)) continue;
                            return;
                        }
                        other = p.inv.getStackInSlot(n16);
                        if ((other == null || other.getItem() != Items.golden_apple) && click(n, n16, 2, Action.STEALER)) {
                            return;
                        }
                    } else if (item == Items.snowball || item == Items.egg || item == Items.fishing_rod) {
                        n16 = (int) (projectileSlot.getInput() - 1.0);
                        if (n16 == -1) {
                            if (!click(n, 0, 1, Action.STEALER)) continue;
                            return;
                        }
                        other = p.inv.getStackInSlot(n16);
                        if (other == null || other.getItem() != Items.snowball && other.getItem() != Items.egg && other.getItem() != Items.fishing_rod
                                || preferredRod(stack) && !preferredRod(other)) {
                            if (click(n, n16, 2, Action.STEALER)) {
                                return;
                            }
                        } else if (stack.stackSize > other.stackSize && other.getItem() != Items.fishing_rod) {
                            boolean done = item == other.getItem() ? (stack.stackSize <= 16 ? click(n, 0, 1, Action.STEALER) : click(n, n16, 2, Action.STEALER))
                                    : click(n, n16, 2, Action.STEALER);
                            if (done) {
                                return;
                            }
                        }
                    } else {
                        if (item == Items.water_bucket || item == Items.lava_bucket) {
                            if (!(item == Items.water_bucket ? !p.water : !p.lava)) continue;
                            n16 = (int) ((item == Items.water_bucket ? waterBucketSlot : lavaBucketSlot).getInput() - 1.0);
                            if (n16 == -1) {
                                if (click(n, 0, 1, Action.STEALER)) {
                                    return;
                                }
                            } else if (click(n, n16, 2, Action.STEALER)) {
                                return;
                            }
                            if (item == Items.water_bucket) {
                                p.water = true;
                                continue;
                            }
                            p.lava = true;
                            continue;
                        }
                        if (item == Items.potionitem) {
                            if (isSpeedPotion(stack)) {
                                n16 = (int) (speedPotionSlot.getInput() - 1.0);
                                if (n16 == -1) {
                                    if (!click(n, 0, 1, Action.STEALER)) continue;
                                    return;
                                }
                                other = p.inv.getStackInSlot(n16);
                                if (!isSpeedPotion(other) && click(n, n16, 2, Action.STEALER)) {
                                    return;
                                }
                            }
                        } else if (item == Items.ender_pearl) {
                            n16 = (int) (enderPearlSlot.getInput() - 1.0);
                            if (n16 == -1) {
                                if (!click(n, 0, 1, Action.STEALER)) continue;
                                return;
                            }
                            other = p.inv.getStackInSlot(n16);
                            if ((other == null || other.getItem() != Items.ender_pearl) && click(n, n16, 2, Action.STEALER)) {
                                return;
                            }
                        }
                    }
                    if (item instanceof ItemBlock && !isBlock(item) && !isUtilityBlock(item)) continue;
                    if (!click(n, 0, 1, Action.STEALER)) continue;
                    return;
                }
            } else {
                for (int i = 0; i < c.size; ++i) {
                    Item item;
                    ItemStack stack = c.inv.getStackInSlot(i);
                    if (stack == null || !isBlock(item = stack.getItem()) && item != Items.golden_apple && item != Items.snowball && item != Items.egg
                            && item != Items.ender_pearl && item != Items.arrow && item != Items.clock && item != Items.diamond
                            && item != Items.emerald && item != Items.gold_ingot && item != Items.iron_ingot && !isUtilityBlock(item)) continue;
                    for (int j = 0; j < p.size; ++j) {
                        ItemStack own = p.inv.getStackInSlot(j);
                        if (own == null || item != own.getItem() || own.stackSize >= own.getMaxStackSize()) continue;
                        if (isBlock(item) && ((ItemBlock) item).getBlock() != ((ItemBlock) own.getItem()).getBlock()) continue;
                        if (!click(i, 0, 1, Action.STEALER)) continue;
                        return;
                    }
                }
            }
            if (closeChest.isToggled()) {
                mc.thePlayer.closeScreen();
            }
        }
    }

    private static boolean isBadPotion(ItemStack stack) {
        List<PotionEffect> list;
        if (stack != null && stack.getItem() == Items.potionitem && (list = ((ItemPotion) stack.getItem()).getEffects(stack)) != null) {
            for (PotionEffect e : list) {
                String s = e.toString();
                if (s.contains("poison") || s.contains("moveSlowdown")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSpeedPotion(ItemStack stack) {
        List<PotionEffect> list;
        if (stack != null && stack.getItem() == Items.potionitem && (list = ((ItemPotion) stack.getItem()).getEffects(stack)) != null) {
            for (PotionEffect e : list) {
                if (e.toString().contains("moveSpeed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void updateArmor() {
        for (ArmorSlot a : armor) {
            ItemStack stack = a.getItemStack();
            a.value = stack != null && isArmor(stack) && durable(stack) ? ItemUtils.armorValue(stack) : 0.0;
            a.duplicates = 0;
        }
    }

    private static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof ItemSword;
    }

    private static boolean click(int slot, int button, int mode, Action action) {
        delay = action.getDelay();
        ++clicks;
        ScriptProtected.guiClick(slot, button, mode);
        return delay != 0L;
    }

    @Override
    public void onDisable() {
        clicks = 0L;
        chestTicks = 0L;
        delay = 0L;
        waited = 0L;
    }

    private static boolean isBlock(Item item) {
        return item instanceof ItemBlock && BlockUtils.isPlaceable(((ItemBlock) item).getBlock());
    }

    private static boolean isBow(ItemStack stack) {
        return stack.getItem() == Items.bow;
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ItemArmor;
    }
}
