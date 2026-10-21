package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.RequestOpenContactPacket;
import com.joelj.mcasocial.network.ToggleContactFavoritePacket;
import com.joelj.mcasocial.social.SocialContact;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ContactListScreen extends Screen {
    private final List<SocialContact> contacts;
    private final List<SocialContact> visibleContacts = new ArrayList<>();
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private ContactFilter filter = ContactFilter.ALL;
    private ContactSort sort = ContactSort.RECENT;
    private String searchText = "";
    private EditBox searchBox;
    private boolean searchFocused;
    private int leftPos;
    private int topPos;
    private int scrollOffset;

    public ContactListScreen(List<SocialContact> contacts, int interfaceType) {
        super(Component.translatable("gui.mcasocial.contacts.title"));
        this.contacts = new ArrayList<>(contacts);
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
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
        rebuildVisibleContacts();

        int rowWidth = Math.min(132, this.theme.imageWidth() - 24);
        int startX = this.leftPos + ((this.theme.imageWidth() - rowWidth) / 2);
        int controlY = this.topPos + controlY();

        this.searchBox = new EditBox(this.font, startX, controlY, rowWidth, 16,
                Component.translatable("gui.mcasocial.contacts.search"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setSuggestion(this.searchText.isEmpty()
                ? Component.translatable("gui.mcasocial.contacts.search").getString()
                : null);
        this.searchBox.setFocused(this.searchFocused);
        this.addRenderableWidget(this.searchBox);
        if (this.searchFocused) {
            this.setFocused(this.searchBox);
        }

        int gap = 4;
        int smallWidth = (rowWidth - gap) / 2;
        int smallY = controlY + 20;
        this.addRenderableWidget(new FilterButton(startX, smallY, smallWidth, 18,
                Component.translatable(this.filter.key), btn -> {
                    this.filter = this.filter.next();
                    this.scrollOffset = 0;
                    rebuildButtons();
                }));
        this.addRenderableWidget(new FilterButton(startX + smallWidth + gap, smallY, smallWidth, 18,
                Component.translatable(this.sort.key), btn -> {
                    this.sort = this.sort.next();
                    this.scrollOffset = 0;
                    rebuildButtons();
                }));

        int visibleRows = visibleRows();
        int maxOffset = Math.max(0, this.visibleContacts.size() - visibleRows);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxOffset));

        int listY = this.topPos + listStartY();
        int stepY = rowStep();
        int favoriteWidth = 16;
        int buttonGap = 2;
        int buttonWidth = rowWidth - favoriteWidth - buttonGap;
        for (int row = 0; row < visibleRows; row++) {
            int contactIndex = this.scrollOffset + row;
            if (contactIndex >= this.visibleContacts.size()) break;

            SocialContact contact = this.visibleContacts.get(contactIndex);
            int y = listY + row * stepY;
            this.addRenderableWidget(new FavoriteButton(startX, y, favoriteWidth, 30, contact));
            this.addRenderableWidget(new ContactButton(startX + favoriteWidth + buttonGap, y, buttonWidth, 30, contact));
        }
    }

    private void rebuildVisibleContacts() {
        this.visibleContacts.clear();
        String query = this.searchText.trim().toLowerCase(Locale.ROOT);
        for (SocialContact contact : this.contacts) {
            if (!this.filter.matches(contact)) continue;
            if (!query.isEmpty() && !contactName(contact).toLowerCase(Locale.ROOT).contains(query)) continue;
            this.visibleContacts.add(contact);
        }
        this.visibleContacts.sort(this.sort.comparator());
    }

    private int controlY() {
        return switch (this.theme) {
            case PHONE -> 61;
            case LETTER -> 68;
            case CRYSTAL -> 70;
            case MAGIC_MIRROR -> 92;
        };
    }

    private int listStartY() {
        return switch (this.theme) {
            case PHONE -> 104;
            case LETTER -> 111;
            case CRYSTAL -> 113;
            case MAGIC_MIRROR -> 133;
        };
    }

    private int visibleRows() {
        return this.theme.contactRows();
    }

    private int rowStep() {
        return this.theme.contactStep();
    }

    private String contactName(SocialContact contact) {
        String name = contact.getName();
        return name == null || name.isBlank()
                ? Component.translatable("gui.mcasocial.contacts.unknown").getString()
                : name;
    }

    private Component contactSubtitle(SocialContact contact) {
        return Component.translatable("gui.mcasocial.contacts.subtitle",
                Component.translatable(contactTagKey(contact)), contact.getAffinity(), contact.getRomance());
    }

    private String contactTagKey(SocialContact contact) {
        if (contact.isSpouse() || contact.isFamily()) return "gui.mcasocial.contacts.tag.family";
        if (contact.getRomance() >= 25) return "gui.mcasocial.contacts.tag.romance";
        if (contact.getAffinity() >= 50) return "gui.mcasocial.contacts.tag.friend";
        return "gui.mcasocial.contacts.tag.known";
    }

    private int subtitleColor(SocialContact contact) {
        if (this.theme == SocialInterfaceTheme.LETTER) {
            if (contact.isSpouse()) return 0xFF77C8;
            if (contact.isFamily()) return 0x55CCFF;
            if (contact.getRomance() >= 25) return 0xFF99DD;
            if (contact.getAffinity() >= 50) return 0x55FF55;
            return this.theme.mutedColor();
        }
        if (contact.isSpouse()) return 0xFF77C8;
        if (contact.isFamily()) return 0x55CCFF;
        if (contact.getRomance() >= 25) return 0xFF99DD;
        if (contact.getAffinity() >= 50) return 0x55FF55;
        return 0xAAAAAA;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        this.theme.drawBackground(guiGraphics, this.leftPos, this.topPos);

        int centerX = this.leftPos + this.theme.imageWidth() / 2;
        int titleY = this.topPos + (this.theme == SocialInterfaceTheme.PHONE ? 20
                : this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 47 : 23);
        guiGraphics.drawCenteredString(this.font, Component.translatable(this.theme.homeTitleKey())
                        .append(" ").append(Component.translatable("gui.mcasocial.version")),
                centerX, titleY, titleColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.contacts.prompt.line1"),
                centerX, this.topPos + promptLineOneY(), textColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.contacts.prompt.line2"),
                centerX, this.topPos + promptLineTwoY(), textColor());

        Component count = this.contacts.isEmpty()
                ? Component.translatable("gui.mcasocial.contacts.empty")
                : Component.translatable("gui.mcasocial.contacts.count_filtered",
                this.visibleContacts.size(), this.contacts.size());
        guiGraphics.drawCenteredString(this.font, count, centerX, this.topPos + countY(), mutedColor());

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (!this.contacts.isEmpty() && this.visibleContacts.isEmpty()) {
            int noResultsY = this.topPos + listStartY() + 12;
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.contacts.no_results.line1"),
                    centerX, noResultsY, mutedColor());
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.contacts.no_results.line2"),
                    centerX, noResultsY + 10, mutedColor());
        }
    }

    private int promptLineOneY() {
        return this.theme == SocialInterfaceTheme.PHONE ? 32
                : this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 59 : 37;
    }

    private int promptLineTwoY() {
        return this.theme == SocialInterfaceTheme.PHONE ? 42
                : this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 69 : 48;
    }

    private int countY() {
        return this.theme == SocialInterfaceTheme.PHONE ? 52
                : this.theme == SocialInterfaceTheme.MAGIC_MIRROR ? 80 : 59;
    }

    private int titleColor() {
        return this.theme.titleColor();
    }

    private int textColor() {
        return this.theme.textColor();
    }

    private int mutedColor() {
        return this.theme.mutedColor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.visibleContacts.size() <= visibleRows()) return super.mouseScrolled(mouseX, mouseY, delta);
        this.scrollOffset += delta > 0 ? -1 : 1;
        rebuildButtons();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.searchFocused = this.searchBox != null && this.searchBox.isFocused();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        return syncSearchBox() || handled;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean handled = super.charTyped(codePoint, modifiers);
        return syncSearchBox() || handled;
    }

    private boolean syncSearchBox() {
        if (this.searchBox == null) return false;
        String value = this.searchBox.getValue();
        if (value.equals(this.searchText)) return false;
        this.searchText = value;
        this.searchFocused = true;
        this.scrollOffset = 0;
        rebuildButtons();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private enum ContactFilter {
        FAMILY("gui.mcasocial.contacts.filter.family") {
            @Override boolean matches(SocialContact contact) { return contact.isFamily() || contact.isSpouse(); }
        },
        FRIENDS("gui.mcasocial.contacts.filter.friends") {
            @Override boolean matches(SocialContact contact) {
                return contact.getAffinity() >= 50 && !contact.isFamily() && !contact.isSpouse();
            }
        },
        ROMANCE("gui.mcasocial.contacts.filter.romance") {
            @Override boolean matches(SocialContact contact) {
                return contact.getRomance() >= 25
                        && !contact.isBaby()
                        && !contact.isMinor()
                        && !contact.isFamily()
                        && !contact.isSpouse();
            }
        },
        KNOWN("gui.mcasocial.contacts.filter.known") {
            @Override boolean matches(SocialContact contact) {
                return contact.getAffinity() < 50 && contact.getRomance() < 25
                        && !contact.isFamily() && !contact.isSpouse();
            }
        },
        ALL("gui.mcasocial.contacts.filter.all") {
            @Override boolean matches(SocialContact contact) { return true; }
        };

        private final String key;

        ContactFilter(String key) {
            this.key = key;
        }

        abstract boolean matches(SocialContact contact);

        ContactFilter next() {
            ContactFilter[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private enum ContactSort {
        RECENT("gui.mcasocial.contacts.sort.recent") {
            @Override Comparator<SocialContact> comparator() {
                return Comparator.comparingLong(SocialContact::getLastSeenGameTime).reversed()
                        .thenComparing(ContactSort::safeName);
            }
        },
        NAME("gui.mcasocial.contacts.sort.name") {
            @Override Comparator<SocialContact> comparator() {
                return Comparator.comparing(ContactSort::safeName);
            }
        },
        AFFINITY("gui.mcasocial.contacts.sort.affinity") {
            @Override Comparator<SocialContact> comparator() {
                return Comparator.comparingInt(SocialContact::getAffinity).reversed()
                        .thenComparing(ContactSort::safeName);
            }
        },
        ROMANCE("gui.mcasocial.contacts.sort.romance") {
            @Override Comparator<SocialContact> comparator() {
                return Comparator.comparingInt(SocialContact::getRomance).reversed()
                        .thenComparing(ContactSort::safeName);
            }
        };

        private final String key;

        ContactSort(String key) {
            this.key = key;
        }

        abstract Comparator<SocialContact> comparator();

        ContactSort next() {
            ContactSort[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        private static String safeName(SocialContact contact) {
            String name = contact.getName();
            return name == null ? "" : name.toLowerCase(Locale.ROOT);
        }
    }

    private class ContactButton extends Button {
        private final SocialContact contact;

        ContactButton(int x, int y, int width, int height, SocialContact contact) {
            super(x, y, width, height, Component.literal(contactName(contact)),
                    btn -> ModMessages.sendToServer(new RequestOpenContactPacket(contact.getUuid(), ContactListScreen.this.interfaceType)),
                    Button.DEFAULT_NARRATION);
            this.contact = contact;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY()
                    && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            guiGraphics.blit(ContactListScreen.this.theme.menuButtons(), this.getX(), this.getY(),
                    this.width, this.height, 0.0F, hovered ? 32.0F : 0.0F, 117, 30, 256, 256);

            ContactPortrait.renderEmbedded(guiGraphics, this.contact.getUuid(),
                    this.getX() + 1, this.getY() + 1, 28, ContactListScreen.this.theme);
            int textCenter = this.getX() + 29 + (this.width - 29) / 2;
            drawFittedName(guiGraphics, this.getMessage(), textCenter, this.getY() + 5,
                    hovered ? ContactListScreen.this.theme.accentColor() : ContactListScreen.this.textColor());
            drawMetrics(guiGraphics, textCenter);
        }

        private void drawFittedName(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
            var font = Minecraft.getInstance().font;
            int textWidth = font.width(text);
            float scale = textWidth <= this.width - 33 ? 1.0F : Math.min(1.0F, (this.width - 33.0F) / textWidth);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawCenteredString(font, text, 0, 0, color);
            guiGraphics.pose().popPose();
        }

        private void drawMetrics(GuiGraphics guiGraphics, int textCenter) {
            boolean trustContact = (this.contact.isFamily() || this.contact.isBaby() || this.contact.isMinor())
                    && !this.contact.isSpouse();
            List<MetricSegment> segments = new ArrayList<>();
            segments.add(new MetricSegment(metricText("gui.mcasocial.label.amistad", this.contact.getAffinity()), 0x55FF55));
            segments.add(new MetricSegment(metricText(trustContact
                    ? "gui.mcasocial.relationship.trust"
                    : "gui.mcasocial.label.romance", trustContact ? this.contact.getTrust() : this.contact.getRomance()),
                    trustContact ? 0x55CCFF : 0xFF77C8));

            var font = Minecraft.getInstance().font;
            int spacing = 5;
            int totalWidth = 0;
            for (int i = 0; i < segments.size(); i++) {
                totalWidth += font.width(segments.get(i).text());
                if (i > 0) totalWidth += spacing;
            }

            float scale = totalWidth <= this.width - 33 ? 1.0F : Math.min(1.0F, (this.width - 33.0F) / totalWidth);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textCenter, this.getY() + 17, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);

            int x = -totalWidth / 2;
            for (MetricSegment segment : segments) {
                guiGraphics.drawString(font, segment.text(), x, 0, segment.color(), false);
                x += font.width(segment.text()) + spacing;
            }
            guiGraphics.pose().popPose();
        }

        private String metricText(String key, int value) {
            return Component.translatable(key).getString() + ":" + value;
        }
    }

    private record MetricSegment(String text, int color) {}

    private class FavoriteButton extends Button {
        private final SocialContact contact;

        FavoriteButton(int x, int y, int width, int height, SocialContact contact) {
            super(x, y, width, height, Component.literal(contact.isFavorite() ? "\u2605" : "\u2606"),
                    btn -> {}, Button.DEFAULT_NARRATION);
            this.contact = contact;
        }

        @Override
        public void onPress() {
            boolean favorite = !this.contact.isFavorite();
            this.contact.setFavorite(favorite);
            ModMessages.sendToServer(new ToggleContactFavoritePacket(this.contact.getUuid(), favorite));
            ContactListScreen.this.rebuildButtons();
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY()
                    && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            int color = this.contact.isFavorite()
                    ? 0xFFD54A
                    : hovered ? ContactListScreen.this.theme.accentColor() : ContactListScreen.this.mutedColor();
            guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                    Component.literal(this.contact.isFavorite() ? "\u2605" : "\u2606"),
                    this.getX() + this.width / 2, this.getY() + 10, color);
        }
    }

    private class FilterButton extends Button {
        FilterButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY()
                    && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            guiGraphics.blit(ContactListScreen.this.theme.filterButtons(), this.getX(), this.getY(),
                    0, hovered ? 18 : 0, this.width, this.height, 64, 64);
            drawFittedText(guiGraphics, this.getMessage(), this.getX() + this.width / 2,
                    this.getY() + 5, hovered ? ContactListScreen.this.theme.accentColor() : ContactListScreen.this.textColor());
        }

        private void drawFittedText(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
            var font = Minecraft.getInstance().font;
            int textWidth = font.width(text);
            float scale = textWidth <= this.width - 6 ? 1.0F : Math.max(0.72F, (this.width - 6.0F) / textWidth);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawCenteredString(font, text, 0, 0, color);
            guiGraphics.pose().popPose();
        }
    }
}
