package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

public class ChestESPModule extends Module {
    public static SliderSetting red, green, blue;
    public static ButtonSetting rainbow, outline, shade, disableIfOpened;

    public ChestESPModule(int key) {
        super("ChestESP", Category.render, key);
    }

    public void link() {
        red = (SliderSetting) ModuleManager.setting(this, "Red");
        green = (SliderSetting) ModuleManager.setting(this, "Green");
        blue = (SliderSetting) ModuleManager.setting(this, "Blue");
        rainbow = (ButtonSetting) ModuleManager.setting(this, "Rainbow");
        outline = (ButtonSetting) ModuleManager.setting(this, "Outline");
        shade = (ButtonSetting) ModuleManager.setting(this, "Shade");
        disableIfOpened = (ButtonSetting) ModuleManager.setting(this, "Disable if opened");
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        int n = rainbow.isToggled() ? RenderUtils.rainbow(2L) : new Color((int) red.getInput(), (int) green.getInput(), (int) blue.getInput()).getRGB();
        for (TileEntity te : mc.theWorld.loadedTileEntityList) {
            if (te instanceof TileEntityChest) {
                if (disableIfOpened.isToggled() && ((TileEntityChest) te).lidAngle > 0.0f) continue;
                this.box(te.getPos(), n);
                continue;
            }
            if (!(te instanceof TileEntityEnderChest) || disableIfOpened.isToggled() && ((TileEntityEnderChest) te).lidAngle > 0.0f) continue;
            this.box(te.getPos(), n);
        }
    }

    private void box(BlockPos pos, int n) {
        Render3D.blockBox(pos.getX(), pos.getY(), pos.getZ(), 0.0625f, 0.0f, 0.0625f, 0.9375f, 0.875f, 0.9375f, n, outline.isToggled(), shade.isToggled());
    }
}
