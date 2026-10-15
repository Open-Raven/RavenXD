package openraven.ravenxd.module.player;

import openraven.ravenxd.util.PlayerUtils;
import openraven.ravenxd.client.BotCheck;
import openraven.ravenxd.module.Category;
import openraven.ravenxd.module.Module;
import openraven.ravenxd.module.ModuleManager;
import openraven.ravenxd.module.setting.ButtonSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public class GhostHandModule extends Module {
    public static GhostHandModule instance;
    public static ButtonSetting allowMining, allowPlacing, disableWithWeapon, disableOnBots, onlyTools, onlySneaking;

    public GhostHandModule(int key) {
        super("GhostHand", Category.player, key);
        instance = this;
    }

    public void link() {
        allowMining = (ButtonSetting) ModuleManager.setting(this, "Allow mining");
        allowPlacing = (ButtonSetting) ModuleManager.setting(this, "Allow placing");
        disableWithWeapon = (ButtonSetting) ModuleManager.setting(this, "Disable with weapon");
        disableOnBots = (ButtonSetting) ModuleManager.setting(this, "Disable on bots");
        onlyTools = (ButtonSetting) ModuleManager.setting(this, "Only while holding tools");
        onlySneaking = (ButtonSetting) ModuleManager.setting(this, "Only while sneaking");
    }

    @Override
    public boolean overrideMouseOver() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
            return false;
        }
        if (!allowMining.enabled && Mouse.isButtonDown(0)) return false;
        if (!allowPlacing.enabled && Mouse.isButtonDown(1)) return false;
        if (disableWithWeapon.enabled && PlayerUtils.holdingWeapon()) return false;
        if (onlyTools.enabled && !PlayerUtils.holdingTool()) return false;
        if (onlySneaking.enabled && !mc.thePlayer.isSneaking()) return false;
        if (disableOnBots.enabled && BotCheck.isBot(mc.objectMouseOver.entityHit, false)) return false;
        MovingObjectPosition hit = BotCheck.rayTraceBlocks(mc.playerController.getBlockReachDistance());
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        mc.objectMouseOver = hit;
        return true;
    }
}
