package openraven.ravenxd.util.rots.compat;

public class EventPlayerMove {
    private float forward, strafe;
    private boolean jump, sneak;
    private double sneakMultiplier;

    public EventPlayerMove(float forward, float strafe, boolean jump, boolean sneak, double sneakMultiplier) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sneakMultiplier = sneakMultiplier;
    }

    public float getForward() {
        return this.forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public boolean isJump() {
        return this.jump;
    }

    public boolean isSneak() {
        return this.sneak;
    }

    public double getSneakMultiplier() {
        return this.sneakMultiplier;
    }
}
