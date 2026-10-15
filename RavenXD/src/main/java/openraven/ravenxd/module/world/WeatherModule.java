package xyz.daikirai.module.world;

import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WeatherModule extends Module {
    public static WeatherModule instance;
    public static SliderSetting time;
    public static SliderSetting lightning;
    public static ButtonSetting rain;
    public static Lightning mode = Lightning.Disabled;
    public static long savedTime = -9L;
    public static long savedRain = -1L;
    public static long lastThunder;

    private static final Random RANDOM = new Random();
    private static Field activeChunkSet;
    private static Field updateLCG;

    public enum Lightning {
        Disabled, Vanilla, Uncommon, Common, Frequent, Intense, Chaotic;

        public boolean shouldStrike() {
            if (this == Vanilla) {
                return RANDOM.nextInt(100000) == 0;
            }
            double d = Math.random();
            switch (this) {
                case Uncommon: return d > 0.9999;
                case Common: return d > 0.999;
                case Frequent: return d > 0.99;
                case Intense: return d > 0.9;
                case Chaotic: return d > 0.3;
                default: return false;
            }
        }
    }

    public WeatherModule(int key) {
        super("Weather", Category.world, key);
        instance = this;
        this.queueAfterDisable = true;
    }

    public void link() {
        time = (SliderSetting) ModuleManager.setting(this, "Time");
        lightning = (SliderSetting) ModuleManager.setting(this, "Lightning");
        rain = (ButtonSetting) ModuleManager.setting(this, "Rain");
    }

    public void guiUpdate() {
        mode = Lightning.values()[(int) lightning.getInput()];
    }

    public void onUpdate() {
        this.updateTime();
        this.updateRain();
        this.strike();
    }

    public void afterDisable() {
        if (!this.isEnabled()) {
            this.updateTime();
            this.updateRain();
        }
    }

    private void updateTime() {
        Minecraft mc = Minecraft.getMinecraft();
        if (this.isEnabled() && time.isNotDefault()) {
            if (savedTime == -9L) {
                savedTime = mc.theWorld.getWorldTime();
            }
            mc.theWorld.setWorldTime((long) (-1000.0 * (16.0 + time.getInput())));
        } else if (savedTime != -9L) {
            mc.theWorld.setWorldTime(savedTime);
            savedTime = -9L;
        }
    }

    private void updateRain() {
        Minecraft mc = Minecraft.getMinecraft();
        if (this.isEnabled() && rain.isToggled()) {
            if (savedRain == -1L) {
                savedRain = mc.theWorld.isRaining() ? 1L : 0L;
            }
            this.worldInfo().setRaining(true);
        } else if (savedRain != -1L) {
            this.worldInfo().setRaining(savedRain == 1L);
            savedRain = -1L;
        }
    }

    private WorldInfo worldInfo() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server.worldServers.length > 0) {
            return server.worldServers[0].getWorldInfo();
        }
        return Minecraft.getMinecraft().theWorld.getWorldInfo();
    }

    @SuppressWarnings("unchecked")
    private void strike() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!lightning.isNotDefault() || mc.isGamePaused()) {
            return;
        }
        try {
            if (activeChunkSet == null) {
                activeChunkSet = ReflectionHelper.findField(World.class, "activeChunkSet", "field_72993_I");
                updateLCG = ReflectionHelper.findField(World.class, "updateLCG", "field_73005_l");
            }
            long l = System.currentTimeMillis();
            Set<ChunkCoordIntPair> set = (Set<ChunkCoordIntPair>) activeChunkSet.get(mc.theWorld);
            for (ChunkCoordIntPair c : set) {
                if (!mode.shouldStrike()) continue;
                int x = c.chunkXPos * 16;
                int z = c.chunkZPos * 16;
                int n3 = (Integer) updateLCG.get(mc.theWorld);
                n3 = n3 * 3 + 1013904223;
                updateLCG.set(mc.theWorld, n3);
                int n4 = n3 >> 2;
                BlockPos pos = this.target(new BlockPos(x + (n4 & 0xF), 0, z + (n4 >> 8 & 0xF)));
                if (!mc.theWorld.canSeeSky(pos)) continue;
                mc.theWorld.addWeatherEffect(new EntityLightningBolt(mc.theWorld, pos.getX(), pos.getY(), pos.getZ()));
                if (Math.abs(l - lastThunder) <= 250L) continue;
                mc.theWorld.playSound(pos.getX(), pos.getY(), pos.getZ(), "ambient.weather.thunder", 10.0f, 0.8f + RANDOM.nextFloat() * 0.2f, false);
                lastThunder = l;
            }
        } catch (Exception exception) {
        }
    }

    private BlockPos target(BlockPos blockPos) {
        Minecraft mc = Minecraft.getMinecraft();
        BlockPos top = mc.theWorld.getPrecipitationHeight(blockPos);
        BlockPos sky = new BlockPos(top.getX(), mc.theWorld.getHeight(), top.getZ());
        AxisAlignedBB box = new AxisAlignedBB(top, sky).expand(3.0, 3.0, 3.0);
        List<EntityLivingBase> list = mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, box, WeatherModule::canStrike);
        return !list.isEmpty() ? list.get(RANDOM.nextInt(list.size())).getPosition() : top;
    }

    private static boolean canStrike(EntityLivingBase e) {
        return e != null && e.isEntityAlive() && Minecraft.getMinecraft().theWorld.canSeeSky(e.getPosition());
    }

    @Override
    public boolean onReceivePacket(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        if (time.isNotDefault()) {
            savedTime = ((S03PacketTimeUpdate) packet).getWorldTime();
            return false;
        }
        return true;
    }
}
