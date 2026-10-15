package xyz.daikirai.module.movement;

import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Sounds;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Jump45Module extends Module {
    public static Jump45Module instance;
    public static ButtonSetting flying, holdingBlocks, holdingBow, holdingThrowable, holdingRod, inLiquid, targeting;
    public static boolean rotating;
    private static float offset;

    public Jump45Module(int key) {
        super("45 Jump", Category.movement, key);
        instance = this;
    }

    public void link() {
        flying = (ButtonSetting) ModuleManager.setting(this, "Flying");
        holdingBlocks = (ButtonSetting) ModuleManager.setting(this, "Holding blocks");
        holdingBow = (ButtonSetting) ModuleManager.setting(this, "Holding bow");
        holdingThrowable = (ButtonSetting) ModuleManager.setting(this, "Holding throwable");
        holdingRod = (ButtonSetting) ModuleManager.setting(this, "Holding rod");
        inLiquid = (ButtonSetting) ModuleManager.setting(this, "In liquid");
        targeting = (ButtonSetting) ModuleManager.setting(this, "Targeting");
    }

    @Override
    public void onDisable() {
        rotating = false;
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (shouldRotate()) {
            if (!rotating) {
                rotating = true;
                offset = 45.0f + Sounds.random(-0.3f, 0.3f);
            }
            return new float[]{mc.thePlayer.rotationYaw + offset, mc.thePlayer.rotationPitch};
        }
        rotating = false;
        return null;
    }

    private static boolean shouldRotate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer.onGround || !mc.thePlayer.isSprinting()) {
            return false;
        }
        if (!PacketUtils.noInteractionOrBreak()) {
            return false;
        }
        float strafe = 0.0f;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) strafe += 1.0f;
        if (mc.gameSettings.keyBindRight.isKeyDown()) strafe -= 1.0f;
        if (strafe != 0.0f) {
            return false;
        }
        if (flying.enabled && mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        if (inLiquid.enabled && PlayerUtils.inLiquid()) {
            return false;
        }
        ItemStack stack = mc.thePlayer.getHeldItem();
        Item item = stack == null ? null : stack.getItem();
        if (item != null) {
            if (holdingBlocks.enabled && ItemUtils.isScaffoldBlock(stack)) {
                return false;
            }
            if (holdingBow.enabled && item == Items.bow) {
                return false;
            }
            if (holdingThrowable.enabled && ItemUtils.isThrowable(stack)) {
                return false;
            }
            if (holdingRod.enabled && item == Items.fishing_rod) {
                return false;
            }
        }
        return !targeting.enabled || !PlayerUtils.targetMatches(2);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (rotating && e.buttonstate && (e.button == 0 || e.button == 1) && mc.entityRenderer != null) {
            float f = mc.thePlayer.rotationYawHead;
            mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw;
            mc.entityRenderer.getMouseOver(1.0f);
            if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                mc.thePlayer.rotationYawHead = f;
            }
        }
    }
}
