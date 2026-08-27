package com.joelj.mcasocial.client;

import com.joelj.mcasocial.MCASocialExpansion;
import com.joelj.mcasocial.network.ContactInteractPacket;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.MorphInterfacePacket;
import com.joelj.mcasocial.network.SocialInteractPacket;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.SocialAgeStage;
import com.joelj.mcasocial.social.RelationshipActionCatalog;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class CrystalSocialScreen extends Screen {
    private static final ResourceLocation TEXTURE = texture("textures/gui/crystal_ui.png");
    private static final ResourceLocation BUTTONS = texture("textures/gui/crystal_buttons.png");
    private static final ResourceLocation MENU_BUTTONS = texture("textures/gui/crystal_buttons_2.png");
    private static final ResourceLocation GEAR = texture("textures/gui/tuerca_crystal.png");
    private static final ResourceLocation GEAR_HOVER = texture("textures/gui/tuerca_crystal_hover.png");
    private static final ResourceLocation EMOJI_AMISTAD = texture("textures/gui/emoji_amistad.png");
    private static final ResourceLocation EMOJI_ROMANCE = texture("textures/gui/emoji_romance.png");

    private static final int IMAGE_WIDTH = 160;
    private static final int IMAGE_HEIGHT = 278;
    private static final int GEAR_SIZE = 32;
    private static final int CORNER_ICON_SIZE = 24;
    private static final int CORNER_ICON_TEXTURE_SIZE = 128;
    private static final float CORNER_ICON_ALPHA = 0.95F;
    private static final float CORNER_ICON_HOVER_ALPHA = 0.70F;

    private int leftPos;
    private int topPos;
    private long timeOpened;
    private boolean gearMenuOpen;
    private long gearMenuOpenTime;

    public final int entityId;
    public final String nombre;
    public final String personalidad;
    public final int afinidad;
    public final int romance;
    public final int trust;
    public final boolean isBaby;
    public final boolean isMinor;
    public final boolean isFamily;
    public final boolean isSpouse;
    public final SocialAgeStage ageStage;
    public final UUID contactUuid;

    public CrystalSocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance,
                               boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse) {
        this(entityId, nombre, personalidad, afinidad, romance, isBaby, isMinor, isFamily, isSpouse, null);
    }

    public CrystalSocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance,
                               boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse, UUID contactUuid) {
        this(entityId, nombre, personalidad, afinidad, romance, 8, isBaby, isMinor, isFamily, isSpouse,
                SocialAgeStage.fromFlags(isBaby, isMinor), contactUuid);
    }

    public CrystalSocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, int trust,
                               boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                               SocialAgeStage ageStage, UUID contactUuid) {
        super(Component.translatable("gui.mcasocial.home.crystal_title"));
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
        this.contactUuid = contactUuid;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;
        this.timeOpened = Util.getMillis();

        int leftX = this.leftPos + 12;
        int rightX = this.leftPos + 83;
        int centerX = this.leftPos + IMAGE_WIDTH / 2;
        int startY = this.topPos + 112;
        int stepY = 28;

        Component friendship = Component.translatable("gui.mcasocial.friendship.title");
        Component humor = Component.translatable("gui.mcasocial.humor.title");
        Component praise = this.isBaby ? Component.literal("Mmmaa!") : Component.translatable("gui.mcasocial.btn_praise");
        Component insult = this.isBaby ? Component.literal("Bwaaa!") : Component.translatable("gui.mcasocial.btn_insult");
        boolean affection = this.isBaby || this.isMinor || this.isFamily;
        Component relationshipTitle = Component.translatable(affection
                ? "gui.mcasocial.relationship.affection.title"
                : "gui.mcasocial.relationship.romance.title");

        this.addRenderableWidget(new CrystalButton(leftX, startY, friendship,
                btn -> Minecraft.getInstance().setScreen(new FriendshipScreen(
                        this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance,
                        this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse,
                        this.ageStage, SocialInterfaceType.CRYSTAL, this.contactUuid)), EMOJI_AMISTAD));
        this.addRenderableWidget(new CrystalButton(rightX, startY, relationshipTitle,
                btn -> openRelationshipScreen(), EMOJI_ROMANCE));
        this.addRenderableWidget(new CrystalButton(leftX, startY + stepY, humor, btn -> openHumorScreen()));
        this.addRenderableWidget(new CrystalButton(rightX, startY + stepY, praise, btn -> sendAction("elogiar")));
        this.addRenderableWidget(new CrystalButton(leftX, startY + stepY * 2, insult, btn -> sendAction("insultar")));
        this.addRenderableWidget(new CrystalButton(rightX, startY + stepY * 2,
                Component.translatable("gui.mcasocial.btn_gift"), btn -> openGifts()));
    }

    public void morphToPhone() {
        Minecraft.getInstance().setScreen(new SocialScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage, this.contactUuid));
    }

    public void morphToCard() {
        Minecraft.getInstance().setScreen(new CardSocialScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage, this.contactUuid));
    }

    private void openRelationshipScreen() {
        Minecraft.getInstance().setScreen(new RelationshipScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage,
                SocialInterfaceType.CRYSTAL, this.contactUuid));
    }

    private void openHumorScreen() {
        Minecraft.getInstance().setScreen(new HumorScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage,
                SocialInterfaceType.CRYSTAL, this.contactUuid));
    }

    private void sendAction(String action) {
        if (this.contactUuid != null) {
            ModMessages.sendToServer(new ContactInteractPacket(this.contactUuid, action, SocialInterfaceType.CRYSTAL));
        } else {
            ModMessages.sendToServer(new SocialInteractPacket(this.entityId, action, this.isFamily));
        }
        this.onClose();
    }

    private void openGifts() {
        if (this.contactUuid != null) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.mcasocial.contacts.remote_gift_disabled"), true);
            }
            return;
        }
        Minecraft.getInstance().setScreen(new GiftScreen(this.entityId, this.nombre, this.isFamily));
    }

    private String traitKey() {
        if (this.personalidad == null) return "gui.mcasocial.trait.unknown";
        String trait = this.personalidad.toLowerCase()
                .replace("í", "i").replace("á", "a").replace("ó", "o");
        return "gui.mcasocial.trait." + trait;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);

        long elapsed = Util.getMillis() - this.timeOpened;
        float progress = Math.min(1.0F, elapsed / 350.0F);
        progress = 1.0F - (float) Math.pow(1.0F - progress, 3);
        float yOffset = this.height * (1.0F - progress);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, yOffset, 0);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                IMAGE_WIDTH, IMAGE_HEIGHT, 512, 512);

        int gearX = this.leftPos + IMAGE_WIDTH - 39;
        int gearY = this.topPos + 11;
        boolean gearHovered = mouseX >= gearX && mouseX <= gearX + GEAR_SIZE
                && mouseY >= gearY + yOffset && mouseY <= gearY + GEAR_SIZE + yOffset;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(gearX, gearY, 0);
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.blit(gearHovered ? GEAR_HOVER : GEAR, 0, 0, 0, 0, 64, 64, 64, 64);
        guiGraphics.pose().popPose();

        if (this.gearMenuOpen) {
            renderGearMenu(guiGraphics, mouseX, mouseY, yOffset);
        } else {
            renderSocialView(guiGraphics, mouseX, mouseY, partialTicks);
        }

        guiGraphics.pose().popPose();
    }

    private void renderSocialView(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int centerX = this.leftPos + IMAGE_WIDTH / 2;
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.home.crystal_title")
                        .append(" ").append(Component.translatable("gui.mcasocial.version")),
                centerX, this.topPos + 21, 0x7FDBFF);
        guiGraphics.drawCenteredString(this.font, this.nombre, centerX, this.topPos + 36, 0xFFFFFF);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, this.topPos + 55, 0);
        guiGraphics.pose().scale(0.95F, 0.95F, 1.0F);

        MutableComponent personality = Component.translatable("gui.mcasocial.personality")
                .append(Component.literal(": "))
                .append(Component.translatable(traitKey()));
        guiGraphics.drawCenteredString(this.font, personality, 0, 0, 0x91A7FF);

        MutableComponent friendshipTag = Component.translatable("gui.mcasocial.tag.amistad.0");
        if (this.afinidad >= 100) friendshipTag = Component.translatable("gui.mcasocial.tag.amistad.3");
        else if (this.afinidad >= 75) friendshipTag = Component.translatable("gui.mcasocial.tag.amistad.2");
        else if (this.afinidad >= 50) friendshipTag = Component.translatable("gui.mcasocial.tag.amistad.1");
        MutableComponent friendship = Component.translatable("gui.mcasocial.label.amistad")
                .append(Component.literal(": " + this.afinidad + " ("))
                .append(friendshipTag)
                .append(Component.literal(")"));
        guiGraphics.drawCenteredString(this.font, friendship, 0, 10, this.afinidad < 0 ? 0xFF5555 : 0x55FFAA);

        if (this.isBaby || this.isMinor || this.isFamily) {
            MutableComponent kinship = Component.translatable("gui.mcasocial.label.parentesco")
                    .append(Component.literal(": "))
                    .append(Component.translatable(this.isFamily
                            ? "gui.mcasocial.tag.familiar"
                            : "gui.mcasocial.tag.ninguno"));
            guiGraphics.drawCenteredString(this.font, kinship, 0, 20, 0xB7C9FF);
            MutableComponent trustTag = Component.translatable(RelationshipActionCatalog.trustTagKey(this.trust));
            MutableComponent trustLine = Component.translatable("gui.mcasocial.relationship.trust")
                    .append(Component.literal(": " + this.trust + " (")).append(trustTag).append(Component.literal(")"));
            guiGraphics.drawCenteredString(this.font, trustLine, 0, 30, 0x55CCFF);
        } else {
            MutableComponent romanceTag = Component.translatable("gui.mcasocial.tag.romance.0");
            if (this.isSpouse) romanceTag = Component.translatable("gui.mcasocial.tag.romance.4");
            else if (this.romance >= 75) romanceTag = Component.translatable("gui.mcasocial.tag.romance.3");
            else if (this.romance >= 50) romanceTag = Component.translatable("gui.mcasocial.tag.romance.2");
            else if (this.romance >= 25) romanceTag = Component.translatable("gui.mcasocial.tag.romance.1");
            MutableComponent romanceLine = Component.translatable("gui.mcasocial.label.romance")
                    .append(Component.literal(": " + this.romance + " ("))
                    .append(romanceTag)
                    .append(Component.literal(")"));
            guiGraphics.drawCenteredString(this.font, romanceLine, 0, 20,
                    this.romance < 0 ? 0xFF5555 : this.romance == 0 ? 0x8290B8 : 0xFF77C8);
        }
        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (this.isBaby) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, this.topPos + IMAGE_HEIGHT - 55, 0);
            guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.mcasocial.hint.bebe"), 0, 0, 0x91A7FF);
            guiGraphics.pose().popPose();
        } else if (this.afinidad < 10 && !this.isFamily && !this.isMinor) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, this.topPos + IMAGE_HEIGHT - 55, 0);
            guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.mcasocial.romance_hint"), 0, 0, 0x91A7FF);
            guiGraphics.pose().popPose();
        }
    }

    private void renderGearMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float yOffset) {
        float menuProgress = Math.min(1.0F, (Util.getMillis() - this.gearMenuOpenTime) / 200.0F);
        menuProgress = 1.0F - (float) Math.pow(1.0F - menuProgress, 3);
        int slideOffset = (int) (-30 * (1.0F - menuProgress));
        int centerX = this.leftPos + IMAGE_WIDTH / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, slideOffset, 0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.title"),
                centerX, this.topPos + 35, 0xB7C9FF);

        int startX = this.leftPos + (IMAGE_WIDTH - 117) / 2;
        int startY = this.topPos + 51;
        drawConversionRow(guiGraphics, mouseX, mouseY, yOffset, slideOffset, startX, startY,
                SocialInterfaceType.PHONE);
        drawConversionRow(guiGraphics, mouseX, mouseY, yOffset, slideOffset, startX, startY + 35,
                SocialInterfaceType.LETTER);

        int activeY = startY + 70;
        boolean activeHover = isRowHovered(mouseX, mouseY, yOffset, slideOffset, startX, activeY);
        guiGraphics.blit(MENU_BUTTONS, startX, activeY, 0, activeHover ? 32 : 0, 117, 30, 256, 256);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.crystal"),
                centerX, activeY + 10, activeHover ? 0x9EEBFF : 0xFFFFFF);

        for (int row = 3; row < 5; row++) {
            int y = startY + row * 35;
            guiGraphics.blit(MENU_BUTTONS, startX, y, 0, 64, 117, 30, 256, 256);
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.proximamente"),
                    centerX, y + 10, 0x888FA8);
        }
        guiGraphics.pose().popPose();
    }

    private void drawConversionRow(GuiGraphics guiGraphics, int mouseX, int mouseY, float yOffset, int slideOffset,
                                   int startX, int y, int targetInterface) {
        int centerX = this.leftPos + IMAGE_WIDTH / 2;
        boolean hover = isRowHovered(mouseX, mouseY, yOffset, slideOffset, startX, y);
        guiGraphics.blit(MENU_BUTTONS, startX, y, 0, hover ? 32 : 0, 117, 30, 256, 256);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.convert"),
                centerX, y + 3, hover ? 0x9EEBFF : 0x999999);

        int itemY = y + 12;
        if (targetInterface == SocialInterfaceType.PHONE) {
            guiGraphics.renderItem(new ItemStack(Items.IRON_INGOT), centerX - 47, itemY);
            guiGraphics.drawString(this.font, "x7", centerX - 31, itemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new ItemStack(Items.REDSTONE), centerX - 13, itemY);
            guiGraphics.drawString(this.font, "x1", centerX + 3, itemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new ItemStack(Items.GLASS_PANE), centerX + 21, itemY);
            guiGraphics.drawString(this.font, "x1", centerX + 37, itemY + 4, 0xCCCCCC, false);
        } else {
            guiGraphics.renderItem(new ItemStack(Items.PAPER), centerX - 32, itemY);
            guiGraphics.drawString(this.font, "x4", centerX - 16, itemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new ItemStack(Items.LEATHER), centerX + 4, itemY);
            guiGraphics.drawString(this.font, "x1", centerX + 20, itemY + 4, 0xCCCCCC, false);
        }
    }

    private boolean isRowHovered(double mouseX, double mouseY, float yOffset, int slideOffset, int startX, int y) {
        return mouseX >= startX && mouseX <= startX + 117
                && mouseY >= y + yOffset + slideOffset && mouseY <= y + 30 + yOffset + slideOffset;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            long elapsed = Util.getMillis() - this.timeOpened;
            float progress = Math.min(1.0F, elapsed / 350.0F);
            progress = 1.0F - (float) Math.pow(1.0F - progress, 3);
            float yOffset = this.height * (1.0F - progress);

            int gearX = this.leftPos + IMAGE_WIDTH - 39;
            int gearY = this.topPos + 11;
            if (mouseX >= gearX && mouseX <= gearX + GEAR_SIZE
                    && mouseY >= gearY + yOffset && mouseY <= gearY + GEAR_SIZE + yOffset) {
                this.gearMenuOpen = !this.gearMenuOpen;
                if (this.gearMenuOpen) this.gearMenuOpenTime = Util.getMillis();
                playClick(1.15F);
                return true;
            }

            if (this.gearMenuOpen) {
                float menuProgress = Math.min(1.0F, (Util.getMillis() - this.gearMenuOpenTime) / 200.0F);
                menuProgress = 1.0F - (float) Math.pow(1.0F - menuProgress, 3);
                int slideOffset = (int) (-30 * (1.0F - menuProgress));
                int startX = this.leftPos + (IMAGE_WIDTH - 117) / 2;
                int startY = this.topPos + 51;

                if (isRowHovered(mouseX, mouseY, yOffset, slideOffset, startX, startY)) {
                    playClick(0.8F);
                    ModMessages.sendToServer(new MorphInterfacePacket(SocialInterfaceType.PHONE));
                    return true;
                }
                if (isRowHovered(mouseX, mouseY, yOffset, slideOffset, startX, startY + 35)) {
                    playClick(0.8F);
                    ModMessages.sendToServer(new MorphInterfacePacket(SocialInterfaceType.LETTER));
                    return true;
                }
                if (isRowHovered(mouseX, mouseY, yOffset, slideOffset, startX, startY + 70)) {
                    this.gearMenuOpen = false;
                    return true;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClick(float pitch) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, pitch);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, path);
    }

    private static class CrystalButton extends Button {
        private final ResourceLocation cornerIcon;

        CrystalButton(int x, int y, Component message, OnPress onPress) {
            this(x, y, message, onPress, null);
        }

        CrystalButton(int x, int y, Component message, OnPress onPress, ResourceLocation cornerIcon) {
            super(x, y, 65, 22, message, onPress, DEFAULT_NARRATION);
            this.cornerIcon = cornerIcon;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();
            int textureY = hovered ? 24 : 0;
            guiGraphics.blit(BUTTONS, this.getX(), this.getY(), 0, textureY,
                    this.width, this.height, 256, 256);
            int color = hovered ? 0x9EEBFF : 0xFFFFFF;
            renderCornerIcon(guiGraphics, hovered);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, color);
        }

        private void renderCornerIcon(GuiGraphics guiGraphics, boolean hovered) {
            if (this.cornerIcon == null) return;

            int iconX = this.getX() + this.width - CORNER_ICON_SIZE + 6;
            int iconY = this.getY() - CORNER_ICON_SIZE / 2;
            float scale = CORNER_ICON_SIZE / (float) CORNER_ICON_TEXTURE_SIZE;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX, iconY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            float alpha = hovered ? CORNER_ICON_HOVER_ALPHA : CORNER_ICON_ALPHA;
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(this.cornerIcon, 0, 0, 0, 0,
                    CORNER_ICON_TEXTURE_SIZE, CORNER_ICON_TEXTURE_SIZE,
                    CORNER_ICON_TEXTURE_SIZE, CORNER_ICON_TEXTURE_SIZE);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.pose().popPose();
        }
    }
}
