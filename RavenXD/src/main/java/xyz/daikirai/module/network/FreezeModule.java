package xyz.daikirai.module.network;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.hook.Hooks;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.combat.AntiKnockbackModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.potion.Potion;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class FreezeModule extends Module {
    public static FreezeModule instance;
    public static SliderSetting autoDisable;
    public static ButtonSetting activateOnHit, disableOnBedBreak, disableOnInteract, showFreezeTicks, showKnockbackDirection, updateChatMessages,
            updateBlockChanges, updatePlayerHealth, updatePlayerPositions, updateScoreboard, updateSoundEffects;

    private static final List<Packet<?>> held = new ArrayList<Packet<?>>();
    private static final Object LOCK = new Object();
    public static volatile boolean frozen;
    private static volatile boolean shown;
    private static long shownAt, hiddenAt;
    private static volatile boolean disableNext;
    private static volatile Packet<?> processing;
    private static int ticks;
    private static String text;
    private static float halfWidth;
    private static boolean knockback;
    private static double knockbackX, knockbackZ;

    public FreezeModule(int key) {
        super("Freeze", Category.network, key);
        instance = this;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        autoDisable = (SliderSetting) ModuleManager.setting(this, "Auto-disable");
        activateOnHit = (ButtonSetting) ModuleManager.setting(this, "Activate on hit");
        disableOnBedBreak = (ButtonSetting) ModuleManager.setting(this, "Disable on bed break");
        disableOnInteract = (ButtonSetting) ModuleManager.setting(this, "Disable on interact");
        showFreezeTicks = (ButtonSetting) ModuleManager.setting(this, "Show freeze ticks");
        showKnockbackDirection = (ButtonSetting) ModuleManager.setting(this, "Show knockback direction");
        updateChatMessages = (ButtonSetting) ModuleManager.setting(this, "Update chat messages");
        updateBlockChanges = (ButtonSetting) ModuleManager.setting(this, "Update block changes");
        updatePlayerHealth = (ButtonSetting) ModuleManager.setting(this, "Update player health");
        updatePlayerPositions = (ButtonSetting) ModuleManager.setting(this, "Update player positions");
        updateScoreboard = (ButtonSetting) ModuleManager.setting(this, "Update scoreboard");
        updateSoundEffects = (ButtonSetting) ModuleManager.setting(this, "Update sound effects");
    }

    private static void clearKnockback() {
        knockback = false;
        knockbackZ = 0.0;
        knockbackX = 0.0;
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            if (frozen && disableOnInteract.isToggled()) {
                disableNext = true;
            }
        } else if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging p = (C07PacketPlayerDigging) packet;
            if (disableOnBedBreak.isToggled() && p.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK && BlockUtils.is(p.getPosition(), Blocks.bed)) {
                disableNext = true;
            }
        }
        return true;
    }

    @Override
    public void onUpdate() {
        if (frozen) {
            if (disableNext) {
                disableNext = false;
                this.disable();
                return;
            }
            if (autoDisable.isNotDefault() && (double) ticks >= autoDisable.getInput() * 20.0) {
                this.disable();
                return;
            }
            ++ticks;
            text = String.valueOf(autoDisable.isNotDefault() ? (int) (autoDisable.getInput() * 20.0) - ticks + 1 : ticks);
        } else {
            text = "idle";
        }
        halfWidth = (float) mc().fontRendererObj.getStringWidth(text) / 2.0f - 0.5f;
    }

    public static void render() {
        Minecraft mc = mc();
        if (instance == null) {
            return;
        }
        if (showKnockbackDirection.isToggled() && knockback) {
            renderArrow();
        }
        if (!showFreezeTicks.isToggled() || text == null || xyz.daikirai.module.network.BlinkModule.text != null) {
            return;
        }
        long now = System.currentTimeMillis();
        int a;
        long age;
        if (shown) {
            age = now - shownAt;
            if (age < 125L) {
                a = (int) ((float) age / (float) 125 * (float) 240);
            } else {
                a = 240;
            }
        } else {
            age = now - hiddenAt;
            if (age < 125L) {
                a = (int) ((float) 240 - (float) age / (float) 125 * (float) 240);
            } else {
                a = 0;
                text = null;
            }
        }
        if (a > 0) {
            float x = (float) RenderUtils.scaledWidth / 2.0f - halfWidth;
            float y = (float) RenderUtils.scaledHeight / 2.0f + 9.0f;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(text, x, y, RenderUtils.withAlpha(Theme.defaultTheme.getRGB(0.0), RenderUtils.clampAlpha(a)), true);
            GlStateManager.disableBlend();
        }
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        Minecraft mc = mc();
        if (packet == processing || packet == PacketUtils.reprocessing) {
            return true;
        }
        if (!(handler instanceof INetHandlerPlayClient) || !Chat.inWorld() || mc.isSingleplayer() || packet instanceof S40PacketDisconnect) {
            synchronized (LOCK) {
                held.clear();
                frozen = false;
            }
            instance.disable();
            return true;
        }
        if (!frozen) {
            boolean hit = false;
            if (activateOnHit.isToggled()) {
                if (packet instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity p = (S12PacketEntityVelocity) packet;
                    if (p.getEntityID() == mc.thePlayer.getEntityId() && p.getMotionY() != -627) {
                        hit = true;
                    }
                } else if (packet instanceof S1DPacketEntityEffect) {
                    S1DPacketEntityEffect p = (S1DPacketEntityEffect) packet;
                    if (p.getEntityId() == mc.thePlayer.getEntityId() && p.getEffectId() == Potion.moveSlowdown.id && p.getDuration() == 20 && Hypixel.bedwars(2)) {
                        hit = true;
                    }
                }
                if (hit) {
                    setFrozen(true);
                }
            }
            if (!hit) {
                return true;
            }
        }
        S12PacketEntityVelocity v;
        if (packet instanceof S12PacketEntityVelocity && (v = (S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
            if (Math.abs(v.getMotionX()) < 400 && Math.abs(v.getMotionZ()) < 400) {
                clearKnockback();
            } else {
                knockbackX = (double) v.getMotionX() / 8000.0;
                knockbackZ = (double) v.getMotionZ() / 8000.0;
                knockback = true;
            }
        }
        if (updateChatMessages.isToggled() && packet instanceof S02PacketChat) {
            return true;
        }
        if (updateBlockChanges.isToggled() && (packet instanceof S22PacketMultiBlockChange || packet instanceof S23PacketBlockChange)) {
            return true;
        }
        if (updatePlayerHealth.isToggled() && packet instanceof S06PacketUpdateHealth) {
            return true;
        }
        if (updatePlayerPositions.isToggled() && packet instanceof S14PacketEntity) {
            return true;
        }
        if (updateScoreboard.isToggled() && (packet instanceof S3BPacketScoreboardObjective || packet instanceof S3CPacketUpdateScore
                || packet instanceof S3DPacketDisplayScoreboard || packet instanceof S3EPacketTeams)) {
            return true;
        }
        if (updateSoundEffects.isToggled() && packet instanceof S29PacketSoundEffect) {
            return true;
        }
        synchronized (LOCK) {
            if (!frozen) {
                return true;
            }
            held.add(packet);
        }
        return false;
    }

    private static void release() {
        ArrayList<Packet<?>> list = null;
        synchronized (LOCK) {
            if (!held.isEmpty()) {
                list = new ArrayList<Packet<?>>(held);
                held.clear();
            }
        }
        if (list != null) {
            for (Packet<?> packet : list) {
                try {
                    PacketUtils.reprocessing = processing = packet;
                    Hooks.onReceive(packet, mc().getNetHandler().getNetworkManager().getNetHandler());
                } catch (Throwable throwable) {
                    Chat.print(MathUtils.timeDigits() + " &cfreeze e1 " + packet.getClass().getSimpleName() + " " + throwable.getClass().getSimpleName());
                } finally {
                    processing = null;
                    PacketUtils.reprocessing = null;
                }
            }
        }
    }

    private static void renderArrow() {
        Minecraft mc = mc();
        double d = Math.atan2(knockbackZ, knockbackX) - Math.toRadians(mc.thePlayer.rotationYaw + 90.0f);
        float sin = (float) Math.sin(d);
        float cos = (float) (-Math.cos(d));
        float cx = (float) RenderUtils.scaledWidth / 2.0f;
        float cy = (float) RenderUtils.scaledHeight / 2.0f - 14.0f;
        float len = 7.0f;
        float head = 3.0f;
        float x1 = cx - sin * 2.5f;
        float y1 = cy - cos * 2.5f;
        float x2 = cx + sin * len;
        float y2 = cy + cos * len;
        float x3 = x2 - sin * head - cos * (head * 0.65f);
        float y3 = y2 - cos * head + sin * (head * 0.65f);
        float x4 = x2 - sin * head + cos * (head * 0.65f);
        float y4 = y2 - cos * head - sin * (head * 0.65f);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GL11.glEnable(2848);
        GL11.glLineWidth(3.5f);
        Render3D.color(-1342177280);
        GL11.glBegin(1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x3, y3);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x4, y4);
        GL11.glEnd();
        GL11.glLineWidth(1.75f);
        Render3D.color(Theme.defaultTheme.getRGB(0.0));
        GL11.glBegin(1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x3, y3);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x4, y4);
        GL11.glEnd();
        float s = 1.75f;
        GL11.glBegin(7);
        GL11.glVertex2f(cx - s, cy - s);
        GL11.glVertex2f(cx + s, cy - s);
        GL11.glVertex2f(cx + s, cy + s);
        GL11.glVertex2f(cx - s, cy + s);
        GL11.glEnd();
        GL11.glDisable(2848);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean canBeEnabled() {
        if (frozen) {
            return false;
        }
        if (mc().isSingleplayer()) {
            Chat.moduleMessage(this, "&cCan't enable in singleplayer.");
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        release();
        clearKnockback();
        setShown(false);
    }

    private static void setShown(boolean b) {
        if (b) {
            if (!shown) {
                shown = true;
                shownAt = System.currentTimeMillis();
                if (!activateOnHit.isToggled()) {
                    setFrozen(true);
                }
            }
        } else if (shown) {
            shown = false;
            hiddenAt = System.currentTimeMillis();
            setFrozen(false);
        }
    }

    private static void setFrozen(boolean b) {
        if (b) {
            if (!frozen) {
                if (AntiKnockbackModule.delayUntil.get() != 0L) {
                    AntiKnockbackModule.reset();
                }
                if (BacktrackModule.holding) {
                    BacktrackModule.flush();
                }
                synchronized (LOCK) {
                    frozen = true;
                    ticks = 0;
                }
            }
        } else if (frozen) {
            synchronized (LOCK) {
                frozen = false;
            }
        }
    }

    @Override
    public String getInfo() {
        return ticks > 0 ? String.valueOf(ticks) : "Idle";
    }

    @Override
    public void onEnable() {
        ticks = 0;
        setShown(true);
    }
}
