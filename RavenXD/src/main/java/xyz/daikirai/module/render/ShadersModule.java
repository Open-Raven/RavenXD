package xyz.daikirai.module.render;

import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ShadersModule extends Module {
    public static ResourceLocation[] shaders;
    public static String[] names;
    public static SliderSetting shader;
    public static int index;
    private final Field shaderIndex = ReflectionHelper.findField(EntityRenderer.class, "shaderIndex", "field_147713_ae");
    private final Method loadShader = ReflectionHelper.findMethod(EntityRenderer.class, null, new String[]{"loadShader", "func_175069_a"}, ResourceLocation.class);

    public ShadersModule(int key) {
        super("Shaders", Category.render, key);
        try {
            shaders = ReflectionHelper.getPrivateValue(EntityRenderer.class, mc.entityRenderer, "shaderResourceLocations", "field_147712_ad");
            names = new String[shaders.length];
            for (int i = 0; i < shaders.length; ++i) {
                names[i] = shaders[i].getResourcePath().replaceFirst("shaders/post/", "").split("\\.json")[0].toUpperCase();
            }
        } catch (Exception e) {
            return;
        }
        shader = new SliderSetting("Shader", "", "", index, 0, names.length - 1, 1.0, names, null);
        this.registerSetting(shader);
    }

    @Override
    public void onDisable() {
        mc.entityRenderer.stopUseShader();
    }

    @Override
    public boolean canBeEnabled() {
        if (!OpenGlHelper.shadersSupported) {
            Chat.print("&b" + this.getName() + "&7: &r" + "&cShaders not supported.");
            return false;
        }
        return true;
    }

    @Override
    public void guiUpdate() {
        index = (int) shader.getInput();
    }

    @Override
    public void onUpdate() {
        try {
            boolean active = mc.entityRenderer.isShaderActive();
            if (!active || this.shaderIndex.getInt(mc.entityRenderer) != index) {
                this.shaderIndex.setInt(mc.entityRenderer, index);
                this.loadShader.invoke(mc.entityRenderer, shaders[index]);
            }
        } catch (Exception ignored) {
        }
    }
}
