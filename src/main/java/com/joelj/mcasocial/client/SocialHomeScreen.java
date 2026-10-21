package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.RequestContactsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import com.joelj.mcasocial.network.RequestInboxPacket;
import com.joelj.mcasocial.social.DeliveryDelayMode;

public class SocialHomeScreen extends Screen {
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private int leftPos;
    private int topPos;
    private long timeOpened;
    private final DeliveryDelayMode delayMode;
    private final int pendingCount;
    private SocialIconButton settingsButton;

    public SocialHomeScreen(int interfaceType) {
        this(interfaceType, DeliveryDelayMode.SHORT, 0);
    }

    public SocialHomeScreen(int interfaceType, DeliveryDelayMode delayMode) {
        this(interfaceType, delayMode, 0);
    }

    public SocialHomeScreen(int interfaceType, DeliveryDelayMode delayMode, int pendingCount) {
        this(SocialInterfaceTheme.fromId(interfaceType), delayMode, pendingCount);
    }

    private SocialHomeScreen(SocialInterfaceTheme theme, DeliveryDelayMode delayMode, int pendingCount) {
        super(Component.translatable(theme.homeTitleKey()));
        this.theme = theme;
        this.interfaceType = theme.id();
        this.delayMode = delayMode == null ? DeliveryDelayMode.SHORT : delayMode;
        this.pendingCount = Math.max(0, pendingCount);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.theme.imageWidth()) / 2;
        this.topPos = (this.height - this.theme.imageHeight()) / 2;
        this.timeOpened = Util.getMillis();
        SocialUiSounds.playOpen(this.theme);

        int buttonWidth = this.theme.menuButtonWidth();
        int buttonHeight = 30;
        int startX = this.leftPos + ((this.theme.imageWidth() - buttonWidth) / 2);
        int startY = this.topPos + (this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 105 : 84);

        this.addRenderableWidget(new SocialMenuButton(startX, startY, buttonWidth, buttonHeight, this.theme.menuButtons(),
                Component.translatable("gui.mcasocial.home.contacts"), btn -> ModMessages.sendToServer(new RequestContactsPacket(this.interfaceType))));
        Component inboxSubtitle = this.pendingCount == 0
                ? Component.translatable("gui.mcasocial.home.inbox.empty")
                : Component.translatable("gui.mcasocial.home.inbox.pending", this.pendingCount);
        this.addRenderableWidget(new SocialMenuButton(startX, startY + 38, buttonWidth, buttonHeight, this.theme.menuButtons(),
                Component.translatable("gui.mcasocial.home.inbox"), inboxSubtitle, this.theme.mutedColor(),
                btn -> ModMessages.sendToServer(new RequestInboxPacket(this.interfaceType))));
        this.addRenderableWidget(new SocialMenuButton(startX, startY + 76, buttonWidth, buttonHeight, this.theme.menuButtons(),
                Component.translatable("gui.mcasocial.home.interface_style"),
                btn -> this.minecraft.setScreen(new InterfaceSettingsScreen(
                        this.interfaceType, this.delayMode, this.pendingCount))));

        int gearSize = this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 20 : 16;
        this.settingsButton = this.addRenderableWidget(new SocialIconButton(
                this.leftPos + (this.theme == SocialInterfaceTheme.MAGIC_MIRROR
                        ? 151 : this.theme.imageWidth() - 28),
                this.topPos + (this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 57 : 18),
                gearSize,
                this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 20
                        : this.theme == SocialInterfaceTheme.PHONE || this.theme == SocialInterfaceTheme.LETTER ? 32 : 64,
                this.theme.settingsTexture(),
                this.theme.settingsHoverTexture(),
                Component.translatable("gui.mcasocial.settings.title"),
                btn -> this.minecraft.setScreen(new SocialSettingsScreen(
                        this.interfaceType, this.delayMode, this.pendingCount))
        ));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);

        HomeMotion motion = homeMotion();
        int centerX = this.leftPos + this.theme.imageWidth() / 2;
        int centerY = this.topPos + this.theme.imageHeight() / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX + motion.xOffset(), centerY + motion.yOffset(), 0);
        guiGraphics.pose().scale(motion.scale(), motion.scale(), 1.0F);
        guiGraphics.pose().translate(-centerX, -centerY, 0);

        this.theme.drawBackground(guiGraphics, this.leftPos, this.topPos);

        Component title = Component.translatable(this.theme.homeTitleKey());
        guiGraphics.drawCenteredString(this.font, title, centerX,
                this.topPos + (this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 61 : 43),
                this.theme.titleColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.version"),
                centerX, this.topPos + (this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 74 : 55),
                this.theme.mutedColor());
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.pose().popPose();
        if (this.settingsButton != null && this.settingsButton.isHoveredOrFocused()) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.mcasocial.settings.tooltip"), mouseX, mouseY);
        }
    }

    private HomeMotion homeMotion() {
        float progress = Math.min(1.0F, (Util.getMillis() - this.timeOpened) / 420.0F);
        float eased = easeOutCubic(progress);
        return switch (this.theme) {
            case PHONE -> new HomeMotion(0.0F, 34.0F * (1.0F - eased), 0.96F + 0.04F * eased);
            case LETTER -> new HomeMotion(-28.0F * (1.0F - eased), -10.0F * (1.0F - eased),
                    0.98F + 0.02F * eased);
            case CRYSTAL -> {
                float elastic = easeOutBack(progress);
                yield new HomeMotion(0.0F, 18.0F * (1.0F - eased), 0.92F + 0.08F * elastic);
            }
            case MAGIC_MIRROR -> new HomeMotion(0.0F, 12.0F * (1.0F - eased),
                    0.96F + 0.04F * eased);
        };
    }

    private float easeOutCubic(float value) {
        return 1.0F - (float) Math.pow(1.0F - value, 3);
    }

    private float easeOutBack(float value) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(value - 1.0F, 3) + c1 * (float) Math.pow(value - 1.0F, 2);
    }

    private record HomeMotion(float xOffset, float yOffset, float scale) {}

    @Override
    public boolean isPauseScreen() { return false; }
}
