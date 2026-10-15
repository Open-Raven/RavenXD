package openraven.ravenxd.module.render;

import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

//yuri skid
public class MotionBlurModule extends Module {
    private static final ResourceLocation SHADER = new ResourceLocation("minecraft", "shaders/post/motion_blur.json");
    private static final Field LIST_SHADERS = ReflectionHelper.findField(ShaderGroup.class, "listShaders", "field_148031_d");
    private static final Method LOAD_SHADER = ReflectionHelper.findMethod(EntityRenderer.class, null, new String[]{"loadShader", "func_175069_a"}, ResourceLocation.class);

    public static SliderSetting blurAmount;

    public MotionBlurModule(int key) {
        super("Motion Blur", Category.render, key);
    }

    public void link() {
        blurAmount = (SliderSetting) ModuleManager.setting(this, "Blur amount");
    }

    private static boolean ours(ShaderGroup g) {
        return g != null && g.getShaderGroupName().equals(SHADER.toString());
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) {
            return;
        }
        // only when no other shader module is active
        if (mc.entityRenderer.getShaderGroup() == null) {
            try {
                LOAD_SHADER.invoke(mc.entityRenderer, SHADER);
            } catch (Exception ignored) {
                return;
            }
        }
        ShaderGroup group = mc.entityRenderer.getShaderGroup();
        if (!ours(group)) {
            return;
        }
        float uniform = 1.0f - Math.min((float) blurAmount.getInput() / 10.0f, 0.9f);
        try {
            List<Shader> shaders = (List<Shader>) LIST_SHADERS.get(group);
            shaders.get(0).getShaderManager().getShaderUniform("Phosphor").set(uniform, 0.0f, 0.0f);
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (ours(mc.entityRenderer.getShaderGroup())) {
            mc.entityRenderer.stopUseShader();
        }
    }
}
