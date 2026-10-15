package xyz.daikirai.util;

import xyz.daikirai.script.ScriptEvents;
import xyz.daikirai.script.ScriptManager;
import xyz.daikirai.hook.Hooks;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public final class RotationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static RotationSource source;
    public static boolean rotating;
    public static boolean wasRotating;
    public static float serverYaw;
    public static float serverPitch;
    public static float lastYaw;
    public static float lastPitch;
    public static float previousPitch;
    public static float playerYawAtSend;
    public static Float smoothYaw;

    private RotationManager() {
    }

    private static final class Provider {
        final String module;
        final boolean needsEnabled;
        final RotationSource source;

        Provider(String module, boolean needsEnabled, RotationSource source) {
            this.module = module;
            this.needsEnabled = needsEnabled;
            this.source = source;
        }
    }

    private static final Provider[] PROVIDERS = {
            new Provider("Freecam", false, RotationSource.FREECAM),
            new Provider("AutoBlockin", true, RotationSource.AUTOBLOCKIN),
            new Provider("Scaffold", false, RotationSource.SCAFFOLD),
            new Provider("Long Jump", false, RotationSource.LONGJUMP),
            new Provider("AutoLadder", false, RotationSource.AUTOLADDER),
            new Provider("Water Bucket", true, RotationSource.WATERBUCKET),
            new Provider("Displace", false, RotationSource.DISPLACE),
            new Provider("AimAssist", true, RotationSource.AIMASSIST),
            new Provider("NoSlow", false, RotationSource.NOSLOW),
            new Provider("BedAura", true, RotationSource.BEDAURA),
            new Provider("KillAura", false, RotationSource.KILLAURA),
            new Provider("AutoRod", true, RotationSource.AUTOROD),
            new Provider("AntiFireball", true, RotationSource.ANTIFIREBALL),
            new Provider("WoolWars", true, RotationSource.WOOLWARS),
            new Provider("InvMove", true, RotationSource.INVMANAGER),
            new Provider("45 Jump", true, RotationSource.JUMP45),
    };

    public static float[] request(float yaw, float pitch) {
        for (Provider p : PROVIDERS) {
            Module m = p.needsEnabled ? ModuleManager.enabled(p.module) : ModuleManager.get(p.module);
            if (m == null) continue;
            float[] rotations = m.getRotations(yaw, pitch);
            if (rotations != null) {
                source = p.source;
                return rotations;
            }
        }
        float[] rotations;
        if (ScriptManager.events == null || (rotations = ScriptEvents.rotations(yaw, pitch)) == null) {
            return null;
        }
        source = RotationSource.SCRIPT;
        return rotations;
    }

    public static void set(float[] rotations) {
        serverYaw = rotations[0];
        serverPitch = rotations[1];
    }

    public static void setLast(float yaw, float pitch, float lastReportedPitch) {
        lastYaw = yaw;
        lastPitch = pitch;
        previousPitch = lastReportedPitch;
        playerYawAtSend = mc.thePlayer.rotationYaw;
    }

    public static boolean movementFix() {
        return Hooks.movementFixSetting() && rotating && source != null && source.movementFix;
    }

    public static float movementYaw() {
        Float f = null;
        if (movementFix()) {
            f = serverYaw;
        } else {
            Module targetStrafe = ModuleManager.enabled("TargetStrafe");
            if (targetStrafe != null) {
                f = targetStrafe.getMovementYaw();
            }
        }
        if (ScriptManager.events != null) {
            Float f2 = ScriptEvents.movementYaw(f == null ? mc.thePlayer.rotationYaw : f);
            if (f2 != null) {
                f = f2;
            }
        }
        return f == null ? mc.thePlayer.rotationYaw : f;
    }

    private static Module scaffold() {
        Module m = ModuleManager.get("Scaffold");
        return m != null && m.isScaffolding() ? m : null;
    }

    public static float headPitch(EntityLivingBase entity, float partialTicks) {
        if (rotating) {
            Module scaffold = scaffold();
            if (scaffold != null) {
                FakeRotation mode = scaffold.fakeRotation();
                if (mode == FakeRotation.None) {
                    return previousPitch + (serverPitch - previousPitch) * partialTicks;
                }
                if (mode == FakeRotation.Forward) {
                    return entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                }
                long l = System.currentTimeMillis() - scaffold.fakeRotationStart();
                long l2 = 140L;
                float f2 = Math.min((float) l / (float) l2, 1.0f);
                float f3 = 84.5f;
                return scaffold.fakeRotationStartPitch() + f2 * (f3 - scaffold.fakeRotationStartPitch());
            }
            return previousPitch + (serverPitch - previousPitch) * partialTicks;
        }
        return entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
    }

    public static void updateBody(float f) {
        Module scaffold = scaffold();
        if (scaffold != null) {
            FakeRotation mode = scaffold.fakeRotation();
            if (mode == FakeRotation.Smooth) {
                if (smoothYaw == null) {
                    smoothYaw = mc.thePlayer.rotationYaw;
                }
                float f3 = PlayerUtils.movementAngle() - 0.01f;
                float f2 = 0.4f;
                float f4 = f3 - smoothYaw;
                if ((f4 = (f4 + 180.0f) % 360.0f - 180.0f) < -180.0f) {
                    f4 += 360.0f;
                } else if (f4 > 180.0f) {
                    f4 = f4 - 360.0f;
                }
                smoothYaw = smoothYaw + f4 * f2;
                f = smoothYaw;
            } else if (mode == FakeRotation.Forward) {
                f = mc.thePlayer.rotationYawHead;
            } else if (mode == FakeRotation.Spin) {
                long l = 750L;
                long l2 = System.currentTimeMillis() - scaffold.fakeRotationStart();
                f = mc.thePlayer.rotationYaw + (float) (l2 % l) / (float) l * 360.0f;
            }
        }
        mc.thePlayer.rotationYawHead = f;
        if (SettingsModule.rotateBody != null && SettingsModule.rotateBody.isToggled()) {
            if (SettingsModule.fullBody.isToggled()) {
                mc.thePlayer.renderYawOffset = f;
            } else {
                float f3 = MathHelper.wrapAngleTo180_float(f - mc.thePlayer.renderYawOffset);
                mc.thePlayer.renderYawOffset += f3 * 0.3f;
                float f2 = MathHelper.wrapAngleTo180_float(f - mc.thePlayer.renderYawOffset);
                f2 = Math.max(-75.0f, Math.min(75.0f, f2));
                mc.thePlayer.renderYawOffset = f - f2;
                if (f2 * f2 > 2500.0f) {
                    mc.thePlayer.renderYawOffset += f2 * 0.2f;
                }
            }
        }
    }
}
