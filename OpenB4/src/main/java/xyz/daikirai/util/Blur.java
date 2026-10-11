package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.List;

public class Blur {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Blur instance;

    public Framebuffer first = new Framebuffer(1, 1, false);
    public List<Framebuffer> buffers = new ArrayList<Framebuffer>();
    public Shader up = new Shader(Shader.KAWASE_UP);
    public Shader down = new Shader(Shader.KAWASE_DOWN);
    public Framebuffer stencil = new Framebuffer(1, 1, true);
    public int iterations;

    public static Blur get() {
        if (instance == null) {
            instance = new Blur();
        }
        return instance;
    }

    public void setup(float n) {
        for (Framebuffer fb : this.buffers) {
            fb.deleteFramebuffer();
        }
        this.buffers.clear();
        this.first = this.resize(null, false);
        this.buffers.add(this.first);
        for (int i = 1; (float) i <= n; ++i) {
            Framebuffer fb = new Framebuffer((int) ((double) mc.displayWidth / Math.pow(2.0, i)), (int) ((double) mc.displayHeight / Math.pow(2.0, i)), false);
            fb.setFramebufferFilter(9729);
            GlStateManager.bindTexture(fb.framebufferTexture);
            GL11.glTexParameteri(3553, 10242, 33648);
            GL11.glTexParameteri(3553, 10243, 33648);
            GlStateManager.bindTexture(0);
            this.buffers.add(fb);
        }
    }

    public Framebuffer resize(Framebuffer fb, boolean depth) {
        if (fb == null || fb.framebufferWidth != mc.displayWidth || fb.framebufferHeight != mc.displayHeight) {
            if (fb != null) {
                fb.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
        }
        return fb;
    }

    public void pass(Framebuffer fb, int texture, Shader shader, float offset) {
        fb.framebufferClear();
        fb.bindFramebuffer(false);
        shader.use();
        GL11.glBindTexture(3553, texture);
        shader.uniformf("offset", offset, offset);
        shader.uniformi("inTexture", 0);
        shader.uniformi("check", 0);
        shader.uniformf("halfpixel", 1.0f / (float) fb.framebufferWidth, 1.0f / (float) fb.framebufferHeight);
        shader.uniformf("iResolution", fb.framebufferWidth, fb.framebufferHeight);
        RenderUtils.screenQuad();
        shader.stop();
    }

    public void startStencil() {
        this.stencil = this.resize(this.stencil, false);
        this.stencil.framebufferClear();
        this.stencil.bindFramebuffer(false);
    }

    public void render(int n, float offset) {
        int i;
        this.stencil.unbindFramebuffer();
        if (n <= 0 || offset <= 0.0f) {
            mc.getFramebuffer().bindFramebuffer(true);
            return;
        }
        if (this.iterations != n || this.first.framebufferWidth != mc.displayWidth || this.first.framebufferHeight != mc.displayHeight) {
            this.setup(n);
            this.iterations = n;
        }
        this.pass(this.buffers.get(1), mc.getFramebuffer().framebufferTexture, this.down, offset);
        for (i = 1; i < n; ++i) {
            this.pass(this.buffers.get(i + 1), this.buffers.get(i).framebufferTexture, this.down, offset);
        }
        for (i = n; i > 1; --i) {
            this.pass(this.buffers.get(i - 1), this.buffers.get(i).framebufferTexture, this.up, offset);
        }
        Framebuffer out = this.buffers.get(0);
        out.framebufferClear();
        out.bindFramebuffer(false);
        this.up.use();
        this.up.uniformf("offset", offset, offset);
        this.up.uniformi("inTexture", 0);
        this.up.uniformi("check", 1);
        this.up.uniformi("textureToCheck", 16);
        this.up.uniformf("halfpixel", 1.0f / (float) out.framebufferWidth, 1.0f / (float) out.framebufferHeight);
        this.up.uniformf("iResolution", out.framebufferWidth, out.framebufferHeight);
        GL13.glActiveTexture(34000);
        GL11.glBindTexture(3553, this.stencil.framebufferTexture);
        GL13.glActiveTexture(33984);
        GL11.glBindTexture(3553, this.buffers.get(1).framebufferTexture);
        RenderUtils.screenQuad();
        this.up.stop();
        mc.getFramebuffer().bindFramebuffer(true);
        GL11.glBindTexture(3553, this.buffers.get(0).framebufferTexture);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.0f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        RenderUtils.screenQuad();
        GlStateManager.bindTexture(0);
        GlStateManager.disableBlend();
    }
}
