package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.GiftInteractPacket;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.social.GiftCategory;
import com.joelj.mcasocial.social.GiftEvaluation;
import com.joelj.mcasocial.social.GiftEligibility;
import com.joelj.mcasocial.social.GiftQuality;
import com.joelj.mcasocial.social.GiftRating;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GiftScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int SLOT_SIZE = 26;
    private static final int GAP = 4;

    private final int entityId;
    private final String contactName;
    private final boolean family;
    private final List<ItemStack> gifts = new ArrayList<>();
    private int page;

    public GiftScreen(int entityId, String contactName, boolean family) {
        super(Component.translatable("gui.mcasocial.gift_title", contactName));
        this.entityId = entityId;
        this.contactName = contactName;
        this.family = family;
    }

    @Override
    protected void init() {
        super.init();
        refreshGifts();
    }

    private void refreshGifts() {
        gifts.clear();
        if (Minecraft.getInstance().player == null) return;

        Set<Item> seen = new HashSet<>();
        var inventory = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (GiftEligibility.canGift(stack) && seen.add(stack.getItem())) gifts.add(stack.copyWithCount(1));
        }
        gifts.sort(Comparator.comparing(stack -> {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id == null ? "" : id.toString();
        }));
        page = Math.max(0, Math.min(page, maxPage()));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0xB0000000);
        int centerX = width / 2;
        Grid grid = grid();

        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.gift_title", contactName),
                centerX, grid.startY() - 34, 0xFFFF55);
        guiGraphics.drawCenteredString(font, preferenceHint(), centerX, grid.startY() - 21, 0xAAAAAA);

        if (gifts.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.gift.empty"),
                    centerX, grid.startY() + 42, 0xAAAAAA);
        }

        int firstIndex = page * PAGE_SIZE;
        int lastIndex = Math.min(gifts.size(), firstIndex + PAGE_SIZE);
        for (int index = firstIndex; index < lastIndex; index++) {
            int localIndex = index - firstIndex;
            int x = grid.startX() + (localIndex % COLUMNS) * (SLOT_SIZE + GAP);
            int y = grid.startY() + (localIndex / COLUMNS) * (SLOT_SIZE + GAP);
            boolean hovered = inside(mouseX, mouseY, x, y);
            guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF8A8A8A : 0xFF555555);
            guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xDD111111);
            ItemStack stack = gifts.get(index);
            guiGraphics.renderItem(stack, x + 5, y + 5);
            if (hovered) {
                List<Component> tooltip = new ArrayList<>(getTooltipFromItem(stack));
                GiftEvaluation evaluation = GiftEvaluation.evaluate(stack);
                tooltip.add(Component.translatable("gui.mcasocial.gift.tooltip.categories",
                        categoryList(evaluation)).withStyle(style -> style.withColor(0xAAAAAA)));
                if (evaluation.quality() != GiftQuality.NONE) {
                    tooltip.add(Component.translatable("gui.mcasocial.gift.tooltip.quality",
                            Component.translatable(evaluation.quality().translationKey()))
                            .withStyle(style -> style.withColor(0xAAAAAA)));
                }
                Component knownPreference = knownPreference(evaluation);
                if (knownPreference != null) tooltip.add(knownPreference);
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }

        if (maxPage() > 0) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.gift.page", page + 1, maxPage() + 1),
                    centerX, grid.startY() + ROWS * (SLOT_SIZE + GAP) + 4, 0xFFFFFF);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.gift.page_hint"),
                    centerX, grid.startY() + ROWS * (SLOT_SIZE + GAP) + 16, 0x888888);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private Component preferenceHint() {
        if (ClientSocialProfile.knowledgeLevel() >= 2 && !ClientSocialProfile.favoriteGiftCategory().isBlank()) {
            Component category = Component.translatable(
                    GiftCategory.fromId(ClientSocialProfile.favoriteGiftCategory()).translationKey());
            return Component.translatable("gui.mcasocial.gift.known_favorite", category);
        }
        return Component.translatable("gui.mcasocial.gift.preference_unknown");
    }

    private List<Component> getTooltipFromItem(ItemStack stack) {
        return Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
    }

    private Component categoryList(GiftEvaluation evaluation) {
        var result = Component.empty();
        for (int index = 0; index < evaluation.categories().size(); index++) {
            if (index > 0) result.append(Component.literal(" / "));
            result.append(Component.translatable(evaluation.categories().get(index).translationKey()));
        }
        return result;
    }

    private Component knownPreference(GiftEvaluation evaluation) {
        int knowledge = ClientSocialProfile.knowledgeLevel();
        GiftRating rating = knownRating(evaluation, knowledge);
        if (rating == GiftRating.FAVORITE) {
            return Component.translatable("gui.mcasocial.gift.tooltip.preference.favorite")
                    .withStyle(style -> style.withColor(0xFF77C8));
        }
        if (rating == GiftRating.LIKED) {
            return Component.translatable("gui.mcasocial.gift.tooltip.preference.liked")
                    .withStyle(style -> style.withColor(0x55FF55));
        }
        if (rating == GiftRating.DISLIKED) {
            return Component.translatable("gui.mcasocial.gift.tooltip.preference.disliked")
                    .withStyle(style -> style.withColor(0xFF7777));
        }
        if (knowledge >= 4) {
            return Component.translatable("gui.mcasocial.gift.tooltip.preference.neutral")
                    .withStyle(style -> style.withColor(0xAAAAAA));
        }
        return null;
    }

    private GiftRating knownRating(GiftEvaluation evaluation, int knowledge) {
        GiftCategory primary = evaluation.primaryCategory();
        GiftRating primaryRating = knownCategoryRating(primary, knowledge);
        if (primaryRating != GiftRating.NEUTRAL) return primaryRating;

        for (GiftCategory category : evaluation.categories()) {
            if (category == primary) continue;
            GiftRating rating = knownCategoryRating(category, knowledge);
            if (rating == GiftRating.FAVORITE || rating == GiftRating.LIKED) return rating;
        }
        return GiftRating.NEUTRAL;
    }

    private GiftRating knownCategoryRating(GiftCategory category, int knowledge) {
        if (knowledge >= 2 && category.id().equals(ClientSocialProfile.favoriteGiftCategory())) {
            return GiftRating.FAVORITE;
        }
        if (knowledge >= 3 && category.id().equals(ClientSocialProfile.likedGiftCategory())) {
            return GiftRating.LIKED;
        }
        if (knowledge >= 4 && category.id().equals(ClientSocialProfile.dislikedGiftCategory())) {
            return GiftRating.DISLIKED;
        }
        return GiftRating.NEUTRAL;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        Grid grid = grid();
        int firstIndex = page * PAGE_SIZE;
        int lastIndex = Math.min(gifts.size(), firstIndex + PAGE_SIZE);
        for (int index = firstIndex; index < lastIndex; index++) {
            int localIndex = index - firstIndex;
            int x = grid.startX() + (localIndex % COLUMNS) * (SLOT_SIZE + GAP);
            int y = grid.startY() + (localIndex / COLUMNS) * (SLOT_SIZE + GAP);
            if (!inside(mouseX, mouseY, x, y)) continue;

            ResourceLocation id = ForgeRegistries.ITEMS.getKey(gifts.get(index).getItem());
            if (id != null) {
                ModMessages.sendToServer(new GiftInteractPacket(entityId, id.toString(), family));
                onClose();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxPage() <= 0) return super.mouseScrolled(mouseX, mouseY, delta);
        page = Math.max(0, Math.min(maxPage(), page + (delta < 0 ? 1 : -1)));
        return true;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    private int maxPage() {
        return Math.max(0, (gifts.size() - 1) / PAGE_SIZE);
    }

    private Grid grid() {
        int gridWidth = COLUMNS * SLOT_SIZE + (COLUMNS - 1) * GAP;
        int gridHeight = ROWS * SLOT_SIZE + (ROWS - 1) * GAP;
        return new Grid((width - gridWidth) / 2, (height - gridHeight) / 2 + 10);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Grid(int startX, int startY) {}
}
