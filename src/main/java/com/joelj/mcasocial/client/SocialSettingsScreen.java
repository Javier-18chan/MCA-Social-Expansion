package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.UpdateSocialSettingsPacket;
import com.joelj.mcasocial.social.DeliveryDelayMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import net.minecraft.util.FormattedCharSequence;

public class SocialSettingsScreen extends Screen {
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private DeliveryDelayMode delayMode;
    private boolean spontaneousEnabled;
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
        this.spontaneousEnabled = ClientSocialSettings.isSpontaneousEnabled();
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
        int buttonWidth = theme.menuButtonWidth();
        int startX = leftPos + (theme.imageWidth() - buttonWidth) / 2;
        int startY = topPos + 106 + (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 36 : 0);

        addRenderableWidget(new SocialMenuButton(
                startX, startY, buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.settings.delay.button"),
                Component.translatable(delayMode.translationKey()), theme.accentColor(),
                button -> {
                    delayMode = delayMode.next();
                    ModMessages.sendToServer(new UpdateSocialSettingsPacket(delayMode, spontaneousEnabled));
                    rebuildButtons();
                }
        ));
        addRenderableWidget(new SocialMenuButton(
                startX, startY + 42, buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.settings.spontaneous"),
                Component.translatable(spontaneousEnabled ? "gui.mcasocial.settings.on" : "gui.mcasocial.settings.off"),
                theme.accentColor(),
                button -> {
                    spontaneousEnabled = !spontaneousEnabled;
                    ClientSocialSettings.setSpontaneousEnabled(spontaneousEnabled);
                    ModMessages.sendToServer(new UpdateSocialSettingsPacket(delayMode, spontaneousEnabled));
                    rebuildButtons();
                }
        ));
        addRenderableWidget(new SocialMenuButton(
                startX, startY + 84, buttonWidth, 30, theme.menuButtons(),
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
        theme.drawBackground(guiGraphics, leftPos, topPos);
        int centerX = leftPos + theme.imageWidth() / 2;
        int contentY = theme == SocialInterfaceTheme.MAGIC_MIRROR ? 28 : 0;
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.settings.title"),
                centerX, topPos + 31 + contentY, theme.titleColor());
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.settings.delay.title"),
                centerX, topPos + 55 + contentY, theme.accentColor());
        if (theme == SocialInterfaceTheme.MAGIC_MIRROR) {
            String description = Component.translatable("gui.mcasocial.settings.delay.description.1").getString()
                    + " " + Component.translatable("gui.mcasocial.settings.delay.description.2").getString()
                    + " " + Component.translatable("gui.mcasocial.settings.delay.description.3").getString();
            List<FormattedCharSequence> lines = font.split(Component.literal(description), 168);
            float scale = Math.min(0.88F, 44.0F / Math.max(1, lines.size() * 11));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, topPos + 94, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            for (int i = 0; i < lines.size(); i++) {
                guiGraphics.drawCenteredString(font, lines.get(i), 0, i * 11, theme.textColor());
            }
            guiGraphics.pose().popPose();
        } else {
            drawFittedLine(guiGraphics, Component.translatable("gui.mcasocial.settings.delay.description.1"), centerX, topPos + 68);
            drawFittedLine(guiGraphics, Component.translatable("gui.mcasocial.settings.delay.description.2"), centerX, topPos + 79);
            drawFittedLine(guiGraphics, Component.translatable("gui.mcasocial.settings.delay.description.3"), centerX, topPos + 90);
        }
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
