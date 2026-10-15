package xyz.daikirai.module.player;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class DelayRemoverModule extends Module {
    public static ButtonSetting hitreg, jumpTicks;

    public DelayRemoverModule(int key) {
        super("Delay Remover", Category.player, key);
    }

    public void link() {
        hitreg = (ButtonSetting) ModuleManager.setting(this, "1.7 hitreg");
        jumpTicks = (ButtonSetting) ModuleManager.setting(this, "Remove jump ticks");
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (hitreg.enabled) {
            try {
                ReflectionHelper.setPrivateValue(Minecraft.class, mc, 0, "leftClickCounter", "field_71429_W");
            } catch (Exception ignored) {
            }
        }
        if (jumpTicks.enabled) {
            try {
                int n = ReflectionHelper.getPrivateValue(EntityLivingBase.class, mc.thePlayer, "jumpTicks", "field_70773_bE");
                if (n > 3) {
                    ReflectionHelper.setPrivateValue(EntityLivingBase.class, mc.thePlayer, 3, "jumpTicks", "field_70773_bE");
                }
            } catch (Exception ignored) {
            }
        }
    }
}
