package openraven.ravenxd.module.movement;

import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.util.TeleportPath;
import openraven.ravenxd.client.BotCheck;
import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.event.MotionEvent;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.SliderSetting;
import openraven.ravenxd.util.PacketUtils;
import openraven.ravenxd.util.Render3D;
import net.minecraft.client.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.ArrayList;

public class TeleportModule extends Module {
    public static ButtonSetting rightClickTeleport, highlightTarget, highlightPath;
    public static SliderSetting mode;
    private ArrayList<TeleportPath.Point> verusPath;
    private volatile int verusTicks;
    private ArrayList<TeleportPath.Point> path = new ArrayList<>();
    private BlockPos target;

    public TeleportModule(int key) {
        super("Teleport", Category.movement, key);
    }

    public void link() {
        rightClickTeleport = (ButtonSetting) ModuleManager.setting(this, "Right click teleport");
        highlightTarget = (ButtonSetting) ModuleManager.setting(this, "Highlight target");
        highlightPath = (ButtonSetting) ModuleManager.setting(this, "Highlight path");
        mode = (SliderSetting) ModuleManager.setting(this, "Mode");
    }

    private static MovingObjectPosition lookBlock() {
        MovingObjectPosition m = BotCheck.rayTraceBlocks(150.0);
        return m == null || m.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK ? null : m;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (verus() || !(rightClickTeleport.enabled && highlightTarget.enabled && this.target != null && Chat.inWorld())) {
            return;
        }
        Render3D.block(this.target, Color.orange.getRGB(), true, true);
        if (highlightPath.enabled) {
            int n = 0;
            for (TeleportPath.Point p : this.path) {
                if (n >= 175) break;
                Render3D.block(new BlockPos(p.x, p.y, p.z), Color.yellow.getRGB(), false, true);
                ++n;
            }
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (verus() || !rightClickTeleport.enabled || !highlightTarget.enabled) {
            return;
        }
        MovingObjectPosition m = lookBlock();
        if (m == null) {
            return;
        }
        this.target = m.getBlockPos();
        if (highlightPath.enabled) {
            this.path = TeleportPath.path(new TeleportPath.Point(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                    new TeleportPath.Point(this.target.getX(), this.target.getY() + 2, this.target.getZ()));
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (verus() || !(e.button == 1 && e.buttonstate && rightClickTeleport.enabled && Chat.inWorld())) {
            return;
        }
        MovingObjectPosition m = lookBlock();
        if (m == null) {
            return;
        }
        this.target = m.getBlockPos();
        TeleportPath.teleport(this.target, true);
    }

    private static boolean verus() {
        return mode != null && mode.getInput() == 1.0;
    }

    private int verusTotal() {
        return Math.max(this.verusPath.size() * 10, 20);
    }

    private void verusStop(String message) {
        Chat.print("&7[&fTeleport&7] " + message);
        this.disable();
    }

    // azura paste
    @Override
    public void onPreMotion(MotionEvent e) {
        if (!verus()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (this.verusTicks > 0 && PlayerUtils.isMoving()) {
            this.verusStop("&7Disabled due to movement");
            return;
        }
        if (this.verusTicks == 0 && !e.onGround && this.verusPath != null) {
            this.verusStop("&cPlease stand on the ground when selecting");
            return;
        }
        if (this.verusPath == null) {
            MovingObjectPosition hit = PlayerUtils.rayTraceBlocks(60.0, e.yaw, e.pitch);
            if (mc.gameSettings.keyBindAttack.isKeyDown() && hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && !mc.theWorld.isAirBlock(hit.getBlockPos())) {
                e.y = -100.0;
                e.onGround = true;
                BlockPos above = hit.getBlockPos().up(1);
                this.verusPath = TeleportPath.path(new TeleportPath.Point(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                        new TeleportPath.Point(above.getX(), above.getY(), above.getZ()));
                if (this.verusPath.isEmpty()) {
                    this.verusStop("&cNo path found.");
                    return;
                }
                double seconds = this.verusTotal() / 20.0;
                Chat.print("&7[&fTeleport&7] Teleporting in &b" + seconds + "&7 seconds.");
            }
        } else {
            mc.thePlayer.setSprinting(true);
            e.sprinting = true;
            if (this.verusTicks < this.verusTotal()) {
                ++this.verusTicks;
            }
        }
        if (this.verusPath != null && this.verusTicks == this.verusTotal()) {
            TeleportPath.Point last = null;
            for (TeleportPath.Point v : this.verusPath) {
                PacketUtils.sendNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(v.x, v.y, v.z, false));
                last = v;
            }
            if (last != null) {
                e.x = last.x;
                e.y = last.y;
                e.z = last.z;
                mc.thePlayer.setPosition(last.x, last.y, last.z);
            }
            this.verusStop("&7Teleported. Wait around 25 seconds before using it again.");
        }
    }

    @Override
    public boolean onSendPacket(Packet<?> packet) {
        if (!verus()) {
            return true;
        }
        if (this.verusTicks > 0 && (packet instanceof C0FPacketConfirmTransaction || packet instanceof C00PacketKeepAlive)) {
            return false;
        }
        ArrayList<TeleportPath.Point> p = this.verusPath;
        return !(p != null && packet instanceof C03PacketPlayer && this.verusTicks < Math.max(p.size() * 2, 20));
    }

    // flag
    @Override
    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        if (!verus() || !(packet instanceof S08PacketPlayerPosLook)) {
            return true;
        }
        if (this.verusTicks > 2) {
            Minecraft.getMinecraft().addScheduledTask(() -> this.verusStop("&7Disabled due to teleport/lagback."));
            return true;
        }
        S08PacketPlayerPosLook s = (S08PacketPlayerPosLook) packet;
        Minecraft.getMinecraft().addScheduledTask(() -> PacketUtils.sendNoEvent(
                new C03PacketPlayer.C06PacketPlayerPosLook(s.getX(), s.getY(), s.getZ(), s.getYaw(), s.getPitch(), false)));
        return false;
    }

    @Override
    public void onDisable() {
        this.verusPath = null;
        this.verusTicks = 0;
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.path.clear();
        if (verus()) {
            this.verusPath = null;
            this.verusTicks = 0;
            Chat.print("&7[&fTeleport&7] Look at a block and left click to teleport.");
            return;
        }
        if (rightClickTeleport.enabled) {
            return;
        }
        MovingObjectPosition m = lookBlock();
        if (m == null) {
            this.disable();
            return;
        }
        TeleportPath.teleport(m.getBlockPos(), true);
        this.disable();
    }
}
