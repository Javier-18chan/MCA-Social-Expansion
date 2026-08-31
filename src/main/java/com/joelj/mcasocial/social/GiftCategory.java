package com.joelj.mcasocial.social;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.Tags;

import java.util.Set;

public enum GiftCategory {
    SWEETS,
    FLOWERS,
    BOOKS,
    FOOD,
    TOOLS,
    WEAPONS,
    ARMOR,
    VALUABLES,
    NATURE,
    OTHER;

    private final TagKey<Item> itemTag = TagKey.create(
            Registries.ITEM, ResourceLocation.tryParse("mcasocial:gifts/" + name().toLowerCase())
    );

    private static final Set<Item> SWEET_ITEMS = Set.of(
            Items.CAKE, Items.COOKIE, Items.PUMPKIN_PIE, Items.HONEY_BOTTLE,
            Items.SWEET_BERRIES, Items.GLOW_BERRIES
    );
    private static final Set<Item> BOOK_ITEMS = Set.of(
            Items.BOOK, Items.WRITABLE_BOOK, Items.WRITTEN_BOOK, Items.ENCHANTED_BOOK
    );
    private static final Set<Item> VALUABLE_ITEMS = Set.of(
            Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.COPPER_INGOT, Items.NETHERITE_INGOT, Items.AMETHYST_SHARD,
            Items.LAPIS_LAZULI, Items.QUARTZ
    );
    private static final Set<Item> NATURE_ITEMS = Set.of(
            Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING,
            Items.MANGROVE_PROPAGULE, Items.CHERRY_SAPLING, Items.HONEYCOMB
    );

    public static GiftCategory classify(ItemStack stack) {
        Item item = stack.getItem();
        for (GiftCategory category : values()) {
            if (stack.is(category.itemTag)) return category;
        }
        if (SWEET_ITEMS.contains(item)) return SWEETS;
        if (stack.is(ItemTags.FLOWERS)) return FLOWERS;
        if (BOOK_ITEMS.contains(item)) return BOOKS;
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) {
            return WEAPONS;
        }
        if (item instanceof ArmorItem) return ARMOR;
        if (item instanceof DiggerItem || item == Items.SHEARS || item == Items.FISHING_ROD || item == Items.FLINT_AND_STEEL) {
            return TOOLS;
        }
        if (VALUABLE_ITEMS.contains(item) || stack.is(Tags.Items.GEMS) || stack.is(Tags.Items.INGOTS)) {
            return VALUABLES;
        }
        if (NATURE_ITEMS.contains(item)) return NATURE;
        if (stack.isEdible()) return FOOD;
        return OTHER;
    }

    public static GiftCategory fromId(String id) {
        if (id == null || id.isBlank()) return OTHER;
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }

    public String id() {
        return name().toLowerCase();
    }

    public String translationKey() {
        return "gui.mcasocial.gift.category." + id();
    }

    public TagKey<Item> itemTag() {
        return itemTag;
    }
}
