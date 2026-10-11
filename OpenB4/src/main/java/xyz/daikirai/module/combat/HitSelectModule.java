package xyz.daikirai.module.combat;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;

public class HitSelectModule extends Module {
    public static HitSelectModule instance;
    public static SliderSetting pause, onlyInCombo;
    public static ButtonSetting disableOnLowHealth, fakeSwing, forceFirstHit, onlyCriticalHits, onlyWhileJumping, resetComboOnSwap,
            resetOnDamage, manual, autoClicker, killAura;
    private static int combo;
    private static int lastTarget;
    private static int hurtLimit;
    private static long lastAttack, damagedAt;
    private static final HashMap<Integer, Long> firstHits = new HashMap<>();

    public HitSelectModule(int key) {
        super("Hit Select", Category.combat, key);
        instance = this;
    }

    public void link() {
        pause = (SliderSetting) ModuleManager.setting(this, "Pause");
        onlyInCombo = (SliderSetting) ModuleManager.setting(this, "Only in combo");
        disableOnLowHealth = (ButtonSetting) ModuleManager.setting(this, "Disable on low health");
        fakeSwing = (ButtonSetting) ModuleManager.setting(this, "Fake swing");
        forceFirstHit = (ButtonSetting) ModuleManager.setting(this, "Force first hit");
        onlyCriticalHits = (ButtonSetting) ModuleManager.setting(this, "Only critical hits");
        onlyWhileJumping = (ButtonSetting) ModuleManager.setting(this, "Only while jumping");
        resetComboOnSwap = (ButtonSetting) ModuleManager.setting(this, "Reset combo on swap");
        resetOnDamage = (ButtonSetting) ModuleManager.setting(this, "Reset on damage");
        manual = (ButtonSetting) ModuleManager.setting(this, "Manual");
        autoClicker = (ButtonSetting) ModuleManager.setting(this, "AutoClicker");
        killAura = (ButtonSetting) ModuleManager.setting(this, "KillAura");
    }

    public static boolean on() {
        return instance != null && instance.isEnabled();
    }

    @Override
    public boolean onSendPacket(net.minecraft.network.Packet<?> packet) {
        if (packet instanceof net.minecraft.network.play.client.C09PacketHeldItemChange) {
            onHeldItemChange();
        } else if (packet instanceof C02PacketUseEntity) {
            onUseEntity((C02PacketUseEntity) packet);
        }
        return true;
    }

    public static void onUseEntity(C02PacketUseEntity p) {
        Entity e;
        if (!onlyInCombo.isAtMin() && p.getAction() == C02PacketUseEntity.Action.ATTACK && (e = p.getEntityFromWorld(Minecraft.getMinecraft().theWorld)) != null) {
            lastTarget = e.getEntityId();
            lastAttack = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.button != 0 || !e.buttonstate) {
            return;
        }
        if (!autoClicker.enabled && AutoClickerModule.clicking()) {
            return;
        }
        if (!manual.enabled) {
            return;
        }
        if (!Chat.inWorld() || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
            return;
        }
        if (hold(mc.objectMouseOver.entityHit)) {
            e.setCanceled(true);
        }
    }

    public static boolean holdForKillAura(Entity e) {
        return killAura.enabled && hold(e);
    }

    @Override
    public boolean onReceivePacket(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        if (!(packet instanceof S19PacketEntityStatus)) {
            return true;
        }
        S19PacketEntityStatus p = (S19PacketEntityStatus) packet;
        Entity e;
        if (!onlyInCombo.isAtMin() && p.getOpCode() == 2 && Math.abs(System.currentTimeMillis() - lastAttack) < 750L
                && (e = p.getEntity(Minecraft.getMinecraft().theWorld)) != null && e.getEntityId() == lastTarget) {
            ++combo;
        }
        return true;
    }

    public static boolean hold(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(on() && entity instanceof EntityLivingBase)) {
            return false;
        }
        if (disableOnLowHealth.enabled && !mc.thePlayer.capabilities.isCreativeMode && mc.thePlayer.getHealth() <= 4.0f) {
            return false;
        }
        if (!onlyInCombo.isAtMin() && combo < (int) onlyInCombo.getInput()) {
            return false;
        }
        if (onlyWhileJumping.enabled && !GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
            return false;
        }
        if (damagedAt != 0L && Math.abs(System.currentTimeMillis() - damagedAt) < 170L) {
            return false;
        }
        boolean hold = shouldHold((EntityLivingBase) entity);
        if (hold && forceFirstHit.enabled) {
            long now = System.currentTimeMillis();
            Long t = firstHits.get(entity.getEntityId());
            if (t == null || now - t > 10000L) {
                hold = false;
            }
            firstHits.put(entity.getEntityId(), now);
        }
        if (hold && fakeSwing.enabled) {
            PlayerUtils.restartSwing();
        }
        return hold;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == Minecraft.getMinecraft().thePlayer) {
            combo = 0;
            firstHits.clear();
        }
    }

    @Override
    public void onUpdate() {
        if (combo > 0 && Math.abs(System.currentTimeMillis() - lastAttack) > 1250L) {
            combo = 0;
        }
    }

    private static void onHeldItemChange() {
        if (resetComboOnSwap.enabled) {
            combo = 0;
        }
    }

    private static boolean shouldHold(EntityLivingBase e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!pause.isAtMin() && e.hurtTime > hurtLimit) {
            return true;
        }
        return !(!onlyCriticalHits.enabled || mc.thePlayer.onGround || mc.thePlayer.fallDistance != 0.0f || mc.thePlayer.isInWater()
                || mc.thePlayer.ridingEntity != null || mc.thePlayer.isPotionActive(Potion.blindness) || mc.thePlayer.isOnLadder());
    }

    @Override
    public void onDisable() {
        combo = 0;
        damagedAt = 0L;
        firstHits.clear();
    }

    @Override
    public void onKnockback(net.minecraft.network.play.server.S12PacketEntityVelocity packet, long now) {
        combo = 0;
        if (resetOnDamage.enabled) {
            if (KillAuraModule.target != null) {
                KillAuraModule.forceAttacks(2);
            } else {
                damagedAt = now;
            }
        }
    }

    @Override
    public void guiUpdate() {
        hurtLimit = 10 - (int) pause.getInput() / 50;
    }
}
