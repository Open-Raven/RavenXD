package xyz.daikirai.module.render;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.Render3D;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RadarModule extends Module {
    public static ButtonSetting showTracerLines;
    private int scale = 2;

    public RadarModule(int key) {
        super("Radar", Category.render, key);
    }

    public void link() {
        showTracerLines = (ButtonSetting) ModuleManager.setting(this, "Show tracer lines");
    }

    @Override
    public void onUpdate() {
        this.scale = new ScaledResolution(mc).getScaleFactor();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Overlay.canRender()) {
            return;
        }
        Gui.drawRect(5, 70, 105, 170, 0x7D000000);
        Gui.drawRect(5 - 1, 70 - 1, 105 + 1, 70, -1);
        Gui.drawRect(5 - 1, 170, 105 + 1, 170 + 1, -1);
        Gui.drawRect(5 - 1, 70, 5, 170, -1);
        Gui.drawRect(105, 70, 105 + 1, 170, -1);
        Render3D.circle((double) 105 / 2.0 + 3.0, 70 + 52, 5.0, 3, -1);
        GL11.glPushMatrix();
        GL11.glEnable(3089);
        GL11.glScissor(5 * this.scale, mc.displayHeight - this.scale * 170, 105 * this.scale - this.scale * 5, this.scale * 100);
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p.deathTime != 0 || BotCheck.isBot(p)) continue;
            double d = p.getDistanceSqToEntity(mc.thePlayer);
            if (!(d <= 360.0)) continue;
            double d6 = p.posX - mc.thePlayer.posX;
            double d7 = p.posZ - mc.thePlayer.posZ;
            double d8 = Math.atan2(d6, d7) * (double) 57.29578f;
            double d9 = ((double) mc.thePlayer.rotationYaw + d8) % 360.0;
            double d10 = d / 5.0;
            double d11 = d10 * Math.sin(Math.toRadians(d9));
            double d12 = d10 * Math.cos(Math.toRadians(d9));
            if (showTracerLines.isToggled()) {
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDisable(3553);
                GL11.glBlendFunc(770, 771);
                GL11.glEnable(3042);
                GL11.glLineWidth(0.5f);
                GL11.glColor3d(1.0, 1.0, 1.0);
                GL11.glBegin(2);
                GL11.glVertex2d((double) 105 / 2.0 + 3.0, 70 + 52);
                GL11.glVertex2d((double) 105 / 2.0 + 3.0 - d11, (double) (70 + 52) - d12);
                GL11.glEnd();
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glDisable(3042);
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDisable(2848);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
            Render3D.circle((double) 105 / 2.0 + 3.0 - d11, (double) (70 + 52) - d12, 3.0, 4, Color.red.getRGB());
        }
        GL11.glDisable(3089);
        GL11.glPopMatrix();
    }
}
