package com.joelj.mcasocial.client;

import com.joelj.mcasocial.social.GiftCategory;
import com.joelj.mcasocial.social.SocialAgeStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ContactInsightScreen extends Screen {
    private final Screen previousScreen;
    private final SocialInterfaceTheme theme;
    private final String contactName;
    private final String personality;
    private final SocialAgeStage ageStage;
    private int leftPos;
    private int topPos;

    public ContactInsightScreen(Screen previousScreen, int interfaceType, String contactName,
                                String personality, SocialAgeStage ageStage) {
        super(Component.translatable("gui.mcasocial.discovery.title"));
        this.previousScreen = previousScreen;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.contactName = contactName;
        this.personality = personality;
        this.ageStage = ageStage;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - theme.imageWidth()) / 2;
        topPos = (height - theme.imageHeight()) / 2;
        int buttonWidth = theme.menuButtonWidth();
        int startX = leftPos + (theme.imageWidth() - buttonWidth) / 2;
        addRenderableWidget(new SocialMenuButton(
                startX, topPos + theme.imageHeight()
                        - (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 92 : 65),
                buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.back"),
                button -> Minecraft.getInstance().setScreen(previousScreen)
        ));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0x90000000);
        theme.drawBackground(guiGraphics, leftPos, topPos);
        int centerX = leftPos + theme.imageWidth() / 2;
        int contentY = theme == SocialInterfaceTheme.MAGIC_MIRROR ? 28 : theme.panelOffset();
        int knowledge = ClientSocialProfile.knowledgeLevel();

        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.discovery.title"),
                centerX, topPos + 25 + contentY, theme.titleColor());
        guiGraphics.drawCenteredString(font, contactName, centerX,
                topPos + 38 + contentY, theme.accentColor());
        drawLine(guiGraphics, Component.translatable("gui.mcasocial.discovery.stage")
                .append(Component.literal(": "))
                .append(Component.translatable("gui.mcasocial.discovery.stage." + knowledge)), centerX, topPos + 58 + contentY, theme.textColor());
        drawLine(guiGraphics, Component.translatable("gui.mcasocial.personality")
                .append(Component.literal(": "))
                .append(ClientSocialProfile.personalityDisplay(personality)), centerX, topPos + 75 + contentY, 0x55CCFF);
        drawLine(guiGraphics, Component.translatable("gui.mcasocial.discovery.life_stage")
                .append(Component.literal(": "))
                .append(Component.translatable("gui.mcasocial.discovery.life_stage." + lifeStageKey())),
                centerX, topPos + 91 + contentY, theme.mutedColor());

        drawPreference(guiGraphics, "favorite", ClientSocialProfile.favoriteGiftCategory(), knowledge >= 2,
                centerX, topPos + (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 114 : 119) + contentY, 0xFF77C8);
        drawPreference(guiGraphics, "liked", ClientSocialProfile.likedGiftCategory(), knowledge >= 3,
                centerX, topPos + (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 132 : 139) + contentY, 0x55FF55);
        drawPreference(guiGraphics, "disliked", ClientSocialProfile.dislikedGiftCategory(), knowledge >= 4,
                centerX, topPos + (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 150 : 159) + contentY, 0xFF7777);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawPreference(GuiGraphics guiGraphics, String type, String category, boolean revealed,
                                int centerX, int y, int color) {
        Component value = revealed && category != null && !category.isBlank()
                ? Component.translatable(GiftCategory.fromId(category).translationKey())
                : Component.translatable("gui.mcasocial.discovery.unknown");
        drawLine(guiGraphics, Component.translatable("gui.mcasocial.discovery.preference." + type)
                .append(Component.literal(": ")).append(value), centerX, y, color);
    }

    private String lifeStageKey() {
        return switch (ageStage) {
            case BABY -> "baby";
            case CHILD -> "child";
            case TEEN -> "teen";
            default -> "adult";
        };
    }

    private void drawLine(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int available = theme.imageWidth() - 24;
        int textWidth = font.width(text);
        float scale = textWidth <= available ? 1.0F : Math.max(0.68F, available / (float) textWidth);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(font, text, 0, 0, color);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
