package com.joelj.mcasocial.social;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class SocialNarrativeService {
    private static final Set<String> ACTIONS = Set.of(
            "insultar", "insulto_leve", "insulto_directo", "insulto_duro",
            "criticar", "confrontar", "provocar", "disculparse", "reconciliar",
            "tema_intereses", "tema_planes", "tema_recuerdos");

    private SocialNarrativeService() {}

    public static boolean isNarrativeAction(String action) {
        return ACTIONS.contains(action);
    }

    public static void perform(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                               String action, SocialActionService.RoleInfo role,
                               SocialActionService.InteractionChannel channel) {
        SocialContact contact = data instanceof SocialContact stored ? stored
                : entity == null ? null : player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS)
                        .map(contacts -> contacts.getContact(entity.getUUID())).orElse(null);
        if (contact == null) return;

        SocialMemory memory = contact.getSocialMemory();
        long now = player.server.overworld().getGameTime();
        if (action.startsWith("tema_")) {
            discussTopic(player, entity, data, contact, role, channel, memory, action.substring(5), now);
        } else if (action.equals("disculparse")) {
            apologize(player, data, role, channel, memory, now);
        } else if (action.equals("reconciliar")) {
            reconcile(player, data, role, channel, memory, now);
        } else {
            conflict(player, entity, data, role, channel, memory, action, now);
        }
    }

    private static void conflict(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                                 SocialActionService.RoleInfo role, SocialActionService.InteractionChannel channel,
                                 SocialMemory memory, String action, long now) {
        if (role.isBaby()) {
            respond(player, role, "chat.mcasocial.narrative.conflict.baby", 0, false, channel);
            return;
        }
        if (action.equals("insulto_duro") && role.isMinor()) {
            respond(player, role, "chat.mcasocial.narrative.conflict.minor_limit", 0, false, channel);
            return;
        }
        if (SocialMemory.recent(memory.lastConflictTime(), now, 200L)) {
            respond(player, role, "chat.mcasocial.narrative.conflict.pause", 0, false, channel);
            return;
        }

        String personality = GiftPreferenceProfile.normalizePersonality(data.getPersonality());
        int points = switch (action) {
            case "insulto_leve" -> -2;
            case "insulto_duro" -> -7;
            case "insultar", "insulto_directo" -> -4;
            case "criticar" -> personality.equals("orgulloso") ? -3 : -1;
            case "confrontar" -> personality.equals("leal") ? 0 : -1;
            case "provocar" -> personality.equals("sarcastico") ? -2 : -4;
            default -> 0;
        };
        int severity = switch (action) {
            case "insulto_duro" -> 3;
            case "insultar", "insulto_directo", "provocar" -> 2;
            default -> 1;
        };
        boolean insult = action.startsWith("insult");
        if (points < 0) {
            memory.recordConflict(action, severity, now);
            data.setAffinity(data.getAffinity() + points);
            data.setTrust(data.getTrust() - (insult ? severity : 1));
            if (insult) SocialActionService.updateMcaHearts(player, entity, data, points);
        } else if (action.equals("confrontar")) {
            data.setTrust(data.getTrust() + 1);
        }

        String stage = stage(role);
        int variant = ThreadLocalRandom.current().nextInt(1, 3);
        String key = insult
                ? "chat.mcasocial.narrative.insult." + insultLevel(action) + "." + stage + "." + variant
                : "chat.mcasocial.conflict." + action + "." + stage + "." + variant;
        respond(player, role, key, points, insult, channel);
    }

    private static void apologize(ServerPlayer player, ISocialData data, SocialActionService.RoleInfo role,
                                   SocialActionService.InteractionChannel channel, SocialMemory memory, long now) {
        String key;
        if (memory.tension() == 0) key = "chat.mcasocial.narrative.apology.none";
        else if (memory.apologyAccepted()) key = "chat.mcasocial.narrative.apology.already";
        else if (SocialMemory.recent(memory.lastConflictTime(), now, 100L)
                || SocialMemory.recent(memory.lastApologyAttempt(), now, 1200L)) {
            key = "chat.mcasocial.narrative.apology.wait";
        } else {
            boolean accepted = ThreadLocalRandom.current().nextDouble()
                    < acceptanceChance(data, role, memory.tension(), true);
            memory.recordApologyAttempt(now, accepted);
            if (accepted) data.setTrust(data.getTrust() + 1);
            key = "chat.mcasocial.narrative.apology." + (accepted ? "accepted." : "refused.") + context(role, data);
        }
        respond(player, role, key, 0, false, channel);
    }

    private static void reconcile(ServerPlayer player, ISocialData data, SocialActionService.RoleInfo role,
                                   SocialActionService.InteractionChannel channel, SocialMemory memory, long now) {
        String key;
        int points = 0;
        if (memory.tension() == 0) key = "chat.mcasocial.narrative.reconcile.none";
        else if (!memory.apologyAccepted()) key = "chat.mcasocial.narrative.reconcile.need_apology";
        else if (SocialMemory.recent(memory.lastApologyTime(), now, 200L)
                || SocialMemory.recent(memory.lastReconciliationAttempt(), now, 1200L)) {
            key = "chat.mcasocial.narrative.reconcile.wait";
        } else {
            boolean accepted = ThreadLocalRandom.current().nextDouble()
                    < acceptanceChance(data, role, memory.tension(), false);
            memory.recordReconciliationAttempt(now, accepted);
            if (accepted) {
                points = 1;
                data.setAffinity(data.getAffinity() + points);
                data.setTrust(data.getTrust() + 1);
            }
            key = "chat.mcasocial.narrative.reconcile." + (accepted ? "accepted." : "refused.") + context(role, data);
        }
        respond(player, role, key, points, false, channel);
    }

    private static void discussTopic(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                                      SocialContact contact, SocialActionService.RoleInfo role,
                                      SocialActionService.InteractionChannel channel, SocialMemory memory,
                                      String topic, long now) {
        if (role.isBaby()) {
            respond(player, role, "chat.mcasocial.narrative.topic.baby", 0, false, channel);
            return;
        }
        String context = context(role, data);
        if (memory.tension() > 0 && !memory.apologyAccepted()) {
            respond(player, role, "chat.mcasocial.narrative.topic.tense." + context, 0, false, channel);
            return;
        }
        if (memory.recentlyDiscussed(topic, now)) {
            respond(player, role, "chat.mcasocial.narrative.topic.repeat." + context, 0, false, channel);
            return;
        }

        memory.recordTopic(topic, now);
        String personality = GiftPreferenceProfile.normalizePersonality(data.getPersonality());
        int preferred = switch (personality) {
            case "timido", "leal", "orgulloso" -> 1;
            case "extrovertido", "chismoso", "romantico" -> 2;
            default -> ThreadLocalRandom.current().nextInt(1, 3);
        };
        int points = memory.tension() > 0 ? 0 : 1;
        if (points > 0) data.setAffinity(data.getAffinity() + points);
        SocialDiscoveryService.recordInteraction(player, entity, data, "tema_" + topic, role);
        String translationTopic = switch (topic) {
            case "intereses" -> "interests";
            case "planes" -> "plans";
            case "recuerdos" -> "memories";
            default -> topic;
        };
        respond(player, role, "chat.mcasocial.narrative.topic." + translationTopic + "." + context + "." + preferred,
                points, false, channel);
    }

    private static double acceptanceChance(ISocialData data, SocialActionService.RoleInfo role,
                                           int tension, boolean apology) {
        String personality = GiftPreferenceProfile.normalizePersonality(data.getPersonality());
        double chance = apology ? 0.72 : 0.65;
        chance += Math.max(-0.15, Math.min(0.20, data.getAffinity() / 250.0));
        chance -= tension * (apology ? 0.025 : 0.04);
        if (role.isFamily() || role.isSpouse()) chance += 0.12;
        if (personality.equals("orgulloso")) chance -= 0.12;
        if (personality.equals("leal") || personality.equals("romantico")) chance += 0.08;
        return Math.max(0.20, Math.min(0.90, chance));
    }

    private static String context(SocialActionService.RoleInfo role, ISocialData data) {
        if (role.ageStage() == SocialAgeStage.CHILD) return role.isFamily() ? "family_child" : "child";
        if (role.ageStage() == SocialAgeStage.TEEN) return role.isFamily() ? "family_teen" : "teen";
        if (role.isSpouse()) return "spouse";
        if (role.isFamily()) return "family";
        return data.getAffinity() >= 50 ? "friend" : "acquaintance";
    }

    private static String stage(SocialActionService.RoleInfo role) {
        return switch (role.ageStage()) {
            case BABY, CHILD -> "child";
            case TEEN -> "teen";
            default -> "adult";
        };
    }

    private static String insultLevel(String action) {
        return switch (action) {
            case "insulto_leve" -> "light";
            case "insulto_duro" -> "harsh";
            default -> "direct";
        };
    }

    private static void respond(ServerPlayer player, SocialActionService.RoleInfo role, String key, int points,
                                boolean hearts, SocialActionService.InteractionChannel channel) {
        SocialActionService.sendResult(player, role, key, points, hearts, channel);
    }
}
