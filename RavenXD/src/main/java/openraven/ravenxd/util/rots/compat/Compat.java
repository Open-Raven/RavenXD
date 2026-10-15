package openraven.ravenxd.util.rots.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Timer;
import openraven.ravenxd.util.MoveUtils;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

// stuff that i patched in directly to mcp in kayasaka
public final class Compat {
    // entityplayer sp
    public static float renderPitchHead;
    public static float movementYaw;
    public static float velocityYaw;
    // getPreviousRotation()
    public static Vector2f previousRotation;

    private Compat() {
    }

    public static Vector3D position(Entity e) {
        return new Vector3D(e.posX, e.posY, e.posZ);
    }

    public static Vector2f previousRotation() {
        if (previousRotation == null) {
            Minecraft mc = Minecraft.getMinecraft();
            return new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
        return previousRotation;
    }

    // prot in vanilla
    public static Vec3 vectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292f - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292f - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292f);
        float f3 = MathHelper.sin(-pitch * 0.017453292f);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static Timer timer() {
        return MoveUtils.timer();
    }

    // block raytrace
    public static MovingObjectPosition rayTraceCustom(Entity e, double range, float yaw, float pitch) {
        Vec3 eyes = e.getPositionEyes(1.0f);
        Vec3 look = vectorForRotation(pitch, yaw);
        Vec3 end = eyes.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        return e.worldObj.rayTraceBlocks(eyes, end, false, false, true);
    }

    // player distance
    public static double threadDistance(Entity e) {
        return Minecraft.getMinecraft().thePlayer.getDistanceToEntity(e);
    }
}
