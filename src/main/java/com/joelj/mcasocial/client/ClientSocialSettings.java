package com.joelj.mcasocial.client;

public final class ClientSocialSettings {
    private static boolean spontaneousEnabled = true;

    private ClientSocialSettings() {}

    public static boolean isSpontaneousEnabled() { return spontaneousEnabled; }

    public static void setSpontaneousEnabled(boolean enabled) { spontaneousEnabled = enabled; }
}
