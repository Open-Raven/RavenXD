package xyz.daikirai.module.combat;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.KeyUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

public class AutoBlockModule extends Module {
    public static SliderSetting maxHurtTime, maxHoldDuration, randomHoldDuration, cooldown, range, lagChance, lagDuration;
    public static ButtonSetting disableBlockOutOfRange, disableWhileChasing, forceBlockAnimation, ignoreMobs, ignoreTeammates,
            onlyWhileLeftClicking, onlyWhileRightClicking, onlyWhileTargeting, flushOnAttack, flushOnInteractItem;

    public static boolean fakeBlocking;
    private static int holdTicks, cooldownTicks;
    private static boolean hadSword;
    private static boolean lagging;
    private static long lagUntil;
    private static boolean startLag, flush;
    private static int lagAccumulator;
    private static double rangeValue, rangeSq;

    public AutoBlockModule(int key) {
        super("AutoBlock", Category.combat, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        maxHurtTime = (SliderSetting) ModuleManager.setting(this, "Max hurt time");
        maxHoldDuration = (SliderSetting) ModuleManager.setting(this, "Max hold duration");
        randomHoldDuration = (SliderSetting) ModuleManager.setting(this, "Random hold duration");
        cooldown = (SliderSetting) ModuleManager.setting(this, "Cooldown");
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        disableBlockOutOfRange = (ButtonSetting) ModuleManager.setting(this, "Disable block out of range");
        disableWhileChasing = (ButtonSetting) ModuleManager.setting(this, "Disable while chasing");
        forceBlockAnimation = (ButtonSetting) ModuleManager.setting(this, "Force block animation");
        ignoreMobs = (ButtonSetting) ModuleManager.setting(this, "Ignore mobs");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        onlyWhileLeftClicking = (ButtonSetting) ModuleManager.setting(this, "Only while left clicking");
        onlyWhileRightClicking = (ButtonSetting) ModuleManager.setting(this, "Only while right clicking");
        onlyWhileTargeting = (ButtonSetting) ModuleManager.setting(this, "Only while targeting");
        lagChance = (SliderSetting) ModuleManager.setting(this, "Lag chance");
        lagDuration = (SliderSetting) ModuleManager.setting(this, "Lag duration");
        flushOnAttack = (ButtonSetting) ModuleManager.setting(this, "Flush on attack");
        flushOnInteractItem = (ButtonSetting) ModuleManager.setting(this, "Flush on interact item");
    }

    @Override
    public boolean isFakeBlocking() {
        return fakeBlocking;
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (lagging) {
            if (packet instanceof C02PacketUseEntity) {
                if (flushOnAttack.isToggled() && ((C02PacketUseEntity) packet).getAction() == C02PacketUseEntity.Action.ATTACK) {
                    flush = true;
                }
            } else if (packet instanceof C08PacketPlayerBlockPlacement && flushOnInteractItem.isToggled()
                    && !ItemUtils.isSword(((C08PacketPlayerBlockPlacement) packet).getStack())) {
                flush = true;
            }
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
            if (p.getPlacedBlockDirection() == 255 && ItemUtils.isSword(p.getStack()) && rollLag()) {
                startLag = true;
            }
        }
        return true;
    }

    private static boolean hurtTimeOk() {
        int n = (int) Math.ceil((double) (int) maxHurtTime.getInput() / 50.0);
        return mc().thePlayer.hurtTime <= n;
    }

    @Override
    public void guiUpdate() {
        rangeValue = range.getInput();
        double d = rangeValue + 1.2;
        rangeSq = d * d;
    }

    @Override
    public void onPostMotion() {
        Minecraft mc = mc();
        if (flush) {
            flush = false;
            if (lagging) {
                stopLag();
            }
        }
        if (startLag) {
            startLag = false;
            if (!lagging) {
                lagging = true;
                PacketQueue.open();
            }
            lagUntil = System.currentTimeMillis() + (long) (int) lagDuration.getInput();
        }
        boolean right = Mouse.isButtonDown(1);
        boolean sword = PlayerUtils.holdingSword();
        boolean manual = right && !onlyWhileRightClicking.isToggled();
        boolean keepHeld = right && (!onlyWhileRightClicking.isToggled() || !sword);
        boolean block;
        if (!forceBlockAnimation.isToggled()) {
            block = canBlock() && hurtTimeOk() && conditions();
            fakeBlocking = false;
        } else {
            fakeBlocking = canBlock() && conditions();
            block = fakeBlocking && hurtTimeOk();
        }
        if (cooldownTicks > 0) {
            --cooldownTicks;
        }
        if (holdTicks > 0) {
            --holdTicks;
            if (!block || holdTicks == 0) {
                holdTicks = 0;
                if (!keepHeld) {
                    release();
                }
            }
        } else if (block && !manual && cooldownTicks == 0) {
            int n = (int) Math.ceil((double) (int) maxHoldDuration.getInput() / 50.0);
            int n2 = 0;
            if (!randomHoldDuration.isAtMin()) {
                n2 = (int) Math.ceil((double) (int) randomHoldDuration.getInput() / 50.0);
                n2 = Math.min(MathUtils.RANDOM.nextInt(n2 + 1), MathUtils.RANDOM.nextInt(n2 + 1));
            }
            holdTicks = n + n2;
            KeyUtils.press(mc.gameSettings.keyBindUseItem);
        }
        if (onlyWhileRightClicking.isToggled() && right && !sword && hadSword) {
            KeyUtils.press(mc.gameSettings.keyBindUseItem);
        }
        hadSword = sword;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (!lagging || e.phase == TickEvent.Phase.START) {
            return;
        }
        if (System.currentTimeMillis() >= lagUntil) {
            stopLag();
        }
    }

    @Override
    public void onDisable() {
        hadSword = false;
        flush = false;
        startLag = false;
        fakeBlocking = false;
        if (lagging) {
            stopLag();
        }
        if (holdTicks > 0) {
            holdTicks = 0;
            KeyUtils.release(mc().gameSettings.keyBindUseItem);
        }
        cooldownTicks = 0;
    }

    private static boolean conditions() {
        if (onlyWhileTargeting.isToggled() && !PlayerUtils.targetMatches(ignoreMobs.isToggled() ? 0 : 1)) {
            return false;
        }
        if (onlyWhileLeftClicking.isToggled() && !PlayerUtils.isClicking()) {
            return false;
        }
        if (onlyWhileRightClicking.isToggled() && !Mouse.isButtonDown(1)) {
            return false;
        }
        return enemyInRange();
    }

    @Override
    public void onKnockback(S12PacketEntityVelocity packet, long sinceLast) {
        if (holdTicks > 0) {
            holdTicks = 0;
            release();
        }
    }

    private static boolean canBlock() {
        return mc().currentScreen == null && (KillAuraModule.target == null || !KillAuraModule.autoBlock.packet) && PlayerUtils.holdingSword();
    }

    private static boolean enemyInRange() {
        Minecraft mc = mc();
        if (range.getInput() == range.getMax()) {
            return true;
        }
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase) entity;
            if (e == mc.thePlayer || e.deathTime != 0) continue;
            if (xyz.daikirai.module.world.AntiBotModule.ignored(e)) continue;
            if (mc.thePlayer.getDistanceSqToEntity(e) > rangeSq) continue;
            if (e instanceof EntityPlayer) {
                if (PlayerUtils.isFriend((EntityPlayer) e)) continue;
                if (BotCheck.isBot(e)) continue;
            } else if (ignoreMobs.isToggled() || !xyz.daikirai.module.render.DamageTagsModule.isMob(e)) continue;
            if (disableWhileChasing.isToggled() && PlayerUtils.awayFromPlayer(e)) continue;
            if (!PlayerUtils.rayHits(e, rangeValue)) continue;
            if (ignoreTeammates.isToggled() && Hypixel.sameTeam(e)) continue;
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button != 1 || !e.buttonstate || !Chat.inWorld()) {
            return;
        }
        if (disableBlockOutOfRange.isToggled() && canBlock() && !PlayerUtils.lookingAtInteractable() && !PlayerUtils.lookingAtBot()) {
            e.setCanceled(true);
        }
    }

    private static void stopLag() {
        flush = false;
        lagging = false;
        PacketQueue.close();
    }

    private static void release() {
        KeyUtils.release(mc().gameSettings.keyBindUseItem);
        if (!cooldown.isAtMin()) {
            cooldownTicks = (int) Math.ceil((double) (int) cooldown.getInput() / 50.0);
        }
    }

    private static boolean rollLag() {
        if (lagChance.isAtMin() || mc().isSingleplayer()) {
            return false;
        }
        if ((lagAccumulator += (int) lagChance.getInput()) >= 100) {
            lagAccumulator -= 100;
            return true;
        }
        return false;
    }
}
