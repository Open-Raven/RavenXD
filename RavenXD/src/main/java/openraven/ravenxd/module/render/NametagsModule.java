package openraven.ravenxd.module.render;

import openraven.ravenxd.client.Friends;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.Render3D;
import openraven.ravenxd.util.RenderUtils;
import openraven.ravenxd.util.Strings;
import openraven.ravenxd.util.Theme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Vec3;

public class NametagsModule extends Module {
    public static NametagsModule instance;
    public static SliderSetting background, scale;
    public static ButtonSetting autoScale, disableInLobby, onlyRenderName, renderSelf, showDistance, showHealth, showHitsToKill, showInvis,
            sneakSuffix, removeTags, showItems, showEnchants, showDurability, showStackSize;

    public NametagsModule(int key) {
        super("Nametags", Category.render, key);
        instance = this;
    }

    public void link() {
        background = (SliderSetting) ModuleManager.setting(this, "Background");
        scale = (SliderSetting) ModuleManager.setting(this, "Scale");
        autoScale = (ButtonSetting) ModuleManager.setting(this, "Auto-scale");
        disableInLobby = (ButtonSetting) ModuleManager.setting(this, "Disable in lobby");
        onlyRenderName = (ButtonSetting) ModuleManager.setting(this, "Only render name");
        renderSelf = (ButtonSetting) ModuleManager.setting(this, "Render self");
        showDistance = (ButtonSetting) ModuleManager.setting(this, "Show distance");
        showHealth = (ButtonSetting) ModuleManager.setting(this, "Show health");
        showHitsToKill = (ButtonSetting) ModuleManager.setting(this, "Show hits to kill");
        showInvis = (ButtonSetting) ModuleManager.setting(this, "Show invis");
        sneakSuffix = (ButtonSetting) ModuleManager.setting(this, "Sneak suffix");
        removeTags = (ButtonSetting) ModuleManager.setting(this, "Remove tags");
        showItems = (ButtonSetting) ModuleManager.setting(this, "Show items");
        showEnchants = (ButtonSetting) ModuleManager.setting(this, "Show enchants");
        showDurability = (ButtonSetting) ModuleManager.setting(this, "Show durability");
        showStackSize = (ButtonSetting) ModuleManager.setting(this, "Show stack size");
    }

    public static void render(EntityLivingBase e, float pt) {
        if (removeTags.isToggled()) {
            return;
        }
        double cx = Render3D.cameraPos[0];
        double cy = Render3D.cameraPos[1];
        double cz = Render3D.cameraPos[2];
        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * (double) pt;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * (double) pt + (double) e.height + 0.03;
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * (double) pt;
        if (!e.isSneaking()) {
            y += 0.3;
        }
        float f2 = Render3D.nametagScale(cx, cy, cz, x, y, z);
        Vec3 vec3 = Render3D.project(x, y, z, RenderUtils.scaleFactor, -1.0f);
        if (vec3 != null && vec3.zCoord < 1.0003684) {
            GlStateManager.scale(f2, f2, f2);
            boolean player = e instanceof EntityPlayer;
            boolean bg = !background.isAtMin();
            String s = Strings.nametag(e, player);
            float fh = mc.fontRendererObj.FONT_HEIGHT;
            float w = mc.fontRendererObj.getStringWidth(s);
            float f6 = (float) (vec3.xCoord - (double) (w * f2 / 2.0f)) / f2;
            float f7 = (float) (vec3.yCoord - (double) (fh * f2)) / f2;
            if (bg) {
                float f8 = (float) background.getInput() / 100.0f;
                float f9 = 0.0f;
                int a = 14 + (int) (f8 * 241.0f);
                RenderUtils.beginRect();
                RenderUtils.rawRect(f6 - (1.0f + f9), f7 + fh - 1.0f + f9, f6 + w + f9, f7 - (1.0f + f9), a << 24);
                RenderUtils.endRect();
            }
            if (player) {
                EntityPlayer p = (EntityPlayer) e;
                int n = 0;
                if (Friends.isFriend(p.getName())) {
                    n = Theme.VINE.first.getRGB();
                } else if (Friends.isEnemy(p.getName())) {
                    n = Theme.FLARE.first.getRGB();
                }
                if (n != 0) {
                    RenderUtils.beginRect();
                    RenderUtils.rawRect(f6 - 1.5f, f7 - 1.5f, f6 + w + 0.5f, f7 - 1.0f, n);
                    RenderUtils.rawRect(f6 - 1.5f, f7 + fh - 1.0f, f6 + w + 0.5f, f7 + fh - 0.5f, n);
                    RenderUtils.rawRect(f6 - 1.5f, f7 - 1.0f, f6 - 1.0f, f7 + fh - 1.0f, n);
                    RenderUtils.rawRect(f6 + w + 0.5f, f7 - 1.0f, f6 + w, f7 + fh - 1.0f, n);
                    RenderUtils.endRect();
                }
            }
            mc.fontRendererObj.drawString(s, f6, f7, -1, !bg);
            if (player && showItems.isToggled()) {
                renderItems((EntityPlayer) e, f6 + w / 2.0f, f7);
            }
        }
        Render3D.setupCamera(pt);
    }

    public static void renderItems(EntityPlayer p, float x, float y) {
        y -= 18.0f;
        for (int i = 0; i < p.inventory.armorInventory.length; ++i) {
            if (p.inventory.armorInventory[i] == null) continue;
            x -= 8.0f;
        }
        boolean ench = showEnchants.isToggled();
        boolean dura = showDurability.isToggled();
        boolean stack = showStackSize.isToggled();
        boolean any = ench || dura || stack;
        ItemStack held = p.getHeldItem();
        if (held != null) {
            x -= 8.0f;
            renderItem(held, x, y, any, ench, dura, stack);
            x += 16.0f;
        }
        for (int i = p.inventory.armorInventory.length - 1; i >= 0; --i) {
            ItemStack s = p.inventory.armorInventory[i];
            if (s == null) continue;
            renderItem(s, x, y, any, ench, dura, stack);
            x += 16.0f;
        }
    }

    public static void renderItem(ItemStack s, float x, float y, boolean any, boolean ench, boolean dura, boolean stack) {
        GlStateManager.pushMatrix();
        RenderUtils.drawItem(s, x, y);
        if (any) {
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.enableAlpha();
            GlStateManager.disableDepth();
            renderItemText(s, x * 2.0f, y * 2.0f, ench, dura, stack);
            GlStateManager.enableDepth();
        }
        GlStateManager.popMatrix();
    }

    public static void renderItemText(ItemStack s, float x, float y, boolean ench, boolean dura, boolean stack) {
        x += 2.0f;
        if (stack && s.getMaxStackSize() > 1) {
            mc.fontRendererObj.drawStringWithShadow(s.stackSize + "x", x, y + 23.0f, -1);
        } else if (dura && s.getMaxDamage() > 0) {
            int n = s.getMaxDamage() - s.getItemDamage();
            mc.fontRendererObj.drawStringWithShadow(String.valueOf(n), x, y + 23.0f, -1);
        }
        if (ench) {
            renderEnchants(s, x, y);
        }
    }

    public static void renderEnchants(ItemStack s, float x, float y) {
        float ty = y - 3.0f;
        float fh = mc.fontRendererObj.FONT_HEIGHT;
        Item item = s.getItem();
        if (item instanceof ItemArmor) {
            int[] l = levels(s, Enchantment.protection, Enchantment.projectileProtection, Enchantment.blastProtection, Enchantment.fireProtection, Enchantment.thorns, Enchantment.unbreaking);
            draw(new String[]{"pr", "pp", "bp", "fp", "th", "ub"}, l, x, ty, fh);
        } else if (item instanceof ItemBow) {
            int[] l = levels(s, Enchantment.power, Enchantment.punch, Enchantment.flame, Enchantment.unbreaking);
            draw(new String[]{"pw", "pn", "fl", "ub"}, l, x, ty, fh);
        } else if (item instanceof ItemSword) {
            int[] l = levels(s, Enchantment.sharpness, Enchantment.knockback, Enchantment.fireAspect, Enchantment.unbreaking);
            draw(new String[]{"sh", "kb", "fr", "ub"}, l, x, ty, fh);
        } else if (item instanceof ItemTool) {
            int[] l = levels(s, Enchantment.efficiency, Enchantment.fortune, Enchantment.silkTouch, Enchantment.unbreaking);
            draw(new String[]{"ef", "fo", "sk", "ub"}, l, x, ty, fh);
        } else if (item == Items.stick || item == Items.fishing_rod) {
            int[] l = levels(s, Enchantment.knockback, Enchantment.unbreaking);
            draw(new String[]{"kb", "ub"}, l, x, ty, fh);
        }
    }

    private static int[] levels(ItemStack s, Enchantment... ids) {
        int[] out = new int[ids.length];
        NBTTagList list = s.getEnchantmentTagList();
        if (list != null) {
            int n = list.tagCount();
            for (int i = 0; i < n; ++i) {
                NBTTagCompound c = list.getCompoundTagAt(i);
                short id = c.getShort("id");
                short lvl = c.getShort("lvl");
                for (int j = 0; j < ids.length; ++j) {
                    if (id == ids[j].effectId) {
                        out[j] = lvl;
                        break;
                    }
                }
            }
        }
        return out;
    }

    private static void draw(String[] tags, int[] levels, float x, float y, float fh) {
        for (int i = 0; i < tags.length; ++i) {
            if (levels[i] > 0) {
                mc.fontRendererObj.drawStringWithShadow(tags[i] + levels[i], x, y, -1);
                y -= fh;
            }
        }
    }
}
