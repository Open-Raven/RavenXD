package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.text.DecimalFormat;

public class BreakProgressModule extends Module {
    public enum Mode {
        Percentage, Seconds, Raw
    }

    public static Mode mode = Mode.Percentage;
    public static SliderSetting modeSetting;
    public static ButtonSetting showManual, showBedAura, fadeIn;
    private String text;
    private float progress;
    private final Field curBlockDamage;
    private BlockPos pos;
    private final DecimalFormat format = new DecimalFormat("0.00");

    public BreakProgressModule(int key) {
        super("BreakProgress", Category.render, key);
        this.curBlockDamage = ReflectionHelper.findField(PlayerControllerMP.class, "curBlockDamageMP", "field_78770_f");
    }

    public void link() {
        modeSetting = (SliderSetting) ModuleManager.setting(this, "Mode");
        showManual = (ButtonSetting) ModuleManager.setting(this, "Show Manual");
        showBedAura = (ButtonSetting) ModuleManager.setting(this, "Show BedAura");
        fadeIn = (ButtonSetting) ModuleManager.setting(this, "Fade in");
    }

    @Override
    public void guiUpdate() {
        mode = Mode.values()[(int) modeSetting.getInput()];
    }

    private void updateText() {
        switch (mode) {
            case Percentage:
                this.text = (int) (100.0 * (double) this.progress) + "%";
                break;
            case Seconds: {
                float f = BlockUtils.breakSpeed(mc.theWorld.getBlockState(this.pos).getBlock(), mc.thePlayer.getHeldItem(), false);
                double d = (double) ((1.0f - this.progress) / f) / 20.0;
                this.text = Strings.round(d, 1) + "s";
                break;
            }
            case Raw:
                this.text = this.format.format(this.progress);
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer.capabilities.isCreativeMode || !mc.thePlayer.capabilities.allowEdit) {
            this.reset();
            return;
        }
        if (showBedAura.isToggled() && xyz.daikirai.module.player.BedAuraModule.progress != 0.0f && xyz.daikirai.module.player.BedAuraModule.target != null) {
            this.progress = Math.min(1.0f, xyz.daikirai.module.player.BedAuraModule.progress);
            this.pos = xyz.daikirai.module.player.BedAuraModule.target;
            this.updateText();
            return;
        }
        if (!showManual.isToggled() || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !Mouse.isButtonDown(0)) {
            this.reset();
            return;
        }
        try {
            this.progress = this.curBlockDamage.getFloat(mc.playerController);
            if (this.progress == 0.0f) {
                this.reset();
                return;
            }
            this.pos = mc.objectMouseOver.getBlockPos();
            this.updateText();
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    private void reset() {
        this.progress = 0.0f;
        this.pos = null;
        this.text = "";
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.progress == 0.0f || this.pos == null || !Chat.inWorld()) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        double d = (double) this.pos.getX() + 0.5 - rm.viewerPosX;
        double d2 = (double) this.pos.getY() + 0.5 - rm.viewerPosY;
        double d3 = (double) this.pos.getZ() + 0.5 - rm.viewerPosZ;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) d, (float) d2, (float) d3);
        GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-0.02266667f, -0.02266667f, -0.02266667f);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        int n = mc.fontRendererObj.getStringWidth(this.text) / -2;
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (fadeIn.isToggled()) {
            int alpha = 26 + (int) ((float) (240 - 26) * this.progress);
            mc.fontRendererObj.drawString(this.text, (float) n, (float) -3, RenderUtils.withAlpha(-1, alpha), true);
        } else {
            mc.fontRendererObj.drawString(this.text, (float) n, (float) -3, RenderUtils.withAlpha(-1, 240), true);
        }
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }
}
