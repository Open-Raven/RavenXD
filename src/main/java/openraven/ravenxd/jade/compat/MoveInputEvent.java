package openraven.ravenxd.jade.compat;

public class MoveInputEvent extends Event {
    private float moveForward, moveStrafe;
    private boolean jumping, sneaking;

    public MoveInputEvent(float moveForward, float moveStrafe, boolean jumping, boolean sneaking) {
        this.moveForward = moveForward;
        this.moveStrafe = moveStrafe;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }

    public float getMoveForward() {
        return this.moveForward;
    }

    public void setMoveForward(float f) {
        this.moveForward = f;
    }

    public float getMoveStrafe() {
        return this.moveStrafe;
    }

    public void setMoveStrafe(float f) {
        this.moveStrafe = f;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    public void setJumping(boolean b) {
        this.jumping = b;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public void setSneaking(boolean b) {
        this.sneaking = b;
    }
}
