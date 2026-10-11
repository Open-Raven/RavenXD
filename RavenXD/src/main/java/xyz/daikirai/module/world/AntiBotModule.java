package xyz.daikirai.module.world;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.HoverEvent;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiBotModule extends Module {
    public static AntiBotModule instance;
    public static SliderSetting spawnDelay, respawnDelay, pitSpawn;
    public static ButtonSetting printWorldJoin;
    public static long respawnTime;

    public AntiBotModule(int key) {
        super("AntiBot", Category.world, key);
        instance = this;
    }

    public void link() {
        spawnDelay = (SliderSetting) ModuleManager.setting(this, "Delay (spawn)");
        respawnDelay = (SliderSetting) ModuleManager.setting(this, "Delay (respawn)");
        pitSpawn = (SliderSetting) ModuleManager.setting(this, "Pit spawn");
        printWorldJoin = (ButtonSetting) ModuleManager.setting(this, "Print world join");
    }

    private static boolean enabled() {
        return instance != null && instance.isEnabled();
    }

    @Override
    public void onDisable() {
        BotCheck.recentlySpawned.clear();
        BotCheck.resetWatchdog();
        respawnTime = 0;
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (respawnDelay.isNotDefault() && s.equals("You have respawned!")) {
            respawnTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity != Minecraft.getMinecraft().thePlayer && e.entity instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e.entity;
            if (spawnDelay.isNotDefault()) {
                BotCheck.recentlySpawned.put(p, System.currentTimeMillis());
            }
            if (printWorldJoin.enabled) {
                printJoin(p, false);
            }
        }
    }

    @Override
    public void onUpdate() {
        if (!BotCheck.recentlySpawned.isEmpty() && Minecraft.getMinecraft().thePlayer.ticksExisted % 2 == 0) {
            long now = System.currentTimeMillis();
            BotCheck.recentlySpawned.values().removeIf(t -> (double) t < (double) now - spawnDelay.getInput() * 1000.0);
        }
    }

    public static boolean ignored(Entity entity) {
        if (enabled()) {
            double d;
            if (pitSpawn.isNotDefault() && entity.posY >= pitSpawn.getInput()) {
                return true;
            }
            if (respawnTime != 0L && (d = respawnDelay.getInput() * 1000.0) > 0.0 && (double) (System.currentTimeMillis() - respawnTime) < d) {
                return true;
            }
        }
        return false;
    }

    public static void printJoin(EntityPlayer p, boolean watchdog) {
        Minecraft mc = Minecraft.getMinecraft();
        String s = "&7Entity &b" + p.getEntityId() + " &7joined: &r" + p.getDisplayName().getUnformattedText() + (watchdog ? " &7(&cwatchdog&7)" : "");
        try {
            ChatComponentText text = new ChatComponentText(Chat.color(Chat.PREFIX + s));
            String hover = "&ename: &r" + p.getName() + "\n&edisplay name: &r" + p.getDisplayName().getUnformattedText()
                    + "\n&estripped: &r" + Chat.strip(p.getDisplayName().getUnformattedText()) + "\n&euuid: &7" + p.getUniqueID()
                    + "\n&ex: &7" + p.posX + "\n&ey: &7" + p.posY + "\n&ez: &7" + p.posZ + "\n&eyaw: &7" + p.rotationYaw
                    + "\n&epitch: &7" + p.rotationPitch + "\n&ehealth: &7" + p.getHealth() + "\n&ehurt time: &7" + p.hurtTime + "/" + p.maxHurtTime
                    + "\n&einvisible: &7" + p.isInvisible() + "\n&eitem: &7" + (p.getHeldItem() == null ? "null" : p.getHeldItem().getDisplayName())
                    + (watchdog ? "" : "\n&ebot: &7" + BotCheck.isBot(p, false));
            Team team = p.getTeam();
            if (team != null) {
                hover = hover + "\n&eteam: &7" + team.getRegisteredName();
            } else {
                hover = hover + "\n&eteam: &7null";
            }
            NetworkPlayerInfo info = Hypixel.info(p);
            if (info != null) {
                hover = hover + "\n&etablist: &7true";
                ScorePlayerTeam t = info.getPlayerTeam();
                hover = hover + "\n&enetwork team: &7" + (t == null ? "null" : t.getTeamName()) + "\n&eping: &7" + info.getResponseTime();
            } else {
                hover = hover + "\ntablist: false";
            }
            ChatStyle style = new ChatStyle();
            style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color(hover))));
            text.setChatStyle(style);
            mc.thePlayer.addChatMessage(text);
        } catch (Exception ex) {
            Chat.moduleMessage(instance, s + " &7(&c" + ex.getClass().getSimpleName() + "&7)");
        }
    }
}
