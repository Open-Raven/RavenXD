package xyz.daikirai.util;

public enum MoveDirection {
    NONE(false, false, false),
    FORWARD(true, false, false),
    FORWARD_LEFT(true, true, false),
    FORWARD_RIGHT(true, true, false),
    BACK(false, false, true),
    BACK_LEFT(false, true, true),
    BACK_RIGHT(false, true, true),
    LEFT(false, false, false),
    RIGHT(false, false, false);

    public final boolean forward;
    public final boolean diagonal;
    public final boolean backward;

    MoveDirection(boolean forward, boolean diagonal, boolean backward) {
        this.forward = forward;
        this.diagonal = diagonal;
        this.backward = backward;
    }
}
