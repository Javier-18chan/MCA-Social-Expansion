package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ContactInteractPacket;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.SocialInteractPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public final class NarrativeChoiceScreen extends Screen {
    private final Screen previous;
    private final SocialInterfaceTheme theme;
    private final int entityId;
    private final UUID contactUuid;
    private final boolean family;
    private final String contactName;
    private final List<Choice> choices;
    private int leftPos;
    private int topPos;
    private int scrollOffset;

    private NarrativeChoiceScreen(Screen previous, int interfaceType, int entityId, UUID contactUuid,
                                  boolean family, String contactName, String titleKey, List<Choice> choices) {
        super(Component.translatable(titleKey));
        this.previous = previous;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.entityId = entityId;
        this.contactUuid = contactUuid;
        this.family = family;
        this.contactName = contactName;
        this.choices = choices;
    }

    public static NarrativeChoiceScreen insults(Screen previous, int interfaceType, int entityId,
                                                 UUID contactUuid, boolean family, String contactName) {
        return new NarrativeChoiceScreen(previous, interfaceType, entityId, contactUuid, family, contactName,
                "gui.mcasocial.narrative.insults", List.of(
                        new Choice("gui.mcasocial.narrative.insult.light", "insulto_leve"),
                        new Choice("gui.mcasocial.narrative.insult.direct", "insulto_directo"),
                        new Choice("gui.mcasocial.narrative.insult.harsh", "insulto_duro")));
    }

    public static NarrativeChoiceScreen topics(Screen previous, int interfaceType, int entityId,
                                                UUID contactUuid, boolean family, String contactName) {
        return new NarrativeChoiceScreen(previous, interfaceType, entityId, contactUuid, family, contactName,
                "gui.mcasocial.narrative.topics", List.of(
                        new Choice("gui.mcasocial.narrative.topic.interests", "tema_intereses"),
                        new Choice("gui.mcasocial.narrative.topic.plans", "tema_planes"),
                        new Choice("gui.mcasocial.narrative.topic.memories", "tema_recuerdos")));
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
        int rows = theme.friendshipRows();
        int total = choices.size() + 1;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, total - rows)));
        int buttonWidth = theme.menuButtonWidth();
        int startX = leftPos + (theme.imageWidth() - buttonWidth) / 2;
        for (int row = 0; row < rows && row + scrollOffset < total; row++) {
            int index = row + scrollOffset;
            int y = topPos + 59 + theme.panelOffset() + row * theme.friendshipStep();
            if (index == choices.size()) {
                addRenderableWidget(new SocialMenuButton(startX, y, buttonWidth, 30, theme.menuButtons(),
                        Component.translatable("gui.mcasocial.back"),
                        button -> Minecraft.getInstance().setScreen(previous)));
            } else {
                Choice choice = choices.get(index);
                addRenderableWidget(new SocialMenuButton(startX, y, buttonWidth, 30, theme.menuButtons(),
                        Component.translatable(choice.labelKey()), button -> sendAction(choice.action())));
            }
        }
    }

    private void sendAction(String action) {
        if (contactUuid == null) {
            ModMessages.sendToServer(new SocialInteractPacket(entityId, action, family));
        } else {
            ModMessages.sendToServer(new ContactInteractPacket(contactUuid, action, theme.id()));
        }
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, choices.size() + 1 - theme.friendshipRows());
        if (maxOffset == 0) return super.mouseScrolled(mouseX, mouseY, delta);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + (delta > 0 ? -1 : 1)));
        rebuildButtons();
        return true;
    }

    @Override public void renderBackground(GuiGraphics graphics) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90000000);
        theme.drawBackground(graphics, leftPos, topPos);
        int centerX = leftPos + theme.imageWidth() / 2;
        graphics.drawCenteredString(font, title, centerX,
                topPos + 31 + theme.panelOffset(), theme.titleColor());
        graphics.drawCenteredString(font, contactName, centerX,
                topPos + 43 + theme.panelOffset(), theme.accentColor());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }

    private record Choice(String labelKey, String action) {}
}
