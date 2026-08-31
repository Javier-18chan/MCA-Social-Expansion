package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.OpenInboxScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.joelj.mcasocial.social.DeliveryDelayMode;
import com.joelj.mcasocial.social.HumorActionCatalog;
import com.joelj.mcasocial.social.RelationshipActionCatalog;

import java.util.List;

public class InboxScreen extends Screen {
    private final List<OpenInboxScreenPacket.Entry> entries;
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private final DeliveryDelayMode delayMode;
    private int leftPos;
    private int topPos;

    public InboxScreen(List<OpenInboxScreenPacket.Entry> entries, int interfaceType, DeliveryDelayMode delayMode) {
        super(Component.translatable("gui.mcasocial.inbox.title"));
        this.entries = entries;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
        this.delayMode = delayMode;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - theme.imageWidth()) / 2;
        topPos = (height - theme.imageHeight()) / 2;
        int buttonWidth = 117;
        int startX = leftPos + (theme.imageWidth() - buttonWidth) / 2;
        addRenderableWidget(new SocialMenuButton(
                startX, topPos + theme.imageHeight() - 65, buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.back"),
                button -> Minecraft.getInstance().setScreen(new SocialHomeScreen(
                        interfaceType, delayMode, entries.size()))
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
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.inbox.title"),
                centerX, topPos + 28, theme.titleColor());

        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.inbox.empty"),
                    centerX, topPos + 91, theme.mutedColor());
        } else {
            int y = topPos + 52;
            int maxRows = Math.min(4, entries.size());
            for (int i = 0; i < maxRows; i++) {
                OpenInboxScreenPacket.Entry entry = entries.get(i);
                Component action = actionLabel(entry.action());
                Component line = Component.translatable("gui.mcasocial.inbox.entry", entry.contactName(), action);
                drawFittedLine(guiGraphics, line, centerX, y, theme.textColor());
                Component remaining = Component.translatable("gui.mcasocial.inbox.remaining", entry.remainingSeconds());
                drawFittedLine(guiGraphics, remaining, centerX, y + 11, theme.mutedColor());
                y += 30;
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawFittedLine(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int available = theme.imageWidth() - 24;
        int textWidth = font.width(text);
        float scale = textWidth <= available ? 1.0F : Math.max(0.68F, available / (float) textWidth);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(font, text, 0, 0, color);
        guiGraphics.pose().popPose();
    }

    private Component actionLabel(String action) {
        HumorActionCatalog.Definition humor = HumorActionCatalog.get(action);
        if (humor != null) return Component.translatable(humor.labelKey());
        RelationshipActionCatalog.Definition relationship = RelationshipActionCatalog.get(action);
        if (relationship != null) return Component.translatable(relationship.labelKey());
        return switch (action) {
            case "amistad_saludar" -> Component.translatable("gui.mcasocial.friendship.greet");
            case "preguntar_dia" -> Component.translatable("gui.mcasocial.friendship.ask_day");
            case "elogiar" -> Component.translatable("gui.mcasocial.btn_praise");
            case "preguntar_intereses" -> Component.translatable("gui.mcasocial.discovery.ask_interests");
            case "chismear" -> Component.translatable("gui.mcasocial.friendship.gossip");
            case "contar_anecdota" -> Component.translatable("gui.mcasocial.friendship.story");
            case "dar_apoyo" -> Component.translatable("gui.mcasocial.friendship.support");
            case "recordar_votos" -> Component.translatable("gui.mcasocial.friendship.spouse_vows");
            case "hablar_hogar" -> Component.translatable("gui.mcasocial.friendship.spouse_home");
            case "planear_juntos" -> Component.translatable("gui.mcasocial.friendship.spouse_future");
            case "criticar" -> Component.translatable("gui.mcasocial.conflict.criticize");
            case "confrontar" -> Component.translatable("gui.mcasocial.conflict.confront");
            case "insultar" -> Component.translatable("gui.mcasocial.conflict.insult");
            case "provocar" -> Component.translatable("gui.mcasocial.conflict.provoke");
            case "disculparse" -> Component.translatable("gui.mcasocial.conflict.apologize");
            default -> Component.literal(action);
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
