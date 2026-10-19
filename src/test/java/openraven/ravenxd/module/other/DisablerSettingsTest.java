package openraven.ravenxd.module.other;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import openraven.ravenxd.module.setting.ButtonSetting;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DisablerSettingsTest {
    @Test void customModeAndAllPacketSettingsAreRegisteredOnDisablerOnly() throws Exception {
        JsonArray modules;
        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(
                "/assets/clickgui/modules.json"), StandardCharsets.UTF_8)) {
            modules = new JsonParser().parse(reader).getAsJsonArray();
        }
        JsonObject disabler = null;
        for (JsonElement element : modules) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.get("name").isJsonNull()) continue; // The shader placeholder has no module name.
            assertNotEquals("Custom Disabler", entry.get("name").getAsString());
            if (entry.get("name").getAsString().equals("Disabler")) disabler = entry;
        }
        assertNotNull(disabler);
        Map<String, ButtonSetting> toggles = new HashMap<>();
        JsonArray options = null;
        for (JsonElement element : disabler.getAsJsonArray("settings")) {
            JsonObject setting = element.getAsJsonObject();
            String name = setting.get("name").getAsString();
            if (name.equals("Mode")) {
                options = setting.getAsJsonArray("options");
                assertEquals(3, setting.get("max").getAsInt());
            }
            if (setting.get("t").getAsString().equals("toggle")) {
                assertFalse(toggles.containsKey(name), "Duplicate setting: " + name);
                toggles.put(name, new ButtonSetting(name, setting.get("value").getAsBoolean()));
            }
        }
        assertNotNull(options);
        assertEquals(4, options.size());
        assertEquals("Verus", options.get(0).getAsString());
        assertEquals("Vulcan", options.get(1).getAsString());
        assertEquals("OldMatrix", options.get(2).getAsString());
        assertEquals("Custom", options.get(3).getAsString());
        int[] linked = {0};
        new CustomPacketFilter(name -> {
            ButtonSetting setting = toggles.get(name);
            assertNotNull(setting, "Missing Custom packet setting: " + name);
            assertFalse(setting.isToggled(), "Custom packet filter must default to off: " + name);
            linked[0]++;
            return setting;
        });
        assertEquals(25, linked[0]);
    }
}
