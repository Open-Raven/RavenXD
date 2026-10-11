package xyz.daikirai.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class GlowShader {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static GlowShader instance;

    private int iterations;
    private final Shader down;
    private final Shader up;
    private Framebuffer capture = new Framebuffer(1, 1, false);
    private Framebuffer first = new Framebuffer(1, 1, true);
    private final List<Framebuffer> buffers = new ArrayList<Framebuffer>();

    public static GlowShader get() {
        if (instance == null) {
            instance = new GlowShader();
        }
        return instance;
    }

    public GlowShader() {
        this.up = new Shader(ShaderSources.KAWASE_BLOOM_UP);
        this.down = new Shader(ShaderSources.KAWASE_BLOOM_DOWN);
    }

    public void render(int n, float f) {
        int i;
        this.capture.unbindFramebuffer();
        if (n <= 0 || f <= 0.0f) {
            mc.getFramebuffer().bindFramebuffer(true);
            return;
        }
        if (this.iterations != n || this.first.framebufferWidth != mc.displayWidth || this.first.framebufferHeight != mc.displayHeight) {
            this.setup(n);
            this.iterations = n;
        }
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.0f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(1, 1);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.pass(this.buffers.get(1), this.capture.framebufferTexture, this.down, f);
        for (i = 1; i < n; ++i) {
            this.pass(this.buffers.get(i + 1), this.buffers.get(i).framebufferTexture, this.down, f);
        }
        for (i = n; i > 1; --i) {
            this.pass(this.buffers.get(i - 1), this.buffers.get(i).framebufferTexture, this.up, f);
        }
        Framebuffer fb = this.buffers.get(0);
        fb.framebufferClear();
        fb.bindFramebuffer(false);
        this.up.use();
        this.up.uniformf("offset", f, f);
        this.up.uniformi("inTexture", 0);
        this.up.uniformi("check", 1);
        this.up.uniformi("textureToCheck", 16);
        this.up.uniformf("halfpixel", 1.0f / (float) fb.framebufferWidth, 1.0f / (float) fb.framebufferHeight);
        this.up.uniformf("iResolution", fb.framebufferWidth, fb.framebufferHeight);
        GlStateManager.setActiveTexture(34000);
        GL11.glBindTexture(3553, this.capture.framebufferTexture);
        GlStateManager.setActiveTexture(33984);
        GL11.glBindTexture(3553, this.buffers.get(1).framebufferTexture);
        RenderUtils.screenQuad();
        this.up.stop();
        GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        mc.getFramebuffer().bindFramebuffer(false);
        GL11.glBindTexture(3553, this.buffers.get(0).framebufferTexture);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.0f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        RenderUtils.screenQuad();
        GlStateManager.bindTexture(0);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.0f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
    }

    private Framebuffer resize(Framebuffer fb, boolean depth) {
        if (fb == null || fb.framebufferWidth != mc.displayWidth || fb.framebufferHeight != mc.displayHeight) {
            if (fb != null) {
                fb.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
        }
        return fb;
    }

    private void setup(float f) {
        for (Framebuffer fb : this.buffers) {
            fb.deleteFramebuffer();
        }
        this.buffers.clear();
        this.first = this.resize(null, true);
        this.buffers.add(this.first);
        int n = 1;
        while ((float) n <= f) {
            Framebuffer fb = new Framebuffer((int) ((double) mc.displayWidth / Math.pow(2.0, n)), (int) ((double) mc.displayHeight / Math.pow(2.0, n)), true);
            fb.setFramebufferFilter(9729);
            GlStateManager.bindTexture(fb.framebufferTexture);
            GL11.glTexParameteri(3553, 10242, 33648);
            GL11.glTexParameteri(3553, 10243, 33648);
            GlStateManager.bindTexture(0);
            this.buffers.add(fb);
            ++n;
        }
    }

    private void pass(Framebuffer fb, int texture, Shader shader, float f) {
        fb.framebufferClear();
        fb.bindFramebuffer(false);
        shader.use();
        GL11.glBindTexture(3553, texture);
        shader.uniformf("offset", f, f);
        shader.uniformi("inTexture", 0);
        shader.uniformi("check", 0);
        shader.uniformf("halfpixel", 1.0f / (float) fb.framebufferWidth, 1.0f / (float) fb.framebufferHeight);
        shader.uniformf("iResolution", fb.framebufferWidth, fb.framebufferHeight);
        RenderUtils.screenQuad();
        shader.stop();
    }

    public void startCapture() {
        this.capture = this.resize(this.capture, false);
        this.capture.framebufferClear();
        this.capture.bindFramebuffer(false);
    }

    public void render(Framebuffer fb, int n, float f) {
        Framebuffer saved = this.capture;
        this.capture = fb;
        try {
            this.render(n, f);
        } finally {
            this.capture = saved;
        }
    }
}
