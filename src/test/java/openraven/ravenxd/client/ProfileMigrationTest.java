package openraven.ravenxd.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileMigrationTest {
    private JsonArray parse(String json) {
        return new JsonParser().parse(json.replace('\'', '"')).getAsJsonArray();
    }

    @Test void enabledLegacyOverridesVelocityRegardlessOfOrder() {
        String legacy = "{'name':'AntiKnockback','enabled':true,'hidden':true,'keybind':42,'Horizontal':20,'Delay':500}";
        String velocity = "{'name':'Velocity','enabled':true,'keybind':12,'Horizontal':90,'Chance':75}";
        for (String input : new String[]{"[" + legacy + "," + velocity + "]", "[" + velocity + "," + legacy + "]"}) {
            JsonArray source = parse(input);
            String original = source.toString();
            JsonArray migrated = ProfileMigration.migrateModules(source, 16);
            assertEquals(1, migrated.size());
            JsonObject entry = migrated.get(0).getAsJsonObject();
            assertEquals("Velocity", entry.get("name").getAsString());
            assertEquals(16, entry.get("Mode").getAsInt());
            assertTrue(entry.get("enabled").getAsBoolean());
            assertTrue(entry.get("hidden").getAsBoolean());
            assertEquals(42, entry.get("keybind").getAsInt());
            assertEquals(20, entry.get("Horizontal").getAsInt());
            assertEquals(500, entry.get("Delay").getAsInt());
            assertEquals(75, entry.get("Chance").getAsInt());
            assertEquals(original, source.toString(), "Loading must not rewrite the input profile");
        }
    }

    @Test void disabledLegacyDoesNotOverrideActiveVelocity() {
        JsonArray migrated = ProfileMigration.migrateModules(parse("["
                + "{'name':'AntiKnockback','enabled':false,'keybind':42,'Horizontal':20,'Delay':500},"
                + "{'name':'Velocity','enabled':true,'keybind':12,'Horizontal':90}]"), 16);
        JsonObject entry = migrated.get(0).getAsJsonObject();
        assertEquals(0, entry.get("Mode").getAsInt());
        assertTrue(entry.get("enabled").getAsBoolean());
        assertEquals(12, entry.get("keybind").getAsInt());
        assertEquals(90, entry.get("Horizontal").getAsInt());
        assertEquals(500, entry.get("Delay").getAsInt());
    }

    @Test void legacyOnlyPreservesDisabledStateAndBind() {
        JsonObject entry = ProfileMigration.migrateModules(parse("["
                + "{'name':'AntiKnockback','enabled':false,'keybind':42}]"), 16).get(0).getAsJsonObject();
        assertEquals(16, entry.get("Mode").getAsInt());
        assertFalse(entry.get("enabled").getAsBoolean());
        assertEquals(42, entry.get("keybind").getAsInt());
    }

    @Test void modernVelocityWinsOverStaleLegacyEntry() {
        JsonArray source = parse("[{'name':'Sprint','enabled':true},"
                + "{'name':'AntiKnockback','enabled':true,'keybind':42},"
                + "{'name':'Velocity','Mode':7,'enabled':false,'keybind':12}]");
        JsonArray migrated = ProfileMigration.migrateModules(source, 16);
        assertEquals(2, migrated.size());
        assertEquals(source.get(0), migrated.get(0));
        assertEquals(source.get(2), migrated.get(1));
        assertEquals(migrated, ProfileMigration.migrateModules(migrated, 16));
    }

    @Test void profilesWithoutLegacyEntryRemainUnchanged() {
        JsonArray source = parse("[{'name':'Velocity','Mode':3,'enabled':true}]");
        assertEquals(source, ProfileMigration.migrateModules(source, 16));
    }

    @Test void activeCustomDisablerMigratesWithItsSettingsAndBindInEitherOrder() {
        String legacy = "{'name':'Custom Disabler','enabled':true,'keybind':42,'C03PacketPlayer':true}";
        String disabler = "{'name':'Disabler','Mode':1,'enabled':false,'keybind':12,'Reach':true}";
        for (String input : new String[]{"[" + legacy + "," + disabler + "]", "[" + disabler + "," + legacy + "]"}) {
            JsonArray source = parse(input);
            String original = source.toString();
            JsonArray migrated = ProfileMigration.migrateDisabler(source, 3);
            assertEquals(1, migrated.size());
            JsonObject entry = migrated.get(0).getAsJsonObject();
            assertEquals("Disabler", entry.get("name").getAsString());
            assertEquals(3, entry.get("Mode").getAsInt());
            assertTrue(entry.get("enabled").getAsBoolean());
            assertEquals(42, entry.get("keybind").getAsInt());
            assertTrue(entry.get("C03PacketPlayer").getAsBoolean());
            assertTrue(entry.get("Reach").getAsBoolean());
            assertEquals(original, source.toString());
            assertEquals(migrated, ProfileMigration.migrateDisabler(migrated, 3));
        }
    }

    @Test void existingActivePresetWinsWhenBothDisablersWereEnabled() {
        JsonObject entry = ProfileMigration.migrateDisabler(parse("["
                + "{'name':'Custom Disabler','enabled':true,'keybind':42,'C00PacketKeepAlive':true},"
                + "{'name':'Disabler','Mode':2,'enabled':true,'keybind':12}]"), 3).get(0).getAsJsonObject();
        assertEquals(2, entry.get("Mode").getAsInt());
        assertTrue(entry.get("enabled").getAsBoolean());
        assertEquals(12, entry.get("keybind").getAsInt());
        assertTrue(entry.get("C00PacketKeepAlive").getAsBoolean());
    }

    @Test void standaloneDisabledCustomDisablerKeepsBindAndDoesNotEnableItself() {
        JsonObject entry = ProfileMigration.migrateDisabler(parse("["
                + "{'name':'Custom Disabler','enabled':false,'keybind':42,'C03PacketPlayer':true}]"), 3)
                .get(0).getAsJsonObject();
        assertEquals(3, entry.get("Mode").getAsInt());
        assertFalse(entry.get("enabled").getAsBoolean());
        assertEquals(42, entry.get("keybind").getAsInt());
        assertTrue(entry.get("C03PacketPlayer").getAsBoolean());
    }

    @Test void modernCustomModeWinsOverStaleStandaloneEntry() {
        JsonArray source = parse("[{'name':'Sprint','enabled':true},"
                + "{'name':'Custom Disabler','enabled':true,'keybind':42,'C03PacketPlayer':true},"
                + "{'name':'Disabler','Mode':3,'enabled':false,'keybind':12,'C03PacketPlayer':false}]");
        JsonArray migrated = ProfileMigration.migrateDisabler(source, 3);
        assertEquals(2, migrated.size());
        assertEquals(source.get(0), migrated.get(0));
        assertEquals(source.get(2), migrated.get(1));
    }
}
