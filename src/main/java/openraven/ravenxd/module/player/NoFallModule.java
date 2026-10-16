package openraven.ravenxd.module.player;

import openraven.ravenxd.util.MoveUtils;
import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.mixin.IC03PacketPlayer;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.client.Hypixel;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.movement.TimerModule;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class NoFallModule extends Module {
    public static NoFallModule instance;
    public static SliderSetting modeSlider, minimumFallDistance;
    public static ButtonSetting disableAdventure, ignoreVoid;

    public enum Mode {
        SPOOF, SINGLE, EXTRA, NOGROUND_A, NOGROUND_B, PRECISION, POSITION, ONGROUND1, VULCAN, MATRIX, GLITCH, CHUNK_LOAD
    }

    public static Mode mode = Mode.SPOOF;
    private static boolean timerSlowed;
    private static int counter;
    private static double triggerHeight = -1.0;
    private static boolean sent;
    private static long cagesOpened, exemptFor;
    private static final int[] vulcanLimits = {3, 2, 2};
    private static int vulcanChanged, vulcanFalls;
    // rise chunkloaad
    private static volatile boolean fakeUnloaded;

    public NoFallModule(int key) {
        super("NoFall", Category.player, key);
        instance = this;
    }

    public void link() {
        modeSlider = (SliderSetting) ModuleManager.setting(this, "Mode");
        minimumFallDistance = (SliderSetting) ModuleManager.setting(this, "Minimum fall distance");
        disableAdventure = (ButtonSetting) ModuleManager.setting(this, "Disable adventure");
        ignoreVoid = (ButtonSetting) ModuleManager.setting(this, "Ignore void");
    }

    @Override
    public void onDisable() {
        if (timerSlowed) {
            timerSlowed = false;
            MoveUtils.setTimer(1.0f);
        }
        counter = 0;
        exemptFor = 0L;
        cagesOpened = 0L;
        sent = false;
        triggerHeight = -1.0;
        vulcanChanged = 0;
        vulcanFalls = 0;
        fakeUnloaded = false;
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSlider.getInput()];
    }

    @Override
    public String getInfo() {
        if (mode == Mode.NOGROUND_A || mode == Mode.NOGROUND_B) {
            return "NoGround";
        }
        return modeSlider.getDisplay(false);
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (mode != Mode.NOGROUND_A && s.equals("Cages opened! FIGHT!")) {
            cagesOpened = System.currentTimeMillis();
            exemptFor = 1500L;
        }
    }

    private static boolean falling() {
        return (double) Minecraft.getMinecraft().thePlayer.fallDistance >= minimumFallDistance.getInput()
                && (!ignoreVoid.enabled || PlayerUtils.heightAboveGround() > -1.0);
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean active = true;
        if (mc.thePlayer.capabilities.allowFlying || mc.thePlayer.capabilities.isCreativeMode
                || disableAdventure.enabled && !mc.thePlayer.capabilities.allowEdit || Hypixel.state == Hypixel.State.BRIDGE_PRACTICE) {
            active = false;
        }
        if (exemptFor > 0L) {
            if (Math.abs(System.currentTimeMillis() - cagesOpened) > exemptFor) {
                exemptFor = 0L;
            } else {
                active = false;
            }
        }
        return active;
    }

    // vulcan
    @Override
    public boolean onSendPacket(Packet<?> packet) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mode != Mode.VULCAN || !(packet instanceof C03PacketPlayer) || mc.thePlayer == null
                || (double) mc.thePlayer.fallDistance <= minimumFallDistance.getInput() || !active()) {
            return true;
        }
        if (vulcanChanged > vulcanLimits[vulcanFalls % vulcanLimits.length]) {
            return true;
        }
        ((IC03PacketPlayer) packet).setOnGround(true);
        mc.thePlayer.fallDistance = 0.0f;
        mc.thePlayer.motionX = 0.0;
        mc.thePlayer.motionY = 0.0;
        mc.thePlayer.motionZ = 0.0;
        ++vulcanChanged;
        return true;
    }

    @Override
    public void onUpdate() {
        if (mode == Mode.VULCAN && Minecraft.getMinecraft().thePlayer.onGround && vulcanChanged > 0) {
            ++vulcanFalls;
            vulcanChanged = 0;
        }
    }

    // chunkload on flag
    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        // TODO: make onFlag event
        if (packet instanceof S08PacketPlayerPosLook) {
            fakeUnloaded = false;
        }
        return true;
    }

    private static boolean canFall(Minecraft mc) {
        return !PlayerUtils.inLiquid() && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isRiding();
    }

    private static boolean solidBelow(Minecraft mc, double height) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, -height, 0.0)).isEmpty();
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!active()) {
            if (timerSlowed) {
                timerSlowed = false;
                MoveUtils.setTimer(1.0f);
            }
            return;
        }
        switch (mode) {
            case SPOOF:
                if (!falling()) break;
                e.onGround = true;
                break;
            case SINGLE:
                if (falling()) {
                    if (minimumFallDistance.getInput() == 0.0) {
                        e.onGround = true;
                        break;
                    }
                    double d = PlayerUtils.heightAboveGround();
                    if (triggerHeight == -1.0) {
                        triggerHeight = (d + minimumFallDistance.getInput()) / 2.0;
                    }
                    if (triggerHeight == -2.0 || !(d <= triggerHeight)) break;
                    triggerHeight = -2.0;
                    e.onGround = true;
                    break;
                }
                triggerHeight = -1.0;
                break;
            case EXTRA: {
                float f = 0.75f;
                if (falling()) {
                    PacketUtils.send(new C03PacketPlayer(true));
                    mc.thePlayer.fallDistance = 0.0f;
                    counter = 1;
                    MoveUtils.setTimer(f);
                    timerSlowed = true;
                }
                if (counter <= 0 || ++counter < 5) break;
                counter = 0;
                MoveUtils.setTimer(1.0f);
                timerSlowed = false;
                break;
            }
            case MATRIX: {
                float distance = mc.thePlayer.fallDistance;
                if (PlayerUtils.heightAboveGround() >= 0.0) {
                    if (distance > 2.0f) {
                        MoveUtils.strafe(0.19);
                    }
                    if (distance > 3.0f && MoveUtils.speed() < 0.2) {
                        e.onGround = true;
                        distance = 0.0f;
                    }
                }
                mc.thePlayer.fallDistance = distance;
                break;
            }
            case GLITCH:
                if (canFall(mc) && mc.thePlayer.fallDistance > 2.5f && solidBelow(mc, 1.0)) {
                    e.onGround = true;
                    e.y -= 1.0;
                    mc.thePlayer.fallDistance = 0.0f;
                }
                break;
            case CHUNK_LOAD:
                if (fakeUnloaded) {
                    mc.thePlayer.motionY = 0.0;
                    e.onGround = false;
                    e.y -= 0.098;
                    mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, e.y, mc.thePlayer.posZ);
                    break;
                }
                if (mc.thePlayer.motionY > 0.0 || mc.thePlayer.fallDistance <= 3.0f) {
                    break;
                }
                if (mc.theWorld.getBlockState(new BlockPos(e.x, e.y + mc.thePlayer.motionY, e.z)).getBlock().getMaterial().isSolid()) {
                    mc.thePlayer.fallDistance = 0.0f;
                    fakeUnloaded = true;
                }
                break;
            case ONGROUND1:
                if (falling()) {
                    PacketUtils.send(new C03PacketPlayer(true));
                }
                break;
            case NOGROUND_A:
                e.onGround = false;
                break;
            case NOGROUND_B:
                if (falling()) {
                    counter = 1;
                } else if (counter == 1 && PlayerUtils.airTicks >= 7L) {
                    counter = 0;
                }
                if (counter != 1) break;
                e.onGround = false;
                break;
            case PRECISION: {
                if (!falling()) break;
                double d = 0.07;
                double floor = Math.floor(mc.thePlayer.posY);
                double frac = Math.abs(mc.thePlayer.posY - floor);
                if (!(frac <= d) && !(frac >= 1.0 - d)) break;
                e.y = floor;
                e.onGround = true;
                break;
            }
            case POSITION:
                if (!sent && falling()) {
                    MovingObjectPosition m = PlayerUtils.rayTraceBlocks(3.5, mc.thePlayer.rotationYaw, 90.0f);
                    if (m == null || m.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) break;
                    PacketUtils.send(new C03PacketPlayer(false));
                    PacketUtils.send(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY - 20.0, mc.thePlayer.posZ, true));
                    PacketUtils.send(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround));
                    sent = true;
                    break;
                }
                if (!mc.thePlayer.onGround) break;
                sent = false;
        }
    }
}
