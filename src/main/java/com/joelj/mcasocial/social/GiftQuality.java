package com.joelj.mcasocial.social;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

public enum GiftQuality {
    NONE(0),
    SIMPLE(0),
    STANDARD(1),
    FINE(2),
    EXCEPTIONAL(3);

    private final int heartBonus;
    private final TagKey<Item> itemTag;

    GiftQuality(int heartBonus) {
        this.heartBonus = heartBonus;
        this.itemTag = TagKey.create(
                Registries.ITEM,
                ResourceLocation.tryParse("mcasocial:gifts/quality/" + name().toLowerCase())
        );
    }

    public int heartBonus() {
        return heartBonus;
    }

    public String translationKey() {
        return "gui.mcasocial.gift.quality." + name().toLowerCase();
    }

    public boolean addsValuableCategory() {
        return this == FINE || this == EXCEPTIONAL;
    }

    public static GiftQuality evaluate(ItemStack stack) {
        for (GiftQuality quality : values()) {
            if (quality != NONE && stack.is(quality.itemTag)) return quality;
        }

        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            var material = armor.getMaterial();
            if (material == ArmorMaterials.LEATHER) return SIMPLE;
            if (material == ArmorMaterials.CHAIN || material == ArmorMaterials.IRON
                    || material == ArmorMaterials.TURTLE) return STANDARD;
            if (material == ArmorMaterials.GOLD || material == ArmorMaterials.DIAMOND) return FINE;
            if (material == ArmorMaterials.NETHERITE) return EXCEPTIONAL;
            return STANDARD;
        }

        if (item instanceof TieredItem tiered) return fromTier(tiered.getTier());
        if (item == Items.SHEARS) return STANDARD;
        if (item instanceof SwordItem || item instanceof DiggerItem) return STANDARD;
        return NONE;
    }

    private static GiftQuality fromTier(Tier tier) {
        if (tier == Tiers.GOLD || tier == Tiers.DIAMOND) return FINE;
        if (tier == Tiers.NETHERITE) return EXCEPTIONAL;
        if (tier == Tiers.IRON) return STANDARD;
        if (tier == Tiers.WOOD || tier == Tiers.STONE) return SIMPLE;

        int level = tier.getLevel();
        if (level >= 4) return EXCEPTIONAL;
        if (level >= 3) return FINE;
        if (level >= 2) return STANDARD;
        return SIMPLE;
    }
}
