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

public final class RelationshipActionCatalog {
    private static final String RESOURCE =
            "data/mcasocial/social_actions/relationship_actions.json";
    private static final Map<String, Definition> ACTIONS = load();

    private RelationshipActionCatalog() {}

    public static Definition get(String id) {
        return ACTIONS.get(id);
    }

    public static Collection<Definition> all() {
        return ACTIONS.values();
    }

    public static boolean contains(String id) {
        return ACTIONS.containsKey(id);
    }

    public static String trustTagKey(int trust) {
        if (trust >= 100) return "gui.mcasocial.relationship.trust.4";
        if (trust >= 75) return "gui.mcasocial.relationship.trust.3";
        if (trust >= 50) return "gui.mcasocial.relationship.trust.2";
        if (trust >= 25) return "gui.mcasocial.relationship.trust.1";
        return "gui.mcasocial.relationship.trust.0";
    }

    public static String romanceStage(int romance, boolean spouse) {
        if (spouse) return "spouse";
        if (romance >= 75) return "partner";
        if (romance >= 50) return "dating";
        if (romance >= 25) return "interest";
        return "known";
    }

    public static int relationshipLevel(int value) {
        if (value >= 100) return 5;
        if (value >= 75) return 4;
        if (value >= 50) return 3;
        if (value >= 25) return 2;
        return 1;
    }

    private static Map<String, Definition> load() {
        InputStream stream = RelationshipActionCatalog.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing relationship action catalog: " + RESOURCE);
        }

        Map<String, Definition> actions = new LinkedHashMap<>();
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("actions");
            for (JsonElement element : array) {
                JsonObject value = element.getAsJsonObject();
                Definition definition = new Definition(
                        string(value, "id"),
                        string(value, "category"),
                        string(value, "role"),
                        string(value, "label_key"),
                        integer(value, "min_affinity"),
                        integer(value, "min_romance"),
                        integer(value, "min_trust"),
                        value.get("spouse_only").getAsBoolean(),
                        integer(value, "romance_points"),
                        integer(value, "affinity_points"),
                        integer(value, "trust_points"),
                        integer(value, "reject_points"),
                        value.get("success_chance").getAsDouble(),
                        integer(value, "cooldown_seconds"),
                        integer(value, "neutral_limit"),
                        integer(value, "harto_limit")
                );
                actions.put(definition.id(), definition);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid relationship action catalog: " + RESOURCE, exception);
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
            String category,
            String role,
            String labelKey,
            int minAffinity,
            int minRomance,
            int minTrust,
            boolean spouseOnly,
            int romancePoints,
            int affinityPoints,
            int trustPoints,
            int rejectPoints,
            double successChance,
            int cooldownSeconds,
            int neutralLimit,
            int hartoLimit
    ) {
        public boolean romanceAction() {
            return category.equals("romance");
        }
    }
}
