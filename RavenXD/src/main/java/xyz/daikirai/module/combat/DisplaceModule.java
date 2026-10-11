package xyz.daikirai.module.combat;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.KeyUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class DisplaceModule extends Module {
    public static DisplaceModule instance;
    public static SliderSetting modeSetting, angle, cooldown;
    public static ButtonSetting hideRotations, onlyKnockbackItems, overrideAttacks, playersOnly, renderArrow, manual, autoClicker, killAura;

    public enum Mode {
        Left, Right, Random, Void
    }

    public static Mode mode = Mode.Left;
    public static boolean rotate;
    private static boolean fromAimAssist;
    public static boolean queued;
    public static long lastAt;
    public static boolean holding;
    private static int holdTicks;
    private static int blinkTicks;
    public static int lastSide = -1;
    private static int side = -1;
    private static EntityLivingBase lastTarget;
    private static boolean arrow;
    private static long arrowAt;
    private static double arrowX, arrowY, arrowZ, dirX, dirZ;
    private static final Random RANDOM = new Random();

    public DisplaceModule(int key) {
        super("Displace", Category.combat, key);
        instance = this;
        this.lite = true;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        angle = (SliderSetting) ModuleManager.setting(this, "Angle");
        cooldown = (SliderSetting) ModuleManager.setting(this, "Cooldown");
        hideRotations = (ButtonSetting) ModuleManager.setting(this, "Hide rotations");
        onlyKnockbackItems = (ButtonSetting) ModuleManager.setting(this, "Only knockback items");
        overrideAttacks = (ButtonSetting) ModuleManager.setting(this, "Override attacks");
        playersOnly = (ButtonSetting) ModuleManager.setting(this, "Players only");
        renderArrow = (ButtonSetting) ModuleManager.setting(this, "Render arrow");
        manual = (ButtonSetting) ModuleManager.setting(this, "Manual");
        autoClicker = (ButtonSetting) ModuleManager.setting(this, "AutoClicker");
        killAura = (ButtonSetting) ModuleManager.setting(this, "KillAura");
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (!rotate) {
            return null;
        }
        rotate = false;
        if (fromAimAssist) {
            fromAimAssist = false;
            AimAssistModule.snap = true;
        }
        startBlink();
        KeyUtils.click(mc().gameSettings.keyBindAttack);
        return new float[]{mc().thePlayer.rotationYaw + offset(), mc().thePlayer.rotationPitch};
    }

    public static void rearm() {
        holdTicks = 3;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        Minecraft mc = mc();
        if (e.button != 0 || !e.buttonstate || !Chat.inWorld()) {
            return;
        }
        if (!autoClicker.isToggled() && AutoClickerModule.clicking()) {
            return;
        }
        if (!manual.isToggled()) {
            return;
        }
        Entity entity = null;
        boolean aim = false;
        if (AimAssistModule.target != null && AimAssistModule.silentAim.isToggled()) {
            entity = AimAssistModule.target;
            aim = true;
        } else if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            entity = mc.objectMouseOver.entityHit;
        }
        if (entity == null) {
            return;
        }
        if (canDisplace(entity, true, true)) {
            e.setCanceled(true);
            trigger(aim ? 1 : 0);
        } else if (overrideAttacks.isToggled() && canDisplace(entity, false, false)) {
            e.setCanceled(true);
        }
    }

    public static void clear() {
        lastAt = 0L;
    }

    public static float offset() {
        float f = (float) angle.getInput();
        if (side != -1) {
            if (side == 0) {
                f = -f;
            }
            side = -1;
        } else if (mode == Mode.Left || mode == Mode.Random && RANDOM.nextBoolean()) {
            f = -f;
        }
        f += MathUtils.randomFloat(-5.0f, 5.0f);
        if (renderArrow.isToggled() && lastTarget != null) {
            setArrow(lastTarget, f);
        }
        return f;
    }

    public static void startBlink() {
        if (hideRotations.isToggled()) {
            blinkTicks = 2;
            PacketQueue.open();
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            this.onDisable();
        }
    }

    @Override
    public void onDisable() {
        if (blinkTicks > 0) {
            blinkTicks = 0;
            PacketQueue.close();
        }
        holding = false;
        arrow = false;
        rotate = false;
        fromAimAssist = false;
        queued = false;
        holdTicks = 0;
        lastSide = -1;
        side = -1;
    }

    public static boolean canDisplace(Entity entity, boolean checkHurtTime, boolean checkCooldown) {
        Minecraft mc = mc();
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        if (checkHurtTime && (double) ((EntityLivingBase) entity).hurtTime > SettingsModule.hittableHurtTime.getInput()) {
            return false;
        }
        if (playersOnly.isToggled() && !(entity instanceof EntityPlayer)) {
            return false;
        }
        int n = EnchantmentHelper.getKnockbackModifier(mc.thePlayer);
        if (!mc.thePlayer.isSprinting() && n == 0) {
            return false;
        }
        if (onlyKnockbackItems.isToggled() && n == 0) {
            return false;
        }
        if (checkCooldown && (double) Math.abs(System.currentTimeMillis() - lastAt) < cooldown.getInput() * 1000.0 - 20.0) {
            return false;
        }
        if (mode == Mode.Void && (side = (lastSide = WorldUtils.dropSide(entity, lastSide))) == -1) {
            return false;
        }
        lastTarget = (EntityLivingBase) entity;
        return true;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (!arrow || !renderArrow.isToggled()) {
            return;
        }
        if (!Chat.inWorld()) {
            return;
        }
        long l = System.currentTimeMillis() - arrowAt;
        if (l > 600L) {
            arrow = false;
            return;
        }
        double d2;
        double d;
        double alpha = 1.0;
        d2 = (double) l / 100.0;
        d2 = d2 < 0.0 ? 0.0 : Math.min(d2, 1.0);
        d = d2 * d2 * (3.0 - 2.0 * d2);
        alpha *= 0.08 + 0.92 * d;
        if (l > 500L) {
            d2 = (double) (l - 500L) / 100.0;
            d2 = d2 < 0.0 ? 0.0 : Math.min(d2, 1.0);
            d = d2 * d2 * (3.0 - 2.0 * d2);
            alpha *= 1.0 - 0.92 * d;
        }
        d2 = (double) l / 450.0;
        d2 = d2 < 0.0 ? 0.0 : Math.min(d2, 1.0);
        d = 1.0 - d2;
        d2 = 1.0 - d * d * d;
        d = Math.sin((double) l * 0.015) * 0.02 * (0.4 + 0.6 * alpha);
        double dist = 0.8 * d2;
        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;
        double x = arrowX - vx + dirX * dist;
        double y = arrowY - vy + d;
        double z = arrowZ - vz + dirZ * dist;
        double yaw = Math.toDegrees(Math.atan2(-dirZ, dirX));
        int fill = (int) (225.0 * alpha) << 24 | 0xFFFFFF;
        int outline = (int) (255.0 * alpha) << 24 | 0xF6F6F6;
        int shadow = (int) (140.0 * alpha) << 24 | 0x5081A;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GL11.glDisable(2929);
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate((float) yaw, 0.0f, 1.0f, 0.0f);
        GL11.glPushMatrix();
        GL11.glTranslated(0.08 * 0.48, -0.08 * 0.24, -0.02);
        color(shadow);
        GL11.glBegin(7);
        GL11.glVertex3d(-0.2784 * 1.01, -0.0816 * 1.01, 0.0);
        GL11.glVertex3d(0.0 * 1.01, -0.0288 * 1.01, 0.0);
        GL11.glVertex3d(0.0 * 1.01, 0.0288 * 1.01, 0.0);
        GL11.glVertex3d(-0.2784 * 1.01, 0.0816 * 1.01, 0.0);
        GL11.glEnd();
        GL11.glBegin(4);
        GL11.glVertex3d(0.0, -0.144 * 1.01, 0.0);
        GL11.glVertex3d(0.288 * 1.01, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144 * 1.01, 0.0);
        GL11.glEnd();
        GL11.glPopMatrix();
        color(fill);
        GL11.glBegin(7);
        GL11.glVertex3d(-0.2784, -0.0816, 0.0);
        GL11.glVertex3d(0.0, -0.0288, 0.0);
        GL11.glVertex3d(0.0, 0.0288, 0.0);
        GL11.glVertex3d(-0.2784, 0.0816, 0.0);
        GL11.glEnd();
        GL11.glBegin(4);
        GL11.glVertex3d(0.0, -0.144, 0.0);
        GL11.glVertex3d(0.288, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144, 0.0);
        GL11.glEnd();
        GL11.glEnable(2848);
        GL11.glLineWidth(2.0f);
        color(outline);
        GL11.glBegin(2);
        GL11.glVertex3d(-0.2784, -0.0816, 0.0);
        GL11.glVertex3d(0.0, -0.0288, 0.0);
        GL11.glVertex3d(0.0, 0.0288, 0.0);
        GL11.glVertex3d(-0.2784, 0.0816, 0.0);
        GL11.glEnd();
        GL11.glBegin(2);
        GL11.glVertex3d(0.0, -0.144, 0.0);
        GL11.glVertex3d(0.288, 0.0, 0.0);
        GL11.glVertex3d(0.0, 0.144, 0.0);
        GL11.glEnd();
        GL11.glDisable(2848);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(2929);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void color(int c) {
        GlStateManager.color((float) (c >> 16 & 0xFF) / 255.0f, (float) (c >> 8 & 0xFF) / 255.0f, (float) (c & 0xFF) / 255.0f, (float) (c >> 24 & 0xFF) / 255.0f);
    }

    @Override
    public String getInfo() {
        return mode.name();
    }

    public static void trigger(int source) {
        if (source == 0) {
            rotate = true;
        } else if (source == 1) {
            fromAimAssist = true;
            rotate = true;
        } else if (source == 2) {
            queued = true;
        }
        lastAt = System.currentTimeMillis();
    }

    @Override
    public void onPreUpdate() {
        if (blinkTicks > 0 && --blinkTicks == 0) {
            PacketQueue.close();
        }
        if (killAura.isToggled() && KillAuraModule.target != null && canDisplace(KillAuraModule.target.entity, true, true)) {
            trigger(2);
        }
        if (holdTicks > 0) {
            holding = --holdTicks > 0;
        }
    }

    @Override
    public void onPostMotion() {
        rotate = false;
        queued = false;
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
    }

    private static void setArrow(EntityLivingBase entity, float f) {
        double d = Math.toRadians(mc().thePlayer.rotationYaw + f);
        dirX = -Math.sin(d);
        dirZ = Math.cos(d);
        arrowX = entity.posX + dirX * 0.8;
        arrowY = entity.posY + 1.2;
        arrowZ = entity.posZ + dirZ * 0.8;
        arrowAt = System.currentTimeMillis();
        arrow = true;
    }
}
