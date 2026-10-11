package xyz.daikirai.module.render;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;

public class ChamsModule extends Module {
    public static ButtonSetting ignoreBots, hidePlayers, renderSelf;
    private final HashSet<Entity> rendered = new HashSet<Entity>();

    public ChamsModule(int key) {
        super("Chams", Category.render, key);
    }

    public void link() {
        ignoreBots = (ButtonSetting) ModuleManager.setting(this, "Ignore bots");
        hidePlayers = (ButtonSetting) ModuleManager.setting(this, "Hide players");
        renderSelf = (ButtonSetting) ModuleManager.setting(this, "Render self");
    }

    @SubscribeEvent
    public void onPre(RenderPlayerEvent.Pre e) {
        if (hidePlayers.isToggled()) {
            return;
        }
        if (!renderSelf.isToggled() && e.entity == mc.thePlayer) {
            return;
        }
        if (ignoreBots.isToggled()) {
            if (BotCheck.isBot(e.entity)) {
                return;
            }
            this.rendered.add(e.entity);
        }
        GL11.glEnable(32823);
        GL11.glPolygonOffset(1.0f, -3800000.0f);
    }

    @Override
    public void onDisable() {
        this.rendered.clear();
    }

    @SubscribeEvent
    public void onPost(RenderPlayerEvent.Post e) {
        if (hidePlayers.isToggled()) {
            return;
        }
        if (!renderSelf.isToggled() && e.entity == mc.thePlayer) {
            return;
        }
        if (ignoreBots.isToggled()) {
            if (!this.rendered.contains(e.entity)) {
                return;
            }
            this.rendered.remove(e.entity);
        }
        GL11.glPolygonOffset(1.0f, 3800000.0f);
        GL11.glDisable(32823);
    }
}
