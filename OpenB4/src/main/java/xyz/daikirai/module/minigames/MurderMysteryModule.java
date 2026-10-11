package xyz.daikirai.module.minigames;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.InfoHud;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.render.ItemESPModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MurderMysteryModule extends Module {
    public static ButtonSetting alertMurderer, infoHud, bow, dead, goldIngots, innocent, murderer;

    public static boolean inGame;
    private static HashSet<Integer> murderers, bows;
    private static HashSet<String> knives;
    private static List<InfoHud.Line> lines;
    private static boolean hadDisableGold, hadItemEsp;

    public MurderMysteryModule(int key) {
        super("Murder Mystery", Category.minigames, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        alertMurderer = (ButtonSetting) ModuleManager.setting(this, "Alert murderer");
        infoHud = (ButtonSetting) ModuleManager.setting(this, "Info HUD");
        bow = (ButtonSetting) ModuleManager.setting(this, "Bow");
        dead = (ButtonSetting) ModuleManager.setting(this, "Dead");
        goldIngots = (ButtonSetting) ModuleManager.setting(this, "Gold ingots");
        innocent = (ButtonSetting) ModuleManager.setting(this, "Innocent");
        murderer = (ButtonSetting) ModuleManager.setting(this, "Murderer");
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            this.reset();
        }
    }

    private void reset() {
        inGame = false;
        murderers.clear();
        bows.clear();
        lines.clear();
    }

    private static boolean isKnife(Item item) {
        return knives.contains(item == null ? "null" : item.getRegistryName().replaceFirst("minecraft:", "")) || item instanceof ItemRecord;
    }

    @Override
    public void onEnable() {
        murderers = new HashSet<Integer>();
        bows = new HashSet<Integer>();
        knives = new HashSet<String>(Arrays.asList("iron_sword", "stone_sword", "iron_shovel", "stick", "wooden_axe", "wooden_sword", "deadbush",
                "reeds", "stone_shovel", "blaze_rod", "diamond_shovel", "quartz", "pumpkin_pie", "golden_pickaxe", "leather", "name_tag", "coal", "flint",
                "bone", "carrot", "golden_carrot", "cookie", "diamond_axe", "double_plant", "prismarine_shard", "cooked_beef", "netherbrick",
                "cooked_chicken", "record_blocks", "golden_hoe", "golden_sword", "diamond_sword", "diamond_hoe", "shears", "fish", "dye", "bread", "boat",
                "speckled_melon", "book", "sapling", "ender_chest", "golden_axe", "diamond_pickaxe", "golden_shovel"));
        if (goldIngots.isToggled()) {
            hadDisableGold = ItemESPModule.disableGold.isToggled();
            if (hadDisableGold) {
                ItemESPModule.disableGold.setEnabled(false);
                ItemESPModule.disableGold.updateLabel();
            }
            Module itemEsp = ModuleManager.get("ItemESP");
            hadItemEsp = itemEsp != null && itemEsp.isEnabled();
            if (!hadItemEsp && itemEsp != null) {
                itemEsp.enable();
            }
        }
        lines = new ArrayList<InfoHud.Line>();
    }

    private static void box(Entity entity, int color, float partialTicks) {
        Render3D.entityBox(entity, 2, color, partialTicks);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (!inGame || lines.isEmpty() || e.phase == TickEvent.Phase.START || !Overlay.canRender()) {
            return;
        }
        for (InfoHud.Line line : lines) {
            InfoHud.add(line);
        }
    }

    @Override
    public void onDisable() {
        this.reset();
        bows = null;
        murderers = null;
        knives = null;
        lines = null;
        if (hadDisableGold) {
            ItemESPModule.disableGold.setEnabled(true);
            ItemESPModule.disableGold.updateLabel();
            hadDisableGold = false;
        }
        if (hadItemEsp) {
            hadItemEsp = false;
        } else if (goldIngots.isToggled()) {
            Module itemEsp = ModuleManager.get("ItemESP");
            if (itemEsp != null) {
                itemEsp.disable();
            }
        }
    }

    public static void render(EntityPlayer p, float partialTicks) {
        if (mc().thePlayer.capabilities.isCreativeMode && p.isInvisible()) {
            return;
        }
        int id = p.getEntityId();
        if (murderer.isToggled() && murderers.contains(id)) {
            box(p, Theme.FLARE.first.getRGB(), partialTicks);
        } else if (bow.isToggled() && bows.contains(id)) {
            box(p, Theme.GOLD.first.getRGB(), partialTicks);
        } else if (dead.isToggled() && p.isPlayerSleeping()) {
            box(p, Theme.FLARE.second.getRGB(), partialTicks);
        } else if (innocent.isToggled()) {
            box(p, Theme.VINE.first.getRGB(), partialTicks);
        }
    }

    private static void updateLines() {
        Minecraft mc = mc();
        lines.clear();
        if (!infoHud.isToggled() || murderers.isEmpty() && bows.isEmpty()) {
            return;
        }
        for (int id : murderers) {
            Entity e = mc.theWorld.getEntityByID(id);
            if (!(e instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer) e;
            double d = p.getDistanceToEntity(mc.thePlayer);
            lines.add(InfoHud.line("§c" + p.getName(), ChatCommands.distance(d, false), p.getHeldItem()));
        }
        for (int id : bows) {
            Entity e = mc.theWorld.getEntityByID(id);
            if (!(e instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer) e;
            double d = p.getDistanceToEntity(mc.thePlayer);
            lines.add(InfoHud.line("§e" + p.getName(), ChatCommands.distance(d, false), p.getHeldItem()));
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = mc();
        inGame = Hypixel.state == Hypixel.State.MURDER_MYSTERY_GAME;
        if (!inGame) {
            return;
        }
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p.getHeldItem() == null || p.getName().isEmpty()) continue;
            Integer id = p.getEntityId();
            if (murderers.contains(id)) continue;
            Item item = p.getHeldItem().getItem();
            if (isKnife(item)) {
                bows.remove(id);
                murderers.add(id);
                if (!alertMurderer.isToggled()) continue;
                double d = mc.thePlayer.getDistanceToEntity(p);
                Chat.print("&b" + p.getName() + " &7is the &cmurderer&7! " + ChatCommands.distance(d, true));
                PlayerUtils.ping();
                continue;
            }
            if (item != Items.bow && item != Items.arrow || bows.contains(id)) continue;
            bows.add(id);
        }
        if (mc.thePlayer.ticksExisted % 3 == 0) {
            updateLines();
        }
    }
}
