package com.joelj.mcasocial.social;

public enum SocialAgeStage {
    BABY,
    CHILD,
    TEEN,
    ADULT;

    public static SocialAgeStage fromMcaName(String ageState) {
        if (ageState == null) return ADULT;
        return switch (ageState.toUpperCase()) {
            case "BABY" -> BABY;
            case "TODDLER", "CHILD" -> CHILD;
            case "TEEN" -> TEEN;
            default -> ADULT;
        };
    }

    public static SocialAgeStage fromFlags(boolean baby, boolean minor) {
        if (baby) return BABY;
        if (minor) return CHILD;
        return ADULT;
    }

    public static SocialAgeStage fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : ADULT;
    }
}
