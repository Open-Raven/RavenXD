package xyz.daikirai.module.player;

import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.movement.TimerModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class NoFallModule extends Module {
    public static NoFallModule instance;
    public static SliderSetting modeSlider, minimumFallDistance;
    public static ButtonSetting disableAdventure, ignoreVoid;

    public enum Mode {
        SPOOF, SINGLE, EXTRA, NOGROUND_A, NOGROUND_B, PRECISION, POSITION
    }

    public static Mode mode = Mode.SPOOF;
    private static boolean timerSlowed;
    private static int counter;
    private static double triggerHeight = -1.0;
    private static boolean sent;
    private static long cagesOpened, exemptFor;

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

    @Override
    public void onPreMotion(MotionEvent e) {
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
        if (!active) {
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
