package xyz.daikirai.module.minigames;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.InfoHud;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.script.ScriptEvents;
import xyz.daikirai.script.ScriptManager;
import xyz.daikirai.script.ScriptProtected;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.Executor;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BedWarsModule extends Module {
    public static SliderSetting alertInterval, closestEnemy;
    public static ButtonSetting instantShop, scrollToDepositWithdraw, whitelistOwnBed, ignoreTeammates, sendPartyChat, shouldPing, dragon, magicMilkTimer;
    public static ButtonSetting bow, diamondArmor, dreamDefender, enderPearl, enchants, fireball, knockbackStick, magicMilk, obsidian, potions,
            rotationItems, waterBucket, invisiblePlayers;

    public enum Alert {
        BLOCK_ZAPPER("&7is holding &3Block Zapper", false), BOW("&7is holding &6Bow", false), BOW_POWER("&7is holding &6Power Bow", false),
        BOW_PUNCH("&7is holding &6Punch Bow", false), CHARLIE_UNICORN("&7is holding &eCharlie Kirk", false),
        DIA_ARMOR("&7has purchased &bDiamond Armor", true), DIA_PICKAXE("&7is holding &bDiamond Pickaxe", false),
        DREAM_DEFENDER("&7is holding &fDream Defender", false), ENCHANT_PROT("&7has purchased &bProtection", true),
        ENCHANT_SHARPNESS("&7has purchased &bSharpness", true), ENDERPEARL("&7is holding &3Ender Pearl", false),
        FIREBALL("&7is holding &6Fireball", false), ICE_BRIDGE("&7is holding &bIce Bridge", false), INVISIBLE("&7is now &bInvisible", false),
        KB_STICK("&7is holding &6Knockback Stick", false), MAGIC_MILK("&7is holding &fMagic Milk", false),
        OBSIDIAN_HELD("&7is holding &dObsidian", false), OBSIDIAN_PLACED("&7placed &dObsidian", false),
        POTION_INVIS("&7is holding &dInvis &7Potion", false), POTION_JUMP("&7is holding &aJump &7Potion", false),
        POTION_SPEED("&7is holding &bSpeed &7Potion", false), SLEEPING_DUST("&7is holding &fSleeping Dust", false),
        WATER_BUCKET("&7is holding &bWater Bucket", false);

        public final String text;
        final boolean permanent;
        ButtonSetting button;
        HashSet<Integer> flagged;

        Alert(String text, boolean permanent) {
            this.text = text;
            this.permanent = permanent;
        }

        boolean hasSet() {
            return this.flagged != null;
        }

        boolean shouldAlert() {
            return this.button.isToggled();
        }

        boolean flag(EntityPlayer p, ItemStack stack, boolean teammate) {
            switch (this) {
                case BLOCK_ZAPPER:
                    return ItemUtils.is(stack, Items.prismarine_shard);
                case BOW:
                case BOW_POWER:
                case BOW_PUNCH: {
                    if (ItemUtils.is(stack, Items.bow)) {
                        if (stack.isItemEnchanted()) {
                            NBTTagList list = stack.getEnchantmentTagList();
                            int n = list != null ? list.tagCount() : 0;
                            if (n == 1) {
                                return this == BOW_POWER;
                            }
                            if (n == 2) {
                                return this == BOW_PUNCH;
                            }
                        } else {
                            return this == BOW;
                        }
                    }
                    return false;
                }
                case CHARLIE_UNICORN:
                    return ItemUtils.is(stack, Items.golden_horse_armor);
                case DIA_ARMOR: {
                    ItemStack legs = p.inventory.armorInventory[1];
                    return legs != null && legs.getItem() == Items.diamond_leggings;
                }
                case DIA_PICKAXE:
                    return ItemUtils.is(stack, Items.diamond_pickaxe);
                case DREAM_DEFENDER:
                    return ItemUtils.is(stack, Items.spawn_egg) && stack.getDisplayName().contains("Dream Defender");
                case ENCHANT_PROT: {
                    if (teammate) {
                        return false;
                    }
                    ItemStack legs = p.inventory.armorInventory[1];
                    return legs != null && legs.isItemEnchanted();
                }
                case ENCHANT_SHARPNESS:
                    if (teammate) {
                        return false;
                    }
                    return ItemUtils.isSword(stack) && stack.isItemEnchanted();
                case ENDERPEARL:
                    return ItemUtils.is(stack, Items.ender_pearl);
                case FIREBALL:
                    return ItemUtils.is(stack, Items.fire_charge);
                case ICE_BRIDGE:
                    return BlockUtils.blockOf(stack) == Blocks.ice;
                case INVISIBLE: {
                    Minecraft mc = Minecraft.getMinecraft();
                    return p.isInvisible() && p.ticksExisted >= 100 && mc.thePlayer.inventory.armorInventory[1] != null && !p.getName().equals(mc.thePlayer.getName())
                            && p.getDisplayName().getUnformattedText().contains(" ");
                }
                case KB_STICK:
                    return ItemUtils.is(stack, Items.stick) && stack.isItemEnchanted();
                case MAGIC_MILK:
                    return ItemUtils.is(stack, Items.milk_bucket);
                case OBSIDIAN_HELD:
                    return BlockUtils.blockOf(stack) == Blocks.obsidian;
                case POTION_INVIS:
                case POTION_JUMP:
                case POTION_SPEED: {
                    List<PotionEffect> list;
                    if (stack != null && stack.getItem() instanceof ItemPotion && (list = ((ItemPotion) stack.getItem()).getEffects(stack)) != null) {
                        for (PotionEffect e : list) {
                            int id = e.getPotionID();
                            if (id == 14) {
                                return this == POTION_INVIS;
                            }
                            if (id == 8) {
                                return this == POTION_JUMP;
                            }
                            if (id != 1) continue;
                            return this == POTION_SPEED;
                        }
                    }
                    return false;
                }
                case SLEEPING_DUST:
                    return ItemUtils.is(stack, Items.snowball) && stack.getDisplayName().contains("Sleeping Dust");
                case WATER_BUCKET:
                    return ItemUtils.is(stack, Items.water_bucket);
                default:
                    return false;
            }
        }
    }

    private static boolean inGame;
    public static BlockPos ownBed;
    private static List<InfoHud.Line> enemies;
    private static final List<BlockPos> obsidianPlaced = new ArrayList<BlockPos>();
    private static final HashMap<UUID, HashMap<Alert, Long>> lastAlerts = new HashMap<UUID, HashMap<Alert, Long>>();
    private static String partyMessage;
    private static double dragonDistance = -1.0;
    private static int milkTicks;
    private static String milkText;
    private static long lastParty, lastPing;
    private static final int DRAGON_COLOR = new Color(214, 91, 191).getRGB();
    private static final List<String> START_LINES = Arrays.asList("  Protect your bed", "  Select an ultimate", "  Collect Lucky Blocks",
            "  Become a Soul Collector", "  All generators are maxed!", "  Destroy the enemy bed and", "  Every few seconds brings a new");
    private static final Set<Item> RESOURCES = new HashSet<Item>(Arrays.asList(Items.iron_ingot, Items.gold_ingot, Items.diamond, Items.emerald));
    private static final Set<String> SHOPS = new HashSet<String>(Arrays.asList("Quick Buy", "Blocks", "Melee", "Armor", "Tools", "Ranged", "Potions",
            "Utility", "Rotating Items", "Purchase Enemy Tracker"));

    public BedWarsModule(int key) {
        super("Bed Wars", Category.minigames, key);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        instantShop = (ButtonSetting) ModuleManager.setting(this, "Instant shop");
        scrollToDepositWithdraw = (ButtonSetting) ModuleManager.setting(this, "Scroll to deposit/withdraw");
        whitelistOwnBed = (ButtonSetting) ModuleManager.setting(this, "Whitelist own bed");
        alertInterval = (SliderSetting) ModuleManager.setting(this, "Alert interval");
        bow = (ButtonSetting) ModuleManager.setting(this, "Bow");
        diamondArmor = (ButtonSetting) ModuleManager.setting(this, "Diamond armor");
        dreamDefender = (ButtonSetting) ModuleManager.setting(this, "Dream defender");
        enderPearl = (ButtonSetting) ModuleManager.setting(this, "Ender pearl");
        enchants = (ButtonSetting) ModuleManager.setting(this, "Enchants");
        fireball = (ButtonSetting) ModuleManager.setting(this, "Fireball");
        knockbackStick = (ButtonSetting) ModuleManager.setting(this, "Knockback stick");
        magicMilk = (ButtonSetting) ModuleManager.setting(this, "Magic milk");
        obsidian = (ButtonSetting) ModuleManager.setting(this, "Obsidian");
        potions = (ButtonSetting) ModuleManager.setting(this, "Potions");
        rotationItems = (ButtonSetting) ModuleManager.setting(this, "Rotation items");
        waterBucket = (ButtonSetting) ModuleManager.setting(this, "Water bucket");
        invisiblePlayers = (ButtonSetting) ModuleManager.setting(this, "Invisible players");
        ignoreTeammates = (ButtonSetting) ModuleManager.setting(this, "Ignore teammates");
        sendPartyChat = (ButtonSetting) ModuleManager.setting(this, "Send party chat");
        shouldPing = (ButtonSetting) ModuleManager.setting(this, "Should ping");
        closestEnemy = (SliderSetting) ModuleManager.setting(this, "Closest enemy");
        dragon = (ButtonSetting) ModuleManager.setting(this, "Dragon");
        magicMilkTimer = (ButtonSetting) ModuleManager.setting(this, "Magic milk timer");
        ButtonSetting[] buttons = {rotationItems, bow, bow, bow, rotationItems, diamondArmor, null, dreamDefender, enchants, enchants, enderPearl,
                fireball, rotationItems, invisiblePlayers, knockbackStick, magicMilk, obsidian, null, potions, potions, potions, rotationItems, waterBucket};
        for (Alert a : Alert.values()) {
            a.button = buttons[a.ordinal()];
            a.flagged = a.button != null ? new HashSet<Integer>() : null;
        }
    }

    @SubscribeEvent
    public void onUseFinish(PlayerUseItemEvent.Finish e) {
        if (!magicMilkTimer.isToggled() || !inGame || e.item.getItem() != Items.milk_bucket) {
            return;
        }
        milkTicks = 600;
    }

    @Override
    public void onDisable() {
        reset();
        enemies = null;
    }

    private static int compare(boolean byBed, EntityPlayer a, EntityPlayer b) {
        double da;
        double db;
        if (byBed) {
            da = a.getDistanceSq(ownBed);
            db = b.getDistanceSq(ownBed);
        } else {
            da = a.getDistanceSqToEntity(mc().thePlayer);
            db = b.getDistanceSqToEntity(mc().thePlayer);
        }
        return Double.compare(da, db);
    }

    private static void alert(EntityPlayer p, Alert a) {
        alert(p, a, Hypixel.sameTeam(p));
    }

    public static boolean isOwnBed(BlockPos pos) {
        return ModuleManager.enabled("Bed Wars") != null && whitelistOwnBed.isToggled() && ownBed != null && ownBed.equals(pos);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (!inGame || !Chat.inWorld()) {
            return;
        }
        if (dragon.isToggled()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityDragon)) continue;
                renderDragon((EntityDragon) entity, DRAGON_COLOR, e.partialTicks);
            }
            if (dragonDistance != -1.0) {
                InfoHud.add("&7Dragon: &" + distanceColor(dragonDistance) + (int) dragonDistance + "m");
            }
        }
        if (enemies != null) {
            for (InfoHud.Line line : enemies) {
                InfoHud.add(line);
            }
        }
        if (milkText != null) {
            InfoHud.add(milkText);
        }
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        Minecraft mc = mc();
        if (!inGame || !(e.gui instanceof GuiChest)) {
            return;
        }
        IInventory chest;
        if (instantShop.isToggled() && Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && !GuiScreen.isShiftKeyDown()
                && SHOPS.contains((chest = lowerChest()).getName())) {
            Slot slot = PlayerUtils.chestSlotUnderMouse((GuiChest) e.gui, chest.getSizeInventory());
            if (slot != null) {
                ScriptProtected.guiClick(slot.slotNumber, 2, 3);
                e.setCanceled(true);
            }
        }
        if (!scrollToDepositWithdraw.isToggled()) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        chest = lowerChest();
        int size = chest.getSizeInventory();
        if (size == 27) {
            if (wheel > 0) {
                for (int i = size; i < size + 36; ++i) {
                    ItemStack stack = mc.thePlayer.openContainer.getSlot(i).getStack();
                    if (stack == null || !RESOURCES.contains(stack.getItem())) continue;
                    ScriptProtected.guiClick(i, 0, 1);
                    break;
                }
            } else {
                for (int i = 0; i < size; ++i) {
                    ItemStack stack = chest.getStackInSlot(i);
                    if (stack == null || !RESOURCES.contains(stack.getItem())) continue;
                    ScriptProtected.guiClick(i, 0, 1);
                    break;
                }
            }
        }
    }

    private static IInventory lowerChest() {
        return ((ContainerChest) mc().thePlayer.openContainer).getLowerChestInventory();
    }

    public static void onGameStartChat(ClientChatReceivedEvent e, String s, boolean bedWars, boolean bedEsp, boolean autoWho) {
        for (String line : START_LINES) {
            if (!s.contains(line)) continue;
            AutoWhoModule.bedWarsAt = System.currentTimeMillis();
            if (bedWars) {
                reset();
                for (Alert a : Alert.values()) {
                    if (!a.hasSet()) continue;
                    a.flagged.clear();
                }
            }
            if (bedWars || bedEsp) {
                Executor.POOL.execute(BedWarsModule::findBedLater);
            }
            if (!autoWho) break;
            AutoWhoModule.request();
            break;
        }
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        if (partyMessage != null) {
            PlayerUtils.sendChatMessage("/pc " + partyMessage);
            partyMessage = null;
        }
        if (!(inGame = Hypixel.bedwars(2))) {
            return;
        }
        if (mc.thePlayer.ticksExisted % 3 == 0) {
            HashSet<Integer> ids = new HashSet<Integer>();
            ArrayList<EntityPlayer> list = new ArrayList<EntityPlayer>();
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == mc.thePlayer || p.deathTime != 0 || Hypixel.info(p) == null) continue;
                if (!ids.add(p.getEntityId())) continue;
                boolean teammate = Hypixel.sameTeam(p);
                if (closestEnemy.isNotDefault() && ownBed != null && !teammate && !BotCheck.isBot(p, false)) {
                    list.add(p);
                }
                int id = p.getEntityId();
                ItemStack held = p.getHeldItem();
                for (Alert a : Alert.values()) {
                    if (!a.hasSet()) continue;
                    if (a.flagged.contains(id)) {
                        if (a.permanent || a.flag(p, held, teammate) || a.permanent) continue;
                        a.flagged.remove(id);
                        continue;
                    }
                    if (!a.flag(p, held, teammate)) continue;
                    a.flagged.add(id);
                    if (!a.shouldAlert()) continue;
                    alert(p, a, teammate);
                }
            }
            enemies.clear();
            if (!list.isEmpty()) {
                boolean byBed = Hypixel.state == Hypixel.State.BEDWARS_GAME_BED;
                list.sort((a, b) -> compare(byBed, a, b));
                int n = Math.min((int) closestEnemy.getInput(), list.size());
                for (int i = 0; i < n; ++i) {
                    EntityPlayer p = list.get(i);
                    double d = byBed ? Math.sqrt(p.getDistanceSq(ownBed)) : (double) p.getDistanceToEntity(mc.thePlayer);
                    enemies.add(InfoHud.line(p.getDisplayName().getUnformattedText(), ChatCommands.distance(d, false), p.getHeldItem()));
                }
            }
            if (dragon.isToggled()) {
                double d = -1.0;
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (!(entity instanceof EntityDragon)) continue;
                    double d2 = mc.thePlayer.getDistanceSqToEntity(entity);
                    if (d != -1.0 && !(d2 < d)) continue;
                    d = d2;
                }
                dragonDistance = d;
                if (d != -1.0) {
                    dragonDistance = Math.sqrt(dragonDistance);
                }
            }
        }
        if (milkTicks != 0) {
            if (milkTicks > 0) {
                milkText = "&7Magic Milk: &a" + Strings.round((double) milkTicks / 20.0, 1) + "s";
                if (--milkTicks == 0) {
                    milkTicks = -40;
                }
            } else {
                milkText = ++milkTicks == 0 ? null : "&7Magic milk: &cFinished";
            }
        }
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (!b.isToggled()) {
            if (b == dragon) {
                dragonDistance = -1.0;
            } else if (b == magicMilkTimer) {
                milkText = null;
                milkTicks = 0;
            }
        }
    }

    private static void findBedLater() {
        try {
            Thread.sleep(1000L);
            findBed();
            if (ownBed != null || mc().theWorld == null) {
                return;
            }
            Thread.sleep(2500L);
            findBed();
        } catch (InterruptedException ignored) {
        }
        Module bedWars = ModuleManager.get("Bed Wars");
        if (ownBed == null && whitelistOwnBed.isToggled() && Hypixel.bedwars(2)) {
            Chat.moduleMessage(bedWars, "&cCouldn't find bed location!");
        }
    }

    private static void alert(EntityPlayer p, Alert a, boolean teammate) {
        Minecraft mc = mc();
        if (ignoreTeammates.isToggled() && teammate) {
            return;
        }
        long now = System.currentTimeMillis();
        if (alertInterval.getInput() > 0.0) {
            HashMap<Alert, Long> map = lastAlerts.get(p.getUniqueID());
            if (map == null) {
                map = new HashMap<Alert, Long>();
            } else {
                Long last = map.get(a);
                if (last != null && (double) Math.abs(now - last) <= alertInterval.getInput() * 1000.0) {
                    return;
                }
            }
            map.put(a, now);
            lastAlerts.put(p.getUniqueID(), map);
        }
        double d = mc.thePlayer.getDistanceToEntity(p);
        String name = p.getDisplayName().getUnformattedText();
        String s = name + " " + a.text + " " + ChatCommands.distance(d, true);
        Chat.print(s);
        if (sendPartyChat.isToggled() && now - lastParty > 1500L && Hypixel.onHypixel()) {
            partyMessage = Chat.strip(Chat.color(s));
            lastParty = now;
        }
        if (shouldPing.isToggled() && now - lastPing > 1500L) {
            PlayerUtils.ping();
            lastPing = now;
        }
        if (ScriptManager.events != null) {
            ScriptEvents.bedWarsAlert(a.name(), p);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            reset();
        }
    }

    @Override
    public void onEnable() {
        enemies = new ArrayList<InfoHud.Line>((int) closestEnemy.getMax());
    }

    private static char distanceColor(double d) {
        if (d <= 25.0) {
            return 'c';
        }
        if (d <= 50.0) {
            return '6';
        }
        if (d <= 75.0) {
            return 'e';
        }
        if (d <= 125.0) {
            return 'a';
        }
        return '2';
    }

    private static void reset() {
        inGame = false;
        milkTicks = 0;
        lastPing = 0;
        dragonDistance = -1.0;
        obsidianPlaced.clear();
        lastAlerts.clear();
        milkText = null;
        partyMessage = null;
        if (enemies != null) {
            enemies.clear();
        }
    }

    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (!(packet instanceof S23PacketBlockChange)) {
            return true;
        }
        S23PacketBlockChange p = (S23PacketBlockChange) packet;
        Minecraft mc = mc();
        if (obsidian.isToggled() && p.getBlockState().getBlock() == Blocks.obsidian && BlockUtils.nextToBed(p.getBlockPosition())) {
            obsidianPlaced.add(p.getBlockPosition());
            double d = 36.0;
            EntityPlayer placer = null;
            for (EntityPlayer other : mc.theWorld.playerEntities) {
                if (other == mc.thePlayer || other.deathTime != 0) continue;
                double d2 = other.getDistanceSq(p.getBlockPosition());
                if (!(d2 < d)) continue;
                d = d2;
                placer = other;
            }
            if (placer != null) {
                alert(placer, Alert.OBSIDIAN_PLACED);
            }
        }
        return true;
    }

    private static void findBed() {
        Minecraft mc = mc();
        ownBed = null;
        for (int i = 10; i >= -10; --i) {
            for (int j = -25; j <= 25; ++j) {
                for (int k = -25; k <= 25; ++k) {
                    if (mc.theWorld == null) {
                        return;
                    }
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + (double) j, mc.thePlayer.posY + (double) i, mc.thePlayer.posZ + (double) k);
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (state.getBlock() != Blocks.bed || state.getValue(BlockBed.PART) != BlockBed.EnumPartType.FOOT) continue;
                    ownBed = pos;
                    return;
                }
            }
        }
    }

    private static void renderDragon(EntityDragon dragon, int color, float partialTicks) {
        RenderManager rm = mc().getRenderManager();
        double x = dragon.lastTickPosX + (dragon.posX - dragon.lastTickPosX) * (double) partialTicks - rm.viewerPosX;
        double y = dragon.lastTickPosY + (dragon.posY - dragon.lastTickPosY) * (double) partialTicks - rm.viewerPosY;
        double z = dragon.lastTickPosZ + (dragon.posZ - dragon.lastTickPosZ) * (double) partialTicks - rm.viewerPosZ;
        AxisAlignedBB bb = dragon.getEntityBoundingBox().expand(2.0, 0.0, 2.0);
        AxisAlignedBB box = new AxisAlignedBB(bb.minX - dragon.posX + x, bb.minY - dragon.posY + y, bb.minZ - dragon.posZ + z,
                bb.maxX - dragon.posX + x, bb.maxY - dragon.posY + y - 3.0, bb.maxZ - dragon.posZ + z);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float a = (float) (color >> 24 & 255) / 255.0f;
        float r = (float) (color >> 16 & 0xFF) / 255.0f;
        float g = (float) (color >> 8 & 0xFF) / 255.0f;
        float b = (float) (color & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
        GL11.glEnable(2848);
        GL11.glLineWidth(2.0f);
        RenderGlobal.drawSelectionBoundingBox(box);
        GL11.glDisable(2848);
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }
}
