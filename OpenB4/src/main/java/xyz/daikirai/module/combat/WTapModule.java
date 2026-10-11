package xyz.daikirai.module.combat;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WTapModule extends Module {
    public static WTapModule instance;
    public static SliderSetting chance, delayBetweenReset, delayUntilReset;
    public static ButtonSetting disableWhileChasing, playersOnly, silent;
    private static boolean pending;
    public static boolean reset;
    private static long hitTime, lastReset;

    public WTapModule(int key) {
        super("WTap", Category.combat, key);
        instance = this;
    }

    public void link() {
        chance = (SliderSetting) ModuleManager.setting(this, "Chance");
        delayBetweenReset = (SliderSetting) ModuleManager.setting(this, "Delay between reset");
        delayUntilReset = (SliderSetting) ModuleManager.setting(this, "Delay until reset");
        disableWhileChasing = (ButtonSetting) ModuleManager.setting(this, "Disable while chasing");
        playersOnly = (ButtonSetting) ModuleManager.setting(this, "Players only");
        silent = (ButtonSetting) ModuleManager.setting(this, "Silent");
    }

    @Override
    public void onPreUpdate() {
        if (pending) {
            long now = System.currentTimeMillis();
            if ((double) (now - hitTime) < delayUntilReset.getInput() - 20.0) {
                return;
            }
            pending = false;
            lastReset = now;
            reset = true;
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (pending) {
            return;
        }
        if (JumpResetModule.pending) {
            return;
        }
        if (!Chat.inWorld() || e.entityPlayer != mc.thePlayer || !mc.thePlayer.isSprinting()) {
            return;
        }
        if (playersOnly.enabled) {
            if (!(e.target instanceof EntityPlayer)) {
                return;
            }
            if ((double) ((EntityPlayer) e.target).hurtTime > SettingsModule.hittableHurtTime.getInput()) {
                return;
            }
        } else if (!(e.target instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase target = (EntityLivingBase) e.target;
        if (target.deathTime != 0) {
            return;
        }
        if (disableWhileChasing.enabled && PlayerUtils.awayFromPlayer(target)) {
            return;
        }
        if (chance.getInput() != 100.0 && !(Math.random() < chance.getInput() / 100.0)) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((double) (now - lastReset) < delayBetweenReset.getInput() - 20.0) {
            return;
        }
        hitTime = now;
        pending = true;
    }

    @Override
    public void onDisable() {
        pending = false;
        reset = false;
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        if (silent.enabled && reset) {
            reset = false;
            e.sprinting = false;
        }
    }

    @Override
    public boolean onSendPacket(net.minecraft.network.Packet<?> packet) {
        if (packet instanceof C0BPacketEntityAction && ((C0BPacketEntityAction) packet).getAction() == C0BPacketEntityAction.Action.START_SPRINTING) {
            lastReset = System.currentTimeMillis() - 50L;
        }
        return true;
    }
}
