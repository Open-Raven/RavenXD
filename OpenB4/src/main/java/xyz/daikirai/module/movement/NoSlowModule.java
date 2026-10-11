package xyz.daikirai.module.movement;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.player.NoRotateModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PacketQueue;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoSlowModule extends Module {
    public static NoSlowModule instance;
    public static SliderSetting mode, slow;
    public static ButtonSetting disableSprint, bow, edibles, potions, sword;

    public enum Mode {
        VANILLA, BLINK, GROUND, JUMP, FLOAT, INTERACT, INVALID, SNEAK;
        public int state;
    }

    public static Mode current = Mode.VANILLA;
    public static int useTicks;
    public static long sinceUse = 10000L;
    public static float tickSlow = -1.0f;
    public static boolean forceVanilla;
    private static boolean blinking;
    private static int blinkTicks;
    private static C08PacketPlayerBlockPlacement heldPlace;
    private static boolean placedOnBlock;
    public static int interactTicks;
    private static float interactYaw, interactPitch;
    private static boolean jumped;
    public static boolean sneaking;
    public static int lastSlowTick = -1;

    public NoSlowModule(int key) {
        super("NoSlow", Category.movement, key);
        instance = this;
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    public void link() {
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
        slow = (SliderSetting) ModuleManager.setting(this, "Slow");
        disableSprint = (ButtonSetting) ModuleManager.setting(this, "Disable sprint");
        bow = (ButtonSetting) ModuleManager.setting(this, "Bow");
        edibles = (ButtonSetting) ModuleManager.setting(this, "Edibles");
        potions = (ButtonSetting) ModuleManager.setting(this, "Potions");
        sword = (ButtonSetting) ModuleManager.setting(this, "Sword");
    }

    @Override
    public void onPostMotion() {
        if (current == Mode.INTERACT && placedOnBlock) {
            placedOnBlock = false;
        }
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = mc();
        boolean using = useTicks > 0;
        switch (current) {
            case FLOAT: {
                if (useTicks > 1 && heldPlace != null) {
                    PacketUtils.sendThroughLag(heldPlace);
                    heldPlace = null;
                }
                if (sinceUse <= 3L) {
                    if (!BlockUtils.is(BlockUtils.below(0.0), Blocks.air) || BlockUtils.isOpenStair(BlockUtils.below(0.5))) {
                        current.state = 0;
                    } else if (current.state == 0 && PlayerUtils.airTicks >= 3L) {
                        current.state = 1;
                    }
                    if (current.state == 1) {
                        if (!mc.thePlayer.onGround || !Hypixel.onHypixel()) break;
                        e.y += 0.001 + Double.parseDouble("0.0000" + PlayerUtils.idOffset());
                        break;
                    }
                    tickSlow = 0.3f;
                    break;
                }
                current.state = 0;
                break;
            }
            case SNEAK: {
                if (using) {
                    sneaking = true;
                    if (mc.thePlayer.onGround && !PlayerUtils.inLiquid()) {
                        PlayerUtils.jump();
                        double d = 0.4 + PlayerUtils.perSpeedLevel(0.03);
                        MoveUtils.strafe(d);
                    }
                } else {
                    sneaking = false;
                }
                if (!sneaking) break;
                e.sprinting = false;
                e.sneaking = true;
                break;
            }
            default:
                break;
        }
    }

    public static boolean usingSlowedItem() {
        Minecraft mc = mc();
        if (!mc.thePlayer.isUsingItem() || !PacketUtils.noUseOrDig()) {
            return false;
        }
        if (mc.thePlayer.inventory.currentItem != PacketUtils.serverSlot) {
            return false;
        }
        return affects(mc.thePlayer.getHeldItem());
    }

    public static boolean affects(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.getItem();
        if (edibles.enabled && item instanceof ItemFood || potions.enabled && (item instanceof ItemPotion && !ItemPotion.isSplash(item.getMetadata(itemStack)) || item instanceof ItemBucketMilk)) {
            return true;
        }
        if (!bow.enabled || !(item instanceof ItemBow)) {
            return false;
        }
        if (!mc().thePlayer.inventory.hasItem(Items.arrow) && !PlayerUtils.isCreative()) {
            return false;
        }
        return !itemStack.getDisplayName().startsWith(Chat.COLOR + "aKit Select");
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        if (interactTicks > 0) {
            return new float[]{interactYaw, interactPitch};
        }
        return null;
    }

    public static boolean slowdown() {
        Minecraft mc = mc();
        int n = mc.thePlayer.ticksExisted;
        if (xyz.daikirai.module.combat.KillAuraModule.slowedThisTick && lastSlowTick == n) {
            return false;
        }
        if (lastSlowTick == n) {
            Chat.print(xyz.daikirai.util.MathUtils.timeDigits() + " &cnoslow e1");
        }
        lastSlowTick = n;
        boolean vanilla = ModuleManager.enabled("NoSlow") == null;
        if (!vanilla) {
            ItemStack itemStack = mc.thePlayer.getHeldItem();
            if (itemStack == null) {
                vanilla = true;
            } else if (itemStack.getItem() instanceof ItemSword) {
                vanilla = !sword.enabled;
            } else {
                vanilla = !affects(itemStack);
            }
        }
        if (vanilla) {
            mc.thePlayer.movementInput.moveStrafe *= 0.2f;
            mc.thePlayer.movementInput.moveForward *= 0.2f;
        } else {
            float f = (100.0f - (float) slow.getInput()) / 100.0f;
            if (tickSlow != -1.0f) {
                if ((useTicks > 0 || forceVanilla) && (f = tickSlow) == 0.2f) {
                    vanilla = true;
                }
                tickSlow = -1.0f;
            } else if (forceVanilla) {
                f = 0.2f;
                vanilla = true;
            }
            mc.thePlayer.movementInput.moveStrafe *= f;
            mc.thePlayer.movementInput.moveForward *= f;
            forceVanilla = false;
        }
        return vanilla;
    }

    @Override
    public boolean onPlayerSlowdown() {
        slowdown();
        return true;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().thePlayer) {
            lastSlowTick = -1;
        }
    }

    @Override
    public void onDisable() {
        interactTicks = 0;
        blinkTicks = 0;
        useTicks = 0;
        forceVanilla = false;
        jumped = false;
        sneaking = false;
        placedOnBlock = false;
        interactPitch = 0.0f;
        interactYaw = 0.0f;
        heldPlace = null;
        if (blinking) {
            blinking = false;
            PacketQueue.close();
        }
        for (Mode m : Mode.values()) {
            m.state = 0;
        }
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = mc();
        boolean using = usingSlowedItem();
        if (using) {
            ++useTicks;
            sinceUse = 0L;
        } else {
            useTicks = 0;
            ++sinceUse;
        }
        switch (current) {
            case BLINK: {
                if (using) {
                    if (useTicks < 14) {
                        tickSlow = 0.2f;
                        break;
                    }
                    if (useTicks == 14) {
                        blinking = true;
                        PacketQueue.open();
                        break;
                    }
                    if (useTicks < 34 || !blinking) break;
                    blinking = false;
                    blinkTicks = 0;
                    PacketQueue.close();
                    break;
                }
                if (!blinking) break;
                blinking = false;
                blinkTicks = 0;
                PacketQueue.close();
                break;
            }
            case GROUND: {
                if (!using || current.state != 1 || !mc.thePlayer.onGround) break;
                tickSlow = 0.3f;
                break;
            }
            case JUMP: {
                if (useTicks <= 1) break;
                if (heldPlace != null) {
                    PacketUtils.sendThroughLag(heldPlace);
                    heldPlace = null;
                }
                if (!mc.thePlayer.onGround) break;
                tickSlow = 0.3f;
                break;
            }
            case INTERACT: {
                MovingObjectPosition hit;
                if (interactTicks <= 0 || ++interactTicks < 3) break;
                interactTicks = 0;
                if (!mc.thePlayer.isUsingItem() || (hit = PlayerUtils.rayTraceBlocks(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, 90.0f)) == null
                        || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) break;
                PlayerUtils.sendPlace(hit, mc.thePlayer.getHeldItem());
                break;
            }
            case INVALID: {
                if (!using || !PacketUtils.noAttackPlaceOrDig() || mc.thePlayer.ticksExisted % 3 != 0) break;
                if (Hypixel.onHypixel()) {
                    ItemStack itemStack = null;
                    PacketUtils.sendSkipScripts(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, itemStack, 0.0f, 0.0f, 0.0f));
                    break;
                }
                PacketUtils.sendSkipScripts(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                break;
            }
            default:
                break;
        }
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (packet instanceof C07PacketPlayerDigging) {
            return current != Mode.FLOAT || heldPlace == null || ((C07PacketPlayerDigging) packet).getStatus() != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            return onPlace((C08PacketPlayerBlockPlacement) packet);
        }
        return true;
    }

    private static boolean onPlace(C08PacketPlayerBlockPlacement p) {
        Minecraft mc = mc();
        boolean affected = affects(p.getStack());
        switch (current) {
            case GROUND: {
                if (!affected || p.getPlacedBlockDirection() != 255) break;
                if (mc.thePlayer.onGround || PlayerUtils.airTicks == 11L && PlayerUtils.groundWithin(0.2)) {
                    current.state = 0;
                    break;
                }
                current.state = 1;
                break;
            }
            case JUMP:
            case FLOAT: {
                if (!affected || PlayerUtils.isCreative() || PlayerUtils.inLiquid()) break;
                boolean hold = true;
                MovingObjectPosition hit = BotCheck.rayTraceBlocks(mc.playerController.getBlockReachDistance());
                if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && BlockUtils.isInteractable(BlockUtils.getBlock(hit.getBlockPos()))) {
                    hold = false;
                }
                Entity entity;
                if (hold && Hypixel.bedwars(2) && (entity = BotCheck.lookingAt(3.0)) != null) {
                    if (entity instanceof EntityPlayer) {
                        if (BotCheck.isBot(entity, false)) {
                            hold = false;
                        }
                    } else if (!(entity instanceof EntityIronGolem) && !(entity instanceof EntitySilverfish)) {
                        hold = false;
                    }
                }
                if (!hold) {
                    return true;
                }
                if (p.getPlacedBlockDirection() == 255) {
                    if (!mc.thePlayer.onGround) {
                        current.state = 1;
                        return true;
                    }
                    if (sinceUse > 3L) {
                        PlayerUtils.jump();
                        if (PlayerUtils.isMoving() && !mc.thePlayer.isSneaking() && !NoRotateModule.sinceSetback(3000L) && PlayerUtils.direction().forward) {
                            MoveUtils.strafe(0.48 + xyz.daikirai.util.MathUtils.randomTimes(0.001));
                        }
                        jumped = true;
                        current.state = 1;
                    }
                    heldPlace = p;
                }
                return false;
            }
            case INTERACT: {
                if (!affected) break;
                if (p.getPlacedBlockDirection() < 255) {
                    placedOnBlock = true;
                    break;
                }
                if (!placedOnBlock) {
                    MovingObjectPosition hit = mc.objectMouseOver;
                    if (isBlockHit(hit)) {
                        PlayerUtils.sendPlace(hit, p.getStack());
                    } else {
                        hit = findBlock();
                        if (hit == null) {
                            return true;
                        }
                        interactTicks = 1;
                    }
                }
                return false;
            }
            default:
                break;
        }
        return true;
    }

    @Override
    public void onPostPlayerInput() {
        Minecraft mc = mc();
        if (useTicks > 0 && current == Mode.GROUND) {
            PlayerUtils.setJump(false);
        }
        if (jumped) {
            jumped = false;
            PlayerUtils.setJump(false);
        } else if (current == Mode.JUMP && useTicks > 1 && mc.thePlayer.onGround) {
            PlayerUtils.setJump(false);
        }
    }

    private static MovingObjectPosition findBlock() {
        Minecraft mc = mc();
        MovingObjectPosition hit = null;
        outer:
        for (float pitch = 90.0f; pitch >= -90.0f; pitch -= 15.0f) {
            float yaw = mc.thePlayer.rotationYaw;
            if (Math.abs(pitch) >= 80.0f) {
                hit = PlayerUtils.rayTraceBlocks(mc.playerController.getBlockReachDistance(), yaw, pitch);
                if (!isBlockHit(hit)) continue;
                interactYaw = yaw;
                interactPitch = pitch;
                break;
            }
            for (float f = yaw; f < yaw + 360.0f; f += 20.0f) {
                hit = PlayerUtils.rayTraceBlocks(mc.playerController.getBlockReachDistance(), f, pitch);
                if (!isBlockHit(hit)) continue;
                interactYaw = f;
                interactPitch = pitch;
                break outer;
            }
        }
        return hit;
    }

    @Override
    public void guiUpdate() {
        if (current.ordinal() != (int) mode.getInput()) {
            if (this.isEnabled()) {
                this.onDisable();
            }
            current = Mode.values()[(int) mode.getInput()];
        }
    }

    private static boolean isBlockHit(MovingObjectPosition hit) {
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }
}
