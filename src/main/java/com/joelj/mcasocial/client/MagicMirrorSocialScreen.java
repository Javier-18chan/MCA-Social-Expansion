package com.joelj.mcasocial.client;

import com.joelj.mcasocial.MCASocialExpansion;
import com.joelj.mcasocial.social.SocialAgeStage;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.RelationshipActionCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class MagicMirrorSocialScreen extends Screen {
    private static final SocialInterfaceTheme THEME = SocialInterfaceTheme.MAGIC_MIRROR;
    private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(
            MCASocialExpansion.MODID, "textures/gui/magic_mirror_action_buttons.png");

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
    private final UUID contactUuid;
    private int leftPos;
    private int topPos;

    public MagicMirrorSocialScreen(int entityId, String name, String personality, int affinity, int romance,
                                   int trust, boolean baby, boolean minor, boolean family, boolean spouse,
                                   SocialAgeStage ageStage, UUID contactUuid) {
        super(Component.translatable(THEME.homeTitleKey()));
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
        this.contactUuid = contactUuid;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - THEME.imageWidth()) / 2;
        topPos = (height - THEME.imageHeight()) / 2;
        int left = leftPos + 26;
        int right = leftPos + 104;
        int y = topPos + 153;
        boolean affection = baby || minor || family;

        addRenderableWidget(new MirrorActionButton(left, y,
                Component.translatable("gui.mcasocial.conversation.title"), button -> openFriendship()));
        addRenderableWidget(new MirrorActionButton(right, y,
                Component.translatable(affection ? "gui.mcasocial.relationship.affection.title"
                        : "gui.mcasocial.relationship.romance.title"), button -> openRelationship()));
        addRenderableWidget(new MirrorActionButton(left, y + 32,
                Component.translatable("gui.mcasocial.humor.title"), button -> openHumor()));
        addRenderableWidget(new MirrorActionButton(right, y + 32,
                Component.translatable("gui.mcasocial.conflict.title"), button -> openConflict()));
        addRenderableWidget(new MirrorActionButton(left, y + 64,
                Component.translatable("gui.mcasocial.btn_gift"), button -> openGifts()));
        addRenderableWidget(new MirrorActionButton(right, y + 64,
                Component.translatable("gui.mcasocial.discovery.button"), button -> openInsight()));
        addRenderableWidget(new SocialIconButton(leftPos + 151, topPos + 57, 20, 20,
                THEME.settingsTexture(), THEME.settingsHoverTexture(),
                Component.translatable("gui.mcasocial.home.interface_style"),
                button -> Minecraft.getInstance().setScreen(
                        new InterfaceSettingsScreen(SocialInterfaceType.MAGIC_MIRROR))));
    }

    private void openFriendship() {
        Minecraft.getInstance().setScreen(new FriendshipScreen(entityId, name, personality, affinity, romance,
                trust, baby, minor, family, spouse, ageStage, SocialInterfaceType.MAGIC_MIRROR, contactUuid));
    }

    private void openRelationship() {
        Minecraft.getInstance().setScreen(new RelationshipScreen(entityId, name, personality, affinity, romance,
                trust, baby, minor, family, spouse, ageStage, SocialInterfaceType.MAGIC_MIRROR, contactUuid));
    }

    private void openHumor() {
        Minecraft.getInstance().setScreen(new HumorScreen(entityId, name, personality, affinity, romance,
                trust, baby, minor, family, spouse, ageStage, SocialInterfaceType.MAGIC_MIRROR, contactUuid));
    }

    private void openConflict() {
        Minecraft.getInstance().setScreen(new ConflictScreen(this, SocialInterfaceType.MAGIC_MIRROR,
                entityId, contactUuid, family, name));
    }

    private void openGifts() {
        if (contactUuid != null) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.mcasocial.contacts.remote_gift_disabled"), true);
            }
            return;
        }
        Minecraft.getInstance().setScreen(new GiftScreen(entityId, name, family));
    }

    private void openInsight() {
        Minecraft.getInstance().setScreen(new ContactInsightScreen(this, SocialInterfaceType.MAGIC_MIRROR,
                name, personality, ageStage));
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, width, height, 0x90000000);
        THEME.drawBackground(graphics, leftPos, topPos);
        int center = leftPos + THEME.imageWidth() / 2;
        graphics.drawCenteredString(font, Component.translatable(THEME.homeTitleKey())
                        .append(" ").append(Component.translatable("gui.mcasocial.version")),
                center, topPos + 61, 0xFFFFFF);
        ContactPortrait.renderProfileHeader(graphics, font, contactUuid, entityId, name,
                ClientSocialProfile.personalityDisplay(personality), leftPos, topPos,
                73, 95, 0, THEME, 0xFFFFFF, 0xF0EEE8, 0x7FDBFF);

        int friendshipTier = affinity >= 100 ? 3 : affinity >= 75 ? 2 : affinity >= 50 ? 1 : 0;
        Component friendship = Component.translatable("gui.mcasocial.label.amistad")
                .append(Component.literal(": " + affinity + " ("))
                .append(Component.translatable("gui.mcasocial.tag.amistad." + friendshipTier))
                .append(Component.literal(")"));
        drawMetric(graphics, friendship, center, topPos + (baby || minor || family ? 116 : 124),
                affinity < 0 ? 0xFF7777 : 0x55FFAA);
        if (baby || minor || family) {
            Component kinship = Component.translatable("gui.mcasocial.label.parentesco")
                    .append(Component.literal(": "))
                    .append(Component.translatable(family
                            ? "gui.mcasocial.tag.familiar" : "gui.mcasocial.tag.ninguno"));
            drawMetric(graphics, kinship, center, topPos + 127, 0xB7C9FF);
            drawMetric(graphics, Component.translatable("gui.mcasocial.relationship.trust")
                    .append(Component.literal(": " + trust + " ("))
                    .append(Component.translatable(RelationshipActionCatalog.trustTagKey(trust)))
                    .append(Component.literal(")")), center, topPos + 138, 0x55CCFF);
        } else {
            int romanceTier = spouse ? 4 : romance >= 75 ? 3 : romance >= 50 ? 2 : romance >= 25 ? 1 : 0;
            drawMetric(graphics, Component.translatable("gui.mcasocial.label.romance")
                    .append(Component.literal(": " + romance + " ("))
                    .append(Component.translatable("gui.mcasocial.tag.romance." + romanceTier))
                    .append(Component.literal(")")), center, topPos + 136,
                    romance < 0 ? 0xFF7777 : 0xFF77C8);
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (baby) {
            Component hint = Component.translatable("gui.mcasocial.hint.bebe");
            float scale = Math.min(0.85F, 150.0F / Math.max(1, font.width(hint)));
            graphics.pose().pushPose();
            graphics.pose().translate(center, topPos + 251, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawCenteredString(font, hint, 0, 0, 0xB5C9FF);
            graphics.pose().popPose();
        }
        ContactPortrait.renderRefreshTooltip(graphics, font, contactUuid, leftPos, topPos,
                73, 0, THEME, mouseX, mouseY);
    }

    private void drawMetric(GuiGraphics graphics, Component label, int centerX, int y, int color) {
        float scale = Math.min(1.0F, 150.0F / Math.max(1, font.width(label)));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, label, 0, 0, color);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ContactPortrait.refreshAt(contactUuid, entityId, leftPos, topPos,
                73, 0, THEME, mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static final class MirrorActionButton extends Button {
        MirrorActionButton(int x, int y, Component message, OnPress onPress) {
            super(x, y, 70, 24, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            graphics.blit(BUTTONS, getX(), getY(), 0, hovered ? 26 : 0,
                    width, height, 256, 256);
            var font = Minecraft.getInstance().font;
            float scale = Math.min(1.0F, 64.0F / Math.max(1, font.width(getMessage())));
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + width / 2.0F, getY() + 8, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawCenteredString(font, getMessage(), 0, 0,
                    hovered ? 0xFFE6C16C : 0xFFFFFF);
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
