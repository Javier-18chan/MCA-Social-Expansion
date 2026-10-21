package com.joelj.mcasocial.social;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class HumorActionService {
    private static final long SPAM_RESET_MS = 2 * 60 * 1000L;
    private static final long LOCKOUT_MS = 5 * 60 * 1000L;

    private HumorActionService() {}

    public static boolean isHumorAction(String action) {
        return HumorActionCatalog.contains(action);
    }

    public static void perform(ServerPlayer player, @Nullable Entity entity, ISocialData data, String action,
                               SocialActionService.RoleInfo role, SocialActionService.InteractionChannel channel) {
        HumorActionCatalog.Definition definition = HumorActionCatalog.get(action);
        if (definition == null) return;

        long now = System.currentTimeMillis();
        if (now < data.getLockoutTime()) {
            sendResult(player, role, "chat.mcasocial.global.bloqueado", 0, channel);
            return;
        }

        String personality = normalizePersonality(data.getPersonality());
        int relationValue = role.isFamily() ? data.getTrust() : data.getAffinity();
        int relationLevel = HumorActionCatalog.relationshipLevel(relationValue);
        long cooldown = cooldown(definition, personality, relationLevel, role);
        long elapsed = now - data.getLastActionTime(action);

        if (now - data.getLastActionTime("GLOBAL_SPAM_TIMER") > SPAM_RESET_MS) {
            data.setSpamCount(0);
        }

        boolean spam = elapsed < cooldown;
        if (!spam) {
            data.setSpamCount(0);
            data.setActionUseCount(action, 0);
        } else {
            data.setSpamCount(data.getSpamCount() + 1);
            data.setLastActionTime("GLOBAL_SPAM_TIMER", now);
        }

        int neutralLimit = Math.max(1, definition.neutralLimit()
                + fatigueBonus(personality, relationLevel, definition.id(), false)
                + (role.isSpouse() ? 3 : 0)
                + (role.isFamily() && relationLevel >= 3 ? 1 : 0));
        int hartoLimit = Math.max(neutralLimit + 2, definition.hartoLimit()
                + fatigueBonus(personality, relationLevel, definition.id(), true)
                + (role.isSpouse() ? 5 : 0)
                + (role.isFamily() && relationLevel >= 3 ? 2 : 0));

        String baseKey = baseKey(definition);
        String audienceKey = audienceKey(role, personality, player);

        if (spam && data.getSpamCount() >= hartoLimit) {
            data.setLockoutTime(now + LOCKOUT_MS);
            int points = -1;
            data.setAffinity(data.getAffinity() + points);
            sendResult(player, role, baseKey + "." + audienceKey + ".harto", points, channel);
            return;
        }

        if (spam && data.getSpamCount() >= neutralLimit) {
            String relationKey = relationLevel >= 3 ? "high" : "low";
            sendResult(player, role, baseKey + "." + audienceKey + ".warning." + relationKey, 0, channel);
            return;
        }

        if (spam) {
            String relationKey = relationLevel >= 3 ? "high" : "low";
            sendResult(player, role, baseKey + "." + audienceKey + ".warning." + relationKey, 0, channel);
            return;
        }

        data.setLastActionTime(action, now);
        data.setActionUseCount(action, data.getActionUseCount(action) + 1);

        boolean success = ThreadLocalRandom.current().nextDouble() < successChance(definition, personality, relationLevel, role);
        int points = success ? adjustedPoints(definition.affinityPoints(), personality, role) : definition.rejectPoints();
        data.setAffinity(data.getAffinity() + points);
        if (role.isFamily() && success) {
            data.setTrust(data.getTrust() + 1);
        }

        int variant = responseVariant(player, entity, data, role.name(), action,
                data.getActionUseCount(action), success ? 6 : 3);
        String key = success
                ? baseKey + "." + audienceKey + ".success." + variant
                : (variant == 1 ? baseKey + "." + audienceKey + ".fail"
                : baseKey + "." + audienceKey + ".fail." + variant);
        sendResult(player, role, key, points, channel);
        SocialDiscoveryService.recordInteraction(player, entity, data, action, role);
    }

    private static String baseKey(HumorActionCatalog.Definition definition) {
        String action = definition.id().startsWith("humor_")
                ? definition.id().substring("humor_".length())
                : definition.id();
        return "chat.mcasocial.humor." + action;
    }

    private static String audienceKey(SocialActionService.RoleInfo role, String personality, ServerPlayer player) {
        if (role.isBaby()) return "bebe";
        if (role.isSpouse()) return "conyuge";
        if (role.isFamily() && role.ageStage() == SocialAgeStage.ADULT) {
            return "hijo_adulto" + detectPlayerParentSuffix(player);
        }
        if (role.isFamily()) return "hijo" + detectPlayerParentSuffix(player);
        if (role.isMinor()) return "menor";
        return personality;
    }

    private static long cooldown(HumorActionCatalog.Definition definition, String personality, int relationLevel,
                                 SocialActionService.RoleInfo role) {
        double relationMultiplier = switch (relationLevel) {
            case 1 -> 1.15;
            case 2 -> 1.0;
            case 3 -> 0.84;
            case 4 -> 0.72;
            default -> 0.62;
        };
        double multiplier = relationMultiplier * personalityCooldown(personality, definition.id());
        if (role.isSpouse()) multiplier *= 0.72;
        if (role.isFamily() && relationLevel >= 3) multiplier *= 0.85;
        return Math.max(10_000L, Math.round(definition.cooldownSeconds() * 1000L * multiplier));
    }

    private static double personalityCooldown(String personality, String action) {
        return switch (personality) {
            case "extrovertido" -> 0.68;
            case "timido" -> action.contains("fastidiar") ? 1.35 : 1.18;
            case "ambicioso" -> action.contains("entretener") ? 0.92 : 1.08;
            case "chismoso" -> 0.76;
            case "orgulloso" -> action.contains("fastidiar") || action.contains("imitar") ? 1.25 : 1.08;
            case "leal" -> 0.92;
            case "sarcastico" -> 0.74;
            case "romantico" -> 0.90;
            default -> 1.0;
        };
    }

    private static int fatigueBonus(String personality, int relationLevel, String action, boolean hardLimit) {
        boolean close = relationLevel >= 3;
        return switch (personality) {
            case "extrovertido" -> hardLimit ? 4 : 2;
            case "timido" -> hardLimit ? (close ? -1 : -2) : (close ? 0 : -1);
            case "ambicioso" -> action.contains("entretener") ? 1 : 0;
            case "chismoso" -> hardLimit ? 3 : 2;
            case "orgulloso" -> hardLimit ? (close ? 0 : -2) : (close ? 0 : -1);
            case "leal" -> hardLimit ? 2 : 1;
            case "sarcastico" -> hardLimit ? 3 : 2;
            case "romantico" -> hardLimit ? 1 : 0;
            default -> 0;
        };
    }

    private static double successChance(HumorActionCatalog.Definition definition, String personality, int relationLevel,
                                        SocialActionService.RoleInfo role) {
        double chance = definition.successChance() + Math.max(0, relationLevel - 1) * 0.035;
        if (role.isSpouse()) chance += 0.08;
        if (role.isFamily()) chance += 0.04;
        chance += switch (personality) {
            case "extrovertido" -> 0.12;
            case "timido" -> definition.id().contains("fastidiar") ? -0.08 : 0.02;
            case "ambicioso" -> definition.id().contains("entretener") ? 0.06 : -0.03;
            case "chismoso" -> 0.09;
            case "orgulloso" -> definition.id().contains("fastidiar") || definition.id().contains("imitar") ? -0.12 : -0.03;
            case "leal" -> definition.id().contains("fastidiar") ? -0.02 : 0.05;
            case "sarcastico" -> 0.11;
            case "romantico" -> definition.id().contains("ser_tonto") ? 0.04 : 0.02;
            default -> 0.0;
        };
        return Math.max(0.10, Math.min(0.98, chance));
    }

    private static int adjustedPoints(int points, String personality, SocialActionService.RoleInfo role) {
        if (points <= 0) return points;
        int adjusted = points;
        if (personality.equals("extrovertido") || personality.equals("sarcastico")) adjusted++;
        if (role.isSpouse() || role.isFamily()) adjusted++;
        return adjusted;
    }

    private static int responseVariant(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                                       String name, String action, int sequence, int variants) {
        UUID targetUuid;
        if (entity != null) {
            targetUuid = entity.getUUID();
        } else if (data instanceof SocialContact contact) {
            targetUuid = contact.getUuid();
        } else {
            targetUuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        }
        return Math.floorMod(Objects.hash(player.getUUID(), targetUuid, action, sequence), variants) + 1;
    }

    private static void sendResult(ServerPlayer player, SocialActionService.RoleInfo role, String key, int points,
                                   SocialActionService.InteractionChannel channel) {
        MutableComponent message = Component.literal(prefix(role.name(), channel))
                .append(SocialKinship.dialogue(player, role.uuid(), key))
                .append(Component.literal(" " + pointText(points)));
        player.sendSystemMessage(message);

        float pitch = points > 0 ? 1.45F : points < 0 ? 0.55F : 0.9F;
        SocialInterfaceSounds.playResult(player, channel, pitch);
    }

    private static String pointText(int points) {
        String color = points > 0 ? "\u00a7a" : points < 0 ? "\u00a7c" : "\u00a7e";
        return color + "(" + (points > 0 ? "+" + points : points) + ")";
    }

    private static String prefix(String name, SocialActionService.InteractionChannel channel) {
        return switch (channel) {
            case PHONE -> "\u00a79[SMS " + name + "] \u00a7f";
            case LETTER -> "\u00a76[Carta " + name + "] \u00a7f";
            case CRYSTAL -> "\u00a7b[Crystal " + name + "] \u00a7f";
            case FACE_TO_FACE -> "\u00a7d<" + name + "> \u00a7f";
        };
    }

    private static String normalizePersonality(String personality) {
        if (personality == null) return "normal";
        String lower = personality.toLowerCase();
        if (lower.contains("extrovertido")) return "extrovertido";
        if (lower.contains("timido") || lower.contains("t\u00edmido")) return "timido";
        if (lower.contains("ambicioso")) return "ambicioso";
        if (lower.contains("chismoso")) return "chismoso";
        if (lower.contains("orgulloso") || lower.contains("serio")) return "orgulloso";
        if (lower.contains("leal") || lower.contains("pacifico") || lower.contains("pac\u00edfico")) return "leal";
        if (lower.contains("sarcastico") || lower.contains("sarc\u00e1stico")) return "sarcastico";
        if (lower.contains("romantico") || lower.contains("rom\u00e1ntico")) return "romantico";
        return "normal";
    }

    private static String detectPlayerParentSuffix(ServerPlayer player) {
        try {
            var playerData = forge.net.conczin.mca.server.world.data.PlayerSaveData.get(player);
            if (playerData != null && playerData.getGender().toString().equalsIgnoreCase("FEMALE")) return ".mama";
        } catch (Exception ignored) {}
        return ".papa";
    }
}
