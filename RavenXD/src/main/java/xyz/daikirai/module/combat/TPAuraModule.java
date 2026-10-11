package xyz.daikirai.module.combat;

import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.module.world.AntiBotModule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TPAuraModule extends Module {
    public static SliderSetting range;
    public static ButtonSetting weaponOnly;
    private double offX, offY, offZ;

    public TPAuraModule(int key) {
        super("TPAura", Category.combat, key);
    }

    public void link() {
        range = (SliderSetting) ModuleManager.setting(this, "Range");
        weaponOnly = (ButtonSetting) ModuleManager.setting(this, "Weapon only");
    }

    @Override
    public void onEnable() {
        if (range.getInput() == 0.0) {
            Chat.print("&cTPAura range values are set to 0.");
            this.disable();
            return;
        }
        this.newOffset();
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (Chat.inWorld() && mc.thePlayer.maxHurtTime > 0 && mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
            this.newOffset();
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (weaponOnly.enabled && !PlayerUtils.holdingWeapon()) {
            return;
        }
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p.deathTime != 0 || (double) mc.thePlayer.getDistanceToEntity(p) > range.getInput()) continue;
            if (AntiBotModule.ignored(p) || BotCheck.isBot(p)) continue;
            mc.thePlayer.setPosition(p.posX + this.offX, p.posY + this.offY, p.posZ + this.offZ);
            PlayerUtils.attack(p, false);
            break;
        }
    }

    private void newOffset() {
        this.offX = (double) randomInt(-15, 15) / 10.0;
        this.offY = (double) randomInt(10, 15) / 10.0;
        this.offZ = (double) randomInt(-15, 15) / 10.0;
    }

    private static int randomInt(int a, int b) {
        return MathUtils.RANDOM.nextInt(b - a + 1) + a;
    }
}
