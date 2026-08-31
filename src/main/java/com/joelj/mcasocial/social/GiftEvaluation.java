package com.joelj.mcasocial.social;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record GiftEvaluation(List<GiftCategory> categories, GiftQuality quality) {
    public GiftEvaluation {
        categories = List.copyOf(categories);
    }

    public static GiftEvaluation evaluate(ItemStack stack) {
        GiftCategory primary = GiftCategory.classify(stack);
        List<GiftCategory> categories = new ArrayList<>();
        categories.add(primary);

        for (GiftCategory category : GiftCategory.values()) {
            if (category != primary && category != GiftCategory.OTHER && stack.is(category.itemTag())) {
                categories.add(category);
            }
        }

        GiftQuality quality = GiftQuality.evaluate(stack);
        if (quality.addsValuableCategory()
                && (primary == GiftCategory.ARMOR || primary == GiftCategory.WEAPONS || primary == GiftCategory.TOOLS)
                && !categories.contains(GiftCategory.VALUABLES)) {
            categories.add(GiftCategory.VALUABLES);
        }
        return new GiftEvaluation(categories, quality);
    }

    public GiftCategory primaryCategory() {
        return categories.get(0);
    }

    public boolean hasCategory(GiftCategory category) {
        return categories.contains(category);
    }
}
