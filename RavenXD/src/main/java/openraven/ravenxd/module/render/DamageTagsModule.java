package xyz.daikirai.module.render;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import xyz.daikirai.util.Theme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DamageTagsModule extends Module {
    private static final Random RANDOM = new Random();

    public static SliderSetting length, scale, themeSetting;
    public static ButtonSetting showHeal, showHurt, showInvis, showMobs, traditionalHealthColor;
    public static Theme theme = Theme.DEFAULT;

    private long lifetime = 1000L;
    private final long fadeTime = 200L;
    private final Map<Integer, Float> lastHealth = new HashMap<Integer, Float>();
    private final List<Tag> tags = new ArrayList<Tag>();

    public DamageTagsModule(int key) {
        super("Damage Tags", Category.render, key);
    }

    public void link() {
        length = (SliderSetting) ModuleManager.setting(this, "Length");
        scale = (SliderSetting) ModuleManager.setting(this, "Scale");
        themeSetting = (SliderSetting) ModuleManager.setting(this, "Theme");
        showHeal = (ButtonSetting) ModuleManager.setting(this, "Show heal");
        showHurt = (ButtonSetting) ModuleManager.setting(this, "Show hurt");
        showInvis = (ButtonSetting) ModuleManager.setting(this, "Show invis");
        showMobs = (ButtonSetting) ModuleManager.setting(this, "Show mobs");
        traditionalHealthColor = (ButtonSetting) ModuleManager.setting(this, "Traditional health color");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Tag t : this.tags) {
            if (!t.visible()) continue;
            long age = now - t.createdAt;
            if (age < 0L || age > this.lifetime) continue;
            double p = Math.max(0.0, Math.min(1.0, (double) age / (double) this.lifetime));
            double rise = t.rise * (1.0 - Math.pow(1.0 - p, 2.0));
            RenderManager rm = mc.getRenderManager();
            double dx = t.x - rm.viewerPosX;
            double dy = t.y + rise - rm.viewerPosY;
            double dz = t.z - rm.viewerPosZ;
            int alpha = 255;
            long fadeStart = this.lifetime - this.fadeTime;
            if (age > fadeStart && this.fadeTime > 0L) {
                double d9 = Math.max(0.0, Math.min(1.0, (double) (age - fadeStart) / (double) this.fadeTime));
                alpha = (int) (255.0 * (1.0 - d9));
            }
            if (alpha <= 4) continue;
            float sc = (float) scale.getInput();
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) dx, (float) dy, (float) dz);
            GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
            float f2 = -0.02266667f * sc;
            GlStateManager.scale(f2, f2, f2);
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            int x = mc.fontRendererObj.getStringWidth(t.text) / -2;
            int color = t.color() & 0xFFFFFF | alpha << 24;
            mc.fontRendererObj.drawString(t.text, (float) x, -3.0f, color, true);
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            RenderUtils.resetColor();
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            this.onDisable();
        }
    }

    @Override
    public void onDisable() {
        this.lastHealth.clear();
        this.tags.clear();
    }

    @Override
    public void onUpdate() {
        final long now = System.currentTimeMillis();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase) entity;
            if (e == mc.thePlayer) continue;
            if (!showInvis.isToggled() && e.isInvisible()) continue;
            if (BotCheck.aboveSpawn(e)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(e) > 2500.0) continue;
            if (e instanceof EntityPlayer) {
                if (BotCheck.isBot(e)) continue;
            } else if (!(showMobs.isToggled() && isMob(e) && !Hypixel.sameTeam(e))) {
                continue;
            }
            float hp = e.getHealth() + e.getAbsorptionAmount();
            float last = this.lastHealth.getOrDefault(e.getEntityId(), hp);
            if (hp != last) {
                float delta = hp - last;
                this.tags.add(new Tag(delta, e.posX, e.posY + (double) e.getEyeHeight(), e.posZ, delta < 0.0f && e.deathTime != 0, now));
            }
            if (e.deathTime <= 0) {
                this.lastHealth.put(e.getEntityId(), hp);
            } else {
                this.lastHealth.remove(e.getEntityId());
            }
        }
        if (!this.tags.isEmpty()) {
            this.tags.removeIf(t -> now - t.createdAt > this.lifetime);
        }
    }

    public static boolean isMob(Entity e) {
        return e instanceof EntityMob || e instanceof EntityGolem || e instanceof EntitySlime;
    }

    @Override
    public void guiUpdate() {
        this.lifetime = (long) (length.getInput() * 1000.0);
        theme = Theme.values()[(int) themeSetting.getInput()];
    }

    private static double range(double d, double d2) {
        return d + (d2 - d) * Math.random();
    }

    private static int sign() {
        return RANDOM.nextBoolean() ? 1 : -1;
    }

    private static final class Tag {
        final long createdAt;
        final boolean fatal;
        final String text;
        final double rise;
        final double delta;
        final double x, y, z;

        Tag(double d, double x, double y, double z, boolean fatal, long now) {
            this.delta = d;
            double spread = 1.0;
            double ox = (double) sign() * range(0.15, 0.65) * spread;
            double oy = -range(0.1, 0.45);
            double oz = (double) sign() * range(0.15, 0.65) * spread;
            this.x = x + ox;
            this.y = y + oy;
            this.z = z + oz;
            this.fatal = fatal;
            this.createdAt = now;
            this.rise = range(0.35, 0.7);
            if (SettingsModule.healthAsHearts.isToggled()) {
                d /= 2.0;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(SliderSetting.formatNumber(Strings.round(Math.abs(d), 1)));
            if (SettingsModule.heartSymbol.isToggled()) {
                Strings.heart(sb);
            }
            this.text = sb.toString();
        }

        boolean visible() {
            return (this.delta > 0.0 ? showHeal : showHurt).isToggled();
        }

        int color() {
            if (traditionalHealthColor.isToggled()) {
                if (this.delta > 0.0) {
                    return -14221530;
                }
                if (this.delta < -6.0 || this.fatal) {
                    return -55770;
                }
                if (this.delta < -3.0) {
                    return -12250;
                }
                return -218;
            }
            return theme.getRGB(0.0);
        }
    }
}
