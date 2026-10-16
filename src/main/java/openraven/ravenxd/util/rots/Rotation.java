package openraven.ravenxd.util.rots;

public class Rotation {
    public float yaw;
    public float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation plus(Rotation rotation) {
        return new Rotation(yaw + rotation.yaw, pitch + rotation.pitch);
    }

    public Rotation plus(float num) {
        return new Rotation(yaw + num, pitch + num);
    }

    @Override
    public String toString() {
        return "Rotation(" + yaw + ", " + pitch + ")";
    }
}