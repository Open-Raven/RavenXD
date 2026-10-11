package xyz.daikirai.util;

public enum RotationSource {
    SCAFFOLD(true),
    LONGJUMP(true),
    WATERBUCKET(false),
    NOSLOW(false),
    AIMASSIST(true),
    FREECAM(false),
    SCRIPT(true),
    BEDAURA(true),
    KILLAURA(true),
    AUTOLADDER(true),
    ANTIFIREBALL(true),
    AUTOBLOCKIN(true),
    QUAKECRAFT(true),
    RODAIMBOT(true),
    WOOLWARS(true),
    INVMANAGER(true),
    AUTOROD(true),
    DISPLACE(true),
    JUMP45(true);

    public final boolean movementFix;

    RotationSource(boolean movementFix) {
        this.movementFix = movementFix;
    }
}
