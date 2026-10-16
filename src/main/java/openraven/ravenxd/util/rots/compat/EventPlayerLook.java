package openraven.ravenxd.util.rots.compat;

import org.lwjgl.util.vector.Vector2f;

public class EventPlayerLook {
    private Vector2f rotation;

    public EventPlayerLook(Vector2f rotation) {
        this.rotation = rotation;
    }

    public Vector2f getRotation() {
        return this.rotation;
    }

    public void setRotation(Vector2f rotation) {
        this.rotation = rotation;
    }
}
