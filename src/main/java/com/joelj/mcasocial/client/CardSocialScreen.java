package com.joelj.mcasocial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import com.mojang.blaze3d.systems.RenderSystem;

import com.joelj.mcasocial.MCASocialExpansion;
import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.MorphInterfacePacket;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.SocialAgeStage;
import com.joelj.mcasocial.social.RelationshipActionCatalog;

import java.util.UUID;

public class CardSocialScreen extends Screen {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/carta_fondo.png");
    private static final ResourceLocation TEX_TUERCA = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/tuerca_carta.png");
    private static final ResourceLocation TEX_TUERCA_HOVER = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/tuerca_carta_hover.png");
    private static final ResourceLocation TEX_BOTONES_CARTA = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/carta_botones.png");
    private static final ResourceLocation TEX_BOTONES_MENU_CARTA = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/botones_menu_carta.png");
    private static final ResourceLocation TEX_ITEM_CELULAR = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/item/smartphone.png");
    private static final ResourceLocation EMOJI_AMISTAD = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/emoji_amistad.png");
    private static final ResourceLocation EMOJI_ROMANCE = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/emoji_romance.png");
    private static final int CORNER_ICON_SIZE = 24;
    private static final int CORNER_ICON_TEXTURE_SIZE = 128;
    private static final float CORNER_ICON_ALPHA = 0.95F;
    private static final float CORNER_ICON_HOVER_ALPHA = 0.70F;
    private static final int CARD_TITLE_COLOR = 0xFFFFFF;
    private static final int CARD_TEXT_COLOR = 0xFFFFFF;
    private static final int CARD_MUTED_COLOR = 0xE6E6E6;
    private static final int CARD_FRIENDSHIP_COLOR = 0x55FF55;
    private static final int CARD_ROMANCE_COLOR = 0xFF77C8;
    private static final int CARD_TRUST_COLOR = 0x55CCFF;

    private final int imageWidth = 165;
    private final int imageHeight = 279;
    private int leftPos;
    private int topPos;
    private long timeOpened;

    private boolean mostrarMenuTuerca = false;
    private long menuOpenTime = 0;

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

    public CardSocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse) {
        this(entityId, nombre, personalidad, afinidad, romance, isBaby, isMinor, isFamily, isSpouse, null);
    }

    public CardSocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse, UUID contactUuid) {
        this(entityId, nombre, personalidad, afinidad, romance, 8, isBaby, isMinor, isFamily, isSpouse,
                SocialAgeStage.fromFlags(isBaby, isMinor), contactUuid);
    }

    public CardSocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, int trust,
                            boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                            SocialAgeStage ageStage, UUID contactUuid) {
        super(Component.literal("Carta Social"));
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
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.timeOpened = Util.getMillis();

        int margenIzq = this.leftPos + 14;
        int margenDer = this.leftPos + 83;
        int centroX = this.leftPos + (this.imageWidth / 2);
        int startY = this.topPos + 122;
        int separacionY = 29;

        Component txtAmistad = Component.translatable("gui.mcasocial.conversation.title");
        Component txtHumor = Component.translatable("gui.mcasocial.humor.title");
        Component txtConflict = Component.translatable("gui.mcasocial.conflict.title");
        boolean affection = this.isBaby || this.isMinor || this.isFamily;
        Component relationshipTitle = Component.translatable(affection
                ? "gui.mcasocial.relationship.affection.title"
                : "gui.mcasocial.relationship.romance.title");

        this.addRenderableWidget(new CardButton(margenIzq, startY, txtAmistad,
                btn -> Minecraft.getInstance().setScreen(new FriendshipScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                        this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse,
                        this.ageStage, 1, this.contactUuid)), EMOJI_AMISTAD));
        this.addRenderableWidget(new CardButton(margenDer, startY, relationshipTitle,
                btn -> openRelationshipScreen(), EMOJI_ROMANCE));
        this.addRenderableWidget(new CardButton(margenIzq, startY + separacionY, txtHumor, btn -> openHumorScreen()));
        this.addRenderableWidget(new CardButton(margenDer, startY + separacionY, txtConflict, btn -> openConflictScreen()));
        this.addRenderableWidget(new CardButton(margenIzq, startY + (separacionY * 2),
                Component.translatable("gui.mcasocial.btn_gift"), btn -> abrirPantallaRegalos()));
        this.addRenderableWidget(new CardButton(margenDer, startY + (separacionY * 2),
                Component.translatable("gui.mcasocial.discovery.button"), btn -> openInsightScreen()));
    }

    public void morphToPhone() {
        Minecraft.getInstance().setScreen(new SocialScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage, this.contactUuid));
    }

    public void morphToCrystal() {
        Minecraft.getInstance().setScreen(new CrystalSocialScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage, this.contactUuid));
    }

    private void openRelationshipScreen() {
        Minecraft.getInstance().setScreen(new RelationshipScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage,
                SocialInterfaceType.LETTER, this.contactUuid));
    }

    private void openHumorScreen() {
        Minecraft.getInstance().setScreen(new HumorScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage,
                SocialInterfaceType.LETTER, this.contactUuid));
    }

    private void openConflictScreen() {
        Minecraft.getInstance().setScreen(new ConflictScreen(
                this, SocialInterfaceType.LETTER, this.entityId, this.contactUuid, this.isFamily, this.nombre));
    }

    private void openInsightScreen() {
        Minecraft.getInstance().setScreen(new ContactInsightScreen(
                this, SocialInterfaceType.LETTER, this.nombre, this.personalidad, this.ageStage));
    }

    private void abrirPantallaRegalos() {
        if (this.contactUuid != null) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("gui.mcasocial.contacts.remote_gift_disabled"), true);
            }
            return;
        }
        Minecraft.getInstance().setScreen(new GiftScreen(this.entityId, this.nombre, this.isFamily));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);

        long elapsed = Util.getMillis() - this.timeOpened;
        float progress = Math.min(1.0f, elapsed / 350.0f);
        progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
        float yOffset = this.height * (1.0f - progress);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, yOffset, 0);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 512, 512);

        int tuercaX = this.leftPos + this.imageWidth - 31;
        int tuercaY = this.topPos + 24;
        int tamanoRenderTuerca = 16;
        boolean mouseSobreTuerca = mouseX >= tuercaX && mouseX <= tuercaX + tamanoRenderTuerca && mouseY >= (tuercaY + yOffset) && mouseY <= (tuercaY + tamanoRenderTuerca + yOffset);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(tuercaX, tuercaY, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
        if (mouseSobreTuerca) {
            guiGraphics.blit(TEX_TUERCA_HOVER, 0, 0, 0, 0, 32, 32, 32, 32);
        } else {
            guiGraphics.blit(TEX_TUERCA, 0, 0, 0, 0, 32, 32, 32, 32);
        }
        guiGraphics.pose().popPose();

        int centroX = this.leftPos + (this.imageWidth / 2);

        if (this.mostrarMenuTuerca) {
            float menuProgress = Math.min(1.0f, (Util.getMillis() - this.menuOpenTime) / 200.0f);
            menuProgress = 1.0f - (float) Math.pow(1.0f - menuProgress, 3);
            int slideOffset = (int) (-30 * (1.0f - menuProgress));

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, slideOffset, 0);

            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.title"), centroX, this.topPos + 39, CARD_TITLE_COLOR);

            int btnWidth = 117;
            int btnHeight = 30;
            int startX = this.leftPos + ((this.imageWidth - btnWidth) / 2);
            int startY = this.topPos + 58;
            int separacion = 5;

            int y1 = startY;
            boolean hover1 = mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y1 + yOffset + slideOffset) && mouseY <= (y1 + btnHeight + yOffset + slideOffset);
            guiGraphics.blit(TEX_BOTONES_MENU_CARTA, startX, y1, 0, hover1 ? 32 : 0, btnWidth, btnHeight, 256, 256);

            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.convert"), centroX, y1 + 3, hover1 ? 0xFFFF55 : 0x999999);
            int itemY1 = y1 + 12;

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
            guiGraphics.blit(TEX_ITEM_CELULAR, centroX - 52, itemY1, 0, 0, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT), centroX - 35, itemY1);
            guiGraphics.drawString(this.font, "x7", centroX - 21, itemY1 + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE), centroX - 7, itemY1);
            guiGraphics.drawString(this.font, "x1", centroX + 7, itemY1 + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_PANE), centroX + 19, itemY1);
            guiGraphics.drawString(this.font, "x1", centroX + 33, itemY1 + 4, 0xCCCCCC, false);

            int y2 = y1 + btnHeight + separacion;
            boolean hover2 = mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y2 + yOffset + slideOffset) && mouseY <= (y2 + btnHeight + yOffset + slideOffset);
            guiGraphics.blit(TEX_BOTONES_MENU_CARTA, startX, y2, 0, hover2 ? 32 : 0, btnWidth, btnHeight, 256, 256);
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.carta_desbloqueada"), centroX, y2 + 10, hover2 ? 0xFFFF55 : 0xFFFFFF);

            int y3 = y2 + btnHeight + separacion;
            boolean hover3 = mouseX >= startX && mouseX <= startX + btnWidth
                    && mouseY >= y3 + yOffset + slideOffset && mouseY <= y3 + btnHeight + yOffset + slideOffset;
            guiGraphics.blit(TEX_BOTONES_MENU_CARTA, startX, y3, 0, hover3 ? 32 : 0, btnWidth, btnHeight, 256, 256);
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.convert"),
                    centroX, y3 + 3, hover3 ? 0xFFFF55 : 0x999999);
            int crystalItemY = y3 + 12;
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND), centroX - 47, crystalItemY);
            guiGraphics.drawString(this.font, "x1", centroX - 31, crystalItemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD), centroX - 13, crystalItemY);
            guiGraphics.drawString(this.font, "x3", centroX + 3, crystalItemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL), centroX + 21, crystalItemY);
            guiGraphics.drawString(this.font, "x1", centroX + 37, crystalItemY + 4, 0xCCCCCC, false);

            for (int i = 0; i < 2; i++) {
                int yFuturo = y3 + ((btnHeight + separacion) * (i + 1));
                guiGraphics.blit(TEX_BOTONES_MENU_CARTA, startX, yFuturo, 0, 64, btnWidth, btnHeight, 256, 256);
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.proximamente"), centroX, yFuturo + 10, 0xCCCCCC);
            }
            guiGraphics.pose().popPose();

        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.home.card_title")
                    .append(" ").append(Component.translatable("gui.mcasocial.version")),
                    centroX, this.topPos + 27, CARD_TITLE_COLOR);
            guiGraphics.drawCenteredString(this.font, this.nombre, centroX, this.topPos + 42, CARD_TEXT_COLOR);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centroX, this.topPos + 64, 0);
            guiGraphics.pose().scale(0.95f, 0.95f, 1.0f);

            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.mcasocial.personality").append(Component.literal(":")),
                    0, 0, CARD_MUTED_COLOR);
            guiGraphics.drawCenteredString(this.font, ClientSocialProfile.personalityDisplay(this.personalidad),
                    0, 10, CARD_TEXT_COLOR);

            MutableComponent tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.0");
            if (this.afinidad >= 100) tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.3");
            else if (this.afinidad >= 75) tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.2");
            else if (this.afinidad >= 50) tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.1");
            MutableComponent labelAmistad = Component.translatable("gui.mcasocial.label.amistad").append(Component.literal(": " + this.afinidad + " (")).append(tagAmistad).append(Component.literal(")"));

            guiGraphics.drawCenteredString(this.font, labelAmistad, 0, 20, CARD_FRIENDSHIP_COLOR);

            if (this.isBaby || this.isMinor || this.isFamily) {
                MutableComponent labelParentesco = Component.translatable("gui.mcasocial.label.parentesco").append(Component.literal(": "));
                MutableComponent tagParentesco = this.isFamily ? Component.translatable("gui.mcasocial.tag.familiar") : Component.translatable("gui.mcasocial.tag.ninguno");
                guiGraphics.drawCenteredString(this.font, labelParentesco.append(tagParentesco), 0, 30, CARD_TEXT_COLOR);
                MutableComponent trustTag = Component.translatable(RelationshipActionCatalog.trustTagKey(this.trust));
                MutableComponent trustLine = Component.translatable("gui.mcasocial.relationship.trust")
                        .append(Component.literal(": " + this.trust + " (")).append(trustTag).append(Component.literal(")"));
                guiGraphics.drawCenteredString(this.font, trustLine, 0, 40, CARD_TRUST_COLOR);
            } else {
                MutableComponent tagRomance = Component.translatable("gui.mcasocial.tag.romance.0");
                if (this.isSpouse) tagRomance = Component.translatable("gui.mcasocial.tag.romance.4");
                else if (this.romance >= 75) tagRomance = Component.translatable("gui.mcasocial.tag.romance.3");
                else if (this.romance >= 50) tagRomance = Component.translatable("gui.mcasocial.tag.romance.2");
                else if (this.romance >= 25) tagRomance = Component.translatable("gui.mcasocial.tag.romance.1");
                MutableComponent labelRomance = Component.translatable("gui.mcasocial.label.romance").append(Component.literal(": " + this.romance + " (")).append(tagRomance).append(Component.literal(")"));
                guiGraphics.drawCenteredString(this.font, labelRomance, 0, 30, CARD_ROMANCE_COLOR);
            }
            guiGraphics.pose().popPose();

            super.render(guiGraphics, mouseX, mouseY, partialTicks);

            if (this.isBaby) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(centroX, this.topPos + this.imageHeight - 45, 0);
                guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);
                guiGraphics.drawCenteredString(this.font,
                        Component.translatable("gui.mcasocial.hint.bebe"), 0, 0, CARD_MUTED_COLOR);
                guiGraphics.pose().popPose();
            } else if (this.afinidad < 10 && !this.isFamily && !this.isMinor) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(centroX, this.topPos + this.imageHeight - 45, 0);
                guiGraphics.pose().scale(0.85F, 0.85F, 1.0F);
                guiGraphics.drawCenteredString(this.font,
                        Component.translatable("gui.mcasocial.romance_hint"), 0, 0, CARD_MUTED_COLOR);
                guiGraphics.pose().popPose();
            }
        }

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            long elapsed = Util.getMillis() - this.timeOpened;
            float progress = Math.min(1.0f, elapsed / 350.0f);
            progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            float yOffset = this.height * (1.0f - progress);

            int tuercaX = this.leftPos + this.imageWidth - 31;
            int tuercaY = this.topPos + 24;
            int tamanoRenderTuerca = 16;

            if (mouseX >= tuercaX && mouseX <= tuercaX + tamanoRenderTuerca && mouseY >= (tuercaY + yOffset) && mouseY <= (tuercaY + tamanoRenderTuerca + yOffset)) {
                this.mostrarMenuTuerca = !this.mostrarMenuTuerca;
                if (this.mostrarMenuTuerca) this.menuOpenTime = Util.getMillis();
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 1.0F);
                }
                return true;
            }

            if (this.mostrarMenuTuerca) {
                int btnWidth = 117;
                int btnHeight = 30;
                int startX = this.leftPos + ((this.imageWidth - btnWidth) / 2);
                int startY = this.topPos + 58;

                float menuProgress = Math.min(1.0f, (Util.getMillis() - this.menuOpenTime) / 200.0f);
                menuProgress = 1.0f - (float) Math.pow(1.0f - menuProgress, 3);
                int slideOffset = (int) (-30 * (1.0f - menuProgress));

                int y1 = startY;
                if (mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y1 + yOffset + slideOffset) && mouseY <= (y1 + btnHeight + yOffset + slideOffset)) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 0.8F);
                    }

                    ModMessages.sendToServer(new MorphInterfacePacket(0));
                    return true;
                }

                int y2 = y1 + btnHeight + 5;
                if (mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y2 + yOffset + slideOffset) && mouseY <= (y2 + btnHeight + yOffset + slideOffset)) {
                    this.mostrarMenuTuerca = false;
                    return true;
                }

                int y3 = y2 + btnHeight + 5;
                if (mouseX >= startX && mouseX <= startX + btnWidth
                        && mouseY >= y3 + yOffset + slideOffset && mouseY <= y3 + btnHeight + yOffset + slideOffset) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 1.2F);
                    }
                    ModMessages.sendToServer(new MorphInterfacePacket(SocialInterfaceType.CRYSTAL));
                    return true;
                }

                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private class CardButton extends Button {
        private final ResourceLocation cornerIcon;

        public CardButton(int x, int y, Component message, OnPress onPress) {
            this(x, y, message, onPress, null);
        }

        public CardButton(int x, int y, Component message, OnPress onPress, ResourceLocation cornerIcon) {
            super(x, y, 68, 24, message, onPress, Button.DEFAULT_NARRATION);
            this.cornerIcon = cornerIcon;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            int texY = isHovered ? 24 : 0;
            guiGraphics.blit(TEX_BOTONES_CARTA, this.getX(), this.getY(), 0, texY, this.width, this.height, 256, 256);
            int color = isHovered ? 0xFFFF55 : 0xFFFFFF;
            renderCornerIcon(guiGraphics, isHovered);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, color);
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
