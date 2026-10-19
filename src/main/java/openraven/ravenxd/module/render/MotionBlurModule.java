package openraven.ravenxd.module.render;

import openraven.ravenxd.client.Chat;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
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
    private boolean failed;

    public MotionBlurModule(int key) {
        super("Motion Blur", Category.render, key);
    }

    public void link() {
        blurAmount = (SliderSetting) ModuleManager.setting(this, "Blur amount");
    }

    private void message(String s) {
        Chat.print("&b" + this.getName() + "&7: &r" + s);
    }

    private static boolean ours(ShaderGroup g) {
        return g != null && g.getShaderGroupName().equals(SHADER.toString());
    }

    @SuppressWarnings("unchecked")
    private static List<Shader> shaders(ShaderGroup g) {
        try {
            return (List<Shader>) LIST_SHADERS.get(g);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static boolean dead(ShaderGroup g) {
        List<Shader> list = shaders(g);
        return list == null || list.isEmpty();
    }

    @Override
    public boolean canBeEnabled() {
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) {
            this.message("&cNeeds shaders and framebuffers (Video Settings > FBO).");
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        this.failed = false;
        try {
            GameSettings gs = Minecraft.getMinecraft().gameSettings;
            if (GameSettings.class.getField("ofFastRender").getBoolean(gs)) {
                this.message("&cturn off OptiFine's Fast Render, it disables motion blur.");
            }
            if (GameSettings.class.getField("ofAaLevel").getInt(gs) > 0) {
                this.message("&cturn off OptiFine's antialiasing, it disables motion blur.");
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) {
            return;
        }
        ShaderGroup group = mc.entityRenderer.getShaderGroup();
        if (!ours(group) || dead(group)) {
            boolean otherShader = group != null && !ours(group) && !dead(group);
            if (otherShader && (mc.currentScreen != null || ModuleManager.enabled("Shaders") != null) || this.failed) {
                return;
            }
            try {
                LOAD_SHADER.invoke(mc.entityRenderer, SHADER);
            } catch (ReflectiveOperationException ignored) {
            }
            group = mc.entityRenderer.getShaderGroup();
            if (!ours(group) || dead(group)) {
                this.failed = true;
                this.message("&cCouldn't load the shader, check the game log.");
                return;
            }
        }
        List<Shader> list = shaders(group);
        float uniform = 1.0f - Math.min((float) blurAmount.getInput() / 10.0f, 0.9f);
        list.get(0).getShaderManager().getShaderUniform("Phosphor").set(uniform, 0.0f, 0.0f);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (ours(mc.entityRenderer.getShaderGroup())) {
            mc.entityRenderer.stopUseShader();
        }
    }
}
