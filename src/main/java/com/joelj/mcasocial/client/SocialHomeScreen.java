package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.RequestContactsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

public class SocialHomeScreen extends Screen {
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private int leftPos;
    private int topPos;
    private long timeOpened;

    public SocialHomeScreen(int interfaceType) {
        this(SocialInterfaceTheme.fromId(interfaceType));
    }

    private SocialHomeScreen(SocialInterfaceTheme theme) {
        super(Component.translatable(theme.homeTitleKey()));
        this.theme = theme;
        this.interfaceType = theme.id();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.theme.imageWidth()) / 2;
        this.topPos = (this.height - this.theme.imageHeight()) / 2;
        this.timeOpened = Util.getMillis();

        int buttonWidth = 117;
        int buttonHeight = 30;
        int startX = this.leftPos + ((this.theme.imageWidth() - buttonWidth) / 2);
        int startY = this.topPos + 92;

        this.addRenderableWidget(new SocialMenuButton(startX, startY, buttonWidth, buttonHeight, this.theme.menuButtons(),
                Component.translatable("gui.mcasocial.home.interface"), btn -> this.minecraft.setScreen(new InterfaceSettingsScreen(this.interfaceType))));
        this.addRenderableWidget(new SocialMenuButton(startX, startY + 40, buttonWidth, buttonHeight, this.theme.menuButtons(),
                Component.translatable("gui.mcasocial.home.contacts"), btn -> ModMessages.sendToServer(new RequestContactsPacket(this.interfaceType))));
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

        guiGraphics.blit(this.theme.background(), this.leftPos, this.topPos, 0, 0,
                this.theme.imageWidth(), this.theme.imageHeight(), this.theme.textureSize(), this.theme.textureSize());

        Component title = Component.translatable(this.theme.homeTitleKey());
        guiGraphics.drawCenteredString(this.font, title, centerX, this.topPos + 43, this.theme.titleColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.version"),
                centerX, this.topPos + 55, this.theme.mutedColor());
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.pose().popPose();
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
