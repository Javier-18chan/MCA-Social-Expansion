package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.UpdateSocialSettingsPacket;
import com.joelj.mcasocial.social.DeliveryDelayMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SocialSettingsScreen extends Screen {
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private DeliveryDelayMode delayMode;
    private final int pendingCount;
    private int leftPos;
    private int topPos;

    public SocialSettingsScreen(int interfaceType, DeliveryDelayMode delayMode) {
        this(interfaceType, delayMode, 0);
    }

    public SocialSettingsScreen(int interfaceType, DeliveryDelayMode delayMode, int pendingCount) {
        super(Component.translatable("gui.mcasocial.settings.title"));
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
        this.delayMode = delayMode == null ? DeliveryDelayMode.SHORT : delayMode;
        this.pendingCount = Math.max(0, pendingCount);
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - theme.imageWidth()) / 2;
        topPos = (height - theme.imageHeight()) / 2;
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int buttonWidth = 117;
        int startX = leftPos + (theme.imageWidth() - buttonWidth) / 2;
        int startY = topPos + 106;

        addRenderableWidget(new SocialMenuButton(
                startX, startY, buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.settings.delay.button"),
                Component.translatable(delayMode.translationKey()), theme.accentColor(),
                button -> {
                    delayMode = delayMode.next();
                    ModMessages.sendToServer(new UpdateSocialSettingsPacket(delayMode));
                    rebuildButtons();
                }
        ));
        addRenderableWidget(new SocialMenuButton(
                startX, startY + 42, buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.back"),
                button -> Minecraft.getInstance().setScreen(new SocialHomeScreen(
                        interfaceType, delayMode, pendingCount))
        ));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0x90000000);
        guiGraphics.blit(theme.background(), leftPos, topPos, 0, 0,
                theme.imageWidth(), theme.imageHeight(), theme.textureSize(), theme.textureSize());
        int centerX = leftPos + theme.imageWidth() / 2;
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.settings.title"),
                centerX, topPos + 31, theme.titleColor());
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.settings.delay.title"),
                centerX, topPos + 55, theme.accentColor());
        drawFittedLine(guiGraphics, Component.translatable("gui.mcasocial.settings.delay.description.1"), centerX, topPos + 68);
        drawFittedLine(guiGraphics, Component.translatable("gui.mcasocial.settings.delay.description.2"), centerX, topPos + 79);
        drawFittedLine(guiGraphics, Component.translatable("gui.mcasocial.settings.delay.description.3"), centerX, topPos + 90);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawFittedLine(GuiGraphics guiGraphics, Component text, int centerX, int y) {
        int available = theme.imageWidth() - 24;
        int textWidth = font.width(text);
        float scale = textWidth <= available ? 1.0F : Math.max(0.72F, available / (float) textWidth);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(font, text, 0, 0, theme.textColor());
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
