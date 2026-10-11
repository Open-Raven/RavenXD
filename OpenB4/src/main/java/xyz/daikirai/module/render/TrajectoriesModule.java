package xyz.daikirai.module.render;

import xyz.daikirai.client.BotCheck;
import xyz.daikirai.client.Chat;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.ButtonSetting;
import xyz.daikirai.util.Render3D;
import xyz.daikirai.util.RenderUtils;
import xyz.daikirai.util.Strings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class TrajectoriesModule extends Module {
    public static ButtonSetting autoScaleTarget, disableUnchargedBow, highlightOnEntity, drawBlockOutline, shortenLine, bow, egg, enderPearl,
            enderPearlThrown, fireballEnemy, fireballThrown, snowball;
    private static int held;
    private final int reach = 75;
    private BlockPos hitBlock;
    private final List<Entity> thrown = new ArrayList<Entity>();
    private final List<EntityLivingBase> aiming = new ArrayList<EntityLivingBase>();

    public TrajectoriesModule(int key) {
        super("Trajectories", Category.render, key);
    }

    public void link() {
        autoScaleTarget = (ButtonSetting) ModuleManager.setting(this, "Auto-scale target");
        disableUnchargedBow = (ButtonSetting) ModuleManager.setting(this, "Disable uncharged bow");
        highlightOnEntity = (ButtonSetting) ModuleManager.setting(this, "Highlight on entity");
        drawBlockOutline = (ButtonSetting) ModuleManager.setting(this, "Draw block outline");
        shortenLine = (ButtonSetting) ModuleManager.setting(this, "Shorten line");
        bow = (ButtonSetting) ModuleManager.setting(this, "Bow");
        egg = (ButtonSetting) ModuleManager.setting(this, "Egg");
        enderPearl = (ButtonSetting) ModuleManager.setting(this, "Ender pearl");
        enderPearlThrown = (ButtonSetting) ModuleManager.setting(this, "Ender pearl (thrown)");
        fireballEnemy = (ButtonSetting) ModuleManager.setting(this, "Fireball (enemy)");
        fireballThrown = (ButtonSetting) ModuleManager.setting(this, "Fireball (thrown)");
        snowball = (ButtonSetting) ModuleManager.setting(this, "Snowball");
    }

    @Override
    public void onUpdate() {
        held = heldType();
        if (!this.thrown.isEmpty()) {
            this.thrown.removeIf(e -> e == null || mc.theWorld.getEntityByID(e.getEntityId()) == null);
        }
        if (fireballEnemy.isToggled()) {
            this.aiming.clear();
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p != mc.thePlayer && p.deathTime == 0) {
                    ItemStack s = p.getHeldItem();
                    if (s != null && s.getItem() == Items.fire_charge && !BotCheck.isBot(p, false)) {
                        this.aiming.add(p);
                    }
                }
            }
        }
    }

    public static int heldType() {
        ItemStack s = mc.thePlayer.getHeldItem();
        if (s != null) {
            Item item = s.getItem();
            if (item != Items.bow) {
                if (item == Items.egg) {
                    return !egg.isToggled() ? 0 : 1;
                }
                if (item == Items.ender_pearl) {
                    return !enderPearl.isToggled() ? 0 : 1;
                }
                if (item == Items.snowball) {
                    return !snowball.isToggled() ? 0 : 1;
                }
            } else if (bow.isToggled()) {
                return disableUnchargedBow.isToggled() && !mc.thePlayer.isUsingItem() ? 0 : 2;
            }
        }
        return 0;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (!e.world.isRemote) {
            return;
        }
        if (e.entity == mc.thePlayer) {
            this.thrown.clear();
            return;
        }
        if (!Chat.inWorld()) {
            return;
        }
        if (e.entity instanceof EntityEnderPearl) {
            if (enderPearlThrown.isToggled() && !this.thrown.contains(e.entity)) {
                this.thrown.add(e.entity);
            }
        } else if (e.entity instanceof EntityLargeFireball && fireballThrown.isToggled() && !this.thrown.contains(e.entity)) {
            this.thrown.add(e.entity);
        }
    }

    private void timeLabel(BlockPos pos, double seconds, float pitch, float yaw, double cx, double cy, double cz) {
        String s = Strings.round(seconds, 1) + "s";
        double x = (double) pos.getX() + 0.5 - cx;
        double y = (double) pos.getY() + 0.5 - cy;
        double z = (double) pos.getZ() + 0.5 - cz;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-yaw, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(pitch, 1.0f, 0.0f, 0.0f);
        double d = mc.thePlayer.getDistanceSq(pos);
        float sc = (float) Math.min(0.025 * (1.0 + d / 500.0), 0.1);
        GlStateManager.scale(-sc, -sc, sc);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        int w = mc.fontRendererObj.getStringWidth(s);
        mc.fontRendererObj.drawString(s, (float) (-w / 2), -4.0f, -1, true);
        GlStateManager.disableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.popMatrix();
    }

    private void pearlPath(Entity e, double cx, double cy, double cz, float pitch, float yaw) {
        double mx = e.motionX;
        if (Double.isNaN(mx)) {
            return;
        }
        float r = 0.45f;
        float g = 0.15f;
        float b = 1.0f;
        GlStateManager.color(r, g, b, 0.8f);
        GL11.glLineWidth(2.0f);
        double my = e.motionY;
        double mz = e.motionZ;
        Vec3 pos = new Vec3(e.posX, e.posY, e.posZ);
        Vec3 hit = null;
        BlockPos block = null;
        GL11.glBegin(3);
        GL11.glVertex3d(pos.xCoord - cx, pos.yCoord - cy, pos.zCoord - cz);
        int ticks = 0;
        for (int i = 0; i < 100; ++i) {
            ++ticks;
            Vec3 next = new Vec3(pos.xCoord + mx, pos.yCoord + my, pos.zCoord + mz);
            MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(pos, next, false, true, false);
            if (mop != null && mop.hitVec != null) {
                next = hit = mop.hitVec;
                if (mop.getBlockPos() != null) {
                    block = mop.getBlockPos();
                }
                GL11.glVertex3d(next.xCoord - cx, next.yCoord - cy, next.zCoord - cz);
                break;
            }
            GL11.glVertex3d(next.xCoord - cx, next.yCoord - cy, next.zCoord - cz);
            pos = next;
            mx *= 0.99;
            my *= 0.99;
            mz *= 0.99;
            my -= 0.03;
            if (pos.yCoord < 0.0) break;
        }
        GL11.glEnd();
        if (hit != null) {
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            if (block != null) {
                this.outline(block, cx, cy, cz, r, g, b, true);
                this.timeLabel(block, (double) ticks / 20.0, pitch, yaw, cx, cy, cz);
            }
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
        }
    }

    private void outline(BlockPos pos, double cx, double cy, double cz, float r, float g, float b, boolean fill) {
        AxisAlignedBB bb = new AxisAlignedBB((double) pos.getX() - cx, (double) pos.getY() - cy, (double) pos.getZ() - cz,
                (double) (pos.getX() + 1) - cx, (double) (pos.getY() + 1) - cy, (double) (pos.getZ() + 1) - cz);
        if (fill) {
            GlStateManager.color(r, g, b, 0.5f);
            GL11.glLineWidth(2.0f);
            GL11.glBegin(7);
            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
            GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
            GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
            GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
            GL11.glEnd();
            GlStateManager.color(r, g, b, 0.8f);
        }
        RenderGlobal.drawSelectionBoundingBox(bb);
    }

    private void aimLine(EntityLivingBase e, double cx, double cy, double cz, float pt) {
        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * (double) pt;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * (double) pt + (double) e.getEyeHeight();
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * (double) pt;
        float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt;
        float pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt;
        double ry = Math.toRadians(yaw);
        double rp = Math.toRadians(pitch);
        double dx = -Math.sin(ry) * Math.cos(rp);
        double dy = -Math.sin(rp);
        double dz = Math.cos(ry) * Math.cos(rp);
        Vec3 from = new Vec3(x, y, z);
        Vec3 to = new Vec3(x + dx * (double) this.reach, y + dy * (double) this.reach, z + dz * (double) this.reach);
        this.hitBlock = null;
        Vec3 end = this.trace(e, from, to);
        float r = 1.0f;
        float g = 0.45f;
        float b = 0.0f;
        GlStateManager.color(r, g, b, 0.8f);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(1);
        GL11.glVertex3d(from.xCoord - cx, from.yCoord - cy, from.zCoord - cz);
        GL11.glVertex3d(end.xCoord - cx, end.yCoord - cy, end.zCoord - cz);
        GL11.glEnd();
        if (this.hitBlock != null) {
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            this.outline(this.hitBlock, cx, cy, cz, r, g, b, false);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
        }
    }

    private Vec3 trace(Entity entity, Vec3 from, Vec3 to) {
        Vec3 end;
        double best;
        BlockPos start = new BlockPos(MathHelper.floor_double(from.xCoord), MathHelper.floor_double(from.yCoord), MathHelper.floor_double(from.zCoord));
        Block block = mc.theWorld.getBlockState(start).getBlock();
        if (!(block instanceof BlockAir || block instanceof BlockLiquid || block instanceof BlockFire)) {
            this.hitBlock = start;
            return from;
        }
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(from, to, false, true, false);
        if (mop != null && mop.hitVec != null) {
            end = mop.hitVec;
            this.hitBlock = mop.getBlockPos();
            best = from.squareDistanceTo(end);
        } else {
            end = to;
            best = from.squareDistanceTo(to);
        }
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len != 0.0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        AxisAlignedBB area = entity.getEntityBoundingBox().addCoord(to.xCoord - from.xCoord, to.yCoord - from.yCoord, to.zCoord - from.zCoord).expand(1.0, 1.0, 1.0);
        for (Entity other : mc.theWorld.getEntitiesWithinAABBExcludingEntity(entity, area)) {
            double d;
            if (!other.canBeCollidedWith()) continue;
            float border = Math.max(other.getCollisionBorderSize(), 0.3f);
            AxisAlignedBB bb = other.getEntityBoundingBox().expand(border, border, border);
            if (bb.isVecInside(from)) {
                if (!(best > 0.0)) continue;
                this.hitBlock = start;
                end = from;
                best = from.squareDistanceTo(end);
                continue;
            }
            MovingObjectPosition hit = bb.calculateIntercept(from, to);
            if (hit == null || hit.hitVec == null || !((d = from.squareDistanceTo(hit.hitVec)) < best)) continue;
            double step = (double) border + 0.001;
            this.hitBlock = new BlockPos(MathHelper.floor_double(hit.hitVec.xCoord + dx * step), MathHelper.floor_double(hit.hitVec.yCoord + dy * step),
                    MathHelper.floor_double(hit.hitVec.zCoord + dz * step));
            AxisAlignedBB cell = new AxisAlignedBB(this.hitBlock.getX(), this.hitBlock.getY(), this.hitBlock.getZ(), this.hitBlock.getX() + 1,
                    this.hitBlock.getY() + 1, this.hitBlock.getZ() + 1);
            MovingObjectPosition cellHit = cell.calculateIntercept(from, to);
            end = cellHit != null && cellHit.hitVec != null ? cellHit.hitVec : hit.hitVec;
            best = d;
        }
        return end;
    }

    @Override
    public void guiButtonToggled(ButtonSetting b) {
        if (b == fireballEnemy && !b.isToggled()) {
            this.aiming.clear();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        if (held != 0) {
            float yaw = (float) Math.toRadians(mc.thePlayer.rotationYaw);
            float pitch = (float) Math.toRadians(mc.thePlayer.rotationPitch);
            RenderManager rm = mc.getRenderManager();
            double x = rm.viewerPosX - (double) (MathHelper.cos(yaw) * 0.16f);
            double y = rm.viewerPosY + (double) mc.thePlayer.getEyeHeight() - 0.1;
            double z = rm.viewerPosZ - (double) (MathHelper.sin(yaw) * 0.16f);
            boolean isBow = held == 2;
            float power = isBow ? 1.0f : 0.4f;
            float mx = -MathHelper.sin(yaw) * MathHelper.cos(pitch) * power;
            float my = -MathHelper.sin(pitch) * power;
            float mz = MathHelper.cos(yaw) * MathHelper.cos(pitch) * power;
            double len = Math.sqrt(mx * mx + my * my + mz * mz);
            mx = (float) ((double) mx / len);
            my = (float) ((double) my / len);
            mz = (float) ((double) mz / len);
            if (isBow) {
                float use = (float) (72000 - mc.thePlayer.getItemInUseCount()) + e.partialTicks;
                float f = use / 20.0f;
                if ((f = (f * f + f * 2.0f) / 3.0f) > 1.0f) {
                    f = 1.0f;
                }
                mx *= (f *= 3.0f);
                my *= f;
                mz *= f;
            } else {
                mx = (float) ((double) mx * 1.5);
                my = (float) ((double) my * 1.5);
                mz = (float) ((double) mz * 1.5);
            }
            GL11.glPushMatrix();
            GL11.glEnable(2848);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glEnable(32925);
            GL11.glDepthMask(false);
            double gravity = isBow ? 0.05 : 0.03;
            ArrayList<double[]> points = new ArrayList<double[]>();
            MovingObjectPosition blockHit = null;
            Entity entityHit = null;
            EnumFacing entityFace = null;
            EnumFacing blockFace = null;
            for (int i = 0; i < 100; ++i) {
                for (int n2 = 0; n2 < 2; ++n2) {
                    points.add(new double[]{x - rm.viewerPosX, y - rm.viewerPosY, z - rm.viewerPosZ});
                    Vec3 from = new Vec3(x, y, z);
                    Vec3 to = new Vec3(x + (double) (mx / (float) 2), y + (double) (my / (float) 2), z + (double) (mz / (float) 2));
                    double sx = mx / (float) 2;
                    double sy = my / (float) 2;
                    double sz = mz / (float) 2;
                    x = to.xCoord;
                    y = to.yCoord;
                    z = to.zCoord;
                    double drag = Math.pow(0.99, 1.0 / (double) 2);
                    mx = (float) ((double) mx * drag);
                    my = (float) ((double) my * drag);
                    mz = (float) ((double) mz * drag);
                    my = (float) ((double) my - gravity / (double) 2);
                    double h = 0.5;
                    AxisAlignedBB area = new AxisAlignedBB(x - h, y - h, z - h, x + h, y + h, z + h).addCoord(sx, sy, sz).expand(1.0, 1.0, 1.0);
                    List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), area);
                    double best = 0.0;
                    for (Entity other : list) {
                        if (!(other instanceof EntityLivingBase) || other instanceof EntityArmorStand || !other.canBeCollidedWith() || ((EntityLivingBase) other).deathTime != 0) continue;
                        if (other instanceof EntityPlayer && BotCheck.isBot(other, false)) continue;
                        AxisAlignedBB bb = other.getEntityBoundingBox().expand(0.3f, 0.3f, 0.3f);
                        MovingObjectPosition hit = bb.calculateIntercept(from, to);
                        if (hit == null) continue;
                        double d = from.squareDistanceTo(hit.hitVec);
                        if (entityHit != null && !(d < best)) continue;
                        entityHit = other;
                        entityFace = hit.sideHit;
                        best = d;
                    }
                    blockHit = mc.theWorld.rayTraceBlocks(from, to);
                    if (blockHit != null) {
                        blockFace = blockHit.sideHit;
                    }
                    if (entityHit != null || blockHit != null) break;
                }
                if (entityHit != null || blockHit != null) break;
            }
            if (entityHit != null && blockHit != null) {
                if (mc.thePlayer.getDistanceSqToEntity(entityHit) >= mc.thePlayer.getDistanceSqToCenter(blockHit.getBlockPos())) {
                    entityHit = null;
                    entityFace = null;
                } else {
                    blockHit = null;
                    blockFace = null;
                }
            }
            EnumFacing face = entityFace == null ? blockFace : entityFace;
            if (entityHit != null && highlightOnEntity.isToggled()) {
                Render3D.color(-55770);
            } else if (face == EnumFacing.UP) {
                Render3D.color(-14221530);
            } else {
                GlStateManager.color(1.0f, 1.0f, 1.0f);
            }
            GL11.glLineWidth(1.8f);
            GL11.glBegin(3);
            for (int i = 0; i < points.size(); ++i) {
                if (i == 0 && shortenLine.isToggled()) continue;
                double[] p = points.get(i);
                GL11.glVertex3d(p[0], p[1], p[2]);
            }
            double ex = x - rm.viewerPosX;
            double ey = y - rm.viewerPosY;
            double ez = z - rm.viewerPosZ;
            double dist = 0.0;
            if (entityHit != null) {
                dist = mc.thePlayer.getDistanceSq(entityHit.getPosition());
            } else if (blockHit != null) {
                dist = mc.thePlayer.getDistanceSq(blockHit.getBlockPos());
            }
            if (face != null) {
                double s = autoScaleTarget.isToggled() ? Math.min(0.1 * (1.0 + dist / 500.0), 0.5) : 0.1;
                switch (face) {
                    case WEST:
                    case EAST:
                        GL11.glVertex3d(ex, ey, ez);
                        GL11.glVertex3d(ex, ey - s, ez - s);
                        GL11.glVertex3d(ex, ey + s, ez + s);
                        GL11.glVertex3d(ex, ey, ez);
                        GL11.glVertex3d(ex, ey - s, ez + s);
                        GL11.glVertex3d(ex, ey + s, ez - s);
                        break;
                    case NORTH:
                    case SOUTH:
                        GL11.glVertex3d(ex, ey, ez);
                        GL11.glVertex3d(ex - s, ey - s, ez);
                        GL11.glVertex3d(ex + s, ey + s, ez);
                        GL11.glVertex3d(ex, ey, ez);
                        GL11.glVertex3d(ex + s, ey - s, ez);
                        GL11.glVertex3d(ex - s, ey + s, ez);
                        break;
                    case DOWN:
                    case UP:
                        GL11.glVertex3d(ex, ey, ez);
                        GL11.glVertex3d(ex - s, ey, ez - s);
                        GL11.glVertex3d(ex + s, ey, ez + s);
                        GL11.glVertex3d(ex, ey, ez);
                        GL11.glVertex3d(ex + s, ey, ez - s);
                        GL11.glVertex3d(ex - s, ey, ez + s);
                }
            }
            GlStateManager.resetColor();
            GL11.glEnd();
            GL11.glDisable(3042);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(32925);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            GL11.glPopMatrix();
            if (blockHit != null && face != null && drawBlockOutline.isToggled()) {
                int n3 = face == EnumFacing.UP ? -14221530 : -1;
                Render3D.face(blockHit.getBlockPos(), face, n3, true, false);
            }
        }
        if (!this.thrown.isEmpty() || !this.aiming.isEmpty()) {
            RenderManager rm = mc.getRenderManager();
            double cx = rm.viewerPosX;
            double cy = rm.viewerPosY;
            double cz = rm.viewerPosZ;
            GL11.glPushMatrix();
            GlStateManager.disableTexture2D();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            for (EntityLivingBase en : this.aiming) {
                this.aimLine(en, cx, cy, cz, e.partialTicks);
            }
            for (Entity en : this.thrown) {
                if (en instanceof EntityEnderPearl) {
                    this.pearlPath(en, cx, cy, cz, rm.playerViewX, rm.playerViewY);
                    continue;
                }
                if (!(en instanceof EntityLargeFireball)) continue;
                this.fireballPath(en, cx, cy, cz, rm.playerViewX, rm.playerViewY);
            }
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GlStateManager.enableTexture2D();
            GL11.glPopMatrix();
            RenderUtils.resetColor();
        }
    }

    public void cross(Vec3 v, double nx, double ny, double nz, double cx, double cy, double cz) {
        double s = 0.1;
        double x = v.xCoord - cx;
        double y = v.yCoord - cy;
        double z = v.zCoord - cz;
        double ux = 0.0;
        double uy = 1.0;
        double uz = 0.0;
        if (Math.abs(ny) > 0.95) {
            ux = 1.0;
            uy = 0.0;
        }
        double ax = ny * uz - nz * uy;
        double ay = nz * ux - nx * uz;
        double az = nx * uy - ny * ux;
        double len = Math.sqrt(ax * ax + ay * ay + az * az);
        if (len != 0.0) {
            ax /= len;
            ay /= len;
            az /= len;
            double bx = ay * nz - az * ny;
            double by = az * nx - ax * nz;
            double bz = ax * ny - ay * nx;
            double p1x = (ax + bx) * s;
            double p1y = (ay + by) * s;
            double p1z = (az + bz) * s;
            double p2x = (ax - bx) * s;
            double p2y = (ay - by) * s;
            double p2z = (az - bz) * s;
            GL11.glLineWidth(2.0f);
            GL11.glBegin(1);
            GL11.glVertex3d(x - p1x, y - p1y, z - p1z);
            GL11.glVertex3d(x + p1x, y + p1y, z + p1z);
            GL11.glVertex3d(x - p2x, y - p2y, z - p2z);
            GL11.glVertex3d(x + p2x, y + p2y, z + p2z);
            GL11.glEnd();
        }
    }

    private void fireballPath(Entity e, double cx, double cy, double cz, float pitch, float yaw) {
        float r = 1.0f;
        float g = 0.1f;
        float b = 0.1f;
        GlStateManager.color(r, g, b, 0.8f);
        double dx = e.posX - e.lastTickPosX;
        double dy = e.posY - e.lastTickPosY;
        double dz = e.posZ - e.lastTickPosZ;
        if (dx == 0.0 && dy == 0.0 && dz == 0.0) {
            dx = e.motionX;
            dy = e.motionY;
            dz = e.motionZ;
        }
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (speed == 0.0 || Double.isNaN(speed)) {
            return;
        }
        dx /= speed;
        dy /= speed;
        dz /= speed;
        Vec3 from = new Vec3(e.posX, e.posY, e.posZ);
        Vec3 to = new Vec3(e.posX + dx * (double) this.reach, e.posY + dy * (double) this.reach, e.posZ + dz * (double) this.reach);
        this.hitBlock = null;
        Vec3 end = this.trace(e, from, to);
        GL11.glLineWidth(3.0f);
        GL11.glBegin(1);
        GL11.glVertex3d(from.xCoord - cx, from.yCoord - cy, from.zCoord - cz);
        GL11.glVertex3d(end.xCoord - cx, end.yCoord - cy, end.zCoord - cz);
        GL11.glEnd();
        if (this.hitBlock == null) {
            return;
        }
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        this.outline(this.hitBlock, cx, cy, cz, r, g, b, true);
        double seconds = from.distanceTo(end) / speed / 20.0;
        this.timeLabel(this.hitBlock, seconds, pitch, yaw, cx, cy, cz);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
    }

    @Override
    public void onDisable() {
        held = 0;
        this.thrown.clear();
        this.aiming.clear();
    }
}
