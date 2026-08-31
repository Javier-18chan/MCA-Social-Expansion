package com.joelj.mcasocial.social;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public final class GiftPreferenceProfile {
    private GiftPreferenceProfile() {}

    public static void ensure(SocialContact contact) {
        String personality = normalizePersonality(contact.getPersonality());
        if (!contact.hasGiftProfile()) {
            GiftCategory[] candidates = candidates(personality, contact.getAgeStage());
            int seed = contact.getUuid().hashCode();
            GiftCategory favorite = candidates[Math.floorMod(seed, candidates.length)];
            GiftCategory liked = candidates[Math.floorMod(seed >>> 4, candidates.length)];
            if (liked == favorite) {
                liked = candidates[(Math.floorMod(seed >>> 4, candidates.length) + 1) % candidates.length];
            }
            GiftCategory disliked = selectDisliked(contact, favorite, liked, personality);
            contact.setGiftProfile(favorite.id(), liked.id(), disliked.id());
            return;
        }

        GiftCategory favorite = GiftCategory.fromId(contact.getFavoriteGiftCategory());
        GiftCategory liked = GiftCategory.fromId(contact.getLikedGiftCategory());
        GiftCategory disliked = GiftCategory.fromId(contact.getDislikedGiftCategory());
        GiftCategory signatureCategory = signatureCategory(personality);
        if (disliked == favorite || disliked == liked || disliked == signatureCategory) {
            contact.setGiftProfile(favorite.id(), liked.id(), selectDisliked(contact, favorite, liked, personality).id());
        }
    }

    public static boolean isSignatureGift(SocialContact contact, ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        if (contact.getAgeStage() == SocialAgeStage.BABY
                || contact.getAgeStage() == SocialAgeStage.CHILD
                || contact.getAgeStage() == SocialAgeStage.TEEN) {
            return itemId.equals(id("minecraft:cake")) || itemId.equals(id("minecraft:cookie"));
        }
        ResourceLocation signature = signatureGift(normalizePersonality(contact.getPersonality()));
        return signature != null && signature.equals(itemId);
    }

    public static ResourceLocation signatureGift(String personality) {
        return switch (normalizePersonality(personality)) {
            case "extrovertido" -> id("minecraft:cake");
            case "timido" -> id("minecraft:cookie");
            case "ambicioso" -> id("minecraft:diamond");
            case "chismoso" -> id("minecraft:sweet_berries");
            case "orgulloso" -> id("minecraft:emerald");
            case "leal" -> id("minecraft:iron_ingot");
            case "sarcastico" -> id("minecraft:amethyst_shard");
            case "romantico" -> id("minecraft:blue_orchid");
            default -> null;
        };
    }

    private static GiftCategory selectDisliked(SocialContact contact, GiftCategory favorite, GiftCategory liked,
                                                String personality) {
        GiftCategory[] candidates = {
                GiftCategory.WEAPONS, GiftCategory.ARMOR, GiftCategory.OTHER,
                GiftCategory.VALUABLES, GiftCategory.FOOD, GiftCategory.TOOLS,
                GiftCategory.BOOKS, GiftCategory.NATURE
        };
        GiftCategory signatureCategory = signatureCategory(personality);
        int start = Math.floorMod(contact.getUuid().hashCode() >>> 8, candidates.length);
        for (int offset = 0; offset < candidates.length; offset++) {
            GiftCategory candidate = candidates[(start + offset) % candidates.length];
            if (candidate != favorite && candidate != liked && candidate != signatureCategory) return candidate;
        }
        return GiftCategory.OTHER;
    }

    private static GiftCategory signatureCategory(String personality) {
        ResourceLocation signature = signatureGift(personality);
        if (signature == null) return null;
        var item = ForgeRegistries.ITEMS.getValue(signature);
        return item == null ? null : GiftCategory.classify(new ItemStack(item));
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }

    private static GiftCategory[] candidates(String personality, SocialAgeStage ageStage) {
        if (ageStage == SocialAgeStage.BABY || ageStage == SocialAgeStage.CHILD) {
            return new GiftCategory[]{GiftCategory.SWEETS, GiftCategory.FOOD, GiftCategory.NATURE, GiftCategory.FLOWERS};
        }
        if (ageStage == SocialAgeStage.TEEN) {
            return new GiftCategory[]{GiftCategory.SWEETS, GiftCategory.BOOKS, GiftCategory.FLOWERS, GiftCategory.VALUABLES};
        }

        return switch (personality) {
            case "extrovertido" -> new GiftCategory[]{GiftCategory.SWEETS, GiftCategory.FLOWERS, GiftCategory.FOOD, GiftCategory.VALUABLES};
            case "timido" -> new GiftCategory[]{GiftCategory.BOOKS, GiftCategory.NATURE, GiftCategory.FLOWERS, GiftCategory.SWEETS};
            case "ambicioso" -> new GiftCategory[]{GiftCategory.VALUABLES, GiftCategory.TOOLS, GiftCategory.ARMOR, GiftCategory.BOOKS};
            case "chismoso" -> new GiftCategory[]{GiftCategory.SWEETS, GiftCategory.BOOKS, GiftCategory.VALUABLES, GiftCategory.FLOWERS};
            case "orgulloso" -> new GiftCategory[]{GiftCategory.VALUABLES, GiftCategory.ARMOR, GiftCategory.FLOWERS, GiftCategory.BOOKS};
            case "leal" -> new GiftCategory[]{GiftCategory.TOOLS, GiftCategory.ARMOR, GiftCategory.FOOD, GiftCategory.NATURE};
            case "sarcastico" -> new GiftCategory[]{GiftCategory.BOOKS, GiftCategory.VALUABLES, GiftCategory.SWEETS, GiftCategory.TOOLS};
            case "romantico" -> new GiftCategory[]{GiftCategory.FLOWERS, GiftCategory.SWEETS, GiftCategory.BOOKS, GiftCategory.NATURE};
            default -> new GiftCategory[]{GiftCategory.FOOD, GiftCategory.FLOWERS, GiftCategory.BOOKS, GiftCategory.TOOLS};
        };
    }

    public static String normalizePersonality(String personality) {
        if (personality == null) return "normal";
        String normalized = personality.toLowerCase(Locale.ROOT)
                .replace("í", "i")
                .replace("á", "a")
                .replace("ó", "o");
        if (normalized.contains("extrovertido")) return "extrovertido";
        if (normalized.contains("timido")) return "timido";
        if (normalized.contains("ambicioso")) return "ambicioso";
        if (normalized.contains("chismoso")) return "chismoso";
        if (normalized.contains("orgulloso") || normalized.contains("serio")) return "orgulloso";
        if (normalized.contains("leal") || normalized.contains("pacifico")) return "leal";
        if (normalized.contains("sarcastico")) return "sarcastico";
        if (normalized.contains("romantico")) return "romantico";
        return "normal";
    }
}
