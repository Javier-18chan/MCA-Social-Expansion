package com.joelj.mcasocial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SocialMenuButton extends Button {
    private final ResourceLocation texture;
    private final Component subtitle;
    private final int subtitleColor;

    public SocialMenuButton(int x, int y, int width, int height, ResourceLocation texture, Component message, OnPress onPress) {
        this(x, y, width, height, texture, message, null, 0xFFFFFF, onPress);
    }

    public SocialMenuButton(int x, int y, int width, int height, ResourceLocation texture,
                            Component message, Component subtitle, int subtitleColor, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.subtitle = subtitle;
        this.subtitleColor = subtitleColor;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int texY = this.active ? (this.isHoveredOrFocused() ? 32 : 0) : 64;
        guiGraphics.blit(this.texture, this.getX(), this.getY(), 0, texY, this.width, this.height, 256, 256);
        int color = this.active ? (this.isHoveredOrFocused() ? 0xFFFF55 : 0xFFFFFF) : 0xAAAAAA;
        int centerX = this.getX() + this.width / 2;
        if (this.subtitle == null) {
            drawFittedText(guiGraphics, this.getMessage(), centerX,
                    this.getY() + (this.height - 8) / 2, color);
            return;
        }

        drawFittedText(guiGraphics, this.getMessage(), centerX, this.getY() + 5, color);
        drawFittedText(guiGraphics, this.subtitle, centerX, this.getY() + 16, this.subtitleColor);
    }

    private void drawFittedText(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        var font = Minecraft.getInstance().font;
        int textWidth = font.width(text);
        float scale = textWidth <= this.width - 8 ? 1.0F : Math.max(0.75F, (this.width - 8.0F) / textWidth);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(font, text, 0, 0, color);
        guiGraphics.pose().popPose();
    }
}
