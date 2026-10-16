package openraven.ravenxd.util.rots;

import openraven.ravenxd.util.rots.compat.Compat;
import openraven.ravenxd.util.rots.stuff.MiauMinusRotationUtil;
import openraven.ravenxd.util.rots.stuff.RiseRotationUtil;
import openraven.ravenxd.util.rots.stuff.YuriRotationUtils;
import openraven.ravenxd.util.rots.stuff.RiseMovementFix;

import openraven.ravenxd.util.rots.compat.Client;
import openraven.ravenxd.util.rots.compat.MethodEventShim;
import openraven.ravenxd.util.rots.compat.EventUpdate;
import openraven.ravenxd.util.rots.compat.EventPlayerLook;
import openraven.ravenxd.util.rots.compat.EventPlayerMotion;
import openraven.ravenxd.util.rots.compat.EventPlayerMove;
import openraven.ravenxd.util.rots.compat.EventPlayerStrafe;
import openraven.ravenxd.util.IMinecraft;
import openraven.ravenxd.util.rots.raycast.RaycastUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.Function;

/*
wrapper around RiseRotationUtil and YuriRotationUtils (and miau minus now)
actual rotations come from quadrillionare 67 omnilarp client
gcd smoothing overshoot crap from yuri
multipoint from miau minus, might use as scaffold mode
half skidded from nebula
 */
public final class RotationManager implements IMinecraft {

    @Getter
    private boolean active;
    private boolean smoothed;

    public Vector2f rotations, lastRotations = new Vector2f(0, 0), targetRotations, lastServerRotations;

    private double rotationSpeed;
    private RiseMovementFix correctMovement = RiseMovementFix.OFF;
    private Function<Vector2f, Boolean> raycast;

    private float randomAngle;
    private final Vector2f offset = new Vector2f(0, 0);

    // opt in overshoot
    @Setter
    @Getter
    private YuriRotationUtils.OvershootHandler overshoot;

    public RotationManager() {}

    public void setRotations(final Vector2f rotations, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(rotations, rotationSpeed, correctMovement, null);
    }

    public void setRotations(final Vector2f rotations, final double rotationSpeed, final RiseMovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        this.targetRotations = rotations;
        this.rotationSpeed = rotationSpeed * 36;
        this.correctMovement = correctMovement;
        this.raycast = raycast;
        active = true;

        smooth();
    }

    public void setRotations(final float yaw, final float pitch, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(new Vector2f(yaw, pitch), rotationSpeed, correctMovement, null);
    }

    public void setRotations(final float yaw, final float pitch, final double rotationSpeed, final RiseMovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(yaw, pitch), rotationSpeed, correctMovement, raycast);
    }

    public void setRotations(final float[] rotations, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, correctMovement, null);
    }

    public void setRotations(final float[] rotations, final double rotationSpeed, final RiseMovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, correctMovement, raycast);
    }

    // force yaw
    public void setRotationYaw(final float yaw) {
        mc.thePlayer.rotationYaw = yaw;
        mc.thePlayer.rotationYawHead = yaw;
        if (rotations != null) rotations.setX(yaw);
        if (lastRotations != null) lastRotations.setX(yaw);
        if (targetRotations != null) targetRotations.setX(yaw);
    }
    // force pitch
    public void setRotationPitch(final float pitch) {
        mc.thePlayer.rotationPitch = pitch;
        Compat.renderPitchHead = pitch;
        if (rotations != null) rotations.setY(pitch);
        if (lastRotations != null) lastRotations.setY(pitch);
        if (targetRotations != null) targetRotations.setY(pitch);
    }

    /** Aim at a target, letting RiseRotationUtil work out the angles. */
    public void lookAt(final Entity entity, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(RiseRotationUtil.calculate(entity), rotationSpeed, correctMovement);
    }

    public void lookAt(final Entity entity, final boolean adaptive, final double range, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(RiseRotationUtil.calculate(entity, adaptive, range), rotationSpeed, correctMovement);
    }

    public void lookAt(final Vec3 target, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(RiseRotationUtil.calculate(target), rotationSpeed, correctMovement);
    }

    public void lookAt(final BlockPos target, final double rotationSpeed, final RiseMovementFix correctMovement) {
        setRotations(RiseRotationUtil.calculate(target), rotationSpeed, correctMovement);
    }

    // miau minus multipoint fallback
    public void lookAt(final Entity entity, final double horizontalMultipoint, final double verticalMultipoint,
                              final double range, final double rotationSpeed, final RiseMovementFix correctMovement) {
        if (entity == null) return;

        final float[] found = MiauMinusRotationUtil.getRotationsWithBackup(
                entity, horizontalMultipoint, verticalMultipoint,
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, range);

        if (found == null) {
            setRotations(RiseRotationUtil.calculate(entity), rotationSpeed, correctMovement);
            return;
        }

        setRotations(new Vector2f(found[0], found[1]), rotationSpeed, correctMovement,
                onTarget(entity, range));
    }

    // raycast
    public Function<Vector2f, Boolean> onTarget(final Entity entity, final double range) {
        return rotation -> RaycastUtil.hitsEntity(rotation, range, entity);
    }

    public void stop() {
        active = false;
        raycast = null;
        correctMovement = RiseMovementFix.OFF;
    }

    public Vector2f serverRotations() {
        return lastServerRotations == null ? new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch) : lastServerRotations;
    }


    //events
    public void onUpdate(final EventUpdate event) {
        if (!active || rotations == null || lastRotations == null || targetRotations == null || lastServerRotations == null) {
            rotations = lastRotations = targetRotations = lastServerRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        if (active) {
            smooth();
        }

        if (correctMovement == RiseMovementFix.BACKWARDS_SPRINT && active
                && Math.abs(rotations.x % 360 - MoveUtil.getDirectionYaw() % 360) > 45) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            mc.thePlayer.setSprinting(false);
        }
    }

    public void onMove(final EventPlayerMove event) {
        if (active && correctMovement == RiseMovementFix.NORMAL && rotations != null) {
            MoveUtil.fixMovement(event, rotations.x);
        }
    }

    public void onLook(final EventPlayerLook event) {
        if (active && rotations != null) {
            event.setRotation(rotations);
        }
    }

    public void onStrafe(final EventPlayerStrafe event) {
        if (active && rotations != null
                && (correctMovement == RiseMovementFix.NORMAL || correctMovement == RiseMovementFix.TRADITIONAL)) {
            event.setYaw(rotations.x);
        }
    }

    public void onMotion(final EventPlayerMotion event) {
        if (active && rotations != null) {
            final float yaw = rotations.x;
            final float pitch = rotations.y;

            event.setYaw(yaw);
            event.setPitch(pitch);

            mc.thePlayer.rotationYawHead = yaw;
            Compat.renderPitchHead = pitch;

            lastServerRotations = new Vector2f(yaw, pitch);

            if (Math.abs((rotations.x - mc.thePlayer.rotationYaw) % 360) < 1
                    && Math.abs(rotations.y - mc.thePlayer.rotationPitch) < 1) {
                active = false;
                correctDisabledRotations();
            }

            lastRotations = rotations;
        } else {
            lastRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        targetRotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        smoothed = false;
    }


    private void correctDisabledRotations() {
        final Vector2f current = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        final Vector2f fixed = RiseRotationUtil.resetRotation(RiseRotationUtil.applySensitivityPatch(current, lastRotations));

        mc.thePlayer.rotationYaw = fixed.x;
        mc.thePlayer.rotationPitch = fixed.y;
    }

    public void smooth() {
        if (smoothed) return;

        float targetYaw = targetRotations.x;
        float targetPitch = targetRotations.y;

        if (raycast != null && (Math.abs(targetYaw - rotations.x) > 5 || Math.abs(targetPitch - rotations.y) > 5)) {
            final Vector2f trueTarget = new Vector2f(targetRotations.getX(), targetRotations.getY());
            final double speed = (Math.random() * Math.random() * Math.random()) * 20;

            randomAngle += (float) ((20 + (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360))
                    * (mc.thePlayer.ticksExisted / 10 % 2 == 0 ? -1 : 1));

            nudgeOffset(speed);
            targetYaw += offset.getX();
            targetPitch += offset.getY();

            if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                randomAngle = (float) Math.toDegrees(Math.atan2(trueTarget.getX() - targetYaw, targetPitch - trueTarget.getY())) - 180;

                targetYaw -= offset.getX();
                targetPitch -= offset.getY();

                nudgeOffset(speed);
                targetYaw += offset.getX();
                targetPitch += offset.getY();
            }

            if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                offset.setX(0);
                offset.setY(0);

                targetYaw = (float) (targetRotations.x + Math.random() * 2);
                targetPitch = (float) (targetRotations.y + Math.random() * 2);
            }
        }

        rotations = RiseRotationUtil.smooth(new Vector2f(targetYaw, targetPitch), rotationSpeed + Math.random());

        if (overshoot != null) {
            rotations = overshoot.apply(rotations);
        }

        if (correctMovement == RiseMovementFix.NORMAL || correctMovement == RiseMovementFix.TRADITIONAL) {
            Compat.movementYaw = rotations.x;
        }
        Compat.velocityYaw = rotations.x;

        smoothed = true;
        mc.entityRenderer.getMouseOver(1);
    }

    private void nudgeOffset(final double speed) {
        offset.setX((float) (offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed));
        offset.setY((float) (offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed));
    }

    public void init() {
        final MethodEventShim shim = Client.getInstance().getShim();
        shim.subscribe(EventUpdate.class, this::onUpdate);
        shim.subscribe(EventPlayerMove.class, this::onMove);
        shim.subscribe(EventPlayerLook.class, this::onLook);
        shim.subscribe(EventPlayerStrafe.class, this::onStrafe);
        shim.subscribe(EventPlayerMotion.class, this::onMotion);
    }
}
