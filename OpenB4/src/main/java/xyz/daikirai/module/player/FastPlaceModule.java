package xyz.daikirai.module.player;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class FastPlaceModule extends Module {
    public static FastPlaceModule instance;
    public static SliderSetting delay, minimumHoldTime;
    public static ButtonSetting allowLadders, autoSwapBlocks, blocksOnly, pitchCheck, showBlockCount;
    private static Field rightClickDelay;
    private static boolean active;
    private static long fadeInStart, fadeOutStart;
    private static long holdStart;
    private static String countText;
    private static int lastSlot = -1;
    private static boolean lastHeldBlocks;

    public FastPlaceModule(int key) {
        super("FastPlace", Category.player, key);
        instance = this;
        try {
            rightClickDelay = ReflectionHelper.findField(Minecraft.class, "rightClickDelayTimer", "field_71467_ac");
        } catch (Exception ignored) {
        }
    }

    public void link() {
        delay = (SliderSetting) ModuleManager.setting(this, "Delay");
        minimumHoldTime = (SliderSetting) ModuleManager.setting(this, "Minimum hold time");
        allowLadders = (ButtonSetting) ModuleManager.setting(this, "Allow ladders");
        autoSwapBlocks = (ButtonSetting) ModuleManager.setting(this, "Auto swap blocks");
        blocksOnly = (ButtonSetting) ModuleManager.setting(this, "Blocks only");
        pitchCheck = (ButtonSetting) ModuleManager.setting(this, "Pitch check");
        showBlockCount = (ButtonSetting) ModuleManager.setting(this, "Show block count");
    }

    public static String countText() {
        return countText;
    }

    public static Field rightClickDelayField() {
        return rightClickDelay;
    }

    @Override
    public boolean onSendPacket(net.minecraft.network.Packet<?> packet) {
        if (!(packet instanceof C08PacketPlayerBlockPlacement)) {
            return true;
        }
        C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
        return p.getPlacedBlockDirection() != 255 || !(p.getStack() != null && p.getStack().getItem() instanceof ItemBlock) || !(Math.random() > 0.4);
    }

    private static void setActive(boolean b) {
        if (b) {
            if (!active) {
                active = true;
                fadeInStart = System.currentTimeMillis();
            }
        } else if (active) {
            active = false;
            fadeOutStart = System.currentTimeMillis();
        }
    }

    private static void updateCount() {
        Minecraft mc = Minecraft.getMinecraft();
        int n = autoSwapBlocks.enabled ? AutoSwapModule.countBlocks() : mc.thePlayer.getHeldItem().stackSize;
        String color = null;
        if (n <= 5) {
            color = "§c";
        } else if (n <= 15) {
            color = "§6";
        } else if (n <= 25) {
            color = "§e";
        }
        if (color == null) {
            countText = n + " blocks";
        } else {
            countText = color + n + "§f blocks";
        }
    }

    public static void renderCount() {
        if (showBlockCount == null || !showBlockCount.enabled || countText == null || AutoBlockinModule.percentText != null || ScaffoldModule.countText != null
                || SafeWalkModule.scaffoldActive) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();
        int alpha;
        if (active) {
            long l = now - fadeInStart;
            alpha = l < 125L ? (int) ((float) l / 125.0f * 240.0f) : 240;
        } else {
            long l = now - fadeOutStart;
            if (l < 125L) {
                alpha = (int) (240.0f - (float) l / 125.0f * 240.0f);
            } else {
                alpha = 0;
                countText = null;
            }
        }
        if (alpha > 0) {
            float x = (float) RenderUtils.scaledWidth / 2.0f + 11.0f;
            float y = (float) RenderUtils.scaledHeight / 2.0f - 3.0f;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(countText, x, y, RenderUtils.withAlpha(-1, RenderUtils.clampAlpha(alpha)), true);
            GlStateManager.disableBlend();
        }
    }

    @Override
    public void onDisable() {
        setActive(false);
        lastHeldBlocks = false;
        holdStart = 0L;
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (pitchCheck.enabled && mc.thePlayer.rotationPitch < 65.0f) {
            setActive(false);
            return;
        }
        int slot = mc.thePlayer.inventory.currentItem;
        boolean blocks = PlayerUtils.holdingScaffoldBlock();
        if (autoSwapBlocks.enabled && (!blocks || (double) mc.thePlayer.getHeldItem().stackSize <= AutoSwapModule.switchAt.getInput())
                && lastHeldBlocks && slot == lastSlot) {
            AutoSwapModule.swap(xyz.daikirai.util.SwapSlot.FastPlace);
            blocks = PlayerUtils.holdingScaffoldBlock();
        }
        lastSlot = slot;
        lastHeldBlocks = blocks;
        if (blocks) {
            if (showBlockCount.enabled) {
                updateCount();
            }
        } else {
            ItemStack held = mc.thePlayer.getHeldItem();
            boolean ladder = held != null && held.getItem() instanceof ItemBlock && ((ItemBlock) held.getItem()).getBlock() == Blocks.ladder;
            if (!(!blocksOnly.enabled || allowLadders.enabled && ladder)) {
                setActive(false);
                return;
            }
            countText = null;
        }
        if (minimumHoldTime.isNotDefault()) {
            long now = System.currentTimeMillis();
            if (!Mouse.isButtonDown(1)) {
                holdStart = 0L;
                setActive(false);
                return;
            }
            if (holdStart == 0L) {
                holdStart = now;
            }
            if ((double) (now - holdStart) < minimumHoldTime.getInput() - 10.0) {
                setActive(false);
                return;
            }
        }
        setActive(true);
        try {
            int d = (int) delay.getInput();
            if (d > 1) {
                if (rightClickDelay.getInt(mc) == 4) {
                    rightClickDelay.set(mc, d);
                }
            } else {
                rightClickDelay.set(mc, 1);
            }
        } catch (IllegalAccessException | IndexOutOfBoundsException ignored) {
        }
    }
}
