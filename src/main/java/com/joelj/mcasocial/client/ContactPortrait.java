package com.joelj.mcasocial.client;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.entity.ai.relationship.AgeState;
import forge.net.conczin.mca.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class ContactPortrait {
    private ContactPortrait() {}

    static void renderProfileHeader(GuiGraphics graphics, Font font, UUID uuid, int entityId,
                                    String name, Component personality, int left, int top,
                                    int nameY, int personalityY, float screenOffsetY,
                                    SocialInterfaceTheme theme, int nameColor, int labelColor,
                                    int personalityColor) {
        int portraitX = left + (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 38 : 25);
        int portraitSize = theme == SocialInterfaceTheme.LETTER ? 40 : 38;
        int textLeft = portraitX + portraitSize + 2;
        int textRight = left + theme.imageWidth()
                - (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 37 : 12);
        int textCenter = (textLeft + textRight) / 2;
        int textWidth = textRight - textLeft;

        render(graphics, uuid, entityId, portraitX, top + nameY, portraitSize, screenOffsetY, theme);
        drawFitted(graphics, font, Component.literal(name), textCenter, top + nameY,
                textWidth, nameColor);
        drawFitted(graphics, font,
                Component.translatable("gui.mcasocial.personality").append(":"),
                textCenter, top + personalityY, textWidth, labelColor);
        drawFitted(graphics, font, personality, textCenter, top + personalityY + 10,
                textWidth, personalityColor);
    }

    private static void drawFitted(GuiGraphics graphics, Font font, Component text,
                                   int centerX, int y, int maxWidth, int color) {
        String value = text.getString();
        int width = font.width(value);
        float scale = width > maxWidth ? Math.max(0.78F, maxWidth / (float) width) : 1.0F;
        if (width * scale > maxWidth) {
            String ellipsis = "...";
            value = font.plainSubstrByWidth(value,
                    Math.max(0, Math.round(maxWidth / scale) - font.width(ellipsis))) + ellipsis;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, value, 0, 0, color);
        graphics.pose().popPose();
    }

    static void render(GuiGraphics graphics, UUID uuid, int entityId, int x, int y, int size,
                       float screenOffsetY, SocialInterfaceTheme theme) {
        render(graphics, uuid, entityId, x, y, size, screenOffsetY, theme, true);
    }

    static void renderEmbedded(GuiGraphics graphics, UUID uuid, int x, int y, int size,
                               SocialInterfaceTheme theme) {
        render(graphics, uuid, -1, x, y, size, 0, theme, false);
    }

    static boolean refreshAt(UUID uuid, int entityId, int left, int top, int nameY,
                             float screenOffsetY, SocialInterfaceTheme theme,
                             double mouseX, double mouseY, int button) {
        if ((button != 0 && button != 1)
                || !isHovering(left, top, nameY, screenOffsetY, theme, mouseX, mouseY)) return false;
        UUID portraitUuid = uuid;
        if (portraitUuid == null && Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof VillagerEntityMCA) portraitUuid = entity.getUUID();
        }
        if (portraitUuid == null) return false;
        ContactPhotoCache.requestRefresh(portraitUuid);
        return true;
    }

    static void renderRefreshTooltip(GuiGraphics graphics, Font font, UUID uuid,
                                     int left, int top, int nameY, float screenOffsetY,
                                     SocialInterfaceTheme theme, int mouseX, int mouseY) {
        if (!isHovering(left, top, nameY, screenOffsetY, theme, mouseX, mouseY)) return;
        String key = ContactPhotoCache.isRefreshPending(uuid)
                ? "gui.mcasocial.portrait.pending" : "gui.mcasocial.portrait.retake";
        graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
    }

    private static boolean isHovering(int left, int top, int nameY, float screenOffsetY,
                                      SocialInterfaceTheme theme, double mouseX, double mouseY) {
        if (Math.abs(screenOffsetY) > 0.5F) return false;
        int size = theme == SocialInterfaceTheme.LETTER ? 40 : 38;
        int x = left + (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 38 : 25);
        int y = top + nameY;
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    private static void render(GuiGraphics graphics, UUID uuid, int entityId, int x, int y, int size,
                               float screenOffsetY, SocialInterfaceTheme theme, boolean framed) {
        int background = 0xFF313C4D;
        int border = theme == SocialInterfaceTheme.LETTER ? 0xFFD7B78A : 0xFF8FAECC;
        if (framed) fillRounded(graphics, x, y, size, border);
        int inset = framed ? 1 : 0;
        int photoX = x + inset;
        int photoY = y + inset;
        int photoSize = size - inset * 2;
        fillRounded(graphics, photoX, photoY, photoSize, background);

        UUID portraitUuid = uuid;
        if (portraitUuid == null && entityId >= 0 && Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof VillagerEntityMCA) portraitUuid = entity.getUUID();
        }

        ContactPhotoCache.Photo photo = ContactPhotoCache.get(portraitUuid);
        boolean refreshing = ContactPhotoCache.isRefreshPending(portraitUuid);
        if (photo != null && !refreshing) {
            drawPhoto(graphics, photo, photoX, photoY, photoSize);
            return;
        }

        VillagerEntityMCA villager = framed && entityId >= 0
                && ContactPhotoCache.shouldCapture(portraitUuid, screenOffsetY)
                ? find(portraitUuid, entityId) : null;
        if (villager == null || !villager.isAlive() || villager.isRemoved() || villager.isSleeping()) {
            if (photo != null) {
                drawPhoto(graphics, photo, photoX, photoY, photoSize);
                return;
            }
            int centerX = x + size / 2;
            int iconColor = theme == SocialInterfaceTheme.LETTER ? 0xFFDBC29F : 0xFFB8C8D8;
            graphics.fill(centerX - 3, y + size / 4, centerX + 3, y + size / 2, iconColor);
            graphics.fill(centerX - 6, y + size / 2 + 1, centerX + 6, y + size - 4, iconColor);
            return;
        }

        graphics.enableScissor(photoX, Math.round(photoY + screenOffsetY),
                photoX + photoSize, Math.round(photoY + photoSize + screenOffsetY));
        try {
            boolean baby = villager.getAgeState() == AgeState.BABY;
            float height = Math.max(0.6F, villager.getBbHeight());
            int scale = Math.round(photoSize * 3.0F / height);
            if (baby) {
                float headWidth = 0.5F * villager.getAgeState().getHead()
                        * villager.getRawHorizontalScaleFactor();
                scale = Math.round(photoSize * 0.8F / Math.max(0.2F, headWidth));
            }
            float headHeight = baby ? villager.getRawVerticalScaleFactor()
                    * (1.5F - 0.6F + 0.25F * villager.getAgeState().getHead())
                    : height * 0.82F;
            int centerY = Math.round(photoY + photoSize / 2.0F + scale * headHeight);
            Config config = Config.getInstance();
            boolean showNameTags = config.showNameTags;
            try {
                config.showNameTags = false;
                InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, photoX + photoSize / 2, centerY,
                        scale, 0.0F, 0.0F, villager);
            } finally {
                config.showNameTags = showNameTags;
            }
        } finally {
            graphics.disableScissor();
        }
        ContactPhotoCache.capture(portraitUuid, graphics, photoX, photoY, photoSize, screenOffsetY);
    }

    private static void drawPhoto(GuiGraphics graphics, ContactPhotoCache.Photo photo,
                                  int x, int y, int size) {
        graphics.blit(photo.texture(), x, y, size, size,
                0.0F, 0.0F, photo.pixelSize(), photo.pixelSize(),
                photo.pixelSize(), photo.pixelSize());
    }

    private static void fillRounded(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 3, y, x + size - 3, y + 1, color);
        graphics.fill(x + 1, y + 1, x + size - 1, y + 2, color);
        graphics.fill(x, y + 2, x + size, y + size - 2, color);
        graphics.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, color);
        graphics.fill(x + 3, y + size - 1, x + size - 3, y + size, color);
    }

    private static VillagerEntityMCA find(UUID uuid, int entityId) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (entityId >= 0) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof VillagerEntityMCA villager
                    && (uuid == null || uuid.equals(villager.getUUID()))) return villager;
        }
        return null;
    }
}
