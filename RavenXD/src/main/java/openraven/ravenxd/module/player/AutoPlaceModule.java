package xyz.daikirai.module.player;

import xyz.daikirai.client.Chat;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.KeyUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class AutoPlaceModule extends Module {
    public static SliderSetting frameDelay, minPlaceDelay, minPostDelay, motion;
    public static ButtonSetting delayUntilPre, disableLeft, holdRight, fastPlaceOnJump, pitchCheck, showBlockCount, silentSwing;

    private BlockPos lastPos;
    private int frames;
    private double lastFrameDelay;
    private static MovingObjectPosition lastHit;
    private static long lastPlace, lastPost;
    private static boolean placing;
    private static C08PacketPlayerBlockPlacement heldPlace;
    private static boolean dropSwing, digging;
    private static boolean active;
    private static long fadeInStart, fadeOutStart;
    public static String countText;

    public AutoPlaceModule(int key) {
        super("AutoPlace", Category.player, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        frameDelay = (SliderSetting) ModuleManager.setting(this, "Frame delay");
        minPlaceDelay = (SliderSetting) ModuleManager.setting(this, "Min place delay");
        minPostDelay = (SliderSetting) ModuleManager.setting(this, "Min post delay");
        motion = (SliderSetting) ModuleManager.setting(this, "Motion");
        delayUntilPre = (ButtonSetting) ModuleManager.setting(this, "Delay until pre");
        disableLeft = (ButtonSetting) ModuleManager.setting(this, "Disable left");
        holdRight = (ButtonSetting) ModuleManager.setting(this, "Hold right");
        fastPlaceOnJump = (ButtonSetting) ModuleManager.setting(this, "Fast place on jump");
        pitchCheck = (ButtonSetting) ModuleManager.setting(this, "Pitch check");
        showBlockCount = (ButtonSetting) ModuleManager.setting(this, "Show block count");
        silentSwing = (ButtonSetting) ModuleManager.setting(this, "Silent swing");
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            onPlayer();
            return true;
        }
        if (packet instanceof C07PacketPlayerDigging) {
            return onDig((C07PacketPlayerDigging) packet);
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            return onPlace((C08PacketPlayerBlockPlacement) packet);
        }
        if (packet instanceof C0APacketAnimation) {
            return onSwing();
        }
        return true;
    }

    private static boolean onDig(C07PacketPlayerDigging p) {
        if (!disableLeft.isToggled()) {
            if (canPlace() && Mouse.isButtonDown(0)) {
                if (p.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                    dropSwing = true;
                    digging = true;
                    return false;
                }
                if (p.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK || p.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
                    digging = false;
                    return false;
                }
            } else if (digging && p.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
                digging = false;
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent e) {
        Minecraft mc = mc();
        if (!canPlace()) {
            setActive(false);
            return;
        }
        setActive(true);
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.sideHit == EnumFacing.UP || hit.sideHit == EnumFacing.DOWN) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((double) (now - lastPost) < minPostDelay.getInput()) {
            return;
        }
        if (lastHit != null && (double) this.frames < frameDelay.getInput()) {
            ++this.frames;
            return;
        }
        lastHit = hit;
        if ((double) (now - lastPlace) < minPlaceDelay.getInput()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (this.lastPos != null && BlockUtils.samePos(pos, this.lastPos)) {
            return;
        }
        if (!mc.thePlayer.isSneaking() && BlockUtils.isInteractable(BlockUtils.getBlock(pos))) {
            return;
        }
        lastPlace = now;
        placing = true;
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), pos, hit.sideHit, hit.hitVec)) {
            KeyUtils.simulateMouse(1, true);
            if (!delayUntilPre.isToggled()) {
                PlayerUtils.swing(silentSwing.isToggled(), true);
            }
            KeyUtils.simulateMouse(1, false);
            this.lastPos = pos;
            this.frames = 0;
        }
        placing = false;
    }

    public static boolean canPlace() {
        Minecraft mc = mc();
        if (!Chat.inWorld() || mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        if (delayUntilPre.isToggled() && placing) {
            return false;
        }
        if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 65.0f) {
            return false;
        }
        if (!PlayerUtils.holdingScaffoldBlock()) {
            return false;
        }
        if (disableLeft.isToggled() && Mouse.isButtonDown(0)) {
            return false;
        }
        return !holdRight.isToggled() || Mouse.isButtonDown(1);
    }

    private void reset() {
        this.lastPos = null;
        lastHit = null;
        lastPlace = 0L;
        this.frames = 0;
        lastPost = 0L;
        dropSwing = false;
        digging = false;
        placing = false;
        heldPlace = null;
    }

    private static boolean onPlace(C08PacketPlayerBlockPlacement p) {
        if (p.getStack() != null && p.getStack().getItem() instanceof ItemBlock) {
            if (p.getPlacedBlockDirection() == 255) {
                if (System.currentTimeMillis() - lastPlace <= 50L || Math.random() > 0.5) {
                    return false;
                }
            } else if (placing && delayUntilPre.isToggled()) {
                heldPlace = p;
                return false;
            }
        }
        return true;
    }

    private static boolean onSwing() {
        if (dropSwing) {
            dropSwing = false;
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        if (holdRight.isToggled()) {
            setRightClickDelay(4);
        }
        this.reset();
        setActive(false);
    }

    @Override
    public void guiUpdate() {
        if (this.lastFrameDelay != frameDelay.getInput()) {
            this.reset();
            this.lastFrameDelay = frameDelay.getInput();
        }
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

    private static void setRightClickDelay(int n) {
        Field f = FastPlaceModule.rightClickDelayField();
        if (f != null) {
            try {
                f.set(mc(), n);
            } catch (IllegalAccessException | IndexOutOfBoundsException ignored) {
            }
        }
    }

    private static void onPlayer() {
        if (minPostDelay.getInput() > 0.0) {
            lastPost = System.currentTimeMillis();
        }
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (!PlayerUtils.holdingScaffoldBlock()) {
            return;
        }
        if (showBlockCount.isToggled()) {
            updateCount();
        }
        if (delayUntilPre.isToggled() && heldPlace != null) {
            if (PacketUtils.noUseOrDig()) {
                PacketUtils.sendThroughLag(heldPlace);
                PlayerUtils.swing(silentSwing.isToggled(), true);
            }
            heldPlace = null;
        }
        if (fastPlaceOnJump.isToggled() && ModuleManager.enabled("FastPlace") == null && Mouse.isButtonDown(1)) {
            if (mc.thePlayer.motionY > 0.0) {
                setRightClickDelay(1);
            } else if (!pitchCheck.isToggled()) {
                setRightClickDelay(1000);
            }
        }
        if (motion.isNotDefault() && mc.thePlayer.onGround && PlayerUtils.isMoving() && (!pitchCheck.isToggled() || mc.thePlayer.rotationPitch >= 70.0f)) {
            PlayerUtils.multiplyMotion(motion.getInput());
        }
    }

    public static void renderCount() {
        if (showBlockCount == null || !showBlockCount.isToggled() || countText == null || AutoBlockinModule.percentText != null
                || ScaffoldModule.countText != null || FastPlaceModule.countText() != null) {
            return;
        }
        Minecraft mc = mc();
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

    private static void updateCount() {
        int n = mc().thePlayer.getHeldItem().stackSize;
        String color = null;
        if (n <= 5) {
            color = "§c";
        } else if (n <= 15) {
            color = "§6";
        } else if (n <= 25) {
            color = "§e";
        }
        countText = color == null ? n + " blocks" : color + n + "§f blocks";
    }
}
