package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ContactInteractPacket;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.SocialInteractPacket;
import com.joelj.mcasocial.social.SocialAgeStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendshipScreen extends Screen {
    private final int entityId;
    private final String nombre;
    private final String personalidad;
    private final int afinidad;
    private final int romance;
    private final int trust;
    private final boolean isBaby;
    private final boolean isMinor;
    private final boolean isFamily;
    private final boolean isSpouse;
    private final SocialAgeStage ageStage;
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private final UUID contactUuid;

    private int leftPos;
    private int topPos;
    private int scrollOffset;

    public FriendshipScreen(int entityId, String nombre, String personalidad, int afinidad, int romance,
                            boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                            int interfaceType, UUID contactUuid) {
        this(entityId, nombre, personalidad, afinidad, romance, 8, isBaby, isMinor, isFamily, isSpouse,
                SocialAgeStage.fromFlags(isBaby, isMinor), interfaceType, contactUuid);
    }

    public FriendshipScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, int trust,
                            boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                            SocialAgeStage ageStage, int interfaceType, UUID contactUuid) {
        super(Component.translatable("gui.mcasocial.conversation.title"));
        this.entityId = entityId;
        this.nombre = nombre;
        this.personalidad = personalidad;
        this.afinidad = afinidad;
        this.romance = romance;
        this.trust = trust;
        this.isBaby = isBaby;
        this.isMinor = isMinor;
        this.isFamily = isFamily;
        this.isSpouse = isSpouse;
        this.ageStage = ageStage == null ? SocialAgeStage.fromFlags(isBaby, isMinor) : ageStage;
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

        int buttonWidth = 117;
        int buttonHeight = 30;
        int startX = this.leftPos + ((this.theme.imageWidth() - buttonWidth) / 2);
        int startY = this.topPos + 59;
        int stepY = this.theme.friendshipStep();
        List<MenuEntry> entries = menuEntries();
        int visibleRows = visibleRows();
        int maxOffset = Math.max(0, entries.size() - visibleRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        for (int row = 0; row < visibleRows; row++) {
            int entryIndex = this.scrollOffset + row;
            if (entryIndex >= entries.size()) break;

            MenuEntry entry = entries.get(entryIndex);
            int y = startY + (row * stepY);
            if (entry.action() == null) {
                this.addRenderableWidget(new SocialMenuButton(startX, y, buttonWidth, buttonHeight, this.theme.menuButtons(),
                        Component.translatable(entry.labelKey()), btn -> openPreviousScreen()));
            } else {
                addActionButton(startX, y, buttonWidth, buttonHeight, this.theme.menuButtons(), entry.labelKey(), entry.action());
            }
        }
    }

    private int visibleRows() {
        return this.theme.friendshipRows();
    }

    private List<MenuEntry> menuEntries() {
        List<MenuEntry> entries = new ArrayList<>(List.of(
                new MenuEntry("gui.mcasocial.friendship.greet", "amistad_saludar"),
                new MenuEntry("gui.mcasocial.friendship.ask_day", "preguntar_dia"),
                new MenuEntry("gui.mcasocial.btn_praise", "elogiar"),
                new MenuEntry("gui.mcasocial.discovery.ask_interests", "preguntar_intereses"),
                new MenuEntry("gui.mcasocial.friendship.gossip", "chismear"),
                new MenuEntry("gui.mcasocial.friendship.story", "contar_anecdota"),
                new MenuEntry("gui.mcasocial.friendship.support", "dar_apoyo")
        ));

        if (this.isSpouse && !this.isBaby && !this.isMinor) {
            entries.add(new MenuEntry("gui.mcasocial.friendship.spouse_vows", "recordar_votos"));
            entries.add(new MenuEntry("gui.mcasocial.friendship.spouse_home", "hablar_hogar"));
            entries.add(new MenuEntry("gui.mcasocial.friendship.spouse_future", "planear_juntos"));
        }

        entries.add(new MenuEntry("gui.mcasocial.friendship.back", null));
        return entries;
    }

    private void addActionButton(int x, int y, int width, int height, ResourceLocation buttons, String labelKey, String action) {
        this.addRenderableWidget(new SocialMenuButton(x, y, width, height, buttons, Component.translatable(labelKey), btn -> {
            if (this.contactUuid != null) {
                ModMessages.sendToServer(new ContactInteractPacket(this.contactUuid, action, this.interfaceType));
            } else {
                ModMessages.sendToServer(new SocialInteractPacket(this.entityId, action, this.isFamily));
            }
            this.onClose();
        }));
    }

    private void openPreviousScreen() {
        if (this.interfaceType == SocialInterfaceTheme.LETTER.id()) {
            Minecraft.getInstance().setScreen(new CardSocialScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                    this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse,
                    this.ageStage, this.contactUuid));
        } else if (this.interfaceType == SocialInterfaceTheme.CRYSTAL.id()) {
            Minecraft.getInstance().setScreen(new CrystalSocialScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                    this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse,
                    this.ageStage, this.contactUuid));
        } else {
            Minecraft.getInstance().setScreen(new SocialScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                    this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse,
                    this.ageStage, this.contactUuid));
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        guiGraphics.blit(this.theme.background(), this.leftPos, this.topPos, 0, 0,
                this.theme.imageWidth(), this.theme.imageHeight(), this.theme.textureSize(), this.theme.textureSize());
        int centerX = this.leftPos + this.theme.imageWidth() / 2;
        int titleY = this.topPos + (this.theme == SocialInterfaceTheme.PHONE ? 20 : 16);
        guiGraphics.drawCenteredString(this.font, Component.translatable(this.theme.homeTitleKey())
                        .append(" ").append(Component.translatable("gui.mcasocial.version")),
                centerX, titleY, this.theme.titleColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.conversation.title"),
                centerX, this.topPos + 32, this.theme.textColor());
        guiGraphics.drawCenteredString(this.font, this.nombre, centerX, this.topPos + 44, this.theme.accentColor());
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, menuEntries().size() - visibleRows());
        if (maxOffset <= 0) return super.mouseScrolled(mouseX, mouseY, delta);

        this.scrollOffset += delta > 0 ? -1 : 1;
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));
        rebuildButtons();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record MenuEntry(String labelKey, String action) {}
}
