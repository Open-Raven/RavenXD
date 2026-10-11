package xyz.daikirai.util;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Friends;
import xyz.daikirai.client.Sounds;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.gui.Console;
import xyz.daikirai.mixin.IEntityPlayerSP;
import xyz.daikirai.mixin.IMinecraft;
import xyz.daikirai.mixin.IPlayerControllerMP;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public final class PlayerUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean attacking;
    public static boolean sendingSlot;
    public static long lastPearl;
    public static long lastFireball;
    public static long lastFireballOnBlock;
    public static long groundTicks;
    public static long airTicks;
    public static int bhopJumpTick;
    public static final double FALL_MOTION = -0.0784000015258789;
    public static final double EPSILON = 1.0E-13;
    public static final double SMALL = 0.001;

    private PlayerUtils() {
    }

    public static void onPlayerUpdate() {
        RenderUtils.updateResolution();
        Module killAura = ModuleManager.get("KillAura");
        if (killAura != null && killAura.consumeSlotResync()) {
            sendSlot(mc.thePlayer.inventory.currentItem);
        }
        if (!ModuleManager.afterDisableQueue.isEmpty()) {
            for (Module m : ModuleManager.afterDisableQueue) {
                m.afterDisable();
            }
            ModuleManager.afterDisableQueue.clear();
        }
    }

    public static void onPreMotion(MotionEvent e) {
        if (mc.thePlayer.onGround) {
            ++groundTicks;
            airTicks = 0L;
        } else {
            groundTicks = 0L;
            ++airTicks;
        }
        if (bhopJumpTick > 0) {
            if (mc.thePlayer.motionY == 0.21932400394947962) {
                mc.thePlayer.motionY -= 0.13;
                bhopJumpTick = 0;
            } else if (Math.abs(mc.thePlayer.ticksExisted - bhopJumpTick) > 2) {
                bhopJumpTick = 0;
            }
        }
    }

    public static void onPlace(C08PacketPlayerBlockPlacement p) {
        if (p.getStack() != null) {
            Item item = p.getStack().getItem();
            if (p.getPlacedBlockDirection() == 255 && item == Items.ender_pearl) {
                Module skyWars = ModuleManager.enabled("Sky Wars");
                if (skyWars != null) {
                    skyWars.onPearlThrown(p.getStack());
                }
                lastPearl = System.currentTimeMillis();
            }
            if (item == Items.fire_charge) {
                lastFireball = System.currentTimeMillis();
                if (p.getPlacedBlockDirection() != 255) {
                    lastFireballOnBlock = lastFireball;
                }
            }
        }
    }

    public static boolean isAirBelow(double x, double y, double z) {
        BlockPos pos = new BlockPos(x, y - (double) (y % 1.0 == 0.0 ? 1 : 0), z);
        return mc.theWorld.isAirBlock(pos);
    }

    public static boolean isAirBelow(Entity entity) {
        return isAirBelow(entity.posX, entity.posY, entity.posZ);
    }

    public static boolean isAirBelow() {
        return isAirBelow(mc.thePlayer);
    }

    public static boolean nearEdge(double margin) {
        int x = MathHelper.floor_double(mc.thePlayer.posX);
        int y = MathHelper.floor_double(mc.thePlayer.posY - 0.01);
        int z = MathHelper.floor_double(mc.thePlayer.posZ);
        BlockPos pos = new BlockPos(x, y, z);
        if (mc.theWorld.isAirBlock(pos)) {
            return true;
        }
        double fx = mc.thePlayer.posX - (double) x;
        double fz = mc.thePlayer.posZ - (double) z;
        return fx < margin && mc.theWorld.isAirBlock(new BlockPos(x - 1, y, z)) || fx > 1.0 - margin && mc.theWorld.isAirBlock(new BlockPos(x + 1, y, z)) || fz < margin && mc.theWorld.isAirBlock(new BlockPos(x, y, z - 1)) || fz > 1.0 - margin && mc.theWorld.isAirBlock(new BlockPos(x, y, z + 1));
    }

    public static double heightAboveGround(double x, double y, double z) {
        MovingObjectPosition hit = rayTraceFrom(100.0, x, y, z, 0.0f, 90.0f);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return -1.0;
        }
        Block block = BlockUtils.getBlock(hit.getBlockPos());
        if (block.getMaterial().isSolid() || block == Blocks.carpet || block == Blocks.waterlily) {
            y -= block.getBlockBoundsMaxY();
        }
        return Math.max(y - (double) hit.getBlockPos().getY(), 0.0);
    }

    public static double heightAboveGround(Entity entity) {
        return heightAboveGround(entity.posX, entity.posY, entity.posZ);
    }

    public static double heightAboveGround() {
        return heightAboveGround(mc.thePlayer);
    }

    public static boolean groundWithin(double d) {
        double h = heightAboveGround();
        return h != -1.0 && h < d;
    }

    public static boolean landsWithin(int ticks) {
        if (mc.thePlayer.onGround) {
            return false;
        }
        double h = heightAboveGround();
        if (h < 0.0) {
            return false;
        }
        double fallen = 0.0;
        double motionY = mc.thePlayer.motionY;
        for (int i = 0; i < ticks; ++i) {
            if (motionY < 0.0 && (fallen -= motionY) >= h) {
                return true;
            }
            motionY = (motionY - 0.08) * 0.98;
        }
        return false;
    }

    public static boolean inLiquid() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }

    public static double horizontalDistanceSq(double x, double z) {
        double dx = mc.thePlayer.posX - x;
        double dz = mc.thePlayer.posZ - z;
        return dx * dx + dz * dz;
    }

    public static BlockPos closer(BlockPos a, BlockPos b) {
        return mc.thePlayer.getDistanceSq(BlockUtils.center(a)) > mc.thePlayer.getDistanceSq(BlockUtils.center(b)) ? b : a;
    }

    public static Vec3 eyes(double x, double y, double z) {
        return new Vec3(x, y + (double) mc.thePlayer.getEyeHeight(), z);
    }

    public static int idOffset() {
        int id = Math.abs(mc.thePlayer.getEntityId());
        return id > 999 ? id + 213 : id * 99;
    }

    public static int decimals(double d) {
        return (int) Math.round(d % 1.0 * 10000.0);
    }

    public static int posYDecimals() {
        return decimals(mc.thePlayer.posY);
    }

    public static int motionYDecimals() {
        return (int) Math.round(mc.thePlayer.motionY % 10.0 * 10000.0);
    }

    public static float forwardInput() {
        float f = 0.0f;
        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            f += 1.0f;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            f -= 1.0f;
        }
        return f;
    }

    public static float strafeInput() {
        float f = 0.0f;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            f += 1.0f;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            f -= 1.0f;
        }
        return f;
    }

    public static boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0.0f || mc.thePlayer.movementInput.moveStrafe != 0.0f;
    }

    public static boolean jumpKeyDown() {
        return KeyUtils.isDown(mc.gameSettings.keyBindJump);
    }

    public static void setJump(boolean jump) {
        mc.thePlayer.movementInput.jump = jump;
    }

    public static void setSneak(boolean sneak) {
        boolean was = mc.thePlayer.movementInput.sneak;
        mc.thePlayer.movementInput.sneak = sneak;
        if (!was && sneak) {
            setForward((float) ((double) getForward() * 0.3));
            setStrafe((float) ((double) getStrafe() * 0.3));
        }
    }

    public static float getForward() {
        return mc.thePlayer.movementInput.moveForward;
    }

    public static float getStrafe() {
        return mc.thePlayer.movementInput.moveStrafe;
    }

    public static void setForward(float f) {
        mc.thePlayer.movementInput.moveForward = f;
    }

    public static void setStrafe(float f) {
        mc.thePlayer.movementInput.moveStrafe = f;
    }

    public static MoveDirection direction(double forward, double strafe) {
        MoveDirection d = null;
        if (forward == 0.0) {
            if (strafe == 0.0) {
                d = MoveDirection.NONE;
            } else if (strafe > 0.0) {
                d = MoveDirection.LEFT;
            } else if (strafe < 0.0) {
                d = MoveDirection.RIGHT;
            }
        } else if (forward > 0.0) {
            if (strafe == 0.0) {
                d = MoveDirection.FORWARD;
            } else if (strafe > 0.0) {
                d = MoveDirection.FORWARD_LEFT;
            } else if (strafe < 0.0) {
                d = MoveDirection.FORWARD_RIGHT;
            }
        } else if (forward < 0.0) {
            if (strafe == 0.0) {
                d = MoveDirection.BACK;
            } else if (strafe > 0.0) {
                d = MoveDirection.BACK_LEFT;
            } else if (strafe < 0.0) {
                d = MoveDirection.BACK_RIGHT;
            }
        }
        return d;
    }

    public static MoveDirection direction() {
        return direction(forwardInput(), strafeInput());
    }

    public static MoveDirection direction(float yaw) {
        float moveYaw = RotationManager.movementYaw();
        if (yaw == moveYaw) {
            return direction();
        }
        double rad = Math.toRadians(yaw - moveYaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double forward = mc.thePlayer.movementInput.moveForward;
        double strafe = mc.thePlayer.movementInput.moveStrafe;
        double f = Math.round(forward * cos - strafe * sin);
        double s = Math.round(forward * sin + strafe * cos);
        return direction(f, s);
    }

    public static MoveDirection direction(MotionEvent e) {
        return direction(e.yaw);
    }

    public static boolean diagonalYaw() {
        float f = Math.abs(mc.thePlayer.rotationYaw % 90.0f);
        return f > 10.0f && f < 80.0f;
    }

    public static boolean isDiagonal() {
        return diagonalYaw() || direction().diagonal;
    }

    public static float movementAngle() {
        return mc.thePlayer.rotationYaw + (float) RotationUtils.inputAngle();
    }

    public static void jump() {
        mc.thePlayer.jump();
    }

    public static void jump(double speed) {
        MoveUtils.strafe(speed);
        jump();
    }

    public static void multiplyMotion(double d) {
        mc.thePlayer.motionX *= d;
        mc.thePlayer.motionZ *= d;
    }

    public static double[] motionTowards(EntityLivingBase entity) {
        double mx = mc.thePlayer.motionX;
        double mz = mc.thePlayer.motionZ;
        double dx = entity.posX - mc.thePlayer.posX;
        double dz = entity.posZ - mc.thePlayer.posZ;
        double m2 = mx * mx + mz * mz;
        double d2 = dx * dx + dz * dz;
        double speed = Math.sqrt(m2);
        double dot = 0.0;
        if (m2 >= 1.0E-8 && d2 >= 1.0E-8) {
            double im = 1.0 / Math.sqrt(m2);
            double id = 1.0 / Math.sqrt(d2);
            dot = mx * im * (dx * id) + mz * im * (dz * id);
        }
        return new double[]{speed, dot};
    }

    public static void resetSprintToggleTimer() {
        ((IEntityPlayerSP) mc.thePlayer).setSprintToggleTimer(0);
    }

    public static int potionLevel(Potion potion) {
        PotionEffect effect = mc.thePlayer.getActivePotionEffect(potion);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    public static int speedLevel() {
        return potionLevel(Potion.moveSpeed);
    }

    public static int miningFatigueLevel() {
        return potionLevel(Potion.digSlowdown);
    }

    public static double perSpeedLevel(double d) {
        return (double) speedLevel() * d;
    }

    public static int currentSlot() {
        return mc.thePlayer.inventory.currentItem;
    }

    public static void setSlot(int slot, boolean sync) {
        mc.thePlayer.inventory.currentItem = slot;
        if (sync) {
            syncCurrentPlayItem();
        }
    }

    public static void sendSlot(int slot) {
        sendingSlot = true;
        PacketUtils.send(new C09PacketHeldItemChange(slot));
        sendingSlot = false;
    }

    public static void syncCurrentPlayItem() {
        ((IPlayerControllerMP) mc.playerController).invokeSyncCurrentPlayItem();
    }

    public static boolean holding(Item item) {
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        return itemStack != null && itemStack.getItem() == item;
    }

    public static boolean holdingSword() {
        return ItemUtils.isSword(mc.thePlayer.getHeldItem());
    }

    public static boolean holdingWeapon(EntityLivingBase entity) {
        return ItemUtils.isWeapon(entity.getHeldItem());
    }

    public static boolean holdingWeapon() {
        return holdingWeapon(mc.thePlayer);
    }

    public static boolean holdingScaffoldBlock() {
        return ItemUtils.isScaffoldBlock(mc.thePlayer.getHeldItem());
    }

    public static boolean holdingBlock(Block block) {
        return ItemUtils.isBlock(mc.thePlayer.getHeldItem(), block);
    }

    public static boolean holdingBlock() {
        return ItemUtils.isBlock(mc.thePlayer.getHeldItem());
    }

    public static boolean holdingThrowable() {
        return ItemUtils.isThrowable(mc.thePlayer.getHeldItem());
    }

    public static boolean holdingTool() {
        return ItemUtils.isTool(mc.thePlayer.getHeldItem());
    }

    public static boolean holdingSharpness() {
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        return itemStack != null && EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) > 0;
    }

    public static boolean isEating(EntityPlayer player) {
        if (mc.thePlayer.isUsingItem()) {
            EnumAction action = player.getItemInUse().getItem().getItemUseAction(player.getItemInUse());
            return action == EnumAction.EAT || action == EnumAction.DRINK;
        }
        return false;
    }

    public static boolean isBlocking() {
        Module killAura = ModuleManager.get("KillAura");
        if (killAura != null && killAura.isFakeBlocking() && holdingSword()) {
            return true;
        }
        return mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse().getItem().getItemUseAction(mc.thePlayer.getItemInUse()) == EnumAction.BLOCK;
    }

    public static void clearEmptyStacks() {
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);
            if (itemStack == null || itemStack.stackSize > 0) continue;
            mc.thePlayer.inventory.setInventorySlotContents(i, null);
        }
    }

    public static void swing(boolean packetOnly, boolean resetEquip) {
        if (packetOnly) {
            PacketUtils.send(new C0APacketAnimation());
        } else {
            mc.thePlayer.swingItem();
            if (resetEquip) {
                mc.getItemRenderer().resetEquippedProgress();
            }
        }
    }

    public static void attack(Entity entity, boolean packetSwing) {
        attacking = true;
        swing(packetSwing, false);
        mc.playerController.attackEntity(mc.thePlayer, entity);
        attacking = false;
        ClickUtils.onAttackClick();
    }

    public static void clickMouse() {
        ((IMinecraft) mc).invokeClickMouse();
    }

    public static void rightClickMouse() {
        ((IMinecraft) mc).invokeRightClickMouse();
    }

    public static void restartSwing() {
        EntityPlayerSP p = mc.thePlayer;
        int n = p.isPotionActive(Potion.digSpeed) ? 6 - (1 + p.getActivePotionEffect(Potion.digSpeed).getAmplifier()) : (p.isPotionActive(Potion.digSlowdown) ? 6 + (1 + p.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);
        if (!p.isSwingInProgress || p.swingProgressInt >= n / 2 || p.swingProgressInt < 0) {
            p.swingProgressInt = -1;
            p.isSwingInProgress = true;
        }
    }

    public static void sendChatMessage(String s) {
        mc.thePlayer.sendChatMessage(s);
    }

    public static void sendPlace(MovingObjectPosition hit, ItemStack itemStack) {
        float fx = (float) (hit.hitVec.xCoord - (double) hit.getBlockPos().getX());
        float fy = (float) (hit.hitVec.yCoord - (double) hit.getBlockPos().getY());
        float fz = (float) (hit.hitVec.zCoord - (double) hit.getBlockPos().getZ());
        PacketUtils.send(new C08PacketPlayerBlockPlacement(hit.getBlockPos(), hit.sideHit.getIndex(), itemStack, fx, fy, fz));
    }

    public static void place(MovingObjectPosition hit, ItemStack itemStack) {
        float fx = (float) (hit.hitVec.xCoord - (double) hit.getBlockPos().getX());
        float fy = (float) (hit.hitVec.yCoord - (double) hit.getBlockPos().getY());
        float fz = (float) (hit.hitVec.zCoord - (double) hit.getBlockPos().getZ());
        PacketUtils.send(new C08PacketPlayerBlockPlacement(hit.getBlockPos(), hit.sideHit.getIndex(), itemStack, fx, fy, fz));
        if (!isCreative()) {
            itemStack.onItemUse(mc.thePlayer, mc.theWorld, hit.getBlockPos(), hit.sideHit, fx, fy, fz);
            if (itemStack.stackSize <= 0) {
                ForgeEventFactory.onPlayerDestroyItem(mc.thePlayer, itemStack);
            }
        }
    }

    public static double blockReach() {
        return mc.playerController.getBlockReachDistance();
    }

    public static double vanillaReach() {
        return mc.playerController.extendedReach() ? 6.0 : 3.0;
    }

    public static double attackReach() {
        if (mc.playerController.extendedReach()) {
            return 6.0;
        }
        Module reach = ModuleManager.enabled("Reach");
        if (reach != null) {
            return MathUtils.randomBetween((SliderSetting) ModuleManager.setting(reach, "Min"), (SliderSetting) ModuleManager.setting(reach, "Max"));
        }
        return 3.0;
    }

    public static boolean isCreative() {
        return mc.thePlayer.capabilities.isCreativeMode;
    }

    public static boolean isAdventure() {
        return !mc.thePlayer.capabilities.allowEdit;
    }

    public static EnumFacing facing() {
        return mc.thePlayer.getHorizontalFacing();
    }

    public static MovingObjectPosition rayTraceFrom(double range, double x, double y, double z, float yaw, float pitch) {
        Vec3 from = eyes(x, y, z);
        Vec3 dir = RotationUtils.getVectorForRotation(yaw, pitch);
        Vec3 to = from.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        return mc.theWorld.rayTraceBlocks(from, to, false, false, false);
    }

    public static MovingObjectPosition rayTraceBlocks(double range, float yaw, float pitch) {
        return rayTraceFrom(range, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, yaw, pitch);
    }

    public static MovingObjectPosition rayTraceBlock(BlockPos pos, EnumFacing facing, double range, float yaw, float pitch) {
        MovingObjectPosition hit = rayTraceBlocks(range, yaw, pitch);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        if (!BlockUtils.samePos(hit.getBlockPos(), pos) || facing != null && hit.sideHit != facing) {
            return null;
        }
        return hit;
    }

    public static boolean canReach(BlockPos pos, double range) {
        float[] r = RotationUtils.toBlock(pos);
        Vec3 from = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = RotationUtils.getVectorForRotation(r[0], r[1]);
        Vec3 to = from.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        AxisAlignedBB box = BlockUtils.getBlock(pos).getSelectedBoundingBox(mc.theWorld, pos);
        return box != null && box.calculateIntercept(from, to) != null;
    }

    public static boolean rayHits(Entity entity, AxisAlignedBB box, float yaw, float pitch, double range) {
        if (entity == null || !entity.canBeCollidedWith()) {
            return false;
        }
        Vec3 from = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = RotationUtils.getVectorForRotation(yaw, pitch);
        Vec3 to = from.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        float f = entity.getCollisionBorderSize();
        box = box.expand(f, f, f);
        return box.calculateIntercept(from, to) != null;
    }

    public static boolean rayHits(Entity entity, float yaw, float pitch, double range) {
        if (entity == null) {
            return false;
        }
        return rayHits(entity, entity.getEntityBoundingBox(), yaw, pitch, range);
    }

    public static boolean rayHits(Entity entity, double range) {
        float[] r = RotationUtils.toEntityBox(entity);
        return rayHits(entity, r[0], r[1], range);
    }

    public static double rayDistanceSq(Entity entity, AxisAlignedBB box, float yaw, float pitch, double range) {
        if (entity == null || !entity.canBeCollidedWith()) {
            return -1.0;
        }
        Vec3 from = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = RotationUtils.getVectorForRotation(yaw, pitch);
        Vec3 to = from.addVector(dir.xCoord * range, dir.yCoord * range, dir.zCoord * range);
        float f = entity.getCollisionBorderSize();
        box = box.expand(f, f, f);
        MovingObjectPosition hit = box.calculateIntercept(from, to);
        if (hit == null) {
            return -1.0;
        }
        return from.squareDistanceTo(hit.hitVec);
    }

    public static double rayDistanceSq(Entity entity, float yaw, float pitch, double range) {
        if (entity == null) {
            return -1.0;
        }
        return rayDistanceSq(entity, entity.getEntityBoundingBox(), yaw, pitch, range);
    }

    public static double rayDistanceSq(Entity entity, double range) {
        float[] r = RotationUtils.toEntityBox(entity);
        return rayDistanceSq(entity, r[0], r[1], range);
    }

    public static double rayDistanceSq(Entity entity, double range, boolean toEntity) {
        float yaw;
        float pitch;
        if (toEntity) {
            float[] r = RotationUtils.toEntityBox(entity);
            yaw = r[0];
            pitch = r[1];
        } else {
            yaw = mc.thePlayer.rotationYaw;
            pitch = mc.thePlayer.rotationPitch;
        }
        Vec3 from = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = RotationUtils.getVectorForRotation(yaw, pitch);
        double dx = dir.xCoord * range;
        double dy = dir.yCoord * range;
        double dz = dir.zCoord * range;
        Vec3 to = from.addVector(dx, dy, dz);
        List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), mc.getRenderViewEntity().getEntityBoundingBox().addCoord(dx, dy, dz).expand(1.0, 1.0, 1.0));
        for (Entity e : list) {
            if (e != entity || !e.canBeCollidedWith()) continue;
            float f = e.getCollisionBorderSize();
            AxisAlignedBB box = e.getEntityBoundingBox().expand(f, f, f);
            MovingObjectPosition hit = box.calculateIntercept(from, to);
            if (hit == null) continue;
            return from.squareDistanceTo(hit.hitVec);
        }
        return -1.0;
    }

    public static boolean lookingAtInteractable() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos pos = mc.objectMouseOver.getBlockPos();
            return pos != null && BlockUtils.isInteractable(BlockUtils.getBlock(pos));
        }
        return false;
    }

    public static boolean lookingAtBot() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return BotCheck.isBot(mc.objectMouseOver.entityHit, false);
        }
        return false;
    }

    public static boolean facingPlayer(EntityLivingBase entity, float fov) {
        float f2 = RotationUtils.yawBetween(entity.posX, entity.posZ, mc.thePlayer.posX, mc.thePlayer.posZ);
        double d = MathHelper.wrapAngleTo180_double((double) (f2 - entity.rotationYaw));
        return Math.abs(d) <= (double) fov;
    }

    public static boolean awayFromPlayer(EntityLivingBase entity) {
        double d = mc.thePlayer.getDistanceSqToEntity(entity);
        return d > 1.0 && !facingPlayer(entity, 90.0f);
    }

    public static boolean inFov(float fov, double x, double z) {
        fov = (float) ((double) fov * 0.5);
        double d = MathHelper.wrapAngleTo180_double((double) ((mc.thePlayer.rotationYaw - RotationUtils.yawTo(x, z)) % 360.0f));
        if (d > 0.0) {
            return d < (double) fov;
        }
        return d > (double) (-fov);
    }

    public static boolean inFov(float fov, Entity entity) {
        if (fov >= 360.0f) {
            return true;
        }
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        float f = entity.getCollisionBorderSize();
        AxisAlignedBB box = entity.getEntityBoundingBox().expand(f, f, f);
        if (box.isVecInside(eyes)) {
            return true;
        }
        return inFov(fov, entity.posX, entity.posZ);
    }

    public static boolean inFov(float fov, BlockPos pos) {
        if (fov >= 360.0f) {
            return true;
        }
        return inFov(fov, pos.getX(), pos.getZ());
    }

    public static boolean isClicking() {
        Module autoClicker = ModuleManager.enabled("AutoClicker");
        Module killAura = ModuleManager.get("KillAura");
        if (autoClicker != null && ((ButtonSetting) ModuleManager.setting(autoClicker, "Left click")).isToggled()
                || killAura != null && killAura.getTarget() != null && ((ButtonSetting) ModuleManager.setting(killAura, "Require mouse down")).isToggled()) {
            return Mouse.isButtonDown(0);
        }
        return System.currentTimeMillis() - ClickUtils.lastLeftClick < 220L;
    }

    public static boolean matches(Entity entity, int type) {
        if (entity != null) {
            switch (type) {
                case 0:
                    return entity instanceof EntityPlayer;
                case 1:
                    return entity instanceof EntityPlayer || xyz.daikirai.module.render.DamageTagsModule.isMob(entity);
                case 2:
                    return true;
            }
        }
        return false;
    }

    public static boolean targetMatches(int type) {
        Module killAura = ModuleManager.get("KillAura");
        Module aimAssist = ModuleManager.get("AimAssist");
        Entity a = killAura != null ? killAura.getTarget() : null;
        Entity b = aimAssist != null && ((ButtonSetting) ModuleManager.setting(aimAssist, "Silent aim")).isToggled() ? aimAssist.getTarget() : null;
        Entity c = mc.objectMouseOver != null ? mc.objectMouseOver.entityHit : null;
        return matches(a, type) || matches(b, type) || matches(c, type);
    }

    public static boolean isInvisibleUnarmoured(EntityLivingBase entity) {
        if (!entity.isInvisible()) {
            return false;
        }
        for (int i = 0; i <= 4; ++i) {
            if (entity.getEquipmentInSlot(i) == null) continue;
            return false;
        }
        return true;
    }

    public static boolean isFriend(EntityPlayer player) {
        return Friends.isFriend(player.getName());
    }

    public static boolean isEnemy(EntityPlayer player) {
        return Friends.isEnemy(player.getName());
    }

    public static String username() {
        return Console.nick.isEmpty() ? mc.thePlayer.getName() : Console.nick;
    }

    public static String blockHitResult(double damage) {
        int n = (int) Math.round(damage * 1000.0);
        switch (n) {
            case 1904:
            case 2016:
            case 2128:
            case 2240:
            case 2352:
                return "&cUNBLOCKED";
            case 2856:
            case 3024:
            case 3192:
            case 3360:
            case 3528:
                return "&cUNBLOCKED CRIT";
            case 1088:
            case 1152:
            case 1216:
            case 1280:
            case 1344:
                return "&aBLOCKED";
            case 1564:
            case 1656:
            case 1748:
            case 1840:
            case 1932:
                return "&aBLOCKED CRIT";
        }
        return "UNKNOWN";
    }

    public static void ping(float volume, float pitch) {
        Sounds.queue("note.pling", volume, pitch);
    }

    public static void ping() {
        ping(1.0f, 1.0f);
    }

    public static String screenName(GuiScreen screen) {
        if (screen == null) {
            return "";
        }
        if (screen instanceof xyz.daikirai.gui.ClickGui) {
            return "GuiRaven";
        }
        if (screen instanceof xyz.daikirai.gui.HudEditor) {
            return "GuiRavenEdit";
        }
        return screen.getClass().getSimpleName();
    }

    public static Slot chestSlotUnderMouse(GuiChest chest, int rows) {
        int n2 = rows / 9;
        int ySize = 114 + n2 * 18;
        int left = (chest.width - 176) / 2;
        int top = (chest.height - ySize) / 2;
        int mx = Mouse.getEventX() * chest.width / mc.displayWidth;
        int my = chest.height - Mouse.getEventY() * chest.height / mc.displayHeight - 1;
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            int x = mx - left;
            int y = my - top;
            if (x < slot.xDisplayPosition - 1 || x >= slot.xDisplayPosition + 16 + 1 || y < slot.yDisplayPosition - 1 || y >= slot.yDisplayPosition + 16 + 1) continue;
            return slot;
        }
        return null;
    }

    public static List<String> bookPages() {
        if (mc.currentScreen instanceof GuiScreenBook) {
            NBTTagList pages = ReflectionHelper.getPrivateValue(GuiScreenBook.class, (GuiScreenBook) mc.currentScreen, "bookPages", "field_146483_y");
            if (pages != null) {
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < pages.tagCount(); ++i) {
                    list.add(pages.getStringTagAt(i));
                }
                return list;
            }
        }
        return null;
    }
}
