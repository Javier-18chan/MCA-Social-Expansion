package com.joelj.mcasocial.social;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public final class SocialDiscoveryService {
    public static final String ACTION = "preguntar_intereses";
    private static final long COOLDOWN_MILLIS = 90_000L;

    private SocialDiscoveryService() {}

    public static boolean isDiscoveryAction(String action) {
        return ACTION.equals(action);
    }

    public static void perform(ServerPlayer player, SocialContact contact, SocialActionService.RoleInfo role,
                               SocialActionService.InteractionChannel channel) {
        if (role.isBaby()) {
            send(player, role.name(), Component.translatable("chat.mcasocial.discovery.baby"), channel, 0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - contact.getLastActionTime(ACTION) < COOLDOWN_MILLIS) {
            String stage = stageKey(role.ageStage());
            send(player, role.name(), Component.translatable("chat.mcasocial.discovery.repeat." + stage), channel, 0);
            return;
        }

        GiftPreferenceProfile.ensure(contact);
        contact.setLastActionTime(ACTION, now);
        if (role.isFamily() || role.isSpouse()) {
            contact.setKnowledgeLevel(4);
            String stage = stageKey(role.ageStage());
            send(player, role.name(), Component.translatable("chat.mcasocial.discovery.repeat." + stage), channel, 0);
            return;
        }

        boolean newEvidence = contact.recordDiscoveryAction(ACTION);
        int previousLevel = contact.getKnowledgeLevel();
        updateKnowledge(contact);
        int nextLevel = contact.getKnowledgeLevel();
        if (!newEvidence || nextLevel == previousLevel) {
            String stage = stageKey(role.ageStage());
            send(player, role.name(), Component.translatable("chat.mcasocial.discovery.repeat." + stage), channel, 0);
            return;
        }

        int variant = ThreadLocalRandom.current().nextInt(1, 3);
        String stage = stageKey(role.ageStage());
        MutableComponent response;
        if (nextLevel == 2) {
            response = preferenceResponse("favorite", stage, contact.getFavoriteGiftCategory(), variant);
        } else if (nextLevel == 3) {
            response = preferenceResponse("liked", stage, contact.getLikedGiftCategory(), variant);
        } else if (nextLevel >= 4) {
            response = preferenceResponse("disliked", stage, contact.getDislikedGiftCategory(), variant);
        } else {
            String personality = GiftPreferenceProfile.normalizePersonality(contact.getPersonality());
            response = Component.translatable("chat.mcasocial.discovery.intro.personality." + stage + "." + variant)
                    .append(Component.literal(" "))
                    .append(Component.translatable("chat.mcasocial.discovery.clue.personality." + personality + "." + variant));
        }

        contact.setAffinity(contact.getAffinity() + 1);
        send(player, role.name(), response, channel, 1);
    }

    public static void recordInteraction(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                                         String action, SocialActionService.RoleInfo role) {
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (contacts == null) return;

        SocialContact contact = data instanceof SocialContact socialContact
                ? socialContact
                : entity == null ? null : contacts.getContact(entity.getUUID());
        if (contact == null) return;

        if (role.isFamily() || role.isSpouse()) {
            contact.setKnowledgeLevel(4);
        } else if (contact.recordDiscoveryAction(action)) {
            updateKnowledge(contact);
        }
        contacts.addOrUpdate(contact);
    }

    public static boolean recordGiftObservation(SocialContact contact, GiftCategory category) {
        boolean firstObservation = contact.observeGiftCategory(category.id());
        if (firstObservation) updateKnowledge(contact);
        return firstObservation;
    }

    private static void updateKnowledge(SocialContact contact) {
        if (contact.isFamily() || contact.isSpouse()) {
            contact.setKnowledgeLevel(4);
            return;
        }

        int evidence = contact.getDiscoveryEvidenceCount();
        int discoveredLevel = evidence >= 9 ? 4
                : evidence >= 6 ? 3
                : evidence >= 3 ? 2
                : evidence >= 1 ? 1 : 0;
        if (discoveredLevel > contact.getKnowledgeLevel()) {
            contact.setKnowledgeLevel(discoveredLevel);
        }
    }

    private static MutableComponent preferenceResponse(String type, String stage, String category, int variant) {
        return Component.translatable("chat.mcasocial.discovery.intro." + type + "." + stage + "." + variant)
                .append(Component.literal(" "))
                .append(Component.translatable("chat.mcasocial.discovery.clue.gift." + category + "." + variant));
    }

    private static void send(ServerPlayer player, String name, Component response,
                             SocialActionService.InteractionChannel channel, int points) {
        MutableComponent message = Component.literal(prefix(name, channel)).append(response);
        if (points > 0) message.append(Component.literal(" \u00a7a(+" + points + ")"));
        else message.append(Component.literal(" \u00a7e(+0)"));
        player.sendSystemMessage(message);

        SocialInterfaceSounds.playResult(player, channel, points > 0 ? 1.2F : 0.85F);
    }

    private static String prefix(String name, SocialActionService.InteractionChannel channel) {
        return switch (channel) {
            case PHONE -> "\u00a79[SMS " + name + "] \u00a7f";
            case LETTER -> "\u00a76[Carta " + name + "] \u00a7f";
            case CRYSTAL -> "\u00a7b[Crystal " + name + "] \u00a7f";
            case FACE_TO_FACE -> "\u00a7d<" + name + "> \u00a7f";
        };
    }

    private static String stageKey(SocialAgeStage ageStage) {
        return switch (ageStage) {
            case BABY, CHILD -> "child";
            case TEEN -> "teen";
            default -> "adult";
        };
    }
}
