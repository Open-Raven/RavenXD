package xyz.daikirai.util;

import xyz.daikirai.module.SettingsModule;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;

import java.util.Map;

public final class ItemUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private ItemUtils() {
    }

    public static boolean is(ItemStack itemStack, Item item) {
        return itemStack != null && itemStack.getItem() == item;
    }

    public static float digSpeed(ItemStack itemStack, Block block) {
        int n;
        float f = itemStack.getStrVsBlock(block);
        if (f > 1.0f && (n = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack)) > 0) {
            f += (float) (n * n + 1);
        }
        return f;
    }

    public static boolean sameItem(ItemStack a, ItemStack b) {
        Item item = a == null ? null : a.getItem();
        Item item2 = b == null ? null : b.getItem();
        return item == item2;
    }

    public static boolean isTool(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item instanceof ItemPickaxe) {
            return true;
        }
        if (item instanceof ItemAxe || item instanceof ItemSpade || item instanceof ItemHoe) {
            return true;
        }
        return item instanceof ItemShears;
    }

    public static double bowEnchantLevels(ItemStack itemStack) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack) + EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, itemStack) + EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack);
    }

    public static boolean isConsumable(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.getItem();
        return item instanceof ItemFood || item instanceof ItemPotion && !ItemPotion.isSplash(item.getMetadata(itemStack));
    }

    public static boolean isThrowable(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.getItem();
        return item == Items.fire_charge || item == Items.ender_pearl || item == Items.ender_eye || item == Items.snowball || item == Items.egg || item == Items.experience_bottle || item == Items.nether_star || item == Items.potionitem && ItemPotion.isSplash(item.getMetadata(itemStack));
    }

    public static boolean isWeapon(ItemStack itemStack) {
        if (itemStack != null) {
            Item item = itemStack.getItem();
            if (item instanceof ItemSword) {
                return true;
            }
            if (SettingsModule.weaponAxe.isToggled() && item instanceof ItemAxe) {
                return true;
            }
            if (SettingsModule.weaponHoe.isToggled() && item instanceof ItemHoe) {
                return true;
            }
            if (SettingsModule.weaponShovel.isToggled() && item instanceof ItemSpade) {
                return true;
            }
            if (SettingsModule.weaponRod.isToggled() && item == Items.fishing_rod) {
                return true;
            }
            if (SettingsModule.weaponStick.isToggled() && item == Items.stick) {
                return true;
            }
            NBTTagList nBTTagList;
            if (SettingsModule.weaponEnchanted.isToggled() && (nBTTagList = itemStack.getEnchantmentTagList()) != null) {
                int n = nBTTagList.tagCount();
                for (int i = 0; i < n; ++i) {
                    short s = nBTTagList.getCompoundTagAt(i).getShort("id");
                    if (s != Enchantment.sharpness.effectId && s != Enchantment.knockback.effectId && s != Enchantment.fireAspect.effectId) continue;
                    return true;
                }
            }
        } else {
            return SettingsModule.weaponFist.isToggled();
        }
        return false;
    }

    public static double attackDamage(ItemStack itemStack) {
        double d = 0.0;
        for (Map.Entry<String, AttributeModifier> entry : itemStack.getAttributeModifiers().entries()) {
            if (!entry.getKey().equals("generic.attackDamage")) continue;
            d = entry.getValue().getAmount();
            break;
        }
        int n = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack);
        int n2 = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack);
        return d + (double) n * 1.25 + (double) (n2 > 0 ? n2 * 4 - 1 : 0);
    }

    public static boolean isBlock(ItemStack itemStack, Block block) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.getItem();
        return item instanceof ItemBlock && ((ItemBlock) item).getBlock() == block;
    }

    public static boolean isSword(ItemStack itemStack) {
        return itemStack != null && itemStack.getItem() instanceof ItemSword;
    }

    public static String name(ItemStack itemStack) {
        return name(itemStack == null ? null : itemStack.getItem());
    }

    public static String name(Item item) {
        return item == null ? "null" : item.getRegistryName().replaceFirst("minecraft:", "");
    }

    public static double armorValue(ItemStack itemStack) {
        DamageSource damageSource = DamageSource.generic;
        int n = ((ItemArmor) itemStack.getItem()).damageReduceAmount;
        int n2 = 0;
        NBTTagList nBTTagList = itemStack.getEnchantmentTagList();
        if (nBTTagList != null) {
            for (int i = 0; i < nBTTagList.tagCount(); ++i) {
                short s = nBTTagList.getCompoundTagAt(i).getShort("id");
                short s2 = nBTTagList.getCompoundTagAt(i).getShort("lvl");
                Enchantment enchantment = Enchantment.getEnchantmentById(s);
                if (enchantment == null) continue;
                n2 += enchantment.calcModifierDamage(s2, damageSource);
            }
        }
        if (n2 > 25) {
            n2 = 25;
        }
        if (n2 < 0) {
            n2 = 0;
        }
        double d = (double) (3 * n2 + (n2 & 1)) / 4.0;
        return (double) n + d;
    }

    public static int bestToolSlot(Block block) {
        float f = 1.0f;
        int n = -1;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            float f2;
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null || !((f2 = digSpeed(itemStack, block)) > f)) continue;
            f = f2;
            n = i;
        }
        return n;
    }

    public static boolean hasDurability(ItemStack itemStack, double fraction) {
        return itemStack != null && (!itemStack.isItemStackDamageable() || itemStack.getMaxDamage() <= 0 || (double) (itemStack.getMaxDamage() - itemStack.getItemDamage()) / (double) itemStack.getMaxDamage() >= fraction);
    }

    public static boolean isPlaceable(Item item) {
        return item instanceof ItemBlock || item == Items.fire_charge || item == Items.spawn_egg || item == Items.bed || item == Items.repeater || item == Items.comparator || item == Items.skull || item == Items.item_frame;
    }

    public static boolean isBlock(ItemStack itemStack) {
        return itemStack != null && itemStack.getItem() instanceof ItemBlock;
    }

    public static boolean isScaffoldBlock(ItemStack itemStack) {
        if (BlockUtils.isBlockItem(itemStack)) {
            Block block = ((ItemBlock) itemStack.getItem()).getBlock();
            return BlockUtils.isPlaceable(block) && block != Blocks.pumpkin;
        }
        return false;
    }
}
