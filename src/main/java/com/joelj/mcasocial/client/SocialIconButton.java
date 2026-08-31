package com.joelj.mcasocial.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SocialIconButton extends Button {
    private final ResourceLocation texture;
    private final ResourceLocation hoverTexture;

    public SocialIconButton(int x, int y, int size, ResourceLocation texture, ResourceLocation hoverTexture,
                            Component narration, OnPress onPress) {
        super(x, y, size, size, narration, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.hoverTexture = hoverTexture;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation selected = isHoveredOrFocused() ? hoverTexture : texture;
        guiGraphics.blit(selected, getX(), getY(), 0, 0, width, height, width, height);
    }
}
