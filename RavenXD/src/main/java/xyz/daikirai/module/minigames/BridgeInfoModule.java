package xyz.daikirai.module.minigames;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.gui.BridgeInfoEditor;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BridgeInfoModule extends Module {
    public static final int COLOR = -16725816;
    public static int posX = 5;
    public static int posY = 70;
    public static ButtonSetting editPosition;
    private static final String ENEMY = "Enemy: ";
    private static final String DISTANCE = "Distance to goal: ";
    private static final String ENEMY_DISTANCE = "Enemy distance to goal: ";
    private static final String BLOCKS = "Blocks: ";
    private static final String DEFEND = "Defend!";
    private static final String JUMP = "Jump in to score!";
    private static final String START = "First player to score 5 goals wins";
    private static final String BRIDGE = "the brid";
    private static final String MODE = "mode";
    private static final String OPPONENT = "Opponent";
    private String enemy = "";
    private boolean awaitingOpponent;
    private BlockPos defendPos, scorePos;
    private double distance, enemyDistance;
    private int blocks;

    public BridgeInfoModule(int key) {
        super("Bridge Info", Category.minigames, key);
    }

    public void link() {
        editPosition = (ButtonSetting) ModuleManager.setting(this, "Edit position");
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == editPosition) {
            Minecraft.getMinecraft().displayGuiScreen(new BridgeInfoEditor());
        }
    }

    private boolean inBridge() {
        if (Hypixel.onHypixel()) {
            for (String s : SumoFencesModule.scoreLines()) {
                String l = s.toLowerCase();
                if (l.contains(MODE) && l.contains(BRIDGE)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.END && Overlay.canRender() && this.inBridge()) {
            mc.fontRendererObj.drawString(ENEMY + this.enemy, (float) posX, (float) posY, COLOR, true);
            mc.fontRendererObj.drawString(DISTANCE + this.distance, (float) posX, (float) (posY + 11), COLOR, true);
            mc.fontRendererObj.drawString(ENEMY_DISTANCE + this.enemyDistance, (float) posX, (float) (posY + 22), COLOR, true);
            mc.fontRendererObj.drawString(BLOCKS + this.blocks, (float) posX, (float) (posY + 33), COLOR, true);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Chat.inWorld()) {
            return;
        }
        String s = Chat.strip(e.message.getUnformattedText());
        if (s.isEmpty()) {
            return;
        }
        if (s.charAt(0) == ' ') {
            if (s.contains(START)) {
                this.awaitingOpponent = true;
            } else if (this.awaitingOpponent && s.contains(OPPONENT + ":")) {
                String name = s.split(":")[1].trim();
                if (name.contains("[")) {
                    name = name.split("] ")[1];
                }
                this.enemy = name;
                this.awaitingOpponent = false;
            }
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == Minecraft.getMinecraft().thePlayer) {
            this.reset();
        }
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (this.enemy.isEmpty() || !this.inBridge()) {
            return;
        }
        EntityPlayer opponent = null;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer) {
                if (!entity.getName().equals(this.enemy)) continue;
                opponent = (EntityPlayer) entity;
                continue;
            }
            if (!(entity instanceof EntityArmorStand)) continue;
            if (entity.getName().contains(DEFEND)) {
                this.defendPos = entity.getPosition();
                continue;
            }
            if (!entity.getName().contains(JUMP)) continue;
            this.scorePos = entity.getPosition();
        }
        if (this.defendPos != null && this.scorePos != null) {
            this.distance = Strings.round(mc.thePlayer.getDistance(this.scorePos.getX(), this.scorePos.getY(), this.scorePos.getZ()) - 1.4, 1);
            if (this.distance < 0.0) {
                this.distance = 0.0;
            }
            this.enemyDistance = opponent == null ? 0.0
                    : Strings.round(opponent.getDistance(this.defendPos.getX(), this.defendPos.getY(), this.defendPos.getZ()) - 1.4, 1);
            if (this.enemyDistance < 0.0) {
                this.enemyDistance = 0.0;
            }
        }
        int n = 0;
        for (int i = 0; i < 9; ++i) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s == null || !(s.getItem() instanceof ItemBlock) || !((ItemBlock) s.getItem()).getBlock().equals(Blocks.stained_hardened_clay)) continue;
            n += s.stackSize;
        }
        this.blocks = n;
    }

    private void reset() {
        this.enemy = "";
        this.awaitingOpponent = false;
        this.defendPos = null;
        this.scorePos = null;
        this.distance = 0.0;
        this.enemyDistance = 0.0;
        this.blocks = 0;
    }
}
