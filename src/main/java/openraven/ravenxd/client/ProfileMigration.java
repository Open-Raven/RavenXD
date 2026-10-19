package openraven.ravenxd.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

final class ProfileMigration {
    private ProfileMigration() {}

    static JsonArray migrateModules(JsonArray modules, int antiKnockbackMode) {
        JsonObject legacy = null, velocity = null;
        for (JsonElement element : modules) {
            JsonObject entry = element.getAsJsonObject();
            String name = entry.get("name").getAsString();
            if (name.equals("AntiKnockback")) legacy = entry;
            if (name.equals("Velocity")) velocity = entry;
        }
        if (legacy == null) return modules;

        JsonObject merged = new JsonObject();
        boolean modern = velocity != null && velocity.has("Mode");
        boolean useLegacy = !modern && (velocity == null || enabled(legacy));
        if (!modern) copy(legacy, merged);
        if (velocity != null) copy(velocity, merged);
        if (useLegacy) copy(legacy, merged);
        merged.addProperty("name", "Velocity");
        if (!modern) merged.addProperty("Mode", useLegacy ? antiKnockbackMode : 0);

        return replaceModules(modules, "AntiKnockback", "Velocity", merged);
    }

    static JsonArray migrateDisabler(JsonArray modules, int customMode) {
        JsonObject legacy = null, disabler = null;
        for (JsonElement element : modules) {
            JsonObject entry = element.getAsJsonObject();
            String name = entry.get("name").getAsString();
            if (name.equals("Custom Disabler")) legacy = entry;
            if (name.equals("Disabler")) disabler = entry;
        }
        if (legacy == null) return modules;

        boolean alreadyCustom = disabler != null && disabler.has("Mode")
                && disabler.get("Mode").getAsInt() == customMode;
        // Keep the preset if both old modules were enabled.
        boolean useLegacy = !alreadyCustom && (disabler == null || enabled(legacy) && !enabled(disabler));
        JsonObject merged = new JsonObject();
        copy(legacy, merged);
        if (disabler != null) copy(disabler, merged);
        if (useLegacy) copy(legacy, merged);
        merged.addProperty("name", "Disabler");
        if (useLegacy) merged.addProperty("Mode", customMode);
        return replaceModules(modules, "Custom Disabler", "Disabler", merged);
    }

    private static JsonArray replaceModules(JsonArray modules, String legacyName, String currentName, JsonObject merged) {
        JsonArray result = new JsonArray();
        boolean inserted = false;
        for (JsonElement element : modules) {
            String name = element.getAsJsonObject().get("name").getAsString();
            if (name.equals(legacyName) || name.equals(currentName)) {
                if (!inserted) result.add(merged);
                inserted = true;
            } else {
                result.add(element);
            }
        }
        return result;
    }

    private static boolean enabled(JsonObject entry) {
        return entry.has("enabled") && !entry.get("enabled").isJsonNull()
                && entry.get("enabled").getAsBoolean();
    }

    private static void copy(JsonObject source, JsonObject target) {
        for (Map.Entry<String, JsonElement> property : source.entrySet()) {
            target.add(property.getKey(), property.getValue());
        }
    }
}
