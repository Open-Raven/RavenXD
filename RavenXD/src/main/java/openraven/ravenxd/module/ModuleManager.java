package openraven.ravenxd.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import openraven.ravenxd.module.setting.ButtonSetting;
import openraven.ravenxd.module.setting.DescriptionSetting;
import openraven.ravenxd.module.setting.Setting;
import openraven.ravenxd.module.setting.SliderSetting;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public static final List<Module> modules = new ArrayList<Module>();
    public static GuiModule gui;
    public static CommandLineModule commandLine;
    public static ChatCommandsModule chatCommands;
    public static IrcModule irc;
    public static SettingsModule settings;
    public static AssistantModule assistant;
    public static HudModule hud;
    public static NPlusThemeModule nplus;
    public static final List<Module> enabledModules = new ArrayList<Module>();
    public static final List<Module> afterDisableQueue = new ArrayList<Module>();
    public static final List<Module> closet = new ArrayList<Module>();
    public static final List<Module> hudOrder = new ArrayList<Module>();
    private static final java.util.Set<String> CLOSET = new java.util.HashSet<String>(java.util.Arrays.asList(
            "AutoClicker", "AimAssist", "Backtrack", "ClickAssist", "Hitbox", "Hit Select", "Jump Reset", "Reach", "Reduce",
            "Velocity", "WTap", "Sprint", "AutoBlockin", "AutoTool", "Delay Remover", "FastMine", "FastPlace", "GhostHand",
            "NoRotate", "AntiBot", "Weather", "BreakProgress", "AutoRequeue", "AutoWho", "Bed Wars", "Sky Wars", "AntiCheat",
            "Chat Bypass", "Hit Particles", "Latency Alerts", "Chat Commands", "IRC"));
    private static final List<Module> linked = new ArrayList<Module>();

    private ModuleManager() {
    }

    public static void init() {
        JsonArray arr = new JsonParser().parse(new InputStreamReader(
                ModuleManager.class.getResourceAsStream("/assets/clickgui/modules.json"), StandardCharsets.UTF_8)).getAsJsonArray();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            Module m;
            if (!o.get("failed").isJsonNull()) {
                m = new openraven.ravenxd.module.render.ShadersModule(0);
            } else {
                String name = o.get("name").getAsString();
                Category cat = Category.valueOf(o.get("category").getAsString());
                int key = o.get("key").getAsInt();
                if (name.equals("Gui")) {
                    m = gui = new GuiModule(key);
                } else if (name.equals("Command Line")) {
                    m = commandLine = new CommandLineModule(key);
                } else if (name.equals("Chat Commands")) {
                    m = chatCommands = new ChatCommandsModule(key);
                } else if (name.equals("IRC")) {
                    m = irc = new IrcModule(key);
                } else if (name.equals("Settings")) {
                    m = settings = new SettingsModule(key);
                } else if (name.equals("N+ Theme")) {
                    m = nplus = new NPlusThemeModule(key);
                } else if (name.equals("Assistant")) {
                    m = assistant = new AssistantModule(key);
                } else if (name.equals(openraven.ravenxd.script.ScriptManager.MANAGER_NAME) && cat == Category.scripts) {
                    m = new ScriptManagerModule(cat, key);
                    linked.add(m);
                } else if (name.equals(openraven.ravenxd.client.Profiles.MANAGER_NAME) && cat == Category.profiles) {
                    m = new ProfileManagerModule(cat, key);
                    linked.add(m);
                } else if (name.equals("Fake Chat")) {
                    m = new FakeChatModule(cat, key);
                } else if ((m = render(name, key)) != null) {
                    linked.add(m);
                } else {
                    m = new Module(name, cat, key);
                }
                m.noToggle = o.get("noToggle").getAsBoolean();
                m.hidden = o.get("hidden").getAsBoolean();
                m.lite = o.get("lite").getAsBoolean();
                for (JsonElement se : o.getAsJsonArray("settings")) {
                    m.registerSetting(setting(se.getAsJsonObject()));
                }
                if (name.equals("HUD")) {
                    hud = (HudModule) m;
                }
            }
            add(m);
            if (CLOSET.contains(m.getName())) {
                closet.add(m);
            }
        }
        gui.link();
        commandLine.link();
        chatCommands.link();
        irc.link();
        settings.link();
        assistant.link();
        nplus.link();
        for (Module m : linked) {
            m.link();
        }
        get("AntiBot").enable();
        get("Chat Commands").enable();
        get("Sprint").enable();
    }

    private static Module render(String name, int key) {
        switch (name) {
            case "HUD": return new HudModule(key);
            case "Test": return new openraven.ravenxd.module.dev.TestModule(key);
            case "View Packets": return new openraven.ravenxd.module.dev.ViewPacketsModule(key);
            case "Debug AC": return new openraven.ravenxd.module.dev.DebugAcModule(key);
            case "AntiDebuff": return new openraven.ravenxd.module.render.AntiDebuffModule(key);
            case "AntiShuffle": return new openraven.ravenxd.module.render.AntiShuffleModule(key);
            case "BedESP": return new openraven.ravenxd.module.render.BedESPModule(key);
            case "BreakProgress": return new openraven.ravenxd.module.render.BreakProgressModule(key);
            case "Chams": return new openraven.ravenxd.module.render.ChamsModule(key);
            case "ChestESP": return new openraven.ravenxd.module.render.ChestESPModule(key);
            case "Damage Tags": return new openraven.ravenxd.module.render.DamageTagsModule(key);
            case "Extend Camera": return new openraven.ravenxd.module.render.ExtendCameraModule(key);
            case "Extra Bobbing": return new openraven.ravenxd.module.render.ExtraBobbingModule(key);
            case "Fall View": return new openraven.ravenxd.module.render.FallViewModule(key);
            case "Indicators": return new openraven.ravenxd.module.render.IndicatorsModule(key);
            case "ItemESP": return new openraven.ravenxd.module.render.ItemESPModule(key);
            case "MobESP": return new openraven.ravenxd.module.render.MobESPModule(key);
            case "Nametags": return new openraven.ravenxd.module.render.NametagsModule(key);
            case "NoCameraClip": return new openraven.ravenxd.module.render.NoCameraClipModule(key);
            case "NoHurtCam": return new openraven.ravenxd.module.render.NoHurtCamModule(key);
            case "PlayerESP": return new openraven.ravenxd.module.render.PlayerESPModule(key);
            case "Radar": return new openraven.ravenxd.module.render.RadarModule(key);
            case "TargetHUD": return new openraven.ravenxd.module.render.TargetHudModule(key);
            case "Tracers": return new openraven.ravenxd.module.render.TracersModule(key);
            case "Trajectories": return new openraven.ravenxd.module.render.TrajectoriesModule(key);
            case "Xray": return new openraven.ravenxd.module.render.XrayModule(key);
            case "Weather": return new openraven.ravenxd.module.world.WeatherModule(key);
            case "Sprint": return new openraven.ravenxd.module.movement.SprintModule(key);
            case "AutoRequeue": return new openraven.ravenxd.module.minigames.AutoRequeueModule(key);
            case "Stop Motion": return new openraven.ravenxd.module.movement.StopMotionModule(key);
            case "Limbo": return new openraven.ravenxd.module.network.LimboModule(key);
            case "Delay Remover": return new openraven.ravenxd.module.player.DelayRemoverModule(key);
            case "GhostHand": return new openraven.ravenxd.module.player.GhostHandModule(key);
            case "KeepSprint": return new openraven.ravenxd.module.movement.KeepSprintModule(key);
            case "NoRotate": return new openraven.ravenxd.module.player.NoRotateModule(key);
            case "AutoWho": return new openraven.ravenxd.module.minigames.AutoWhoModule(key);
            case "Latency Alerts": return new openraven.ravenxd.module.other.LatencyAlertsModule(key);
            case "Custom Disabler": return new openraven.ravenxd.module.other.CustomDisablerModule(key);
            case "Disabler": return new openraven.ravenxd.module.other.DisablerModule(key);
            case "VPN Status": return new openraven.ravenxd.module.network.VpnStatusModule(key);
            case "Timer": return new openraven.ravenxd.module.movement.TimerModule(key);
            case "AutoJump": return new openraven.ravenxd.module.player.AutoJumpModule(key);
            case "AutoTool": return new openraven.ravenxd.module.player.AutoToolModule(key);
            case "AutoSwap": return new openraven.ravenxd.module.player.AutoSwapModule(key);
            case "FastPlace": return new openraven.ravenxd.module.player.FastPlaceModule(key);
            case "Scaffold": return new openraven.ravenxd.module.player.ScaffoldModule(key);
            case "Tower": return new openraven.ravenxd.module.player.TowerModule(key);
            case "Reach": return new openraven.ravenxd.module.combat.ReachModule(key);
            case "Hitbox": return new openraven.ravenxd.module.combat.HitboxModule(key);
            case "SafeWalk": return new openraven.ravenxd.module.player.SafeWalkModule(key);
            case "Teleport": return new openraven.ravenxd.module.movement.TeleportModule(key);
            case "45 Jump": return new openraven.ravenxd.module.movement.Jump45Module(key);
            case "Dolphin": return new openraven.ravenxd.module.movement.DolphinModule(key);
            case "Hit Particles": return new openraven.ravenxd.module.other.HitParticlesModule(key);
            case "Duels Stats": return new openraven.ravenxd.module.minigames.DuelsStatsModule(key);
            case "Bridge Info": return new openraven.ravenxd.module.minigames.BridgeInfoModule(key);
            case "Sumo Fences": return new openraven.ravenxd.module.minigames.SumoFencesModule(key);
            case "AntiBot": return new openraven.ravenxd.module.world.AntiBotModule(key);
            case "Reduce": return new openraven.ravenxd.module.combat.ReduceModule(key);
            case "Boost": return new openraven.ravenxd.module.movement.BoostModule(key);
            case "TPAura": return new openraven.ravenxd.module.combat.TPAuraModule(key);
            case "Velocity": return new openraven.ravenxd.module.combat.VelocityModule(key);
            case "Stasis": return new openraven.ravenxd.module.movement.StasisModule(key);
            case "Fly": return new openraven.ravenxd.module.movement.FlyModule(key);
            case "WTap": return new openraven.ravenxd.module.combat.WTapModule(key);
            case "Speed": return new openraven.ravenxd.module.movement.SpeedModule(key);
            case "FastMine": return new openraven.ravenxd.module.player.FastMineModule(key);
            case "AntiFireball": return new openraven.ravenxd.module.player.AntiFireballModule(key);
            case "Jump Reset": return new openraven.ravenxd.module.combat.JumpResetModule(key);
            case "Water Bucket": return new openraven.ravenxd.module.player.WaterBucketModule(key);
            case "AutoClicker": return new openraven.ravenxd.module.combat.AutoClickerModule(key);
            case "ClickAssist": return new openraven.ravenxd.module.combat.ClickAssistModule(key);
            case "NoFall": return new openraven.ravenxd.module.player.NoFallModule(key);
            case "AutoLadder": return new openraven.ravenxd.module.player.AutoLadderModule(key);
            case "AimAssist": return new openraven.ravenxd.module.combat.AimAssistModule(key);
            case "Hit Select": return new openraven.ravenxd.module.combat.HitSelectModule(key);
            case "Fake Lag": return new openraven.ravenxd.module.network.FakeLagModule(key);
            case "AntiAFK": return new openraven.ravenxd.module.player.AntiAFKModule(key);
            case "Blink": return new openraven.ravenxd.module.network.BlinkModule(key);
            case "VClip": return new openraven.ravenxd.module.movement.VClipModule(key);
            case "AutoBlock": return new openraven.ravenxd.module.combat.AutoBlockModule(key);
            case "Displace": return new openraven.ravenxd.module.combat.DisplaceModule(key);
            case "KillAura": return new openraven.ravenxd.module.combat.KillAuraModule(key);
            case "Bhop": return new openraven.ravenxd.module.movement.BhopModule(key);
            case "InvMove": return new openraven.ravenxd.module.movement.InvMoveModule(key);
            case "Long Jump": return new openraven.ravenxd.module.movement.LongJumpModule(key);
            case "NoSlow": return new openraven.ravenxd.module.movement.NoSlowModule(key);
            case "TargetStrafe": return new openraven.ravenxd.module.movement.TargetStrafeModule(key);
            case "AutoBlockin": return new openraven.ravenxd.module.player.AutoBlockinModule(key);
            case "AutoPlace": return new openraven.ravenxd.module.player.AutoPlaceModule(key);
            case "BedAura": return new openraven.ravenxd.module.player.BedAuraModule(key);
            case "Freecam": return new openraven.ravenxd.module.player.FreecamModule(key);
            case "InvManager": return new openraven.ravenxd.module.player.InvManagerModule(key);
            case "Backtrack": return new openraven.ravenxd.module.network.BacktrackModule(key);
            case "Freeze": return new openraven.ravenxd.module.network.FreezeModule(key);
            case "Lag Range": return new openraven.ravenxd.module.network.LagRangeModule(key);
            case "Bed Wars": return new openraven.ravenxd.module.minigames.BedWarsModule(key);
            case "Murder Mystery": return new openraven.ravenxd.module.minigames.MurderMysteryModule(key);
            case "Sky Wars": return new openraven.ravenxd.module.minigames.SkyWarsModule(key);
            case "Speed Builders": return new openraven.ravenxd.module.minigames.SpeedBuildersModule(key);
            case "AntiCheat": return new openraven.ravenxd.module.other.AntiCheatModule(key);
            default: return null;
        }
    }

    private static Setting setting(JsonObject s) {
        String t = s.get("t").getAsString();
        String name = s.get("name").getAsString();
        if (t.equals("toggle")) {
            return new ButtonSetting(name, s.get("value").getAsBoolean());
        }
        if (t.equals("button")) {
            return new ButtonSetting(name);
        }
        if (t.equals("desc")) {
            return new DescriptionSetting(name);
        }
        String[] options = null;
        if (!s.get("options").isJsonNull()) {
            JsonArray a = s.getAsJsonArray("options");
            options = new String[a.size()];
            for (int i = 0; i < options.length; i++) options[i] = a.get(i).getAsString();
        }
        HashMap<Double, String> labels = null;
        if (!s.get("labels").isJsonNull()) {
            labels = new HashMap<Double, String>();
            for (Map.Entry<String, JsonElement> l : s.getAsJsonObject("labels").entrySet()) {
                labels.put(Double.parseDouble(l.getKey()), l.getValue().getAsString());
            }
        }
        return new SliderSetting(name, s.get("prefix").getAsString(), s.get("suffix").getAsString(), s.get("value").getAsDouble(),
                s.get("min").getAsDouble(), s.get("max").getAsDouble(), s.get("step").getAsDouble(), options, labels);
    }

    private static final Map<String, Module> byName = new HashMap<String, Module>();
    private static final Map<String, Module> byLowerName = new HashMap<String, Module>();
    public static final Map<String, List<String>> categories = new HashMap<String, List<String>>();

    public static void add(Module m) {
        modules.add(m);
        hudOrder.add(m);
        byName.put(m.getName(), m);
        byLowerName.put(m.getName().toLowerCase(), m);
        categories.computeIfAbsent(m.moduleCategory().name(), k -> new ArrayList<String>()).add(m.getName());
    }

    public static void remove(Module m) {
        modules.remove(m);
        hudOrder.remove(m);
        byName.remove(m.getName());
        byLowerName.remove(m.getName().toLowerCase());
        List<String> list = categories.get(m.moduleCategory().name());
        if (list != null) {
            list.remove(m.getName());
        }
    }

    public static Module get(String name) {
        return byName.get(name);
    }

    public static Module script(String name) {
        Module m = byName.get(name);
        return m == null || m.isProfile() ? null : m;
    }

    public static Module getIgnoreCase(String name) {
        return byLowerName.get(name.toLowerCase());
    }

    public static Module enabled(String name) {
        Module m = get(name);
        return m != null && m.isEnabled() ? m : null;
    }

    public static Setting setting(Module m, String name) {
        for (Setting s : m.getSettings()) {
            if (s.getName().equals(name)) return s;
        }
        throw new IllegalStateException(m.getName() + " has no setting " + name);
    }
}
