package xyz.daikirai.module.player;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class WaterBucketModule extends Module {
    public static WaterBucketModule instance;
    public static SliderSetting minimumFallDistance;
    public static ButtonSetting pickupWater, silentAim, switchToItem;
    private static long stage;
    private static long placedAt;
    private static float aimPitch;

    public WaterBucketModule(int key) {
        super("Water Bucket", Category.player, key);
        instance = this;
    }

    public void link() {
        minimumFallDistance = (SliderSetting) ModuleManager.setting(this, "Minimum fall distance");
        pickupWater = (ButtonSetting) ModuleManager.setting(this, "Pickup water");
        silentAim = (ButtonSetting) ModuleManager.setting(this, "Silent aim");
        switchToItem = (ButtonSetting) ModuleManager.setting(this, "Switch to item");
    }

    private static boolean holding(Item item) {
        ItemStack s = Minecraft.getMinecraft().thePlayer.getHeldItem();
        return s != null && s.getItem() == item;
    }

    private static MovingObjectPosition target() {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition m = PlayerUtils.rayTraceBlocks(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw,
                silentAim.enabled ? aimPitch : mc.thePlayer.rotationPitch);
        if (m != null && m.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && m.sideHit == EnumFacing.UP) {
            return m;
        }
        return null;
    }

    private static void use(MovingObjectPosition m) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), m.getBlockPos(), m.sideHit, m.hitVec);
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
    }

    private static double minFall() {
        PotionEffect e = Minecraft.getMinecraft().thePlayer.getActivePotionEffect(Potion.jump);
        return minimumFallDistance.getInput() - 1.5 + (double) (e == null ? 0 : e.getAmplifier() + 1);
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused()) {
            return;
        }
        if (stage == 1L) {
            boolean reset = false;
            if (!mc.thePlayer.onGround) {
                if (Math.abs(System.currentTimeMillis() - placedAt) > 500L) {
                    reset = true;
                }
            } else {
                if (pickupWater.enabled && holding(Items.bucket)) {
                    MovingObjectPosition m = target();
                    if (m != null) {
                        use(m);
                        stage = !silentAim.enabled ? 0L : 2L;
                    }
                }
                if (stage != 2L) {
                    reset = true;
                }
            }
            if (reset) {
                stage = 0L;
                aimPitch = 0.0f;
            }
            return;
        }
        if (stage == 2L) {
            stage = 0L;
            aimPitch = 0.0f;
            return;
        }
        if (stage == 0L && aimPitch != 0.0f && PlayerUtils.inLiquid()) {
            aimPitch = 0.0f;
            return;
        }
        if (!mc.thePlayer.capabilities.isFlying && !mc.thePlayer.capabilities.isCreativeMode && !mc.thePlayer.onGround
                && !((double) mc.thePlayer.fallDistance < minFall())) {
            int slot = waterSlot();
            if (slot != -1) {
                if (!silentAim.enabled) {
                    if (mc.thePlayer.rotationPitch < 77.0f) {
                        return;
                    }
                } else if (aimPitch == 0.0f) {
                    if (PlayerUtils.heightAboveGround() > 1.0) {
                        aimPitch = mc.thePlayer.movementInput.moveForward != 1.0f ? 90.0f : (float) (81 + (int) (3.0 * Math.random()));
                    }
                    return;
                }
                long now = System.currentTimeMillis();
                if (Math.abs(now - placedAt) >= 500L) {
                    MovingObjectPosition m = target();
                    if (m == null) {
                        return;
                    }
                    if (holding(Items.water_bucket) || switchToItem.enabled && select(slot)) {
                        placedAt = now;
                        use(m);
                        stage = 1L;
                        return;
                    }
                    aimPitch = 0.0f;
                }
                return;
            }
        }
        if (aimPitch != 0.0f) {
            stage = 0L;
            aimPitch = 0.0f;
        }
    }

    @Override
    public void onDisable() {
        stage = 0L;
        placedAt = 0L;
        aimPitch = 0.0f;
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (aimPitch != 0.0f) {
            return new float[]{Minecraft.getMinecraft().thePlayer.rotationYaw, aimPitch};
        }
        return null;
    }

    private static int waterSlot() {
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s == null || s.getItem() != Items.water_bucket) continue;
            return i;
        }
        return -1;
    }

    private static boolean select(int n) {
        if (n != -1) {
            Minecraft.getMinecraft().thePlayer.inventory.currentItem = n;
            return true;
        }
        return false;
    }
}
