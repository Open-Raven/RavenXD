package openraven.ravenxd.module.combat;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.mixin.IMinecraft;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.ClientActionTimer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// Adapted from KClient's rod macro.
public class RodMacroModule extends Module {
    public static RodMacroModule instance;
    public static SliderSetting minLaunch, maxLaunch, minSwitchback, maxSwitchback;
    public static ButtonSetting showFailMessage, infiniteRod;

    private final ClientActionTimer timer = new ClientActionTimer();
    private EntityPlayerSP player;
    private WorldClient world;
    private boolean processing, waitingForBobber, sawBobber;
    private int originalSlot = -1, rodSlot = -1;
    private long castAt;

    public RodMacroModule(int key) {
        super("Rod Macro", Category.combat, key);
        instance = this;
        // Keep ticking after the one-shot module disables itself.
        MinecraftForge.EVENT_BUS.register(new SequenceWatcher());
    }

    public void link() {
        minLaunch = (SliderSetting) ModuleManager.setting(this, "Min launch delay");
        maxLaunch = (SliderSetting) ModuleManager.setting(this, "Max launch delay");
        minSwitchback = (SliderSetting) ModuleManager.setting(this, "Min switchback delay");
        maxSwitchback = (SliderSetting) ModuleManager.setting(this, "Max switchback delay");
        showFailMessage = (ButtonSetting) ModuleManager.setting(this, "Show fail message");
        infiniteRod = (ButtonSetting) ModuleManager.setting(this, "Infinite rod");
    }

    private static long now() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private static long delay(SliderSetting min, SliderSetting max) {
        double a = min.getInput(), b = max.getInput();
        return (long) (a >= b ? a : ThreadLocalRandom.current().nextDouble(a, b));
    }

    private boolean sameSession() {
        return this.player != null && mc.thePlayer == this.player && mc.theWorld == this.world;
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead
                || mc.currentScreen != null || !mc.inGameHasFocus) {
            this.cancelPending();
            this.disable();
            return;
        }
        if (this.processing && !this.sameSession()) this.reset();
        if (this.processing && !this.holdingRod()) this.reset();
        int rod = this.findRod();
        if (rod == -1) {
            this.cancelPending();
            if (showFailMessage.isToggled()) Chat.moduleMessage(this, "&cNo rod found in hotbar!");
            this.disable();
            return;
        }
        this.timer.cancel();
        if (!this.processing) this.originalSlot = mc.thePlayer.inventory.currentItem;
        this.player = mc.thePlayer;
        this.world = mc.theWorld;
        this.rodSlot = rod;
        this.processing = true;
        this.waitingForBobber = false;
        this.sawBobber = false;
        this.selectSlot(rod);
        if (this.player.fishEntity != null) this.useRod();
        this.timer.schedule(now(), delay(minLaunch, maxLaunch), this::cast);
        this.disable();
    }

    private void selectSlot(int slot) {
        this.player.inventory.currentItem = slot;
        mc.playerController.updateController();
    }

    private boolean holdingRod() {
        ItemStack stack = this.player.getHeldItem();
        return this.player.inventory.currentItem == this.rodSlot && stack != null
                && stack.getItem() instanceof ItemFishingRod;
    }

    private void useRod() {
        ((IMinecraft) mc).invokeRightClickMouse();
    }

    private void cast() {
        if (!this.holdingRod()) {
            this.cancelPending();
            return;
        }
        this.useRod();
        if (infiniteRod.isToggled()) {
            this.waitingForBobber = true;
            this.castAt = now();
            this.sawBobber = this.player.fishEntity != null;
        } else {
            this.timer.schedule(now(), delay(minSwitchback, maxSwitchback), () -> {
                if (this.holdingRod()) this.selectSlot(this.originalSlot);
                this.reset();
            });
        }
    }

    public void cancelPending() {
        if (this.sameSession() && this.holdingRod()) this.selectSlot(this.originalSlot);
        this.reset();
    }

    private void reset() {
        this.timer.cancel();
        this.processing = this.waitingForBobber = this.sawBobber = false;
        this.originalSlot = this.rodSlot = -1;
        this.player = null;
        this.world = null;
    }

    private int findRod() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemFishingRod) return i;
        }
        return -1;
    }

    private int findSword() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = this.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSword) return i;
        }
        return this.originalSlot;
    }

    // Forge's generated handler requires a public subscriber.
    public final class SequenceWatcher {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END || !processing) return;
            if (!sameSession() || player.isDead || mc.currentScreen != null || !mc.inGameHasFocus) {
                cancelPending();
                return;
            }
            if (!holdingRod()) {
                reset();
                return;
            }
            timer.tick(now());
            if (!waitingForBobber) return;
            if (player.fishEntity != null) {
                sawBobber = true;
            } else if (sawBobber) {
                selectSlot(findSword());
                reset();
            } else if (now() - castAt >= 1500L) {
                cancelPending();
            }
        }
    }
}
