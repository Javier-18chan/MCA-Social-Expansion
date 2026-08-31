package com.joelj.mcasocial.social;

public enum DeliveryDelayMode {
    INSTANT,
    SHORT,
    IMMERSIVE;

    public static DeliveryDelayMode fromOrdinal(int ordinal) {
        DeliveryDelayMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SHORT;
    }

    public int delayTicks(int interfaceType) {
        if (this == INSTANT) return 0;

        int normalized = SocialInterfaceType.normalize(interfaceType);
        if (this == SHORT) {
            return switch (normalized) {
                case SocialInterfaceType.LETTER -> 80;
                case SocialInterfaceType.CRYSTAL -> 40;
                default -> 20;
            };
        }

        return switch (normalized) {
            case SocialInterfaceType.LETTER -> 240;
            case SocialInterfaceType.CRYSTAL -> 100;
            default -> 60;
        };
    }

    public String translationKey() {
        return "gui.mcasocial.settings.delay." + name().toLowerCase();
    }

    public DeliveryDelayMode next() {
        DeliveryDelayMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
