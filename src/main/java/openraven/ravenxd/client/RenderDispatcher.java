package openraven.ravenxd.client;

import openraven.ravenxd.gui.Console;
import openraven.ravenxd.module.render.BedESPModule;
import openraven.ravenxd.module.render.FallViewModule;
import openraven.ravenxd.module.render.NametagsModule;
import openraven.ravenxd.module.render.PlayerESPModule;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.util.Render3D;
import openraven.ravenxd.util.Strings;
import openraven.ravenxd.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class RenderDispatcher {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean playerEsp() {
        return PlayerESPModule.instance != null && PlayerESPModule.instance.isEnabled()
                && (!PlayerESPModule.disableInLobby.isToggled() || Hypixel.state != Hypixel.State.LOBBY);
    }

    public static boolean nametags() {
        return NametagsModule.instance != null && NametagsModule.instance.isEnabled()
                && (!NametagsModule.disableInLobby.isToggled() || Hypixel.state != Hypixel.State.LOBBY);
    }

    public static boolean visible(EntityLivingBase e, ButtonSetting showInvis) {
        if (showInvis != null && !showInvis.isToggled() && e.isInvisible()) {
            return false;
        }
        return !BotCheck.aboveSpawn(e) && Render3D.isVisible(e) && !BotCheck.isBot(e);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        float pt = e.partialTicks;
        boolean esp = playerEsp();
        boolean tags = nametags();
        boolean murder = openraven.ravenxd.module.minigames.MurderMysteryModule.inGame;
        boolean track = ChatCommands.tracking != null;
        boolean beds = BedESPModule.active;
        if (tags || beds) {
            Render3D.updateCameraPos();
        }
        if (beds) {
            BedESPModule.render(pt);
        }
        if (esp && PlayerESPModule.isShader()) {
            PlayerESPModule.renderShader(pt);
        }
        if (esp || tags || murder || track) {
            EntityPlayer self = null;
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == mc.thePlayer) {
                    self = p;
                    continue;
                }
                if (p.deathTime != 0) continue;
                if (esp && visible(p, PlayerESPModule.showInvis)) {
                    PlayerESPModule.render(p, pt);
                }
                if (tags && visible(p, NametagsModule.showInvis)) {
                    NametagsModule.render(p, pt);
                }
                if (murder) {
                    openraven.ravenxd.module.minigames.MurderMysteryModule.render(p, pt);
                }
                if (!track || !p.getName().equals(ChatCommands.tracking)) continue;
                Render3D.entityBox((Entity) p, 2, Theme.defaultTheme.getRGB(0.0), pt);
                if (esp || tags || murder) continue;
                break;
            }
            if (self != null && mc.gameSettings.thirdPersonView != 0 && !self.isPlayerSleeping()) {
                if (esp && PlayerESPModule.renderSelf.isToggled()) {
                    PlayerESPModule.render(self, pt);
                }
                if (tags && NametagsModule.renderSelf.isToggled()) {
                    NametagsModule.render(self, pt);
                }
            }
        }
        BotCheck.renderWatchdog(pt);
        openraven.ravenxd.module.player.ScaffoldModule.renderPlaced();
    }

    @SubscribeEvent
    public void onSpecials(RenderLivingEvent.Specials.Pre<?> e) {
        if (PlayerESPModule.shaderPass || e.x == 0.0 && e.y == 0.0 && e.z == 0.0) {
            e.setCanceled(true);
        } else if (nametags() && Chat.inWorld() && visible(e.entity, NametagsModule.showInvis)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            Irc.countFrame();
            Render3D.updateFrustum(e.renderTickTime);
        } else if (Chat.inWorld()) {
            openraven.ravenxd.module.combat.KillAuraModule.onRenderTick();
            openraven.ravenxd.module.minigames.SpeedBuildersModule.render();
            boolean visible = Overlay.canRender();
            if (visible) {
                openraven.ravenxd.module.player.ScaffoldModule.renderCount();
                openraven.ravenxd.module.player.FastPlaceModule.renderCount();
                openraven.ravenxd.module.player.AutoPlaceModule.renderCount();
                openraven.ravenxd.module.network.BlinkModule.renderTicks();
                openraven.ravenxd.module.network.FreezeModule.render();
                FallViewModule.render();
                if (Console.bps) {
                    mc.fontRendererObj.drawString(bps(mc.thePlayer, 2) + "bps", 20.0f, 20.0f, -1, true);
                }
            }
            InfoHud.render(visible);
        }
    }

    public static double bps(Entity e, int places) {
        double dx = e.posX - e.prevPosX;
        double dz = e.posZ - e.prevPosZ;
        double d = Math.sqrt(dx * dx + dz * dz) * 20.0;
        return Strings.round(d, places);
    }
}
