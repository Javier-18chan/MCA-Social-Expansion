package com.joelj.mcasocial.client;

import com.joelj.mcasocial.MCASocialExpansion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PhoneButton extends Button {
    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/smartphone_buttons.png");
    private static final int CORNER_ICON_SIZE = 24;
    private static final int CORNER_ICON_TEXTURE_SIZE = 128;
    private static final float CORNER_ICON_ALPHA = 0.95F;
    private static final float CORNER_ICON_HOVER_ALPHA = 0.70F;

    private final ResourceLocation cornerIcon;

    public PhoneButton(int x, int y, Component message, OnPress onPress) {
        this(x, y, message, onPress, null);
    }

    public PhoneButton(int x, int y, Component message, OnPress onPress, ResourceLocation cornerIcon) {
        super(x, y, 65, 22, message, onPress, DEFAULT_NARRATION);
        this.cornerIcon = cornerIcon;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int vOffset = this.isHovered() ? 24 : 0;
        guiGraphics.blit(BUTTON_TEXTURE, this.getX(), this.getY(), 0, vOffset, this.width, this.height, 256, 256);

        int textColor = this.isHovered() ? 0xDEFF9A : 0xFFFFFF;
        int textX = this.getX() + this.width / 2;
        int textY = this.getY() + (this.height - 8) / 2;

        renderCornerIcon(guiGraphics);
        guiGraphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                this.getMessage(), textX, textY, textColor);
    }

    private void renderCornerIcon(GuiGraphics guiGraphics) {
        if (this.cornerIcon == null) return;

        int iconX = this.getX() + this.width - CORNER_ICON_SIZE + 6;
        int iconY = this.getY() - CORNER_ICON_SIZE / 2;
        float scale = CORNER_ICON_SIZE / (float) CORNER_ICON_TEXTURE_SIZE;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(iconX, iconY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        float alpha = this.isHovered() ? CORNER_ICON_HOVER_ALPHA : CORNER_ICON_ALPHA;
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(this.cornerIcon, 0, 0, 0, 0,
                CORNER_ICON_TEXTURE_SIZE, CORNER_ICON_TEXTURE_SIZE,
                CORNER_ICON_TEXTURE_SIZE, CORNER_ICON_TEXTURE_SIZE);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.pose().popPose();
    }
}
