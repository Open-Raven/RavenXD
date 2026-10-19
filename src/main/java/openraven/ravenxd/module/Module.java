package openraven.ravenxd.module;

import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;

public class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public String name;
    public Category category;
    public int keycode;
    public boolean enabled;
    public boolean noToggle;
    public boolean hidden;
    public boolean lite;
    public long enabledAt;
    public long disabledAt;
    public long hudEnabledAt;
    public long hudDisabledAt;
    public boolean queueAfterDisable;
    public ArrayList<Setting> settings = new ArrayList<Setting>();

    public openraven.ravenxd.script.Script script;

    public Module(String name, Category category, int keycode) {
        this.name = name;
        this.category = category;
        this.keycode = keycode;
    }

    public Module(openraven.ravenxd.script.Script script, int keycode) {
        this.script = script;
        this.name = script.name;
        this.category = Category.scripts;
        this.keycode = keycode;
        this.lite = true;
    }

    public openraven.ravenxd.client.Profile profile;

    public Module(openraven.ravenxd.client.Profile profile, int keycode) {
        this.profile = profile;
        this.name = profile.name;
        this.category = Category.profiles;
        this.keycode = keycode;
        this.lite = true;
    }

    public boolean isScript() {
        return this.script != null;
    }

    public boolean isProfile() {
        return this.profile != null;
    }

    public String saveName() {
        return this.isScript() ? "sc-" + this.name : this.name;
    }

    public String getName() {
        return this.name;
    }

    public Category moduleCategory() {
        return this.category;
    }

    public ArrayList<Setting> getSettings() {
        return this.settings;
    }

    public void registerSetting(Setting s) {
        this.settings.add(s);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean canBeEnabled() {
        return true;
    }

    public void enable() {
        if (!this.canBeEnabled() || this.noToggle || this.isEnabled()) {
            return;
        }
        if (this.isScript()) {
            if (this.script.failed) {
                return;
            }
            if (openraven.ravenxd.script.ScriptManager.loading) {
                openraven.ravenxd.client.Chat.print("&cScripts are still loading...");
                return;
            }
        }
        if (this.isProfile()) {
            openraven.ravenxd.client.Profiles.enable(this.profile, false);
            return;
        }
        long now = System.currentTimeMillis();
        this.enabledAt = now;
        float f = HudModule.visibility(this, now);
        long l;
        if ((double) f <= 0.1) {
            l = 0L;
        } else if (f >= 1.0f) {
            l = 135;
        } else {
            l = (long) (((double) f - 0.1) / 0.9 * (double) 135);
            if (l < 0L) {
                l = 0L;
            } else if (l > (long) 135) {
                l = 135;
            }
        }
        this.hudEnabledAt = now - l;
        this.enabled = true;
        ModuleManager.enabledModules.add(this);
        if (!this.isScript()) {
            MinecraftForge.EVENT_BUS.register(this);
            this.onEnable();
        } else {
            openraven.ravenxd.script.ScriptManager.onEnable(this.script);
        }
    }

    public void disable() {
        if (!this.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.disabledAt = now;
        float f = HudModule.visibility(this, now);
        long l;
        if (f >= 1.0f) {
            l = 0L;
        } else if ((double) f <= 0.1) {
            l = 135;
        } else {
            l = (long) ((double) (1.0f - f) / 0.9 * (double) 135);
            if (l < 0L) {
                l = 0L;
            }
            if (l > (long) 135) {
                l = 135;
            }
        }
        this.hudDisabledAt = now - l;
        this.enabled = false;
        ModuleManager.enabledModules.remove(this);
        if (!this.isScript()) {
            MinecraftForge.EVENT_BUS.unregister(this);
            this.onDisable();
            if (this.queueAfterDisable) {
                ModuleManager.afterDisableQueue.add(this);
            }
        } else {
            openraven.ravenxd.script.ScriptManager.onDisable(this.script);
        }
    }

    public void link() {
    }

    public void onUpdate() {
    }

    public void afterDisable() {
    }

    public String getInfo() {
        return InfoSuffix.get(this);
    }

    public String getDisplayName() {
        return this.displayName(this.name);
    }

    public String displayName(String s) {
        String info;
        if (HudModule.showModuleInfo.isToggled() && (info = this.getInfo()) != null) {
            s = s + HudModule.INFO_SEPARATOR + info;
        }
        return HudModule.lowercase.isToggled() ? s.toLowerCase() : s;
    }

    public boolean isCloset() {
        return ModuleManager.closet.contains(this);
    }

    private boolean bindHeld;

    public boolean pollBind() {
        int key = this.getKeycode();
        if (key == 0 || key == -1) {
            return false;
        }
        if (key < 0 ? org.lwjgl.input.Mouse.isButtonDown(key + 100) : org.lwjgl.input.Keyboard.isKeyDown(key)) {
            if (!this.bindHeld) {
                this.toggle();
                this.bindHeld = true;
            }
        } else {
            this.bindHeld = false;
        }
        return this.bindHeld;
    }

    public void toggle() {
        if (this.isEnabled()) {
            this.disable();
        } else {
            this.enable();
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void guiButtonToggled(ButtonSetting b) {
    }

    public void guiUpdate() {
    }

    public boolean canBeBound() {
        return !this.noToggle && this != ModuleManager.commandLine;
    }

    public int getKeycode() {
        return this.keycode;
    }

    public void setBind(int n) {
        if (n >= 256) {
            return;
        }
        if (this.keycode == n) {
            return;
        }
        this.keycode = n;
        if (this.category != Category.profiles) {
            openraven.ravenxd.client.Profiles.markModified();
        }
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void toggleHidden() {
        if (this == ModuleManager.hud || this == ModuleManager.gui || this == ModuleManager.commandLine || this.isProfile()) {
            return;
        }
        this.hidden = !this.hidden;
        openraven.ravenxd.client.Profiles.markModified();
    }

    public void onPreUpdate() {
    }

    public void onPreUpdateAlways() {
    }

    public void onPreMotion(openraven.ravenxd.event.MotionEvent e) {
    }

    public void onPreMotionAlways(openraven.ravenxd.event.MotionEvent e) {
    }

    public void onPostMotion() {
    }

    public void onPostPlayerInput() {
    }

    public void onPostPlayerInputAlways() {
    }

    public float[] getRotations(float yaw, float pitch) {
        return null;
    }

    public Float getMovementYaw() {
        return null;
    }

    public boolean onPlayerSlowdown() {
        return false;
    }

    public void onUpdateEntityActionState() {
    }

    public boolean allowsSafeWalk() {
        return true;
    }

    public boolean stopsOnEdge() {
        return false;
    }

    public boolean isFakeBlocking() {
        return false;
    }

    public void onAttackSlowdown(net.minecraft.entity.Entity target) {
    }

    public boolean overrideMouseOver() {
        return false;
    }

    public boolean isClickingForReach() {
        return false;
    }

    public int spoofedSlot() {
        return -1;
    }

    public boolean onSendPacket(net.minecraft.network.Packet<?> packet) {
        return true;
    }

    public boolean onSendPacketAlways(net.minecraft.network.Packet<?> packet) {
        return true;
    }

    public boolean allowHeldItemPacket() {
        return true;
    }

    public void onPacketReceivedAlways() {
    }

    public boolean onReceivePacket(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        return true;
    }

    public boolean onReceivePacketAlways(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        return true;
    }

    public void onVelocityAlways() {
    }

    public void onKnockback(net.minecraft.network.play.server.S12PacketEntityVelocity packet, long sinceLast) {
    }

    public boolean isScaffolding() {
        return false;
    }

    public boolean isTowering() {
        return false;
    }

    public boolean isBlinking() {
        return false;
    }

    public boolean isFreecamActive() {
        return false;
    }

    public openraven.ravenxd.util.FakeRotation fakeRotation() {
        return openraven.ravenxd.util.FakeRotation.None;
    }

    public long fakeRotationStart() {
        return 0L;
    }

    public float fakeRotationStartPitch() {
        return 0.0f;
    }

    public void onPacketQueueTick() {
    }

    public void onPacketHeld(net.minecraft.network.Packet<?> packet) {
    }

    public net.minecraft.entity.Entity getTarget() {
        return null;
    }

    public boolean consumeSlotResync() {
        return false;
    }

    public void onChat(net.minecraftforge.client.event.ClientChatReceivedEvent e, String s) {
    }

    public void onPearlThrown(net.minecraft.item.ItemStack stack) {
    }
}
