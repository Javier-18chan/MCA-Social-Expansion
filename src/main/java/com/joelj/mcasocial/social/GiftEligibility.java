package com.joelj.mcasocial.social;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public final class GiftEligibility {
    public static final TagKey<Item> INVALID_GIFTS = TagKey.create(
            Registries.ITEM, ResourceLocation.tryParse("mcasocial:invalid_gifts")
    );

    private GiftEligibility() {}

    public static boolean canGift(ItemStack stack) {
        if (stack.isEmpty() || stack.is(INVALID_GIFTS) || stack.getItem() instanceof SpawnEggItem) return false;
        GiftCategory category = GiftCategory.classify(stack);
        return category != GiftCategory.OTHER || stack.is(GiftCategory.OTHER.itemTag());
    }
}
