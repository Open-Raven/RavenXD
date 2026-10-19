package openraven.ravenxd.module.render;

import openraven.ravenxd.client.BotCheck;
import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.Render3D;
import openraven.ravenxd.util.RenderUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class TracersModule extends Module {
    public static SliderSetting lineWidth, red, green, blue;
    public static ButtonSetting showInvis, rainbow;
    private int color = 0;
    private boolean viewBobbing;

    public TracersModule(int key) {
        super("Tracers", Category.render, key);
    }

    public void link() {
        showInvis = (ButtonSetting) ModuleManager.setting(this, "Show invis");
        lineWidth = (SliderSetting) ModuleManager.setting(this, "Line width");
        red = (SliderSetting) ModuleManager.setting(this, "Red");
        green = (SliderSetting) ModuleManager.setting(this, "Green");
        blue = (SliderSetting) ModuleManager.setting(this, "Blue");
        rainbow = (ButtonSetting) ModuleManager.setting(this, "Rainbow");
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        int n = rainbow.isToggled() ? RenderUtils.rainbow(2L, 0L) : this.color;
        float f = (float) lineWidth.getInput();
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p.deathTime != 0 || !showInvis.isToggled() && p.isInvisible()) continue;
            if (BotCheck.isBot(p)) continue;
            Render3D.tracer(p, f, n, e.partialTicks);
        }
    }

    @Override
    public void onEnable() {
        this.viewBobbing = mc.gameSettings.viewBobbing;
        if (this.viewBobbing) {
            mc.gameSettings.viewBobbing = false;
        }
    }

    @Override
    public void onUpdate() {
        if (mc.gameSettings.viewBobbing) {
            mc.gameSettings.viewBobbing = false;
        }
    }

    @Override
    public void onDisable() {
        mc.gameSettings.viewBobbing = this.viewBobbing;
    }

    @Override
    public void guiUpdate() {
        this.color = new Color((int) red.getInput(), (int) green.getInput(), (int) blue.getInput()).getRGB();
    }
}
