package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ContactInteractPacket;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.SocialInteractPacket;
import com.joelj.mcasocial.social.HumorActionCatalog;
import com.joelj.mcasocial.social.SocialAgeStage;
import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HumorScreen extends Screen {
    private final int entityId;
    private final String name;
    private final String personality;
    private final int affinity;
    private final int romance;
    private final int trust;
    private final boolean baby;
    private final boolean minor;
    private final boolean family;
    private final boolean spouse;
    private final SocialAgeStage ageStage;
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private final UUID contactUuid;

    private int leftPos;
    private int topPos;
    private int scrollOffset;

    public HumorScreen(int entityId, String name, String personality, int affinity, int romance, int trust,
                       boolean baby, boolean minor, boolean family, boolean spouse,
                       SocialAgeStage ageStage, int interfaceType, UUID contactUuid) {
        super(Component.translatable("gui.mcasocial.humor.title"));
        this.entityId = entityId;
        this.name = name;
        this.personality = personality;
        this.affinity = affinity;
        this.romance = romance;
        this.trust = trust;
        this.baby = baby;
        this.minor = minor;
        this.family = family;
        this.spouse = spouse;
        this.ageStage = ageStage == null ? SocialAgeStage.fromFlags(baby, minor) : ageStage;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
        this.contactUuid = contactUuid;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.theme.imageWidth()) / 2;
        this.topPos = (this.height - this.theme.imageHeight()) / 2;
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();
        List<MenuEntry> entries = menuEntries();
        int visibleRows = this.theme.friendshipRows();
        int maxOffset = Math.max(0, entries.size() - visibleRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        int buttonWidth = this.theme.menuButtonWidth();
        int buttonHeight = 30;
        int startX = this.leftPos + (this.theme.imageWidth() - buttonWidth) / 2;
        int startY = this.topPos + 59 + this.theme.panelOffset();

        for (int row = 0; row < visibleRows; row++) {
            int entryIndex = this.scrollOffset + row;
            if (entryIndex >= entries.size()) break;
            MenuEntry entry = entries.get(entryIndex);
            int y = startY + row * this.theme.friendshipStep();
            this.addRenderableWidget(new SocialMenuButton(startX, y, buttonWidth, buttonHeight,
                    this.theme.menuButtons(), Component.translatable(entry.labelKey()),
                    entry.action() == null ? ignored -> openPreviousScreen() : ignored -> sendAction(entry.action())));
        }
    }

    private List<MenuEntry> menuEntries() {
        List<MenuEntry> entries = new ArrayList<>();
        for (HumorActionCatalog.Definition definition : HumorActionCatalog.all()) {
            entries.add(new MenuEntry(definition.labelKey(), definition.id()));
        }
        entries.add(new MenuEntry("gui.mcasocial.friendship.back", null));
        return entries;
    }

    private void sendAction(String action) {
        if (this.contactUuid != null) {
            ModMessages.sendToServer(new ContactInteractPacket(this.contactUuid, action, this.interfaceType));
        } else {
            ModMessages.sendToServer(new SocialInteractPacket(this.entityId, action, this.family));
        }
        this.onClose();
    }

    private void openPreviousScreen() {
        if (this.interfaceType == SocialInterfaceType.LETTER) {
            Minecraft.getInstance().setScreen(new CardSocialScreen(
                    this.entityId, this.name, this.personality, this.affinity, this.romance, this.trust,
                    this.baby, this.minor, this.family, this.spouse, this.ageStage, this.contactUuid));
        } else if (this.interfaceType == SocialInterfaceType.CRYSTAL) {
            Minecraft.getInstance().setScreen(new CrystalSocialScreen(
                    this.entityId, this.name, this.personality, this.affinity, this.romance, this.trust,
                    this.baby, this.minor, this.family, this.spouse, this.ageStage, this.contactUuid));
        } else if (this.interfaceType == SocialInterfaceType.MAGIC_MIRROR) {
            Minecraft.getInstance().setScreen(new MagicMirrorSocialScreen(
                    this.entityId, this.name, this.personality, this.affinity, this.romance, this.trust,
                    this.baby, this.minor, this.family, this.spouse, this.ageStage, this.contactUuid));
        } else {
            Minecraft.getInstance().setScreen(new SocialScreen(
                    this.entityId, this.name, this.personality, this.affinity, this.romance, this.trust,
                    this.baby, this.minor, this.family, this.spouse, this.ageStage, this.contactUuid));
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        this.theme.drawBackground(guiGraphics, this.leftPos, this.topPos);

        int centerX = this.leftPos + this.theme.imageWidth() / 2;
        int titleY = this.topPos + (this.theme == SocialInterfaceTheme.PHONE ? 20 : 16)
                + this.theme.panelOffset();
        guiGraphics.drawCenteredString(this.font, Component.translatable(this.theme.homeTitleKey())
                        .append(" ").append(Component.translatable("gui.mcasocial.version")),
                centerX, titleY, this.theme.titleColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.humor.title"),
                centerX, this.topPos + 32 + this.theme.panelOffset(), this.theme.textColor());
        guiGraphics.drawCenteredString(this.font, this.name, centerX,
                this.topPos + 44 + this.theme.panelOffset(), this.theme.accentColor());
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, menuEntries().size() - this.theme.friendshipRows());
        if (maxOffset <= 0) return super.mouseScrolled(mouseX, mouseY, delta);
        this.scrollOffset += delta > 0 ? -1 : 1;
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));
        rebuildButtons();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record MenuEntry(String labelKey, String action) {}
}
