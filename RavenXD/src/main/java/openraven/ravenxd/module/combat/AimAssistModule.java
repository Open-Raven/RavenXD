package xyz.daikirai.module.combat;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.util.WorldUtils;
import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Friends;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.module.world.AntiBotModule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

public class AimAssistModule extends Module {
    public static AimAssistModule instance;
    public static SliderSetting speed, fov, range;
    public static ButtonSetting rotateYaw, rotatePitch, aimInvis, aimMobs, aimThroughBlocks, clickAim, ignoreTeammates, silentAim,
            stopOnTarget, stopWhenBreaking, weaponOnly;
    public static EntityLivingBase target;
    private static float[] silentRotation;
    private static double rangeSq;
    public static boolean snap;

    public AimAssistModule(int key) {
        super("AimAssist", Category.combat, key);
        instance = this;
    }

    public void link() {
        speed = (SliderSetting) ModuleManager.setting(this, "Speed");
        fov = (SliderSetting) ModuleManager.setting(this, "FOV");
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        rotateYaw = (ButtonSetting) ModuleManager.setting(this, "Rotate yaw");
        rotatePitch = (ButtonSetting) ModuleManager.setting(this, "Rotate pitch");
        aimInvis = (ButtonSetting) ModuleManager.setting(this, "Aim invis");
        aimMobs = (ButtonSetting) ModuleManager.setting(this, "Aim mobs");
        aimThroughBlocks = (ButtonSetting) ModuleManager.setting(this, "Aim through blocks");
        clickAim = (ButtonSetting) ModuleManager.setting(this, "Click aim");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        silentAim = (ButtonSetting) ModuleManager.setting(this, "Silent aim");
        stopOnTarget = (ButtonSetting) ModuleManager.setting(this, "Stop on target");
        stopWhenBreaking = (ButtonSetting) ModuleManager.setting(this, "Stop when breaking");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
    }

    @Override
    public boolean overrideMouseOver() {
        if (!silentAim.enabled) {
            return false;
        }
        silentRotation = aim(RotationManager.lastYaw, RotationManager.lastPitch);
        if (silentRotation == null) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.objectMouseOver = BotCheck.rayTrace(target, PlayerUtils.attackReach(), mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                silentRotation[0], silentRotation[1]);
        return true;
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (silentAim.enabled && silentRotation != null) {
            float[] r = new float[2];
            if (rotateYaw.enabled) {
                r[0] = silentRotation[0];
            }
            if (rotatePitch.enabled) {
                r[1] = silentRotation[1];
            }
            silentRotation = null;
            return r;
        }
        return null;
    }

    private static float[] aim(float lastYaw, float lastPitch) {
        Minecraft mc = Minecraft.getMinecraft();
        float baseYaw;
        float basePitch;
        if (mc.currentScreen != null || !mc.inGameHasFocus) {
            target = null;
            return null;
        }
        if (weaponOnly.enabled && !PlayerUtils.holdingWeapon()) {
            target = null;
            return null;
        }
        if (clickAim.enabled && !PlayerUtils.isClicking()) {
            target = null;
            return null;
        }
        if (stopWhenBreaking.enabled && mc.thePlayer.capabilities.allowEdit && Mouse.isButtonDown(0)) {
            MovingObjectPosition m = silentAim.enabled
                    ? BotCheck.rayTraceBlocks(mc.playerController.extendedReach() ? 6.0 : 3.0) : mc.objectMouseOver;
            if (m != null && m.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                target = null;
                return null;
            }
        }
        if ((target = findTarget()) == null) {
            return null;
        }
        if (stopOnTarget.enabled && mc.objectMouseOver != null && mc.objectMouseOver.entityHit == target) {
            return null;
        }
        if (speed.getInput() == speed.getMax() || snap) {
            snap = false;
            return RotationUtils.toEntityFixed(target, lastYaw, lastPitch);
        }
        if (silentAim.enabled) {
            baseYaw = lastYaw;
            basePitch = lastPitch;
        } else {
            baseYaw = mc.thePlayer.rotationYaw;
            basePitch = mc.thePlayer.rotationPitch;
        }
        float[] to = RotationUtils.toEntityBox(target);
        float dyaw = MathHelper.wrapAngleTo180_float(to[0] - baseYaw);
        float dpitch = to[1] - basePitch;
        double s = speed.getInput() / 100.0;
        double rate = 0.3 + 14.7 * (s * s);
        double ay = Math.abs(dyaw);
        double ap = Math.abs(dpitch);
        double near = Math.min(1.0, ay / Math.max(10.0, fov.getInput()));
        double step = rate * (0.3 + (double) (5 + (int) (3.0 * Math.random())) * near);
        double ky = ay / Math.max(1.0E-6, step);
        double kp = ap / Math.max(1.0E-6, step);
        double k = Math.max(1.0, Math.max(ky, kp));
        float my = (float) Math.copySign(Math.min(ay, ay / k), (double) dyaw);
        float mp = (float) Math.copySign(Math.min(ap, ap / k), (double) dpitch);
        float yaw = baseYaw + my;
        float pitch = RotationUtils.clampPitch(basePitch + mp);
        return RotationUtils.fixSensitivity(yaw, pitch, lastYaw, lastPitch, true);
    }

    @Override
    public void guiUpdate() {
        rangeSq = range.getInput() * range.getInput();
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        float[] r;
        if (!silentAim.enabled && (r = aim(RotationManager.lastYaw, RotationManager.lastPitch)) != null) {
            if (rotateYaw.enabled) {
                mc.thePlayer.rotationYaw = r[0];
            }
            if (rotatePitch.enabled) {
                mc.thePlayer.rotationPitch = r[1];
            }
        }
    }

    @Override
    public Entity getTarget() {
        return target;
    }

    @Override
    public void onDisable() {
        target = null;
        snap = false;
    }

    private static EntityLivingBase findTarget() {
        Minecraft mc = Minecraft.getMinecraft();
        double halfFov = fov.getInput() / 2.0;
        double best = 999.0;
        EntityLivingBase found = null;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase) entity;
            if (e == mc.thePlayer || e.deathTime != 0) continue;
            if (!aimInvis.enabled && PlayerUtils.isInvisibleUnarmoured(e)) continue;
            if (AntiBotModule.ignored(e)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(e) > rangeSq) continue;
            double angle = Math.abs(MathHelper.wrapAngleTo180_double((double) (RotationUtils.yawBetween(mc.thePlayer.posX, mc.thePlayer.posZ, e.posX, e.posZ) - mc.thePlayer.rotationYaw)));
            if (angle > halfFov) continue;
            if (e instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) e;
                if (Friends.isFriend(p.getName())) continue;
                if (BotCheck.isBot(p)) continue;
                if (ignoreTeammates.enabled && Hypixel.sameTeam(p)) continue;
            } else if (!aimMobs.enabled || !PlayerUtils.matches(e, 1) || Hypixel.sameTeam(e)) continue;
            if (!aimThroughBlocks.enabled && !RotationUtils.canSee(e)) continue;
            if (best != 999.0 && !(angle < best)) continue;
            best = angle;
            found = e;
        }
        return found;
    }
}
