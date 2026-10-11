package xyz.daikirai.module.network;

import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlinkModule extends Module {
    public static BlinkModule instance;
    public static SliderSetting autoDisable;
    public static ButtonSetting cancelKnockback, disableOnAttack, disableOnInventory, disableOnMine, disableOnPlace, showBlinkTicks,
            showInitialPosition;
    public static boolean blinking;
    private static int ticks;
    public static String text;
    private static float halfWidth;
    private static long startedAt, stoppedAt;
    public static EntityOtherPlayerMP ghost;

    public BlinkModule(int key) {
        super("Blink", Category.network, key);
        instance = this;
    }

    public void link() {
        autoDisable = (SliderSetting) ModuleManager.setting(this, "Auto-disable");
        cancelKnockback = (ButtonSetting) ModuleManager.setting(this, "Cancel knockback");
        disableOnAttack = (ButtonSetting) ModuleManager.setting(this, "Disable on attack");
        disableOnInventory = (ButtonSetting) ModuleManager.setting(this, "Disable on inventory");
        disableOnMine = (ButtonSetting) ModuleManager.setting(this, "Disable on mine");
        disableOnPlace = (ButtonSetting) ModuleManager.setting(this, "Disable on place");
        showBlinkTicks = (ButtonSetting) ModuleManager.setting(this, "Show blink ticks");
        showInitialPosition = (ButtonSetting) ModuleManager.setting(this, "Show initial position");
    }

    @Override
    public void onPacketHeld(Packet<?> p) {
        if (p instanceof C02PacketUseEntity) {
            if (disableOnAttack.enabled && ((C02PacketUseEntity) p).getAction() == C02PacketUseEntity.Action.ATTACK) {
                instance.disable();
            }
        } else if (p instanceof C0EPacketClickWindow) {
            if (disableOnInventory.enabled) {
                instance.disable();
            }
        } else if (p instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging d;
            if (disableOnMine.enabled && ((d = (C07PacketPlayerDigging) p).getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
                    || d.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                instance.disable();
            }
        } else if (p instanceof C08PacketPlayerBlockPlacement && disableOnPlace.enabled) {
            C08PacketPlayerBlockPlacement c = (C08PacketPlayerBlockPlacement) p;
            if (c.getStack() != null && c.getStack().getItem() instanceof ItemBlock && c.getPlacedBlockDirection() != 255) {
                instance.disable();
            }
        }
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        setBlinking(true);
        if (showInitialPosition.enabled) {
            ghost = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            ghost.setEntityId(-170);
            ghost.copyLocationAndAnglesFrom(mc.thePlayer);
        }
    }

    @Override
    public void onPacketQueueTick() {
        if (autoDisable.isNotDefault() && (double) ticks >= autoDisable.getInput() * 20.0) {
            instance.disable();
            return;
        }
        text = String.valueOf(autoDisable.isNotDefault() ? (int) (autoDisable.getInput() * 20.0) - ticks + 1 : ++ticks);
        halfWidth = (float) Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) / 2.0f - 0.5f;
    }

    @Override
    public boolean isBlinking() {
        return blinking;
    }

    @Override
    public String getInfo() {
        return String.valueOf(ticks);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (ghost == null || !Chat.inWorld()) {
            return;
        }
        if (SettingsModule.hideFirstPersonSelfEsp.enabled && mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        Render3D.entityBox(ghost, 2, Theme.defaultTheme.getRGB(0.0), e.partialTicks);
    }

    @Override
    public void onDisable() {
        setBlinking(false);
        ghost = null;
    }

    @Override
    public boolean canBeEnabled() {
        if (blinking) {
            return false;
        }
        if (Minecraft.getMinecraft().isSingleplayer()) {
            Chat.moduleMessage(this, "&cCan't enable in singleplayer.");
            return false;
        }
        return true;
    }

    public static void setBlinking(boolean b) {
        if (b) {
            if (!blinking) {
                blinking = true;
                startedAt = System.currentTimeMillis();
                ticks = 0;
                PacketQueue.open();
            }
        } else if (blinking) {
            blinking = false;
            stoppedAt = System.currentTimeMillis();
            PacketQueue.close();
        }
    }

    public static void renderTicks() {
        Minecraft mc = Minecraft.getMinecraft();
        int alpha;
        long l;
        if (showBlinkTicks == null || !showBlinkTicks.enabled || text == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (blinking) {
            l = now - startedAt;
            alpha = l < 125L ? (int) ((float) l / 125.0f * 240.0f) : 240;
        } else {
            l = now - stoppedAt;
            if (l < 125L) {
                alpha = (int) (240.0f - (float) l / 125.0f * 240.0f);
            } else {
                alpha = 0;
                text = null;
            }
        }
        if (alpha > 0) {
            float x = (float) RenderUtils.scaledWidth / 2.0f - halfWidth;
            float y = (float) RenderUtils.scaledHeight / 2.0f + 9.0f;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(text, x, y, RenderUtils.withAlpha(Theme.defaultTheme.getRGB(0.0), RenderUtils.clampAlpha(alpha)), true);
            GlStateManager.disableBlend();
        }
    }
}
