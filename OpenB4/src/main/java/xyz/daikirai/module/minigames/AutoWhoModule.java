package xyz.daikirai.module.minigames;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AutoWhoModule extends Module {
    public static AutoWhoModule instance;
    private static final Logger LOG = LogManager.getLogger();
    private static final String COMMAND = "/who";
    public static ButtonSetting artificial, hideMessage, runBedWarsInGame;
    private static boolean joined;
    private static boolean requested;
    public static long bedWarsAt;

    public AutoWhoModule(int key) {
        super("AutoWho", Category.minigames, key);
        instance = this;
    }

    public void link() {
        artificial = (ButtonSetting) ModuleManager.setting(this, "Artificial");
        hideMessage = (ButtonSetting) ModuleManager.setting(this, "Hide message");
        runBedWarsInGame = (ButtonSetting) ModuleManager.setting(this, "Run BedWars in game");
    }

    public static void request() {
        requested = true;
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (joined && System.currentTimeMillis() - Hypixel.joinedAt > 500L) {
            joined = false;
            if (runBedWarsInGame.enabled && Hypixel.state == Hypixel.State.BEDWARS_PREGAME) {
                return;
            }
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(8);
            if (s != null && s.getItem() == Items.bed) {
                run();
            }
        } else if (requested && System.currentTimeMillis() - bedWarsAt > 500L) {
            requested = false;
            run();
        }
    }

    @Override
    public void onDisable() {
        requested = false;
        joined = false;
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (hideMessage.enabled && s.startsWith("ONLINE: ")) {
            e.setCanceled(true);
            LOG.info("[CHAT] " + s);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == Minecraft.getMinecraft().thePlayer) {
            joined = true;
        }
    }

    private static List<NetworkPlayerInfo> players() {
        Minecraft mc = Minecraft.getMinecraft();
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

    public static void run() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!artificial.enabled) {
            PlayerUtils.sendChatMessage(COMMAND);
            return;
        }
        StringBuilder sb = new StringBuilder(hideMessage.enabled ? "ONLINE: " : "&b&lONLINE: &r");
        for (NetworkPlayerInfo info : players()) {
            ScorePlayerTeam team = info.getPlayerTeam();
            if (team != null && team.getTeamName().isEmpty() && team.getColorPrefix().startsWith("§c")) continue;
            if (hideMessage.enabled) {
                sb.append(info.getGameProfile().getName()).append(", ");
                continue;
            }
            sb.append(ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), info.getGameProfile().getName())).append("§").append("7, ");
        }
        if (hideMessage.enabled) {
            sb.append(mc.thePlayer.getName());
            LOG.info("[CHAT] " + sb);
            return;
        }
        sb.append(mc.thePlayer.getDisplayName().getFormattedText());
        Chat.raw(sb.toString());
    }
}
