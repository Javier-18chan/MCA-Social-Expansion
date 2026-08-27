package com.joelj.mcasocial.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HumorActionCatalog {
    private static final String RESOURCE = "data/mcasocial/social_actions/humor_actions.json";
    private static final Map<String, Definition> ACTIONS = load();

    private HumorActionCatalog() {}

    public static Definition get(String id) {
        return ACTIONS.get(id);
    }

    public static Collection<Definition> all() {
        return ACTIONS.values();
    }

    public static boolean contains(String id) {
        return ACTIONS.containsKey(id);
    }

    public static int relationshipLevel(int value) {
        if (value >= 100) return 5;
        if (value >= 75) return 4;
        if (value >= 50) return 3;
        if (value >= 25) return 2;
        return 1;
    }

    private static Map<String, Definition> load() {
        InputStream stream = HumorActionCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing humor action catalog: " + RESOURCE);
        }

        Map<String, Definition> actions = new LinkedHashMap<>();
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("actions");
            for (JsonElement element : array) {
                JsonObject value = element.getAsJsonObject();
                Definition definition = new Definition(
                        string(value, "id"),
                        string(value, "label_key"),
                        integer(value, "affinity_points"),
                        integer(value, "reject_points"),
                        value.get("success_chance").getAsDouble(),
                        integer(value, "cooldown_seconds"),
                        integer(value, "neutral_limit"),
                        integer(value, "harto_limit")
                );
                actions.put(definition.id(), definition);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid humor action catalog: " + RESOURCE, exception);
        }
        return Collections.unmodifiableMap(actions);
    }

    private static String string(JsonObject value, String key) {
        return value.get(key).getAsString();
    }

    private static int integer(JsonObject value, String key) {
        return value.get(key).getAsInt();
    }

    public record Definition(
            String id,
            String labelKey,
            int affinityPoints,
            int rejectPoints,
            double successChance,
            int cooldownSeconds,
            int neutralLimit,
            int hartoLimit
    ) {}
}
