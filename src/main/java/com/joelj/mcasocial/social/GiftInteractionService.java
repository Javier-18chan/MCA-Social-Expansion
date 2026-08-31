package com.joelj.mcasocial.social;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.ThreadLocalRandom;

public final class GiftInteractionService {
    private GiftInteractionService() {}

    public static void perform(ServerPlayer player, int entityId, String itemId, boolean familyHint) {
        ResourceLocation requestedItem = ResourceLocation.tryParse(itemId);
        if (requestedItem == null) return;

        Entity entity = player.level().getEntity(entityId);
        if (entity == null || !entity.isAlive() || player.distanceToSqr(entity) > 64.0D) return;

        ItemStack gift = findGift(player, requestedItem);
        if (gift.isEmpty()) return;

        ISocialData data = entity.getCapability(SocialCapability.SOCIAL_CAPABILITY).orElse(null);
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (data == null || contacts == null) return;

        SocialContact storedContact = contacts.getContact(entity.getUUID());
        SocialActionService.RoleInfo detectedRole = SocialActionService.inspectRole(entity, player, false);
        boolean spouse = detectedRole.isSpouse() || storedContact != null && storedContact.isSpouse();
        boolean family = (detectedRole.isFamily() || storedContact != null && storedContact.isFamily()) && !spouse;
        SocialActionService.RoleInfo role = new SocialActionService.RoleInfo(
                detectedRole.name(), detectedRole.isBaby(), detectedRole.isMinor(),
                family, spouse,
                detectedRole.ageStage()
        );

        if (!GiftEligibility.canGift(gift)) {
            player.sendSystemMessage(Component.literal("\u00a7d<" + role.name() + "> \u00a7f")
                    .append(Component.translatable("chat.mcasocial.gift.invalid")));
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 0.5F, 0.85F);
            return;
        }

        SocialContactHelper.remember(player, entity, data, role.isBaby(), role.isMinor(), role.ageStage(),
                role.isFamily(), role.isSpouse());
        SocialContact contact = contacts.getContact(entity.getUUID());
        if (contact == null) return;

        GiftPreferenceProfile.ensure(contact);
        long gameTime = player.level().getGameTime();
        int giftsToday = contact.getActionUseCount("personalized_gifts");
        if (gameTime - contact.getLastActionTime("personalized_gift_day") >= 24_000L) {
            giftsToday = 0;
        }

        if (giftsToday >= 4) {
            player.sendSystemMessage(Component.literal("\u00a7d<" + role.name() + "> \u00a7f")
                    .append(Component.translatable(GiftDialogueResolver.limit(role))));
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 0.5F, 1.0F);
            return;
        }

        GiftEvaluation evaluation = GiftEvaluation.evaluate(gift);
        RatedGift ratedGift = rate(contact, evaluation, gift);
        GiftRating rating = ratedGift.rating();
        int points = pointsFor(rating, evaluation.quality(), giftsToday);
        int variant = ThreadLocalRandom.current().nextInt(1, 3);
        MutableComponent dialogue = GiftDialogueResolver.response(
                player, role, data.getPersonality(), rating, evaluation, gift, giftsToday, variant
        );
        if (role.isBaby()) points = Math.max(1, points);

        updateHearts(player, entity, data, points);
        contact.updateSocialValues(data);
        SocialDiscoveryService.recordGiftObservation(contact, ratedGift.observedCategory());
        contact.setActionUseCount("personalized_gifts", giftsToday + 1);
        contact.setLastActionTime("personalized_gift_day", gameTime);
        contacts.addOrUpdate(contact);
        gift.shrink(1);

        String pointsText = points > 0
                ? " \u00a7d(+" + points + " \u2665)"
                : points == 0 ? " \u00a7e(+0)" : " \u00a7c(" + points + " \u2665)";
        player.sendSystemMessage(Component.literal("\u00a7d<" + role.name() + "> \u00a7f")
                .append(dialogue)
                .append(Component.literal(pointsText)));

        if (points < 0) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 0.5F, 0.9F);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.5F, points >= 8 ? 1.5F : 1.15F);
        }
    }

    private static ItemStack findGift(ServerPlayer player, ResourceLocation requestedItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            ResourceLocation stackId = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (requestedItem.equals(stackId)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static RatedGift rate(SocialContact contact, GiftEvaluation evaluation, ItemStack gift) {
        GiftCategory primary = evaluation.primaryCategory();
        if (GiftPreferenceProfile.isSignatureGift(contact, gift)) {
            return new RatedGift(GiftRating.SIGNATURE, primary);
        }

        GiftRating primaryRating = rateCategory(contact, primary);
        if (primaryRating != GiftRating.NEUTRAL) return new RatedGift(primaryRating, primary);

        for (GiftCategory category : evaluation.categories()) {
            if (category == primary) continue;
            GiftRating secondaryRating = rateCategory(contact, category);
            if (secondaryRating == GiftRating.FAVORITE || secondaryRating == GiftRating.LIKED) {
                return new RatedGift(secondaryRating, category);
            }
        }
        return new RatedGift(GiftRating.NEUTRAL, primary);
    }

    private static GiftRating rateCategory(SocialContact contact, GiftCategory category) {
        if (category.id().equals(contact.getFavoriteGiftCategory())) return GiftRating.FAVORITE;
        if (category.id().equals(contact.getLikedGiftCategory())) return GiftRating.LIKED;
        if (category.id().equals(contact.getDislikedGiftCategory())) return GiftRating.DISLIKED;
        return GiftRating.NEUTRAL;
    }

    private static int pointsFor(GiftRating rating, GiftQuality quality, int giftsToday) {
        int base = switch (rating) {
            case SIGNATURE -> 12;
            case FAVORITE -> 10;
            case LIKED -> 6;
            case NEUTRAL -> 2;
            case DISLIKED -> -4;
        };
        if (rating != GiftRating.DISLIKED) base += quality.heartBonus();
        if (rating == GiftRating.DISLIKED) return giftsToday >= 2 ? -8 : base;
        if (giftsToday == 1) return Math.min(base, 5);
        if (giftsToday == 2) return Math.min(base, 2);
        if (giftsToday >= 3) return base < 0 ? base : 0;
        return base;
    }

    private static void updateHearts(ServerPlayer player, Entity entity, ISocialData data, int points) {
        int hearts = data.getRomance() + points;
        data.setRomance(hearts);
        if (entity instanceof VillagerEntityMCA villager) {
            try {
                var memories = villager.getVillagerBrain().getMemoriesForPlayer(player);
                memories.setHearts(hearts);
                memories.setInteractionFatigue(memories.getInteractionFatigue() + 1);
            } catch (Exception ignored) {}
        }
    }

    private record RatedGift(GiftRating rating, GiftCategory observedCategory) {}
}
