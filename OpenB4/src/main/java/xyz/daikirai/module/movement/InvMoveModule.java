package xyz.daikirai.module.movement;

import xyz.daikirai.client.Overlay;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.player.InvManagerModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiBeacon;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InvMoveModule extends Module {
    public static InvMoveModule instance;
    public static SliderSetting inventorySetting, chestSetting, motion;
    public static ButtonSetting modifyMotionAfterClick, slowMotionWhenNecessary, allowJumping, allowMovement, allowMidair, allowRotating,
            allowSpeedPotion, allowSprinting, onlyWithInventoryManager;

    public enum Inventory {
        Disabled, Vanilla, Legit, Legit_pa1Slowpa2, Pause, Blink, Close;

        public boolean isLegitMode() {
            return this == Legit || this == Legit_pa1Slowpa2;
        }
    }

    public enum Chest {
        Disabled, Vanilla, Pause, Blink
    }

    public enum State {
        IDLE, OPEN, ACTIVE, FINISH
    }

    public static Inventory inventory = Inventory.Vanilla;
    public static Chest chest = Chest.Vanilla;
    private static State state = State.IDLE;
    private static final LinkedHashMap<Integer, List<C0EPacketClickWindow>> queued = new LinkedHashMap<Integer, List<C0EPacketClickWindow>>();
    private static final Map<Integer, Boolean> emptyCursor = new HashMap<Integer, Boolean>();
    private static boolean closeQueued;
    private static int slowTicks;
    private static boolean clickedThisTick, clicked;
    private static boolean blinking;
    private static int releaseTicks;
    private static boolean sendClose;
    private static C0DPacketCloseWindow ownClose;
    private static boolean closed;
    private static final float[] screenRotation = {0.0f, 100.0f};
    private static boolean mouseLook;
    private static List<KeyBinding> keys;

    public InvMoveModule(int key) {
        super("InvMove", Category.movement, key);
        instance = this;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static List<KeyBinding> keys() {
        if (keys == null) {
            Minecraft mc = mc();
            keys = Arrays.asList(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindLeft,
                    mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSneak);
        }
        return keys;
    }

    public void link() {
        inventorySetting = (SliderSetting) ModuleManager.setting(this, "Inventory");
        chestSetting = (SliderSetting) ModuleManager.setting(this, "Chest & others");
        motion = (SliderSetting) ModuleManager.setting(this, "Motion");
        modifyMotionAfterClick = (ButtonSetting) ModuleManager.setting(this, "Modify motion after click");
        slowMotionWhenNecessary = (ButtonSetting) ModuleManager.setting(this, "Slow motion when necessary");
        allowJumping = (ButtonSetting) ModuleManager.setting(this, "Allow jumping");
        allowMovement = (ButtonSetting) ModuleManager.setting(this, "Allow movement");
        allowMidair = (ButtonSetting) ModuleManager.setting(this, "Allow midair");
        allowRotating = (ButtonSetting) ModuleManager.setting(this, "Allow rotating");
        allowSpeedPotion = (ButtonSetting) ModuleManager.setting(this, "Allow speed potion");
        allowSprinting = (ButtonSetting) ModuleManager.setting(this, "Allow sprinting");
        onlyWithInventoryManager = (ButtonSetting) ModuleManager.setting(this, "Only with inventory manager");
    }

    @Override
    public void onPostPlayerInput() {
        if (inventory.isLegitMode() && (slowTicks > 0 || state == State.ACTIVE && waiting())) {
            PlayerUtils.setForward(0.0f);
            PlayerUtils.setStrafe(0.0f);
            PlayerUtils.setJump(false);
            PlayerUtils.setSneak(false);
        } else if (clickedThisTick && (inventory == Inventory.Pause && inInventory() || chest == Chest.Pause && inChest())) {
            PlayerUtils.setForward(0.0f);
            PlayerUtils.setStrafe(0.0f);
        }
    }

    private static boolean inCreative() {
        return mc().currentScreen instanceof GuiContainerCreative;
    }

    @Override
    public void onPostMotion() {
        clickedThisTick = false;
        if (inventory.isLegitMode()) {
            if (state == State.FINISH) {
                state = queued.isEmpty() ? State.IDLE : State.OPEN;
            }
            if (slowTicks > 0) {
                --slowTicks;
            }
        }
    }

    public static boolean busy() {
        return inventory.isLegitMode() && (slowTicks > 0 || state == State.OPEN || state == State.ACTIVE && waiting());
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (packet instanceof C16PacketClientStatus) {
            return onOpen((C16PacketClientStatus) packet);
        }
        if (packet instanceof C0DPacketCloseWindow) {
            return onClose((C0DPacketCloseWindow) packet);
        }
        if (packet instanceof C0EPacketClickWindow) {
            return onClick((C0EPacketClickWindow) packet);
        }
        return true;
    }

    private static boolean onOpen(C16PacketClientStatus p) {
        if (p.getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
            if (inventory.isLegitMode()) {
                return false;
            }
            if (inventory == Inventory.Blink) {
                if (!blinking) {
                    blinking = true;
                    PacketQueue.open();
                }
            } else if (inventory == Inventory.Close) {
                return false;
            }
        }
        return true;
    }

    private static boolean onClick(C0EPacketClickWindow p) {
        Minecraft mc = mc();
        if (inventory.isLegitMode() && inInventory()) {
            boolean holding = mc.thePlayer.inventory.getItemStack() != null;
            if (!holding) {
                int mode = p.getMode();
                if (mode == 3) {
                    return false;
                }
                if (mode == 0 && p.getUsedButton() == 1) {
                    int slot = p.getSlotId();
                    if (slot >= 0 && slot < mc.thePlayer.inventoryContainer.inventorySlots.size() && mc.thePlayer.inventoryContainer.getSlot(slot).getStack() == null) {
                        return false;
                    }
                } else if (mode == 4 && p.getSlotId() == -999) {
                    return false;
                }
            }
            int tick = mc.thePlayer.ticksExisted;
            queued.computeIfAbsent(tick, k -> new ArrayList<C0EPacketClickWindow>()).add(p);
            emptyCursor.put(tick, !holding);
            clicked = true;
            clickedThisTick = true;
            if (state == State.IDLE) {
                state = State.OPEN;
            }
            return false;
        }
        if (inventory == Inventory.Close && !inChestLike()) {
            sendClose = true;
            if (!blinking) {
                blinking = true;
                PacketQueue.open();
                releaseTicks = 3;
            }
        }
        clickedThisTick = true;
        clicked = true;
        return true;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = mc();
        if (e.phase != TickEvent.Phase.END || !mouseLook) {
            return;
        }
        if (Display.isActive()) {
            int x = Mouse.getX();
            int y = Mouse.getY();
            int cx = mc.displayWidth / 2;
            int cy = mc.displayHeight / 2;
            int dx = x - cx;
            int dy = y - cy;
            double d = (double) mc.gameSettings.mouseSensitivity * 0.6 + 0.2;
            double d2 = d * d * d * 1.2;
            mc.thePlayer.rotationYaw = (float) ((double) mc.thePlayer.rotationYaw + (double) dx * d2);
            mc.thePlayer.rotationPitch = (float) ((double) mc.thePlayer.rotationPitch - (double) dy * d2);
            Mouse.setCursorPosition(cx, cy);
        }
    }

    @Override
    public String getInfo() {
        if (inventory != Inventory.Disabled) {
            if (inventory.isLegitMode()) {
                return "Legit";
            }
            return inventory.name();
        }
        if (chest != Chest.Disabled) {
            return chest.name();
        }
        return null;
    }

    public static boolean inInventory() {
        return mc().currentScreen instanceof GuiInventory;
    }

    private static void sendCloseNow() {
        PacketUtils.sendThroughLag(new C0DPacketCloseWindow(0));
        if (inventory == Inventory.Legit_pa1Slowpa2) {
            slowTicks = 1;
        }
        closeQueued = false;
        state = State.FINISH;
    }

    private static boolean inChest() {
        return mc().currentScreen instanceof GuiChest;
    }

    public static boolean inChestLike() {
        Minecraft mc = mc();
        return inChest() || mc.currentScreen instanceof GuiCrafting || mc.currentScreen instanceof GuiFurnace || mc.currentScreen instanceof GuiBeacon
                || mc.currentScreen instanceof GuiBrewingStand || mc.currentScreen instanceof GuiDispenser;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            this.onDisable();
        }
    }

    public static void arrowRotate() {
        Minecraft mc = mc();
        float yaw = 0.0f;
        float pitch = 0.0f;
        if (Keyboard.isKeyDown(205)) {
            yaw += 8.0f;
        }
        if (Keyboard.isKeyDown(203)) {
            yaw -= 8.0f;
        }
        if (Keyboard.isKeyDown(200)) {
            pitch -= 5.0f;
        }
        if (Keyboard.isKeyDown(208)) {
            pitch += 5.0f;
        }
        if (yaw != 0.0f) {
            yaw = (float) ((double) yaw + (Math.random() - 0.5));
            mc.thePlayer.rotationYaw += yaw;
        }
        if (pitch != 0.0f) {
            pitch = (float) ((double) pitch + (Math.random() - 0.5));
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitch, -90.0f, 90.0f);
        }
    }

    @Override
    public void onDisable() {
        if (blinking) {
            blinking = false;
            PacketQueue.close();
        }
        mouseLook = false;
        sendClose = false;
        closed = false;
        clickedThisTick = false;
        clicked = false;
        releaseTicks = 0;
        screenRotation[1] = 100.0f;
        ownClose = null;
        resetLegit();
    }

    private static boolean onClose(C0DPacketCloseWindow p) {
        clicked = false;
        if (inventory.isLegitMode() && (inInventory() || state != State.IDLE || !queued.isEmpty())) {
            if (state != State.IDLE || !queued.isEmpty()) {
                closeQueued = true;
            }
            return false;
        }
        if (inventory == Inventory.Blink || chest == Chest.Blink) {
            if (blinking) {
                releaseTicks = 2;
            }
        } else if (inventory == Inventory.Close && inInventory() && p != ownClose) {
            return false;
        }
        return true;
    }

    private static void resetLegit() {
        state = State.IDLE;
        queued.clear();
        emptyCursor.clear();
        closeQueued = false;
    }

    @Override
    public void guiUpdate() {
        inventory = Inventory.values()[(int) inventorySetting.getInput()];
        chest = Chest.values()[(int) chestSetting.getInput()];
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        if (releaseTicks > 0 && --releaseTicks == 0) {
            blinking = false;
            PacketQueue.close();
        }
        if (inventory.isLegitMode()) {
            if (state == State.OPEN) {
                PacketUtils.sendThroughLag(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                state = State.ACTIVE;
            } else if (state == State.ACTIVE) {
                if (!queued.isEmpty()) {
                    Map.Entry<Integer, List<C0EPacketClickWindow>> entry = queued.entrySet().iterator().next();
                    int tick = entry.getKey();
                    for (C0EPacketClickWindow p : entry.getValue()) {
                        PacketUtils.sendThroughLag(p);
                    }
                    boolean empty = Boolean.TRUE.equals(emptyCursor.remove(tick));
                    queued.remove(tick);
                    if (queued.isEmpty() && (empty || closeQueued)) {
                        sendCloseNow();
                    }
                } else if (closeQueued) {
                    sendCloseNow();
                }
            }
        } else if (state != State.IDLE || !queued.isEmpty() || closeQueued) {
            resetLegit();
        }
        if (closed) {
            closed = false;
            screenRotation[1] = 100.0f;
        }
        if (sendClose) {
            sendClose = false;
            ownClose = new C0DPacketCloseWindow(0);
            PacketUtils.send(ownClose);
            e.yaw = RotationManager.lastYaw;
            e.pitch = RotationManager.lastPitch;
        }
        mouseLook = false;
        if (mc.currentScreen == null || mc.currentScreen instanceof GuiChat || Overlay.ownScreen()) {
            return;
        }
        if (!allowMidair.isToggled() && PlayerUtils.groundTicks <= 2L) {
            return;
        }
        if (!allowSpeedPotion.isToggled() && PlayerUtils.speedLevel() > 0) {
            return;
        }
        if (onlyWithInventoryManager.isToggled() && (ModuleManager.enabled("InvManager") == null || InvManagerModule.pausedInLobby())) {
            return;
        }
        boolean container = true;
        if (inInventory()) {
            if (inventory == Inventory.Disabled) {
                return;
            }
            if (onlyWithInventoryManager.isToggled() && !InvManagerModule.autoArmor.isNotDefault() && !InvManagerModule.autoSort.isNotDefault()
                    && !InvManagerModule.inventoryCleaner.isNotDefault()) {
                return;
            }
        } else if (inChestLike()) {
            if (chest == Chest.Disabled) {
                return;
            }
            if (onlyWithInventoryManager.isToggled() && !InvManagerModule.chestStealer.isNotDefault()) {
                return;
            }
            if (chest == Chest.Blink && !blinking) {
                blinking = true;
                PacketQueue.open();
            }
        } else {
            if (inCreative()) {
                return;
            }
            container = false;
        }
        if (container) {
            if (screenRotation[1] == 100.0f) {
                screenRotation[0] = mc.thePlayer.rotationYaw;
                screenRotation[1] = mc.thePlayer.rotationPitch;
            }
            if (motion.isNotDefault() && (!modifyMotionAfterClick.isToggled() || clicked) && PlayerUtils.isMoving()) {
                double d = motion.getInput();
                if (slowMotionWhenNecessary.isToggled()) {
                    if (!mc.thePlayer.onGround) {
                        d -= 0.2;
                        if (!allowSprinting.isToggled()) {
                            d -= 0.05;
                        }
                    }
                    d -= PlayerUtils.perSpeedLevel(0.25);
                    d = Math.max(0.0, d);
                }
                PlayerUtils.multiplyMotion(d);
            }
        }
        if (allowMovement.isToggled()) {
            setKeys(false, false, container);
        }
        if (allowRotating.isToggled()) {
            arrowRotate();
            if (ModuleManager.enabled("InvManager") != null && inChest() && InvManagerModule.chestStealer.isNotDefault() && InvManagerModule.closeChest.isToggled()
                    && (InvManagerModule.stealFromCustomChests.isToggled() || ((ContainerChest) mc.thePlayer.openContainer).getLowerChestInventory().getName().contains("Chest"))) {
                mouseLook = true;
            }
        }
    }

    public static void setKeys(boolean release, boolean all, boolean container) {
        for (KeyBinding k : keys()) {
            if (!all && container) {
                if (k == mc().gameSettings.keyBindSneak) continue;
                if (k == mc().gameSettings.keyBindJump && !allowJumping.isToggled()) continue;
            }
            KeyBinding.setKeyBindState(k.getKeyCode(), !release && GameSettings.isKeyDown(k));
        }
    }

    private static boolean waiting() {
        if (inventory == Inventory.Legit_pa1Slowpa2) {
            return true;
        }
        if (closeQueued) {
            return queued.size() > 1;
        }
        if (queued.size() != 1) {
            return true;
        }
        Map.Entry<Integer, List<C0EPacketClickWindow>> entry = queued.entrySet().iterator().next();
        return !Boolean.TRUE.equals(emptyCursor.get(entry.getKey()));
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (screenRotation[1] != 100.0f) {
            return screenRotation;
        }
        return null;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (e.gui == null && (inInventory() || inChestLike() || inCreative())) {
            closed = true;
            setKeys(false, true, false);
        }
    }
}
