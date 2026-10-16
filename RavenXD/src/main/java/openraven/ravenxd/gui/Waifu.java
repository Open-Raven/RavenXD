package openraven.ravenxd.gui;

import openraven.ravenxd.module.GuiModule;
import openraven.ravenxd.util.IMinecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

public final class Waifu implements IMinecraft {
    private static final String[] NAMES = {null, "astolfo", "kurumi", "uzaki", "megumin", "mai", "magic", "kiwi", "utena"};
    private static final ResourceLocation[] IMAGES = new ResourceLocation[NAMES.length];

    public static int x = 340, y = 135;
    private static boolean dragging;
    private static int lastX, lastY;

    static {
        for (int i = 1; i < NAMES.length; ++i) {
            IMAGES[i] = new ResourceLocation("ravenxd", "haru/waifu/" + NAMES[i] + ".png");
        }
    }

    private Waifu() {
    }

    private static ResourceLocation image() {
        int i = GuiModule.waifu == null ? 0 : (int) GuiModule.waifu.getInput();
        return i > 0 && i < IMAGES.length ? IMAGES[i] : null;
    }

    private static boolean over(int mx, int my, int width, int height) {
        return mx >= x && mx <= x + width / 5.1f && my >= y && my <= y + height / 2.0f;
    }

    public static void render(int mx, int my, int width, int height) {
        ResourceLocation image = image();
        if (image == null) {
            dragging = false;
            return;
        }
        if (dragging) {
            x += mx - lastX;
            y += my - lastY;
            lastX = mx;
            lastY = my;
        }
        float w = width / 5.2f, h = height / 2.0f;
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0f, 0.0f, (int) w, (int) h, w, h);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    public static boolean mouseClicked(int mx, int my, int button, int width, int height) {
        if (button != 0 || image() == null || !over(mx, my, width, height)) {
            return false;
        }
        dragging = true;
        lastX = mx;
        lastY = my;
        return true;
    }

    public static void mouseReleased() {
        if (dragging) {
            dragging = false;
            openraven.ravenxd.client.ClientConfig.save();
        }
    }
}
