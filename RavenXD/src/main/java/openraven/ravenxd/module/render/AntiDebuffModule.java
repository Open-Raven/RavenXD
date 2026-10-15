package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Strings;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StatCollector;

public class AntiDebuffModule extends Module {
    public static ButtonSetting alertInChat, removeBlindness, removeNausea;

    public AntiDebuffModule(int key) {
        super("AntiDebuff", Category.render, key);
    }

    public void link() {
        alertInChat = (ButtonSetting) ModuleManager.setting(this, "Alert in chat");
        removeBlindness = (ButtonSetting) ModuleManager.setting(this, "Remove blindness");
        removeNausea = (ButtonSetting) ModuleManager.setting(this, "Remove nausea");
    }

    private void removeEffects() {
        PotionEffect e;
        if (!Chat.inWorld()) {
            return;
        }
        if (removeBlindness.isToggled() && (e = mc.thePlayer.getActivePotionEffect(Potion.blindness)) != null) {
            mc.thePlayer.removePotionEffectClient(Potion.blindness.id);
            if (alertInChat.isToggled()) {
                alert(e.getPotionID(), e.getAmplifier(), e.getDuration());
            }
        }
        if (removeNausea.isToggled() && (e = mc.thePlayer.getActivePotionEffect(Potion.confusion)) != null) {
            mc.thePlayer.removePotionEffectClient(Potion.confusion.id);
            if (alertInChat.isToggled()) {
                alert(e.getPotionID(), e.getAmplifier(), e.getDuration());
            }
        }
    }

    private static void alert(int id, int amplifier, int duration) {
        String name = StatCollector.translateToLocal(Potion.potionTypes[id].getName());
        Chat.print("&b" + ModuleManager.get("AntiDebuff").getName() + "&7: &r" + "&7Removing &d" + name + " " + (amplifier + 1)
                + " &7for &b" + SliderSetting.formatNumber(Strings.round((double) duration / 20.0, 1)) + "&7s.");
    }

    @Override
    public void onEnable() {
        this.removeEffects();
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b.isToggled() && (b == removeBlindness || b == removeNausea)) {
            this.removeEffects();
        }
    }

    public static boolean onEffect(S1DPacketEntityEffect p) {
        boolean drop = false;
        byte id = p.getEffectId();
        if (id == Potion.blindness.id) {
            if (removeBlindness.isToggled()) {
                drop = true;
            }
        } else if (id == Potion.confusion.id && removeNausea.isToggled()) {
            drop = true;
        }
        if (drop) {
            if (alertInChat.isToggled()) {
                alert(id, p.getAmplifier(), p.getDuration());
            }
            return true;
        }
        return false;
    }
}
