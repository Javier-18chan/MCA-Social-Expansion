package com.joelj.mcasocial.social;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RelationshipActionService {
    private static final long SPAM_RESET_MS = 2 * 60 * 1000L;
    private static final long LOCKOUT_MS = 5 * 60 * 1000L;

    private RelationshipActionService() {}

    public static boolean isRelationshipAction(String action) {
        return RelationshipActionCatalog.contains(action);
    }

    public static void perform(ServerPlayer player, @Nullable Entity entity, ISocialData data, String action,
                               SocialActionService.RoleInfo role, SocialActionService.InteractionChannel channel) {
        RelationshipActionCatalog.Definition definition = RelationshipActionCatalog.get(action);
        if (definition == null) return;

        if (!isRoleAllowed(definition, role)) {
            String key = definition.romanceAction()
                    ? "chat.mcasocial.relationship.denied.romance"
                    : "chat.mcasocial.relationship.denied.affection";
            sendResult(player, role.name(), key, null, null, null, 0, 0, 0,
                    definition.romanceAction(), channel);
            return;
        }

        if (!requirementsMet(definition, data, role)) {
            String key = lockedKey(definition, data, role);
            sendResult(player, role.name(), key, null, null, null, 0, 0, 0,
                    definition.romanceAction(), channel);
            return;
        }

        long now = System.currentTimeMillis();
        if (now < data.getLockoutTime()) {
            sendResult(player, role.name(), "chat.mcasocial.global.bloqueado", null, null, null,
                    0, 0, 0, definition.romanceAction(), channel);
            return;
        }

        String personality = normalizePersonality(data.getPersonality());
        int relationValue = definition.romanceAction() ? data.getRomance() : data.getTrust();
        int relationLevel = RelationshipActionCatalog.relationshipLevel(relationValue);
        long cooldown = cooldown(definition, personality, relationLevel, role.isSpouse());
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
                + fatigueBonus(personality, relationLevel, action, false)
                + (role.isSpouse() ? 3 : 0));
        int hartoLimit = Math.max(neutralLimit + 2, definition.hartoLimit()
                + fatigueBonus(personality, relationLevel, action, true)
                + (role.isSpouse() ? 5 : 0));

        if (spam && data.getSpamCount() >= hartoLimit) {
            data.setLockoutTime(now + LOCKOUT_MS);
            int romancePoints = definition.romanceAction() ? -1 : 0;
            int trustPoints = definition.romanceAction() ? 0 : -1;
            applyPoints(player, entity, data, romancePoints, 0, trustPoints);
            sendResult(player, role.name(), "chat.mcasocial.global.harto", null, null, null,
                    romancePoints, 0, trustPoints, definition.romanceAction(), channel);
            return;
        }

        if (spam && data.getSpamCount() >= neutralLimit) {
            sendResult(player, role.name(), "chat.mcasocial.global.neutral."
                            + ThreadLocalRandom.current().nextInt(1, 3), null, null, null,
                    0, 0, 0, definition.romanceAction(), channel);
            return;
        }

        if (spam) {
            sendResult(player, role.name(), "chat.mcasocial.relationship.repeat", null, null, null,
                    0, 0, 0, definition.romanceAction(), channel);
            return;
        }

        data.setLastActionTime(action, now);
        data.setActionUseCount(action, data.getActionUseCount(action) + 1);

        double successChance = successChance(definition, personality, relationLevel, role.isSpouse());
        boolean success = ThreadLocalRandom.current().nextDouble() < successChance;
        int romancePoints = success ? adjustedRomancePoints(definition.romancePoints(), personality) : definition.rejectPoints();
        int affinityPoints = success ? definition.affinityPoints() : 0;
        int trustPoints = success ? adjustedTrustPoints(definition.trustPoints(), personality) : 0;

        applyPoints(player, entity, data, romancePoints, affinityPoints, trustPoints);

        String responseCounter = "RESPONSE_" + action;
        int responseSequence = data.getActionUseCount(responseCounter) + 1;
        data.setActionUseCount(responseCounter, responseSequence);

        int primaryVariant = responseVariant(player, entity, data, role.name(), action,
                responseSequence, Objects.hash(11, personality), success ? 8 : 4);
        boolean closeRomance = definition.romanceAction()
                && (role.isSpouse() || data.getRomance() >= 75);
        String primaryKey;
        if (success && definition.romanceAction()) {
            if (!personality.equals("normal") && primaryVariant > 4) {
                int personalityVariant = ((primaryVariant - 5) % 2) + 1;
                primaryKey = "chat.mcasocial.relationship.personality." + action + "." + personality
                        + "." + (closeRomance ? "close" : "standard") + "." + personalityVariant;
            } else {
                primaryKey = "chat.mcasocial.relationship."
                        + (closeRomance ? "close." : "standard.") + action + "."
                        + channel.keySegment() + "." + primaryVariant;
            }
        } else {
            primaryKey = "chat.mcasocial.relationship.action." + action
                    + (success ? ".success." + primaryVariant
                    : primaryVariant == 1 ? ".reject" : ".reject." + primaryVariant);
        }

        String parentKey = success && !definition.romanceAction() && role.isFamily()
                && role.ageStage() != SocialAgeStage.BABY
                ? "chat.mcasocial.relationship.parent_action." + action
                + detectPlayerParentSuffix(player)
                : null;
        if (parentKey != null) {
            primaryKey = parentKey;
            parentKey = null;
        }
        sendResult(player, role.name(), primaryKey, null, null, parentKey,
                romancePoints, affinityPoints, trustPoints, definition.romanceAction(), channel);
        spawnFeedback(player, entity, channel, definition.romanceAction(), success);
    }

    private static boolean isRoleAllowed(RelationshipActionCatalog.Definition definition,
                                         SocialActionService.RoleInfo role) {
        if (definition.romanceAction()) {
            return !role.isBaby() && !role.isMinor() && !role.isFamily();
        }

        return switch (definition.role()) {
            case "baby" -> role.ageStage() == SocialAgeStage.BABY;
            case "child" -> role.ageStage() == SocialAgeStage.CHILD;
            case "teen" -> role.ageStage() == SocialAgeStage.TEEN;
            case "adult_family" -> role.ageStage() == SocialAgeStage.ADULT && role.isFamily();
            default -> false;
        };
    }

    private static boolean requirementsMet(RelationshipActionCatalog.Definition definition, ISocialData data,
                                           SocialActionService.RoleInfo role) {
        if (definition.spouseOnly()) return role.isSpouse();
        if (role.isSpouse() && definition.romanceAction()) return true;
        return data.getAffinity() >= definition.minAffinity()
                && data.getRomance() >= definition.minRomance()
                && data.getTrust() >= definition.minTrust();
    }

    private static String lockedKey(RelationshipActionCatalog.Definition definition, ISocialData data,
                                    SocialActionService.RoleInfo role) {
        if (definition.spouseOnly() && !role.isSpouse()) return "chat.mcasocial.relationship.locked.spouse";
        if (data.getAffinity() < definition.minAffinity()) return "chat.mcasocial.relationship.locked.affinity";
        if (data.getRomance() < definition.minRomance()) return "chat.mcasocial.relationship.locked.romance";
        return "chat.mcasocial.relationship.locked.trust";
    }

    private static long cooldown(RelationshipActionCatalog.Definition definition, String personality,
                                 int relationLevel, boolean spouse) {
        double relationMultiplier = switch (relationLevel) {
            case 1 -> 1.15;
            case 2 -> 1.0;
            case 3 -> 0.85;
            case 4 -> 0.70;
            default -> 0.60;
        };
        double multiplier = relationMultiplier * personalityCooldown(personality, definition.id());
        if (spouse) multiplier *= 0.70;
        return Math.max(10_000L, Math.round(definition.cooldownSeconds() * 1000L * multiplier));
    }

    private static double personalityCooldown(String personality, String action) {
        return switch (personality) {
            case "extrovertido" -> 0.70;
            case "timido" -> 1.30;
            case "ambicioso" -> action.contains("cita") || action.contains("vida") ? 0.85 : 1.05;
            case "chismoso" -> 0.95;
            case "orgulloso" -> action.contains("halagar") ? 0.85 : 1.15;
            case "leal" -> 0.88;
            case "sarcastico" -> action.contains("coquetear") || action.contains("reir") ? 0.85 : 1.0;
            case "romantico" -> action.startsWith("romance_") ? 0.72 : 0.90;
            default -> 1.0;
        };
    }

    private static int fatigueBonus(String personality, int relationLevel, String action, boolean hardLimit) {
        int relationBonus = Math.max(0, relationLevel - 2);
        return relationBonus + switch (personality) {
            case "extrovertido" -> hardLimit ? 4 : 2;
            case "timido" -> hardLimit ? -2 : -1;
            case "ambicioso" -> action.contains("cita") || action.contains("vida") ? 1 : 0;
            case "chismoso" -> hardLimit ? 2 : 1;
            case "orgulloso" -> relationLevel >= 3 ? 0 : -1;
            case "leal" -> hardLimit ? 2 : 1;
            case "sarcastico" -> action.contains("coquetear") || action.contains("reir") ? 1 : 0;
            case "romantico" -> action.startsWith("romance_") ? (hardLimit ? 3 : 2) : 1;
            default -> 0;
        };
    }

    private static double successChance(RelationshipActionCatalog.Definition definition, String personality,
                                        int relationLevel, boolean spouse) {
        double chance = definition.successChance() + Math.max(0, relationLevel - 1) * 0.035;
        if (spouse) chance += 0.08;

        chance += switch (personality) {
            case "extrovertido" -> definition.id().contains("coquetear") || definition.id().contains("reir") ? 0.10 : 0.04;
            case "timido" -> definition.id().contains("mano") || definition.id().contains("escuchar") ? 0.07 : -0.05;
            case "ambicioso" -> definition.id().contains("cita") || definition.id().contains("vida") ? 0.10 : -0.02;
            case "chismoso" -> definition.id().contains("dia") || definition.id().contains("confesar") ? 0.08 : 0.0;
            case "orgulloso" -> definition.id().contains("halagar") || definition.id().contains("orgullo") ? 0.10 : -0.06;
            case "leal" -> definition.id().contains("reafirmar") || definition.id().contains("confianza")
                    || definition.id().contains("apoyo") ? 0.12 : -0.02;
            case "sarcastico" -> definition.id().contains("coquetear") || definition.id().contains("reir") ? 0.10 : -0.01;
            case "romantico" -> definition.romanceAction() ? 0.14 : 0.05;
            default -> 0.0;
        };
        return Math.max(0.10, Math.min(0.98, chance));
    }

    private static int adjustedRomancePoints(int points, String personality) {
        if (points <= 0) return points;
        return personality.equals("romantico") ? points + 1 : points;
    }

    private static int adjustedTrustPoints(int points, String personality) {
        if (points <= 0) return points;
        return personality.equals("leal") ? points + 1 : points;
    }

    private static void applyPoints(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                                    int romancePoints, int affinityPoints, int trustPoints) {
        if (romancePoints != 0) {
            int romance = data.getRomance() + romancePoints;
            data.setRomance(romance);
            if (entity instanceof VillagerEntityMCA villager) {
                try {
                    villager.getVillagerBrain().getMemoriesForPlayer(player).setHearts(romance);
                } catch (Exception ignored) {}
            }
        }
        if (affinityPoints != 0) data.setAffinity(data.getAffinity() + affinityPoints);
        if (trustPoints != 0) data.setTrust(data.getTrust() + trustPoints);
    }

    private static int responseVariant(ServerPlayer player, @Nullable Entity entity, ISocialData data,
                                       String name, String action, int sequence, int salt, int variants) {
        UUID targetUuid;
        if (entity != null) {
            targetUuid = entity.getUUID();
        } else if (data instanceof SocialContact contact) {
            targetUuid = contact.getUuid();
        } else {
            targetUuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        }
        return Math.floorMod(Objects.hash(player.getUUID(), targetUuid, action, sequence, salt), variants) + 1;
    }

    private static void sendResult(ServerPlayer player, String name, String primaryKey,
                                   @Nullable String personalityKey, @Nullable String progressKey,
                                   @Nullable String parentKey,
                                   int romancePoints, int affinityPoints, int trustPoints,
                                   boolean romanceAction, SocialActionService.InteractionChannel channel) {
        MutableComponent message = Component.literal(prefix(name, channel))
                .append(Component.translatable(primaryKey));
        if (personalityKey != null) {
            message.append(Component.literal(" ")).append(Component.translatable(personalityKey));
        }
        if (progressKey != null) {
            message.append(Component.literal(" ")).append(Component.translatable(progressKey));
        }
        if (parentKey != null) {
            message.append(Component.literal(" ")).append(Component.translatable(parentKey));
        }

        if (romanceAction) {
            message.append(Component.literal(pointText(romancePoints, true)));
        } else {
            message.append(Component.literal(pointText(affinityPoints, false)));
            if (trustPoints != 0) {
                String color = trustPoints > 0 ? "\u00a7b" : "\u00a7c";
                message.append(Component.literal(" " + color + "(" + signed(trustPoints) + " C)"));
            }
        }
        player.sendSystemMessage(message);

        int combined = romancePoints + affinityPoints + trustPoints;
        float pitch = combined > 0 ? 1.45F : combined < 0 ? 0.55F : 0.9F;
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.5F, pitch);
    }

    private static String pointText(int points, boolean hearts) {
        String color = points > 0 ? (hearts ? "\u00a7d" : "\u00a7a")
                : points < 0 ? "\u00a7c" : "\u00a7e";
        return " " + color + "(" + signed(points) + (hearts ? " \u2665)" : ")");
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private static String prefix(String name, SocialActionService.InteractionChannel channel) {
        return switch (channel) {
            case PHONE -> "\u00a79[SMS " + name + "] \u00a7f";
            case LETTER -> "\u00a76[Carta " + name + "] \u00a7f";
            case CRYSTAL -> "\u00a7b[Crystal " + name + "] \u00a7f";
            case FACE_TO_FACE -> "\u00a7d<" + name + "> \u00a7f";
        };
    }

    private static void spawnFeedback(ServerPlayer player, @Nullable Entity entity,
                                      SocialActionService.InteractionChannel channel,
                                      boolean romance, boolean success) {
        if (!success || channel != SocialActionService.InteractionChannel.FACE_TO_FACE
                || entity == null || !(player.level() instanceof ServerLevel level)) return;
        level.sendParticles(romance ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(),
                romance ? 5 : 7, 0.3, 0.35, 0.3, 0.02);
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
