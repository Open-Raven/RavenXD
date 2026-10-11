package xyz.daikirai.module.render;

import xyz.daikirai.util.PlayerUtils;
import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.client.Friends;
import xyz.daikirai.client.Hypixel;
import xyz.daikirai.client.Overlay;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.module.setting.SliderSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class IndicatorsModule extends Module {
    public enum Arrow {
        Caret, Greater_than, Triangle
    }

    public enum Kind {
        Arrow(Items.arrow, null),
        Fireball(Items.fire_charge, new Color(255, 150, 0, 255)),
        Ender_pearl(Items.ender_pearl, new Color(210, 0, 255, 255)),
        Player(null, null);

        public final ItemStack item;
        public final Color color;

        Kind(Item item, Color color) {
            this.item = item == null ? null : new ItemStack(item);
            this.color = color;
        }
    }

    public static ButtonSetting renderArrows, renderEnderPearls, renderFireballs, itemColors, renderItem, renderOnlyThreats;
    public static SliderSetting arrowSetting, circleRadius;
    public static Arrow arrow = Arrow.Caret;

    private final List<Entity> tracked = new ArrayList<Entity>();
    private final long throwGrace = 400L;
    private final double threatDistSq = Math.pow(7.0, 2.0);
    private final long ownDistSq = 8L;
    private final Field inGround = ReflectionHelper.findField(EntityArrow.class, "inGround", "field_70254_i");

    public IndicatorsModule(int key) {
        super("Indicators", Category.render, key);
    }

    public void link() {
        renderArrows = (ButtonSetting) ModuleManager.setting(this, "Render arrows");
        renderEnderPearls = (ButtonSetting) ModuleManager.setting(this, "Render ender pearls");
        renderFireballs = (ButtonSetting) ModuleManager.setting(this, "Render fireballs");
        arrowSetting = (SliderSetting) ModuleManager.setting(this, "Arrow");
        circleRadius = (SliderSetting) ModuleManager.setting(this, "Circle radius");
        itemColors = (ButtonSetting) ModuleManager.setting(this, "Item colors");
        renderItem = (ButtonSetting) ModuleManager.setting(this, "Render item");
        renderOnlyThreats = (ButtonSetting) ModuleManager.setting(this, "Render only threats");
    }

    @Override
    public void guiUpdate() {
        arrow = Arrow.values()[(int) arrowSetting.getInput()];
    }

    private void render(Entity entity, Kind kind, float pt) {
        if (!this.shouldRender(entity, kind)) {
            return;
        }
        int n = itemColors.isToggled() && kind.color != null ? kind.color.getRGB() : -1;
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) pt;
        double y = (double) (entity.height / 2.0f) + entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) pt;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) pt;
        ScaledResolution sr = new ScaledResolution(mc);
        Vec3 vec3 = Render3D.project(x, y, z, sr.getScaleFactor(), pt);
        if (vec3 == null) {
            return;
        }
        double dx = vec3.xCoord - (double) sr.getScaledWidth() / 2.0;
        double dy = vec3.yCoord - (double) sr.getScaledHeight() / 2.0;
        boolean inFront = vec3.zCoord < 1.0003684;
        if (!inFront) {
            dx *= -1.0;
            dy *= -1.0;
        }
        double angle = Math.atan2(dx, dy);
        double rot = Math.atan2(dy, dx) * (double) 57.29578f + 90.0;
        double dist = Math.hypot(dx, dy);
        double r = circleRadius.getInput();
        if (renderItem.isToggled()) {
            r += 20.0;
        }
        if (inFront && dist < r + 15.0) {
            return;
        }
        double cx = (double) sr.getScaledWidth() / 2.0;
        double cy = (double) sr.getScaledHeight() / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double px = cx + r * sin;
        double py = cy + r * cos;
        GlStateManager.pushMatrix();
        GlStateManager.translate(px, py, 0.0);
        GlStateManager.rotate((float) rot, 0.0f, 0.0f, 1.0f);
        GlStateManager.scale(1.0f, 1.0f, 1.0f);
        if (arrow == Arrow.Caret) {
            if (n == -1) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                GlStateManager.color(kind.color.getRed() / 255.0f, kind.color.getGreen() / 255.0f, kind.color.getBlue() / 255.0f);
            }
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            double a = 35.0f * ((float) Math.PI / 180);
            double len = 9.0;
            double off = 5.0;
            GL11.glLineWidth(3.0f);
            GL11.glBegin(3);
            GL11.glVertex2d(Math.sin(-a) * len, Math.cos(-a) * len - off);
            GL11.glVertex2d(0.0, -off);
            GL11.glVertex2d(Math.sin(a) * len, Math.cos(a) * len - off);
            GL11.glEnd();
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glDisable(2848);
            if (n != -1) {
                RenderUtils.resetColor();
            }
        } else if (arrow == Arrow.Greater_than) {
            GlStateManager.rotate(-90.0f, 0.0f, 0.0f, 1.0f);
            GlStateManager.scale(1.5, 1.5, 1.5);
            mc.fontRendererObj.drawString(">", -2.0f, -4.0f, n, false);
            if (n != -1) {
                RenderUtils.resetColor();
            }
        } else if (arrow == Arrow.Triangle) {
            Render3D.circle(0.0, 0.0, 5.0, 3, n);
        }
        GlStateManager.popMatrix();
        px = cx + (r - 13.0) * sin;
        py = cy + (r - 13.0) * cos;
        GlStateManager.pushMatrix();
        GlStateManager.translate(px, py, 0.0);
        GlStateManager.scale(0.8, 0.8, 0.8);
        String s = (int) mc.thePlayer.getDistanceToEntity(entity) + "m";
        mc.fontRendererObj.drawString(s, (float) (-mc.fontRendererObj.getStringWidth(s)) / 2.0f, -4.0f, -1, true);
        GlStateManager.popMatrix();
        if (renderItem.isToggled()) {
            GlStateManager.pushMatrix();
            if (kind == Kind.Arrow) {
                px = cx + (r - 26.0) * sin;
                py = cy + (r - 26.0) * cos;
                GlStateManager.translate(px, py, 0.0);
                GlStateManager.scale(1.0f, 1.0f, 1.0f);
                GlStateManager.rotate((float) rot - 45.0f, 0.0f, 0.0f, 1.0f);
                mc.getRenderItem().renderItemIntoGUI(kind.item, -12, -4);
            } else {
                px = cx + (r - 29.0) * sin;
                py = cy + (r - 29.0) * cos;
                GlStateManager.translate(px, py, 0.0);
                GlStateManager.scale(1.0f, 1.0f, 1.0f);
                mc.getRenderItem().renderItemIntoGUI(kind.item, -8, -9);
            }
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (!e.world.isRemote) {
            return;
        }
        if (e.entity == mc.thePlayer) {
            this.tracked.clear();
        } else if (Chat.inWorld()) {
            if (e.entity instanceof EntityArrow) {
                if (renderArrows.isToggled() && mc.thePlayer.getDistanceSqToEntity(e.entity) > this.threatDistSq && !this.tracked.contains(e.entity)) {
                    this.tracked.add(e.entity);
                }
            } else if (e.entity instanceof EntityEnderPearl) {
                if (renderEnderPearls.isToggled() && (since(PlayerUtils.lastPearl) > this.throwGrace || mc.thePlayer.getDistanceSqToEntity(e.entity) > (double) this.ownDistSq)
                        && !this.tracked.contains(e.entity)) {
                    this.tracked.add(e.entity);
                }
            } else if (e.entity instanceof EntityLargeFireball && renderFireballs.isToggled()
                    && (since(PlayerUtils.lastFireball) > this.throwGrace || mc.thePlayer.getDistanceSqToEntity(e.entity) > (double) this.ownDistSq)
                    && !this.tracked.contains(e.entity)) {
                this.tracked.add(e.entity);
            }
        }
    }

    private static long since(long t) {
        return Math.abs(System.currentTimeMillis() - t);
    }

    private boolean shouldRender(Entity entity, Kind kind) {
        if (renderOnlyThreats.isToggled() && kind == Kind.Fireball) {
            double range = 200.0;
            double step = 0.5;
            double x = entity.posX;
            double y = entity.posY;
            double z = entity.posZ;
            double mx = x - entity.lastTickPosX;
            double my = y - entity.lastTickPosY;
            double mz = z - entity.lastTickPosZ;
            if (mx != 0.0 || my != 0.0 || mz != 0.0) {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                int i = 0;
                while ((double) i < range / step) {
                    double d = mc.thePlayer.getDistanceSq(x, y, z);
                    if (d <= this.threatDistSq) {
                        return true;
                    }
                    pos.set(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (!(block instanceof BlockAir) && !(block instanceof BlockLiquid) && !(block instanceof BlockFire)) break;
                    x += mx * step;
                    y += my * step;
                    z += mz * step;
                    ++i;
                }
            }
            return false;
        }
        if (kind == Kind.Player) {
            EntityPlayer p = (EntityPlayer) entity;
            if (Friends.isFriend(p.getName())) {
                return false;
            }
            if (BotCheck.aboveSpawn(entity) || BotCheck.isBot(entity, false)) {
                return false;
            }
            if (Hypixel.sameTeam(p)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Chat.inWorld() || Overlay.screenOpen()) {
            return;
        }
        for (Entity entity : this.tracked) {
            Kind kind = null;
            if (entity instanceof EntityArrow) {
                try {
                    if (!this.inGround.getBoolean(entity)) {
                        kind = Kind.Arrow;
                    }
                } catch (IllegalAccessException ignored) {
                }
            } else if (entity instanceof EntityEnderPearl) {
                kind = Kind.Ender_pearl;
            } else if (entity instanceof EntityLargeFireball) {
                kind = Kind.Fireball;
            }
            if (kind == null) continue;
            this.render(entity, kind, e.renderTickTime);
        }
    }

    @Override
    public void onDisable() {
        this.tracked.clear();
    }

    @Override
    public void onUpdate() {
        if (!this.tracked.isEmpty()) {
            this.tracked.removeIf(en -> en == null || mc.theWorld.getEntityByID(en.getEntityId()) == null);
        }
    }
}
