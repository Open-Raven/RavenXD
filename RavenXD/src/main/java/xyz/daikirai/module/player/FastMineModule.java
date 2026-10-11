package xyz.daikirai.module.player;

import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class FastMineModule extends Module {
    public static SliderSetting breakDelay, breakSpeed, visualMode;
    public static ButtonSetting disableInCreative, ignoreShears;

    public enum Visual {
        PRE, POST, INCREMENT
    }

    public static Visual visual = Visual.PRE;
    private float lastDamage;
    private Field curBlockDamage;
    private Field blockHitDelay;

    public FastMineModule(int key) {
        super("FastMine", Category.player, key);
        try {
            this.curBlockDamage = ReflectionHelper.findField(PlayerControllerMP.class, "curBlockDamageMP", "field_78770_f");
            this.blockHitDelay = ReflectionHelper.findField(PlayerControllerMP.class, "blockHitDelay", "field_78781_i");
        } catch (Exception ignored) {
        }
    }

    public void link() {
        breakDelay = (SliderSetting) ModuleManager.setting(this, "Break delay");
        breakSpeed = (SliderSetting) ModuleManager.setting(this, "Break speed");
        visualMode = (SliderSetting) ModuleManager.setting(this, "Visual mode");
        disableInCreative = (ButtonSetting) ModuleManager.setting(this, "Disable in creative");
        ignoreShears = (ButtonSetting) ModuleManager.setting(this, "Ignore shears");
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (disableInCreative.enabled && mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        if (ignoreShears.enabled && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() == Items.shears) {
            return;
        }
        double d = breakDelay.getInput();
        if (d < 5.0) {
            try {
                if (d == 0.0) {
                    this.blockHitDelay.set(mc.playerController, 0);
                } else {
                    int n = this.blockHitDelay.getInt(mc.playerController);
                    if ((double) n > d) {
                        this.blockHitDelay.set(mc.playerController, (int) d);
                    }
                }
            } catch (IllegalAccessException | IndexOutOfBoundsException ignored) {
            }
        }
        double speed = breakSpeed.getInput();
        if (speed > 1.0) {
            if (!mc.thePlayer.capabilities.isCreativeMode && Mouse.isButtonDown(0)) {
                try {
                    float f = this.curBlockDamage.getFloat(mc.playerController);
                    switch (visual) {
                        case PRE: {
                            double d3 = 1.0 - 1.0 / speed;
                            if (!(f > 0.0f) || !((double) f < d3)) break;
                            this.curBlockDamage.set(mc.playerController, (float) d3);
                            break;
                        }
                        case POST: {
                            double d4 = 1.0 / speed;
                            if (!(f < 1.0f) || !((double) f >= d4)) break;
                            this.curBlockDamage.set(mc.playerController, 1.0f);
                            break;
                        }
                        case INCREMENT: {
                            float next = -1.0f;
                            if (f < 1.0f) {
                                if (f > this.lastDamage && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                                    float per = BlockUtils.breakSpeed(mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock(),
                                            mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem), false);
                                    next = (float) ((double) this.lastDamage + (double) per * (speed - 0.2152857 * (speed - 1.0)));
                                }
                                if (next != -1.0f && f > 0.0f) {
                                    this.curBlockDamage.set(mc.playerController, next);
                                }
                            }
                            this.lastDamage = f;
                        }
                    }
                } catch (IllegalAccessException | IndexOutOfBoundsException ignored) {
                }
            } else if (visual == Visual.INCREMENT) {
                this.lastDamage = 0.0f;
            }
        }
    }

    @Override
    public void guiUpdate() {
        visual = Visual.values()[(int) visualMode.getInput()];
    }

    @Override
    public void onEnable() {
        this.lastDamage = 0.0f;
    }
}
