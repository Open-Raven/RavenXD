package xyz.daikirai.module.player;

import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;

public class AntiFireballModule extends Module {
    public static AntiFireballModule instance;
    public static SliderSetting fov, range;
    public static ButtonSetting disableWhileFlying, disableWhileScaffold, rotateWithBlocks, rotateWithProjectiles, silentSwing;
    private final HashSet<Entity> fireballs = new HashSet<>();
    private double lastRange;
    private double rangeSq;
    private static int ticks;
    public static EntityFireball target;
    public static boolean pending;

    public AntiFireballModule(int key) {
        super("AntiFireball", Category.player, key);
        instance = this;
    }

    public void link() {
        fov = (SliderSetting) ModuleManager.setting(this, "FOV");
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        disableWhileFlying = (ButtonSetting) ModuleManager.setting(this, "Disable while flying");
        disableWhileScaffold = (ButtonSetting) ModuleManager.setting(this, "Disable while scaffold");
        rotateWithBlocks = (ButtonSetting) ModuleManager.setting(this, "Rotate with blocks");
        rotateWithProjectiles = (ButtonSetting) ModuleManager.setting(this, "Rotate with projectiles");
        silentSwing = (ButtonSetting) ModuleManager.setting(this, "Silent swing");
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
        if (PlayerUtils.isBlocking() || Overlay.screenOpen()) {
            return;
        }
        PlayerUtils.attack(target, silentSwing.enabled);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (++ticks >= 1200) {
            ticks = 0;
            if (!this.fireballs.isEmpty()) {
                this.fireballs.removeIf(e -> mc.theWorld.getEntityByID(e.getEntityId()) == null);
            }
        }
        if (Overlay.screenOpen() || disableWhileFlying.enabled && mc.thePlayer.capabilities.isFlying || disableWhileScaffold.enabled && ScaffoldModule.active) {
            pending = false;
            target = null;
            return;
        }
        target = this.find();
        if (target != null && mc.thePlayer.ticksExisted % 2 == 0) {
            pending = true;
        }
    }

    @Override
    public void onDisable() {
        pending = false;
        ticks = 0;
        this.fireballs.clear();
        target = null;
    }

    private EntityFireball find() {
        Minecraft mc = Minecraft.getMinecraft();
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityLargeFireball)) continue;
            if (!this.fireballs.contains(e)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(e) > this.rangeSq) continue;
            float f = (float) fov.getInput();
            if (f != 360.0f && !PlayerUtils.inFov(f, e)) continue;
            return (EntityFireball) e;
        }
        return null;
    }

    @Override
    public void guiUpdate() {
        if (this.lastRange != range.getInput()) {
            this.lastRange = range.getInput();
            this.rangeSq = this.lastRange * this.lastRange;
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.entity == mc.thePlayer) {
            this.fireballs.clear();
        } else if (Hypixel.state != Hypixel.State.LOBBY && e.entity instanceof EntityLargeFireball && mc.thePlayer.getDistanceSqToEntity(e.entity) > 20.0) {
            this.fireballs.add(e.entity);
        }
    }

    @Override
    public float[] getRotations(float lastYaw, float lastPitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (target != null) {
            ItemStack s = mc.thePlayer.getHeldItem();
            if (s != null) {
                if (!rotateWithBlocks.enabled && s.getItem() instanceof ItemBlock) {
                    return null;
                }
                if (!rotateWithProjectiles.enabled && (s.getItem() instanceof ItemSnowball || s.getItem() instanceof ItemEgg
                        || s.getItem() instanceof ItemEnderPearl || s.getItem() instanceof ItemFireball || s.getItem() instanceof ItemBow)) {
                    return null;
                }
            }
            return RotationUtils.toEntityFixed(target, lastYaw, lastPitch);
        }
        return null;
    }
}
