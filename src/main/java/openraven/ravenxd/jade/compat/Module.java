package openraven.ravenxd.jade.compat;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

// Jade module adapter.
public abstract class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();
    public final String name;
    public final List<BooleanSetting> settings = new ArrayList<BooleanSetting>();
    public boolean enabled;

    protected Module(String name, Category category) {
        this.name = name;
    }

    protected void registerSetting(BooleanSetting setting) {
        this.settings.add(setting);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void guiUpdate() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }
}
