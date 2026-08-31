package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.OpenSocialScreenPacket;
import com.joelj.mcasocial.social.GiftPreferenceProfile;
import net.minecraft.network.chat.Component;

public final class ClientSocialProfile {
    private static int knowledgeLevel;
    private static String favoriteGiftCategory = "";
    private static String likedGiftCategory = "";
    private static String dislikedGiftCategory = "";

    private ClientSocialProfile() {}

    public static void update(OpenSocialScreenPacket packet) {
        knowledgeLevel = packet.knowledgeLevel;
        favoriteGiftCategory = packet.favoriteGiftCategory;
        likedGiftCategory = packet.likedGiftCategory;
        dislikedGiftCategory = packet.dislikedGiftCategory;
    }

    public static int knowledgeLevel() { return knowledgeLevel; }
    public static String favoriteGiftCategory() { return favoriteGiftCategory; }
    public static String likedGiftCategory() { return likedGiftCategory; }
    public static String dislikedGiftCategory() { return dislikedGiftCategory; }

    public static Component personalityDisplay(String personality) {
        if (knowledgeLevel <= 0) return Component.translatable("gui.mcasocial.discovery.unknown");
        String normalized = GiftPreferenceProfile.normalizePersonality(personality);
        if (knowledgeLevel < 4) {
            return Component.translatable("gui.mcasocial.discovery.impression." + normalized);
        }
        return Component.translatable("gui.mcasocial.trait." + normalized);
    }
}
