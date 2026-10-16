package openraven.ravenxd.util.rots.stuff;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RiseMovementFix {
    OFF("Off"),
    NORMAL("OpenRise"),
    TRADITIONAL("Traditional"),
    BACKWARDS_SPRINT("Backwards Sprint");

    final String name;

    @Override
    public String toString() {
        return name;
    }
}