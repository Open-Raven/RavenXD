package xyz.daikirai.module.render;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class FallViewModule extends Module {
    public static ButtonSetting disableWhileFlying, onlyWhileSneaking, overrideHealthFormat, showDamage, showDistance, showRemainingHealth;

    private static int halfWidth;
    private static boolean shown;
    private static double fallFrom;
    private static final StringBuilder sb = new StringBuilder();
    private static String text;
    private static long shownAt, hiddenAt;

    public FallViewModule(int key) {
        super("Fall View", Category.render, key);
    }

    public void link() {
        disableWhileFlying = (ButtonSetting) ModuleManager.setting(this, "Disable while flying");
        onlyWhileSneaking = (ButtonSetting) ModuleManager.setting(this, "Only while sneaking");
        overrideHealthFormat = (ButtonSetting) ModuleManager.setting(this, "Override health format");
        showDamage = (ButtonSetting) ModuleManager.setting(this, "Show damage");
        showDistance = (ButtonSetting) ModuleManager.setting(this, "Show distance");
        showRemainingHealth = (ButtonSetting) ModuleManager.setting(this, "Show remaining health");
    }

    @Override
    public void onDisable() {
        setShown(false);
        fallFrom = 0.0;
    }

    public static void render() {
        if (text == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int a;
        if (shown) {
            long l = now - shownAt;
            a = l < (long) 125 ? (int) ((float) l / (float) 125 * (float) 240) : 240;
        } else {
            long l = now - hiddenAt;
            if (l < (long) 125) {
                a = (int) ((float) 240 - (float) l / (float) 125 * (float) 240);
            } else {
                a = 0;
                text = null;
            }
        }
        if (a <= 0) {
            return;
        }
        float x = (float) RenderUtils.scaledWidth / 2.0f - (float) halfWidth;
        float y = (float) RenderUtils.scaledHeight / 2.0f - 15.0f;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.fontRendererObj.drawString(text, x, y, RenderUtils.withAlpha(-1, RenderUtils.clampAlpha(a)), true);
        GlStateManager.disableBlend();
    }

    @Override
    public void onUpdate() {
        if (!this.active()) {
            setShown(false);
            return;
        }
        MovingObjectPosition mop = BotCheck.rayTraceBlocks(45.0);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mop.sideHit != EnumFacing.UP) {
            setShown(false);
            return;
        }
        double dist = mc.thePlayer.posY - (double) mop.getBlockPos().getY() - 1.0;
        if (dist < 4.0) {
            setShown(false);
            return;
        }
        if (fallFrom != 0.0 && mc.thePlayer.fallDistance > 0.0f) {
            dist = fallFrom;
        } else {
            fallFrom = dist = mc.thePlayer.posY - (double) mop.getBlockPos().getY() - 1.0;
        }
        double dmg = Math.floor(dist - 3.0);
        PotionEffect jump = mc.thePlayer.getActivePotionEffect(Potion.jump);
        if (jump != null && (dmg -= (double) (jump.getAmplifier() + 1)) <= 0.0) {
            setShown(false);
            return;
        }
        PotionEffect res = mc.thePlayer.getActivePotionEffect(Potion.resistance);
        if (res != null) {
            int n3 = res.getAmplifier() + 1;
            dmg *= 1.0 - 0.2 * (double) n3;
            if ((dmg = (double) ((int) Math.floor(dmg))) <= 0.0) {
                setShown(false);
                return;
            }
        }
        ItemStack boots = mc.thePlayer.getEquipmentInSlot(1);
        int lvl;
        if (boots != null && (lvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, boots)) > 0) {
            double d = (double) (6 + lvl * lvl) * 2.5 / 3.0;
            int n = (int) Math.floor(d);
            if (n > 20) {
                n = 20;
            }
            int n4 = n / 2;
            if (n4 > 0) {
                dmg *= 1.0 - (double) n4 / 25.0;
            }
            dmg = (int) Math.ceil(dmg) - 1;
            if (dmg <= 0.0) {
                setShown(false);
                return;
            }
        }
        char c = color(dmg);
        sb.setLength(0);
        sb.append(Strings.SECTION);
        sb.append(c);
        if (showDistance.isToggled()) {
            sb.append((int) dist);
        }
        if (showDamage.isToggled()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            boolean override = overrideHealthFormat.isToggled();
            if (override || SettingsModule.healthAsHearts.isToggled()) {
                sb.append(SliderSetting.formatNumber(Strings.round(dmg / 2.0, 1)));
            } else {
                sb.append((int) dmg);
            }
            if (override || SettingsModule.heartSymbol.isToggled()) {
                Strings.heart(sb);
            }
        }
        if (showRemainingHealth.isToggled()) {
            if (sb.length() > 0) {
                sb.append(' ');
                sb.append(Strings.SECTION);
                sb.append(c);
            }
            double left = Math.max(0.0, 20.0 - dmg);
            boolean override = overrideHealthFormat.isToggled();
            if (override || SettingsModule.healthAsHearts.isToggled()) {
                sb.append(SliderSetting.formatNumber(Strings.round(left / 2.0, 1)));
            } else {
                sb.append((int) left);
            }
            if (override || SettingsModule.heartSymbol.isToggled()) {
                Strings.heart(sb);
            }
        }
        text = sb.toString();
        halfWidth = mc.fontRendererObj.getStringWidth(text) / 2 + 1;
        setShown(true);
    }

    private boolean active() {
        if (mc.thePlayer.rotationPitch < 55.0f) {
            return false;
        }
        if (disableWhileFlying.isToggled() && mc.thePlayer.capabilities.allowFlying) {
            return false;
        }
        if (onlyWhileSneaking.isToggled() && !mc.thePlayer.isSneaking()) {
            return false;
        }
        return showDamage.isToggled() || showDistance.isToggled();
    }

    private static void setShown(boolean b) {
        if (b) {
            if (!shown) {
                shown = true;
                shownAt = System.currentTimeMillis();
            }
        } else if (shown) {
            shown = false;
            hiddenAt = System.currentTimeMillis();
        }
    }

    private static char color(double d) {
        float f = Strings.health((net.minecraft.entity.EntityLivingBase) mc.thePlayer);
        if (d >= (double) f) {
            return 'c';
        }
        if (d >= (double) f * 0.6) {
            return '6';
        }
        if (d >= (double) f * 0.3) {
            return 'e';
        }
        return 'a';
    }
}
