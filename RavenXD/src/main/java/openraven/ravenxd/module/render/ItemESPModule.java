package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemESPModule extends Module {
    public static ButtonSetting disableDiamond, disableEmerald, disableGold, disableIron;

    public ItemESPModule(int key) {
        super("ItemESP", Category.render, key);
    }

    public void link() {
        disableDiamond = (ButtonSetting) ModuleManager.setting(this, "Disable diamond");
        disableEmerald = (ButtonSetting) ModuleManager.setting(this, "Disable emerald");
        disableGold = (ButtonSetting) ModuleManager.setting(this, "Disable gold");
        disableIron = (ButtonSetting) ModuleManager.setting(this, "Disable iron");
    }

    public static boolean shouldRender(Item item) {
        return !disableIron.isToggled() && item == Items.iron_ingot || !disableGold.isToggled() && item == Items.gold_ingot
                || !disableDiamond.isToggled() && item == Items.diamond || !disableEmerald.isToggled() && item == Items.emerald;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        HashMap<Item, ArrayList<EntityItem>> byItem = new HashMap<Item, ArrayList<EntityItem>>();
        HashMap<Double, Integer> counts = new HashMap<Double, Integer>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItem) || entity.ticksExisted < 3) continue;
            EntityItem ei = (EntityItem) entity;
            if (ei.getEntityItem().stackSize == 0) continue;
            Item item = ei.getEntityItem().getItem();
            if (!shouldRender(item)) continue;
            int size = ei.getEntityItem().stackSize;
            double key = hash(item, entity.posX, entity.posY, entity.posZ);
            Integer count = counts.get(key);
            if (count == null) {
                count = size;
                ArrayList<EntityItem> list = byItem.get(item);
                if (list == null) {
                    list = new ArrayList<EntityItem>();
                }
                list.add(ei);
                byItem.put(item, list);
            } else {
                count = count + size;
            }
            counts.put(key, count);
        }
        if (byItem.isEmpty()) {
            return;
        }
        for (Map.Entry<Item, ArrayList<EntityItem>> entry : byItem.entrySet()) {
            int text;
            int box;
            Item item = entry.getKey();
            if (item == Items.iron_ingot) {
                text = -1;
                box = -1;
            } else if (item == Items.gold_ingot) {
                box = -331703;
                text = -152;
            } else if (item == Items.diamond) {
                box = -10362113;
                text = -7667713;
            } else {
                if (item != Items.emerald) continue;
                box = -15216030;
                text = -14614644;
            }
            for (EntityItem ei : (List<EntityItem>) entry.getValue()) {
                float pt = e.partialTicks;
                double key = hash(item, ei.posX, ei.posY, ei.posZ);
                double x = ei.lastTickPosX + (ei.posX - ei.lastTickPosX) * (double) pt;
                double y = ei.lastTickPosY + (ei.posY - ei.lastTickPosY) * (double) pt;
                double z = ei.lastTickPosZ + (ei.posZ - ei.lastTickPosZ) * (double) pt;
                double dx = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * (double) pt - x;
                double dy = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * (double) pt - y;
                double dz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * (double) pt - z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                render(box, text, counts.get(key), x, y, z, dist);
            }
        }
    }

    public static void render(int n, int n2, int count, double d, double d2, double d3, double dist) {
        RenderManager rm = mc.getRenderManager();
        d -= rm.viewerPosX;
        d2 -= rm.viewerPosY;
        d3 -= rm.viewerPosZ;
        GL11.glPushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        float f4 = Math.min(Math.max(0.2f, (float) ((double) 0.01f * dist)), 0.4f);
        AxisAlignedBB bb = new AxisAlignedBB(d - (double) f4, d2, d3 - (double) f4, d + (double) f4, d2 + (double) (f4 * 2.0f), d3 + (double) f4);
        Render3D.filledBox(bb, r, g, b, 0.56f);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate((double) ((float) d), (double) ((float) d2) + 0.3, (double) ((float) d3));
        GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
        float scale = Math.min(Math.max(0.02266667f, (float) ((double) 0.0015f * dist)), 0.07f);
        GlStateManager.scale(-scale, -scale, -scale);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        String s = String.valueOf(count);
        int half = mc.fontRendererObj.getStringWidth(s) / 2;
        float tx = (float) (-half) + scale * 3.5f;
        float ty = -(123.805f * scale - 2.47494f);
        mc.fontRendererObj.drawString(s, tx, ty, n2, true);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        RenderUtils.resetColor();
        GlStateManager.popMatrix();
    }

    public static double hash(Item item, double d, double d2, double d3) {
        double h = Hypixel.positionHash(d, d2, d3);
        if (item == Items.iron_ingot) {
            h += 0.155;
        } else if (item == Items.gold_ingot) {
            h += 0.255;
        } else if (item == Items.diamond) {
            h += 0.355;
        } else if (item == Items.emerald) {
            h += 0.455;
        }
        return h;
    }
}
