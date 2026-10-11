package xyz.daikirai.module.combat;

import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class AutoClickerModule extends Module {
    public static AutoClickerModule instance;
    public static SliderSetting minCps, maxCps, inventoryCps, blockHit, jitter;
    public static ButtonSetting leftClick, clickOnDamage, breakBlocks, weaponOnly, rightClick, blocksOnly;

    private static long nextPress, nextRelease;
    private static long phaseEnd, extraEnd;
    private static boolean slowPhase;
    private static double slowFactor;
    private static double slowMin = 1.1, slowMax = 1.25;
    private static int extraMin = 50, extraMax = 90;
    private static int blockHitAcc = 100;
    private static boolean blockHitDown;
    private static boolean breaking;
    private static boolean paused;
    private static boolean inventoryClick;
    public static boolean firing;

    public AutoClickerModule(int key) {
        super("AutoClicker", Category.combat, key);
        instance = this;
    }

    public void link() {
        minCps = (SliderSetting) ModuleManager.setting(this, "Min CPS");
        maxCps = (SliderSetting) ModuleManager.setting(this, "Max CPS");
        inventoryCps = (SliderSetting) ModuleManager.setting(this, "Inventory CPS");
        blockHit = (SliderSetting) ModuleManager.setting(this, "Block hit");
        jitter = (SliderSetting) ModuleManager.setting(this, "Jitter");
        leftClick = (ButtonSetting) ModuleManager.setting(this, "Left click");
        clickOnDamage = (ButtonSetting) ModuleManager.setting(this, "^ Click on damage");
        breakBlocks = (ButtonSetting) ModuleManager.setting(this, "^ Break blocks");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "^ Weapon only");
        rightClick = (ButtonSetting) ModuleManager.setting(this, "Right click");
        blocksOnly = (ButtonSetting) ModuleManager.setting(this, "^ Blocks only");
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static boolean on() {
        return instance != null && instance.isEnabled();
    }

    public static boolean clicking() {
        return nextPress > 0L;
    }

    public static boolean clickingLeft() {
        return clicking() && leftClick.enabled && Mouse.isButtonDown(0);
    }

    @Override
    public boolean isClickingForReach() {
        return clickingLeft();
    }

    private static int randomInt(int a, int b) {
        return a + (int) ((double) (b - a + 1) * Math.random());
    }

    @Override
    public void onPreUpdate() {
        if (inventoryClick) {
            inventoryClick = false;
            if (mc().currentScreen != null) {
                clickScreen(mc().currentScreen);
            }
        }
    }

    private static void schedule(boolean inventory) {
        double cps;
        if (!inventory) {
            cps = MathUtils.randomBetween(minCps, maxCps) + MathUtils.randomBetween(-0.3, 0.4);
        } else {
            cps = inventoryCps.getInput() + MathUtils.randomBetween(-0.6, 0.8);
        }
        long now = System.currentTimeMillis();
        long delay = (int) Math.round(1000.0 / cps);
        if (now > phaseEnd) {
            if (slowPhase) {
                slowPhase = false;
            } else if (MathUtils.RANDOM.nextInt(100) >= 80) {
                slowPhase = true;
                slowFactor = MathUtils.randomBetween(slowMin, slowMax);
            }
            phaseEnd = now + (long) randomInt(700, 2200);
        }
        if (slowPhase) {
            delay = (long) ((double) delay * slowFactor);
        }
        if (now > extraEnd) {
            if (MathUtils.RANDOM.nextInt(100) >= 75) {
                delay += (long) randomInt(extraMin, extraMax);
            }
            extraEnd = now + (long) randomInt(700, 2200);
        }
        nextPress = now + delay;
        nextRelease = now + delay / 2L - (long) MathUtils.RANDOM.nextInt(10);
    }

    public static boolean fakeMouse(int button, boolean down) {
        boolean ok = true;
        try {
            MouseEvent e = new MouseEvent();
            Field fb = ReflectionHelper.findField(MouseEvent.class, "button");
            Field fs = ReflectionHelper.findField(MouseEvent.class, "buttonstate");
            fb.set(e, button);
            fs.set(e, down);
            firing = true;
            ok = !MinecraftForge.EVENT_BUS.post(e);
            firing = false;
            if (ok) {
                Field f = ReflectionHelper.findField(Mouse.class, "buttons");
                ByteBuffer buf = (ByteBuffer) f.get(null);
                if (button >= 0 && button < buf.limit()) {
                    buf.put(button, (byte) (down ? 1 : 0));
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return ok;
    }

    private static void click(int key, int button) {
        Minecraft mc = mc();
        BlockPos pos;
        if (breakBlocks.enabled && button == 0 && mc.objectMouseOver != null && (pos = mc.objectMouseOver.getBlockPos()) != null) {
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block != Blocks.air && !(block instanceof BlockLiquid)) {
                breaking = true;
                KeyBinding.setKeyBindState(key, true);
                return;
            }
            if (breaking) {
                breaking = false;
                KeyBinding.setKeyBindState(key, false);
            }
        }
        if (jitter.isNotDefault()) {
            double d = jitter.getInput() * 0.45;
            mc.thePlayer.rotationYaw = MathUtils.RANDOM.nextBoolean() ? (float) ((double) mc.thePlayer.rotationYaw + (double) MathUtils.RANDOM.nextFloat() * d)
                    : (float) ((double) mc.thePlayer.rotationYaw - (double) MathUtils.RANDOM.nextFloat() * d);
            mc.thePlayer.rotationPitch = MathUtils.RANDOM.nextBoolean() ? (float) ((double) mc.thePlayer.rotationPitch + (double) MathUtils.RANDOM.nextFloat() * (d * 0.45))
                    : (float) ((double) mc.thePlayer.rotationPitch - (double) MathUtils.RANDOM.nextFloat() * (d * 0.45));
        }
        if (nextPress > 0L && nextRelease > 0L) {
            long now = System.currentTimeMillis();
            if (now > nextPress) {
                if (fakeMouse(button, true)) {
                    KeyBinding.setKeyBindState(key, true);
                    KeyBinding.onTick(key);
                    double d = blockHit.getInput();
                    if (button == 0 && d > 0.0 && Mouse.isButtonDown(1) && holdingSword()) {
                        blockHitAcc = (int) ((double) blockHitAcc + d);
                        if (blockHitAcc >= 100) {
                            blockHitAcc -= 100;
                            blockHitDown = true;
                            if (fakeMouse(1, true)) {
                                int use = mc.gameSettings.keyBindUseItem.getKeyCode();
                                KeyBinding.setKeyBindState(use, true);
                                KeyBinding.onTick(use);
                            }
                        }
                    }
                }
                schedule(false);
            } else if (nextRelease != 1L && now > nextRelease) {
                fakeMouse(button, false);
                KeyBinding.setKeyBindState(key, false);
                if (button == 0 && blockHitDown && holdingSword()) {
                    fakeMouse(1, false);
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                }
                nextRelease = 1L;
            }
        } else {
            schedule(false);
        }
    }

    private static boolean holdingSword() {
        return mc().thePlayer.getHeldItem() != null && mc().thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    @Override
    public void onDisable() {
        reset();
        inventoryClick = false;
        breaking = false;
    }

    private static int mode() {
        if (xyz.daikirai.module.combat.KillAuraModule.target != null || xyz.daikirai.module.player.ScaffoldModule.active
                || xyz.daikirai.module.player.AutoBlockinModule.active) {
            return -1;
        }
        if (AntiKnockbackModule.disableAttackWhileDelay.isToggled() && AntiKnockbackModule.delayUntil.get() != 0L) {
            return -1;
        }
        boolean weapon = PlayerUtils.holdingWeapon();
        if (!weapon && mc().thePlayer.isEating()) {
            return -1;
        }
        return weapon ? 1 : 0;
    }

    public static void reset() {
        nextRelease = 0L;
        nextPress = 0L;
        paused = false;
        blockHitDown = false;
        blockHitAcc = 100;
    }

    @Override
    public void onKnockback(net.minecraft.network.play.server.S12PacketEntityVelocity packet, long now) {
        Minecraft mc = mc();
        if (clickOnDamage.enabled && clickingLeft() && now - PacketUtils.lastVelocity > 250L && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (now < nextRelease) {
                nextRelease = 2L;
                nextPress = now + 20L;
            } else {
                nextPress = 2L;
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = mc();
        if (e.phase != TickEvent.Phase.START || !Chat.inWorld()) {
            return;
        }
        if (mc.currentScreen == null && mc.inGameHasFocus) {
            int n = mode();
            if (n == -1) {
                paused = true;
                return;
            }
            if (leftClick.enabled && Mouse.isButtonDown(0)) {
                if (n == 0) {
                    if (weaponOnly.enabled) {
                        paused = true;
                        return;
                    }
                } else if (!blockHit.isNotDefault() && PlayerUtils.isBlocking()) {
                    paused = true;
                    return;
                }
                if (paused) {
                    paused = false;
                    nextRelease = 2L;
                    nextPress = 2L;
                }
                click(mc.gameSettings.keyBindAttack.getKeyCode(), 0);
            } else if (rightClick.enabled && Mouse.isButtonDown(1)) {
                if (blocksOnly.enabled && !(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)) {
                    return;
                }
                click(mc.gameSettings.keyBindUseItem.getKeyCode(), 1);
            } else {
                reset();
            }
        } else if (inventoryCps.isNotDefault() && (mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiChest)) {
            if (Mouse.isButtonDown(0) && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54))) {
                if (nextRelease == 0L || nextPress == 0L) {
                    schedule(true);
                } else if (System.currentTimeMillis() > nextPress) {
                    schedule(true);
                    inventoryClick = true;
                }
            } else {
                reset();
            }
        }
    }

    @Override
    public void guiUpdate() {
        SliderSetting.order(minCps, maxCps);
    }

    private static void clickScreen(GuiScreen gui) {
        int x = Mouse.getX() * gui.width / mc().displayWidth;
        int y = gui.height - Mouse.getY() * gui.height / mc().displayHeight - 1;
        try {
            Method m = ReflectionHelper.findMethod(GuiScreen.class, gui, new String[]{"mouseClicked", "func_73864_a"}, int.class, int.class, int.class);
            m.invoke(gui, x, y, 0);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnable() {
        slowMin = MathUtils.randomBetween(1.09, 1.12);
        slowMax = MathUtils.randomBetween(1.22, 1.25);
        extraMin = randomInt(40, 50);
        extraMax = randomInt(85, 95);
    }
}
