package com.joelj.mcasocial.social;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class GiftDialogueResolver {
    private GiftDialogueResolver() {}

    public static MutableComponent response(ServerPlayer player, SocialActionService.RoleInfo role, String personality,
                                             GiftRating rating, GiftEvaluation evaluation, ItemStack gift,
                                             int giftsToday, int variant) {
        if (role.isBaby()) return Component.translatable("chat.mcasocial.bebe.regalo");
        if (giftsToday >= 2 && role.ageStage() == SocialAgeStage.ADULT) {
            return Component.translatable(repeatedAdultResponse(role, rating));
        }

        GiftRating dialogueRating = emotionalRating(rating, evaluation.quality());
        String audience = contextualAudience(role);
        if (audience != null) {
            String key = "chat.mcasocial.gift.context." + audience + "." + dialogueRating.dialogueKey() + "." + variant;
            if (role.isFamily() && !role.isSpouse()) {
                return Component.translatable(key, SocialKinship.address(player, role.uuid(), false), gift.getHoverName());
            }
            return Component.translatable(key, gift.getHoverName());
        }

        if (evaluation.quality() != GiftQuality.NONE || evaluation.hasCategory(GiftCategory.FLOWERS)) {
            return Component.translatable("chat.mcasocial.gift.personalized."
                    + stageKey(role.ageStage()) + "." + dialogueRating.dialogueKey() + "." + variant,
                    gift.getHoverName());
        }

        String normalized = GiftPreferenceProfile.normalizePersonality(personality);
        boolean positive = dialogueRating == GiftRating.SIGNATURE
                || dialogueRating == GiftRating.FAVORITE || dialogueRating == GiftRating.LIKED;
        if (!normalized.equals("normal")) {
            if (dialogueRating == GiftRating.DISLIKED) {
                return Component.translatable("chat.mcasocial.gift.personality."
                        + normalized + ".disliked." + variant);
            }
            if (positive) {
                return Component.translatable("chat.mcasocial.gift.personality."
                        + normalized + "." + variant, gift.getHoverName());
            }
        }

        return Component.translatable("chat.mcasocial.gift.personalized."
                + stageKey(role.ageStage()) + "." + dialogueRating.dialogueKey() + "." + variant,
                gift.getHoverName());
    }

    public static String limit(SocialActionService.RoleInfo role) {
        if (role.isFamily() && role.ageStage() == SocialAgeStage.ADULT) {
            return "chat.mcasocial.familiar_adulto.spam";
        }
        return "chat.mcasocial.gift.personalized.limit." + stageKey(role.ageStage());
    }

    private static String repeatedAdultResponse(SocialActionService.RoleInfo role, GiftRating rating) {
        String suffix = switch (rating) {
            case FAVORITE, LIKED -> "bueno";
            case DISLIKED -> "malo";
            default -> "normal";
        };
        if (role.isFamily()) return "chat.mcasocial.familiar_adulto.regalo.sospecha_" + suffix;
        return "chat.mcasocial.regalo.sospecha_" + suffix;
    }

    private static String contextualAudience(SocialActionService.RoleInfo role) {
        if (role.isSpouse()) return "spouse";
        if (!role.isFamily()) return null;
        return switch (role.ageStage()) {
            case BABY, CHILD -> "family_child";
            case TEEN -> "family_teen";
            case ADULT -> "family_adult";
        };
    }

    private static GiftRating emotionalRating(GiftRating rating, GiftQuality quality) {
        if (rating == GiftRating.DISLIKED) return rating;
        if (quality == GiftQuality.SIMPLE) {
            if (rating == GiftRating.SIGNATURE || rating == GiftRating.FAVORITE) return GiftRating.LIKED;
            if (rating == GiftRating.LIKED) return GiftRating.NEUTRAL;
        }
        if (quality == GiftQuality.EXCEPTIONAL) {
            if (rating == GiftRating.LIKED) return GiftRating.FAVORITE;
            if (rating == GiftRating.NEUTRAL) return GiftRating.LIKED;
        }
        return rating;
    }

    private static String stageKey(SocialAgeStage stage) {
        return switch (stage) {
            case BABY, CHILD -> "child";
            case TEEN -> "teen";
            default -> "adult";
        };
    }
}
