package xyz.daikirai.module.player;

import xyz.daikirai.module.combat.KillAuraModule;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Irc;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.event.MotionEvent;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.BlockUtils;
import xyz.daikirai.util.FakeRotation;
import xyz.daikirai.util.ItemUtils;
import xyz.daikirai.util.KeyUtils;
import xyz.daikirai.util.MathUtils;
import xyz.daikirai.util.MoveUtils;
import xyz.daikirai.util.PacketUtils;
import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.RotationManager;
import xyz.daikirai.util.RotationUtils;
import xyz.daikirai.util.SwapSlot;
import xyz.daikirai.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ScaffoldModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public enum Rotation {
        None, Strict, Smooth, Simple, Lazy, Offset;

        public float side;
        public float sideJitter;
        public float groundSide;

        public void onDisable() {
            this.groundSide = 0.0f;
            this.sideJitter = 0.0f;
            this.side = 0.0f;
        }
    }

    public enum FastScaffold {
        Disabled(false, false, false),
        Vanilla(true, false, false),
        Constant_A(true, false, false),
        Constant_B(false, false, false),
        Float_A(true, false, false),
        Float_B(true, false, false),
        Jump_A(false, true, false),
        Jump_B(false, true, false),
        Jump_C(true, true, false),
        Jump_D(true, true, false),
        Keep_Y_A(false, false, true),
        Keep_Y_B(false, false, true),
        Side(true, false, false);

        public final boolean sprint;
        public final boolean jump;
        public final boolean keepY;
        public int state;
        public int ticks;
        public double height;

        FastScaffold(boolean sprint, boolean jump, boolean keepY) {
            this.sprint = sprint;
            this.jump = jump;
            this.keepY = keepY;
        }

        public boolean canSprint() {
            if (!this.canFastScaffold()) {
                if (mc.thePlayer.ticksExisted == startTick && jumpState == 2) {
                    return false;
                }
                return SettingsModule.movementFix.isToggled();
            }
            if (legitMotion.isToggled() && (this.jump || this.keepY)) {
                return legitTicks > 0;
            }
            if (!this.sprint) {
                return false;
            }
            return this != Side || this.ticks != -1;
        }

        public void onEnable() {
            enableTick = mc.thePlayer.ticksExisted;
            this.state = 0;
            if (this == Float_A) {
                if (mc.thePlayer.onGround) {
                    this.ticks = 1;
                    delay = 1;
                    if (mc.thePlayer.motionY == -0.0784000015258789) {
                        PlayerUtils.jump();
                    }
                    keptY = (int) Math.floor(mc.thePlayer.posY) - 1;
                }
            } else if (this == Jump_B || this == Jump_C) {
                this.height = 2.2;
            } else if (this == Jump_D) {
                this.height = 2.5;
            } else if (this == Side) {
                this.ticks = 4;
            }
        }

        public boolean canFastScaffold() {
            if (!active || this == Disabled) {
                return false;
            }
            if (TowerModule.towering || PlayerUtils.jumpKeyDown()) {
                return false;
            }
            if (this != Float_A) {
                if (!PlayerUtils.isMoving() || mc.thePlayer.isSneaking()) {
                    return false;
                }
                if (this.sprint && !this.jump && !PlayerUtils.direction().forward) {
                    return false;
                }
            }
            if (this == Side) {
                return !PlayerUtils.isDiagonal();
            }
            return true;
        }

        public void onDisable(boolean disabling) {
            if (this.jump || this.keepY) {
                keptY = -1;
            }
            if (this.jump && !disabling && !TowerModule.towering && TowerModule.slowedTicks.getInput() > 0.0) {
                MoveUtils.setSpeed(0.05);
            }
            if (this == Float_A) {
                if (disablingModule) {
                    if (mc.thePlayer.onGround && PlayerUtils.isAirBelow()) {
                        PlayerUtils.jump();
                    }
                    if (rotation != Rotation.Offset) {
                        multiplyMotion(0.58, false);
                    }
                }
            } else if (this == Float_B && disablingModule) {
                multiplyMotion(0.85, false);
            }
            this.ticks = 0;
        }
    }

    public enum MultiPlace {
        Disabled, _1_extra, _2_extra
    }

    public static ScaffoldModule instance;

    public static SliderSetting motion, rotationSetting, fakeRotationSetting, fastScaffoldSetting, fastScaffoldRmbSetting, fastScaffoldMotion,
            multiPlaceSetting, precisionSetting, sneakEvery, speedBoost, highlightTheme;
    public static ButtonSetting autoSwap, cancelKnockback, highlightBlocks, jumpFacingForward, legitMotion, safeWalk, showBlockCount,
            silentSwing, slowOnEnable, slowSpeedPotion, tower;

    public static Rotation rotation = Rotation.Strict;
    public static FakeRotation fakeRotation = FakeRotation.None;
    public static FastScaffold fastScaffold = FastScaffold.Disabled;
    private static FastScaffold lastFastScaffold = FastScaffold.Disabled;
    public static MultiPlace multiPlace = MultiPlace.Disabled;
    public static RotationUtils.Precision precision = RotationUtils.Precision.High;
    public static Theme theme = Theme.DEFAULT;

    public static boolean active;
    public static boolean disablingModule;
    private static boolean fastEnabled;
    private static boolean rightHeld;
    public static int keptY = -1;
    private static int searchMode;
    public static int legitTicks;
    public static int delay;
    public static int enableTick;
    public static int jumpState;
    public static int startTick;
    private static int jumpTick;
    private static int airBelowTicks;
    private static int placedCount;
    public static int pausedTicks;
    private static boolean sneakQueued;
    private static boolean smoothSnap;
    private static BlockPos lastPlaced;
    private static float aimYaw, aimPitch = 100.0f;
    private static float smoothLimit;
    private static float legitYaw;
    private static float restPitch;
    private static double yawOffset;
    private static long placedAt;
    public static long startedAt;
    public static float startPitch;
    private static long stoppedAt;
    private static long smoothAt;
    private static final HashMap<BlockPos, Long> placed = new HashMap<BlockPos, Long>();
    public static String countText;
    private static final EnumFacing[] SIDES = {EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST};
    private static final BlockPos.MutableBlockPos SCRATCH = new BlockPos.MutableBlockPos();

    public ScaffoldModule(int key) {
        super("Scaffold", Category.player, key);
        instance = this;
    }

    @Override
    public void link() {
        motion = (SliderSetting) ModuleManager.setting(this, "Motion");
        rotationSetting = (SliderSetting) ModuleManager.setting(this, "Rotation");
        fakeRotationSetting = (SliderSetting) ModuleManager.setting(this, "Rotation (fake)");
        fastScaffoldSetting = (SliderSetting) ModuleManager.setting(this, "Fast scaffold");
        fastScaffoldRmbSetting = (SliderSetting) ModuleManager.setting(this, "Fast scaffold RMB");
        fastScaffoldMotion = (SliderSetting) ModuleManager.setting(this, "Fast scaffold motion");
        multiPlaceSetting = (SliderSetting) ModuleManager.setting(this, "Multi-place");
        precisionSetting = (SliderSetting) ModuleManager.setting(this, "Precision");
        sneakEvery = (SliderSetting) ModuleManager.setting(this, "Sneak every");
        speedBoost = (SliderSetting) ModuleManager.setting(this, "Speed boost");
        highlightTheme = (SliderSetting) ModuleManager.setting(this, "Highlight theme");
        autoSwap = (ButtonSetting) ModuleManager.setting(this, "Auto swap");
        cancelKnockback = (ButtonSetting) ModuleManager.setting(this, "Cancel knockback");
        highlightBlocks = (ButtonSetting) ModuleManager.setting(this, "Highlight blocks");
        jumpFacingForward = (ButtonSetting) ModuleManager.setting(this, "Jump facing forward");
        legitMotion = (ButtonSetting) ModuleManager.setting(this, "Legit motion");
        safeWalk = (ButtonSetting) ModuleManager.setting(this, "SafeWalk");
        showBlockCount = (ButtonSetting) ModuleManager.setting(this, "Show block count");
        silentSwing = (ButtonSetting) ModuleManager.setting(this, "Silent swing");
        slowOnEnable = (ButtonSetting) ModuleManager.setting(this, "Slow on enable");
        slowSpeedPotion = (ButtonSetting) ModuleManager.setting(this, "Slow speed potion");
        tower = (ButtonSetting) ModuleManager.setting(this, "Tower");
    }

    @Override
    public boolean isScaffolding() {
        return active;
    }

    @Override
    public FakeRotation fakeRotation() {
        return fakeRotation;
    }

    @Override
    public long fakeRotationStart() {
        return startedAt;
    }

    @Override
    public float fakeRotationStartPitch() {
        return startPitch;
    }

    @Override
    public void onPreUpdate() {
        update();
    }

    @Override
    public void onPreMotion(MotionEvent e) {
        preMotion(e);
    }

    @Override
    public void onPostPlayerInputAlways() {
        postPlayerInput();
    }

    @Override
    public boolean stopsOnEdge() {
        return stopOnEdge();
    }

    @Override
    public float[] getRotations(float yaw, float pitch) {
        return rotations(yaw, pitch);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate || !active) {
            return;
        }
        e.setCanceled(true);
    }

    @Override
    public void onEnable() {
        if (autoSwap.isToggled()) {
            AutoSwapModule.remember(SwapSlot.Scaffold);
        }
        restPitch = randomFloat(84.5f, 87.5f);
        yawOffset = RotationUtils.randomOffset();
        if (!SafeWalkModule.scaffoldActive) {
            if (!mc.thePlayer.onGround) {
                delay = 3;
            } else if (PlayerUtils.potionLevel(Potion.jump) <= 3 && PlayerUtils.nearEdge(0.2) && AutoSwapModule.countBlocks() > 0) {
                jumpState = 2;
                jumpTick = mc.thePlayer.ticksExisted;
                delay = 4;
            }
        } else {
            delay = 1;
        }
    }

    @Override
    public void onDisable() {
        disablingModule = true;
        stop();
        legitTicks = 0;
        delay = 0;
        airBelowTicks = 0;
        pausedTicks = 0;
        placedCount = 0;
        placedAt = 0L;
        double d = MoveUtils.speed();
        if (d > 0.34) {
            multiplyMotion((0.29 + 0.02 * Math.random()) / d, false);
        }
        disablingModule = false;
    }

    @Override
    public void guiUpdate() {
        rotation = Rotation.values()[(int) rotationSetting.getInput()];
        fakeRotation = FakeRotation.values()[(int) fakeRotationSetting.getInput()];
        fastScaffold = FastScaffold.values()[(int) (rightHeld ? fastScaffoldRmbSetting.getInput() : fastScaffoldSetting.getInput())];
        multiPlace = MultiPlace.values()[(int) multiPlaceSetting.getInput()];
        precision = RotationUtils.Precision.values()[(int) precisionSetting.getInput()];
        theme = Theme.values()[(int) highlightTheme.getInput()];
        TowerModule.setFromScaffold(tower);
    }

    @Override
    public String getInfo() {
        if (fastScaffold == FastScaffold.Disabled) {
            return rotationSetting.getDisplay(false);
        }
        if (fastScaffold.jump) {
            return "Jump";
        }
        if (fastScaffold.keepY) {
            return "Keep-Y";
        }
        if (fastScaffold == FastScaffold.Constant_A || fastScaffold == FastScaffold.Constant_B) {
            return "Constant";
        }
        if (fastScaffold == FastScaffold.Float_A || fastScaffold == FastScaffold.Float_B) {
            return "Float";
        }
        return fastScaffold.name().replace("pa1", "(").replace("pa2", ")").replace("___", "-").replace("__", ".").replace("_", " ").trim();
    }

    private static void updateCount() {
        int n = AutoSwapModule.countBlocks();
        String color = null;
        if (n <= 5) {
            color = "§c";
        } else if (n <= 15) {
            color = "§6";
        } else if (n <= 25) {
            color = "§e";
        }
        countText = color == null ? n + " blocks" : color + n + "§f blocks";
    }

    public static void renderPlaced() {
        if (placed.isEmpty()) {
            return;
        }
        long l = System.currentTimeMillis();
        Iterator<Map.Entry<BlockPos, Long>> iterator = placed.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            long l2 = l - Math.abs(entry.getValue());
            float f = (float) l2 / 500.0f;
            if (f >= 1.0f) {
                iterator.remove();
                continue;
            }
            if (!highlightBlocks.isToggled()) continue;
            float f2 = 255.0f - Math.min(f * 255.0f, 255.0f);
            Render3D.block(entry.getKey(), RenderUtils.withAlpha(theme.getRGB(0.0), (int) f2), true, false);
        }
    }

    private static BlockPos place(BlockPos last, ItemStack itemStack) {
        BlockPos target = search(last);
        if (target == null) {
            return null;
        }
        EnumFacing facing = faceFor(target, null);
        if (facing == null) {
            return null;
        }
        BlockPos against = target.offset(facing.getOpposite());
        if (airBelowTicks > 0) {
            float yaw = aimYaw;
            float pitch = aimPitch;
            MovingObjectPosition hit = aim(against, facing, -170.0f, RotationManager.lastYaw);
            if (hit == null) {
                return last == null ? null : place(null, itemStack);
            }
            switch (rotation) {
                case Simple: {
                    float f3 = PlayerUtils.movementAngle();
                    if (!(Math.abs(f3 - aimYaw) < 100.0f)) break;
                    float f4 = aimYaw - f3;
                    f4 = (f4 + 540.0f) % 360.0f - 180.0f;
                    float f6 = 0.0f;
                    if (Math.abs(f4) >= 10.0f) {
                        f6 = Math.max(-10.0f, Math.min(f4, 10.0f));
                    }
                    aimYaw = f3 + f6;
                    if (!mc.thePlayer.onGround) break;
                    aimPitch = 82.5f;
                    break;
                }
                case Lazy: {
                    if (pitch == 100.0f || PlayerUtils.rayTraceBlock(against, null, mc.playerController.getBlockReachDistance(), yaw, pitch) == null) break;
                    aimYaw = yaw;
                    aimPitch = pitch;
                    break;
                }
                default:
                    break;
            }
            Vec3 vec3 = hit.hitVec;
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, against, facing, vec3)) {
                PlayerUtils.swing(silentSwing.isToggled(), true);
                if (searchMode == 1 || searchMode == 2 && fastScaffold.keepY) {
                    fastScaffold.ticks = mc.thePlayer.ticksExisted;
                }
                lastPlaced = target;
                placedAt = System.currentTimeMillis();
                placed.put(target, placedAt);
                airBelowTicks = 0;
                return target;
            }
        } else if (PlayerUtils.isMoving() && Math.random() > 0.8) {
            PlayerUtils.syncCurrentPlayItem();
            PacketUtils.send(new C08PacketPlayerBlockPlacement(itemStack));
        }
        return null;
    }

    private static BlockPos search(BlockPos last) {
        ArrayList<Vec3> list = new ArrayList<Vec3>();
        searchMode = 0;
        if (keptY != -1 && fastScaffold.canFastScaffold()) {
            int decimals = PlayerUtils.posYDecimals();
            if (fastScaffold == FastScaffold.Jump_A) {
                if (decimals == 1661 || decimals == 2492 && mc.thePlayer.motionY > 0.0) {
                    searchMode = 1;
                } else {
                    searchMode = 2;
                }
            } else if (fastScaffold == FastScaffold.Jump_B || fastScaffold == FastScaffold.Jump_C || fastScaffold == FastScaffold.Jump_D) {
                if (mc.thePlayer.ticksExisted - fastScaffold.ticks > 4 && mc.thePlayer.posY - (double) keptY < fastScaffold.height && mc.thePlayer.motionY < -0.1) {
                    searchMode = 1;
                    if (fastScaffold.ticks == 0) {
                        multiplyMotion(0.4, false);
                    }
                } else {
                    searchMode = 2;
                }
            } else if (fastScaffold.keepY) {
                searchMode = 2;
            } else if (fastScaffold == FastScaffold.Float_A) {
                if (mc.thePlayer.onGround && mc.thePlayer.ticksExisted - enableTick > 0) {
                    keptY = -1;
                } else {
                    searchMode = 2;
                }
            }
        }
        return candidates(list, searchMode, keptY) ? firstPlaceable(list, last) : null;
    }

    private static boolean candidates(List<Vec3> list, int mode, int y) {
        int n3;
        double d;
        for (int i = -5; i <= 5; ++i) {
            for (int j = -5; j <= 5; ++j) {
                for (int k = -3; k <= -1; ++k) {
                    int n4 = MathHelper.floor_double(mc.thePlayer.posY) + k;
                    if (mode > 0 && n4 != y) continue;
                    double d2 = mc.thePlayer.posX + (double) i;
                    d = mc.thePlayer.posZ + (double) j;
                    SCRATCH.set(MathHelper.floor_double(d2), n4, MathHelper.floor_double(d));
                    if (BlockUtils.isReplaceable(SCRATCH)) continue;
                    for (n3 = -1; n3 <= 1; n3 += 2) {
                        switch (mode) {
                            case 0:
                                list.add(new Vec3(d2 + (double) n3, (double) n4, d));
                                list.add(new Vec3(d2, (double) n4, d + (double) n3));
                            case 1:
                                list.add(new Vec3(d2, (double) (n4 + n3), d));
                                continue;
                            case 2:
                                list.add(new Vec3(d2 + (double) n3, (double) n4, d));
                                list.add(new Vec3(d2, (double) n4, d + (double) n3));
                        }
                    }
                }
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        HashSet<Long> seen = new HashSet<Long>(list.size() * 2);
        ArrayList<Vec3> unique = new ArrayList<Vec3>(list.size());
        for (Vec3 vec3 : list) {
            long l = Double.doubleToLongBits(vec3.xCoord) ^ Double.doubleToLongBits(vec3.yCoord) * 31L ^ Double.doubleToLongBits(vec3.zCoord) * 31L * 31L;
            if (!seen.add(l)) continue;
            unique.add(vec3);
        }
        list.clear();
        list.addAll(unique);
        final double x = mc.thePlayer.posX;
        double y2 = mc.thePlayer.posY - 1.0;
        final double z = mc.thePlayer.posZ;
        if (!mc.thePlayer.onGround && !PlayerUtils.jumpKeyDown() && !fastScaffold.canFastScaffold()) {
            y2 -= 1.0;
        }
        final double fy = y2;
        Comparator<Vec3> comparator = Comparator.comparingDouble(p -> xyz.daikirai.util.WorldUtils.distanceSq(x, fy, z, p));
        if (mc.thePlayer.ticksExisted - startTick <= 3) {
            double yaw = mc.thePlayer.rotationYaw * ((float) Math.PI / 180);
            final double dirX = -Math.sin(yaw);
            final double dirZ = Math.cos(yaw);
            comparator = comparator.thenComparingDouble(p -> xyz.daikirai.util.WorldUtils.behind(x, z, dirX, dirZ, p));
        }
        list.sort(comparator);
        return true;
    }

    private static BlockPos firstPlaceable(List<Vec3> list, BlockPos last) {
        for (Vec3 vec3 : list) {
            SCRATCH.set(MathHelper.floor_double(vec3.xCoord), MathHelper.floor_double(vec3.yCoord), MathHelper.floor_double(vec3.zCoord));
            if (!BlockUtils.isReplaceable(SCRATCH) || faceFor(SCRATCH, last) == null) continue;
            return new BlockPos((Vec3i) SCRATCH);
        }
        return null;
    }

    private static float[] rotations(float f, float f2) {
        if (!active || rotation == Rotation.None) {
            return null;
        }
        long l = sincePlace();
        float f7;
        float f8;
        float f9;
        float f10;
        if (rotation == Rotation.Offset) {
            float f6 = (float) RotationUtils.inputAngleSigned();
            f7 = mc.thePlayer.rotationYaw + f6;
            f8 = (f7 % 360.0f + 360.0f) % 360.0f;
            f9 = f8 % 90.0f;
            if (!(f9 >= 80.0f) && !(f9 < 10.0f)) {
                f10 = f9 >= 10.0f && f9 < 45.0f ? 45.0f : -45.0f;
            } else {
                f10 = -45.0f;
            }
            if (!mc.thePlayer.onGround && rotation.groundSide != 0.0f) {
                f10 = rotation.groundSide;
            } else {
                rotation.groundSide = f10;
            }
            if (f10 != rotation.side) {
                rotation.side = f10;
                rotation.sideJitter = (float) ((double) Math.signum(f10) * MathUtils.randomBetween(0.02, 0.08));
            }
            aimYaw = PlayerUtils.movementAngle() + (f10 + rotation.sideJitter);
        }
        boolean jumpRotation = false;
        if (legitTicks <= 0) {
            if (fastScaffold == FastScaffold.Float_A && fastScaffold.ticks == 1 && mc.thePlayer.ticksExisted == enableTick) {
                jumpRotation = true;
            }
        } else {
            --legitTicks;
            jumpRotation = true;
        }
        if (jumpRotation) {
            f7 = -180.0f;
            f8 = aimPitch == 100.0f ? mc.thePlayer.rotationPitch : aimPitch;
            if (legitMotion.isToggled()) {
                if (legitTicks == 4) {
                    if ((double) (mc.thePlayer.rotationYaw - RotationManager.playerYawAtSend) < -0.7) {
                        f7 = -f7;
                    }
                    legitYaw = f7;
                    --delay;
                    jumpRotation = false;
                } else if (legitTicks == 3) {
                    if (legitYaw > 0.0f) {
                        f7 = -f7;
                    }
                    if (rotation == Rotation.Smooth) {
                        legitTicks = Math.min(legitTicks, 1);
                        if (delay > legitTicks) {
                            delay = legitTicks;
                        }
                        smoothSnap = true;
                    }
                    f8 = randomFloat(20.0f, 25.0f);
                } else if (legitTicks == 2) {
                    legitYaw += randomFloat(105.0f, 120.0f) * -Math.signum(legitYaw);
                    f7 = legitYaw;
                    f8 = randomFloat(40.0f, 45.0f);
                } else if (legitTicks == 1) {
                    legitYaw += randomFloat(33.0f, 35.0f) * -Math.signum(legitYaw);
                    f7 = legitYaw;
                    if (aimPitch == 100.0f || rotation == Rotation.Simple) {
                        f8 = randomFloat(50.0f, 55.0f);
                    }
                } else if (legitTicks == 0) {
                    legitYaw += randomFloat(29.0f, 33.0f) * -Math.signum(legitYaw);
                    f7 = legitYaw;
                    if (aimPitch == 100.0f || rotation == Rotation.Simple) {
                        f8 = randomFloat(65.0f, 70.0f);
                    }
                }
            }
            if (rotation == Rotation.Offset && rotation.side > 0.0f) {
                f7 = -f7;
            }
            if (jumpRotation) {
                return RotationUtils.fixSensitivity(PlayerUtils.movementAngle() + f7 + RotationUtils.randomOffset(), f8, f, f2, false);
            }
        }
        float yaw;
        float pitch;
        if (rotation == Rotation.Smooth && lastPlaced != null && PlayerUtils.groundTicks <= 3L) {
            yaw = aimYaw;
            pitch = aimPitch;
            f7 = smoothLimit;
            if (smoothSnap) {
                smoothSnap = false;
                f7 = 81.0f;
                f7 += randomFloat(-2.0f, 2.0f);
            }
            f8 = MathHelper.wrapAngleTo180_float(yaw - f);
            if (Math.abs(f8) > f7) {
                yaw = f + (!(f8 > 0.0f) ? -f7 : f7);
            }
            float[] r = RotationUtils.fixSensitivity(yaw, pitch, f, f2, false);
            return new float[]{r[0], r[1]};
        }
        if (aimPitch != 100.0f && l < holdTime()) {
            yaw = aimYaw;
            pitch = aimPitch;
            if (l > 5L && PlayerUtils.isMoving() && Math.random() > 0.3) {
                yaw = (float) ((double) yaw + MathUtils.randomBetween(0.05, 0.5) * (double) (!(Math.random() < 0.5) ? 1 : -1));
                pitch += RotationUtils.randomOffset();
            }
            float[] r = RotationUtils.fixSensitivity(yaw, pitch, f, f2, false);
            yaw = r[0];
            pitch = r[1];
        } else {
            if (aimPitch != 100.0f) {
                aimPitch = 100.0f;
            }
            int n = mc.thePlayer.ticksExisted - startTick;
            f8 = !SafeWalkModule.scaffoldActive ? PlayerUtils.movementAngle() : mc.thePlayer.rotationYaw;
            f9 = rotation != Rotation.Simple ? restPitch : 82.5f;
            f8 += (float) yawOffset;
            f9 += RotationUtils.randomOffset();
            if (n <= 2 && !mc.thePlayer.onGround && mc.thePlayer.ticksExisted - jumpTick > 3) {
                BlockPos target = search(null);
                if (target != null) {
                    EnumFacing facing = faceFor(target, null);
                    if (facing != null && facing != EnumFacing.UP) {
                        BlockPos against = target.offset(facing.getOpposite());
                        float[] r = RotationUtils.searchBlockRotation(against, facing, -170.0f, precision, f, SafeWalkModule.scaffoldActive);
                        if (r != null) {
                            f8 = r[0];
                            f9 = r[1];
                        }
                    }
                }
            } else if (rotation == Rotation.Offset) {
                f8 = aimYaw;
            }
            yaw = f8;
            pitch = f9;
            if (n <= 2) {
                f10 = n != 0 ? (n != 2 ? 0.8f : 0.55f) : 0.3f;
                f10 = (float) ((double) f10 + MathUtils.randomBetween(-0.02, 0.02));
                yaw = f + f10 * (f8 - f);
                pitch = f2 + f10 * (f9 - f2);
                if (Math.abs(yaw - f8) < 5.0f) {
                    delay = 0;
                }
            }
            float[] r = RotationUtils.fixSensitivity(yaw, pitch, f, f2, false);
            yaw = r[0];
            if (yaw != f) {
                pitch = r[1];
                yawOffset = RotationUtils.randomOffset();
            } else {
                pitch = f2;
            }
        }
        return new float[]{yaw, pitch};
    }

    public static void renderCount() {
        if (showBlockCount == null || !showBlockCount.isToggled() || countText == null || AutoBlockinModule.percentText != null) {
            return;
        }
        long now = System.currentTimeMillis();
        int alpha;
        if (active) {
            long l = now - startedAt;
            alpha = l < 125L ? (int) ((float) l / 125.0f * 240.0f) : 240;
        } else {
            long l = now - stoppedAt;
            if (l < 125L) {
                alpha = (int) (240.0f - (float) l / 125.0f * 240.0f);
            } else {
                alpha = 0;
                countText = null;
            }
        }
        if (alpha > 0) {
            float x = (float) RenderUtils.scaledWidth / 2.0f + 11.0f;
            float y = (float) RenderUtils.scaledHeight / 2.0f - 3.0f;
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(countText, x, y, RenderUtils.withAlpha(-1, RenderUtils.clampAlpha(alpha)), true);
            GlStateManager.disableBlend();
        }
    }

    private static void jumpWith(double d) {
        if (legitMotion.isToggled()) {
            return;
        }
        PlayerUtils.jump(boosted(d));
    }

    private static boolean placeExtra(ItemStack itemStack) {
        BlockPos target = search(lastPlaced);
        if (target == null) {
            return false;
        }
        EnumFacing facing = faceFor(target, lastPlaced);
        if (facing == null) {
            return false;
        }
        BlockPos against = target.offset(facing.getOpposite());
        float[] r = RotationUtils.searchBlockRotation(against, facing, -170.0f, precision, RotationManager.lastYaw, SafeWalkModule.scaffoldActive);
        if (r == null) {
            return false;
        }
        MovingObjectPosition hit = PlayerUtils.rayTraceBlock(against, null, mc.playerController.getBlockReachDistance(), r[0], r[1]);
        if (hit == null) {
            return false;
        }
        PlayerUtils.place(hit, itemStack);
        PlayerUtils.swing(silentSwing.isToggled(), true);
        lastPlaced = target;
        placedAt = System.currentTimeMillis();
        placed.put(target, placedAt);
        return true;
    }

    private static void fastScaffoldTick() {
        rightHeld = Mouse.isButtonDown(1) && mc.currentScreen == null;
        fastScaffold = FastScaffold.values()[(int) (!rightHeld ? fastScaffoldSetting.getInput() : fastScaffoldRmbSetting.getInput())];
        boolean moving = PlayerUtils.isMoving();
        boolean groundMoving = mc.thePlayer.onGround && moving;
        boolean can = fastScaffold.canFastScaffold();
        boolean sprintForward = fastScaffold.sprint && PlayerUtils.direction().forward;
        boolean applied = false;
        if (fastScaffold == lastFastScaffold) {
            if (!can) {
                if (fastEnabled) {
                    fastEnabled = false;
                    fastScaffold.onDisable(false);
                }
            } else if (!fastEnabled) {
                fastEnabled = true;
                fastScaffold.onEnable();
            }
        } else {
            if (fastEnabled && lastFastScaffold != null) {
                lastFastScaffold.onDisable(false);
            }
            if (can) {
                fastScaffold.onEnable();
            }
            fastEnabled = can;
            lastFastScaffold = fastScaffold;
        }
        double d;
        if (!can) {
            if (!TowerModule.towering && legitTicks > 0 && mc.thePlayer.onGround) {
                delay = 0;
                legitTicks = 0;
            }
        } else {
            boolean newY;
            double speed;
            switch (fastScaffold) {
                case Constant_A:
                    if (groundMoving) {
                        d = 0.153 + PlayerUtils.perSpeedLevel(0.034);
                        if (PlayerUtils.speedLevel() == 0 && PlayerUtils.isDiagonal()) {
                            d *= 0.87;
                        }
                        MoveUtils.strafe(d);
                    }
                    break;
                case Constant_B:
                    if (groundMoving) {
                        MoveUtils.strafe(0.181);
                    }
                    break;
                case Jump_A:
                case Jump_B:
                case Jump_C:
                case Jump_D:
                    if (!groundMoving) {
                        if (moving && jumpFacingForward.isToggled() && mc.thePlayer.motionY < 0.0) {
                            d = Math.floor(mc.thePlayer.posY);
                            if (d - 2.0 == (double) keptY && mc.thePlayer.posY + mc.thePlayer.motionY < d) {
                                legitTicks = !legitMotion.isToggled() ? 1 : 5;
                            }
                        }
                        break;
                    }
                    if (fastScaffold.state != 2) {
                        newY = false;
                        if (mc.thePlayer.ticksExisted - fastScaffold.ticks > 10) {
                            keptY = (int) Math.floor(mc.thePlayer.posY) - 1;
                            newY = true;
                        }
                        if ((jumpFacingForward.isToggled() || FastScaffold.Float_A.state == 1 || FastScaffold.Float_B.state == 1) && newY) {
                            if (jumpFacingForward.isToggled()) {
                                legitTicks = !legitMotion.isToggled() ? 1 : 5;
                            }
                            fastScaffold.state = 1;
                        }
                        if (fastScaffold.state == 0) {
                            if (fastScaffold != FastScaffold.Jump_A) {
                                if (!sprintForward) {
                                    speed = !newY ? 0.545 : 0.4;
                                } else {
                                    speed = !newY ? 0.3 : 0.25;
                                }
                            } else {
                                speed = !PlayerUtils.isDiagonal() ? 0.53 : 0.37;
                            }
                            jumpWith(speed);
                        }
                    } else {
                        fastScaffold.state = 0;
                    }
                    multiplyMotion(0.95, false);
                    break;
                case Keep_Y_A:
                case Keep_Y_B:
                    if (!groundMoving) {
                        if (moving && jumpFacingForward.isToggled() && mc.thePlayer.motionY < 0.0) {
                            d = Math.floor(mc.thePlayer.posY);
                            if (d - 1.0 == (double) keptY && mc.thePlayer.posY + mc.thePlayer.motionY < d) {
                                legitTicks = !legitMotion.isToggled() ? 1 : 5;
                            }
                        }
                    } else {
                        if (fastScaffold.state != 2) {
                            newY = false;
                            if (mc.thePlayer.ticksExisted - fastScaffold.ticks > 10) {
                                keptY = (int) Math.floor(mc.thePlayer.posY) - 1;
                                newY = true;
                            }
                            if ((jumpFacingForward.isToggled() || FastScaffold.Float_A.state == 1 || FastScaffold.Float_B.state == 1) && newY) {
                                if (jumpFacingForward.isToggled() && legitTicks == 0) {
                                    legitTicks = !legitMotion.isToggled() ? 1 : 5;
                                }
                                fastScaffold.state = 1;
                                multiplyMotion(PlayerUtils.speedLevel() <= 0 ? 0.8 : 0.4, false);
                            }
                            if (fastScaffold.state == 0) {
                                if (!sprintForward) {
                                    speed = !newY ? 0.545 : 0.54;
                                } else {
                                    speed = !newY ? 0.3 : 0.25;
                                }
                                jumpWith(speed);
                            }
                        } else {
                            fastScaffold.state = 0;
                        }
                        multiplyMotion(0.95, false);
                    }
                    break;
                default:
                    break;
            }
            if (!mc.thePlayer.onGround && (fastScaffold == FastScaffold.Jump_D || fastScaffold == FastScaffold.Keep_Y_B) && PlayerUtils.motionYDecimals() == 30) {
                mc.thePlayer.motionY -= 0.14;
            }
            if (groundMoving) {
                d = fastScaffoldMotion.getInput();
                if (!legitMotion.isToggled() && PlayerUtils.direction().diagonal) {
                    d -= 0.02;
                }
                multiplyMotion(d, true);
                applied = true;
            }
        }
        if (!applied && groundMoving) {
            d = motion.getInput();
            if (!legitMotion.isToggled()) {
                if (slowSpeedPotion.isToggled()) {
                    d -= PlayerUtils.perSpeedLevel(0.17);
                }
                if (PlayerUtils.direction().diagonal) {
                    d -= 0.02;
                }
                if (mc.thePlayer.ticksExisted - startTick <= 2) {
                    d -= 0.05;
                }
            }
            multiplyMotion(d, true);
        }
    }

    private static long holdTime() {
        if (!SafeWalkModule.scaffoldActive) {
            if (mc.thePlayer.isSprinting()) {
                return 710L;
            }
            if (!PlayerUtils.isMoving()) {
                return 460L;
            }
        }
        return 110L;
    }

    private static void stop() {
        if (active) {
            stoppedAt = System.currentTimeMillis();
            rotation.onDisable();
            if (!Overlay.screenOpen()) {
                AutoSwapModule.restore(SwapSlot.Scaffold, autoSwap.isToggled());
            }
            if (fastEnabled) {
                fastScaffold.onDisable(true);
                fastEnabled = false;
            }
            TowerModule.stop();
            sneakQueued = false;
            smoothSnap = false;
            rightHeld = false;
            active = false;
            RotationManager.smoothYaw = null;
            jumpState = 1;
            if (mc.thePlayer.onGround && rotation != Rotation.Offset) {
                multiplyMotion(0.75, false);
            }
        }
        keptY = -1;
        lastPlaced = null;
        aimPitch = 100.0f;
    }

    public static boolean pauseForKillAura() {
        if (instance != null && instance.isEnabled() && AutoSwapModule.countBlocks() > 0) {
            if (KillAuraModule.stopBlocking(false)) {
                pausedTicks = 2;
            }
            return false;
        }
        return true;
    }

    public static void resetKeepY() {
        keptY = -1;
    }

    private static boolean stopOnEdge() {
        if (active) {
            if (safeWalk.isToggled()) {
                if (fastScaffold.jump || fastScaffold.keepY) {
                    return !fastScaffold.canFastScaffold();
                }
                return true;
            }
            if ((fastScaffold == FastScaffold.Float_A || fastScaffold == FastScaffold.Float_B) && fastScaffold.canFastScaffold()) {
                return true;
            }
        }
        return false;
    }

    public static EnumFacing faceFor(BlockPos pos, BlockPos only) {
        BlockPos down = pos.down();
        if (canPlaceAgainst(down) && (only == null || down.equals(only))) {
            return EnumFacing.UP;
        }
        double d = mc.thePlayer.posX - ((double) pos.getX() + 0.5);
        double d2 = mc.thePlayer.posZ - ((double) pos.getZ() + 0.5);
        EnumFacing facing = Math.abs(d) > Math.abs(d2) ? (d > 0.0 ? EnumFacing.EAST : EnumFacing.WEST) : (d2 > 0.0 ? EnumFacing.SOUTH : EnumFacing.NORTH);
        BlockPos toward = pos.offset(facing);
        if (canPlaceAgainst(toward) && (only == null || toward.equals(only))) {
            return facing.getOpposite();
        }
        for (EnumFacing side : SIDES) {
            if (side == facing) continue;
            BlockPos other = pos.offset(side);
            if (!canPlaceAgainst(other)) continue;
            if (only != null && !other.equals(only)) continue;
            return side.getOpposite();
        }
        return null;
    }

    public static void multiplyMotion(double d, boolean always) {
        if (!always && legitMotion.isToggled()) {
            return;
        }
        PlayerUtils.multiplyMotion(d);
    }

    private static double speedBonus() {
        int n = PlayerUtils.speedLevel();
        switch (n) {
            case 0:
                return 0.0;
            case 1:
                return 0.05;
            case 2:
                return speedBoost.getInput() / 10.0;
        }
        return 0.06 * (double) n;
    }

    private static void afterPlace(BlockPos pos, ItemStack itemStack) {
        if (fastScaffold == FastScaffold.Side && fastScaffold.canFastScaffold() && (fastScaffold.ticks == -1 || ++fastScaffold.ticks == 2 || fastScaffold.ticks >= 5)) {
            if (fastScaffold.ticks >= 5) {
                fastScaffold.ticks = 0;
            }
            double cx = (double) pos.getX() + 0.5;
            double cz = (double) pos.getZ() + 0.5;
            EnumFacing facing = null;
            switch (PlayerUtils.facing()) {
                case NORTH:
                case SOUTH:
                    facing = mc.thePlayer.posX > cx ? EnumFacing.EAST : EnumFacing.WEST;
                    break;
                case WEST:
                case EAST:
                    facing = mc.thePlayer.posZ > cz ? EnumFacing.SOUTH : EnumFacing.NORTH;
                    break;
                default:
                    break;
            }
            Vec3 vec3 = BlockUtils.hitVec(pos, facing);
            MovingObjectPosition hit = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.BLOCK, vec3, facing, pos);
            if (BlockUtils.isReplaceable(BlockUtils.placedPos(hit))) {
                PlayerUtils.place(hit, itemStack);
                PlayerUtils.swing(silentSwing.isToggled(), true);
                placedAt = System.currentTimeMillis();
                placed.put(pos, placedAt);
                if (fastScaffold.ticks == -1) {
                    fastScaffold.ticks = 0;
                }
                return;
            }
            fastScaffold.ticks = -1;
        }
        if (multiPlace != MultiPlace.Disabled && itemStack.stackSize > 0 && (TowerModule.towering || fastScaffold.canFastScaffold()) && placeExtra(itemStack)
                && multiPlace == MultiPlace._2_extra && itemStack.stackSize > 0) {
            placeExtra(itemStack);
        }
    }

    private static boolean canPlaceAgainst(BlockPos pos) {
        if (!placed.isEmpty() && !placed.containsKey(pos) && sincePlace() < 250L) {
            boolean near = false;
            for (Map.Entry<BlockPos, Long> entry : placed.entrySet()) {
                if (!(mc.thePlayer.getDistanceSq(entry.getKey()) < 12.0)) continue;
                near = true;
                break;
            }
            if (near) {
                return false;
            }
        }
        return !BlockUtils.isReplaceable(pos) && !BlockUtils.isInteractable(BlockUtils.getBlock(pos));
    }

    private static void postPlayerInput() {
        if (sneakQueued && sincePlace() > 90L) {
            sneakQueued = false;
            PlayerUtils.setSneak(true);
        }
        if (jumpState == 1) {
            if (PlayerUtils.jumpKeyDown()) {
                PlayerUtils.setJump(false);
                jumpState = 2;
            } else {
                jumpState = 0;
            }
        } else if (jumpState == 2) {
            PlayerUtils.setJump(true);
            jumpState = 0;
        }
        if (fastScaffold == FastScaffold.Float_A || fastScaffold == FastScaffold.Float_B) {
            if (!fastScaffold.canFastScaffold() && fastScaffold.state == 1) {
                fastScaffold.state = 0;
            }
        } else if ((fastScaffold.jump || fastScaffold.keepY) && legitMotion.isToggled() && fastScaffold.canFastScaffold() && mc.thePlayer.onGround && fastScaffold.state == 0) {
            PlayerUtils.setJump(true);
        }
    }

    private static double boosted(double d) {
        d += speedBonus();
        d += 1.0E-4 * Math.random();
        if (NoRotateModule.sinceSetback(500L)) {
            d *= PlayerUtils.speedLevel() > 0 ? 0.85 : 0.95;
        }
        return d;
    }

    private static void update() {
        if (showBlockCount.isToggled()) {
            updateCount();
        }
        if (pausedTicks > 0 && --pausedTicks != 0) {
            stop();
            return;
        }
        if (PacketUtils.sentCloseWindow || Overlay.screenOpen() || PlayerUtils.isAdventure()) {
            stop();
            return;
        }
        if (autoSwap.isToggled()) {
            AutoSwapModule.swap(SwapSlot.Scaffold);
        }
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        if (!ItemUtils.isScaffoldBlock(itemStack)) {
            if (!autoSwap.isToggled() || AutoSwapModule.countBlocks() == 0) {
                stop();
            }
            return;
        }
        long l = System.currentTimeMillis();
        if (!active) {
            startTick = mc.thePlayer.ticksExisted;
            startedAt = l;
            startPitch = mc.thePlayer.rotationPitch;
            KeyUtils.release(mc.gameSettings.keyBindAttack);
            KeyUtils.release(mc.gameSettings.keyBindUseItem);
            if (slowOnEnable.isToggled()) {
                multiplyMotion(0.2, true);
            }
        }
        active = true;
        fastScaffoldTick();
        if (xyz.daikirai.module.movement.LongJumpModule.throwTicks > 0) {
            delay = 0;
        } else if (delay > 0) {
            --delay;
            return;
        }
        if (!PacketUtils.noUseOrDig()) {
            return;
        }
        BlockPos below = BlockUtils.below(1.0);
        airBelowTicks = BlockUtils.isReplaceable(below) ? ++airBelowTicks : 0;
        BlockPos pos = place(null, itemStack);
        if (pos != null) {
            ++placedCount;
            if (!sneakEvery.isAtMin() && placedCount % (int) sneakEvery.getInput() == 0) {
                sneakQueued = true;
            }
            Irc.onScaffoldPlace();
            if (itemStack.stackSize > 0) {
                afterPlace(pos, itemStack);
            }
        }
    }

    private static MovingObjectPosition aim(BlockPos pos, EnumFacing facing, float offset, float lastYaw) {
        float[] r = RotationUtils.searchBlockRotation(pos, facing, offset, precision, lastYaw, SafeWalkModule.scaffoldActive);
        if (r == null) {
            return null;
        }
        aimYaw = r[0];
        aimPitch = r[1];
        if (rotation == Rotation.Smooth && lastPlaced != null && PlayerUtils.groundTicks <= 3L) {
            smoothLimit = 35.0f + (float) (1.0 * Math.random());
            smoothAt = System.currentTimeMillis();
            float f3 = MathHelper.wrapAngleTo180_float(aimYaw - lastYaw);
            if (Math.abs(f3) > smoothLimit) {
                return null;
            }
        }
        return PlayerUtils.rayTraceBlock(pos, null, mc.playerController.getBlockReachDistance(), aimYaw, aimPitch);
    }

    private static long sincePlace() {
        return Math.abs(System.currentTimeMillis() - placedAt);
    }

    public static boolean towerAllowed() {
        long l;
        return tower.isToggled() && active && ((l = sincePlace()) < 400L || l > 700L);
    }

    private static void preMotion(MotionEvent e) {
        boolean floatWasSet = FastScaffold.Float_A.state == 1 || FastScaffold.Float_B.state == 1;
        FastScaffold.Float_A.state = 0;
        FastScaffold.Float_B.state = 0;
        if (!fastScaffold.canFastScaffold()) {
            return;
        }
        if (fastScaffold == FastScaffold.Float_A) {
            if (e.y % 1.0 == 0.0 && mc.thePlayer.motionY == -0.0784000015258789) {
                if (Hypixel.onHypixel()) {
                    e.y += 1.0E-13;
                }
                fastScaffold.state = 1;
                if (PlayerUtils.isMoving() && !mc.thePlayer.isSneaking() && !NoRotateModule.sinceSetback(3000L)) {
                    double d = fastScaffold.sprint ? 0.2 + PlayerUtils.perSpeedLevel(PlayerUtils.isDiagonal() ? 0.035 : 0.043)
                            : 0.23 + PlayerUtils.perSpeedLevel(PlayerUtils.isDiagonal() ? 0.042 : 0.05);
                    if (PlayerUtils.speedLevel() == 0 && PlayerUtils.isDiagonal()) {
                        d *= 0.87;
                    }
                    MoveUtils.strafe(d);
                    if (!fastScaffold.sprint) {
                        mc.thePlayer.setSprinting(true);
                        mc.thePlayer.spawnRunningParticles();
                    }
                }
            }
        } else if (fastScaffold == FastScaffold.Float_B) {
            if (e.y % 1.0 == 0.0 && mc.thePlayer.motionY == -0.0784000015258789 && PlayerUtils.isMoving() && !mc.thePlayer.isSneaking()
                    && !NoRotateModule.sinceSetback(3000L)) {
                double d;
                if (!floatWasSet && (PlayerUtils.isDiagonal() || !PlayerUtils.isAirBelow())) {
                    if (Hypixel.onHypixel()) {
                        e.y += 0.001 + Double.parseDouble("0.0000" + PlayerUtils.idOffset());
                        e.onGround = false;
                    }
                    fastScaffold.state = 1;
                    d = 0.153;
                } else {
                    d = 0.245 + PlayerUtils.perSpeedLevel(0.025);
                }
                MoveUtils.strafe(d);
            }
        } else if ((fastScaffold.jump || fastScaffold.keepY) && mc.thePlayer.onGround && (jumpFacingForward.isToggled() || floatWasSet)) {
            if (fastScaffold.state == 1) {
                fastScaffold.state = 2;
                double d = fastScaffold.sprint && PlayerUtils.direction().forward ? 0.25 : (fastScaffold.keepY ? 0.54 : 0.4);
                jumpWith(d);
            }
            delay = legitTicks;
        }
    }

    private static float randomFloat(float a, float b) {
        return (float) ((double) a + (double) (b - a) * Math.random());
    }
}
