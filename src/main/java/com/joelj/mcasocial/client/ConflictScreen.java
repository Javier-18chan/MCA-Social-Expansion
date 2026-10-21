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

public class ConflictScreen extends Screen {
    private final Screen previousScreen;
    private final SocialInterfaceTheme theme;
    private final int entityId;
    private final UUID contactUuid;
    private final boolean family;
    private final String contactName;
    private int leftPos;
    private int topPos;
    private int scrollOffset;

    public ConflictScreen(Screen previousScreen, int interfaceType, int entityId, UUID contactUuid,
                          boolean family, String contactName) {
        super(Component.translatable("gui.mcasocial.conflict.title"));
        this.previousScreen = previousScreen;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.entityId = entityId;
        this.contactUuid = contactUuid;
        this.family = family;
        this.contactName = contactName;
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
        List<Entry> entries = List.of(
                new Entry("gui.mcasocial.conflict.criticize", "criticar"),
                new Entry("gui.mcasocial.conflict.confront", "confrontar"),
                new Entry("gui.mcasocial.conflict.insult", "open_insults"),
                new Entry("gui.mcasocial.conflict.provoke", "provocar"),
                new Entry("gui.mcasocial.conflict.apologize", "disculparse"),
                new Entry("gui.mcasocial.narrative.reconcile", "reconciliar"),
                new Entry("gui.mcasocial.back", null)
        );
        int visibleRows = theme.friendshipRows();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, entries.size() - visibleRows)));
        int buttonWidth = theme.menuButtonWidth();
        int startX = leftPos + (theme.imageWidth() - buttonWidth) / 2;
        int startY = topPos + 59 + theme.panelOffset();

        for (int row = 0; row < visibleRows; row++) {
            int index = scrollOffset + row;
            if (index >= entries.size()) break;
            Entry entry = entries.get(index);
            int y = startY + row * theme.friendshipStep();
            addRenderableWidget(new SocialMenuButton(
                    startX, y, buttonWidth, 30, theme.menuButtons(), Component.translatable(entry.labelKey()),
                    button -> {
                        if (entry.action() == null) {
                            Minecraft.getInstance().setScreen(previousScreen);
                        } else if (entry.action().equals("open_insults")) {
                            Minecraft.getInstance().setScreen(NarrativeChoiceScreen.insults(
                                    this, theme.id(), entityId, contactUuid, family, contactName));
                        } else {
                            sendAction(entry.action());
                        }
                    }
            ));
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
        int maxOffset = Math.max(0, 7 - theme.friendshipRows());
        if (maxOffset <= 0) return super.mouseScrolled(mouseX, mouseY, delta);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + (delta > 0 ? -1 : 1)));
        rebuildButtons();
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0x90000000);
        theme.drawBackground(guiGraphics, leftPos, topPos);
        int centerX = leftPos + theme.imageWidth() / 2;
        guiGraphics.drawCenteredString(font, Component.translatable("gui.mcasocial.conflict.title"),
                centerX, topPos + 31 + theme.panelOffset(), theme.titleColor());
        guiGraphics.drawCenteredString(font, contactName, centerX,
                topPos + 43 + theme.panelOffset(), theme.accentColor());
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Entry(String labelKey, String action) {}
}
