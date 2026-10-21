package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.OpenInboxScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.joelj.mcasocial.social.DeliveryDelayMode;
import com.joelj.mcasocial.social.HumorActionCatalog;
import com.joelj.mcasocial.social.RelationshipActionCatalog;

import java.util.List;

public class InboxScreen extends Screen {
    private static final int ROW_STEP = 36;
    private final List<OpenInboxScreenPacket.Entry> entries;
    private final List<OpenInboxScreenPacket.IncomingEntry> incomingEntries;
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private final DeliveryDelayMode delayMode;
    private int leftPos;
    private int topPos;
    private int scrollOffset;

    public InboxScreen(List<OpenInboxScreenPacket.Entry> entries,
                       List<OpenInboxScreenPacket.IncomingEntry> incomingEntries,
                       int interfaceType, DeliveryDelayMode delayMode) {
        super(Component.translatable("gui.mcasocial.inbox.title"));
        this.entries = entries;
        this.incomingEntries = incomingEntries;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
        this.delayMode = delayMode;
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
        addRenderableWidget(new SocialMenuButton(
                startX, topPos + theme.imageHeight()
                        - (theme == SocialInterfaceTheme.MAGIC_MIRROR ? 92 : 65),
                buttonWidth, 30, theme.menuButtons(),
                Component.translatable("gui.mcasocial.back"),
                button -> Minecraft.getInstance().setScreen(new SocialHomeScreen(
                        interfaceType, delayMode, entries.size() +
                                (int) incomingEntries.stream().filter(entry -> !entry.replied()).count()))
        ));
        int rows = Math.min(visibleRows(), incomingEntries.size() + entries.size() - scrollOffset);
        for (int row = 0; row < rows; row++) {
            int index = scrollOffset + row;
            if (index >= incomingEntries.size()) continue;
            OpenInboxScreenPacket.IncomingEntry entry = incomingEntries.get(index);
            Component subtitle = entry.remainingSeconds() > 0
                    ? Component.translatable("gui.mcasocial.inbox.remaining", entry.remainingSeconds())
                    : Component.translatable(entry.replied()
                    ? "gui.mcasocial.spontaneous.replied" : "gui.mcasocial.spontaneous.received");
            addRenderableWidget(new IncomingMessageButton(startX, topPos + 52 + theme.panelOffset() + row * ROW_STEP,
                    buttonWidth, entry, subtitle));
        }
    }

    private int visibleRows() {
        return theme == SocialInterfaceTheme.PHONE || theme == SocialInterfaceTheme.MAGIC_MIRROR ? 3 : 4;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0x90000000);
        theme.drawBackground(guiGraphics, leftPos, topPos);
        int centerX = leftPos + theme.imageWidth() / 2;
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.inbox.title"),
                centerX, topPos + 28 + theme.panelOffset(), theme.titleColor());

        if (entries.isEmpty() && incomingEntries.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.inbox.empty"),
                    centerX, topPos + 91 + theme.panelOffset(), theme.mutedColor());
        } else {
            int maxRows = Math.min(visibleRows(), incomingEntries.size() + entries.size() - scrollOffset);
            for (int row = 0; row < maxRows; row++) {
                int index = scrollOffset + row - incomingEntries.size();
                if (index < 0 || index >= entries.size()) continue;
                int y = topPos + 52 + theme.panelOffset() + row * ROW_STEP;
                OpenInboxScreenPacket.Entry entry = entries.get(index);
                Component action = actionLabel(entry.action());
                Component line = Component.translatable("gui.mcasocial.inbox.entry", entry.contactName(), action);
                drawFittedLine(guiGraphics, line, centerX, y, theme.textColor());
                Component remaining = Component.translatable("gui.mcasocial.inbox.remaining", entry.remainingSeconds());
                drawFittedLine(guiGraphics, remaining, centerX, y + 11, theme.mutedColor());
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, incomingEntries.size() + entries.size() - visibleRows());
        if (maxOffset == 0) return super.mouseScrolled(mouseX, mouseY, delta);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + (delta > 0 ? -1 : 1)));
        rebuildButtons();
        return true;
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

    private final class IncomingMessageButton extends Button {
        private final OpenInboxScreenPacket.IncomingEntry entry;
        private final Component subtitle;

        private IncomingMessageButton(int x, int y, int width,
                                      OpenInboxScreenPacket.IncomingEntry entry, Component subtitle) {
            super(x, y, width, 30, Component.literal(entry.contactName()),
                    button -> Minecraft.getInstance().setScreen(new SpontaneousMessageScreen(entry, interfaceType)),
                    DEFAULT_NARRATION);
            this.entry = entry;
            this.subtitle = subtitle;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(theme.menuButtons(), getX(), getY(), width, height,
                    0.0F, isHoveredOrFocused() ? 32.0F : 0.0F, 117, 30, 256, 256);
            ContactPortrait.renderEmbedded(graphics, entry.contactUuid(), getX() + 1, getY() + 1, 28, theme);
            int textCenter = getX() + 29 + (width - 29) / 2;
            drawText(graphics, getMessage(), textCenter, getY() + 5,
                    isHoveredOrFocused() ? theme.accentColor() : theme.textColor());
            drawText(graphics, subtitle, textCenter, getY() + 16, theme.mutedColor());
        }

        private void drawText(GuiGraphics graphics, Component text, int centerX, int y, int color) {
            String value = text.getString();
            int maxWidth = width - 34;
            if (font.width(value) > maxWidth) {
                value = font.plainSubstrByWidth(value, maxWidth - font.width("...")) + "...";
            }
            graphics.drawCenteredString(font, value, centerX, y, color);
        }
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
            case "reconciliar" -> Component.translatable("gui.mcasocial.narrative.reconcile");
            case "insulto_leve" -> Component.translatable("gui.mcasocial.narrative.insult.light");
            case "insulto_directo" -> Component.translatable("gui.mcasocial.narrative.insult.direct");
            case "insulto_duro" -> Component.translatable("gui.mcasocial.narrative.insult.harsh");
            case "tema_intereses" -> Component.translatable("gui.mcasocial.narrative.topic.interests");
            case "tema_planes" -> Component.translatable("gui.mcasocial.narrative.topic.plans");
            case "tema_recuerdos" -> Component.translatable("gui.mcasocial.narrative.topic.memories");
            default -> Component.literal(action);
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
