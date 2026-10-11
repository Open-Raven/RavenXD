package xyz.daikirai.script;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandSender;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

public class ScriptDefaults
implements Defaults {
    private static ExecutorService f117391;

    public static int mdd3633583e7d33() {
        return inventory.getSize();
    }

    public static boolean m1af74fef8fc377() {
        return client.allowEditing();
    }

    public static void m546db8b3dc50f4(CPacket cPacket) {
        client.sendPacket(cPacket);
    }

    public static boolean m4d9ecce632e9c8() {
        return client.isSingleplayer();
    }

    public static void mac039f9b7d3ae3(String string, Object object) {
        bridge.add(string, object);
    }

    public static void m3d0ce66b6ef37d() {
        bridge.clear();
    }

    public static void mf33a824ef31eec(float f, float f2, float f3) {
        gl.normal(f, f2, f3);
    }

    public static void m185ad04565aad7(String string, float f, float f2, float f3, int n, boolean bl) {
        render.text(string, f, f2, f3, n, bl);
    }

    public static void protected_resetCache(boolean bl) {
        if (bl) {
            Iterator iterator = Entity.access$100().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (ScriptDefaults.mc.theWorld.getEntityByID(((Entity)entry.getValue()).entityId) != null) continue;
                iterator.remove();
            }
        } else {
            Entity.access$100().clear();
        }
    }

    public static Object[] m4f5cda78272a4d(double d) {
        return client.raycastEntity(d);
    }

    public static void me892fc59daeb87(int n, float f) {
        render.bloom.apply(n, f);
    }

    public static void mfa7a986f479fc5() {
        client.swing();
    }

    public static void m2fc39d62d9f641(Vec3 vec3, double d, double d2, int n, float f) {
        render.drawBeam(vec3, d, d2, n, f);
    }

    public static void mde7d7e4a3985e8(boolean bl) {
        gl.enableItemLighting(bl);
    }

    public static void m29132265b76392(double d, double d2) {
        gl.texCoord2(d, d2);
    }

    public static void m953a5f7ddfc415(boolean bl) {
        gl.scissor(bl);
    }

    public static void mdc3ab0a92aa4a4(double d) {
        gl.lineWidth(d);
    }

    public static Map<String, Object> m00ad2a4437038f(String string) {
        return modules.getSettings(string);
    }

    public static void md5e586c80dd84c(boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        gl.colorMask(bl, bl2, bl3, bl4);
    }

    public static boolean me3259e23ea00b4() {
        return client.isFlying();
    }

    public static void me800e361c6285e(Vec3 vec3) {
        client.setMotion(vec3);
    }

    public static double mc1569e33fa2277(double d) {
        return Math.sin(d);
    }

    public static void meecca183577287(int n) {
        client.removePotionEffect(n);
    }

    public static ItemStack mf8330127bfd6f8(int n) {
        return inventory.getStackInChestSlot(n);
    }

    public static String mdfd946b5a90474(String string, String string2) {
        return modules.getSliderString(string, string2);
    }

    public static boolean m28a24107977ad8(String string) {
        return client.isFriend(string);
    }

    public static double md7aaa2f08e71ca(String string, String string2) {
        return modules.getSliderMax(string, string2);
    }

    public static int m4ab5a3282b8c47() {
        return client.timeTag();
    }

    public static int m36e1e4a8abbf23(int n, int n2) {
        return util.randomInt(n, n2);
    }

    public static boolean m1c968ea86f53ba(String string) {
        return bridge.has(string);
    }

    public static int me330f9aed55da9() {
        return gl.getBoundTexture();
    }

    public static void m5eba1266aa2c40(double d, double d2, double d3, double d4) {
        gl.color(d, d2, d3, d4);
    }

    public static void m9b0e5e0b1bac1a(Entity entity, int n, float f, boolean bl, boolean bl2) {
        render.entity(entity, n, f, bl, bl2);
    }

    public static void m8e2c353266dfc0(double d, double d2, double d3, double d4, float f, int n) {
        render.line2D(d, d2, d3, d4, f, n);
    }

    public static boolean m27f8906475f111() {
        return client.isJump();
    }

    public static float mc15bbb9c75c1ca() {
        return client.getForward();
    }

    public static void m8605b32c6a144d(String string, String string2) {
        client.setTabName(string, string2);
    }

    public static void maf259e9b5e39ec(CPacket cPacket) {
        client.sendPacketNoEvent(cPacket);
    }

    public static void ma07fe23195153e(String string) {
        client.removeFriend(string);
    }

    public static Block me055b71d99fb47(int n, int n2, int n3) {
        return world.getBlockAt(n, n2, n3);
    }

    public static int ma6212788a85c03(String string) {
        return render.getThemeRGB(string);
    }

    public static void mc6b713c110a518(Image image, float f, float f2, float f3, float f4) {
        render.image(image, f, f2, f3, f4);
    }

    public static String m855b15bb2ae8a8() {
        return world.getTitleText();
    }

    public static void me0a1c755ccff88(float f, float f2, float f3) {
        gl.vertex3(f, f2, f3);
    }

    public static String m6b1068853973b1() {
        return inventory.getChest();
    }

    public static Object med2be72c9bff0b(String string) {
        return bridge.get(string);
    }

    public static int m7986035be6be7e(int n, int n2) {
        return Math.max(n, n2);
    }

    public static void me09283c565c742(double d, double d2, double d3) {
        gl.scale(d, d2, d3);
    }

    public static boolean mef8419966fdac8() {
        return client.isFacingDiagonal();
    }

    public static void mec665253088a75(String string, boolean bl) {
        keybinds.setPressed(string, bl);
    }

    public static void m1ccc03bf1dcc02(float f) {
        client.setRenderArmPitch(f);
    }

    public static boolean m3e922db3bd36db(String string) {
        return client.runRavenCommand(string);
    }

    static ExecutorService access$200() {
        return f117391;
    }

    public static Entity m4b5e8525dbfdfc() {
        return modules.getAimAssistTarget();
    }

    public static void m04799cddcc8a7b(double d, double d2, double d3, double d4) {
        gl.rotate(d, d2, d3, d4);
    }

    public static Entity m98c33d21a00129() {
        return modules.getKillAuraTarget();
    }

    public static float[] maee49d39b1f7df(Vec3 vec3) {
        return client.getRotations(vec3);
    }

    public static void m8c41e7a2f96bd3(String string, String string2, int n, int n2, int n3) {
        world.displayTitleText(string, string2, n, n2, n3);
    }

    public static void m05e90ff16070da(Vec3 vec3, float f, int n, float f2) {
        render.tracer(vec3, f, n, f2);
    }

    public static double m351d12ebfcdc1f(String string, String string2) {
        return modules.getSlider(string, string2);
    }

    public static void m0ecbf38f7d5747(String string) {
        client.chat(string);
    }

    public static boolean mc586d0dc380c07() {
        return client.allowFlying();
    }

    public static void m8c29e8614a0ef2(Vec3 vec3, int n, boolean bl, boolean bl2) {
        render.block(vec3, n, bl, bl2);
    }

    static ExecutorService access$202(ExecutorService executorService) {
        f117391 = executorService;
        return f117391;
    }

    public static void ma8f1a79fb233d3(Entity entity) {
        client.attack(entity);
    }

    public static void me395e4aff1afa9(int n, float f) {
        render.blur.apply(n, f);
    }

    public static String m1c8bb2663e1742() {
        return client.getVersion();
    }

    public static boolean m95afc2ac9f9f84() {
        return modules.isScaffolding();
    }

    public static Vec3 m16f2b0301ec88f() {
        return modules.getWhitelistedBedPos();
    }

    public static void m6bab061890dc7c() {
        keybinds.leftClick();
    }

    public static void m7d3d20b91b9cd4() {
        client.jump();
    }

    public static void m29132265b76392(float f, float f2) {
        gl.texCoord2(f, f2);
    }

    public static boolean m9d418a3b9fcbb6() {
        return client.isCreative();
    }

    public static String m5e4509156c9320() {
        return client.getDirection();
    }

    public static void mecc0542efd6108() {
        gl.disableItemLighting();
    }

    public static void mecf90067eafae7() {
        client.pling();
    }

    public static void m42729bbc9cb0a5() {
        render.blur.prepare();
    }

    public static void m04799cddcc8a7b(float f, float f2, float f3, float f4) {
        gl.rotate(f, f2, f3, f4);
    }

    public static double m854902085fd2c2(double d) {
        return Math.cos(d);
    }

    public static long mc8e4bee14e04e0() {
        return client.getTotalMemory();
    }

    public static long mcc90058f464ac0() {
        return client.getFreeMemory();
    }

    public static boolean m6dbc5b6f65b465(long l) {
        return client.sleep(l);
    }

    public static void m80838a7cde9cf3(boolean bl) {
        client.setSneak(bl);
    }

    public static void m68463e7ed61479(boolean bl) {
        client.setJump(bl);
    }

    public static boolean m540db9ef457c96(String string) {
        return client.runForgeCommand(string);
    }

    public static Vec3 m561fb4b556a781() {
        return client.getMotion();
    }

    public static int m453a86ec2d6a16(int n) {
        return Math.abs(n);
    }

    public static List<String> meb3ff1ee276835() {
        return world.getScoreboard();
    }

    public static float medd3f4994c880b(float f, float f2) {
        return Math.min(f, f2);
    }

    public static boolean m281863e2efcda6() {
        return world.exists();
    }

    public static double m453a86ec2d6a16(double d) {
        return Math.abs(d);
    }

    public static String md1b86243863a44() {
        return client.getScreen();
    }

    public static void m05e90ff16070da(Entity entity, float f, int n, float f2) {
        render.tracer(entity, f, n, f2);
    }

    public static String getScriptName() {
        return ScriptBridge.getScriptName();
    }

    public static void m5eba1266aa2c40(int n) {
        gl.color(n);
    }

    public static double medd3f4994c880b(double d, double d2) {
        return Math.min(d, d2);
    }

    public static String mebf2506b505f4b() {
        return client.getServerIP();
    }

    public static void mb3e1a5077344ee(Vec3 vec3, double d, int n, double d2, int n2) {
        render.draw3DPolygon(vec3, d, n, d2, n2);
    }

    public static void mac039f9b7d3ae3(String string) {
        bridge.add(string);
    }

    public static double m7986035be6be7e(double d, double d2) {
        return Math.max(d, d2);
    }

    public static Object ma9669d9f2a5ffe(String string) {
        return bridge.remove(string);
    }

    public static long medd3f4994c880b(long l, long l2) {
        return Math.min(l, l2);
    }

    public static float[] m1827f9253f9864(Vec3 vec3, String string) {
        return client.getRotationsToBlock(vec3, string);
    }

    public static void m38bc00efa958fc(ItemStack itemStack, float f, float f2, float f3) {
        render.item(itemStack, f, f2, f3);
    }

    public static void m67e7e838f620c3(double d) {
        client.setTimer(d);
    }

    public static void m0268d71e7608c3(float f) {
        client.setStrafe(f);
    }

    public static void mec23a44e078eb3(double d) {
        client.multiplyMotion(d);
    }

    public static List<NetworkPlayer> m90948d6ff3e8fa() {
        return world.getNetworkPlayers();
    }

    public static float m453a86ec2d6a16(float f) {
        return Math.abs(f);
    }

    public static void m8de38ef0beefb4(Entity entity, int n, int n2, float f, float f2, int n3) {
        render.entityGui(entity, n, n2, f, f2, n3);
    }

    public static String m64cc72e128f071() {
        return config.getDirectory();
    }

    public static long m0b6c53e6038cb3() {
        return client.getMaxMemory();
    }

    public static double m040a8eeaf7f24c(double d, double d2) {
        return Math.atan2(d, d2);
    }

    public static int m77f53666dde1e4() {
        return client.getUID();
    }

    public static void m9866379cacfb36(float f, float f2, float f3, float f4, float f5, int n) {
        render.roundedRect(f, f2, f3, f4, f5, n);
    }

    public static String mbaeb6f1b627814(String string) {
        return util.strip(string);
    }

    public static boolean med53d00cb85603(String string, String string2) {
        return modules.getButton(string, string2);
    }

    public static void mb7aa1d5d8e0d32(int n) {
        gl.begin(n);
    }

    public static boolean m21eba45aea8060(String string) {
        return keybinds.isPressed(string);
    }

    public static ItemStack mcddb23d9d93af2(int n) {
        return inventory.getStackInSlot(n);
    }

    public static void me09283c565c742(float f, float f2, float f3) {
        gl.scale(f, f2, f3);
    }

    public static boolean m5f6dfdc85e1411() {
        return client.isSneak();
    }

    public static void m2db878eb3da599(String string, String string2, boolean bl) {
        modules.setButton(string, string2, bl);
    }

    public static double me1a1d4dcccfab5(double d) {
        return Math.tan(d);
    }

    public static Block me055b71d99fb47(Vec3 vec3) {
        return world.getBlockAt(vec3);
    }

    public static void m05e90ff16070da(double d, double d2, double d3, float f, int n, float f2) {
        render.tracer(d, d2, d3, f, n, f2);
    }

    public static int md954cf44f2f89b() {
        return inventory.getSlot();
    }

    public static String m50c4ded863baed(String string) {
        return config.get(string);
    }

    public static void mf2079d4b61ac85() {
        world.clearTitleText();
    }

    public static boolean med58d44c6355fc() {
        return client.isDiagonal();
    }

    public static long m871f660ab8aaa5(double d) {
        return Math.round(d);
    }

    public static long m453a86ec2d6a16(long l) {
        return Math.abs(l);
    }

    public static String m8921a371a8fe22(String string) {
        return util.color(string);
    }

    public static double mdd9a49f77d375a(String string, String string2) {
        return modules.getSliderInterval(string, string2);
    }

    public static double m2f24b888fe3f96(double d, int n) {
        return util.round(d, n);
    }

    public static Entity m290e3ed6a26d96() {
        return modules.getBacktrackTarget();
    }

    public static int mc1c335986adfd9() {
        return render.getFontHeight();
    }

    public static boolean mcb8294fd6e78fd(Entity entity) {
        return client.canSeeEntity(entity);
    }

    public static boolean mb6cb1cd3241227(int n) {
        return keybinds.isMouseDown(n);
    }

    public static String m95650c6a61892e() {
        return world.getTabFooter();
    }

    public static void m7b390610ce13f1(Vec3 vec3, Vec3 vec32, float f, int n) {
        render.line3D(vec3, vec32, f, n);
    }

    public static void m2fbe45d8b38789(boolean bl) {
        client.setFlying(bl);
    }

    public static Vec3 mbaae2e9b6ce051() {
        return modules.getBedAuraPosition();
    }

    public static void m1bbc8a0ce32204(boolean bl) {
        gl.alpha(bl);
    }

    public static void m8246c06a696131(String string) {
        modules.enable(string);
    }

    public static String me09691ac655c4c() {
        return world.getSubtitleText();
    }

    public static void m953a5f7ddfc415(int n, int n2, int n3, int n4) {
        gl.scissor(n, n2, n3, n4);
    }

    public static void me0952c6b0fb76a(float f, float f2) {
        gl.vertex2(f, f2);
    }

    public static double m16a99ae0de7452(String string, String string2) {
        return modules.getSliderMin(string, string2);
    }

    public static void m8c29e8614a0ef2(int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
        render.block(n, n2, n3, n4, bl, bl2);
    }

    public static boolean m24be62d71cc84c(String string) {
        return gl.bindTexture(string);
    }

    public static void mf58ad24838894d(boolean bl) {
        gl.lighting(bl);
    }

    public static void m2426d48c0afb22(int n) {
        gl.disable(n);
    }

    public static void m472985965311d3(double d) {
        client.setSpeedForward(d);
    }

    public static void mc954fdf467b8c9(boolean bl) {
        client.dropItem(bl);
    }

    public static List<Entity> m2878149fb33f9c() {
        return world.getEntities();
    }

    public static void me0a1c755ccff88(double d, double d2, double d3) {
        gl.vertex3(d, d2, d3);
    }

    public static double m1faebc42cb44d0(double d, double d2) {
        return util.randomDouble(d, d2);
    }

    public static void m7099690dcd081f(boolean bl) {
        gl.polygonOffset(bl);
    }

    public static void m6eb36c7aa4e10c(boolean bl) {
        gl.cull(bl);
    }

    public static boolean mbd17e59e39c093(String string, String string2) {
        return config.set(string, string2);
    }

    public static Map<String, List<String>> m83af62475f6081() {
        return modules.getCategories();
    }

    public static String mde70cdae6121c9() {
        return world.getDimension();
    }

    public static Object[] m2185c0d24cd9b7(double d) {
        return client.raycastBlock(d);
    }

    public static void m1a50b61fe803b7(float f) {
        client.setForward(f);
    }

    public static void m0dee334df8cbf8(String string) {
        client.removeEnemy(string);
    }

    public static int medd3f4994c880b(int n, int n2) {
        return Math.min(n, n2);
    }

    public static Entity mf5c3d5169e5fd4() {
        return modules.getLagRangeTarget();
    }

    public static void me8fce7cd6becd0() {
        gl.resetColor();
    }

    public static boolean m51699116dc0d32() {
        return client.isMoving();
    }

    public static String m1141c097c1d56c(String string) {
        return util.encodeb64(string);
    }

    public static boolean ma340d7b22f2cbd() {
        return modules.isTowering();
    }

    public static int m871f660ab8aaa5(float f) {
        return Math.round(f);
    }

    public static int maa00019f32541b(String string) {
        return keybinds.getKeycode(string);
    }

    public static void m6de92a3398625f(boolean bl) {
        gl.blend(bl);
    }

    public static void mdc3ab0a92aa4a4(float f) {
        gl.lineWidth(f);
    }

    public static void m8ed0a0604d0b3b(double d) {
        client.setSpeed(d);
    }

    public static void m8df4a6c012f52c() {
        gl.pop();
    }

    public static void md4e19304382adf() {
        client.swingReset();
    }

    public static boolean m4244608c71e6e5(Vec3 vec3, String string, Vec3 vec32) {
        return client.placeBlock(vec3, string, vec32);
    }

    public static void mef74bbf389a75c(int n) {
        inventory.setSlot(n);
    }

    public static int m0d3e2df49b5358(String string) {
        return render.getFontWidth(string);
    }

    public static void me2100da9b36a7a(boolean bl) {
        client.setSprinting(bl);
    }

    public static String m51e8a7d3c9bf42() {
        return ScriptDefaults.getScriptName();
    }

    public static float[] ma6a5b35d1711dd() {
        return modules.getBedAuraProgress();
    }

    public static void mcfd033cca02a03() {
        keybinds.rightClick();
    }

    public static boolean me0ef0d2d6d8947(String string) {
        return modules.isEnabled(string);
    }

    public static void md5f03fca9857d7(Object object) {
        client.log(object);
    }

    public static Vec3 m282ffeba28dcb5(double d, double d2, double d3, int n, float f) {
        return render.worldToScreen(d, d2, d3, n, f);
    }

    public static Entity ma1da5bf03ec1d5() {
        return client.getPlayer();
    }

    public static void protected_setupCache() {
        if (keybinds.access$000().isEmpty()) {
            for (KeyBinding keyBinding : ScriptDefaults.mc.gameSettings.keyBindings) {
                String string = keyBinding.getKeyDescription().replaceFirst("key\\.", "");
                keybinds.access$000().put(string, keyBinding);
            }
        }
    }

    public static void m2fc39d62d9f641(double d, double d2, double d3, double d4, double d5, int n, float f) {
        render.drawBeam(d, d2, d3, d4, d5, n, f);
    }

    public static void m3fca3d49bc5c40(String string, float f, float f2) {
        client.playSound(string, f, f2);
    }

    public static void m8d292598440528(int n) {
        gl.bindTextureId(n);
    }

    public static long mf05c888f157741() {
        return client.nanoTime();
    }

    public static List<Entity> m30fd0ef1340249() {
        return world.getPlayerEntities();
    }

    public static void ma03258600c2d43() {
        gl.push();
    }

    public static void m273b7e3f39ca1e(String string) {
        modules.disable(string);
    }

    public static int m3864f8b6ab2ac5() {
        return inventory.getChestSize();
    }

    public static void mb336c0d5a55ff1(SPacket sPacket) {
        client.processPacket(sPacket);
    }

    public static float m7986035be6be7e(float f, float f2) {
        return Math.max(f, f2);
    }

    public static void mf33a824ef31eec(double d, double d2, double d3) {
        gl.normal(d, d2, d3);
    }

    public static String m65fddd71d64518() {
        return inventory.getContainer();
    }

    public static void mc57265675f94e9(double d, double d2, double d3) {
        gl.translate(d, d2, d3);
    }

    public static boolean m7f9bd98a84114e(double d, double d2, double d3, double d4, double d5, double d6) {
        return render.isInView(d, d2, d3, d4, d5, d6);
    }

    public static void mb3e1a5077344ee(double d, double d2, double d3, double d4, int n, double d5, int n2) {
        render.draw3DPolygon(d, d2, d3, d4, n, d5, n2);
    }

    public static void m7099690dcd081f(float f, float f2) {
        gl.polygonOffset(f, f2);
    }

    public static String m512063da427e55() {
        return world.getTabHeader();
    }

    public static boolean m7f9bd98a84114e(Entity entity) {
        return render.isInView(entity);
    }

    public static boolean m70b304793c2be6(String string) {
        return client.isEnemy(string);
    }

    public static double[] mc9cc9ee3c096ef() {
        return render.getRotations();
    }

    public static void m5f938e6a2bf3dc() {
        render.bloom.prepare();
    }

    public static float m553f62a0e7c256() {
        return client.getRenderArmPitch();
    }

    public static double m3fd9a8cdd0b860(double d) {
        return Math.sqrt(d);
    }

    public static void m59b52fda01d0c1(String string) {
        client.addEnemy(string);
    }

    public static int m7b6f4c4f4213df() {
        return client.getFPS();
    }

    public static float m7e79b326fc6e30() {
        return client.getStrafe();
    }

    public static void m3820eb7af2f4de(Object object) {
        client.print(object);
    }

    public static void m847f3ea4155e20(boolean bl) {
        gl.texture2d(bl);
    }

    public static void ma79704fafafda0() {
        inventory.open();
    }

    public static String m83eba01017358a() {
        return client.getUser();
    }

    public static void mc57265675f94e9(float f, float f2, float f3) {
        gl.translate(f, f2, f3);
    }

    public static boolean m3cb75614ee4976(int n) {
        return keybinds.isKeyDown(n);
    }

    public static void m6acae7cce7b395(Runnable runnable) {
        client.async(runnable);
    }

    public static void mbc99457df9804f(boolean bl) {
        gl.lineSmooth(bl);
    }

    public static void m667a512a1583a1(int n, int n2, int n3) {
        inventory.click(n, n2, n3);
    }

    public static void mc9e9651588b2cd(boolean bl) {
        gl.depth(bl);
    }

    public static Object[] m2185c0d24cd9b7(double d, float f, float f2) {
        return client.raycastBlock(d, f, f2);
    }

    public static float[] md76d1acc7d335e(Entity entity) {
        return client.getRotationsToEntity(entity);
    }

    public static long mf2e1f6b7d7175f() {
        return client.time();
    }

    public static Vec3 m282ffeba28dcb5(Vec3 vec3, int n, float f) {
        return render.worldToScreen(vec3, n, f);
    }

    public static void m6dc53bb6d3e9a0(int n) {
        gl.enable(n);
    }

    public static void me800e361c6285e(double d, double d2, double d3) {
        client.setMotion(d, d2, d3);
    }

    public static void mfc369f2f7806b9(boolean bl) {
        gl.depthMask(bl);
    }

    public static void mbc0ea1e8a151f8(String string, String string2, double d) {
        modules.setSlider(string, string2, d);
    }

    public static String m19faf4ecb4ba48(PlayerState playerState) {
        return client.getServerDirection(playerState);
    }

    public static void m5eba1266aa2c40(float f, float f2, float f3, float f4) {
        gl.color(f, f2, f3, f4);
    }

    public static void mff16ce049ee001(float f, float f2, float f3, float f4, int n) {
        render.rect(f, f2, f3, f4, n);
    }

    public static void m7b390610ce13f1(double d, double d2, double d3, double d4, double d5, double d6, float f, int n) {
        render.line3D(d, d2, d3, d4, d5, d6, f, n);
    }

    public static long m7986035be6be7e(long l, long l2) {
        return Math.max(l, l2);
    }

    public static List<String> mdfecbab2d0614d() {
        return inventory.getBookContents();
    }

    public static boolean m08cc5285f9c491(Entity entity) {
        return world.isValidEntity(entity);
    }

    public static int[] m9d43496368ef56() {
        return keybinds.getMousePosition();
    }

    public static int[] m18c1a7869e06b8() {
        return client.getDisplaySize();
    }

    public static void m6819a202b32c17() {
        client.closeScreen();
    }

    public static float[] m1827f9253f9864(Vec3 vec3) {
        return client.getRotationsToBlock(vec3);
    }

    public static Vec3 m184663ac3accc6() {
        return render.getPosition();
    }

    public static void m5eba1266aa2c40(int n, int n2, int n3, int n4) {
        gl.color(n, n2, n3, n4);
    }

    public static String mfd44150f0ec073() {
        return world.getMode();
    }

    public static void mcde6e3f1988429(int n) {
        gl.depthFunc(n);
    }

    public static void me383a8a14f149b() {
        gl.end();
    }

    public static Map<String, List<String>> mc42e80c3dcf47d() {
        return world.getTeams();
    }

    public static String ma66488b570e2f5(String string) {
        return util.decodeb64(string);
    }

    public static Object[] m4f5cda78272a4d(double d, float f, float f2) {
        return client.raycastEntity(d, f, f2);
    }

    public static void me0952c6b0fb76a(double d, double d2) {
        gl.vertex2(d, d2);
    }

    public static Entity mbb65122787aa51(int n) {
        return world.getEntityById(n);
    }

    public static List<TileEntity> mca3366b26c7d6f() {
        return world.getTileEntities();
    }

    public static void mcc178139d03b37(String string) {
        client.addFriend(string);
    }

    public static double m9e30ee825cc7a8(double d) {
        return Math.toRadians(d);
    }

    public static class Response {
        String contents;
        int responseCode;

        public Json json() {
            return this.contents == null ? null : new Json(this.contents);
        }

        public Response(int n, String string) {
            this.responseCode = n;
            this.contents = string;
        }

        public String toString() {
            return "Response(" + this.responseCode + ")";
        }

        public String string() {
            return this.contents;
        }

        public int code() {
            return this.responseCode;
        }
    }

    public static class inventory {
        private static int f116879 = Integer.reverse(1817099556);
        private static int f116880 = -1303652573 >>> 156 | -1303652573 << ~156 + 1;
        private static int f116881 = Integer.reverse(1885954710);
        private static int f116882 = -158045856 >>> 28 | -158045856 << ~28 + 1;
        private static int f116883 = -1522840660 >>> 255 | -1522840660 << -255;
        private static int f116884 = 783455469 >>> 113 | 783455469 << -113;
        private static int f116885 = Integer.reverse(1180116769);
        private static int f116886 = -1590084968 >>> 190 | -1590084968 << -190;

        public static int getSlot() {
            return Defaults.mc.thePlayer.inventory.currentItem;
        }

        public static void setSlot(int n) {
            Defaults.mc.thePlayer.inventory.currentItem = n;
        }

        public static String getChest() {
            if (!(Defaults.mc.thePlayer.openContainer instanceof ContainerChest)) {
                return "";
            }
            return ((ContainerChest)Defaults.mc.thePlayer.openContainer).getLowerChestInventory().getName();
        }

        public static ItemStack getStackInSlot(int n) {
            return ItemStack.of(Defaults.mc.thePlayer.inventory.getStackInSlot(n));
        }

        public static int getSize() {
            return Defaults.mc.thePlayer.inventory.getSizeInventory();
        }

        public static void open() {
            int n;
            int n2 = n = Defaults.mc.gameSettings.keyBindInventory.getKeyCode();
            if (n == 0) {
                n2 = n;
                n = 13;
                Defaults.mc.gameSettings.keyBindInventory.setKeyCode(13);
                KeyBinding.resetKeyBindingArrayAndHash();
            }
            KeyBinding.setKeyBindState((int)n, (boolean)true);
            KeyBinding.onTick((int)n);
            KeyBinding.setKeyBindState((int)n, (boolean)false);
            if (n2 != n) {
                Defaults.mc.gameSettings.keyBindInventory.setKeyCode(n2);
                KeyBinding.resetKeyBindingArrayAndHash();
            }
        }

        public static List<String> getBookContents() {
            return ScriptBridge.getBookContents();
        }

        public static int getChestSize() {
            if (!(Defaults.mc.thePlayer.openContainer instanceof ContainerChest)) {
                return 0;
            }
            return ((ContainerChest)Defaults.mc.thePlayer.openContainer).getLowerChestInventory().getSizeInventory();
        }

        public static String getContainer() {
            if (Defaults.mc.thePlayer.openContainer == null) {
                return "";
            }
            return Defaults.mc.thePlayer.openContainer.getClass().getSimpleName();
        }

        public static void click(int n, int n2, int n3) {
            ScriptBridge.guiClick(n, n2, n3);
        }

        public static ItemStack getStackInChestSlot(int n) {
            if (!(Defaults.mc.thePlayer.openContainer instanceof ContainerChest)) {
                return null;
            }
            return ItemStack.of(((ContainerChest)Defaults.mc.thePlayer.openContainer).getLowerChestInventory().getStackInSlot(n));
        }
    }

    public static class Json {
        JsonElement json;
        private static int f116895 = (-636169792 >>> 47 | -636169792 << ~47 + 1) & 0xFFFFFFFF;
        private static int f116896 = Integer.reverse(338526661);
        private static int f116897 = Integer.reverse(280299935);
        private static int f116898 = Integer.reverse(280299935);
        private static int f116899 = Integer.reverse(1762640735);
        private static int f116900 = (2115174232 >>> 45 | 2115174232 << ~45 + 1) & 0xFFFFFFFF;
        private static int f116901 = Integer.reverse(890090817);
        private static int f116902 = -1240100779 >>> 77 | -1240100779 << ~77 + 1;

        public String string() {
            if (this.exists()) {
                if (!this.json.isJsonArray()) {
                    try {
                        return this.json.getAsString();
                    }
                    catch (UnsupportedOperationException unsupportedOperationException) {
                    }
                }
                return this.json.toString();
            }
            return "";
        }

        public String get(String string, String string2) {
            String string3 = this.get(string);
            return string3.isEmpty() ? string2 : string3;
        }

        public boolean exists() {
            return this.json != null;
        }

        public Json(String string) {
            this.json = new JsonParser().parse(string);
        }

        public boolean equals(Json json) {
            return (json != null && (this.json == json.json || this.exists() && json.exists() && this.string().equals(json.string())) ? 1 : 0) != 0;
        }

        public List<Json> array(String string) {
            ArrayList<Json> arrayList = new ArrayList<Json>();
            for (JsonElement jsonElement : string == null ? this.json.getAsJsonArray() : ((JsonObject)this.json).getAsJsonArray(string)) {
                arrayList.add(new Json((byte) 0, jsonElement));
            }
            return arrayList;
        }

        public String toString() {
            return "Json()";
        }

        public List<Json> array() {
            return this.array(null);
        }

        public Json object() {
            return this.object(null);
        }

        private Json(byte by, JsonElement jsonElement) {
            this.json = jsonElement;
        }

        public String get(String string) {
            return ScriptUtil.jsonString((JsonObject)this.json, string);
        }

        public Json object(String string) {
            return new Json((byte) 0, (JsonElement)(string == null ? this.json.getAsJsonObject() : ((JsonObject)this.json).getAsJsonObject(string)));
        }

        public Map<String, Json> map() {
            HashMap<String, Json> hashMap = new HashMap<String, Json>();
            for (Map.Entry entry : ((JsonObject)this.json).entrySet()) {
                hashMap.put((String)entry.getKey(), new Json((byte) 0, (JsonElement)entry.getValue()));
            }
            return hashMap;
        }
    }

    public static class modules {
        private static int f116913 = Integer.reverse(1526155430);
        private static int f116914 = Integer.reverse(452413606);
        private static int f116915 = Integer.reverse(-1268781368);
        private static int f116916 = 2141376823 >>> 108 | 2141376823 << -108;
        private static int f116917 = Integer.reverse(-23431187);
        private static int f116918 = Integer.reverse(1050310637);
        private static int f116919 = Integer.reverse(589904693);
        private static int f116920 = Integer.reverse(589904693);
        private static int f116921 = Integer.reverse(1769908760);
        private static int f116922 = -1085731798 >>> 15 | -1085731798 << -15;
        private static int f116923 = Integer.reverse(445941078);
        private static int f116924 = Integer.reverse(445941078);
        private static int f116925 = (333314624 >>> 247 | 333314624 << ~247 + 1) & 0xFFFFFFFF;
        private static int f116926 = Integer.reverse(-469684259);
        private static int f116927 = 1042583037 >>> 124 | 1042583037 << ~124 + 1;
        private static int f116928 = Integer.reverse(-872738233);
        private static int f116929 = 1000081911 >>> 207 | 1000081911 << ~207 + 1;
        private static int f116930 = Integer.reverse(-1662093368);

        public static double getSliderMin(String string, String string2) {
            return (Double)modules.getSliderData(string, string2, 2, 0);
        }

        public static Entity getBacktrackTarget() {
            return Entity.of(ScriptBridge.getBacktrackTarget());
        }

        public static float[] getBedAuraProgress() {
            return ScriptBridge.getBedAuraProgress();
        }

        public static double getSliderMax(String string, String string2) {
            return (Double)modules.getSliderData(string, string2, 3, 0);
        }

        public static boolean isEnabled(String string) {
            return ScriptBridge.isEnabled(string);
        }

        public static void registerSlider(String string, String string2, double d, double d2, double d3, double d4) {
            ScriptBridge.registerSlider(string, string2, d, d2, d3, d4);
        }

        public static boolean isTowering() {
            return ScriptBridge.isTowering();
        }

        public static Entity getLagRangeTarget() {
            return Entity.of(ScriptBridge.getLagRangeTarget());
        }

        public static boolean isScaffolding() {
            return ScriptBridge.isScaffolding();
        }

        public static double getSliderInterval(String string, String string2) {
            return (Double)modules.getSliderData(string, string2, 4, 0);
        }

        public static Map<String, Object> getSettings(String string) {
            return ScriptBridge.getSettings(string);
        }

        public static boolean isHidden(String string) {
            return ScriptBridge.isHidden(string);
        }

        public static Vec3 getWhitelistedBedPos() {
            BlockPos blockPos = ScriptBridge.getWhitelistedBedPos();
            return blockPos == null ? null : new Vec3(blockPos);
        }

        public static void setSlider(String string, String string2, double d) {
            ScriptBridge.setSlider(string, string2, d);
        }

        public static void disable(String string) {
            ScriptBridge.disable(string);
        }

        public static Entity getAimAssistTarget() {
            return Entity.of(ScriptBridge.getAimAssistTarget());
        }

        public static Vec3 getBedAuraPosition() {
            BlockPos blockPos = ScriptBridge.getBedAuraPosition();
            return blockPos == null ? null : new Vec3(blockPos);
        }

        private static Object getSliderData(String string, String string2, int n, Object object) {
            Object[] objectArray = ScriptBridge.getSlider(string, string2);
            return objectArray == null ? object : objectArray[n];
        }

        public static double getSlider(String string, String string2) {
            return (Double)modules.getSliderData(string, string2, 0, 0);
        }

        public static void enable(String string) {
            ScriptBridge.enable(string);
        }

        public static Map<String, List<String>> getCategories() {
            return ScriptBridge.getCategories();
        }

        public static void setButton(String string, String string2, boolean bl) {
            ScriptBridge.setButton(string, string2, bl);
        }

        public static void registerSlider(String string, String string2, int n, String[] stringArray) {
            ScriptBridge.registerSlider(string, string2, n, stringArray);
        }

        public static String getSliderString(String string, String string2) {
            return (String)modules.getSliderData(string, string2, 1, "");
        }

        public static void registerButton(String string, boolean bl) {
            ScriptBridge.registerButton(string, bl);
        }

        public static boolean getButton(String string, String string2) {
            return ScriptBridge.getButton(string, string2);
        }

        public static Entity getKillAuraTarget() {
            return Entity.of(ScriptBridge.getKillAuraTarget());
        }

        public static void registerDescription(String string) {
            ScriptBridge.registerDescription(string);
        }
    }

    public static class config {
        private static final long MAX_CONFIG_SIZE_BYTES = 0x100000L;
        private static final String CONFIG_DIR;
        private static final String SEPARATOR = ":";
        private static final String SEPARATOR_FULL = ": ";
        private static long f116723;
        private static long f116724;
        private static long f116725;
        private static int f116726;
        private static int f116727;
        private static int f116728;
        private static int f116729;
        private static int f116730;
        private static int f116731;
        private static int f116732;
        private static int f116733;
        private static int f116734;
        private static int f116735;
        private static long f116736;
        private static long f116737;
        private static long f116738;
        private static int f116739;
        private static int f116740;
        private static int f116741;
        private static int f116742;
        private static int f116743;
        private static int f116744;
        private static int f116745;
        private static int f116746;
        private static int f116747;
        private static int f116748;
        private static int f116749;
        private static int f116750;

        static {
            f116723 = Long.reverse(0x25CAC147D13BA2AFL ^ 0x144DEC140CD4C2F1L);
            f116724 = Long.reverse(0x78CB8BC3106DAE6AL ^ 0xA621A516DB1AFB3EL);
            f116725 = Long.reverse(0x704E5978F5E44782L ^ 0xAEA47FAD3E9312D6L);
            f116726 = 340997998 >>> 231 | 340997998 << -231;
            f116727 = Integer.reverse(1986335803);
            f116728 = (1020572599 >>> 232 | 1020572599 << ~232 + 1) & 0xFFFFFFFF;
            f116729 = (-1701415193 >>> 237 | -1701415193 << ~237 + 1) & 0xFFFFFFFF;
            f116730 = Integer.reverse(-1621148692);
            f116731 = Integer.reverse(-1621148692);
            f116732 = 1375058870 >>> 192 | 1375058870 << -192;
            f116733 = Integer.reverse(1843244938);
            f116734 = Integer.reverse(861837665);
            f116735 = Integer.reverse(861837665);
            f116736 = Long.reverse(0x8218BCF2413E4E9DL ^ 0xC8D4E287152C6B58L);
            f116737 = Long.reverse(0xCA7E6E3276388AF9L ^ 0x3EAFD1275E7D58D3L);
            f116738 = Long.reverse(0xE350EEDCA5785CDCL ^ 0x178159C98D3D8EF6L);
            f116739 = (2038065007 >>> 145 | 2038065007 << -145) & 0xFFFFFFFF;
            f116740 = Integer.reverse(-1120014900);
            f116741 = (-604721759 >>> 19 | -604721759 << ~19 + 1) & 0xFFFFFFFF;
            f116742 = Integer.reverse(2128358825);
            f116743 = 514567042 >>> 132 | 514567042 << ~132 + 1;
            f116744 = (-178430832 >>> 199 | -178430832 << -199) & 0xFFFFFFFF;
            f116745 = Integer.reverse(971950108);
            f116746 = Integer.reverse(971950108);
            f116747 = Integer.reverse(1443082175);
            f116748 = (-343877637 >>> 73 | -343877637 << -73) & 0xFFFFFFFF;
            f116749 = (-910617516 >>> 241 | -910617516 << -241) & 0xFFFFFFFF;
            f116750 = Integer.reverse(992367697);
            CONFIG_DIR = Defaults.mc.mcDataDir + File.separator + "keystrokes" + File.separator + "script_config.txt";
        }

        public static String get(String string) {
            if (string == null || string.isEmpty()) {
                return null;
            }
            try {
                config.ensureConfigFileExists();
                Path path = new File(CONFIG_DIR).toPath();
                if (Files.size(path) < (f116724 ^ f116725)) {
                    return Files.lines(path).filter(arg_0 -> config.lambda_get_0(string, arg_0)).findFirst().map(arg_0 -> config.lambda_get_1(string, arg_0)).orElse(null);
                }
            }
            catch (IOException iOException) {
            }
            return null;
        }

        private static boolean lambda_get_0(String string, String string2) {
            return string2.startsWith(string + SEPARATOR_FULL);
        }

        private static void ensureConfigFileExists() throws IOException {
            Path path = Paths.get(CONFIG_DIR, new String[0]);
            if (Files.notExists(path, new LinkOption[0])) {
                Files.createDirectories(path.getParent(), new FileAttribute[0]);
                Files.createFile(path, new FileAttribute[0]);
            }
        }

        private static String lambda_get_1(String string, String string2) {
            return string2.substring((string + SEPARATOR_FULL).length());
        }

        public static String getDirectory() {
            return CONFIG_DIR;
        }

        public static boolean set(String string, String string2) {
            if (string == null || string.isEmpty()) {
                return false;
            }
            string = string.replace(SEPARATOR, "");
            String string3 = string + SEPARATOR_FULL + string2;
            try {
                config.ensureConfigFileExists();
                Path path = new File(CONFIG_DIR).toPath();
                if (Files.size(path) < (f116737 ^ f116738)) {
                    ArrayList<String> arrayList = new ArrayList<String>(Files.readAllLines(path));
                    int n = 0;
                    for (int i = 0; i < arrayList.size(); ++i) {
                        String string4 = (String)arrayList.get(i);
                        if (!string4.startsWith(string + SEPARATOR_FULL)) continue;
                        arrayList.set(i, string3);
                        n = 1;
                        break;
                    }
                    if (n == 0) {
                        arrayList.add(string3);
                    }
                    Files.write(path, arrayList, new OpenOption[0]);
                    return true;
                }
            }
            catch (IOException iOException) {
            }
            return false;
        }
    }

    public static class C10
    extends CPacket {
        public ItemStack itemStack;
        public int slot;

        public C10(int n, ItemStack itemStack) {
            super((Packet)C10.create(n, itemStack));
        }

        public C10(byte by, C10PacketCreativeInventoryAction c10PacketCreativeInventoryAction) {
            super((Packet)c10PacketCreativeInventoryAction);
            this.slot = c10PacketCreativeInventoryAction.getSlotId();
            this.itemStack = ItemStack.of(c10PacketCreativeInventoryAction.getStack());
        }

        private static C10PacketCreativeInventoryAction create(int n, ItemStack itemStack) {
            return new C10PacketCreativeInventoryAction(n, itemStack == null ? null : itemStack.itemStack);
        }
    }

    public static class Entity {
        public boolean isLiving;
        private static HashMap<Integer, Entity> cacheMap;
        public boolean isPlayer;
        public int entityId;
        private net.minecraft.entity.Entity entity;
        public String type;
        public boolean isUser;
        private static int f116751;
        private static int f116752;
        private static int f116753;
        private static int f116754;
        private static int f116755;
        private static int f116756;
        private static int f116757;
        private static int f116758;
        private static int f116759;
        private static int f116760;
        private static float f116761;
        private static int f116762;
        private static int f116763;
        private static int f116764;
        private static int f116765;
        private static int f116766;
        private static int f116767;
        private static int f116768;
        private static int f116769;
        private static int f116770;
        private static int f116771;
        private static int f116772;
        private static int f116773;
        private static float f116774;
        private static int f116775;
        private static int f116776;
        private static int f116777;
        private static int f116778;
        private static double f116779;
        private static float f116780;
        private static int f116781;
        private static int f116782;
        private static int f116783;
        private static int f116784;
        private static int f116785;
        private static int f116786;
        private static int f116787;
        private static int f116788;
        private static double f116789;
        private static int f116790;
        private static int f116791;
        private static int f116792;
        private static int f116793;
        private static int f116794;
        private static int f116795;
        private static int f116796;
        private static int f116797;
        private static int f116798;
        private static int f116799;
        private static int f116800;
        private static int f116801;
        private static int f116802;
        private static int f116803;
        private static int f116804;
        private static int f116805;
        private static int f116806;
        private static int f116807;
        private static int f116808;
        private static int f116809;
        private static int f116810;
        private static int f116811;
        private static int f116812;
        private static int f116813;
        private static float f116814;
        private static int f116815;
        private static int f116816;

        public void setYaw(float f) {
            this.entity.rotationYaw = f;
        }

        public boolean isHoldingWeapon() {
            return (this.isLiving && ScriptBridge.isHoldingWeapon((EntityLivingBase)this.entity) ? 1 : 0) != 0;
        }

        public void setPitch(float f) {
            this.entity.rotationPitch = f;
        }

        public int getStackSize() {
            return this.entity instanceof EntityItem ? ((EntityItem)this.entity).getEntityItem().stackSize : -1;
        }

        public boolean isSleeping() {
            return (this.isPlayer && ((EntityPlayer)this.entity).isPlayerSleeping() ? 1 : 0) != 0;
        }

        public String getDisplayName() {
            if (this.entity instanceof EntityItem) {
                return ((EntityItem)this.entity).getEntityItem().getDisplayName();
            }
            return this.entity.getDisplayName().getUnformattedText();
        }

        public double getSpeed() {
            return Math.sqrt(this.entity.motionX * this.entity.motionX + this.entity.motionZ * this.entity.motionZ);
        }

        public float getPrevYaw() {
            return this.entity.prevRotationYaw;
        }

        public float getHealth() {
            return this.isLiving ? ((EntityLivingBase)this.entity).getHealth() : -1.0f;
        }

        public void setPrevYaw(float f) {
            this.entity.prevRotationYaw = f;
        }

        public int getMaxHurtTime() {
            return this.isLiving ? ((EntityLivingBase)this.entity).maxHurtTime : -1;
        }

        public boolean isCollided() {
            return this.entity.isCollided;
        }

        public String getUUID() {
            return this.entity.getUniqueID().toString();
        }

        public float getPrevPitch() {
            return this.entity.prevRotationPitch;
        }

        public boolean isInLava() {
            return this.entity.isInLava();
        }

        public List<Object[]> getPotionEffects() {
            if (this.isLiving) {
                ArrayList<Object[]> arrayList = new ArrayList<Object[]>();
                for (PotionEffect potionEffect : ((EntityLivingBase)this.entity).getActivePotionEffects()) {
                    arrayList.add(new Object[]{potionEffect.getPotionID(), potionEffect.getEffectName(), potionEffect.getAmplifier(), potionEffect.getDuration()});
                }
                return arrayList;
            }
            return null;
        }

        public NetworkPlayer getNetworkPlayer() {
            return NetworkPlayer.of(Defaults.mc.getNetHandler().getPlayerInfo(this.entity.getUniqueID()));
        }

        public boolean isCollidedHorizontally() {
            return this.entity.isCollidedHorizontally;
        }

        protected Entity(net.minecraft.entity.Entity entity) {
            this.entity = entity;
            this.type = entity.getClass().getSimpleName();
            this.entityId = entity.getEntityId();
            this.isLiving = entity instanceof EntityLivingBase;
            if (this.isLiving && (this.isPlayer = entity instanceof EntityPlayer)) {
                this.isUser = entity.getUniqueID().equals(Defaults.mc.thePlayer.getUniqueID());
            }
        }

        public float getMoveAttribute() {
            return this.isPlayer ? ((EntityPlayer)this.entity).getAIMoveSpeed() : -1.0f;
        }

        public Vec3 getPosition() {
            return new Vec3(this.entity.posX, this.entity.posY, this.entity.posZ);
        }

        public float getPitch() {
            return this.entity.rotationPitch;
        }

        public int getTicksExisted() {
            return this.entity.ticksExisted;
        }

        public boolean isInLiquid() {
            return (this.isInWater() || this.isInLava() ? 1 : 0) != 0;
        }

        public boolean isSprinting() {
            return this.entity.isSprinting();
        }

        public boolean isInvisible() {
            return this.entity.isInvisible();
        }

        public void setPrevPitch(float f) {
            this.entity.prevRotationPitch = f;
        }

        public double getBPS() {
            if (!this.isLiving) {
                return 0.0;
            }
            double d = this.entity.posX - this.entity.prevPosX;
            double d2 = this.entity.posZ - this.entity.prevPosZ;
            return Math.sqrt(d * d + d2 * d2) * 20.0;
        }

        public float getMaxHealth() {
            return this.isLiving ? ((EntityLivingBase)this.entity).getMaxHealth() : -1.0f;
        }

        public boolean isDead() {
            return (this.entity.isDead || this.isLiving && ((EntityLivingBase)this.entity).deathTime > 0 ? 1 : 0) != 0;
        }

        public boolean isConsuming() {
            return (this.isPlayer && ScriptBridge.isConsuming((EntityPlayer)this.entity) ? 1 : 0) != 0;
        }

        public Vec3 getBlockPosition() {
            Vec3 vec3 = this.getPosition();
            return new Vec3(MathHelper.floor_double((double)vec3.x), MathHelper.floor_double((double)(vec3.y + 0.5)), MathHelper.floor_double((double)vec3.z));
        }

        public String toString() {
            return "Entity(" + this.entityId + "," + this.type + "," + this.getName() + ")";
        }

        public boolean isOnLadder() {
            return (this.isLiving && ((EntityLivingBase)this.entity).isOnLadder() ? 1 : 0) != 0;
        }

        public String getName() {
            if (this.entity instanceof EntityItem) {
                return ScriptUtil.stripNamespace(((EntityItem)this.entity).getEntityItem().getItem().getRegistryName());
            }
            return this.entity.getName();
        }

        static {
            f116751 = 245266983 >>> 41 | 245266983 << ~41 + 1;
            f116752 = Integer.reverse(1022550472);
            f116753 = Integer.reverse(596293183);
            f116754 = Integer.reverse(596293183);
            f116755 = (1291047410 >>> 139 | 1291047410 << -139) & 0xFFFFFFFF;
            f116756 = (1838702960 >>> 230 | 1838702960 << -230) & 0xFFFFFFFF;
            f116757 = Integer.reverse(-647061326);
            f116758 = Integer.reverse(1500422322);
            f116759 = Integer.reverse(-1357295303);
            f116760 = Integer.reverse(-1357295303);
            f116761 = Float.intBitsToFloat((200802304 >>> 124 | 200802304 << -124) & 0xFFFFFFFF);
            f116762 = Integer.reverse(-141802682);
            f116763 = Integer.reverse(141802681);
            f116764 = Integer.reverse(-1948914726);
            f116765 = Integer.reverse(-1412043814);
            f116766 = 140961904 >>> 77 | 140961904 << ~77 + 1;
            f116767 = Integer.reverse(-322829886);
            f116768 = Integer.reverse(-131265164);
            f116769 = (-2053049995 >>> 43 | -2053049995 << ~43 + 1) & 0xFFFFFFFF;
            f116770 = (1372792156 >>> 84 | 1372792156 << ~84 + 1) & 0xFFFFFFFF;
            f116771 = (1460960457 >>> 138 | 1460960457 << ~138 + 1) & 0xFFFFFFFF;
            f116772 = (1222959405 >>> 213 | 1222959405 << -213) & 0xFFFFFFFF;
            f116773 = -2065617592 >>> 253 | -2065617592 << -253;
            f116774 = Float.intBitsToFloat(-134217717 >>> 68 | -134217717 << -68);
            f116775 = Integer.reverse(-356670856);
            f116776 = Integer.reverse(1790812792);
            f116777 = Integer.reverse(-1383199367);
            f116778 = Integer.reverse(-1383199367);
            f116779 = Double.longBitsToDouble(Long.reverse(0xC00D9329D4ABA835L ^ 0xC00D9329D4AB8437L));
            f116780 = Float.intBitsToFloat(Integer.reverse(509));
            f116781 = (-647143647 >>> 16 | -647143647 << -16) & 0xFFFFFFFF;
            f116782 = Integer.reverse(916161786);
            f116783 = Integer.reverse(-530369531);
            f116784 = Integer.reverse(-530369531);
            f116785 = 1669335032 >>> 3 | 1669335032 << ~3 + 1;
            f116786 = Integer.reverse(2130710064);
            f116787 = 1888467389 >>> 228 | 1888467389 << ~228 + 1;
            f116788 = (1937432863 >>> 85 | 1937432863 << -85) & 0xFFFFFFFF;
            f116789 = Double.longBitsToDouble(Long.reverse(0x23947DD5C3D600EAL ^ 0x23947DD5C3D60716L));
            f116790 = Integer.reverse(-582329584);
            f116791 = Integer.reverse(1565154064);
            f116792 = Integer.reverse(-1046736239);
            f116793 = -2087176044 >>> 204 | -2087176044 << ~204 + 1;
            f116794 = (1261816452 >>> 167 | 1261816452 << -167) & 0xFFFFFFFF;
            f116795 = 576302672 >>> 194 | 576302672 << -194;
            f116796 = -1333401876 >>> 214 | -1333401876 << ~214 + 1;
            f116797 = (-1333401876 >>> 54 | -1333401876 << ~54 + 1) & 0xFFFFFFFF;
            f116798 = Integer.reverse(-1821707990);
            f116799 = -1252293303 >>> 123 | -1252293303 << -123;
            f116800 = Integer.reverse(43579825);
            f116801 = Integer.reverse(-2103903823);
            f116802 = 1676659598 >>> 72 | 1676659598 << -72;
            f116803 = Integer.reverse(-738736527);
            f116804 = Integer.reverse(-2078830920);
            f116805 = Integer.reverse(68652728);
            f116806 = -1558356793 >>> 253 | -1558356793 << -253;
            f116807 = (1970478732 >>> 39 | 1970478732 << ~39 + 1) & 0xFFFFFFFF;
            f116808 = (-390630192 >>> 93 | -390630192 << ~93 + 1) & 0xFFFFFFFF;
            f116809 = (420933274 >>> 186 | 420933274 << ~186 + 1) & 0xFFFFFFFF;
            f116810 = Integer.reverse(1641467083);
            f116811 = Integer.reverse(1641467083);
            f116812 = (729911935 >>> 149 | 729911935 << ~149 + 1) & 0xFFFFFFFF;
            f116813 = (-1066778826 >>> 76 | -1066778826 << -76) & 0xFFFFFFFF;
            f116814 = Float.intBitsToFloat(100401152 >>> 123 | 100401152 << ~123 + 1);
            f116815 = 361249687 >>> 87 | 361249687 << -87;
            f116816 = -2130218270 >>> 252 | -2130218270 << ~252 + 1;
            cacheMap = new HashMap();
        }

        public float getHeight() {
            return this.entity.height;
        }

        public boolean isOnEdge() {
            return ScriptBridge.isOnEdge(this.entity);
        }

        public float getEyeHeight() {
            return this.entity.getEyeHeight();
        }

        static net.minecraft.entity.Entity access$300(Entity entity) {
            return entity.entity;
        }

        static HashMap access$100() {
            return cacheMap;
        }

        public boolean isSwinging() {
            return (this.isLiving && ((EntityLivingBase)this.entity).isSwingInProgress ? 1 : 0) != 0;
        }

        public int getHurtTime() {
            return this.isLiving ? ((EntityLivingBase)this.entity).hurtTime : -1;
        }

        public Vec3 getLastPosition() {
            return new Vec3(this.entity.lastTickPosX, this.entity.lastTickPosY, this.entity.lastTickPosZ);
        }

        public boolean isHoldingBlock() {
            return (this.isLiving && ((EntityLivingBase)this.entity).getHeldItem() != null && ((EntityLivingBase)this.entity).getHeldItem().getItem() instanceof ItemBlock ? 1 : 0) != 0;
        }

        public boolean isHoldingSword() {
            return (this.isLiving && ((EntityLivingBase)this.entity).getHeldItem() != null && ((EntityLivingBase)this.entity).getHeldItem().getItem() instanceof ItemSword ? 1 : 0) != 0;
        }

        public boolean isUsingItem() {
            return (this.isPlayer && ((EntityPlayer)this.entity).isUsingItem() ? 1 : 0) != 0;
        }

        public void setPosition(Vec3 vec3) {
            this.setPosition(vec3.x, vec3.y, vec3.z);
        }

        public int getSwingProgress() {
            return this.isLiving ? ((EntityLivingBase)this.entity).swingProgressInt : -1;
        }

        public float getFallDistance() {
            return this.entity.fallDistance;
        }

        public float getAbsorption() {
            return this.isLiving ? ((EntityLivingBase)this.entity).getAbsorptionAmount() : -1.0f;
        }

        public boolean isCollidedVertically() {
            return this.entity.isCollidedVertically;
        }

        public float getYaw() {
            return this.entity.rotationYaw;
        }

        public boolean isSneaking() {
            return this.entity.isSneaking();
        }

        public boolean onGround() {
            return this.entity.onGround;
        }

        public double distanceToGround() {
            return ScriptBridge.distanceToGround(this.entity);
        }

        protected static Entity of(net.minecraft.entity.Entity entity) {
            if (entity == null) {
                return null;
            }
            int n = entity.getEntityId() + System.identityHashCode(entity);
            Entity entity2 = cacheMap.get(n);
            if (entity2 == null) {
                entity2 = new Entity(entity);
                cacheMap.put(n, entity2);
            }
            return entity2;
        }

        public ItemStack getArmorInSlot(int n) {
            return this.isPlayer && n >= 0 && n <= 3 ? ItemStack.of(((EntityPlayer)this.entity).inventory.armorInventory[n]) : null;
        }

        public static Entity protected_of(net.minecraft.entity.Entity entity) {
            return Entity.of(entity);
        }

        public boolean isInWater() {
            return this.entity.isInWater();
        }

        public float getWidth() {
            return this.entity.width;
        }

        public void setPosition(double d, double d2, double d3) {
            this.entity.setPosition(d, d2, d3);
        }

        public String getFacing() {
            return this.entity.getHorizontalFacing().name();
        }

        public boolean isRiding() {
            return this.entity.isRiding();
        }

        public boolean isBurning() {
            return this.entity.isBurning();
        }

        public ItemStack getHeldItem() {
            return this.isLiving ? ItemStack.of(((EntityLivingBase)this.entity).getHeldItem()) : null;
        }
    }

    public static class render {
        private static final FloatBuffer OBJECTCOORDS;
        private static final IntBuffer VIEWPORT;
        private static final FloatBuffer PROJECTION;
        private static final Frustum FRUSTUM;
        private static final FloatBuffer MODELVIEW;
        private static int f116965;
        private static int f116966;
        private static int f116967;
        private static int f116968;
        private static int f116969;
        private static int f116970;
        private static int f116971;
        private static int f116972;
        private static double f116973;
        private static double f116974;
        private static double f116975;
        private static int f116976;
        private static int f116977;
        private static int f116978;
        private static int f116979;
        private static int f116980;
        private static int f116981;
        private static int f116982;
        private static int f116983;
        private static int f116984;
        private static int f116985;
        private static int f116986;
        private static int f116987;
        private static int f116988;
        private static int f116989;
        private static int f116990;
        private static int f116991;
        private static float f116992;
        private static int f116993;
        private static int f116994;
        private static int f116995;
        private static int f116996;
        private static float f116997;
        private static int f116998;
        private static int f116999;
        private static int f117000;
        private static int f117001;
        private static float f117002;
        private static int f117003;
        private static int f117004;
        private static float f117005;
        private static int f117006;
        private static int f117007;
        private static int f117008;
        private static int f117009;
        private static float f117010;
        private static int f117011;
        private static int f117012;
        private static int f117013;
        private static int f117014;
        private static int f117015;
        private static int f117016;
        private static int f117017;
        private static int f117018;
        private static int f117019;
        private static int f117020;
        private static int f117021;
        private static int f117022;
        private static int f117023;
        private static int f117024;
        private static int f117025;
        private static int f117026;
        private static int f117027;
        private static int f117028;
        private static int f117029;
        private static int f117030;
        private static int f117031;
        private static int f117032;
        private static int f117033;
        private static int f117034;
        private static int f117035;
        private static int f117036;
        private static int f117037;
        private static int f117038;
        private static int f117039;
        private static int f117040;
        private static int f117041;
        private static int f117042;
        private static int f117043;
        private static int f117044;
        private static float f117045;
        private static int f117046;
        private static int f117047;
        private static int f117048;
        private static int f117049;
        private static float f117050;
        private static int f117051;
        private static int f117052;
        private static float f117053;
        private static double f117054;
        private static double f117055;
        private static double f117056;
        private static double f117057;
        private static int f117058;
        private static int f117059;
        private static int f117060;
        private static int f117061;
        private static int f117062;
        private static int f117063;
        private static int f117064;
        private static int f117065;
        private static int f117066;
        private static int f117067;
        private static float f117068;
        private static int f117069;
        private static int f117070;
        private static int f117071;
        private static int f117072;
        private static float f117073;
        private static int f117074;
        private static int f117075;
        private static int f117076;
        private static int f117077;
        private static float f117078;
        private static int f117079;
        private static int f117080;
        private static float f117081;
        private static int f117082;
        private static int f117083;
        private static int f117084;
        private static int f117085;
        private static int f117086;
        private static int f117087;
        private static int f117088;
        private static int f117089;
        private static int f117090;
        private static int f117091;
        private static int f117092;
        private static int f117093;
        private static int f117094;
        private static int f117095;
        private static int f117096;
        private static int f117097;
        private static int f117098;
        private static int f117099;
        private static int f117100;
        private static int f117101;
        private static double f117102;
        private static double f117103;
        private static int f117104;
        private static int f117105;
        private static int f117106;
        private static int f117107;
        private static int f117108;
        private static int f117109;
        private static int f117110;
        private static int f117111;
        private static int f117112;
        private static int f117113;
        private static int f117114;
        private static int f117115;
        private static int f117116;
        private static int f117117;
        private static int f117118;
        private static int f117119;
        private static int f117120;
        private static int f117121;
        private static int f117122;
        private static int f117123;
        private static int f117124;
        private static int f117125;
        private static int f117126;
        private static int f117127;
        private static int f117128;
        private static int f117129;
        private static int f117130;
        private static int f117131;
        private static int f117132;
        private static int f117133;
        private static int f117134;
        private static int f117135;
        private static int f117136;
        private static int f117137;
        private static float f117138;
        private static int f117139;
        private static int f117140;
        private static int f117141;
        private static int f117142;
        private static float f117143;
        private static int f117144;
        private static int f117145;
        private static int f117146;
        private static int f117147;
        private static float f117148;
        private static int f117149;
        private static int f117150;
        private static float f117151;
        private static int f117152;
        private static int f117153;
        private static int f117154;
        private static int f117155;
        private static int f117156;
        private static int f117157;
        private static int f117158;
        private static int f117159;
        private static int f117160;
        private static int f117161;
        private static int f117162;
        private static int f117163;
        private static int f117164;
        private static int f117165;
        private static float f117166;
        private static int f117167;
        private static int f117168;
        private static int f117169;
        private static int f117170;
        private static float f117171;
        private static int f117172;
        private static int f117173;
        private static int f117174;
        private static int f117175;
        private static float f117176;
        private static int f117177;
        private static int f117178;
        private static float f117179;
        private static int f117180;
        private static int f117181;
        private static int f117182;
        private static int f117183;
        private static int f117184;
        private static int f117185;
        private static int f117186;
        private static int f117187;
        private static int f117188;
        private static int f117189;
        private static int f117190;
        private static int f117191;
        private static int f117192;
        private static int f117193;
        private static int f117194;
        private static int f117195;
        private static int f117196;
        private static int f117197;
        private static int f117198;
        private static int f117199;
        private static int f117200;
        private static int f117201;
        private static int f117202;
        private static int f117203;
        private static int f117204;
        private static int f117205;
        private static int f117206;
        private static int f117207;
        private static int f117208;
        private static int f117209;
        private static int f117210;
        private static int f117211;
        private static int f117212;
        private static int f117213;
        private static int f117214;
        private static int f117215;
        private static int f117216;
        private static int f117217;
        private static int f117218;
        private static int f117219;
        private static int f117220;
        private static int f117221;
        private static int f117222;
        private static int f117223;
        private static int f117224;
        private static int f117225;
        private static int f117226;
        private static int f117227;
        private static int f117228;
        private static int f117229;
        private static int f117230;
        private static int f117231;
        private static int f117232;
        private static int f117233;
        private static int f117234;
        private static int f117235;
        private static int f117236;
        private static int f117237;
        private static int f117238;
        private static int f117239;
        private static int f117240;
        private static int f117241;
        private static int f117242;
        private static int f117243;
        private static int f117244;
        private static int f117245;
        private static int f117246;
        private static int f117247;
        private static int f117248;
        private static int f117249;
        private static double f117250;
        private static int f117251;
        private static int f117252;
        private static int f117253;
        private static int f117254;
        private static int f117255;
        private static int f117256;
        private static int f117257;
        private static int f117258;
        private static int f117259;
        private static int f117260;
        private static int f117261;
        private static int f117262;
        private static int f117263;
        private static int f117264;
        private static int f117265;
        private static int f117266;
        private static int f117267;
        private static int f117268;
        private static float f117269;
        private static int f117270;
        private static int f117271;
        private static int f117272;
        private static int f117273;
        private static float f117274;
        private static int f117275;
        private static int f117276;
        private static int f117277;
        private static int f117278;
        private static float f117279;
        private static int f117280;
        private static int f117281;
        private static float f117282;
        private static int f117283;
        private static int f117284;
        private static int f117285;
        private static int f117286;
        private static int f117287;
        private static int f117288;
        private static int f117289;
        private static int f117290;
        private static int f117291;
        private static int f117292;
        private static int f117293;
        private static int f117294;
        private static int f117295;
        private static int f117296;
        private static int f117297;
        private static int f117298;
        private static int f117299;
        private static int f117300;
        private static int f117301;
        private static int f117302;
        private static int f117303;
        private static int f117304;

        public static int getFontWidth(String string) {
            return Defaults.mc.fontRendererObj.getStringWidth(string);
        }

        public static void block(int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
            ScriptBridge.renderBlock(n, n2, n3, n4, bl, bl2);
        }

        public static void line3D(Vec3 vec3, Vec3 vec32, float f, int n) {
            render.line3D(vec3.x, vec3.y, vec3.z, vec32.x, vec32.y, vec32.z, f, n);
        }

        static {
            f116965 = (909174022 >>> 11 | 909174022 << -11) & 0xFFFFFFFF;
            f116966 = Integer.reverse(811819781);
            f116967 = Integer.reverse(-179050314);
            f116968 = -223356091 >>> 78 | -223356091 << ~78 + 1;
            f116969 = Integer.reverse(-13486362);
            f116970 = -1713451304 >>> 49 | -1713451304 << ~49 + 1;
            f116971 = Integer.reverse(1993184291);
            f116972 = Integer.reverse(-1228041181);
            f116973 = Double.longBitsToDouble(Long.reverse(0x47EC71EF0D90273L ^ 0x5DE75E8769409F8FL));
            f116974 = Double.longBitsToDouble(Long.reverse(0x1DEADFC7EDF55097L ^ 0x4473465E746CCD6BL));
            f116975 = Double.longBitsToDouble(Long.reverse(0x2DF3FAEEB8F66994L ^ 0x746A6377216FF468L));
            f116976 = (1657872085 >>> 240 | 1657872085 << ~240 + 1) & 0xFFFFFFFF;
            f116977 = 1444753901 >>> 44 | 1444753901 << ~44 + 1;
            f116978 = Integer.reverse(-1517512326);
            f116979 = Integer.reverse(1699518842);
            f116980 = (-455102805 >>> 127 | -455102805 << -127) & 0xFFFFFFFF;
            f116981 = -710053265 >>> 142 | -710053265 << -142;
            f116982 = 1266222742 >>> 138 | 1266222742 << ~138 + 1;
            f116983 = Integer.reverse(97208741);
            f116984 = Integer.reverse(-423796220);
            f116985 = 742428012 >>> 217 | 742428012 << ~217 + 1;
            f116986 = Integer.reverse(645379025);
            f116987 = -420007355 >>> 7 | -420007355 << -7;
            f116988 = (229220391 >>> 138 | 229220391 << ~138 + 1) & 0xFFFFFFFF;
            f116989 = 327603424 >>> 33 | 327603424 << ~33 + 1;
            f116990 = -1575742569 >>> 225 | -1575742569 << ~225 + 1;
            f116991 = Integer.reverse(747655307);
            f116992 = Float.intBitsToFloat(Integer.reverse(65218));
            f116993 = Integer.reverse(-790704807);
            f116994 = Integer.reverse(-656487079);
            f116995 = Integer.reverse(-1136837020);
            f116996 = (1741409314 >>> 68 | 1741409314 << -68) & 0xFFFFFFFF;
            f116997 = Float.intBitsToFloat(-268434377 >>> 236 | -268434377 << ~236 + 1);
            f116998 = Integer.reverse(191731389);
            f116999 = -694456949 >>> 164 | -694456949 << ~164 + 1;
            f117000 = (1690542974 >>> 78 | 1690542974 << -78) & 0xFFFFFFFF;
            f117001 = Integer.reverse(-1882611791);
            f117002 = Float.intBitsToFloat(Integer.reverse(65218));
            f117003 = 936872901 >>> 111 | 936872901 << ~111 + 1;
            f117004 = -1493892744 >>> 204 | -1493892744 << ~204 + 1;
            f117005 = Float.intBitsToFloat(Integer.reverse(65218));
            f117006 = -584323360 >>> 35 | -584323360 << ~35 + 1;
            f117007 = -545033042 >>> 149 | -545033042 << ~149 + 1;
            f117008 = Integer.reverse(-2108636312);
            f117009 = (-1329571771 >>> 198 | -1329571771 << -198) & 0xFFFFFFFF;
            f117010 = Float.intBitsToFloat(Integer.reverse(1977779836));
            f117011 = Integer.reverse(-730114470);
            f117012 = (-1800088153 >>> 217 | -1800088153 << -217) & 0xFFFFFFFF;
            f117013 = (-57434729 >>> 184 | -57434729 << ~184 + 1) & 0xFFFFFFFF;
            f117014 = Integer.reverse(-1321616951);
            f117015 = 1255314513 >>> 149 | 1255314513 << -149;
            f117016 = Integer.reverse(-363772631);
            f117017 = (-999013075 >>> 110 | -999013075 << -110) & 0xFFFFFFFF;
            f117018 = Integer.reverse(-866587352);
            f117019 = -1164112077 >>> 52 | -1164112077 << -52;
            f117020 = 1137503931 >>> 98 | 1137503931 << ~98 + 1;
            f117021 = (948365765 >>> 104 | 948365765 << ~104 + 1) & 0xFFFFFFFF;
            f117022 = Integer.reverse(2002853027);
            f117023 = (2013385471 >>> 190 | 2013385471 << -190) & 0xFFFFFFFF;
            f117024 = Integer.reverse(1607655431);
            f117025 = Integer.reverse(-378509497);
            f117026 = Integer.reverse(158361415);
            f117027 = Integer.reverse(610230791);
            f117028 = Integer.reverse(-1000381945);
            f117029 = 169088676 >>> 72 | 169088676 << -72;
            f117030 = Integer.reverse(-2010623963);
            f117031 = Integer.reverse(-1678071421);
            f117032 = -641624737 >>> 24 | -641624737 << -24;
            f117033 = -301547931 >>> 52 | -301547931 << -52;
            f117034 = Integer.reverse(1203398614);
            f117035 = Integer.reverse(-338891930);
            f117036 = (-1556694650 >>> 219 | -1556694650 << -219) & 0xFFFFFFFF;
            f117037 = Integer.reverse(-379661746);
            f117038 = -726429093 >>> 179 | -726429093 << -179;
            f117039 = Integer.reverse(-1421092895);
            f117040 = 1263935407 >>> 146 | 1263935407 << ~146 + 1;
            f117041 = 133446475 >>> 3 | 133446475 << ~3 + 1;
            f117042 = 1627293561 >>> 160 | 1627293561 << ~160 + 1;
            f117043 = 1315714096 >>> 131 | 1315714096 << -131;
            f117044 = Integer.reverse(-1637764208);
            f117045 = Float.intBitsToFloat(283099136 >>> 222 | 283099136 << -222);
            f117046 = (-1844782620 >>> 209 | -1844782620 << ~209 + 1) & 0xFFFFFFFF;
            f117047 = (-639331814 >>> 225 | -639331814 << -225) & 0xFFFFFFFF;
            f117048 = 907959715 >>> 49 | 907959715 << -49;
            f117049 = Integer.reverse(265915252);
            f117050 = Float.intBitsToFloat((-2030174208 >>> 129 | -2030174208 << -129) & 0xFFFFFFFF);
            f117051 = (1416495812 >>> 131 | 1416495812 << ~131 + 1) & 0xFFFFFFFF;
            f117052 = Integer.reverse(-452742831);
            f117053 = Float.intBitsToFloat(Integer.reverse(65218));
            f117054 = Double.longBitsToDouble(Long.reverse(0xC501DB7D352D1B99L ^ 0xC501DB7D352D1C65L));
            f117055 = Double.longBitsToDouble(Long.reverse(0xAD36ED99BDC21EB3L ^ 0xAD36ED99BDC2194FL));
            f117056 = Double.longBitsToDouble(Long.reverse(0x4CBC7786C4D5294AL ^ 0x4CBC7786C4D52EB6L));
            f117057 = Double.longBitsToDouble(Long.reverse(0x7FE3D0D081D9C487L ^ 0x7FE3D0D081D9C37BL));
            f117058 = 333985795 >>> 122 | 333985795 << -122;
            f117059 = (-49872799 >>> 127 | -49872799 << -127) & 0xFFFFFFFF;
            f117060 = (-2095909699 >>> 29 | -2095909699 << -29) & 0xFFFFFFFF;
            f117061 = Integer.reverse(-1213863656);
            f117062 = Integer.reverse(1921416620);
            f117063 = 1872253773 >>> 6 | 1872253773 << ~6 + 1;
            f117064 = (-1089869842 >>> 45 | -1089869842 << -45) & 0xFFFFFFFF;
            f117065 = Integer.reverse(-367022340);
            f117066 = (-1333889186 >>> 132 | -1333889186 << ~132 + 1) & 0xFFFFFFFF;
            f117067 = 1480539223 >>> 3 | 1480539223 << -3;
            f117068 = Float.intBitsToFloat(Integer.reverse(65218));
            f117069 = Integer.reverse(401786061);
            f117070 = Integer.reverse(536003789);
            f117071 = Integer.reverse(-150136451);
            f117072 = 561852768 >>> 153 | 561852768 << -153;
            f117073 = Float.intBitsToFloat(Integer.reverse(65218));
            f117074 = Integer.reverse(-571714969);
            f117075 = (1046782843 >>> 252 | 1046782843 << ~252 + 1) & 0xFFFFFFFF;
            f117076 = Integer.reverse(-2022345157);
            f117077 = Integer.reverse(2020963899);
            f117078 = Float.intBitsToFloat((34558 >>> 17 | 34558 << ~17 + 1) & 0xFFFFFFFF);
            f117079 = -90136746 >>> 84 | -90136746 << -84;
            f117080 = Integer.reverse(-1426673776);
            f117081 = Float.intBitsToFloat(Integer.reverse(65218));
            f117082 = Integer.reverse(-563479400);
            f117083 = Integer.reverse(1507458200);
            f117084 = Integer.reverse(-381093480);
            f117085 = Integer.reverse(-1365706344);
            f117086 = Integer.reverse(-1351599637);
            f117087 = Integer.reverse(-273663509);
            f117088 = (-1953029482 >>> 146 | -1953029482 << ~146 + 1) & 0xFFFFFFFF;
            f117089 = Integer.reverse(1736811795);
            f117090 = (1820716176 >>> 0 | 1820716176 << ~0 + 1) & 0xFFFFFFFF;
            f117091 = (-2048401044 >>> 168 | -2048401044 << ~168 + 1) & 0xFFFFFFFF;
            f117092 = Integer.reverse(2078106177);
            f117093 = -2014047691 >>> 19 | -2014047691 << -19;
            f117094 = 32405586 >>> 227 | 32405586 << ~227 + 1;
            f117095 = Integer.reverse(1358150658);
            f117096 = Integer.reverse(422837363);
            f117097 = Integer.reverse(-1724646285);
            f117098 = (593889500 >>> 178 | 593889500 << -178) & 0xFFFFFFFF;
            f117099 = (923326850 >>> 8 | 923326850 << -8) & 0xFFFFFFFF;
            f117100 = (1085215880 >>> 210 | 1085215880 << -210) & 0xFFFFFFFF;
            f117101 = Integer.reverse(-1811397469);
            f117102 = Double.longBitsToDouble(Long.reverse(0x4DCBF052E0D9469CL ^ 0x557FD2783F5DDE9EL));
            f117103 = Double.longBitsToDouble(Long.reverse(0x6C259CF6E4829FB1L ^ 0x6C259CF6E483F9B3L));
            f117104 = Integer.reverse(1030270443);
            f117105 = -144780198 >>> 26 | -144780198 << -26;
            f117106 = Integer.reverse(1954007738);
            f117107 = (-878019423 >>> 125 | -878019423 << -125) & 0xFFFFFFFF;
            f117108 = Integer.reverse(-1978590547);
            f117109 = 1521238416 >>> 223 | 1521238416 << ~223 + 1;
            f117110 = (-220770561 >>> 45 | -220770561 << -45) & 0xFFFFFFFF;
            f117111 = -1326448838 >>> 121 | -1326448838 << -121;
            f117112 = Integer.reverse(-898497478);
            f117113 = -1203730588 >>> 1 | -1203730588 << -1;
            f117114 = -1348562299 >>> 212 | -1348562299 << ~212 + 1;
            f117115 = Integer.reverse(-547744617);
            f117116 = Integer.reverse(-1042378906);
            f117117 = 931912731 >>> 131 | 931912731 << ~131 + 1;
            f117118 = Integer.reverse(-2109484696);
            f117119 = Integer.reverse(37998952);
            f117120 = Integer.reverse(509916652);
            f117121 = Integer.reverse(509916652);
            f117122 = Integer.reverse(335104777);
            f117123 = Integer.reverse(1982417673);
            f117124 = Integer.reverse(1257121112);
            f117125 = (2056081982 >>> 183 | 2056081982 << ~183 + 1) & 0xFFFFFFFF;
            f117126 = (-1918264087 >>> 251 | -1918264087 << -251) & 0xFFFFFFFF;
            f117127 = (963197779 >>> 172 | 963197779 << ~172 + 1) & 0xFFFFFFFF;
            f117128 = 1559953403 >>> 207 | 1559953403 << ~207 + 1;
            f117129 = -19448129 >>> 197 | -19448129 << ~197 + 1;
            f117130 = Integer.reverse(-535534436);
            f117131 = 1233781134 >>> 38 | 1233781134 << -38;
            f117132 = Integer.reverse(297831933);
            f117133 = Integer.reverse(1371573757);
            f117134 = Integer.reverse(-1634871591);
            f117135 = Integer.reverse(-2037524775);
            f117136 = Integer.reverse(-1157938822);
            f117137 = (-1570864417 >>> 216 | -1570864417 << ~216 + 1) & 0xFFFFFFFF;
            f117138 = Float.intBitsToFloat((283099136 >>> 30 | 283099136 << ~30 + 1) & 0xFFFFFFFF);
            f117139 = Integer.reverse(538818469);
            f117140 = Integer.reverse(673036197);
            f117141 = Integer.reverse(1046498828);
            f117142 = -1043850740 >>> 66 | -1043850740 << -66;
            f117143 = Float.intBitsToFloat((1132396544 >>> 128 | 1132396544 << ~128 + 1) & 0xFFFFFFFF);
            f117144 = Integer.reverse(-894197475);
            f117145 = -574345622 >>> 187 | -574345622 << ~187 + 1;
            f117146 = Integer.reverse(1823611472);
            f117147 = Integer.reverse(-1817044400);
            f117148 = Float.intBitsToFloat(1132396544 >>> 64 | 1132396544 << -64);
            f117149 = (1596811227 >>> 146 | 1596811227 << -146) & 0xFFFFFFFF;
            f117150 = Integer.reverse(753627034);
            f117151 = Float.intBitsToFloat(Integer.reverse(65218));
            f117152 = (501877491 >>> 39 | 501877491 << -39) & 0xFFFFFFFF;
            f117153 = 801929134 >>> 57 | 801929134 << -57;
            f117154 = (-921533073 >>> 74 | -921533073 << -74) & 0xFFFFFFFF;
            f117155 = Integer.reverse(1172459482);
            f117156 = (68030647 >>> 101 | 68030647 << -101) & 0xFFFFFFFF;
            f117157 = 158351585 >>> 185 | 158351585 << ~185 + 1;
            f117158 = -343878819 >>> 112 | -343878819 << -112;
            f117159 = Integer.reverse(30915275);
            f117160 = 1681383451 >>> 145 | 1681383451 << ~145 + 1;
            f117161 = (-653363194 >>> 143 | -653363194 << ~143 + 1) & 0xFFFFFFFF;
            f117162 = Integer.reverse(1595107304);
            f117163 = 1604002696 >>> 66 | 1604002696 << -66;
            f117164 = Integer.reverse(134753727);
            f117165 = (251648833 >>> 84 | 251648833 << -84) & 0xFFFFFFFF;
            f117166 = Float.intBitsToFloat(-67108595 >>> 106 | -67108595 << ~106 + 1);
            f117167 = (-462460245 >>> 41 | -462460245 << -41) & 0xFFFFFFFF;
            f117168 = Integer.reverse(-1511239766);
            f117169 = (773938564 >>> 83 | 773938564 << ~83 + 1) & 0xFFFFFFFF;
            f117170 = (-666722923 >>> 135 | -666722923 << ~135 + 1) & 0xFFFFFFFF;
            f117171 = Float.intBitsToFloat(Integer.reverse(65218));
            f117172 = (2006615144 >>> 201 | 2006615144 << -201) & 0xFFFFFFFF;
            f117173 = (1244937165 >>> 120 | 1244937165 << -120) & 0xFFFFFFFF;
            f117174 = -1592397475 >>> 127 | -1592397475 << -127;
            f117175 = Integer.reverse(575132738);
            f117176 = Float.intBitsToFloat(Integer.reverse(65218));
            f117177 = (914132206 >>> 98 | 914132206 << ~98 + 1) & 0xFFFFFFFF;
            f117178 = -230930215 >>> 76 | -230930215 << ~76 + 1;
            f117179 = Float.intBitsToFloat(Integer.reverse(65218));
            f117180 = Integer.reverse(1856357561);
            f117181 = Integer.reverse(695583929);
            f117182 = Integer.reverse(155659489);
            f117183 = Integer.reverse(228011233);
            f117184 = Integer.reverse(-1675895312);
            f117185 = (-1876985946 >>> 25 | -1876985946 << -25) & 0xFFFFFFFF;
            f117186 = -2144721356 >>> 17 | -2144721356 << ~17 + 1;
            f117187 = (-1926825463 >>> 39 | -1926825463 << ~39 + 1) & 0xFFFFFFFF;
            f117188 = 1189558267 >>> 31 | 1189558267 << ~31 + 1;
            f117189 = (1738829427 >>> 83 | 1738829427 << -83) & 0xFFFFFFFF;
            f117190 = Integer.reverse(-499127456);
            f117191 = 1811915267 >>> 135 | 1811915267 << -135;
            f117192 = -546856515 >>> 209 | -546856515 << -209;
            f117193 = Integer.reverse(-1913160949);
            f117194 = 950526183 >>> 181 | 950526183 << ~181 + 1;
            f117195 = 152027597 >>> 150 | 152027597 << ~150 + 1;
            f117196 = (-1243737223 >>> 209 | -1243737223 << -209) & 0xFFFFFFFF;
            f117197 = Integer.reverse(2039168400);
            f117198 = Integer.reverse(-957110823);
            f117199 = Integer.reverse(-1037851175);
            f117200 = Integer.reverse(-1678754626);
            f117201 = Integer.reverse(-601867074);
            f117202 = 405526929 >>> 150 | 405526929 << ~150 + 1;
            f117203 = Integer.reverse(-102917878);
            f117204 = Integer.reverse(-1429611128);
            f117205 = (-1601315595 >>> 187 | -1601315595 << -187) & 0xFFFFFFFF;
            f117206 = (1320509281 >>> 250 | 1320509281 << ~250 + 1) & 0xFFFFFFFF;
            f117207 = Integer.reverse(-904926539);
            f117208 = Integer.reverse(-137458018);
            f117209 = Integer.reverse(-139817314);
            f117210 = (1469968395 >>> 191 | 1469968395 << -191) & 0xFFFFFFFF;
            f117211 = Integer.reverse(-273163019);
            f117212 = (1621871073 >>> 126 | 1621871073 << ~126 + 1) & 0xFFFFFFFF;
            f117213 = (260407983 >>> 48 | 260407983 << -48) & 0xFFFFFFFF;
            f117214 = (-1876556248 >>> 108 | -1876556248 << ~108 + 1) & 0xFFFFFFFF;
            f117215 = (340337411 >>> 131 | 340337411 << -131) & 0xFFFFFFFF;
            f117216 = 1007468264 >>> 92 | 1007468264 << ~92 + 1;
            f117217 = -2120753911 >>> 33 | -2120753911 << -33;
            f117218 = 1658888309 >>> 122 | 1658888309 << -122;
            f117219 = Integer.reverse(-440921059);
            f117220 = Integer.reverse(-1589279613);
            f117221 = -1408099801 >>> 124 | -1408099801 << -124;
            f117222 = 323870899 >>> 97 | 323870899 << -97;
            f117223 = (1295484212 >>> 99 | 1295484212 << ~99 + 1) & 0xFFFFFFFF;
            f117224 = -243094792 >>> 67 | -243094792 << ~67 + 1;
            f117225 = Integer.reverse(95030392);
            f117226 = Integer.reverse(-1223625864);
            f117227 = Integer.reverse(1209070456);
            f117228 = 1998946133 >>> 213 | 1998946133 << ~213 + 1;
            f117229 = Integer.reverse(1658150868);
            f117230 = Integer.reverse(1816995023);
            f117231 = (749894857 >>> 78 | 749894857 << ~78 + 1) & 0xFFFFFFFF;
            f117232 = Integer.reverse(-865714844);
            f117233 = (1630921526 >>> 155 | 1630921526 << -155) & 0xFFFFFFFF;
            f117234 = Integer.reverse(958292930);
            f117235 = (-241297200 >>> 6 | -241297200 << -6) & 0xFFFFFFFF;
            f117236 = 948249200 >>> 240 | 948249200 << ~240 + 1;
            f117237 = 118441038 >>> 141 | 118441038 << ~141 + 1;
            f117238 = Integer.reverse(780228939);
            f117239 = Integer.reverse(-780052149);
            f117240 = Integer.reverse(2051700043);
            f117241 = 489350442 >>> 60 | 489350442 << -60;
            f117242 = (-239195574 >>> 64 | -239195574 << -64) & 0xFFFFFFFF;
            f117243 = Integer.reverse(-1386971761);
            f117244 = Integer.reverse(-871762696);
            f117245 = -1865670865 >>> 208 | -1865670865 << ~208 + 1;
            f117246 = Integer.reverse(-201588529);
            f117247 = Integer.reverse(0xCFC00CF);
            f117248 = (-298934157 >>> 21 | -298934157 << ~21 + 1) & 0xFFFFFFFF;
            f117249 = Integer.reverse(1912193198);
            f117250 = Double.longBitsToDouble(Long.reverse(0x19BE0FFCC12F8609L ^ 0x4027966558B61DF5L));
            f117251 = Integer.reverse(-1810940817);
            f117252 = Integer.reverse(-737198993);
            f117253 = 977488721 >>> 151 | 977488721 << -151;
            f117254 = Integer.reverse(776300897);
            f117255 = Integer.reverse(-2033524311);
            f117256 = Integer.reverse(113959337);
            f117257 = Integer.reverse(426993244);
            f117258 = Integer.reverse(1504929372);
            f117259 = -28244463 >>> 239 | -28244463 << -239;
            f117260 = (1124663272 >>> 126 | 1124663272 << ~126 + 1) & 0xFFFFFFFF;
            f117261 = (-974739553 >>> 170 | -974739553 << ~170 + 1) & 0xFFFFFFFF;
            f117262 = (2132253326 >>> 164 | 2132253326 << ~164 + 1) & 0xFFFFFFFF;
            f117263 = (1610266924 >>> 76 | 1610266924 << ~76 + 1) & 0xFFFFFFFF;
            f117264 = Integer.reverse(-704666807);
            f117265 = Integer.reverse(2045947941);
            f117266 = Integer.reverse(1643294757);
            f117267 = (-936256525 >>> 164 | -936256525 << -164) & 0xFFFFFFFF;
            f117268 = Integer.reverse(45662524);
            f117269 = Float.intBitsToFloat(Integer.reverse(65218));
            f117270 = (725284874 >>> 105 | 725284874 << ~105 + 1) & 0xFFFFFFFF;
            f117271 = (1526809959 >>> 54 | 1526809959 << -54) & 0xFFFFFFFF;
            f117272 = Integer.reverse(512899884);
            f117273 = Integer.reverse(-510510292);
            f117274 = Float.intBitsToFloat(Integer.reverse(65218));
            f117275 = -393014400 >>> 151 | -393014400 << -151;
            f117276 = 496132720 >>> 148 | 496132720 << -148;
            f117277 = Integer.reverse(-814515124);
            f117278 = Integer.reverse(812874828);
            f117279 = Float.intBitsToFloat(Integer.reverse(65218));
            f117280 = (-1532727755 >>> 92 | -1532727755 << -92) & 0xFFFFFFFF;
            f117281 = Integer.reverse(-1513659822);
            f117282 = Float.intBitsToFloat((276464 >>> 20 | 276464 << -20) & 0xFFFFFFFF);
            f117283 = Integer.reverse(-1364368492);
            f117284 = Integer.reverse(-377658476);
            f117285 = (1984495299 >>> 154 | 1984495299 << ~154 + 1) & 0xFFFFFFFF;
            f117286 = Integer.reverse(-1075970999);
            f117287 = Integer.reverse(-683778915);
            f117288 = -454435222 >>> 226 | -454435222 << -226;
            f117289 = Integer.reverse(-1798708683);
            f117290 = -1631015865 >>> 48 | -1631015865 << -48;
            f117291 = Integer.reverse(-1032413961);
            f117292 = Integer.reverse(-2101961481);
            f117293 = Integer.reverse(-1829753520);
            f117294 = -1810536424 >>> 249 | -1810536424 << -249;
            f117295 = (1493458263 >>> 117 | 1493458263 << ~117 + 1) & 0xFFFFFFFF;
            f117296 = Integer.reverse(1398626116);
            f117297 = Integer.reverse(-465492381);
            f117298 = Integer.reverse(1676748387);
            f117299 = Integer.reverse(-556396175);
            f117300 = Integer.reverse(1342574961);
            f117301 = 2013679281 >>> 179 | 2013679281 << ~179 + 1;
            f117302 = (1648495773 >>> 140 | 1648495773 << -140) & 0xFFFFFFFF;
            f117303 = Integer.reverse(-1064642718);
            f117304 = 1189829345 >>> 192 | 1189829345 << -192;
            FRUSTUM = new Frustum();
            VIEWPORT = GLAllocation.createDirectIntBuffer((int)((int)((long)f116965 ^ (long)f116966)));
            MODELVIEW = GLAllocation.createDirectFloatBuffer((int)((int)((long)f116967 ^ (long)f116968)));
            PROJECTION = GLAllocation.createDirectFloatBuffer((int)((int)((long)f116969 ^ (long)f116970)));
            OBJECTCOORDS = GLAllocation.createDirectFloatBuffer((int)((int)((long)f116971 ^ (long)f116972)));
        }

        public static void entity(Entity entity, int n, float f, boolean bl, boolean bl2) {
            net.minecraft.entity.Entity entity2 = Entity.access$300(entity);
            RenderManager renderManager = Defaults.mc.getRenderManager();
            double d = entity2.lastTickPosX;
            double d2 = entity2.lastTickPosY;
            double d3 = entity2.lastTickPosZ;
            double d4 = d + (entity2.posX - d) * (double)f - renderManager.viewerPosX;
            double d5 = d2 + (entity2.posY - d2) * (double)f - renderManager.viewerPosY;
            double d6 = d3 + (entity2.posZ - d3) * (double)f - renderManager.viewerPosZ;
            AxisAlignedBB axisAlignedBB = entity2.getEntityBoundingBox().expand(f116973, f116974, f116975);
            AxisAlignedBB axisAlignedBB2 = new AxisAlignedBB(axisAlignedBB.minX - entity2.posX + d4, axisAlignedBB.minY - entity2.posY + d5, axisAlignedBB.minZ - entity2.posZ + d6, axisAlignedBB.maxX - entity2.posX + d4, axisAlignedBB.maxY - entity2.posY + d5, axisAlignedBB.maxZ - entity2.posZ + d6);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glEnable((int)3042);
            GL11.glDisable((int)3553);
            GL11.glDisable((int)2929);
            GL11.glDepthMask((boolean)false);
            float f2 = (float)(n >> 24 & 0xFF) / 255.0f;
            float f3 = (float)(n >> 16 & 0xFF) / 255.0f;
            float f4 = (float)(n >> 8 & 0xFF) / 255.0f;
            float f5 = (float)(n & 0xFF) / 255.0f;
            if (bl) {
                GlStateManager.color((float)f3, (float)f4, (float)f5, (float)f2);
                GL11.glEnable((int)2848);
                GL11.glLineWidth((float)2.0f);
                RenderGlobal.drawSelectionBoundingBox((AxisAlignedBB)axisAlignedBB2);
                GL11.glDisable((int)2848);
            }
            if (bl2) {
                render.drawBoundingBox(axisAlignedBB2, f3, f4, f5, 0.22f);
            }
            gl.resetColor();
            GL11.glEnable((int)3553);
            GL11.glEnable((int)2929);
            GL11.glDepthMask((boolean)true);
            GL11.glDisable((int)3042);
        }

        private static void drawBoundingBox(AxisAlignedBB axisAlignedBB, float f, float f2, float f3, float f4) {
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            tessellator.draw();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            tessellator.draw();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            tessellator.draw();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            tessellator.draw();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            tessellator.draw();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(f, f2, f3, f4).endVertex();
            tessellator.draw();
        }

        public static boolean isInView(double d, double d2, double d3, double d4, double d5, double d6) {
            net.minecraft.entity.Entity entity = Defaults.mc.getRenderViewEntity();
            FRUSTUM.setPosition(entity.posX, entity.posY, entity.posZ);
            return FRUSTUM.isBoxInFrustum(d, d2, d3, d4, d5, d6);
        }

        public static int getFontHeight() {
            return Defaults.mc.fontRendererObj.FONT_HEIGHT;
        }

        public static void item(ItemStack itemStack, float f, float f2, float f3) {
            Defaults.mc.entityRenderer.setupOverlayRendering();
            if (f3 != 1.0f) {
                GlStateManager.pushMatrix();
                GlStateManager.scale((float)f3, (float)f3, (float)f3);
            }
            ScriptBridge.renderItem(itemStack.itemStack, f / f3, f2 / f3);
            if (f3 != 1.0f) {
                GlStateManager.scale((float)1.0f, (float)1.0f, (float)1.0f);
                GlStateManager.popMatrix();
            }
        }

        public static void draw3DPolygon(Vec3 vec3, double d, int n, double d2, int n2) {
            render.draw3DPolygon(vec3.x, vec3.y, vec3.z, d, n, d2, n2);
        }

        public static void drawBeam(double d, double d2, double d3, double d4, double d5, int n, float f) {
            RenderManager renderManager = Defaults.mc.getRenderManager();
            d -= renderManager.viewerPosX;
            d2 -= renderManager.viewerPosY;
            d3 -= renderManager.viewerPosZ;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableCull();
            GlStateManager.depthMask((boolean)false);
            GlStateManager.tryBlendFuncSeparate((int)770, (int)771, (int)1, (int)0);
            float f2 = (float)(n >> 16 & 0xFF) / 255.0f;
            float f3 = (float)(n >> 8 & 0xFF) / 255.0f;
            float f4 = (float)(n & 0xFF) / 255.0f;
            double d6 = 0.0;
            double d7 = d4;
            double d8 = d + f117054 - d5;
            double d9 = d + f117055 + d5;
            double d10 = d3 + f117056 - d5;
            double d11 = d3 + f117057 + d5;
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(d8, d2 + d7, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d6, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d6, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d7, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d7, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d6, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d6, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d7, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d7, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d6, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d6, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d8, d2 + d7, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d7, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d6, d10).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d6, d11).color(f2, f3, f4, f).endVertex();
            worldRenderer.pos(d9, d2 + d7, d11).color(f2, f3, f4, f).endVertex();
            tessellator.draw();
            GlStateManager.depthMask((boolean)true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
        }

        public static void draw3DPolygon(double d, double d2, double d3, double d4, int n, double d5, int n2) {
            if (n < 3) {
                return;
            }
            float f = (float)(n2 >> 24 & 0xFF) / 255.0f;
            float f2 = (float)(n2 >> 16 & 0xFF) / 255.0f;
            float f3 = (float)(n2 >> 8 & 0xFF) / 255.0f;
            float f4 = (float)(n2 & 0xFF) / 255.0f;
            GL11.glDisable((int)3553);
            GL11.glEnable((int)3042);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glDisable((int)2929);
            GL11.glEnable((int)2848);
            GL11.glDepthMask((boolean)false);
            GL11.glLineWidth((float)((float)d5));
            GlStateManager.color((float)f2, (float)f3, (float)f4, (float)f);
            GL11.glBegin((int)1);
            for (int i = 0; i < n * 2; ++i) {
                double d6 = f117102 * (double)i / (double)n + Math.toRadians(f117103);
                GL11.glVertex3d((double)(d + Math.cos(d6) * d4), (double)d2, (double)(d3 + Math.sin(d6) * d4));
            }
            GL11.glEnd();
            gl.resetColor();
            GL11.glDepthMask((boolean)true);
            GL11.glDisable((int)2848);
            GL11.glEnable((int)2929);
            GL11.glDisable((int)3042);
            GL11.glEnable((int)3553);
        }

        public static void block(Vec3 vec3, int n, boolean bl, boolean bl2) {
            render.block(MathHelper.floor_double((double)vec3.x), MathHelper.floor_double((double)vec3.y), MathHelper.floor_double((double)vec3.z), n, bl, bl2);
        }

        public static Vec3 worldToScreen(double d, double d2, double d3, int n, float f) {
            if (!ScriptReflection.failed) {
                try {
                    RenderManager renderManager = Defaults.mc.getRenderManager();
                    d -= renderManager.viewerPosX;
                    d2 -= renderManager.viewerPosY;
                    d3 -= renderManager.viewerPosZ;
                    ScriptReflection.SETUP_CAMERA_TRANSFORM.get().invoke(Defaults.mc.entityRenderer, Float.valueOf(f), 0);
                    GL11.glGetFloat((int)2982, (FloatBuffer)MODELVIEW);
                    GL11.glGetFloat((int)2983, (FloatBuffer)PROJECTION);
                    GL11.glGetInteger((int)2978, (IntBuffer)VIEWPORT);
                    net.minecraft.util.Vec3 vec3 = null;
                    if (Project.gluProject((float)((float)d), (float)((float)d2), (float)((float)d3), (FloatBuffer)MODELVIEW, (FloatBuffer)PROJECTION, (IntBuffer)VIEWPORT, (FloatBuffer)OBJECTCOORDS)) {
                        vec3 = new net.minecraft.util.Vec3((double)(OBJECTCOORDS.get(0) / (float)n), (double)(((float)Display.getHeight() - OBJECTCOORDS.get(1)) / (float)n), (double)OBJECTCOORDS.get(2));
                    }
                    Defaults.mc.entityRenderer.setupOverlayRendering();
                    if (vec3 != null) {
                        return new Vec3(vec3);
                    }
                }
                catch (IllegalAccessException | InvocationTargetException reflectiveOperationException) {
                }
            }
            return null;
        }

        public static boolean isInView(Entity entity) {
            net.minecraft.entity.Entity entity2 = Defaults.mc.getRenderViewEntity();
            FRUSTUM.setPosition(entity2.posX, entity2.posY, entity2.posZ);
            return FRUSTUM.isBoundingBoxInFrustum(Entity.access$300(entity).getEntityBoundingBox());
        }

        public static void rect(float f, float f2, float f3, float f4, int n) {
            float f5;
            if (f < f3) {
                f5 = f;
                f = f3;
                f3 = f5;
            }
            if (f2 < f4) {
                f5 = f2;
                f2 = f4;
                f4 = f5;
            }
            f5 = (float)(n >> 24 & 0xFF) / 255.0f;
            float f6 = (float)(n >> 16 & 0xFF) / 255.0f;
            float f7 = (float)(n >> 8 & 0xFF) / 255.0f;
            float f8 = (float)(n & 0xFF) / 255.0f;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate((int)770, (int)771, (int)1, (int)0);
            GlStateManager.color((float)f6, (float)f7, (float)f8, (float)f5);
            worldRenderer.begin(7, DefaultVertexFormats.POSITION);
            worldRenderer.pos((double)f, (double)f4, 0.0).endVertex();
            worldRenderer.pos((double)f3, (double)f4, 0.0).endVertex();
            worldRenderer.pos((double)f3, (double)f2, 0.0).endVertex();
            worldRenderer.pos((double)f, (double)f2, 0.0).endVertex();
            tessellator.draw();
            gl.resetColor();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }

        public static void tracer(Vec3 vec3, float f, int n, float f2) {
            render.tracer(vec3.x, vec3.y, vec3.z, f, n, f2);
        }

        public static void tracer(double d, double d2, double d3, float f, int n, float f2) {
            render.line3D(Defaults.mc.thePlayer.posX, Defaults.mc.thePlayer.posY + (double)Defaults.mc.thePlayer.getEyeHeight(), Defaults.mc.thePlayer.posZ, d, d2, d3, f, n);
        }

        public static Vec3 getPosition() {
            return new Vec3(Defaults.mc.getRenderManager().viewerPosX, Defaults.mc.getRenderManager().viewerPosY, Defaults.mc.getRenderManager().viewerPosZ);
        }

        public static void line3D(double d, double d2, double d3, double d4, double d5, double d6, float f, int n) {
            RenderManager renderManager = Defaults.mc.getRenderManager();
            d4 -= renderManager.viewerPosX;
            d5 -= renderManager.viewerPosY;
            d6 -= renderManager.viewerPosZ;
            float f2 = (float)(n >> 24 & 0xFF) / 255.0f;
            float f3 = (float)(n >> 16 & 0xFF) / 255.0f;
            float f4 = (float)(n >> 8 & 0xFF) / 255.0f;
            float f5 = (float)(n & 0xFF) / 255.0f;
            GL11.glEnable((int)3042);
            GL11.glEnable((int)2848);
            GL11.glDisable((int)2929);
            GL11.glDisable((int)3553);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glLineWidth((float)f);
            GlStateManager.color((float)f3, (float)f4, (float)f5, (float)f2);
            GL11.glBegin((int)2);
            GL11.glVertex3d((double)(d - Defaults.mc.thePlayer.posX), (double)(d2 - Defaults.mc.thePlayer.posY), (double)(d3 - Defaults.mc.thePlayer.posZ));
            GL11.glVertex3d((double)d4, (double)d5, (double)d6);
            GL11.glEnd();
            gl.resetColor();
            GL11.glEnable((int)3553);
            GL11.glEnable((int)2929);
            GL11.glDisable((int)2848);
            GL11.glDisable((int)3042);
        }

        public static void image(Image image, float f, float f2, float f3, float f4) {
            DynamicTexture dynamicTexture;
            if (image == null || !image.isLoaded()) {
                return;
            }
            if (image.textureId == -1) {
                dynamicTexture = new DynamicTexture(image.bufferedImage);
                GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
                dynamicTexture.updateDynamicTexture();
                image.textureId = dynamicTexture.getGlTextureId();
            }
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture((int)image.textureId);
            GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldRenderer.pos((double)f, (double)(f2 + f4), 0.0).tex(0.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldRenderer.pos((double)(f + f3), (double)(f2 + f4), 0.0).tex(1.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldRenderer.pos((double)(f + f3), (double)f2, 0.0).tex(1.0, 0.0).color(255, 255, 255, 255).endVertex();
            worldRenderer.pos((double)f, (double)f2, 0.0).tex(0.0, 0.0).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            gl.resetColor();
        }

        public static void entityGui(Entity entity, int n, int n2, float f, float f2, int n3) {
            if (!entity.isLiving) {
                return;
            }
            GuiInventory.drawEntityOnScreen((int)n, (int)n2, (int)n3, (float)f, (float)f2, (EntityLivingBase)((EntityLivingBase)Entity.access$300(entity)));
        }

        public static void roundedRect(float f, float f2, float f3, float f4, float f5, int n) {
            ScriptBridge.roundedRect(f, f2, f3, f4, f5, n);
        }

        public static void drawBeam(Vec3 vec3, double d, double d2, int n, float f) {
            render.drawBeam(vec3.x, vec3.y, vec3.z, d, d2, n, f);
        }

        public static void tracer(Entity entity, float f, int n, float f2) {
            net.minecraft.entity.Entity entity2 = Entity.access$300(entity);
            double d = entity2.lastTickPosX + (entity2.posX - entity2.lastTickPosX) * (double)f2;
            double d2 = (double)entity2.getEyeHeight() - f117250 + entity2.lastTickPosY + (entity2.posY - entity2.lastTickPosY) * (double)f2;
            double d3 = entity2.lastTickPosZ + (entity2.posZ - entity2.lastTickPosZ) * (double)f2;
            render.tracer(d, d2, d3, f, n, f2);
        }

        public static double[] getRotations() {
            return new double[]{Defaults.mc.getRenderManager().playerViewY, Defaults.mc.getRenderManager().playerViewX};
        }

        public static void text(String string, float f, float f2, float f3, int n, boolean bl) {
            if (f3 != 1.0f) {
                GlStateManager.pushMatrix();
                GlStateManager.scale((float)f3, (float)f3, (float)f3);
            }
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate((int)770, (int)771, (int)1, (int)0);
            Defaults.mc.fontRendererObj.drawString(string, f / f3, f2 / f3, n, bl);
            GlStateManager.disableBlend();
            if (f3 != 1.0f) {
                GlStateManager.scale((float)1.0f, (float)1.0f, (float)1.0f);
                GlStateManager.popMatrix();
            }
        }

        public static void line2D(double d, double d2, double d3, double d4, float f, int n) {
            float f2 = (float)(n >> 24 & 0xFF) / 255.0f;
            float f3 = (float)(n >> 16 & 0xFF) / 255.0f;
            float f4 = (float)(n >> 8 & 0xFF) / 255.0f;
            float f5 = (float)(n & 0xFF) / 255.0f;
            GL11.glEnable((int)3042);
            GL11.glEnable((int)2848);
            GL11.glDisable((int)2929);
            GL11.glDisable((int)3553);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glLineWidth((float)f);
            GlStateManager.color((float)f3, (float)f4, (float)f5, (float)f2);
            GL11.glBegin((int)2);
            GL11.glVertex2d((double)d, (double)d2);
            GL11.glVertex2d((double)d3, (double)d4);
            GL11.glEnd();
            gl.resetColor();
            GL11.glEnable((int)3553);
            GL11.glEnable((int)2929);
            GL11.glDisable((int)2848);
            GL11.glDisable((int)3042);
        }

        public static int getThemeRGB(String string) {
            return ScriptBridge.getThemeRGB(string);
        }

        public static Vec3 worldToScreen(Vec3 vec3, int n, float f) {
            return render.worldToScreen(vec3.x, vec3.y, vec3.z, n, f);
        }

        public static class blur {
            public static void prepare() {
                ScriptBridge.prepareBlur();
            }

            public static void apply(int n, float f) {
                ScriptBridge.applyBlur(n, f);
            }
        }

        public static class bloom {
            public static void apply(int n, float f) {
                ScriptBridge.applyBloom(n, f);
            }

            public static void prepare() {
                ScriptBridge.prepareBloom();
            }
        }
    }

    public static class C0F
    extends CPacket {
        public int windowId;
        public short uid;
        private static int f116672 = -1446089756 >>> 214 | -1446089756 << -214;
        private static int f116673 = Integer.reverse(1699345820);

        public C0F(int n, short s2) {
            super((Packet)C0F.create(n, s2));
        }

        private static C0FPacketConfirmTransaction create(int n, short s2) {
            return new C0FPacketConfirmTransaction(n, s2, true);
        }

        public C0F(byte by, C0FPacketConfirmTransaction c0FPacketConfirmTransaction) {
            super((Packet)c0FPacketConfirmTransaction);
            this.windowId = c0FPacketConfirmTransaction.getWindowId();
            this.uid = c0FPacketConfirmTransaction.getUid();
        }
    }

    public static class C03
    extends CPacket {
        public boolean ground;
        public boolean moving;
        public float yaw;
        public boolean rotating;
        public float pitch;
        public Vec3 position;

        public C03(byte by, C03PacketPlayer c03PacketPlayer) {
            super((Packet)c03PacketPlayer);
            this.position = new Vec3(c03PacketPlayer.getPositionX(), c03PacketPlayer.getPositionY(), c03PacketPlayer.getPositionZ());
            this.yaw = c03PacketPlayer.getYaw();
            this.pitch = c03PacketPlayer.getPitch();
            this.ground = c03PacketPlayer.isOnGround();
            this.moving = c03PacketPlayer.isMoving();
            this.rotating = c03PacketPlayer.getRotating();
        }

        public C03(Vec3 vec3, boolean bl) {
            super((Packet)new C03PacketPlayer.C04PacketPlayerPosition(vec3.x, vec3.y, vec3.z, bl));
        }

        public C03(Vec3 vec3, float f, float f2, boolean bl) {
            super((Packet)new C03PacketPlayer.C06PacketPlayerPosLook(vec3.x, vec3.y, vec3.z, f, f2, bl));
        }

        public C03(float f, float f2, boolean bl) {
            super((Packet)new C03PacketPlayer.C05PacketPlayerLook(f, f2, bl));
        }

        public C03(boolean bl) {
            super((Packet)new C03PacketPlayer(bl));
        }
    }

    public static class C02
    extends CPacket {
        public String action;
        public Vec3 hitVec;
        public Entity entity;

        public C02(byte by, C02PacketUseEntity c02PacketUseEntity) {
            super((Packet)c02PacketUseEntity);
            this.entity = Entity.of(c02PacketUseEntity.getEntityFromWorld((World)Defaults.mc.theWorld));
            this.action = c02PacketUseEntity.getAction().name();
            if (c02PacketUseEntity.getHitVec() != null) {
                this.hitVec = new Vec3(c02PacketUseEntity.getHitVec().xCoord, c02PacketUseEntity.getHitVec().yCoord, c02PacketUseEntity.getHitVec().zCoord);
            }
        }

        private static C02PacketUseEntity create(Entity entity, String string, Vec3 vec3) {
            if (vec3 == null) {
                return new C02PacketUseEntity(Entity.access$300(entity), ScriptUtil.enumByName(C02PacketUseEntity.Action.class, string));
            }
            return new C02PacketUseEntity(Entity.access$300(entity), Vec3.getVec3(vec3));
        }

        public C02(Entity entity, String string, Vec3 vec3) {
            super((Packet)C02.create(entity, string, vec3));
        }
    }

    public static class S27
    extends SPacket {
        public float strength;
        public Vec3 position;
        public Vec3 motion;

        public S27(S27PacketExplosion s27PacketExplosion) {
            super((Packet)s27PacketExplosion);
            this.strength = s27PacketExplosion.getStrength();
            this.position = new Vec3(s27PacketExplosion.getX(), s27PacketExplosion.getY(), s27PacketExplosion.getZ());
            this.motion = new Vec3(s27PacketExplosion.func_149149_c(), s27PacketExplosion.func_149144_d(), s27PacketExplosion.func_149147_e());
        }
    }

    public static class client {
        private static int f116674 = Integer.reverse(1113760002);
        private static int f116675 = Integer.reverse(-1033723646);
        private static int f116676 = 2047044228 >>> 95 | 2047044228 << -95;
        private static int f116677 = Integer.reverse(279666735);
        private static int f116678 = 1766691074 >>> 225 | 1766691074 << ~225 + 1;
        private static int f116679 = 699539533 >>> 198 | 699539533 << ~198 + 1;
        private static int f116680 = 797282357 >>> 72 | 797282357 << -72;
        private static int f116681 = 797283125 >>> 168 | 797283125 << ~168 + 1;
        private static int f116682 = (-1092533624 >>> 149 | -1092533624 << ~149 + 1) & 0xFFFFFFFF;
        private static int f116683 = (293454469 >>> 238 | 293454469 << -238) & 0xFFFFFFFF;
        private static int f116684 = -119645973 >>> 217 | -119645973 << -217;
        private static int f116685 = (1672473465 >>> 139 | 1672473465 << -139) & 0xFFFFFFFF;
        private static int f116686 = (-601489436 >>> 181 | -601489436 << -181) & 0xFFFFFFFF;
        private static int f116687 = (805250787 >>> 32 | 805250787 << -32) & 0xFFFFFFFF;
        private static int f116688 = -250508013 >>> 67 | -250508013 << -67;
        private static int f116689 = Integer.reverse(-980663170);
        private static int f116690 = Integer.reverse(1768757121);
        private static int f116691 = Integer.reverse(1768757121);
        private static int f116692 = -2116221918 >>> 106 | -2116221918 << ~106 + 1;
        private static int f116693 = Integer.reverse(1122895120);
        private static int f116694 = -387403829 >>> 26 | -387403829 << ~26 + 1;
        private static int f116695 = Integer.reverse(1599067228);
        private static int f116696 = 999794585 >>> 81 | 999794585 << ~81 + 1;
        private static int f116697 = (-908284612 >>> 76 | -908284612 << -76) & 0xFFFFFFFF;
        private static int f116698 = -108410638 >>> 51 | -108410638 << -51;
        private static int f116699 = (-1100582340 >>> 81 | -1100582340 << -81) & 0xFFFFFFFF;
        private static int f116700 = Integer.reverse(-254258160);
        private static int f116701 = Integer.reverse(-254258160);
        private static int f116702 = Integer.reverse(-178539782);
        private static int f116703 = (929741181 >>> 202 | 929741181 << -202) & 0xFFFFFFFF;
        private static int f116704 = Integer.reverse(-16468270);
        private static int f116705 = Integer.reverse(-16468270);
        private static int f116706 = Integer.reverse(-1620447591);
        private static int f116707 = Integer.reverse(-1620447591);
        private static int f116708 = Integer.reverse(-1474157944);
        private static int f116709 = -2103302008 >>> 21 | -2103302008 << ~21 + 1;
        private static int f116710 = Integer.reverse(-1655460721);
        private static int f116711 = -1696244488 >>> 167 | -1696244488 << ~167 + 1;
        private static int f116712 = Integer.reverse(687093207);
        private static int f116713 = -1024360774 >>> 204 | -1024360774 << -204;
        private static int f116714 = Integer.reverse(1867943668);
        private static int f116715 = -1111118888 >>> 34 | -1111118888 << -34;
        private static int f116716 = Integer.reverse(949751065);
        private static int f116717 = (479120472 >>> 47 | 479120472 << ~47 + 1) & 0xFFFFFFFF;
        private static int f116718 = Integer.reverse(14830615);
        private static int f116719 = -399358206 >>> 192 | -399358206 << ~192 + 1;
        private static long f116720 = Long.reverse(0xEB5144EB8318DDC7L ^ 0xBD6A98B827054BA6L);
        private static long f116721 = Long.reverse(0x64D3FDD438B371E1L ^ 0x946C6F41FA058BF4L);
        private static long f116722 = Long.reverse(0x8739F6687A89A8B9L ^ 0x7F6264FDB83F52ACL);

        public static void setSneak(boolean bl) {
            Defaults.mc.thePlayer.movementInput.sneak = bl;
        }

        public static void attack(Entity entity) {
            ScriptBridge.attack(Entity.access$300(entity));
        }

        public static void setForward(float f) {
            Defaults.mc.thePlayer.movementInput.moveForward = f;
        }

        public static int getFPS() {
            return Minecraft.getDebugFPS();
        }

        public static void sendPacketNoEvent(CPacket cPacket) {
            ScriptBridge.sendPacketNoEvent(cPacket.packet);
        }

        public static void setStrafe(float f) {
            Defaults.mc.thePlayer.movementInput.moveStrafe = f;
        }

        public static void removeFriend(String string) {
            ScriptBridge.removeFriend(string);
        }

        public static boolean isFlying() {
            return Defaults.mc.thePlayer.capabilities.isFlying;
        }

        public static boolean isMoving() {
            return (client.getForward() != 0.0f || client.getStrafe() != 0.0f ? 1 : 0) != 0;
        }

        public static Object[] raycastEntity(double d, float f, float f2) {
            net.minecraft.entity.Entity entity = null;
            MovingObjectPosition movingObjectPosition = Defaults.mc.thePlayer.rayTrace(d, 1.0f);
            net.minecraft.util.Vec3 vec3 = Defaults.mc.thePlayer.getPositionEyes(1.0f);
            net.minecraft.util.Vec3 vec32 = ScriptUtil.lookVec(f, f2);
            net.minecraft.util.Vec3 vec33 = vec3.addVector(vec32.xCoord * d, vec32.yCoord * d, vec32.zCoord * d);
            net.minecraft.util.Vec3 vec34 = null;
            List list = Defaults.mc.theWorld.getEntitiesWithinAABBExcludingEntity(Defaults.mc.getRenderViewEntity(), Defaults.mc.getRenderViewEntity().getEntityBoundingBox().addCoord(vec32.xCoord * d, vec32.yCoord * d, vec32.zCoord * d).expand(1.0, 1.0, 1.0));
            double d2 = d;
            for (int i = 0; i < list.size(); ++i) {
                double d3;
                net.minecraft.entity.Entity entity2 = (net.minecraft.entity.Entity)list.get(i);
                if (!(entity2 instanceof EntityLivingBase) || !entity2.canBeCollidedWith() || ((EntityLivingBase)entity2).deathTime != 0) continue;
                float f3 = entity2.getCollisionBorderSize();
                AxisAlignedBB axisAlignedBB = entity2.getEntityBoundingBox().expand((double)f3, (double)f3, (double)f3);
                MovingObjectPosition movingObjectPosition2 = axisAlignedBB.calculateIntercept(vec3, vec33);
                if (axisAlignedBB.isVecInside(vec3)) {
                    if (!(0.0 < d2) && d2 != 0.0) continue;
                    entity = entity2;
                    vec34 = movingObjectPosition2 == null ? vec3 : movingObjectPosition2.hitVec;
                    d2 = 0.0;
                    continue;
                }
                if (movingObjectPosition2 == null || !((d3 = vec3.distanceTo(movingObjectPosition2.hitVec)) < d2) && d2 != 0.0) continue;
                if (entity2 == Defaults.mc.getRenderViewEntity().ridingEntity && !entity2.canRiderInteract()) {
                    if (d2 != 0.0) continue;
                    entity = entity2;
                    vec34 = movingObjectPosition2.hitVec;
                    continue;
                }
                entity = entity2;
                vec34 = movingObjectPosition2.hitVec;
                d2 = d3;
            }
            if (entity != null && (d2 < d || movingObjectPosition == null)) {
                movingObjectPosition = new MovingObjectPosition(entity, vec34);
                Vec3 vec35 = new Vec3(movingObjectPosition.hitVec.xCoord - entity.posX, movingObjectPosition.hitVec.yCoord - entity.posY, movingObjectPosition.hitVec.zCoord - entity.posZ);
                return new Object[]{new Entity(movingObjectPosition.entityHit), vec35, vec3.squareDistanceTo(movingObjectPosition.hitVec)};
            }
            return null;
        }

        public static void processPacket(SPacket sPacket) {
            try {
                sPacket.packet.processPacket(Defaults.mc.getNetHandler().getNetworkManager().getNetHandler());
            }
            catch (Exception exception) {
            }
        }

        public static long getMaxMemory() {
            return Runtime.getRuntime().maxMemory();
        }

        public static boolean sleep(long l) {
            try {
                Thread.sleep(l);
                return true;
            }
            catch (InterruptedException interruptedException) {
                return false;
            }
        }

        public static boolean isFriend(String string) {
            return ScriptBridge.isFriend(string);
        }

        public static void log(Object object) {
            ScriptUtil.LOGGER.info(object);
        }

        public static void setTimer(double d) {
            ScriptBridge.setTimer(d);
        }

        public static boolean isJump() {
            return Defaults.mc.thePlayer.movementInput.jump;
        }

        public static int[] getDisplaySize() {
            ScaledResolution scaledResolution = new ScaledResolution(Defaults.mc);
            return new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        }

        public static float getForward() {
            return Defaults.mc.thePlayer.movementInput.moveForward;
        }

        public static void setRenderArmPitch(float f) {
            Defaults.mc.thePlayer.renderArmPitch = Defaults.mc.thePlayer.prevRenderArmPitch = f;
        }

        public static Object[] raycastEntity(double d) {
            return client.raycastEntity(d, Defaults.mc.thePlayer.rotationYaw, Defaults.mc.thePlayer.rotationPitch);
        }

        public static void jump() {
            Defaults.mc.thePlayer.jump();
        }

        public static void swingReset() {
            client.swing();
            Defaults.mc.getItemRenderer().resetEquippedProgress();
        }

        public static float getRenderArmPitch() {
            return Defaults.mc.thePlayer.renderArmPitch;
        }

        public static boolean allowFlying() {
            return Defaults.mc.thePlayer.capabilities.allowFlying;
        }

        public static void removeEnemy(String string) {
            ScriptBridge.removeEnemy(string);
        }

        public static void setMotion(double d, double d2, double d3) {
            Defaults.mc.thePlayer.motionX = d;
            Defaults.mc.thePlayer.motionY = d2;
            Defaults.mc.thePlayer.motionZ = d3;
        }

        public static int getUID() {
            return ScriptBridge.getUID();
        }

        public static void playSound(String string, float f, float f2) {
            Defaults.mc.thePlayer.playSound(string, f, f2);
        }

        public static boolean allowEditing() {
            return Defaults.mc.thePlayer.capabilities.allowEdit;
        }

        public static void setMotion(Vec3 vec3) {
            client.setMotion(vec3.x, vec3.y, vec3.z);
        }

        public static String getServerIP() {
            return client.isSingleplayer() || Defaults.mc.getCurrentServerData() == null ? "" : Defaults.mc.getCurrentServerData().serverIP;
        }

        public static Entity getPlayer() {
            return Entity.of((net.minecraft.entity.Entity)Defaults.mc.thePlayer);
        }

        public static boolean isSingleplayer() {
            return Defaults.mc.isSingleplayer();
        }

        public static long getFreeMemory() {
            return Runtime.getRuntime().freeMemory();
        }

        public static void addFriend(String string) {
            ScriptBridge.addFriend(string);
        }

        public static void setSpeed(double d) {
            ScriptBridge.setSpeed(d);
        }

        public static void setTabName(String string, String string2) {
            NetworkPlayerInfo networkPlayerInfo = Defaults.mc.getNetHandler().getPlayerInfo(string);
            if (networkPlayerInfo != null) {
                networkPlayerInfo.setDisplayName((IChatComponent)(string2 == null ? null : new ChatComponentText(util.color(string2))));
            }
        }

        public static void setFlying(boolean bl) {
            Defaults.mc.thePlayer.capabilities.isFlying = bl;
        }

        public static void sendPacket(CPacket cPacket) {
            ScriptBridge.sendPacket(cPacket.packet);
        }

        public static long nanoTime() {
            return System.nanoTime();
        }

        public static boolean isSneak() {
            return Defaults.mc.thePlayer.movementInput.sneak;
        }

        public static Object[] raycastBlock(double d) {
            return client.raycastBlock(d, Defaults.mc.thePlayer.rotationYaw, Defaults.mc.thePlayer.rotationPitch);
        }

        public static void setJump(boolean bl) {
            Defaults.mc.thePlayer.movementInput.jump = bl;
        }

        public static boolean canSeeEntity(Entity entity) {
            return entity == null ? false : ScriptBridge.canSeeEntity(Entity.access$300(entity));
        }

        public static boolean isEnemy(String string) {
            return ScriptBridge.isEnemy(string);
        }

        public static void pling() {
            ScriptBridge.pling();
        }

        public static void addEnemy(String string) {
            ScriptBridge.addEnemy(string);
        }

        public static long getTotalMemory() {
            return Runtime.getRuntime().totalMemory();
        }

        public static void multiplyMotion(double d) {
            Defaults.mc.thePlayer.motionX *= d;
            Defaults.mc.thePlayer.motionZ *= d;
        }

        public static boolean runForgeCommand(String string) {
            return ClientCommandHandler.instance.executeCommand((ICommandSender)Defaults.mc.thePlayer, "/" + string) != 0;
        }

        public static String getServerDirection(PlayerState playerState) {
            return ScriptBridge.getServerDirection(playerState);
        }

        public static void setSpeedForward(double d) {
            ScriptBridge.setSpeedForward(d);
        }

        public static void swing() {
            Defaults.mc.thePlayer.swingItem();
        }

        public static void dropItem(boolean bl) {
            Defaults.mc.thePlayer.dropOneItem(bl);
        }

        public static Object[] raycastBlock(double d, float f, float f2) {
            net.minecraft.util.Vec3 vec3 = Defaults.mc.thePlayer.getPositionEyes(1.0f);
            net.minecraft.util.Vec3 vec32 = ScriptUtil.lookVec(f, f2);
            net.minecraft.util.Vec3 vec33 = vec3.addVector(vec32.xCoord * d, vec32.yCoord * d, vec32.zCoord * d);
            MovingObjectPosition movingObjectPosition = Defaults.mc.theWorld.rayTraceBlocks(vec3, vec33, false, false, false);
            if (movingObjectPosition == null || movingObjectPosition.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return null;
            }
            Vec3 vec34 = new Vec3(movingObjectPosition.getBlockPos());
            Vec3 vec35 = new Vec3(movingObjectPosition.hitVec.xCoord - vec34.x, movingObjectPosition.hitVec.yCoord - vec34.y, movingObjectPosition.hitVec.zCoord - vec34.z);
            return new Object[]{vec34, vec35, movingObjectPosition.sideHit.name()};
        }

        public static boolean placeBlock(Vec3 vec3, String string, Vec3 vec32) {
            return Defaults.mc.playerController.onPlayerRightClick(Defaults.mc.thePlayer, Defaults.mc.theWorld, Defaults.mc.thePlayer.getHeldItem(), Vec3.getBlockPos(vec3), ScriptUtil.enumByName(EnumFacing.class, string), Vec3.getVec3(vec32));
        }

        public static void closeScreen() {
            String string = client.getScreen();
            if (string.contains("GuiRaven")) {
                Defaults.mc.displayGuiScreen(null);
            } else {
                Defaults.mc.thePlayer.closeScreen();
            }
        }

        public static float getStrafe() {
            return Defaults.mc.thePlayer.movementInput.moveStrafe;
        }

        public static float[] getRotationsToEntity(Entity entity) {
            return ScriptBridge.getRotationsToEntity(Entity.access$300(entity));
        }

        public static Vec3 getMotion() {
            return new Vec3(Defaults.mc.thePlayer.motionX, Defaults.mc.thePlayer.motionY, Defaults.mc.thePlayer.motionZ);
        }

        public static String getUser() {
            return ScriptBridge.getUser();
        }

        public static void async(Runnable runnable) {
            if (ScriptDefaults.access$200() == null) {
                ScriptDefaults.access$202(Executors.newCachedThreadPool());
            }
            ScriptDefaults.access$200().execute(runnable);
        }

        public static float[] getRotationsToBlock(Vec3 vec3, String string) {
            return ScriptBridge.getRotationsToBlock(Vec3.getBlockPos(vec3), ScriptUtil.enumByName(EnumFacing.class, string));
        }

        public static int timeTag() {
            return (int)(client.time() % (0xA85F6D43A949FD0FL ^ 0xA85F6D43A949DA1FL));
        }

        public static boolean isCreative() {
            return Defaults.mc.thePlayer.capabilities.isCreativeMode;
        }

        public static String debug(String string) {
            return ScriptBridge.debug(string);
        }

        public static void print(Object object) {
            ScriptBridge.sendMessageNoPrefix(object);
        }

        public static String getVersion() {
            return ScriptBridge.getVersion();
        }

        public static boolean isFacingDiagonal() {
            return ScriptBridge.isFacingDiagonal();
        }

        public static String getDirection() {
            return ScriptBridge.getDirection();
        }

        public static float[] getRotationsToBlock(Vec3 vec3) {
            return client.getRotationsToBlock(vec3, null);
        }

        public static String getScreen() {
            return ScriptBridge.getScreen();
        }

        public static float[] getRotations(Vec3 vec3) {
            return ScriptBridge.getRotationsToPos(vec3.x, vec3.y, vec3.z);
        }

        public static void chat(String string) {
            if (ScriptUtil.inGame()) {
                Defaults.mc.thePlayer.sendChatMessage(string);
            }
        }

        public static boolean isDiagonal() {
            return ScriptBridge.isDiagonal();
        }

        public static long time() {
            return System.currentTimeMillis();
        }

        public static boolean runRavenCommand(String string) {
            return ScriptBridge.runRavenCommand(string);
        }

        public static void removePotionEffect(int n) {
            Defaults.mc.thePlayer.removePotionEffect(n);
        }

        public static void setSprinting(boolean bl) {
            Defaults.mc.thePlayer.setSprinting(bl);
        }
    }

    public static class bridge {
        private static Map<String, Object> dataMap = new HashMap<String, Object>();

        public static Object get(String string) {
            return dataMap.get(string);
        }

        public static void add(String string, Object object) {
            dataMap.put(string, object);
        }

        public static boolean has(String string) {
            return dataMap.containsKey(string);
        }

        public static void add(String string) {
            bridge.add(string, null);
        }

        public static Object remove(String string) {
            return dataMap.remove(string);
        }

        public static void clear() {
            dataMap.clear();
        }
    }

    public static class S08
    extends SPacket {
        public float yaw;
        public Vec3 position;
        public float pitch;

        public S08(S08PacketPlayerPosLook s08PacketPlayerPosLook) {
            super((Packet)s08PacketPlayerPosLook);
            this.position = new Vec3(s08PacketPlayerPosLook.getX(), s08PacketPlayerPosLook.getY(), s08PacketPlayerPosLook.getZ());
            this.yaw = s08PacketPlayerPosLook.getYaw();
            this.pitch = s08PacketPlayerPosLook.getPitch();
        }
    }

    public static class S06
    extends SPacket {
        public int food;
        public float health;
        public float saturation;

        public S06(S06PacketUpdateHealth s06PacketUpdateHealth) {
            super((Packet)s06PacketUpdateHealth);
            this.health = s06PacketUpdateHealth.getHealth();
            this.saturation = s06PacketUpdateHealth.getSaturationLevel();
            this.food = s06PacketUpdateHealth.getFoodLevel();
        }
    }

    public static class S2A
    extends SPacket {
        public String type;
        public int count;
        public float speed;
        public int[] args;
        public Vec3 offset;
        public Vec3 position;

        public S2A(S2APacketParticles s2APacketParticles) {
            super((Packet)s2APacketParticles);
            this.type = s2APacketParticles.getParticleType() == null ? "null" : s2APacketParticles.getParticleType().name();
            this.position = new Vec3(s2APacketParticles.getXCoordinate(), s2APacketParticles.getYCoordinate(), s2APacketParticles.getZCoordinate());
            this.offset = new Vec3(s2APacketParticles.getXOffset(), s2APacketParticles.getYOffset(), s2APacketParticles.getZOffset());
            this.speed = s2APacketParticles.getParticleSpeed();
            this.count = s2APacketParticles.getParticleCount();
            this.args = s2APacketParticles.getParticleArgs();
        }
    }

    public static class C0B
    extends CPacket {
        public int horsepower;
        public String action;

        public C0B(String string, int n) {
            super((Packet)C0B.create(string, n));
        }

        public C0B(byte by, C0BPacketEntityAction c0BPacketEntityAction) {
            super((Packet)c0BPacketEntityAction);
            this.action = c0BPacketEntityAction.getAction().name();
            this.horsepower = c0BPacketEntityAction.getAuxData();
        }

        private static C0BPacketEntityAction create(String string, int n) {
            return new C0BPacketEntityAction((net.minecraft.entity.Entity)Defaults.mc.thePlayer, ScriptUtil.enumByName(C0BPacketEntityAction.Action.class, string), n);
        }
    }

    public static class Block {
        public double height;
        private IBlockState state;
        public double length;
        public String name;
        public double width;
        public String type;
        public boolean interactable;
        public int variant;

        public String toString() {
            return "Block(" + this.type + "," + this.name + ")";
        }

        public Block(IBlockState iBlockState) {
            net.minecraft.block.Block block = iBlockState.getBlock();
            this.type = block.getClass().getSimpleName();
            this.name = ScriptUtil.stripNamespace(block.getRegistryName());
            this.variant = block.getMetaFromState(iBlockState);
            this.interactable = ScriptBridge.canInteract(block);
            this.height = block.getBlockBoundsMaxY();
            this.width = block.getBlockBoundsMaxX();
            this.length = block.getBlockBoundsMaxZ();
        }

        public Block(int n, int n2, int n3) {
            this(Defaults.mc.theWorld.getBlockState(new BlockPos(n, n2, n3)));
        }
    }

    public static class C09
    extends CPacket {
        public int slot;

        public C09(byte by, C09PacketHeldItemChange c09PacketHeldItemChange) {
            super((Packet)c09PacketHeldItemChange);
            this.slot = c09PacketHeldItemChange.getSlotId();
        }

        private static C09PacketHeldItemChange create(int n) {
            return new C09PacketHeldItemChange(n);
        }

        public C09(int n) {
            super((Packet)C09.create(n));
        }
    }

    public static class PlayerState {
        public double x;
        public double z;
        public boolean isSneaking;
        public double y;
        public float yaw;
        public float pitch;
        public boolean isSprinting;
        public boolean onGround;
        private static int f116931 = Integer.reverse(1122765627);
        private static int f116932 = (-427705834 >>> 35 | -427705834 << -35) & 0xFFFFFFFF;
        private static int f116933 = Integer.reverse(1158956192);
        private static int f116934 = Integer.reverse(-988527456);
        private static int f116935 = (-461220213 >>> 46 | -461220213 << -46) & 0xFFFFFFFF;
        private static int f116936 = Integer.reverse(-800459686);
        private static int f116937 = Integer.reverse(725109881);
        private static int f116938 = (-968647949 >>> 229 | -968647949 << -229) & 0xFFFFFFFF;
        private static int f116939 = Integer.reverse(996128809);
        private static int f116940 = Integer.reverse(459257897);
        private static int f116941 = (-219053794 >>> 89 | -219053794 << ~89 + 1) & 0xFFFFFFFF;
        private static int f116942 = -1887668032 >>> 80 | -1887668032 << ~80 + 1;
        private static int f116943 = Integer.reverse(869389834);
        private static int f116944 = (-1801398505 >>> 121 | -1801398505 << -121) & 0xFFFFFFFF;
        private static int f116945 = (-1038817437 >>> 236 | -1038817437 << ~236 + 1) & 0xFFFFFFFF;
        private static int f116946 = Integer.reverse(-1836827538);
        private static int f116947 = Integer.reverse(-1860592156);
        private static int f116948 = -1736366151 >>> 80 | -1736366151 << ~80 + 1;
        private static int f116949 = (-1968780678 >>> 243 | -1968780678 << -243) & 0xFFFFFFFF;
        private static int f116950 = Integer.reverse(713749147);
        private static int f116951 = Integer.reverse(-1023288026);
        private static int f116952 = 815345376 >>> 150 | 815345376 << ~150 + 1;
        private static int f116953 = (1319749391 >>> 200 | 1319749391 << ~200 + 1) & 0xFFFFFFFF;
        private static int f116954 = Integer.reverse(-1550486800);
        private static int f116955 = Integer.reverse(-830925786);
        private static int f116956 = Integer.reverse(242816038);
        private static int f116957 = Integer.reverse(-1168474006);
        private static int f116958 = Integer.reverse(-1705344918);
        private static int f116959 = -1416983359 >>> 6 | -1416983359 << -6;
        private static int f116960 = 354091863 >>> 207 | 354091863 << ~207 + 1;
        private static int f116961 = Integer.reverse(-625394924);
        private static int f116962 = (1970578290 >>> 114 | 1970578290 << ~114 + 1) & 0xFFFFFFFF;
        private static int f116963 = -498645669 >>> 108 | -498645669 << -108;
        private static int f116964 = Integer.reverse(-836469334);

        public PlayerState(Object[] objectArray) {
            this.x = (Double)objectArray[0];
            this.y = (Double)objectArray[1];
            this.z = (Double)objectArray[2];
            this.yaw = ((Float)objectArray[3]).floatValue();
            this.pitch = ((Float)objectArray[4]).floatValue();
            this.onGround = (Boolean)objectArray[5];
            this.isSprinting = (Boolean)objectArray[6];
            this.isSneaking = (Boolean)objectArray[7];
        }

        public Object[] asArray() {
            return new Object[]{this.x, this.y, this.z, Float.valueOf(this.yaw), Float.valueOf(this.pitch), this.onGround, this.isSprinting, this.isSneaking};
        }

        public PlayerState(double d, double d2, double d3, float f, float f2, boolean bl, boolean bl2, boolean bl3) {
            this.x = d;
            this.y = d2;
            this.z = d3;
            this.yaw = f;
            this.pitch = f2;
            this.onGround = bl;
            this.isSprinting = bl2;
            this.isSneaking = bl3;
        }
    }

    public static class S12
    extends SPacket {
        public Vec3 motion;
        public int entityId;

        public S12(S12PacketEntityVelocity s12PacketEntityVelocity) {
            super((Packet)s12PacketEntityVelocity);
            this.entityId = s12PacketEntityVelocity.getEntityID();
            this.motion = new Vec3(s12PacketEntityVelocity.getMotionX(), s12PacketEntityVelocity.getMotionY(), s12PacketEntityVelocity.getMotionZ());
        }
    }

    public static class C0E
    extends CPacket {
        public int slot;
        public int windowId;
        public int mode;
        public int button;
        public short action;
        public ItemStack itemStack;

        private static C0EPacketClickWindow create(int n, int n2, int n3, int n4, ItemStack itemStack) {
            short s2 = Defaults.mc.thePlayer.openContainer.getNextTransactionID(Defaults.mc.thePlayer.inventory);
            return new C0EPacketClickWindow(n, n2, n3, n4, itemStack == null ? null : itemStack.itemStack, s2);
        }

        public C0E(int n, int n2, int n3, int n4, ItemStack itemStack) {
            super((Packet)C0E.create(n, n2, n3, n4, itemStack));
        }

        public C0E(byte by, C0EPacketClickWindow c0EPacketClickWindow) {
            super((Packet)c0EPacketClickWindow);
            this.windowId = c0EPacketClickWindow.getWindowId();
            this.slot = c0EPacketClickWindow.getSlotId();
            this.button = c0EPacketClickWindow.getUsedButton();
            this.mode = c0EPacketClickWindow.getMode();
            this.action = c0EPacketClickWindow.getActionNumber();
            this.itemStack = ItemStack.of(c0EPacketClickWindow.getClickedItem());
        }
    }

    public static class C0D
    extends CPacket {
        public int windowId;

        public C0D(int n) {
            super((Packet)C0D.create(n));
        }

        private static C0DPacketCloseWindow create(int n) {
            return new C0DPacketCloseWindow(n);
        }

        public C0D(byte by, C0DPacketCloseWindow c0DPacketCloseWindow) {
            super((Packet)c0DPacketCloseWindow);
            try {
                this.windowId = ScriptReflection.WINDOW_ID.get().getInt(c0DPacketCloseWindow);
            }
            catch (Exception exception) {
            }
        }
    }

    public static class S29
    extends SPacket {
        public float volume;
        public Vec3 position;
        public String sound;
        public float pitch;

        public S29(S29PacketSoundEffect s29PacketSoundEffect) {
            super((Packet)s29PacketSoundEffect);
            this.sound = s29PacketSoundEffect.getSoundName();
            this.position = new Vec3(s29PacketSoundEffect.getX(), s29PacketSoundEffect.getY(), s29PacketSoundEffect.getZ());
            this.volume = s29PacketSoundEffect.getVolume();
            this.pitch = s29PacketSoundEffect.getPitch();
        }
    }

    public static class C08
    extends CPacket {
        public Vec3 position;
        public Vec3 offset;
        public int direction;
        public ItemStack itemStack;

        private static C08PacketPlayerBlockPlacement create(ItemStack itemStack, Vec3 vec3, int n, Vec3 vec32) {
            return new C08PacketPlayerBlockPlacement(vec3 == null ? null : Vec3.getBlockPos(vec3), n, itemStack == null ? null : itemStack.itemStack, (float)vec32.x, (float)vec32.y, (float)vec32.z);
        }

        public C08(byte by, C08PacketPlayerBlockPlacement c08PacketPlayerBlockPlacement) {
            super((Packet)c08PacketPlayerBlockPlacement);
            this.itemStack = ItemStack.of(c08PacketPlayerBlockPlacement.getStack());
            this.position = new Vec3(c08PacketPlayerBlockPlacement.getPosition());
            this.offset = new Vec3(c08PacketPlayerBlockPlacement.getPlacedBlockOffsetX(), c08PacketPlayerBlockPlacement.getPlacedBlockOffsetY(), c08PacketPlayerBlockPlacement.getPlacedBlockOffsetZ());
            this.direction = c08PacketPlayerBlockPlacement.getPlacedBlockDirection();
        }

        public C08(ItemStack itemStack, Vec3 vec3, int n, Vec3 vec32) {
            super((Packet)C08.create(itemStack, vec3, n, vec32));
        }

        public C08(ItemStack itemStack) {
            super((Packet)new C08PacketPlayerBlockPlacement(itemStack == null ? null : itemStack.itemStack));
        }
    }

    public static class NetworkPlayer {
        private NetworkPlayerInfo playerInfo;

        public String getCape() {
            return this.playerInfo.getLocationCape().getResourcePath();
        }

        public int getPing() {
            return this.playerInfo.getResponseTime();
        }

        public String getSkinData() {
            Property property = (Property)Iterables.getFirst((Iterable)this.playerInfo.getGameProfile().getProperties().get("textures"), null);
            if (property == null) {
                return null;
            }
            return new String(Base64.getDecoder().decode(property.getValue().getBytes(StandardCharsets.UTF_8)));
        }

        public String getDisplayName() {
            return this.playerInfo.getGameProfile() == Defaults.mc.thePlayer.getGameProfile() ? Defaults.mc.thePlayer.getDisplayName().getUnformattedText() : ScorePlayerTeam.formatPlayerName((Team)this.playerInfo.getPlayerTeam(), (String)this.getName());
        }

        public String getName() {
            return this.playerInfo.getGameProfile().getName();
        }

        protected static NetworkPlayer of(NetworkPlayerInfo networkPlayerInfo) {
            if (networkPlayerInfo == null) {
                return null;
            }
            return new NetworkPlayer(networkPlayerInfo);
        }

        public String toString() {
            return "NetworkPlayer(" + this.getName() + "," + this.getUUID() + ")";
        }

        public String getUUID() {
            return this.playerInfo.getGameProfile().getId().toString();
        }

        public NetworkPlayer(NetworkPlayerInfo networkPlayerInfo) {
            this.playerInfo = networkPlayerInfo;
        }
    }

    public static class util {
        public static String colorSymbol = "\u00a7";

        public static double round(double d, int n) {
            return ScriptUtil.round(d, n);
        }

        public static String encodeb64(String string) {
            try {
                return Base64.getEncoder().encodeToString(string.getBytes("UTF-8"));
            }
            catch (Exception exception) {
                return "";
            }
        }

        public static double randomDouble(double d, double d2) {
            return ScriptUtil.randomDouble(d, d2);
        }

        public static String decodeb64(String string) {
            try {
                byte[] byArray = Base64.getDecoder().decode(string);
                return new String(byArray, "UTF-8");
            }
            catch (Exception exception) {
                return "";
            }
        }

        public static String strip(String string) {
            return ScriptBridge.stripString(string);
        }

        public static int randomInt(int n, int n2) {
            return ScriptUtil.randomInt(n, n2);
        }

        public static String color(String string) {
            return string.replace("&", colorSymbol);
        }
    }

    public static class S23
    extends SPacket {
        public Block block;
        public Vec3 position;

        public S23(S23PacketBlockChange s23PacketBlockChange) {
            super((Packet)s23PacketBlockChange);
            this.position = new Vec3(s23PacketBlockChange.getBlockPosition());
            this.block = new Block(s23PacketBlockChange.getBlockState());
        }
    }

    public static class C01
    extends CPacket {
        public String message;

        private static C01PacketChatMessage create(String string) {
            return new C01PacketChatMessage(string);
        }

        public C01(byte by, C01PacketChatMessage c01PacketChatMessage) {
            super((Packet)c01PacketChatMessage);
            this.message = c01PacketChatMessage.getMessage();
        }

        public C01(String string) {
            super((Packet)C01.create(string));
        }
    }

    public static class Image {
        BufferedImage bufferedImage;
        int width;
        private static HashMap<String, BufferedImage> imageCache;
        int textureId = -1;
        String url;
        int height;
        boolean cached;
        private static int f116867;
        private static int f116868;
        private static int f116869;
        private static int f116870;
        private static int f116871;
        private static int f116872;
        private static int f116873;
        private static int f116874;
        private static int f116875;
        private static int f116876;
        private static int f116877;
        private static int f116878;

        public String toString() {
            return "Image(" + this.height + "," + this.width + "," + this.url + ")";
        }

        public float[] getDimensions() {
            int n = new ScaledResolution(Defaults.mc).getScaleFactor();
            return new float[]{(float)this.width / (float)n, (float)this.height / (float)n};
        }

        public boolean isLoaded() {
            return this.bufferedImage != null;
        }

        private void lambda_new_0(String string, boolean bl) {
            BufferedImage bufferedImage = null;
            if (bufferedImage != null) {
                this.bufferedImage = bufferedImage;
                this.height = bufferedImage.getHeight();
                this.width = bufferedImage.getWidth();
                if (bl) {
                    imageCache.put(string, bufferedImage);
                }
            }
        }

        static {
            f116867 = Integer.reverse(-953951931);
            f116868 = (-567866092 >>> 107 | -567866092 << ~107 + 1) & 0xFFFFFFFF;
            f116869 = (-470913097 >>> 134 | -470913097 << ~134 + 1) & 0xFFFFFFFF;
            f116870 = -1209799057 >>> 94 | -1209799057 << ~94 + 1;
            f116871 = 497208510 >>> 73 | 497208510 << -73;
            f116872 = Integer.reverse(-427069190);
            f116873 = 612946960 >>> 126 | 612946960 << ~126 + 1;
            f116874 = Integer.reverse(-2113223607);
            f116875 = Integer.reverse(-332520627);
            f116876 = (2066499395 >>> 60 | 2066499395 << -60) & 0xFFFFFFFF;
            f116877 = -916923460 >>> 62 | -916923460 << -62;
            f116878 = Integer.reverse(814299483);
            imageCache = new HashMap();
        }

        public Image(String string, boolean bl) {
            BufferedImage bufferedImage;
            this.url = string;
            this.cached = bl;
            BufferedImage bufferedImage2 = bufferedImage = bl ? imageCache.get(string) : null;
            if (bufferedImage == null) {
                client.async(() -> this.lambda_new_0(string, bl));
            } else {
                this.bufferedImage = bufferedImage;
                this.height = bufferedImage.getHeight();
                this.width = bufferedImage.getWidth();
            }
        }
    }

    public static class gl {
        private static int f116817 = (-1130693163 >>> 192 | -1130693163 << ~192 + 1) & 0xFFFFFFFF;
        private static int f116818 = -911221429 >>> 4 | -911221429 << ~4 + 1;
        private static int f116819 = Integer.reverse(-1311862102);
        private static int f116820 = Integer.reverse(-1311862102);
        private static int f116821 = 1842072627 >>> 205 | 1842072627 << ~205 + 1;
        private static int f116822 = (1727761880 >>> 198 | 1727761880 << -198) & 0xFFFFFFFF;
        private static int f116823 = -552620350 >>> 131 | -552620350 << -131;
        private static int f116824 = (-1207206850 >>> 21 | -1207206850 << -21) & 0xFFFFFFFF;
        private static int f116825 = -22899289 >>> 110 | -22899289 << ~110 + 1;
        private static int f116826 = Integer.reverse(-1369441942);
        private static float f116827 = Float.intBitsToFloat(Integer.reverse(65218));
        private static int f116828 = (1535917684 >>> 116 | 1535917684 << ~116 + 1) & 0xFFFFFFFF;
        private static int f116829 = 523906374 >>> 67 | 523906374 << ~67 + 1;
        private static int f116830 = Integer.reverse(1154968307);
        private static int f116831 = Integer.reverse(-1143510285);
        private static float f116832 = Float.intBitsToFloat((-134217189 >>> 171 | -134217189 << ~171 + 1) & 0xFFFFFFFF);
        private static int f116833 = 1141773464 >>> 131 | 1141773464 << ~131 + 1;
        private static int f116834 = Integer.reverse(-649887472);
        private static int f116835 = Integer.reverse(-1911616057);
        private static int f116836 = (2017948120 >>> 111 | 2017948120 << ~111 + 1) & 0xFFFFFFFF;
        private static float f116837 = Float.intBitsToFloat(Integer.reverse(65218));
        private static int f116838 = -288936512 >>> 203 | -288936512 << ~203 + 1;
        private static int f116839 = Integer.reverse(1478211613);
        private static float f116840 = Float.intBitsToFloat(552928 >>> 245 | 552928 << -245);
        private static float f116841 = Float.intBitsToFloat(Integer.reverse(65218));
        private static float f116842 = Float.intBitsToFloat((-2147475009 >>> 79 | -2147475009 << -79) & 0xFFFFFFFF);
        private static float f116843 = Float.intBitsToFloat(-67108595 >>> 170 | -67108595 << ~170 + 1);
        private static float f116844 = Float.intBitsToFloat(35387392 >>> 59 | 35387392 << ~59 + 1);
        private static int f116845 = (-1221767498 >>> 74 | -1221767498 << ~74 + 1) & 0xFFFFFFFF;
        private static int f116846 = Integer.reverse(651343285);
        private static int f116847 = (1163977121 >>> 111 | 1163977121 << ~111 + 1) & 0xFFFFFFFF;
        private static int f116848 = (-154975734 >>> 56 | -154975734 << -56) & 0xFFFFFFFF;
        private static int f116849 = Integer.reverse(156975835);
        private static int f116850 = (-1327807791 >>> 216 | -1327807791 << -216) & 0xFFFFFFFF;
        private static int f116851 = Integer.reverse(-1548555322);
        private static int f116852 = Integer.reverse(-1486689338);
        private static int f116853 = Integer.reverse(1996519555);
        private static int f116854 = Integer.reverse(-256870269);
        private static int f116855 = Integer.reverse(-1597008189);
        private static int f116856 = Integer.reverse(-535849277);
        private static int f116857 = -1661376537 >>> 17 | -1661376537 << ~17 + 1;
        private static int f116858 = Integer.reverse(-21770291);
        private static int f116859 = 946867005 >>> 196 | 946867005 << -196;
        private static int f116860 = Integer.reverse(1291903435);
        private static int f116861 = Integer.reverse(1800528399);
        private static int f116862 = Integer.reverse(1800528399);
        private static int f116863 = (-1822965586 >>> 81 | -1822965586 << -81) & 0xFFFFFFFF;
        private static int f116864 = Integer.reverse(1570957831);
        private static int f116865 = (-1400963903 >>> 121 | -1400963903 << -121) & 0xFFFFFFFF;
        private static int f116866 = Integer.reverse(-482967812);

        public static boolean bindTexture(String string) {
            try {
                Defaults.mc.getTextureManager().bindTexture(new ResourceLocation(string));
                return true;
            }
            catch (Exception exception) {
                return false;
            }
        }

        public static void polygonOffset(float f, float f2) {
            GL11.glPolygonOffset((float)f, (float)f2);
        }

        public static void pop() {
            GlStateManager.popMatrix();
        }

        public static void rotate(double d, double d2, double d3, double d4) {
            GlStateManager.rotate((float)((float)d), (float)((float)d2), (float)((float)d3), (float)((float)d4));
        }

        public static void enableItemLighting(boolean bl) {
            if (bl) {
                RenderHelper.enableGUIStandardItemLighting();
            } else {
                RenderHelper.enableStandardItemLighting();
            }
        }

        public static void scissor(int n, int n2, int n3, int n4) {
            GL11.glScissor((int)n, (int)n2, (int)n3, (int)n4);
        }

        public static void resetColor() {
            gl.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public static void texCoord2(float f, float f2) {
            GL11.glTexCoord2f((float)f, (float)f2);
        }

        public static void rotate(float f, float f2, float f3, float f4) {
            GlStateManager.rotate((float)f, (float)f2, (float)f3, (float)f4);
        }

        public static void push() {
            GlStateManager.pushMatrix();
        }

        public static void translate(float f, float f2, float f3) {
            GlStateManager.translate((float)f, (float)f2, (float)f3);
        }

        public static void texture2d(boolean bl) {
            if (bl) {
                GlStateManager.enableTexture2D();
            } else {
                GlStateManager.disableTexture2D();
            }
        }

        public static void normal(double d, double d2, double d3) {
            GL11.glNormal3d((double)d, (double)d2, (double)d3);
        }

        public static void disableItemLighting() {
            RenderHelper.disableStandardItemLighting();
        }

        public static int getBoundTexture() {
            return GL11.glGetInteger((int)32873);
        }

        public static void color(int n) {
            float f = (float)(n >> 24 & 0xFF) / 255.0f;
            float f2 = (float)(n >> 16 & 0xFF) / 255.0f;
            float f3 = (float)(n >> 8 & 0xFF) / 255.0f;
            float f4 = (float)(n & 0xFF) / 255.0f;
            gl.color(f2, f3, f4, f);
        }

        public static void begin(int n) {
            GL11.glBegin((int)n);
        }

        public static void end() {
            GL11.glEnd();
        }

        public static void color(int n, int n2, int n3, int n4) {
            GlStateManager.color((float)((float)n / 255.0f), (float)((float)n2 / 255.0f), (float)((float)n3 / 255.0f), (float)((float)n4 / 255.0f));
        }

        public static void scale(double d, double d2, double d3) {
            GlStateManager.scale((double)d, (double)d2, (double)d3);
        }

        public static void vertex3(double d, double d2, double d3) {
            GL11.glVertex3d((double)d, (double)d2, (double)d3);
        }

        public static void polygonOffset(boolean bl) {
            if (bl) {
                GL11.glEnable((int)32823);
            } else {
                GL11.glDisable((int)32823);
            }
        }

        public static void alpha(boolean bl) {
            if (bl) {
                GlStateManager.enableAlpha();
            } else {
                GlStateManager.disableAlpha();
            }
        }

        public static void color(float f, float f2, float f3, float f4) {
            GlStateManager.color((float)f, (float)f2, (float)f3, (float)f4);
        }

        public static void scale(float f, float f2, float f3) {
            GlStateManager.scale((float)f, (float)f2, (float)f3);
        }

        public static void vertex2(double d, double d2) {
            GL11.glVertex2d((double)d, (double)d2);
        }

        public static void lineSmooth(boolean bl) {
            if (bl) {
                gl.enable(2848);
            } else {
                gl.disable(2848);
            }
        }

        public static void lineWidth(double d) {
            GL11.glLineWidth((float)((float)d));
        }

        public static void depthMask(boolean bl) {
            GlStateManager.depthMask((boolean)bl);
        }

        public static void normal(float f, float f2, float f3) {
            GL11.glNormal3f((float)f, (float)f2, (float)f3);
        }

        public static void depthFunc(int n) {
            GL11.glDepthFunc((int)n);
        }

        public static void bindTextureId(int n) {
            GL11.glBindTexture((int)3553, (int)n);
        }

        public static void vertex2(float f, float f2) {
            GL11.glVertex2f((float)f, (float)f2);
        }

        public static void enable(int n) {
            GL11.glEnable((int)n);
        }

        public static void translate(double d, double d2, double d3) {
            GlStateManager.translate((double)d, (double)d2, (double)d3);
        }

        public static void texCoord2(double d, double d2) {
            GL11.glTexCoord2d((double)d, (double)d2);
        }

        public static void vertex3(float f, float f2, float f3) {
            GL11.glVertex3f((float)f, (float)f2, (float)f3);
        }

        public static void disable(int n) {
            GL11.glDisable((int)n);
        }

        public static void depth(boolean bl) {
            if (bl) {
                GlStateManager.enableDepth();
            } else {
                GlStateManager.disableDepth();
            }
        }

        public static void lighting(boolean bl) {
            if (bl) {
                GlStateManager.enableLighting();
            } else {
                GlStateManager.disableLighting();
            }
        }

        public static void cull(boolean bl) {
            if (bl) {
                GlStateManager.enableCull();
            } else {
                GlStateManager.disableCull();
            }
        }

        public static void blend(boolean bl) {
            if (bl) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate((int)770, (int)771, (int)1, (int)0);
            } else {
                GlStateManager.disableBlend();
            }
        }

        public static void scissor(boolean bl) {
            if (bl) {
                GL11.glEnable((int)3089);
            } else {
                GL11.glDisable((int)3089);
            }
        }

        public static void lineWidth(float f) {
            GL11.glLineWidth((float)f);
        }

        public static void colorMask(boolean bl, boolean bl2, boolean bl3, boolean bl4) {
            GL11.glColorMask((boolean)bl, (boolean)bl2, (boolean)bl3, (boolean)bl4);
        }

        public static void color(double d, double d2, double d3, double d4) {
            GlStateManager.color((float)((float)d), (float)((float)d2), (float)((float)d3), (float)((float)d4));
        }
    }

    public static class world {
        private static int f117365 = 223297891 >>> 46 | 223297891 << ~46 + 1;
        private static int f117366 = Integer.reverse(1017917856);
        private static int f117367 = 182844392 >>> 189 | 182844392 << -189;
        private static int f117368 = Integer.reverse(50066666);
        private static int f117369 = 2097935614 >>> 216 | 2097935614 << ~216 + 1;
        private static int f117370 = Integer.reverse(1098960943);
        private static int f117371 = 13729912 >>> 57 | 13729912 << -57;
        private static int f117372 = -1006659777 >>> 208 | -1006659777 << -208;
        private static int f117373 = Integer.reverse(1664335211);
        private static int f117374 = Integer.reverse(-1664335212);
        private static int f117375 = Integer.reverse(1072020005);
        private static int f117376 = Integer.reverse(-1072020006);
        private static int f117377 = Integer.reverse(-2065436560);
        private static int f117378 = -2117666929 >>> 11 | -2117666929 << -11;
        private static int f117379 = -102863678 >>> 222 | -102863678 << ~222 + 1;
        private static int f117380 = Integer.reverse(791830808);
        private static int f117381 = (-332308061 >>> 77 | -332308061 << -77) & 0xFFFFFFFF;
        private static int f117382 = (-526486286 >>> 200 | -526486286 << -200) & 0xFFFFFFFF;
        private static int f117383 = (1108823495 >>> 145 | 1108823495 << ~145 + 1) & 0xFFFFFFFF;
        private static int f117384 = Integer.reverse(796539034);
        private static int f117385 = Integer.reverse(-1348419422);
        private static int f117386 = (1440212944 >>> 219 | 1440212944 << -219) & 0xFFFFFFFF;
        private static int f117387 = (-1921275512 >>> 249 | -1921275512 << ~249 + 1) & 0xFFFFFFFF;
        private static int f117388 = -582204293 >>> 36 | -582204293 << ~36 + 1;
        private static int f117389 = -859491328 >>> 222 | -859491328 << -222;
        private static int f117390 = 1291858737 >>> 12 | 1291858737 << ~12 + 1;

        public static boolean isValidEntity(Entity entity) {
            return (world.exists() && entity != null && Defaults.mc.theWorld.getEntityByID(entity.entityId) != null ? 1 : 0) != 0;
        }

        public static String getSubtitleText() {
            try {
                String string;
                int n = (Integer)ScriptReflection.TITLE_TIMER.get().get(Defaults.mc.ingameGUI);
                if (n > 0 && (string = (String)ScriptReflection.TITLE.get().get(Defaults.mc.ingameGUI)) != null) {
                    return string;
                }
            }
            catch (Exception exception) {
            }
            return "";
        }

        public static String getMode() {
            return ScriptBridge.getMode();
        }

        public static Map<String, List<String>> getTeams() {
            Scoreboard scoreboard = Defaults.mc.theWorld.getScoreboard();
            if (scoreboard == null) {
                return null;
            }
            HashMap<String, List<String>> hashMap = new HashMap<String, List<String>>();
            for (ScorePlayerTeam scorePlayerTeam : scoreboard.getTeams()) {
                Collection collection = scorePlayerTeam.getMembershipCollection();
                hashMap.put(scorePlayerTeam.getTeamName(), new ArrayList(collection));
            }
            return hashMap;
        }

        public static List<Entity> getEntities() {
            ArrayList<Entity> arrayList = new ArrayList<Entity>();
            for (net.minecraft.entity.Entity entity : Defaults.mc.theWorld.loadedEntityList) {
                arrayList.add(Entity.of(entity));
            }
            return arrayList;
        }

        public static List<TileEntity> getTileEntities() {
            ArrayList<TileEntity> arrayList = new ArrayList<TileEntity>();
            for (net.minecraft.tileentity.TileEntity tileEntity : Defaults.mc.theWorld.loadedTileEntityList) {
                arrayList.add(TileEntity.of(tileEntity));
            }
            return arrayList;
        }

        public static Block getBlockAt(Vec3 vec3) {
            return world.getBlockAt(MathHelper.floor_double((double)vec3.x), MathHelper.floor_double((double)vec3.y), MathHelper.floor_double((double)vec3.z));
        }

        public static void clearTitleText() {
            Defaults.mc.ingameGUI.displayTitle(null, null, -1, -1, -1);
        }

        public static String getTabFooter() {
            try {
                Object object = ScriptReflection.HEADER.get().get(Defaults.mc.ingameGUI.getTabList());
                if (object != null) {
                    return ((IChatComponent)object).getUnformattedText();
                }
            }
            catch (Exception exception) {
            }
            return "";
        }

        public static Block getBlockAt(int n, int n2, int n3) {
            return new Block(n, n2, n3);
        }

        public static List<Entity> getPlayerEntities() {
            ArrayList<Entity> arrayList = new ArrayList<Entity>();
            for (EntityPlayer entityPlayer : Defaults.mc.theWorld.playerEntities) {
                arrayList.add(Entity.of((net.minecraft.entity.Entity)entityPlayer));
            }
            return arrayList;
        }

        public static List<String> getScoreboard() {
            return ScriptBridge.getSidebarLines();
        }

        public static void displayTitleText(String string, String string2, int n, int n2, int n3) {
            Defaults.mc.ingameGUI.displayTitle(null, null, n, n2, n3);
            if (string2 != null && !string2.isEmpty()) {
                Defaults.mc.ingameGUI.displayTitle(null, util.color(string2), -1, -1, -1);
            }
            if (string != null && !string.isEmpty()) {
                Defaults.mc.ingameGUI.displayTitle(util.color(string), null, -1, -1, -1);
            }
        }

        public static Entity getEntityById(int n) {
            return world.exists() ? Entity.of(Defaults.mc.theWorld.getEntityByID(n)) : null;
        }

        public static boolean exists() {
            return Defaults.mc.theWorld != null;
        }

        public static String getTitleText() {
            try {
                String string;
                int n = (Integer)ScriptReflection.TITLE_TIMER.get().get(Defaults.mc.ingameGUI);
                if (n > 0 && (string = (String)ScriptReflection.SUBTITLE.get().get(Defaults.mc.ingameGUI)) != null) {
                    return string;
                }
            }
            catch (Exception exception) {
            }
            return "";
        }

        public static List<NetworkPlayer> getNetworkPlayers() {
            ArrayList<NetworkPlayer> arrayList = new ArrayList<NetworkPlayer>();
            ArrayList<NetworkPlayerInfo> arrayList2 = new ArrayList<NetworkPlayerInfo>(Defaults.mc.getNetHandler().getPlayerInfoMap());
            ScriptUtil.dedupe(arrayList2);
            for (NetworkPlayerInfo networkPlayerInfo : arrayList2) {
                arrayList.add(NetworkPlayer.of(networkPlayerInfo));
            }
            return arrayList;
        }

        public static String getTabHeader() {
            try {
                Object object = ScriptReflection.FOOTER.get().get(Defaults.mc.ingameGUI.getTabList());
                if (object != null) {
                    return ((IChatComponent)object).getUnformattedText();
                }
            }
            catch (Exception exception) {
            }
            return "";
        }

        public static String getDimension() {
            return world.exists() ? Defaults.mc.theWorld.provider.getDimensionName() : null;
        }
    }

    public static class keybinds {
        private static HashMap<String, KeyBinding> keyMap;
        private static int f116903;
        private static int f116904;
        private static int f116905;
        private static int f116906;
        private static int f116907;
        private static int f116908;
        private static int f116909;
        private static int f116910;
        private static int f116911;
        private static int f116912;

        public static boolean isMouseDown(int n) {
            return Mouse.isButtonDown((int)n);
        }

        public static boolean isPressed(String string) {
            KeyBinding keyBinding = keyMap.get(string);
            return keyBinding != null ? (boolean)keyBinding.isKeyDown() : false;
        }

        public static boolean isKeyDown(int n) {
            return Keyboard.isKeyDown((int)n);
        }

        public static int getKeycode(String string) {
            KeyBinding keyBinding = keyMap.get(string);
            return keyBinding != null ? keyBinding.getKeyCode() : -1;
        }

        public static void leftClick() {
            ScriptBridge.leftClick();
        }

        public static void rightClick() {
            ScriptBridge.rightClick();
        }

        public static int[] getMousePosition() {
            return new int[]{Mouse.getX(), Mouse.getY()};
        }

        public static void setPressed(String string, boolean bl) {
            KeyBinding keyBinding = keyMap.get(string);
            if (keyBinding != null) {
                KeyBinding.setKeyBindState((int)keyBinding.getKeyCode(), (boolean)bl);
                if (bl) {
                    KeyBinding.onTick((int)keyBinding.getKeyCode());
                }
            }
        }

        static {
            f116903 = (692592237 >>> 36 | 692592237 << -36) & 0xFFFFFFFF;
            f116904 = Integer.reverse(1736517963);
            f116905 = 229757351 >>> 98 | 229757351 << -98;
            f116906 = Integer.reverse(1775356220);
            f116907 = Integer.reverse(1265022933);
            f116908 = (-1410505008 >>> 64 | -1410505008 << -64) & 0xFFFFFFFF;
            f116909 = 579336586 >>> 128 | 579336586 << ~128 + 1;
            f116910 = Integer.reverse(1369432388);
            f116911 = Integer.reverse(338161861);
            f116912 = (-1559239639 >>> 128 | -1559239639 << ~128 + 1) & 0xFFFFFFFF;
            keyMap = new HashMap();
        }

        static HashMap access$000() {
            return keyMap;
        }
    }

    public static class S09
    extends SPacket {
        public int slot;

        public S09(S09PacketHeldItemChange s09PacketHeldItemChange) {
            super((Packet)s09PacketHeldItemChange);
            this.slot = s09PacketHeldItemChange.getHeldItemHotbarIndex();
        }
    }

    public static class C13
    extends CPacket {
        public boolean creativeMode;
        public float flySpeed;
        public boolean flying;
        public boolean invulnerable;
        public float walkSpeed;
        public boolean allowFlying;

        public C13(boolean bl, boolean bl2, boolean bl3, boolean bl4, float f, float f2) {
            super((Packet)C13.create(bl, bl2, bl3, bl4, f, f2));
        }

        public C13(byte by, C13PacketPlayerAbilities c13PacketPlayerAbilities) {
            super((Packet)c13PacketPlayerAbilities);
            this.invulnerable = c13PacketPlayerAbilities.isInvulnerable();
            this.flying = c13PacketPlayerAbilities.isFlying();
            this.allowFlying = c13PacketPlayerAbilities.isAllowFlying();
            this.creativeMode = c13PacketPlayerAbilities.isCreativeMode();
            try {
                this.flySpeed = ScriptReflection.FLY_SPEED.get().getFloat(c13PacketPlayerAbilities);
                this.walkSpeed = ScriptReflection.WALK_SPEED.get().getFloat(c13PacketPlayerAbilities);
            }
            catch (Exception exception) {
            }
        }

        private static C13PacketPlayerAbilities create(boolean bl, boolean bl2, boolean bl3, boolean bl4, float f, float f2) {
            PlayerCapabilities playerCapabilities = new PlayerCapabilities();
            playerCapabilities.disableDamage = bl;
            playerCapabilities.isFlying = bl2;
            playerCapabilities.allowFlying = bl3;
            playerCapabilities.isCreativeMode = bl4;
            playerCapabilities.setFlySpeed(f);
            playerCapabilities.setPlayerWalkSpeed(f2);
            return new C13PacketPlayerAbilities(playerCapabilities);
        }
    }

    public static class Vec3 {
        public double z;
        public double y;
        public double x;
        private static int f117349 = 1452261042 >>> 235 | 1452261042 << -235;
        private static int f117350 = Integer.reverse(1871401579);
        private static int f117351 = 2067903541 >>> 73 | 2067903541 << -73;
        private static int f117352 = (914796392 >>> 214 | 914796392 << ~214 + 1) & 0xFFFFFFFF;
        private static int f117353 = (-1554512241 >>> 123 | -1554512241 << -123) & 0xFFFFFFFF;
        private static int f117354 = Integer.reverse(-1349844778);
        private static int f117355 = -1430057388 >>> 214 | -1430057388 << -214;
        private static int f117356 = 1434852521 >>> 55 | 1434852521 << ~55 + 1;
        private static int f117357 = Integer.reverse(-491689772);
        private static int f117358 = Integer.reverse(582052052);
        private static int f117359 = Integer.reverse(930342658);
        private static int f117360 = Integer.reverse(930342658);
        private static int f117361 = 663678867 >>> 152 | 663678867 << -152;
        private static int f117362 = -1707360692 >>> 218 | -1707360692 << -218;
        private static int f117363 = (1414628593 >>> 105 | 1414628593 << -105) & 0xFFFFFFFF;
        private static int f117364 = (-1945662814 >>> 244 | -1945662814 << -244) & 0xFFFFFFFF;

        public Vec3 translate(Vec3 vec3) {
            return this.translate(vec3.x, vec3.y, vec3.z);
        }

        protected Vec3(net.minecraft.util.Vec3 vec3) {
            this(vec3.xCoord, vec3.yCoord, vec3.zCoord);
        }

        public double distanceTo(Vec3 vec3) {
            return MathHelper.sqrt_double((double)this.distanceToSq(vec3));
        }

        public Vec3 floor() {
            return new Vec3(MathHelper.floor_double((double)this.x), MathHelper.floor_double((double)this.y), MathHelper.floor_double((double)this.z));
        }

        public Vec3(double d, double d2, double d3) {
            this.x = d;
            this.y = d2;
            this.z = d3;
        }

        public Vec3 ceil() {
            return new Vec3(MathHelper.ceiling_double_int((double)this.x), MathHelper.ceiling_double_int((double)this.y), MathHelper.ceiling_double_int((double)this.z));
        }

        public Vec3 offset(Vec3 vec3) {
            return this.offset(vec3.x, vec3.y, vec3.z);
        }

        public String toString() {
            return "Vec3(" + this.x + "," + this.y + "," + this.z + ")";
        }

        public double distanceToSq(Vec3 vec3) {
            double d = this.x - vec3.x;
            double d2 = this.y - vec3.y;
            double d3 = this.z - vec3.z;
            return d * d + d2 * d2 + d3 * d3;
        }

        public Vec3() {
        }

        protected Vec3(BlockPos blockPos) {
            this(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }

        protected static BlockPos getBlockPos(Vec3 vec3) {
            return new BlockPos(vec3.x, vec3.y, vec3.z);
        }

        public Vec3 translate(double d, double d2, double d3) {
            this.x += d;
            this.y += d2;
            this.z += d3;
            return this;
        }

        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object == null || object.getClass() != Vec3.class) {
                return false;
            }
            Vec3 vec3 = (Vec3)object;
            return (vec3.x == this.x && vec3.y == this.y && vec3.z == this.z ? 1 : 0) != 0;
        }

        public Vec3 offset(double d, double d2, double d3) {
            return new Vec3(this.x + d, this.y + d2, this.z + d3);
        }

        public int hashCode() {
            return Objects.hash(this.x, this.y, this.z);
        }

        protected static net.minecraft.util.Vec3 getVec3(Vec3 vec3) {
            return new net.minecraft.util.Vec3(vec3.x, vec3.y, vec3.z);
        }

        public Vec3 inverse() {
            return new Vec3(-this.x, -this.y, -this.z);
        }
    }

    public static class TileEntity {
        public String name;
        public String type;
        private net.minecraft.tileentity.TileEntity tileEntity;
        private static int f117335 = (-1784586246 >>> 111 | -1784586246 << -111) & 0xFFFFFFFF;
        private static int f117336 = (-772801206 >>> 86 | -772801206 << ~86 + 1) & 0xFFFFFFFF;
        private static int f117337 = (414965323 >>> 171 | 414965323 << -171) & 0xFFFFFFFF;
        private static int f117338 = Integer.reverse(-555170157);
        private static int f117339 = 1779840268 >>> 221 | 1779840268 << -221;
        private static int f117340 = (-1809028584 >>> 158 | -1809028584 << -158) & 0xFFFFFFFF;
        private static int f117341 = -1897023814 >>> 204 | -1897023814 << -204;
        private static int f117342 = Integer.reverse(-1686694442);
        private static int f117343 = (-1352018595 >>> 129 | -1352018595 << -129) & 0xFFFFFFFF;
        private static int f117344 = Integer.reverse(-1252577813);
        private static int f117345 = Integer.reverse(-2016322227);
        private static int f117346 = Integer.reverse(-942580403);
        private static int f117347 = -145850489 >>> 111 | -145850489 << -111;
        private static int f117348 = -1006917633 >>> 198 | -1006917633 << ~198 + 1;

        public Object[] getSkullData() {
            if (this.tileEntity instanceof TileEntitySkull) {
                TileEntitySkull tileEntitySkull = (TileEntitySkull)this.tileEntity;
                Object[] objectArray = new Object[5];
                objectArray[0] = tileEntitySkull.getSkullType();
                objectArray[1] = tileEntitySkull.getSkullRotation();
                if (tileEntitySkull.getPlayerProfile() == null) {
                    objectArray[3] = null;
                    objectArray[2] = null;
                } else {
                    objectArray[2] = tileEntitySkull.getPlayerProfile().getName();
                    objectArray[3] = tileEntitySkull.getPlayerProfile().getId();
                }
                return objectArray;
            }
            return null;
        }

        protected static TileEntity of(net.minecraft.tileentity.TileEntity tileEntity) {
            if (tileEntity == null) {
                return null;
            }
            return new TileEntity(tileEntity);
        }

        protected TileEntity(net.minecraft.tileentity.TileEntity tileEntity) {
            this.tileEntity = tileEntity;
            this.type = tileEntity.getBlockType().getClass().getSimpleName();
            this.name = ScriptUtil.stripNamespace(tileEntity.getBlockType().getRegistryName());
        }

        public String toString() {
            return "TileEntity(" + this.type + "," + this.name + ")";
        }

        public Vec3 getPosition() {
            BlockPos blockPos = this.tileEntity.getPos();
            return new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }

    public static class ItemStack {
        public int maxStackSize;
        protected net.minecraft.item.ItemStack itemStack;
        public int durability;
        public boolean isBlock;
        public String name;
        public int meta;
        public int maxDurability;
        public String displayName;
        public String type;
        public int stackSize;
        private static int f116887 = Integer.reverse(630267996);
        private static int f116888 = -1904139671 >>> 62 | -1904139671 << ~62 + 1;
        private static int f116889 = Integer.reverse(-268023337);
        private static int f116890 = Integer.reverse(-268023337);
        private static int f116891 = (-1006493708 >>> 74 | -1006493708 << ~74 + 1) & 0xFFFFFFFF;
        private static int f116892 = 822118141 >>> 8 | 822118141 << ~8 + 1;
        private static int f116893 = Integer.reverse(-1931801695);
        private static int f116894 = Integer.reverse(215681953);

        public ItemStack(String string) {
            this((byte) 0, new net.minecraft.item.ItemStack((Item)Item.itemRegistry.getObject(new ResourceLocation("minecraft:" + string))));
        }

        protected ItemStack(byte by, net.minecraft.item.ItemStack itemStack) {
            if (itemStack == null) {
                return;
            }
            this.itemStack = itemStack;
            Item item = itemStack.getItem();
            this.type = item.getClass().getSimpleName();
            this.name = ScriptUtil.stripNamespace(item.getRegistryName());
            this.meta = itemStack.getMetadata();
            this.displayName = itemStack.getDisplayName();
            this.stackSize = itemStack.stackSize;
            this.maxStackSize = itemStack.getMaxStackSize();
            this.maxDurability = itemStack.getMaxDamage();
            this.durability = this.maxDurability - itemStack.getItemDamage();
            this.isBlock = item instanceof ItemBlock;
        }

        public List<Object[]> getEnchantments() {
            Map<Integer, Integer> map = EnchantmentHelper.getEnchantments((net.minecraft.item.ItemStack)this.itemStack);
            if (map.isEmpty()) {
                return null;
            }
            ArrayList<Object[]> arrayList = new ArrayList<Object[]>();
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                Enchantment enchantment = Enchantment.getEnchantmentById((int)((Integer)entry.getKey()));
                String string = StatCollector.translateToFallback((String)enchantment.getName()).toLowerCase().replace(" ", "_");
                arrayList.add(new Object[]{string, entry.getValue()});
            }
            return arrayList;
        }

        protected static ItemStack of(net.minecraft.item.ItemStack itemStack) {
            if (itemStack == null) {
                return null;
            }
            return new ItemStack((byte) 0, itemStack);
        }

        public List<String> getTooltip() {
            return this.itemStack.getTooltip((EntityPlayer)Defaults.mc.thePlayer, true);
        }

        public String toString() {
            return "ItemStack(" + this.type + "," + this.name + ")";
        }
    }

    public static class C16
    extends CPacket {
        public String status;

        private static C16PacketClientStatus create(String string) {
            return new C16PacketClientStatus(ScriptUtil.enumByName(C16PacketClientStatus.EnumState.class, string));
        }

        public C16(byte by, C16PacketClientStatus c16PacketClientStatus) {
            super((Packet)c16PacketClientStatus);
            this.status = c16PacketClientStatus.getStatus().name();
        }

        public C16(String string) {
            super((Packet)C16.create(string));
        }
    }

    public static class C07
    extends CPacket {
        public Vec3 position;
        public String facing;
        public String status;

        private static C07PacketPlayerDigging create(Vec3 vec3, String string, String string2) {
            return new C07PacketPlayerDigging(ScriptUtil.enumByName(C07PacketPlayerDigging.Action.class, string), vec3 == null ? null : Vec3.getBlockPos(vec3), ScriptUtil.enumByName(EnumFacing.class, string2));
        }

        public C07(Vec3 vec3, String string, String string2) {
            super((Packet)C07.create(vec3, string, string2));
        }

        public C07(byte by, C07PacketPlayerDigging c07PacketPlayerDigging) {
            super((Packet)c07PacketPlayerDigging);
            this.position = new Vec3(c07PacketPlayerDigging.getPosition());
            this.facing = c07PacketPlayerDigging.getFacing().name();
            this.status = c07PacketPlayerDigging.getStatus().name();
        }
    }

    public static class CPacket {
        public String name;
        protected Packet packet;

        public CPacket(Packet packet) {
            this.packet = packet;
            this.name = packet.getClass().getSimpleName();
        }

        public String toString() {
            return "CPacket(" + this.name + ")";
        }
    }

    public static class SPacket {
        public String name;
        protected Packet packet;

        public SPacket(Packet packet) {
            this.packet = packet;
            this.name = packet.getClass().getSimpleName();
        }

        public String toString() {
            return "SPacket(" + this.name + ")";
        }
    }

    public static class Request {
        String type;
        int readTimeout;
        String content;
        int connectTimeout;
        String userAgent;
        List<String[]> headerList;
        String url;
        private static int f117305 = 1980459105 >>> 182 | 1980459105 << ~182 + 1;
        private static int f117306 = (1570953752 >>> 52 | 1570953752 << ~52 + 1) & 0xFFFFFFFF;
        private static int f117307 = Integer.reverse(-2030630131);
        private static int f117308 = Integer.reverse(-2030630131);
        private static int f117309 = 302111043 >>> 224 | 302111043 << -224;
        private static int f117310 = (-1809768419 >>> 52 | -1809768419 << -52) & 0xFFFFFFFF;
        private static int f117311 = Integer.reverse(-644946036);
        private static int f117312 = Integer.reverse(-644946036);
        private static int f117313 = -1363605941 >>> 172 | -1363605941 << ~172 + 1;
        private static int f117314 = Integer.reverse(-1982374618);
        private static int f117315 = (1845431909 >>> 74 | 1845431909 << -74) & 0xFFFFFFFF;
        private static int f117316 = (1286455265 >>> 159 | 1286455265 << ~159 + 1) & 0xFFFFFFFF;
        private static int f117317 = -124655095 >>> 141 | -124655095 << ~141 + 1;
        private static int f117318 = Integer.reverse(-123473398);
        private static int f117319 = (-1624232348 >>> 197 | -1624232348 << ~197 + 1) & 0xFFFFFFFF;
        private static int f117320 = Integer.reverse(1317117732);
        private static int f117321 = Integer.reverse(-1691748028);
        private static int f117322 = Integer.reverse(-1691748028);
        private static int f117323 = 1826376333 >>> 173 | 1826376333 << -173;
        private static int f117324 = Integer.reverse(-949561814);
        private static int f117325 = Integer.reverse(-1411395874);
        private static int f117326 = -403199242 >>> 137 | -403199242 << -137;
        private static int f117327 = -1798033058 >>> 8 | -1798033058 << ~8 + 1;
        private static int f117328 = Integer.reverse(1943328389);
        private static int f117329 = (-1899453438 >>> 111 | -1899453438 << ~111 + 1) & 0xFFFFFFFF;
        private static int f117330 = Integer.reverse(-1984389102);
        private static int f117331 = Integer.reverse(-1793631689);
        private static int f117332 = Integer.reverse(353851959);
        private static int f117333 = Integer.reverse(2065142630);
        private static int f117334 = Integer.reverse(1793037158);

        public void setContent(String string) {
            this.content = string;
        }

        public void setConnectTimeout(int n) {
            this.connectTimeout = n;
        }

        public void addHeader(String string, String string2) {
            if (this.headerList == null) {
                this.headerList = new ArrayList<String[]>();
            }
            this.headerList.add(new String[]{string, string2});
        }

        public String toString() {
            return "Request(" + this.type + "," + this.url + ")";
        }

        public void setReadTimeout(int n) {
            this.readTimeout = n;
        }
        public Response fetch() {
            return null;
        }

        public void setUserAgent(String string) {
            this.userAgent = string;
        }

        public Request(String string, String string2) {
            switch (string = string.toUpperCase()) {
                case "GET":
                case "POST": {
                    break;
                }
                default: {
                    string = "GET";
                }
            }
            this.type = string;
            this.url = string2;
            this.content = "";
            this.userAgent = "";
            this.readTimeout = 5000;
            this.connectTimeout = 5000;
        }
    }

    public static class C0A
    extends CPacket {
        public C0A(byte by, C0APacketAnimation c0APacketAnimation) {
            super((Packet)c0APacketAnimation);
        }

        public C0A() {
            super((Packet)new C0APacketAnimation());
        }
    }

    public static class S3E
    extends SPacket {
        public String prefix;
        public int friendlyFlags;
        public int color;
        public String suffix;
        public String name;
        public Collection<String> playerList;
        public int action;
        public String nametagVisibility;
        public String displayName;

        public S3E(S3EPacketTeams s3EPacketTeams) {
            super((Packet)s3EPacketTeams);
            this.name = s3EPacketTeams.getName();
            this.displayName = s3EPacketTeams.getDisplayName();
            this.prefix = s3EPacketTeams.getPrefix();
            this.suffix = s3EPacketTeams.getSuffix();
            this.nametagVisibility = s3EPacketTeams.getNameTagVisibility();
            this.playerList = s3EPacketTeams.getPlayers();
            this.action = s3EPacketTeams.getAction();
            this.friendlyFlags = s3EPacketTeams.getFriendlyFlags();
            this.color = s3EPacketTeams.getColor();
        }
    }
}

