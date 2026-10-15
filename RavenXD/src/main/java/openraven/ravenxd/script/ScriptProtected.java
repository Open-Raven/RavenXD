package xyz.daikirai.script;

import xyz.daikirai.client.Account;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.ChatCommands;
import xyz.daikirai.client.Friends;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.ChatCommandsModule;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.Setting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Blur;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.EspShaders;
import xyz.daikirai.util.GlowShader;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.util.Theme;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptProtected implements Defaults {

    public static boolean canSeeEntity(Entity entity) {
        return RotationUtils.canSee(entity);
    }

    public static void addEnemy(String string) {
        Friends.addEnemy(string);
    }

    public static void applyBloom(int n, float f) {
        GlowShader.get().render(n, f);
    }

    public static String stripString(String string) {
        return Chat.strip(string);
    }

    public static void setTimer(float f) {
        MoveUtils.setTimer(f);
    }

    public static String getMode() {
        return Hypixel.state.name();
    }

    public static void setButton(String string, String string2, boolean bl) {
        Module module = ModuleManager.script(string);
        if (module != null) {
            for (Setting setting : module.getSettings()) {
                if (!(setting instanceof ButtonSetting) || !setting.getName().equals(string2) || ((ButtonSetting) setting).isMethodButton()) continue;
                ((ButtonSetting) setting).setEnabled(bl);
                ((ButtonSetting) setting).updateLabel();
                module.guiUpdate();
                break;
            }
        }
    }

    public static BlockPos getWhitelistedBedPos() {
        return xyz.daikirai.module.minigames.BedWarsModule.ownBed;
    }

    public static Map<String, List<String>> getCategories() {
        HashMap<String, List<String>> hashMap = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : ModuleManager.categories.entrySet()) {
            hashMap.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return Collections.unmodifiableMap(hashMap);
    }

    public static int getUID() {
        try {
            return Integer.parseInt(Account.UID);
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    public static Entity getLagRangeTarget() {
        return target("Lag Range");
    }

    public static boolean isHoldingWeapon(EntityLivingBase entityLivingBase) {
        return PlayerUtils.holdingWeapon(entityLivingBase);
    }

    public static boolean isTowering() {
        Module tower = ModuleManager.get("Tower");
        return tower != null && tower.isTowering();
    }

    public static void roundedRect(float f, float f2, float f3, float f4, float f5, int n) {
        EspShaders.roundRect(f, f2, f3, f4, f5, n);
    }

    public static List<String> getSidebarLines() {
        return Hypixel.sidebar();
    }

    public static void guiClick(int n, int n2, int n3) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, n, n2, n3, mc.thePlayer);
    }

    public static float[] invokeFloatArray(String string, Object... objectArray) {
        ScriptEvent event = ScriptEvent.byName(string);
        return event == null ? null : ScriptInvoker.dispatchFloatArray(event, objectArray);
    }

    public static boolean isHidden(String string) {
        Module module = ModuleManager.script(string);
        return module != null && module.isHidden();
    }

    public static boolean getButton(String string, String string2) {
        Module module = ModuleManager.script(string);
        if (module != null) {
            for (Setting setting : module.getSettings()) {
                if (!(setting instanceof ButtonSetting) || !setting.getName().equals(string2)) continue;
                return ((ButtonSetting) setting).isToggled();
            }
        }
        return false;
    }

    public static String getServerDirection(float f) {
        return PlayerUtils.direction(f).name();
    }

    public static void setSpeedForward(double d) {
        MoveUtils.setSpeed(d);
    }

    public static void applyBlur(int n, float f) {
        Blur.get().render(n, f);
    }

    public static void renderBlock(int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
        Render3D.blockBox(n, n2, n3, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, n4, bl, bl2);
    }

    public static void registerButton(String string, boolean bl) {
        ScriptManager.pendingSettings.add(new Object[]{1, string, bl});
    }

    public static String getDirection() {
        return PlayerUtils.direction().name();
    }

    public static void addFriend(String string) {
        Friends.addFriend(string);
    }

    public static String getScriptName() {
        return ScriptInvoker.currentScriptName();
    }

    public static Entity getKillAuraTarget() {
        return target("KillAura");
    }

    public static void enable(String string) {
        Module module = ModuleManager.script(string);
        if (module != null && !module.isEnabled()) {
            module.enable();
        }
    }

    public static float[] getRotationsToPos(double d, double d2, double d3) {
        return RotationUtils.toPositionFixed(d, d2, d3, RotationManager.lastYaw, RotationManager.lastPitch);
    }

    public static void prepareBloom() {
        GlowShader.get().startCapture();
    }

    public static Entity getAimAssistTarget() {
        return target("AimAssist");
    }

    public static String getVersion() {
        return Account.VERSION;
    }

    public static void setSlider(String string, String string2, double d) {
        Module module = ModuleManager.script(string);
        if (module != null) {
            for (Setting setting : module.getSettings()) {
                if (!(setting instanceof SliderSetting) || !setting.getName().equals(string2)) continue;
                SliderSetting slider = (SliderSetting) setting;
                if (slider.isMode()) {
                    slider.setValue(d);
                } else {
                    slider.setValueRaw(d);
                }
                module.guiUpdate();
                break;
            }
        }
    }

    public static boolean isEnemy(String string) {
        return Friends.isEnemy(string);
    }

    public static boolean canInteract(Block block) {
        return BlockUtils.isInteractable(block);
    }

    public static String getUser() {
        return Account.user();
    }

    public static void removeEnemy(String string) {
        Friends.removeEnemy(string);
    }

    public static void pling() {
        PlayerUtils.ping();
    }

    public static boolean runRavenCommand(String string) {
        return ChatCommands.handle(ChatCommandsModule.prefix + string);
    }

    public static Map<String, Object> getSettings(String string) {
        Module module = ModuleManager.script(string);
        if (module != null) {
            HashMap<String, Object> hashMap = new HashMap<String, Object>();
            for (Setting setting : module.getSettings()) {
                Object value = null;
                if (setting instanceof SliderSetting) {
                    value = ((SliderSetting) setting).getInput();
                } else if (setting instanceof ButtonSetting) {
                    value = ((ButtonSetting) setting).isToggled();
                }
                hashMap.put(setting.getName(), value);
            }
            return hashMap;
        }
        return null;
    }

    public static void registerSlider(String string, String string2, int n, String[] stringArray) {
        ScriptManager.pendingSettings.add(new Object[]{3, string, string2, n, stringArray});
    }

    public static void disable(String string) {
        Module module = ModuleManager.script(string);
        if (module != null && module.isEnabled()) {
            module.disable();
        }
    }

    public static void prepareBlur() {
        Blur.get().startStencil();
    }

    public static Entity getBacktrackTarget() {
        return target("Backtrack");
    }

    public static void sendPacketNoEvent(Packet packet) {
        PacketUtils.sendThroughLag(packet);
    }

    public static int invokeBoolean(String string, Object... objectArray) {
        ScriptEvent event = ScriptEvent.byName(string);
        return event == null ? -1 : ScriptInvoker.dispatchBoolean(event, objectArray);
    }

    public static void removeFriend(String string) {
        Friends.removeFriend(string);
    }

    public static void registerSlider(String string, String string2, double d, double d2, double d3, double d4) {
        ScriptManager.pendingSettings.add(new Object[]{2, string, string2, d, d2, d3, d4});
    }

    public static boolean isFriend(String string) {
        return Friends.isFriend(string);
    }

    public static void invoke(String string, Object... objectArray) {
        ScriptEvent event = ScriptEvent.byName(string);
        if (event != null) {
            ScriptInvoker.dispatch(event, objectArray);
        }
    }

    public static void leftClick() {
        PlayerUtils.clickMouse();
    }

    public static boolean isFacingDiagonal() {
        return PlayerUtils.diagonalYaw();
    }

    public static void setSpeed(double d) {
        MoveUtils.strafe(d);
    }

    public static BlockPos getBedAuraPosition() {
        return xyz.daikirai.module.player.BedAuraModule.target;
    }

    public static void sendPacket(Packet packet) {
        PacketUtils.send(packet);
    }

    public static List<String> getBookContents() {
        return PlayerUtils.bookPages();
    }

    public static double distanceToGround(Entity entity) {
        return PlayerUtils.heightAboveGround(entity);
    }

    public static boolean isOnEdge(Entity entity) {
        return PlayerUtils.isAirBelow(entity);
    }

    public static void renderItem(ItemStack itemStack, float f, float f2) {
        RenderUtils.drawItem(itemStack, f, f2);
    }

    public static void sendMessageNoPrefix(Object object) {
        Chat.raw(String.valueOf(object));
    }

    public static boolean isScaffolding() {
        Module scaffold = ModuleManager.get("Scaffold");
        return scaffold != null && scaffold.isScaffolding();
    }

    public static void attack(Entity entity) {
        PlayerUtils.attack(entity, false);
    }

    public static float[] getBedAuraProgress() {
        return new float[]{xyz.daikirai.module.player.BedAuraModule.progress, xyz.daikirai.module.player.BedAuraModule.vanillaProgress};
    }

    public static boolean isConsuming(EntityPlayer entityPlayer) {
        return PlayerUtils.isEating(entityPlayer);
    }

    public static Object[] getSlider(String string, String string2) {
        Module module = ModuleManager.script(string);
        if (module != null) {
            for (Setting setting : module.getSettings()) {
                if (!(setting instanceof SliderSetting) || !setting.getName().equals(string2)) continue;
                SliderSetting slider = (SliderSetting) setting;
                return new Object[]{slider.getInput(), slider.getDisplay(false), slider.getMin(), slider.getMax(), slider.interval};
            }
        }
        return null;
    }

    public static float[] getRotationsToEntity(Entity entity) {
        return RotationUtils.toEntityFixed(entity, RotationManager.lastYaw, RotationManager.lastPitch);
    }

    public static void rightClick() {
        PlayerUtils.rightClickMouse();
    }

    public static String getScreen() {
        return PlayerUtils.screenName(mc.currentScreen);
    }

    public static float[] getRotationsToBlock(BlockPos blockPos, EnumFacing enumFacing) {
        return RotationUtils.toBlockFixed(blockPos, enumFacing, RotationManager.lastYaw, RotationManager.lastPitch);
    }

    public static boolean isEnabled(String string) {
        Module module = ModuleManager.script(string);
        return module != null && module.isEnabled();
    }

    public static int getThemeRGB(String string) {
        Theme theme = Theme.BY_NAME.get(string);
        return theme == null ? 0 : theme.getRGB(0.0);
    }

    public static void registerDescription(String string) {
        ScriptManager.pendingSettings.add(new Object[]{0, string});
    }

    public static boolean isDiagonal() {
        return PlayerUtils.isDiagonal();
    }

    public static String debug(String string) {
        throw new UnsupportedOperationException("client.debug: R555.m2699 target was not recovered");
    }

    private static Entity target(String name) {
        Module module = ModuleManager.get(name);
        return module != null ? module.getTarget() : null;
    }
}
