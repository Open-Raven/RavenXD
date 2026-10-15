package xyz.daikirai.module.player;

import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public class AutoToolModule extends Module {
    public static SliderSetting hoverDelay;
    public static ButtonSetting disableOnInteractable, disableHoldingBlocks, disableRightClick, enableHoldingTools,
            enableSneaking, requireMouseDown, spoofItem, swapToPrevious;
    public static int previousSlot = -1;

    @Override
    public int spoofedSlot() {
        return previousSlot;
    }
    private boolean swapped;
    private long hoverTicks;

    public AutoToolModule(int key) {
        super("AutoTool", Category.player, key);
    }

    public void link() {
        hoverDelay = (SliderSetting) ModuleManager.setting(this, "Hover delay");
        disableOnInteractable = (ButtonSetting) ModuleManager.setting(this, "Disable on interactable");
        disableHoldingBlocks = (ButtonSetting) ModuleManager.setting(this, "Disable while holding blocks");
        disableRightClick = (ButtonSetting) ModuleManager.setting(this, "Disable while right click");
        enableHoldingTools = (ButtonSetting) ModuleManager.setting(this, "Enable when holding tools");
        enableSneaking = (ButtonSetting) ModuleManager.setting(this, "Enable when sneaking");
        requireMouseDown = (ButtonSetting) ModuleManager.setting(this, "Require mouse down");
        spoofItem = (ButtonSetting) ModuleManager.setting(this, "Spoof item");
        swapToPrevious = (ButtonSetting) ModuleManager.setting(this, "Swap to previous slot");
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.inGameHasFocus || mc.currentScreen != null || ScaffoldModule.active || AutoBlockinModule.active || disableHoldingBlocks.enabled && PlayerUtils.holdingScaffoldBlock()
                || requireMouseDown.enabled && !Mouse.isButtonDown(0) || disableRightClick.enabled && Mouse.isButtonDown(1)) {
            swapBack();
            this.swapped = false;
            return;
        }
        boolean tools = enableHoldingTools.enabled;
        boolean sneak = enableSneaking.enabled;
        boolean ok = !tools && !sneak || sneak && mc.thePlayer.isSneaking() || tools && PlayerUtils.holdingTool();
        if (!ok) {
            swapBack();
            this.swapped = false;
            return;
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            this.hoverTicks = 0L;
            this.reset();
            return;
        }
        if (!hoverDelay.isAtMin() && (double) (++this.hoverTicks) < hoverDelay.getInput()) {
            return;
        }
        Block block = mc.theWorld.getBlockState(mop.getBlockPos()).getBlock();
        if (disableOnInteractable.enabled && BlockUtils.isInteractable(block)) {
            this.reset();
            return;
        }
        if (!this.swapped && swapToPrevious.enabled) {
            previousSlot = mc.thePlayer.inventory.currentItem;
        }
        this.swapped = true;
        setSlot(ItemUtils.bestToolSlot(block));
    }

    public static void swapBack() {
        if (previousSlot != -1) {
            setSlot(previousSlot);
            previousSlot = -1;
        }
    }

    public static boolean setSlot(int n) {
        if (n == -1) {
            return false;
        }
        Minecraft.getMinecraft().thePlayer.inventory.currentItem = n;
        return true;
    }

    private void reset() {
        swapBack();
        this.swapped = false;
    }

    @Override
    public void onDisable() {
        this.hoverTicks = 0L;
        this.reset();
    }
}
