package xyz.daikirai.module.combat;

import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class ReachModule extends Module {
    public static ReachModule instance;
    public static SliderSetting min, max;
    public static ButtonSetting weaponOnly, movingOnly, sprintOnly, hitThroughBlocks;

    public ReachModule(int key) {
        super("Reach", Category.combat, key);
        instance = this;
    }

    public void link() {
        min = (SliderSetting) ModuleManager.setting(this, "Min");
        max = (SliderSetting) ModuleManager.setting(this, "Max");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
        movingOnly = (ButtonSetting) ModuleManager.setting(this, "Moving only");
        sprintOnly = (ButtonSetting) ModuleManager.setting(this, "Sprint only");
        hitThroughBlocks = (ButtonSetting) ModuleManager.setting(this, "Hit through blocks");
    }

    @Override
    public void guiUpdate() {
        SliderSetting.order(min, max);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate || e.button < 0 || !Chat.inWorld() || AutoClickerModule.clickingLeft()) {
            return;
        }
        pick(true);
    }

    @Override
    public boolean overrideMouseOver() {
        return pick(false);
    }

    public static boolean pick(boolean fromClick) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!fromClick && !(instance != null && instance.isEnabled())) {
            return false;
        }
        if (weaponOnly.enabled && !PlayerUtils.holdingWeapon()) {
            return false;
        }
        if (movingOnly.enabled && !PlayerUtils.isMoving()) {
            return false;
        }
        if (sprintOnly.enabled && !mc.thePlayer.isSprinting()) {
            return false;
        }
        if (!hitThroughBlocks.enabled && mc.objectMouseOver != null) {
            BlockPos p = mc.objectMouseOver.getBlockPos();
            if (p != null && mc.theWorld.getBlockState(p).getBlock() != Blocks.air) {
                return false;
            }
        }
        Object[] hit = rayTrace(MathUtils.randomBetween(min, max));
        if (hit == null) {
            return false;
        }
        Entity entity = (Entity) hit[0];
        mc.objectMouseOver = new MovingObjectPosition(entity, (Vec3) hit[1]);
        mc.pointedEntity = entity;
        return true;
    }

    public static Object[] rayTrace(double d) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        Entity found = null;
        if (viewer == null) {
            return null;
        }
        mc.mcProfiler.startSection("pick");
        Vec3 eyes = viewer.getPositionEyes(1.0f);
        Vec3 look = BotCheck.look(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        Vec3 end = eyes.addVector(look.xCoord * d, look.yCoord * d, look.zCoord * d);
        Vec3 hitVec = null;
        List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(viewer, viewer.getEntityBoundingBox()
                .addCoord(look.xCoord * d, look.yCoord * d, look.zCoord * d).expand(1.0, 1.0, 1.0));
        double best = d;
        for (Entity e : list) {
            double dist;
            if (!e.canBeCollidedWith()) continue;
            float f = e.getCollisionBorderSize();
            AxisAlignedBB box = e.getEntityBoundingBox().expand(f, f, f);
            MovingObjectPosition mop = box.calculateIntercept(eyes, end);
            if (box.isVecInside(eyes)) {
                if (!(0.0 < best) && best != 0.0) continue;
                found = e;
                hitVec = mop == null ? eyes : mop.hitVec;
                best = 0.0;
                continue;
            }
            if (mop == null || !((dist = eyes.distanceTo(mop.hitVec)) < best) && best != 0.0) continue;
            if (e == viewer.ridingEntity) {
                if (best != 0.0) continue;
                found = e;
                hitVec = mop.hitVec;
                continue;
            }
            found = e;
            hitVec = mop.hitVec;
            best = dist;
        }
        if (best < d && !(found instanceof EntityLivingBase) && !(found instanceof EntityItemFrame)) {
            found = null;
        }
        mc.mcProfiler.endSection();
        if (found == null || hitVec == null) {
            return null;
        }
        return new Object[]{found, hitVec};
    }
}
