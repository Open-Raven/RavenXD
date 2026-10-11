package xyz.daikirai.module.minigames;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.InfoHud;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SkyWarsModule extends Module {
    public static SliderSetting closestEnemy;
    public static ButtonSetting strengthIndicator, timeWarpPearl;

    private static boolean inGame;
    private static List<InfoHud.Line> enemies;
    public static final HashMap<String, Long> strength = new HashMap<String, Long>();
    private static final HashMap<EntityOtherPlayerMP, Long> ghosts = new HashMap<EntityOtherPlayerMP, Long>();
    public static long pearlAt;
    private static long countdownAt;
    private static final String TIME_WARP = "§b§l";

    public SkyWarsModule(int key) {
        super("Sky Wars", Category.minigames, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        closestEnemy = (SliderSetting) ModuleManager.setting(this, "Closest enemy");
        strengthIndicator = (ButtonSetting) ModuleManager.setting(this, "Strength indicator");
        timeWarpPearl = (ButtonSetting) ModuleManager.setting(this, "Time warp pearl");
    }

    @Override
    public void onDisable() {
        reset();
        enemies = null;
    }

    @Override
    public void onEnable() {
        enemies = new ArrayList<InfoHud.Line>((int) closestEnemy.getMax());
    }

    private static int byDistance(EntityPlayer a, EntityPlayer b) {
        return Double.compare(a.getDistanceSqToEntity(mc().thePlayer), b.getDistanceSqToEntity(mc().thePlayer));
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (!Chat.inWorld()) {
            return;
        }
        if (strengthIndicator.isToggled() && !strength.isEmpty()) {
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == mc.thePlayer || p.deathTime != 0) continue;
                Long until = strength.get(p.getName());
                if (until == null) continue;
                long left = until - System.currentTimeMillis();
                if (left <= 0L) {
                    strength.remove(p.getName());
                    continue;
                }
                int alpha = left > 150L ? 112 : (int) ((double) left / 150.0 * 112.0);
                Render3D.shadedBox(p, RenderUtils.withAlpha(Theme.FLARE.first.getRGB(), alpha), e.partialTicks);
            }
        }
        if (timeWarpPearl.isToggled() && !ghosts.isEmpty()) {
            Iterator<Map.Entry<EntityOtherPlayerMP, Long>> it = ghosts.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<EntityOtherPlayerMP, Long> entry = it.next();
                long until = entry.getValue();
                long left = until - System.currentTimeMillis();
                if (left <= 0L) {
                    it.remove();
                    continue;
                }
                double d = left > 150L ? 1.0 : (double) left / 150.0;
                int alpha = (int) (112.0 * d);
                int textAlpha = (int) (255.0 * d);
                EntityOtherPlayerMP ghost = entry.getKey();
                Render3D.shadedBox(ghost, RenderUtils.withAlpha(13082111, alpha), e.partialTicks);
                this.renderTimer(ghost, until, RenderUtils.withAlpha(-1, RenderUtils.clampAlpha(textAlpha)), e.partialTicks);
            }
        }
    }

    private static void reset() {
        strength.clear();
        countdownAt = 0L;
        pearlAt = 0L;
        ghosts.clear();
        if (enemies != null) {
            enemies.clear();
        }
    }

    public static void onSetback() {
        if (System.currentTimeMillis() - pearlAt < 7000L) {
            addGhost(mc().thePlayer);
            countdownAt = System.currentTimeMillis();
        }
        pearlAt = 0L;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = mc();
        if (!inGame || e.phase == TickEvent.Phase.START || !Overlay.canRender()) {
            return;
        }
        if (enemies != null) {
            for (InfoHud.Line line : enemies) {
                InfoHud.add(line);
            }
        }
        if (countdownAt != 0L) {
            long now = System.currentTimeMillis();
            long elapsed = now - countdownAt;
            long left = 3000L - elapsed;
            if (left <= 0L) {
                countdownAt = 0L;
                return;
            }
            int alpha = elapsed < 125L ? (int) ((float) elapsed / 125.0f * 240.0f) : (left < 125L ? (int) ((float) left / 125.0f * 240.0f) : 240);
            if (alpha <= 0) {
                return;
            }
            String s = Strings.round((double) left / 1000.0, 1) + "s";
            float half = (float) mc.fontRendererObj.getStringWidth(s) / 2.0f - 0.5f;
            float x = (float) RenderUtils.scaledWidth / 2.0f - half;
            float y = (float) RenderUtils.scaledHeight / 2.0f - 11.0f;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(s, x, y, RenderUtils.withAlpha(13082111, RenderUtils.clampAlpha(alpha)), true);
            GlStateManager.disableBlend();
        }
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (!inGame) {
            return;
        }
        if (s.endsWith(".")) {
            HashSet<String> names = new HashSet<String>();
            for (NetworkPlayerInfo info : tabList()) {
                names.add(info.getGameProfile().getName());
            }
            String[] words = s.split(" ");
            if (words.length > 2) {
                for (int i = words.length - 1; i >= 1; --i) {
                    String w = words[i];
                    if (w.endsWith(".")) {
                        w = w.replace(".", "");
                    } else if (w.endsWith("'s")) {
                        w = w.replace("'s", "");
                    }
                    if (!names.contains(w)) continue;
                    long l = Hypixel.skywars(3) ? 5000L : 2000L;
                    strength.put(w, System.currentTimeMillis() + l);
                    break;
                }
            }
        }
    }

    private static List<NetworkPlayerInfo> tabList() {
        Minecraft mc = mc();
        if (mc.getNetHandler() == null) {
            return new ArrayList<NetworkPlayerInfo>();
        }
        ArrayList<NetworkPlayerInfo> list = new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap());
        HashSet<NetworkPlayerInfo> set = new HashSet<NetworkPlayerInfo>(list);
        list.clear();
        list.addAll(set);
        list.remove(mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()));
        return list;
    }

    @Override
    public void onPearlThrown(ItemStack stack) {
        if (timeWarpPearl.isToggled() && stack.getDisplayName().startsWith(TIME_WARP)) {
            pearlAt = System.currentTimeMillis();
        }
    }

    private static void addGhost(EntityPlayer p) {
        EntityOtherPlayerMP ghost = new EntityOtherPlayerMP(mc().theWorld, p.getGameProfile());
        ghost.setEntityId(-170);
        ghost.copyLocationAndAnglesFrom(p);
        ghosts.put(ghost, System.currentTimeMillis() + 3000L);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = mc();
        inGame = Hypixel.skywars(2);
        if (!inGame) {
            return;
        }
        if (mc.thePlayer.ticksExisted % 3 == 0) {
            HashSet<Integer> ids = new HashSet<Integer>();
            ArrayList<EntityPlayer> list = new ArrayList<EntityPlayer>();
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == mc.thePlayer || p.deathTime != 0 || Hypixel.info(p) == null) continue;
                if (!ids.add(p.getEntityId())) continue;
                if (Hypixel.sameTeam(p)) continue;
                if (BotCheck.isBot(p, false)) continue;
                list.add(p);
            }
            enemies.clear();
            if (!list.isEmpty()) {
                list.sort(SkyWarsModule::byDistance);
                int n = Math.min((int) closestEnemy.getInput(), list.size());
                for (int i = 0; i < n; ++i) {
                    EntityPlayer p = list.get(i);
                    double d = p.getDistanceToEntity(mc.thePlayer);
                    enemies.add(InfoHud.line(p.getDisplayName().getUnformattedText(), ChatCommands.distance(d, false), p.getHeldItem()));
                }
            }
        }
    }

    private void renderTimer(EntityOtherPlayerMP ghost, long until, int color, float partialTicks) {
        Minecraft mc = mc();
        long left = until - System.currentTimeMillis();
        double d = Math.max(0.0, (double) left / 1000.0);
        String s = Strings.round(d, 1) + "s";
        RenderManager rm = mc.getRenderManager();
        double x = ghost.lastTickPosX + (ghost.posX - ghost.lastTickPosX) * (double) partialTicks - rm.viewerPosX;
        double y = ghost.lastTickPosY + (ghost.posY - ghost.lastTickPosY) * (double) partialTicks + (double) ghost.height * 0.6 - rm.viewerPosY;
        double z = ghost.lastTickPosZ + (ghost.posZ - ghost.lastTickPosZ) * (double) partialTicks - rm.viewerPosZ;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
        double dist = mc.thePlayer.getDistanceSqToEntity(ghost);
        float scale = (float) Math.min(0.025 * (1.0 + dist / 500.0), 0.1);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        int w = mc.fontRendererObj.getStringWidth(s);
        mc.fontRendererObj.drawString(s, (float) (-w / 2), -4.0f, color, true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        RenderUtils.resetColor();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            reset();
        }
    }
}
