package xyz.daikirai.script;

import java.util.HashMap;
import java.util.Map;

public enum ScriptEvent {
    PRE_UPDATE("onPreUpdate", 4827),
    PRE_MOTION("onPreMotion", 7314),
    POST_MOTION("onPostMotion", 1958),
    POST_PLAYER_INPUT("onPostPlayerInput", 8642),
    CHAT("onChat", 3271),
    MOUSE("onMouse", 5906),
    KEY("onKey", 2483),
    WORLD_JOIN("onWorldJoin", 9175),
    RENDER_TICK("onRenderTick", 4068),
    RENDER_WORLD("onRenderWorld", 7539),
    GUI_UPDATE("onGuiUpdate", 6812),
    PACKET_SENT("onPacketSent", 1397),
    PACKET_RECEIVED("onPacketReceived", 8246),
    GET_ROTATIONS("getRotations", 2754),
    GET_MOVEMENT_YAW("getMovementYaw", 9681),
    ANTI_CHEAT_FLAG("onAntiCheatFlag", 5143),
    BED_WARS_ALERT("onBedWarsAlert", 7925),
    LOCRAW("onLocRaw", 3469),
    IRC_MESSAGE("onIRCMessage", 6284);

    private static final Map<String, ScriptEvent> BY_NAME = byNameMap();

    public final String normalName;
    public final int salt;

    ScriptEvent(String normalName, int salt) {
        this.normalName = normalName;
        this.salt = salt;
    }

    public static ScriptEvent byName(String string) {
        return BY_NAME.get(string);
    }

    private static Map<String, ScriptEvent> byNameMap() {
        ScriptEvent[] values = values();
        HashMap<String, ScriptEvent> hashMap = new HashMap<String, ScriptEvent>(values.length, 1.0f);
        for (ScriptEvent e : values) {
            hashMap.put(e.normalName, e);
        }
        return hashMap;
    }

    public String deriveEncryptedName(String string) {
        String string2 = ScriptCrypto.md5(string + this.salt);
        return string2.length() == 32 ? "m" + string2.substring(0, 14) : null;
    }

    public String getMethodName(Script script) {
        return script.methodName(this);
    }

    public String getNormalName() {
        return this.normalName;
    }
}
