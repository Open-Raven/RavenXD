package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class MobESPModule extends Module {
    public static ButtonSetting healthBar, outline, shaded, blaze, creeper, enderman, ghast, ironGolem, silverfish, skeleton, slime, spider, witch, wolf, zombie, zombiePigman;

    public MobESPModule(int key) {
        super("MobESP", Category.render, key);
    }

    public void link() {
        ButtonSetting[] s = new ButtonSetting[16];
        for (int i = 0; i < s.length; ++i) {
            s[i] = (ButtonSetting) this.settings.get(i);
        }
        healthBar = s[0];
        outline = s[1];
        shaded = s[2];
        blaze = s[3];
        creeper = s[4];
        enderman = s[5];
        ghast = s[6];
        ironGolem = s[7];
        silverfish = s[8];
        skeleton = s[9];
        slime = s[10];
        spider = s[11];
        witch = s[12];
        wolf = s[13];
        zombie = s[14];
        zombiePigman = s[15];
    }

    private void render(Entity entity, int n, float f) {
        if (healthBar.isToggled()) {
            Render3D.entityBox(entity, 4, n, f);
        }
        if (outline.isToggled()) {
            Render3D.entityBox(entity, 1, n, f);
        }
        if (shaded.isToggled()) {
            Render3D.shadedBox((EntityLivingBase) entity, RenderUtils.withAlpha(n, 90), f);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || ((EntityLivingBase) entity).deathTime != 0) continue;
            this.renderMob(entity, e.partialTicks);
        }
    }

    private void renderMob(Entity entity, float f) {
        if (entity instanceof EntityBlaze) {
            if (blaze.isToggled()) this.render(entity, Color.orange.getRGB(), f);
        } else if (entity instanceof EntityCreeper) {
            if (creeper.isToggled()) this.render(entity, Color.green.getRGB(), f);
        } else if (entity instanceof EntityEnderman) {
            if (enderman.isToggled()) this.render(entity, Color.black.getRGB(), f);
        } else if (entity instanceof EntityGhast) {
            if (ghast.isToggled()) this.render(entity, -1, f);
        } else if (entity instanceof EntityIronGolem) {
            if (ironGolem.isToggled()) this.render(entity, -1, f);
        } else if (entity instanceof EntitySilverfish) {
            if (silverfish.isToggled()) this.render(entity, Color.gray.getRGB(), f);
        } else if (entity instanceof EntitySkeleton) {
            if (skeleton.isToggled()) this.render(entity, -1, f);
        } else if (entity instanceof EntitySlime) {
            if (slime.isToggled()) this.render(entity, Color.green.getRGB(), f);
        } else if (entity instanceof EntitySpider) {
            if (spider.isToggled()) this.render(entity, Color.black.getRGB(), f);
        } else if (entity instanceof EntityWitch) {
            if (witch.isToggled()) this.render(entity, Color.magenta.getRGB(), f);
        } else if (entity instanceof EntityWolf) {
            if (wolf.isToggled()) this.render(entity, -1, f);
        } else if (entity instanceof EntityPigZombie) {
            if (zombiePigman.isToggled()) this.render(entity, Color.pink.getRGB(), f);
        } else if (entity instanceof EntityZombie) {
            if (zombie.isToggled()) this.render(entity, Color.blue.getRGB(), f);
        }
    }
}
