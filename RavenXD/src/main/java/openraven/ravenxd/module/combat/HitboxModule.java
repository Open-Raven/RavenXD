package xyz.daikirai.module.combat;

import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.WorldUtils;
import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Friends;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class HitboxModule extends Module {
    public static SliderSetting multiplier;
    public static ButtonSetting playersOnly, showModifiedHitbox, weaponOnly;
    public static EntityLivingBase target;
    public static boolean pending;

    public HitboxModule(int key) {
        super("Hitbox", Category.combat, key);
    }

    public void link() {
        multiplier = (SliderSetting) ModuleManager.setting(this, "Multiplier");
        playersOnly = (ButtonSetting) ModuleManager.setting(this, "Players only");
        showModifiedHitbox = (ButtonSetting) ModuleManager.setting(this, "Show modified hitbox");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onMouse(MouseEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(e.buttonstate && e.button == 0 && Chat.inWorld() && !multiplier.isAtMin() && mc.currentScreen == null) || PlayerUtils.isBlocking()) {
            return;
        }
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return;
        }
        if (weaponOnly.enabled && !PlayerUtils.holdingWeapon()) {
            return;
        }
        EntityLivingBase e2 = this.pick();
        if (e2 == null) {
            return;
        }
        if (e2 instanceof EntityPlayer) {
            if (Friends.isFriend(e2.getName())) {
                return;
            }
        } else if (playersOnly.enabled) {
            return;
        }
        e.setCanceled(true);
        target = e2;
        pending = true;
    }

    @Override
    public void onPreUpdateAlways() {
        if (pending) {
            attackPending();
        }
    }

    private static void attackPending() {
        Minecraft mc = Minecraft.getMinecraft();
        pending = false;
        if (!PacketUtils.noAttackPlaceOrDig() || target == null || mc.theWorld.getEntityByID(target.getEntityId()) == null) {
            return;
        }
        if (mc.currentScreen != null || PlayerUtils.isBlocking()) {
            return;
        }
        PlayerUtils.attack(target, false);
    }

    @Override
    public void onDisable() {
        pending = false;
        target = null;
    }

    private void drawBox(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        Timer timer;
        try {
            timer = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "timer", "field_71428_T");
        } catch (Exception ex) {
            return;
        }
        if (timer == null) {
            return;
        }
        float pt = timer.renderPartialTicks;
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) pt - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) pt - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) pt - rm.viewerPosZ;
        float f = (float) ((double) entity.getCollisionBorderSize() * multiplier.getInput());
        AxisAlignedBB bb = entity.getEntityBoundingBox().expand(f, f, f);
        AxisAlignedBB box = new AxisAlignedBB(bb.minX - entity.posX + x, bb.minY - entity.posY + y, bb.minZ - entity.posZ + z,
                bb.maxX - entity.posX + x, bb.maxY - entity.posY + y, bb.maxZ - entity.posZ + z);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(2848);
        GL11.glLineWidth(2.0f);
        RenderGlobal.drawSelectionBoundingBox(box);
        GL11.glDisable(2848);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!showModifiedHitbox.enabled || !Chat.inWorld()) {
            return;
        }
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) continue;
            if (entity instanceof EntityArmorStand || entity.isInvisible() || ((EntityLivingBase) entity).deathTime != 0) continue;
            this.drawBox(entity);
        }
    }

    private EntityLivingBase pick() {
        Minecraft mc = Minecraft.getMinecraft();
        double d = PlayerUtils.attackReach();
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 look = BotCheck.look(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        Vec3 end = eyes.addVector(look.xCoord * d, look.yCoord * d, look.zCoord * d);
        List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), mc.getRenderViewEntity().getEntityBoundingBox()
                .addCoord(look.xCoord * d, look.yCoord * d, look.zCoord * d).expand(1.0, 1.0, 1.0));
        for (Entity entity : list) {
            if (!(entity instanceof EntityLivingBase)) continue;
            if (!entity.canBeCollidedWith()) continue;
            if (((EntityLivingBase) entity).deathTime != 0) continue;
            float f = (float) ((double) entity.getCollisionBorderSize() * multiplier.getInput());
            AxisAlignedBB box = entity.getEntityBoundingBox().expand(f, f, f);
            MovingObjectPosition mop = box.calculateIntercept(eyes, end);
            if (mop == null || !WorldUtils.clearLine(eyes, mop.hitVec)) continue;
            return (EntityLivingBase) entity;
        }
        return null;
    }
}
