package openraven.ravenxd.util;

import openraven.ravenxd.module.SettingsModule;
import openraven.ravenxd.module.render.NametagsModule;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.util.Map;

public final class Strings {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final char SECTION = '§';
    public static final char HEART = '❤';
    public static final String RED = "§c", GOLD = "§6", YELLOW = "§e", GREEN = "§a", DARK_GREEN = "§2";

    private Strings() {
    }

    public static float health(EntityLivingBase e) {
        return e.getHealth() + e.getAbsorptionAmount();
    }

    public static double round(double d, int n) {
        if (n == 0) {
            return Math.round(d);
        }
        double d2 = Math.pow(10.0, n);
        return (double) Math.round(d * d2) / d2;
    }

    public static void heart(StringBuilder sb) {
        sb.append(SECTION);
        sb.append('c');
        sb.append(HEART);
    }

    public static StringBuilder reset(StringBuilder sb) {
        sb.append(SECTION).append('r').append(' ');
        return sb;
    }

    public static String health(double d, double d2) {
        String prefix = d < 0.3 ? RED : (d < 0.5 ? GOLD : (d < 0.7 ? YELLOW : GREEN));
        if (SettingsModule.healthAsHearts.isToggled()) {
            d2 /= 2.0;
        }
        String s = SliderSetting.formatNumber(round(d2, 1));
        StringBuilder sb = new StringBuilder(16);
        sb.append(prefix);
        sb.append(s);
        if (SettingsModule.heartSymbol.isToggled()) {
            heart(sb);
        }
        return sb.toString();
    }

    public static String health(EntityPlayer p) {
        float f = health((EntityLivingBase) p);
        return health(Math.min(f / p.getMaxHealth(), 1.0f), f);
    }

    public static String healthDifference(float f, boolean heart) {
        String prefix = f >= 8.0f ? DARK_GREEN : (f > 2.0f ? GREEN : (f > -2.0f ? YELLOW : (f > -6.0f ? GOLD : RED)));
        if (SettingsModule.healthAsHearts.isToggled()) {
            f /= 2.0f;
        }
        String s = SliderSetting.formatNumber(round(f, 1));
        StringBuilder sb = new StringBuilder(16);
        sb.append(prefix);
        if (f >= 0.0f) {
            sb.append('+');
        }
        sb.append(s);
        if (heart && SettingsModule.heartSymbol.isToggled()) {
            heart(sb);
        }
        return sb.toString();
    }

    public static int healthColor(double d) {
        return d < 0.3 ? -55770 : (d < 0.5 ? -12250 : (d < 0.7 ? -218 : -14221530));
    }

    public static char distanceColor(double d) {
        if (d <= 10.0) return 'c';
        if (d <= 25.0) return '6';
        if (d <= 50.0) return 'e';
        if (d <= 100.0) return 'a';
        return '2';
    }

    public static double attackDamage(ItemStack stack) {
        double d = 0.0;
        for (Map.Entry<String, AttributeModifier> e : stack.getAttributeModifiers().entries()) {
            if (!e.getKey().equals("generic.attackDamage")) continue;
            d = e.getValue().getAmount();
            break;
        }
        int n = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        int n2 = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
        return d + (double) n * 1.25 + (double) (n2 > 0 ? n2 * 4 - 1 : 0);
    }

    public static double hitsToKill(EntityPlayer p, ItemStack stack) {
        double d = 1.0;
        if (stack != null && (stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe)) {
            d += attackDamage(stack);
        }
        double d2 = 0.0;
        double d3 = 0.0;
        for (int i = 0; i < 4; ++i) {
            ItemStack armor = p.inventory.armorItemInSlot(i);
            if (armor == null || !(armor.getItem() instanceof ItemArmor)) continue;
            d2 += (double) ((ItemArmor) armor.getItem()).damageReduceAmount * 0.04;
            int n = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, armor);
            if (n == 0) continue;
            d3 += Math.floor(0.75 * (double) (6 + n * n) / 3.0);
        }
        d3 = 0.04 * Math.min(Math.ceil(Math.min(d3, 25.0) * 0.75), 20.0);
        double d5 = d2 + d3 * (1.0 - d2);
        double d6 = d * (1.0 - d5);
        return round((double) health((EntityLivingBase) p) / d6, 1);
    }

    public static String hitsToKillString(EntityPlayer p, ItemStack stack) {
        int n = (int) Math.ceil(hitsToKill(p, stack));
        return SECTION + (n <= 1 ? "c" : (n <= 3 ? "6" : (n <= 5 ? "e" : "a"))) + n;
    }

    public static String nametag(EntityLivingBase e, boolean isPlayer) {
        if (!isPlayer) {
            return e.getDisplayName().getFormattedText();
        }
        StringBuilder sb = new StringBuilder();
        EntityPlayer p = (EntityPlayer) e;
        Long l;
        if (openraven.ravenxd.module.ModuleManager.enabled("Sky Wars") != null && openraven.ravenxd.module.minigames.SkyWarsModule.strengthIndicator.isToggled()
                && (l = openraven.ravenxd.module.minigames.SkyWarsModule.strength.get(e.getName())) != null && (l = l - System.currentTimeMillis()) > 0L) {
            double d = round((double) l / 1000.0, 1);
            sb.append(SECTION).append('4').append(d).append('s');
        }
        if (NametagsModule.showDistance.isToggled() && p != mc.thePlayer) {
            double d = mc.thePlayer.getDistanceToEntity(p);
            char c = distanceColor(d);
            sb.append(SECTION).append(c).append((int) d).append('m');
        }
        if (sb.length() > 0) {
            reset(sb);
        }
        if (NametagsModule.onlyRenderName.isToggled()) {
            String prefix;
            ScorePlayerTeam team = (ScorePlayerTeam) e.getTeam();
            if (team != null && (prefix = FontRenderer.getFormatFromString(team.getColorPrefix())).length() >= 2) {
                sb.append(SECTION).append(prefix.charAt(1));
            }
            sb.append(e.getName());
        } else {
            sb.append(e.getDisplayName().getFormattedText());
        }
        if (NametagsModule.sneakSuffix.isToggled() && p.isSneaking()) {
            reset(sb).append(SECTION).append('e').append('S');
        }
        if (NametagsModule.showHealth.isToggled()) {
            reset(sb).append(health(p));
        }
        if (NametagsModule.showHitsToKill.isToggled()) {
            reset(sb).append(hitsToKillString(p, mc.thePlayer.getHeldItem()));
        }
        return sb.toString();
    }
}
