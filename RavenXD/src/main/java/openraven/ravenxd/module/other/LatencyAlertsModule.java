package xyz.daikirai.module.other;

import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import net.minecraft.client.Minecraft;

public class LatencyAlertsModule extends Module {
    public static SliderSetting alertInterval, highLatency;
    public static ButtonSetting ignoreLimbo;
    public static long lastPacket;
    private long cooldown;
    private long intervalTicks;
    private long thresholdMs;
    private double lastInterval;
    private double lastThreshold;

    public LatencyAlertsModule(int key) {
        super("Latency Alerts", Category.other, key);
    }

    public void link() {
        alertInterval = (SliderSetting) ModuleManager.setting(this, "Alert interval");
        highLatency = (SliderSetting) ModuleManager.setting(this, "High latency");
        ignoreLimbo = (ButtonSetting) ModuleManager.setting(this, "Ignore limbo");
    }

    @Override
    public void onPacketReceivedAlways() {
        lastPacket = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        lastPacket = 0L;
        this.cooldown = 0L;
    }

    @Override
    public void guiUpdate() {
        if (this.lastInterval != alertInterval.getInput()) {
            this.lastInterval = alertInterval.getInput();
            this.intervalTicks = (long) (this.lastInterval * 20.0);
        }
        if (this.lastThreshold != highLatency.getInput()) {
            this.lastThreshold = highLatency.getInput();
            this.thresholdMs = (long) (this.lastThreshold * 1000.0);
        }
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null || ignoreLimbo.enabled && Hypixel.state == Hypixel.State.LIMBO) {
            lastPacket = 0L;
            return;
        }
        if (lastPacket == 0L || this.cooldown > 0L && --this.cooldown != 0L) {
            return;
        }
        long l = System.currentTimeMillis() - lastPacket;
        if (l >= this.thresholdMs) {
            Chat.print("&7Packet loss detected: &c" + l + "&7ms");
            this.cooldown = this.intervalTicks;
        }
    }
}
