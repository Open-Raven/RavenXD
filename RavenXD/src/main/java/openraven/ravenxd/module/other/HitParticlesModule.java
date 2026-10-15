package xyz.daikirai.module.other;

import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class HitParticlesModule extends Module {
    public static SliderSetting intensity, primary, secondary;
    public static ButtonSetting disableReachHits, playersOnly, weaponOnly;

    public enum Type {
        DISABLED(null, false, 0, 0.0, 0.0),
        CRITICALS(EnumParticleTypes.CRIT, true, 0, 0.0, 0.0),
        CUTE(null, false, 0, 0.0, 0.0),
        ENCHANTMENT_TABLE(EnumParticleTypes.ENCHANTMENT_TABLE, false, 6, 0.5, 0.8),
        REDSTONE(EnumParticleTypes.REDSTONE, false, 6, 0.8, 0.1),
        SHARPNESS(EnumParticleTypes.CRIT_MAGIC, true, 0, 0.0, 0.0),
        SMOKE(EnumParticleTypes.SMOKE_NORMAL, false, 6, 0.4, 0.1),
        SMOKE_LARGE(EnumParticleTypes.SMOKE_LARGE, false, 6, 0.4, 0.1),
        SPARKS(EnumParticleTypes.FIREWORKS_SPARK, false, 6, 0.4, 0.1);

        final EnumParticleTypes particle;
        final boolean emitter;
        final int count;
        final double radius;
        final double speed;

        Type(EnumParticleTypes particle, boolean emitter, int count, double radius, double speed) {
            this.particle = particle;
            this.emitter = emitter;
            this.count = count;
            this.radius = radius;
            this.speed = speed;
        }

        void generate(Entity entity, int n) {
            if (this.emitter) {
                if (this == SHARPNESS && entity instanceof EntityPlayer && heldSharpness()) {
                    --n;
                }
                for (int i = 0; i < n; ++i) {
                    Minecraft.getMinecraft().effectRenderer.emitParticleAtEntity(entity, this.particle);
                }
            } else if (this == CUTE) {
                spawnCute(entity, n);
            } else {
                burst(entity, this.particle, this.count * n, this.radius, this.speed);
            }
        }
    }

    private static final class Particle {
          byte shape;
          double x, y, z;
          double vx, vy, vz;
          float size;
          boolean alive;
          long born;
    }

    private static Type primaryType = Type.SHARPNESS;
    private static Type secondaryType = Type.DISABLED;
    private static final Particle[] POOL = new Particle[100];
    private static int alive;
    private static int next;

    static {
        for (int i = 0; i < POOL.length; ++i) {
            POOL[i] = new Particle();
        }
    }

    public HitParticlesModule(int key) {
        super("Hit Particles", Category.other, key);
    }

    public void link() {
        intensity = (SliderSetting) ModuleManager.setting(this, "Intensity");
        primary = (SliderSetting) ModuleManager.setting(this, "Primary");
        secondary = (SliderSetting) ModuleManager.setting(this, "Secondary");
        disableReachHits = (ButtonSetting) ModuleManager.setting(this, "Disable reach hits");
        playersOnly = (ButtonSetting) ModuleManager.setting(this, "Players only");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
    }

    private static boolean heldSharpness() {
        ItemStack s = Minecraft.getMinecraft().thePlayer.getHeldItem();
        return s != null && EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, s) > 0;
    }

    @Override
    public void guiUpdate() {
        primaryType = Type.values()[(int) primary.getInput() + 1];
        secondaryType = Type.values()[(int) secondary.getInput()];
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (!Chat.inWorld() || !e.entityPlayer.worldObj.isRemote) {
            return;
        }
        onHit(e.target);
    }

    private static void onHit(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        if (((EntityLivingBase) entity).deathTime != 0) {
            return;
        }
        if (playersOnly.enabled && !(entity instanceof EntityPlayer)) {
            return;
        }
        if (weaponOnly.enabled && !PlayerUtils.holdingWeapon()) {
            return;
        }
        if (disableReachHits.enabled && mc.objectMouseOver != null) {
            Vec3 eyes = mc.getRenderViewEntity().getPositionEyes(1.0f);
            double reach = mc.playerController.extendedReach() ? 6.0 : 3.0;
            if (mc.objectMouseOver.hitVec.distanceTo(eyes) > reach) {
                return;
            }
        }
        int n = (int) intensity.getInput();
        primaryType.generate(entity, n);
        if (secondaryType != Type.DISABLED) {
            secondaryType.generate(entity, n);
        }
    }

    private static void burst(Entity entity, EnumParticleTypes type, int n, double radius, double speed) {
        Minecraft mc = Minecraft.getMinecraft();
        double x = entity.posX;
        double y = entity.getEntityBoundingBox().minY + (double) (entity.height / 2.0f) + 1.0 * (double) entity.height / 4.0;
        double z = entity.posZ;
        for (int i = 0; i < n; ++i) {
            double dz;
            double dy;
            double dx = mc.theWorld.rand.nextDouble() * 2.0 - 1.0;
            double len = Math.sqrt(dx * dx + (dy = mc.theWorld.rand.nextDouble() * 2.0 - 1.0) * dy + (dz = mc.theWorld.rand.nextDouble() * 2.0 - 1.0) * dz);
            if (len < 1.0E-4) continue;
            mc.theWorld.spawnParticle(type, x + (dx /= len) * radius, y + (dy /= len) * radius, z + (dz /= len) * radius,
                    dx * speed, dy * speed, dz * speed);
        }
    }

    private static void spawnCute(Entity entity, int n) {
        Minecraft mc = Minecraft.getMinecraft();
        int count = 5 + n - 1;
        double x = entity.posX;
        double y = entity.getEntityBoundingBox().minY;
        double z = entity.posZ;
        double h = (double) entity.height * 1.2;
        Entity view = mc.getRenderViewEntity();
        double dx = view.posX - x;
        double dy = view.posY + (double) view.getEyeHeight() - y;
        double dz;
        double sq = dx * dx + dy * dy + (dz = view.posZ - z) * dz;
        if (sq > 1.0E-6) {
            double inv = 1.0 / Math.sqrt(sq);
            dx *= inv;
            dy *= inv;
            dz *= inv;
        } else {
            dz = 0.0;
            dy = 0.0;
            dx = 0.0;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; ++i) {
            Particle p = take();
            p.x = x + (MathUtils.RANDOM.nextDouble() - 0.5) * 1.5;
            p.y = y + MathUtils.RANDOM.nextDouble() * h;
            p.z = z + (MathUtils.RANDOM.nextDouble() - 0.5) * 1.5;
            double yaw = MathUtils.RANDOM.nextDouble() * Math.PI * 2.0;
            double pitch = MathUtils.RANDOM.nextDouble() * Math.PI - 1.5707963267948966;
            double speed = (0.3 + MathUtils.RANDOM.nextDouble() * 0.7) * 1.0;
            double c = Math.cos(pitch);
            double rx = c * Math.cos(yaw) * speed;
            double ry = Math.sin(pitch) * speed;
            double rz = c * Math.sin(yaw) * speed;
            double rest = 1.0 - 0.6;
            p.vx = rx * rest + dx * speed * 0.6;
            p.vy = ry * rest + dy * speed * 0.6 + 0.1;
            p.vz = rz * rest + dz * speed * 0.6;
            p.size = (float) (0.15 * (0.5 + MathUtils.RANDOM.nextDouble()));
            p.shape = randomShape();
            p.born = now;
            revive(p);
        }
    }

    private static byte randomShape() {
        int n = MathUtils.RANDOM.nextInt(5);
        if (n <= 1) return 0;
        if (n == 2) return 1;
        if (n == 3) return 2;
        return 3;
    }

    private static Particle take() {
        for (int i = 0; i < POOL.length; ++i) {
            int idx = next++;
            if (next >= POOL.length) {
                next = 0;
            }
            Particle p = POOL[idx];
            if (p.alive) continue;
            return p;
        }
        Particle oldest = POOL[0];
        for (int n = 1; n < POOL.length; ++n) {
            Particle p = POOL[n];
            if (p.born >= oldest.born) continue;
            oldest = p;
        }
        return oldest;
    }

    private static void revive(Particle p) {
        if (!p.alive) {
            ++alive;
        }
        p.alive = true;
    }

    private static void kill(Particle p) {
        if (p.alive) {
            p.alive = false;
            if (alive > 0) {
                --alive;
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (alive <= 0 || !Chat.inWorld()) {
            return;
        }
        long now = System.currentTimeMillis();
        RenderManager rm = mc.getRenderManager();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        GL11.glEnable(2848);
        GL11.glHint(3154, 4353);
        GL11.glLineWidth(2.0f);
        for (int i = 0; i < POOL.length; ++i) {
            float b;
            float g;
            float r;
            float alpha;
            double d;
            Particle p = POOL[i];
            if (!p.alive) continue;
            long age = now - p.born;
            if (age > 700L) {
                kill(p);
                continue;
            }
            double life = (double) age / 700.0;
            double t = (double) age * 0.001;
            double px = p.x + p.vx * t;
            double py = p.y + p.vy * t - 0.8 * t * t;
            double pz = p.z + p.vz * t;
            px += Math.sin((double) age * 0.002 + (double) i * 1.7) * 0.05;
            pz += Math.cos((double) age * 0.0015 + (double) i * 2.3) * 0.05;
            if (life < 0.1) {
                d = life * 10.0;
                d = d * d * (3.0 - 2.0 * d);
                alpha = (float) (0.1 + 0.9 * d);
            } else if (life > 0.7) {
                d = (life - 0.7) / 0.3;
                d = d * d * (3.0 - 2.0 * d);
                alpha = (float) (1.0 - 0.9 * d);
            } else {
                alpha = 1.0f;
            }
            double rx = px - rm.viewerPosX;
            double ry = py - rm.viewerPosY;
            double rz = pz - rm.viewerPosZ;
            switch (i & 3) {
                case 0: r = 1.0f; g = 0.45f; b = 0.7f; break;
                case 1: r = 1.0f; g = 0.6f; b = 0.8f; break;
                case 2: r = 0.92f; g = 0.5f; b = 0.9f; break;
                default: r = 1.0f; g = 0.75f; b = 0.88f;
            }
            float size = p.size * alpha;
            GlStateManager.pushMatrix();
            GlStateManager.translate(rx, ry, rz);
            GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
            GlStateManager.rotate((float) (t * 40.0 + (double) i * 60.0), 0.0f, 0.0f, 1.0f);
            switch (p.shape) {
                case 0: heart(size, alpha, r, g, b); break;
                case 1: star(size, alpha, r, g, b); break;
                case 2: circle(size, alpha, r, g, b); break;
                case 3: diamond(size, alpha, r, g, b); break;
            }
            GlStateManager.popMatrix();
        }
        GL11.glDisable(2848);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void heart(double d, float a, float r, float g, float b) {
        double s = d / 16.0;
        heartLine(s * 1.2, a * 0.125f, r, g, b);
        heartLine(s * 1.1, a * 0.25f, r, g, b);
        heartLine(s, a * 0.9f, r, g, b);
    }

    private static void heartLine(double d, float a, float r, float g, float b) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(3);
        for (int i = 0; i <= 20; ++i) {
            double t = (double) i * 0.3141592653589793;
            double sin = Math.sin(t);
            double x = 16.0 * sin * sin * sin * d;
            double y = (13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t)) * d;
            GL11.glVertex3d(x, y, 0.0);
        }
        GL11.glEnd();
    }

    private static void star(double d, float a, float r, float g, float b) {
        double s = d / 16.0;
        starLine(s * 1.2, a * 0.125f, r, g, b);
        starLine(s * 1.1, a * 0.25f, r, g, b);
        starLine(s, a * 0.9f, r, g, b);
    }

    private static void starLine(double d, float a, float r, float g, float b) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(3);
        for (int i = 0; i <= 8; ++i) {
            double t = (double) i * 0.7853981633974483 - 1.5707963267948966;
            double len = (i & 1) == 0 ? 12.0 : 5.0;
            GL11.glVertex3d(Math.cos(t) * len * d, Math.sin(t) * len * d, 0.0);
        }
        GL11.glEnd();
    }

    private static void circle(double d, float a, float r, float g, float b) {
        double s = d / 16.0;
        circleFill(s * 1.3, a * 0.125f, r, g, b);
        circleFill(s * 1.15, a * 0.25f, r, g, b);
        circleFill(s, a * 0.9f, r, g, b);
    }

    private static void circleFill(double d, float a, float r, float g, float b) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(6);
        GL11.glVertex3d(0.0, 0.0, 0.0);
        for (int i = 0; i <= 8; ++i) {
            double t = (double) i * 0.7853981633974483;
            GL11.glVertex3d(Math.cos(t) * 8.0 * d, Math.sin(t) * 8.0 * d, 0.0);
        }
        GL11.glEnd();
    }

    private static void diamond(double d, float a, float r, float g, float b) {
        double s = d / 16.0;
        diamondLine(s * 1.2, a * 0.125f, r, g, b);
        diamondLine(s * 1.1, a * 0.25f, r, g, b);
        diamondLine(s, a * 0.9f, r, g, b);
    }

    private static void diamondLine(double d, float a, float r, float g, float b) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(3);
        GL11.glVertex3d(0.0, 14.0 * d, 0.0);
        GL11.glVertex3d(8.0 * d, 0.0, 0.0);
        GL11.glVertex3d(0.0, -14.0 * d, 0.0);
        GL11.glVertex3d(-8.0 * d, 0.0, 0.0);
        GL11.glVertex3d(0.0, 14.0 * d, 0.0);
        GL11.glEnd();
    }

    @Override
    public void onDisable() {
        if (alive > 0) {
            for (Particle p : POOL) {
                p.alive = false;
            }
            alive = 0;
            next = 0;
        }
    }
}
