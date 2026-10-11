package xyz.daikirai.client;

import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public final class BotCheck {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random RANDOM = new Random();

    public static HashMap<EntityPlayer, Long> recentlySpawned = new HashMap<EntityPlayer, Long>();
    private static long lastWatchdogMessage;
    private static int watchdogId;

    private static final String[] WATCHDOG = new String[]{
            "&cWatchdog just arrived. Staff is watching you RIGHT NOW!",
            "&cYo, Watchdog bot just rolled up. You're totally screwed, bro.",
            "&cWatchdog boutta spin da block on you. It's wraps fam.",
            "&cOh no, Watchdog just showed up. Time to say goodbye :/",
            "&cWatchdog's here and it's not looking good. RIP :(",
            "&cOh snap, Watchdog's here. Looks like the ban screen's coming up >_<",
            "&cYikes, Watchdog spotted you. You're in big trouble now!",
            "&cWatchdog's watching you closely. Better start praying!",
            "&cOh boy, Watchdog just entered the lobby. Things ain't looking good.",
            "&cWatchdog just said 'nice hacks' in chat. Pack it up, chief.",
            "&cBro, Watchdog just pulled up with receipts. It's GG for you."};

    private BotCheck() {
    }

    public static boolean aboveSpawn(Entity entity) {
        Module antiBot = ModuleManager.get("AntiBot");
        if (antiBot == null || !antiBot.isEnabled()) {
            return false;
        }
        SliderSetting pitSpawn = (SliderSetting) ModuleManager.setting(antiBot, "Pit spawn");
        return pitSpawn.getInput() != pitSpawn.defaultValue && entity.posY >= pitSpawn.getInput();
    }

    public static boolean isBot(Entity entity) {
        return isBot(entity, true);
    }

    public static boolean isBot(Entity entity, boolean useSpawnDelay) {
        if (!(entity instanceof EntityPlayer) || entity.isDead) {
            return true;
        }
        if (Hypixel.isFakeEntity(entity)) {
            return true;
        }
        Module antiBot = ModuleManager.get("AntiBot");
        if (antiBot == null || !antiBot.isEnabled()) {
            return false;
        }
        SliderSetting spawnDelay = (SliderSetting) ModuleManager.setting(antiBot, "Delay (spawn)");
        ButtonSetting disableInDuels = (ButtonSetting) ModuleManager.setting(antiBot, "^ Disable in duels");
        ButtonSetting tabList = (ButtonSetting) ModuleManager.setting(antiBot, "Tab list");
        ButtonSetting filterWatchdog = (ButtonSetting) ModuleManager.setting(antiBot, "Filter watchdog");
        EntityPlayer p = (EntityPlayer) entity;
        if (useSpawnDelay && spawnDelay.getInput() != spawnDelay.defaultValue && !recentlySpawned.isEmpty() && recentlySpawned.containsKey(p)
                && (!disableInDuels.isToggled() || !Hypixel.duels(2))) {
            return true;
        }
        if (tabList.isToggled() && Hypixel.info(p) == null) {
            return true;
        }
        if (!Hypixel.onHypixel()) {
            return false;
        }
        String name = p.getName();
        if (name.isEmpty()) {
            return true;
        }
        if (p.getHealth() != 20.0f && name.length() >= 3 && name.charAt(0) == Chat.SECTION && name.charAt(1) == 'c') {
            return true;
        }
        String display = p.getDisplayName().getUnformattedText();
        if (p.maxHurtTime == 0) {
            int n = display.length();
            if (p.getHealth() == 20.0f) {
                if (display.charAt(0) != Chat.SECTION) {
                    if (n == 10) {
                        return true;
                    }
                    if (n >= 4 && display.charAt(0) == 'c' && display.charAt(1) == 'p' && display.charAt(2) == '_') {
                        return true;
                    }
                    if (display.equals("Opalsmith")) {
                        return true;
                    }
                }
                if (n == 12 && p.isPlayerSleeping() && display.charAt(0) == Chat.SECTION) {
                    return true;
                }
                if (n >= 7 && display.charAt(2) == '[' && display.charAt(3) == 'N' && display.charAt(6) == ']') {
                    return true;
                }
            } else if (n >= 3) {
                if (display.charAt(0) == Chat.SECTION) {
                    if (p.isInvisible() && display.charAt(1) == 'c') {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        NetworkPlayerInfo info;
        ScorePlayerTeam team;
        if (filterWatchdog.isToggled() && display.length() >= 3 && display.charAt(0) == Chat.SECTION && display.charAt(1) == 'c'
                && (info = Hypixel.info(p)) != null && (team = info.getPlayerTeam()) != null && team.getTeamName().isEmpty()) {
            long now = System.currentTimeMillis();
            if (p.getHeldItem() != null && Math.abs(now - lastWatchdogMessage) >= 1000L) {
                lastWatchdogMessage = now;
                if (watchdogId == 0) {
                    Sounds.queue("note.pling", 1.0f, 1.0f);
                    Chat.print(WATCHDOG[RANDOM.nextInt(WATCHDOG.length)]);
                }
                watchdogId = p.getEntityId();
            }
            return true;
        }
        return false;
    }

    public static EntityLivingBase lookingAt(double range) {
        MovingObjectPosition mop = rayTrace(null, range, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit instanceof EntityLivingBase) {
            return (EntityLivingBase) mop.entityHit;
        }
        return null;
    }

    public static void resetWatchdog() {
        watchdogId = 0;
    }

    public static void renderWatchdog(float pt) {
        if (watchdogId == 0) {
            return;
        }
        Entity e = mc.theWorld.getEntityByID(watchdogId);
        if (e == null) {
            watchdogId = 0;
            return;
        }
        xyz.daikirai.util.Render3D.entityBox(e, 2, java.awt.Color.red.getRGB(), pt);
    }

    public static MovingObjectPosition rayTraceBlocks(double d) {
        Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 dir = look(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        Vec3 end = eyes.addVector(dir.xCoord * d, dir.yCoord * d, dir.zCoord * d);
        return mc.theWorld.rayTraceBlocks(eyes, end, false, false, false);
    }

    public static Vec3 look(float yaw, float pitch) {
        float f3 = MathHelper.cos((yaw *= -((float) Math.PI / 180)) - (float) Math.PI);
        float f4 = MathHelper.sin(yaw - (float) Math.PI);
        float f5 = -MathHelper.cos(pitch *= -((float) Math.PI / 180));
        float f6 = MathHelper.sin(pitch);
        return new Vec3(f4 * f5, f6, f3 * f5);
    }

    public static MovingObjectPosition rayTrace(Entity only, double d, double x, double y, double z, float yaw, float pitch) {
        Entity hit = null;
        Vec3 eyes = new Vec3(x, y + (double) mc.thePlayer.getEyeHeight(), z);
        Vec3 dir = look(yaw, pitch);
        Vec3 end = eyes.addVector(dir.xCoord * d, dir.yCoord * d, dir.zCoord * d);
        Vec3 hitVec = null;
        Entity view = mc.getRenderViewEntity();
        List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(view,
                view.getEntityBoundingBox().addCoord(dir.xCoord * d, dir.yCoord * d, dir.zCoord * d).expand(1.0, 1.0, 1.0));
        double best = d;
        for (Entity e : list) {
            if (!e.canBeCollidedWith()) continue;
            if (only != null && e != only) continue;
            float border = e.getCollisionBorderSize();
            AxisAlignedBB bb = e.getEntityBoundingBox().expand(border, border, border);
            MovingObjectPosition mop = bb.calculateIntercept(eyes, end);
            if (bb.isVecInside(eyes)) {
                if (!(best >= 0.0)) continue;
                hit = e;
                hitVec = mop == null ? eyes : mop.hitVec;
                best = 0.0;
                continue;
            }
            double dist;
            if (mop == null || !((dist = eyes.distanceTo(mop.hitVec)) < best) && best != 0.0) continue;
            if (e == view.ridingEntity && !e.canRiderInteract()) {
                if (best != 0.0) continue;
                hit = e;
                hitVec = mop.hitVec;
                continue;
            }
            hit = e;
            hitVec = mop.hitVec;
            best = dist;
        }
        return hit == null ? null : new MovingObjectPosition(hit, hitVec);
    }
}
