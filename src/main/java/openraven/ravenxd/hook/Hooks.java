package openraven.ravenxd.hook;

import openraven.ravenxd.util.Rots;
import openraven.ravenxd.module.combat.VelocityModule;
import openraven.ravenxd.client.ChatCommands;
import openraven.ravenxd.client.Hypixel;
import openraven.ravenxd.client.Irc;
import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.gui.Console;
import openraven.ravenxd.mixin.IEntityPlayerSP;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.client.Account;
import openraven.ravenxd.script.ScriptEvents;
import openraven.ravenxd.script.ScriptManager;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.SettingsModule;
import openraven.ravenxd.module.render.AntiShuffleModule;
import openraven.ravenxd.module.dev.DebugAcModule;
import openraven.ravenxd.module.dev.TestModule;
import openraven.ravenxd.module.dev.ViewPacketsModule;
import openraven.ravenxd.module.render.CameraModules;
import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.util.PacketQueue;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.util.RenderUtils;
import openraven.ravenxd.util.RotationManager;
import openraven.ravenxd.util.SwapSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeHooks;

public final class Hooks {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private Hooks() {
    }

    private static Module on(String name) {
        return ModuleManager.enabled(name);
    }

    private static Module any(String name) {
        return ModuleManager.get(name);
    }

    public static String renderString(String s) {
        if (s == null) {
            return null;
        }
        Module nameHider = any("Name Hider");
        if (mc.thePlayer != null && nameHider != null && nameHider.isEnabled()) {
            String name = Console.nick.isEmpty() ? mc.thePlayer.getName() : Console.nick;
            if (s.length() >= name.length()) {
                s = s.replace(name, ChatCommands.nameHider);
            }
        }
        Module antiShuffle = AntiShuffleModule.instance;
        if (antiShuffle != null && antiShuffle.isEnabled()) {
            s = s.replace(AntiShuffleModule.OBFUSCATED, "");
        }
        return s;
    }

    public static void onUpdate() {
        preUpdate();
        float[] rotations = RotationManager.request(RotationManager.lastYaw, RotationManager.lastPitch);
        if (rotations == null) {
            RotationManager.rotating = false;
            Rots.stop();
        } else {
            RotationManager.set(rotations);
            RotationManager.rotating = true;
        }
    }

    private static void preUpdate() {
        Hypixel.tick();
        PlayerUtils.onPlayerUpdate();
        Module m;
        if ((m = on("Chat Commands")) != null) {
            ChatCommands.tick();
        }
        String[] enabledOrder = {"Displace", "KillAura", "Scaffold", "SafeWalk", "NoSlow"};
        for (String name : enabledOrder) {
            if ((m = on(name)) != null) m.onPreUpdate();
        }
        if ((m = any("Hitbox")) != null) m.onPreUpdateAlways();
        if ((m = any("AntiFireball")) != null) m.onPreUpdateAlways();
        String[] enabledOrder2 = {"BedAura", "AutoRod", "Long Jump", "InvManager", "WoolWars", "AutoClicker", "AutoRequeue",
                "AutoWho", "Lag Range", "Limbo", "AutoLadder", "Water Bucket", "WTap", "Stasis", "Sprint", "Velocity"};
        for (String name : enabledOrder2) {
            if ((m = on(name)) != null) m.onPreUpdate();
        }
        if (ScriptManager.events != null) {
            ScriptEvents.preUpdate();
        }
    }

    public static void onUpdateWalkingPlayer() {
        EntityPlayerSP p = mc.thePlayer;
        IEntityPlayerSP acc = (IEntityPlayerSP) p;
        try {
            float yaw;
            float pitch;
            if (RotationManager.rotating) {
                yaw = RotationManager.serverYaw;
                pitch = RotationManager.serverPitch;
            } else if (RotationManager.wasRotating) {
                yaw = RotationManager.serverYaw - RotationManager.serverYaw % 360.0f + p.rotationYaw % 360.0f;
                pitch = p.rotationPitch;
                p.renderArmYaw = p.prevRenderArmYaw = yaw;
                p.prevRotationYaw = p.prevRenderArmYaw;
                p.rotationYaw = p.prevRenderArmYaw;
            } else {
                yaw = p.rotationYaw;
                pitch = p.rotationPitch;
            }
            MotionEvent e = new MotionEvent(p.posX, p.getEntityBoundingBox().minY, p.posZ, yaw, pitch, p.onGround, p.isSprinting(), p.isSneaking());
            preMotion(e);
            if (yaw != e.yaw || pitch != e.pitch) {
                RotationManager.serverYaw = yaw = e.yaw;
                RotationManager.serverPitch = pitch = e.pitch;
                RotationManager.rotating = true;
            }
            boolean sprint = e.sprinting;
            if (sprint != acc.getServerSprintState()) {
                if (sprint) {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(p, C0BPacketEntityAction.Action.START_SPRINTING));
                } else {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(p, C0BPacketEntityAction.Action.STOP_SPRINTING));
                }
                acc.setServerSprintState(sprint);
            }
            boolean sneak = e.sneaking;
            if (sneak != acc.getServerSneakState()) {
                if (sneak) {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(p, C0BPacketEntityAction.Action.START_SNEAKING));
                } else {
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(p, C0BPacketEntityAction.Action.STOP_SNEAKING));
                }
                acc.setServerSneakState(sneak);
            }
            Module freecam = any("Freecam");
            if (mc.getRenderViewEntity() == p || freecam != null && freecam.isFreecamActive()) {
                float lastYaw = acc.getLastReportedYaw();
                float lastPitch = acc.getLastReportedPitch();
                if (RotationManager.rotating) {
                    RotationManager.updateBody(yaw);
                }
                RotationManager.wasRotating = RotationManager.rotating;
                RotationManager.setLast(yaw, pitch, lastPitch);
                double dx = e.x - acc.getLastReportedPosX();
                double dy = e.y - acc.getLastReportedPosY();
                double dz = e.z - acc.getLastReportedPosZ();
                double dyaw = yaw - lastYaw;
                double dpitch = pitch - lastPitch;
                int ticks = acc.getPositionUpdateTicks();
                boolean moved = dx * dx + dy * dy + dz * dz > 9.0E-4 || ticks >= 20;
                boolean rotated = dyaw != 0.0 || dpitch != 0.0;
                if (p.ridingEntity == null) {
                    if (moved && rotated) {
                        p.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(e.x, e.y, e.z, yaw, pitch, e.onGround));
                    } else if (moved) {
                        p.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(e.x, e.y, e.z, e.onGround));
                    } else if (rotated) {
                        p.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, e.onGround));
                    } else {
                        p.sendQueue.addToSendQueue(new C03PacketPlayer(e.onGround));
                    }
                } else {
                    p.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(p.motionX, -999.0, p.motionZ, yaw, pitch, e.onGround));
                    moved = false;
                }
                if (moved) {
                    acc.setLastReportedPosX(e.x);
                    acc.setLastReportedPosY(e.y);
                    acc.setLastReportedPosZ(e.z);
                    acc.setPositionUpdateTicks(0);
                } else {
                    acc.setPositionUpdateTicks(ticks + 1);
                }
                if (rotated) {
                    acc.setLastReportedYaw(yaw);
                    acc.setLastReportedPitch(pitch);
                }
                postMotion();
            }
        } catch (Exception exception) {
        }
    }

    private static void preMotion(MotionEvent e) {
        PlayerUtils.onPreMotion(e);
        Module m;
        if ((m = on("WTap")) != null) m.onPreMotion(e);
        if ((m = on("BedAura")) != null) m.onPreMotion(e);
        if ((m = any("Tower")) != null) m.onPreMotionAlways(e);
        String[] order = {"Scaffold", "NoSlow", "InvMove", "Bhop", "Long Jump", "Speed", "AutoPlace", "Freecam", "NoFall", "Disabler",
                "Speed Builders", "Bed Wars", "AntiCheat", "Duels Stats"};
        for (String name : order) {
            if ((m = on(name)) != null) m.onPreMotion(e);
        }
        if (ScriptManager.events != null) {
            ScriptEvents.preMotion(e);
        }
        if ((m = on("Sprint")) != null) m.onPreMotion(e);
    }

    private static void postMotion() {
        String[] order = {"Displace", "KillAura", "NoSlow", "InvMove", "AutoBlock"};
        Module m;
        for (String name : order) {
            if ((m = on(name)) != null) m.onPostMotion();
        }
        if (ScriptManager.events != null) {
            ScriptEvents.postMotion();
        }
    }

    public static void onPlayerSlowdown() {
        Module noSlow = any("NoSlow");
        if (noSlow != null && noSlow.onPlayerSlowdown()) {
            return;
        }
        mc.thePlayer.movementInput.moveStrafe *= 0.2f;
        mc.thePlayer.movementInput.moveForward *= 0.2f;
    }

    public static void onUpdateEntityActionState() {
        Module m;
        if ((m = on("Jump Reset")) != null) m.onUpdateEntityActionState();
    }

    public static void postPlayerInput() {
        Module m;
        if ((m = any("Scaffold")) != null) m.onPostPlayerInputAlways();
        if ((m = any("Tower")) != null) m.onPostPlayerInputAlways();
        String[] order = {"Freecam", "KillAura", "Bhop", "NoSlow", "SafeWalk", "Long Jump", "Dolphin", "AntiAFK", "InvMove",
                "TargetStrafe", "Stasis", "Velocity"};
        for (String name : order) {
            if ((m = on(name)) != null) m.onPostPlayerInput();
        }
        if (ScriptManager.events != null) {
            ScriptEvents.postPlayerInput();
        }
        if (!RotationManager.movementFix() || !PlayerUtils.isMoving()) {
            return;
        }
        float target = MathHelper.wrapAngleTo180_float(MoveUtils.moveYaw(mc.thePlayer.rotationYaw));
        float bestForward = 0.0f;
        float bestStrafe = 0.0f;
        float best = -1.0f;
        for (float forward = -1.0f; forward <= 1.0f; forward += 1.0f) {
            for (float strafe = -1.0f; strafe <= 1.0f; strafe += 1.0f) {
                if (forward == 0.0f && strafe == 0.0f) continue;
                float yaw = MathHelper.wrapAngleTo180_float(MoveUtils.moveYaw(RotationManager.serverYaw, forward, strafe));
                float diff = Math.abs(MathHelper.wrapAngleTo180_float(target - yaw));
                if (best != -1.0f && !(diff < best)) continue;
                best = diff;
                bestForward = forward;
                bestStrafe = strafe;
            }
        }
        float speed = Math.max(Math.abs(mc.thePlayer.movementInput.moveForward), Math.abs(mc.thePlayer.movementInput.moveStrafe));
        mc.thePlayer.movementInput.moveForward = bestForward * speed;
        mc.thePlayer.movementInput.moveStrafe = bestStrafe * speed;
    }

    public static boolean onJump(EntityLivingBase entity) {
        if (entity != mc.thePlayer) {
            return false;
        }
        mc.thePlayer.motionY = 0.42f;
        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            mc.thePlayer.motionY += (double) ((float) (mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1f);
        }
        if (mc.thePlayer.isSprinting()) {
            float f = RotationManager.movementYaw() * ((float) Math.PI / 180);
            float f2 = 0.2f;
            mc.thePlayer.motionX -= (double) (MathHelper.sin(f) * f2);
            mc.thePlayer.motionZ += (double) (MathHelper.cos(f) * f2);
        }
        mc.thePlayer.isAirBorne = true;
        ForgeHooks.onLivingJump(mc.thePlayer);
        return true;
    }

    public static boolean stopOnEdge(Entity entity) {
        if (entity != mc.thePlayer) {
            return entity.onGround && entity.isSneaking() && entity instanceof EntityPlayer;
        }
        if (!mc.thePlayer.onGround) {
            return false;
        }
        if (PlayerUtils.isAirBelow()) {
            Module tower = any("Tower");
            if (tower != null && !tower.allowsSafeWalk()) {
                return false;
            }
            Module scaffold = any("Scaffold");
            if (scaffold != null && scaffold.stopsOnEdge()) {
                return true;
            }
            Module safeWalk = on("SafeWalk");
            if (safeWalk != null && safeWalk.stopsOnEdge()) {
                return true;
            }
        }
        return mc.thePlayer.isSneaking();
    }

    public static float getMovementYaw(Entity entity) {
        return entity == mc.thePlayer ? RotationManager.movementYaw() : entity.rotationYaw;
    }

    public static boolean isBlocking(EntityPlayer player) {
        if (player == mc.thePlayer && fakeBlocking() && PlayerUtils.holdingSword()) {
            return true;
        }
        return player.isUsingItem() && player.getItemInUse().getItem().getItemUseAction(player.getItemInUse()) == EnumAction.BLOCK;
    }

    public static int getItemInUseCount(EntityPlayer player, int count) {
        if (player == mc.thePlayer && fakeBlocking() && PlayerUtils.holdingSword()) {
            return 1;
        }
        return count;
    }

    private static boolean fakeBlocking() {
        Module killAura = any("KillAura");
        Module autoBlock = any("AutoBlock");
        return killAura != null && killAura.isFakeBlocking() || autoBlock != null && autoBlock.isFakeBlocking();
    }

    public static void onAttackSlowdown(Entity target) {
        Module m;
        if ((m = on("Reduce")) != null) {
            m.onAttackSlowdown(target);
            return;
        }
        if ((m = on("KeepSprint")) != null) {
            m.onAttackSlowdown(target);
            return;
        }
        mc.thePlayer.motionX *= 0.6;
        mc.thePlayer.motionZ *= 0.6;
        mc.thePlayer.setSprinting(false);
    }

    public static void getMouseOver() {
        if (mc.thePlayer != null && mc.theWorld != null) {
            Module m;
            if ((m = on("GhostHand")) != null && m.overrideMouseOver()) {
                return;
            }
            if ((m = on("AimAssist")) != null && m.overrideMouseOver()) {
                return;
            }
            Module autoClicker = any("AutoClicker");
            if (autoClicker != null && autoClicker.isClickingForReach() && (m = any("Reach")) != null && m.overrideMouseOver()) {
                return;
            }
        }
        if (mc.entityRenderer != null) {
            mc.entityRenderer.getMouseOver(1.0f);
        }
    }

    public static float getHurtCam() {
        return CameraModules.hurtCam();
    }

    public static boolean shouldCameraClip() {
        return CameraModules.shouldCameraClip();
    }

    public static float getRotationPitchHead(EntityLivingBase entity, float partialTicks) {
        if (entity == null) {
            return 0.0f;
        }
        if (entity != mc.thePlayer) {
            return entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        }
        return RotationManager.headPitch(entity, partialTicks);
    }

    public static <E extends EntityLivingBase> void doRenderLayer(RendererLivingEntity<E> renderer, LayerRenderer<E> layer, E e, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        if (e instanceof AbstractClientPlayer && layer instanceof LayerCape) {
            openraven.ravenxd.client.CapeLayer.render((RenderPlayer) (Object) renderer, (AbstractClientPlayer) e, f3);
            return;
        }
        layer.doRenderLayer(e, f, f2, f3, f4, f5, f6, f7);
    }

    public static boolean spoofingItemAsFist;
    public static boolean resetAnimationProgress;
    public static int stopSpoofTick;
    public static int extraSpoofTicks;
    public static boolean spoofingItem;

    public static ItemStack getItemToRender(ItemStack itemStack) {
        Object object = spoofedItem();
        if (object != null) {
            resetAnimationProgress = true;
            spoofingItem = true;
            if (object.equals(0)) {
                itemStack = null;
                spoofingItemAsFist = true;
            } else {
                itemStack = (ItemStack) object;
            }
        } else {
            if (spoofingItem) {
                itemStack = mc.thePlayer.getHeldItem();
                stopSpoofTick = mc.thePlayer.ticksExisted;
                extraSpoofTicks = 5;
            }
            if (extraSpoofTicks > 0) {
                resetAnimationProgress = true;
                if (mc.thePlayer.ticksExisted != stopSpoofTick) {
                    ++stopSpoofTick;
                    --extraSpoofTicks;
                }
            }
            spoofingItemAsFist = false;
            spoofingItem = false;
        }
        return itemStack;
    }

    private static Object spoofedItem() {
        Module autoSwap = any("AutoSwap");
        Module scaffold = any("Scaffold");
        if (autoSwap != null && toggled(autoSwap, "Spoof item") && SwapSlot.Scaffold.slot != -1 && scaffold != null && scaffold.isScaffolding()) {
            if (SwapSlot.Scaffold.slot == mc.thePlayer.inventory.currentItem) {
                return null;
            }
            ItemStack itemStack = SwapSlot.Scaffold.getItemStack();
            return itemStack == null ? Integer.valueOf(0) : itemStack;
        }
        Module autoTool = any("AutoTool");
        if (autoTool != null && autoTool.spoofedSlot() != -1 && toggled(autoTool, "Spoof item")) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(autoTool.spoofedSlot());
            return itemStack == null ? Integer.valueOf(0) : itemStack;
        }
        Module longJump = any("Long Jump");
        if (longJump != null && longJump.spoofedSlot() != -1) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(longJump.spoofedSlot());
            return itemStack == null ? Integer.valueOf(0) : itemStack;
        }
        return null;
    }

    private static boolean toggled(Module m, String setting) {
        return ((openraven.ravenxd.module.setting.ButtonSetting) ModuleManager.setting(m, setting)).isToggled();
    }

    public static float setAnimationProgress(float f) {
        if (resetAnimationProgress) {
            resetAnimationProgress = false;
            return 0.0f;
        }
        return f;
    }

    public static void updateModelVisibilities(AbstractClientPlayer player, ModelPlayer model) {
        if (player == mc.thePlayer && spoofingItemAsFist) {
            model.heldItemRight = 0;
        }
    }

    public static boolean onSend(Packet<?> packet) {
        boolean viewSent = ModuleManager.enabled("View Packets") != null && ViewPacketsModule.sent.isToggled();
        if (packet == PacketUtils.sendingNoEvent) {
            if (packet == PacketUtils.sendingQueued) {
                if (viewSent) {
                    ViewPacketsModule.onSent(packet);
                }
                return true;
            }
            return finalSendCheck(packet, viewSent);
        }
        boolean showAfter = false;
        if (viewSent) {
            if (ViewPacketsModule.includeCancelled.isToggled()) {
                ViewPacketsModule.onSent(packet);
            } else {
                showAfter = true;
            }
        }
        PacketUtils.track(packet);
        if (ScriptManager.events != null && PacketUtils.sendingSkipScripts != packet && !ScriptEvents.packetSent(packet)) {
            return false;
        }
        if (packet instanceof C01PacketChatMessage) {
            String s = ((C01PacketChatMessage) packet).getMessage();
            if (!s.isEmpty() && s.charAt(0) == '/' && !Hypixel.onHypixel() && !Account.isDev() && s.startsWith("/packetdebugger")) {
                return false;
            }
            if (ModuleManager.chatCommands.isEnabled() && ChatCommands.handle(s)) {
                return false;
            }
            if (PacketUtils.sending != packet && Irc.onChat(s)) {
                return false;
            }
            Module m;
            if ((m = on("Chat Bypass")) != null && !m.onSendPacket(packet)) {
                return false;
            }
        }
        Module m;
        if ((m = on("Freecam")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if ((m = on("KillAura")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if ((m = on("Custom Disabler")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if ((m = on("Disabler")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if ((m = on("Velocity")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if (packet instanceof C03PacketPlayer) {
            if ((m = on("AutoPlace")) != null) {
                m.onSendPacket(packet);
            }
            if ((m = any("Stasis")) != null && !m.onSendPacketAlways(packet)) {
                return false;
            }
        } else if (packet instanceof C09PacketHeldItemChange) {
            if ((m = on("BedAura")) != null && !m.onSendPacket(packet)) {
                return false;
            }
            if ((m = on("AutoRod")) != null) {
                m.onSendPacket(packet);
            }
            if ((m = on("Hit Select")) != null) {
                m.onSendPacket(packet);
            }
        } else if (packet instanceof C07PacketPlayerDigging) {
            String[] cancelling = {"NoSlow", "BedAura", "AutoPlace"};
            for (String name : cancelling) {
                if ((m = on(name)) != null && !m.onSendPacket(packet)) {
                    return false;
                }
            }
            if ((m = on("Freeze")) != null) {
                m.onSendPacket(packet);
            }
            if (on("Test") != null) {
                TestModule.onDig((C07PacketPlayerDigging) packet);
            }
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            String[] cancelling = {"NoSlow", "BedAura", "AutoPlace", "FastPlace"};
            for (String name : cancelling) {
                if ((m = on(name)) != null && !m.onSendPacket(packet)) {
                    return false;
                }
            }
            if ((m = VelocityModule.AntiKnockback.running()) != null) {
                m.onSendPacket(packet);
            }
            if ((m = on("AutoRod")) != null) {
                m.onSendPacket(packet);
            }
            PlayerUtils.onPlace((C08PacketPlayerBlockPlacement) packet);
        } else if (packet instanceof C02PacketUseEntity) {
            if ((m = on("Hit Select")) != null) {
                m.onSendPacket(packet);
            }
            if ((m = on("Freeze")) != null) {
                m.onSendPacket(packet);
            }
        } else if (packet instanceof C0APacketAnimation) {
            if ((m = on("AutoPlace")) != null && !m.onSendPacket(packet)) {
                return false;
            }
        } else if (packet instanceof C16PacketClientStatus || packet instanceof C0DPacketCloseWindow || packet instanceof C0EPacketClickWindow) {
            if ((m = on("InvMove")) != null && !m.onSendPacket(packet)) {
                return false;
            }
        } else if (packet instanceof C0BPacketEntityAction) {
            if ((m = on("WTap")) != null) {
                m.onSendPacket(packet);
            }
        }
        if ((m = on("AutoBlock")) != null) {
            m.onSendPacket(packet);
        }
        if (!lagPassesOutbound(packet)) {
            return false;
        }
        return finalSendCheck(packet, showAfter);
    }

    public static boolean finalSendCheck(Packet<?> packet) {
        return finalSendCheck(packet, false);
    }

    public static boolean finalSendCheck(Packet<?> packet, boolean show) {
        PacketUtils.trackPost(packet);
        boolean send = true;
        Module killAura;
        if (packet instanceof C09PacketHeldItemChange && (killAura = on("KillAura")) != null && !killAura.allowHeldItemPacket()) {
            send = false;
        }
        if (show && (send || ViewPacketsModule.includeCancelled.isToggled())) {
            ViewPacketsModule.onSent(packet);
        }
        if (send && on("Debug AC") != null) {
            DebugAcModule.onSent(packet);
        }
        return send;
    }

    public static boolean lagPassesOutbound(Packet<?> packet) {
        Module m;
        if ((m = on("Lag Range")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if ((m = on("Fake Lag")) != null && !m.onSendPacket(packet)) {
            return false;
        }
        if (PacketQueue.active && !PacketQueue.hold(packet)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onReceive(Packet packet, INetHandler handler) {
        if (ModuleManager.enabled("View Packets") != null && ViewPacketsModule.received.isToggled()) {
            ViewPacketsModule.onReceived(packet);
        }
        PacketUtils.trackReceived(packet);
        Module m;
        if ((m = any("Latency Alerts")) != null) m.onPacketReceivedAlways();
        if (ScriptManager.events != null && !ScriptEvents.packetReceived(packet)) {
            return;
        }
        if ((m = on("AntiCheat")) != null) {
            m.onReceivePacket(packet, handler);
        }
        if ((m = on("Freeze")) != null && !m.onReceivePacket(packet, handler)) {
            return;
        }
        if ((m = VelocityModule.AntiKnockback.running()) != null && !m.onReceivePacket(packet, handler)) {
            return;
        }
        if ((m = on("Backtrack")) != null && !m.onReceivePacket(packet, handler)) {
            return;
        }
        if (packet instanceof S08PacketPlayerPosLook && (m = on("Disabler")) != null && !m.onReceivePacket(packet, handler)) {
            return;
        }
        if (packet instanceof S08PacketPlayerPosLook && (m = on("Fly")) != null) {
            m.onReceivePacket(packet, handler);
        }
        if (packet instanceof S08PacketPlayerPosLook && mc.thePlayer != null) {
            PacketUtils.lastPosLook = System.currentTimeMillis();
        }
        if (packet instanceof S08PacketPlayerPosLook && (m = any("NoRotate")) != null && !m.onReceivePacketAlways(packet, handler)) {
            return;
        }
        if (packet instanceof S03PacketTimeUpdate) {
            if ((m = on("Weather")) != null && !m.onReceivePacket(packet, handler)) {
                return;
            }
        } else if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity p = (S12PacketEntityVelocity) packet;
            if (mc.thePlayer != null && p.getEntityID() == mc.thePlayer.getEntityId()) {
                if ((m = any("Stasis")) != null) m.onVelocityAlways();
                if (cancelsKnockback()) {
                    return;
                }
                if ((m = on("BedAura")) != null && !m.onReceivePacket(packet, handler)) {
                    return;
                }
                if (blinkCancelsKnockback()) {
                    return;
                }
                if (PacketUtils.isKnockback(p)) {
                    long l = System.currentTimeMillis();
                    String[] order = {"AutoBlock", "Jump Reset", "Hit Select", "KillAura", "AutoClicker"};
                    for (String name : order) {
                        if ((m = on(name)) != null) m.onKnockback(p, l);
                    }
                    PacketUtils.lastVelocity = l;
                }
                if ((m = on("Velocity")) != null && !m.onReceivePacket(packet, handler)) {
                    return;
                }
            }
        } else if (packet instanceof S32PacketConfirmTransaction) {
            if ((m = on("Velocity")) != null && !m.onReceivePacket(packet, handler)) {
                return;
            }
        } else if (packet instanceof S19PacketEntityStatus) {
            if ((m = on("Hit Select")) != null) {
                m.onReceivePacket(packet, handler);
            }
        } else if (packet instanceof S1DPacketEntityEffect) {
            if ((m = on("AntiDebuff")) != null) {
                S1DPacketEntityEffect p = (S1DPacketEntityEffect) packet;
                if (mc.thePlayer != null && p.getEntityId() == mc.thePlayer.getEntityId() && openraven.ravenxd.module.render.AntiDebuffModule.onEffect(p)) {
                    return;
                }
            }
        } else if (packet instanceof S23PacketBlockChange) {
            if ((m = on("Bed Wars")) != null) {
                m.onReceivePacket(packet, handler);
            }
        } else if (packet instanceof S27PacketExplosion) {
            if ((m = on("Long Jump")) != null && !m.onReceivePacket(packet, handler)) {
                return;
            }
            if ((m = on("BedAura")) != null && !m.onReceivePacket(packet, handler)) {
                return;
            }
            if (cancelsKnockback()) {
                return;
            }
            if (blinkCancelsKnockback()) {
                return;
            }
            if ((m = on("Jump Reset")) != null) {
                m.onReceivePacket(packet, handler);
            }
        } else if (packet instanceof S2FPacketSetSlot || packet instanceof S30PacketWindowItems) {
            if ((m = on("KillAura")) != null && !m.onReceivePacket(packet, handler)) {
                return;
            }
        }
        try {
            packet.processPacket(handler);
        } catch (ThreadQuickExitException threadQuickExitException) {
        }
    }

    private static boolean cancelsKnockback() {
        Module scaffold = any("Scaffold");
        if (scaffold == null || !scaffold.isScaffolding()) {
            return false;
        }
        if (!toggled(scaffold, "Cancel knockback")) {
            return false;
        }
        Module tower = any("Tower");
        return tower != null && tower.isTowering() || PlayerUtils.isMoving();
    }

    private static boolean blinkCancelsKnockback() {
        Module blink = any("Blink");
        return blink != null && blink.isBlinking() && toggled(blink, "Cancel knockback");
    }

    public static boolean movementFixSetting() {
        return SettingsModule.movementFix != null && SettingsModule.movementFix.isToggled();
    }

    static void updateResolution() {
        RenderUtils.updateResolution();
    }
}
