package xyz.daikirai.event;

public class MotionEvent {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean sprinting;
    public boolean sneaking;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean sprinting, boolean sneaking) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.sprinting = sprinting;
        this.sneaking = sneaking;
    }

    public MotionEvent(Object[] values) {
        this.set(values);
    }

    public Object[] toArray() {
        return new Object[]{this.x, this.y, this.z, this.yaw, this.pitch, this.onGround, this.sprinting, this.sneaking};
    }

    public void set(Object[] values) {
        this.x = (Double) values[0];
        this.y = (Double) values[1];
        this.z = (Double) values[2];
        this.yaw = (Float) values[3];
        this.pitch = (Float) values[4];
        this.onGround = (Boolean) values[5];
        this.sprinting = (Boolean) values[6];
        this.sneaking = (Boolean) values[7];
    }
}
