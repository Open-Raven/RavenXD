package xyz.daikirai.script;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.List;
import java.util.Map;

public final class ScriptBridge {
    private ScriptBridge() {
    }

    public static boolean isOnEdge(Entity entity) {
        try {
            return ScriptProtected.isOnEdge(entity);
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static void roundedRect(float f, float f2, float f3, float f4, float f5, int n) {
        ScriptProtected.roundedRect(f, f2, f3, f4, f5, n);
    }

    public static void guiClick(int n, int n2, int n3) {
        try {
            ScriptProtected.guiClick(n, n2, n3);
        } catch (Throwable t) {
        }
    }

    public static void registerSlider(String string, String string2, int n, String[] stringArray) {
        try {
            ScriptProtected.registerSlider(string, string2, n, stringArray);
        } catch (Throwable t) {
        }
    }

    public static boolean isHoldingWeapon(EntityLivingBase entityLivingBase) {
        return ScriptProtected.isHoldingWeapon(entityLivingBase);
    }

    public static void removeFriend(String string) {
        try {
            ScriptProtected.removeFriend(string);
        } catch (Throwable t) {
        }
    }

    public static String getScriptName() {
        return ScriptProtected.getScriptName();
    }

    public static boolean isConsuming(EntityPlayer entityPlayer) {
        return ScriptProtected.isConsuming(entityPlayer);
    }

    public static boolean isTowering() {
        try {
            return ScriptProtected.isTowering();
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static String getServerDirection(ScriptDefaults.PlayerState playerState) {
        try {
            return ScriptProtected.getServerDirection(playerState.yaw);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void rightClick() {
        try {
            ScriptProtected.rightClick();
        } catch (Throwable t) {
        }
    }

    public static void prepareBloom() {
        ScriptProtected.prepareBloom();
    }

    public static void sendMessageNoPrefix(Object object) {
        ScriptProtected.sendMessageNoPrefix(object);
    }

    public static void setSpeedForward(double d) {
        try {
            ScriptProtected.setSpeedForward(d);
        } catch (Throwable t) {
        }
    }

    public static String stripString(String string) {
        return ScriptProtected.stripString(string);
    }

    public static void registerDescription(String string) {
        try {
            ScriptProtected.registerDescription(string);
        } catch (Throwable t) {
        }
    }

    public static void applyBloom(int n, float f) {
        ScriptProtected.applyBloom(n, f);
    }

    public static Entity getLagRangeTarget() {
        try {
            return ScriptProtected.getLagRangeTarget();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void addFriend(String string) {
        try {
            ScriptProtected.addFriend(string);
        } catch (Throwable t) {
        }
    }

    public static float[] getRotationsToBlock(BlockPos blockPos, EnumFacing enumFacing) {
        try {
            return ScriptProtected.getRotationsToBlock(blockPos, enumFacing);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void attack(Entity entity) {
        try {
            ScriptProtected.attack(entity);
        } catch (Throwable t) {
        }
    }

    public static boolean isEnemy(String string) {
        return ScriptProtected.isEnemy(string);
    }

    public static boolean isDiagonal() {
        try {
            return ScriptProtected.isDiagonal();
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static void sendPacket(Packet packet) {
        ScriptProtected.sendPacket(packet);
    }

    public static void prepareBlur() {
        ScriptProtected.prepareBlur();
    }

    public static void setTimer(double d) {
        try {
            ScriptProtected.setTimer((float) d);
        } catch (Throwable t) {
        }
    }

    public static String debug(String string) {
        return ScriptProtected.debug(string);
    }

    public static void removeEnemy(String string) {
        try {
            ScriptProtected.removeEnemy(string);
        } catch (Throwable t) {
        }
    }

    public static boolean canSeeEntity(Entity entity) {
        return ScriptProtected.canSeeEntity(entity);
    }

    public static Entity getKillAuraTarget() {
        return ScriptProtected.getKillAuraTarget();
    }

    public static List<String> getBookContents() {
        try {
            return ScriptProtected.getBookContents();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getUser() {
        return ScriptProtected.getUser();
    }

    public static float[] getBedAuraProgress() {
        try {
            return ScriptProtected.getBedAuraProgress();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void sendPacketNoEvent(Packet packet) {
        ScriptProtected.sendPacketNoEvent(packet);
    }

    public static void setSpeed(double d) {
        try {
            ScriptProtected.setSpeed(d);
        } catch (Throwable t) {
        }
    }

    public static String getDirection() {
        try {
            return ScriptProtected.getDirection();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean runRavenCommand(String string) {
        try {
            return ScriptProtected.runRavenCommand(string);
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static void renderItem(ItemStack itemStack, float f, float f2) {
        ScriptProtected.renderItem(itemStack, f, f2);
    }

    public static void renderBlock(int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
        ScriptProtected.renderBlock(n, n2, n3, n4, bl, bl2);
    }

    public static boolean isEnabled(String string) {
        return ScriptProtected.isEnabled(string);
    }

    public static float[] getRotationsToEntity(Entity entity) {
        try {
            return ScriptProtected.getRotationsToEntity(entity);
        } catch (Throwable t) {
            return null;
        }
    }

    public static double distanceToGround(Entity entity) {
        try {
            return ScriptProtected.distanceToGround(entity);
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static boolean isScaffolding() {
        try {
            return ScriptProtected.isScaffolding();
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static String getVersion() {
        return ScriptProtected.getVersion();
    }

    public static Object[] getSlider(String string, String string2) {
        return ScriptProtected.getSlider(string, string2);
    }

    public static boolean isFacingDiagonal() {
        try {
            return ScriptProtected.isFacingDiagonal();
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static Entity getAimAssistTarget() {
        try {
            return ScriptProtected.getAimAssistTarget();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean canInteract(Block block) {
        try {
            return ScriptProtected.canInteract(block);
        } catch (Throwable t) {
            throw new NullPointerException();
        }
    }

    public static Map<String, Object> getSettings(String string) {
        return ScriptProtected.getSettings(string);
    }

    public static void applyBlur(int n, float f) {
        ScriptProtected.applyBlur(n, f);
    }

    public static int getThemeRGB(String string) {
        return ScriptProtected.getThemeRGB(string);
    }

    public static void addEnemy(String string) {
        try {
            ScriptProtected.addEnemy(string);
        } catch (Throwable t) {
        }
    }

    public static float[] getRotationsToPos(double d, double d2, double d3) {
        try {
            return ScriptProtected.getRotationsToPos(d, d2, d3);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void enable(String string) {
        ScriptProtected.enable(string);
    }

    public static void leftClick() {
        try {
            ScriptProtected.leftClick();
        } catch (Throwable t) {
        }
    }

    public static BlockPos getWhitelistedBedPos() {
        return ScriptProtected.getWhitelistedBedPos();
    }

    public static void setSlider(String string, String string2, double d) {
        ScriptProtected.setSlider(string, string2, d);
    }

    public static boolean getButton(String string, String string2) {
        return ScriptProtected.getButton(string, string2);
    }

    public static boolean isFriend(String string) {
        return ScriptProtected.isFriend(string);
    }

    public static boolean isHidden(String string) {
        return ScriptProtected.isHidden(string);
    }

    public static void setButton(String string, String string2, boolean bl) {
        ScriptProtected.setButton(string, string2, bl);
    }

    public static void pling() {
        try {
            ScriptProtected.pling();
        } catch (Throwable t) {
        }
    }

    public static List<String> getSidebarLines() {
        return ScriptProtected.getSidebarLines();
    }

    public static Map<String, List<String>> getCategories() {
        return ScriptProtected.getCategories();
    }

    public static void disable(String string) {
        ScriptProtected.disable(string);
    }

    public static String getScreen() {
        return ScriptProtected.getScreen();
    }

    public static int getUID() {
        return ScriptProtected.getUID();
    }

    public static BlockPos getBedAuraPosition() {
        try {
            return ScriptProtected.getBedAuraPosition();
        } catch (Throwable t) {
            return null;
        }
    }

    public static Entity getBacktrackTarget() {
        try {
            return ScriptProtected.getBacktrackTarget();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void registerSlider(String string, String string2, double d, double d2, double d3, double d4) {
        try {
            ScriptProtected.registerSlider(string, string2, d, d2, d3, d4);
        } catch (Throwable t) {
        }
    }

    public static String getMode() {
        return ScriptProtected.getMode();
    }

    public static void registerButton(String string, boolean bl) {
        try {
            ScriptProtected.registerButton(string, bl);
        } catch (Throwable t) {
        }
    }
}
