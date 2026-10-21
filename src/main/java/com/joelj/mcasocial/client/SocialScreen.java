package com.joelj.mcasocial.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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

public class SocialScreen extends Screen {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/smartphone_ui.png");
    private static final ResourceLocation TEX_TUERCA = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/tuerca_celular.png");
    private static final ResourceLocation TEX_TUERCA_HOVER = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/tuerca_celular_hover.png");
    private static final ResourceLocation TEX_BOTONES_MENU = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/botones_menu_celular.png");
    private static final ResourceLocation TEX_ITEM_CARTA = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/item/carta.png");
    private static final ResourceLocation EMOJI_AMISTAD = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/emoji_amistad.png");
    private static final ResourceLocation EMOJI_ROMANCE = ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, "textures/gui/emoji_romance.png");

    private final int imageWidth = 157;
    private final int imageHeight = 251;
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

    public SocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse) {
        this(entityId, nombre, personalidad, afinidad, romance, isBaby, isMinor, isFamily, isSpouse, null);
    }

    public SocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse, UUID contactUuid) {
        this(entityId, nombre, personalidad, afinidad, romance, 8, isBaby, isMinor, isFamily, isSpouse,
                SocialAgeStage.fromFlags(isBaby, isMinor), contactUuid);
    }

    public SocialScreen(int entityId, String nombre, String personalidad, int afinidad, int romance, int trust,
                        boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                        SocialAgeStage ageStage, UUID contactUuid) {
        super(Component.translatable("gui.mcasocial.title"));
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

        int margenIzq = this.leftPos + 11;
        int margenDer = this.leftPos + 79;
        int centroCelularX = this.leftPos + (this.imageWidth / 2);

        int startY = this.topPos + 120;
        int separacionY = 28;

        Component txtAmistad = Component.translatable("gui.mcasocial.conversation.title");
        Component txtHumor = Component.translatable("gui.mcasocial.humor.title");
        Component txtConflict = Component.translatable("gui.mcasocial.conflict.title");
        boolean affection = this.isBaby || this.isMinor || this.isFamily;
        Component relationshipTitle = Component.translatable(affection
                ? "gui.mcasocial.relationship.affection.title"
                : "gui.mcasocial.relationship.romance.title");

        this.addRenderableWidget(new PhoneButton(margenIzq, startY, txtAmistad,
                btn -> Minecraft.getInstance().setScreen(new FriendshipScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
                        this.romance, this.trust, this.isBaby, this.isMinor, this.isFamily, this.isSpouse,
                        this.ageStage, 0, this.contactUuid)), EMOJI_AMISTAD));
        this.addRenderableWidget(new PhoneButton(margenDer, startY, relationshipTitle,
                btn -> openRelationshipScreen(), EMOJI_ROMANCE));
        this.addRenderableWidget(new PhoneButton(margenIzq, startY + separacionY, txtHumor, btn -> openHumorScreen()));
        this.addRenderableWidget(new PhoneButton(margenDer, startY + separacionY, txtConflict, btn -> openConflictScreen()));
        this.addRenderableWidget(new PhoneButton(margenIzq, startY + (separacionY * 2),
                Component.translatable("gui.mcasocial.btn_gift"), btn -> abrirPantallaRegalos()));
        this.addRenderableWidget(new PhoneButton(margenDer, startY + (separacionY * 2),
                Component.translatable("gui.mcasocial.discovery.button"), btn -> openInsightScreen()));
    }

    public void morphToCard() {
        Minecraft.getInstance().setScreen(new CardSocialScreen(this.entityId, this.nombre, this.personalidad, this.afinidad,
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
                SocialInterfaceType.PHONE, this.contactUuid));
    }

    private void openHumorScreen() {
        Minecraft.getInstance().setScreen(new HumorScreen(
                this.entityId, this.nombre, this.personalidad, this.afinidad, this.romance, this.trust,
                this.isBaby, this.isMinor, this.isFamily, this.isSpouse, this.ageStage,
                SocialInterfaceType.PHONE, this.contactUuid));
    }

    private void openConflictScreen() {
        Minecraft.getInstance().setScreen(new ConflictScreen(
                this, SocialInterfaceType.PHONE, this.entityId, this.contactUuid, this.isFamily, this.nombre));
    }

    private void openInsightScreen() {
        Minecraft.getInstance().setScreen(new ContactInsightScreen(
                this, SocialInterfaceType.PHONE, this.nombre, this.personalidad, this.ageStage));
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

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        int tuercaX = this.leftPos + this.imageWidth - 28;
        int tuercaY = this.topPos + 18;
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

        int centroCelularX = this.leftPos + (this.imageWidth / 2);

        if (this.mostrarMenuTuerca) {
            float menuProgress = Math.min(1.0f, (Util.getMillis() - this.menuOpenTime) / 200.0f);
            menuProgress = 1.0f - (float) Math.pow(1.0f - menuProgress, 3);
            int slideOffset = (int) (-30 * (1.0f - menuProgress));

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, slideOffset, 0);

            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.title"), centroCelularX, this.topPos + 35, 0xAAAAAA);

            int btnWidth = 117;
            int btnHeight = 30;
            int startX = this.leftPos + ((this.imageWidth - btnWidth) / 2);
            int startY = this.topPos + 51;
            int separacion = 5;

            int y1 = startY;
            boolean hover1 = mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y1 + yOffset + slideOffset) && mouseY <= (y1 + btnHeight + yOffset + slideOffset);
            guiGraphics.blit(TEX_BOTONES_MENU, startX, y1, 0, hover1 ? 32 : 0, btnWidth, btnHeight, 256, 256);
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.celular"), centroCelularX, y1 + 10, hover1 ? 0xFFFF55 : 0xFFFFFF);

            int y2 = y1 + btnHeight + separacion;
            boolean hover2 = mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y2 + yOffset + slideOffset) && mouseY <= (y2 + btnHeight + yOffset + slideOffset);
            guiGraphics.blit(TEX_BOTONES_MENU, startX, y2, 0, hover2 ? 32 : 0, btnWidth, btnHeight, 256, 256);

            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.convert"), centroCelularX, y2 + 3, hover2 ? 0xFFFF55 : 0x999999);
            int itemY = y2 + 12;

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
            guiGraphics.blit(TEX_ITEM_CARTA, centroCelularX - 42, itemY, 0, 0, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER), centroCelularX - 24, itemY);
            guiGraphics.drawString(this.font, "x4", centroCelularX - 8, itemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER), centroCelularX + 8, itemY);
            guiGraphics.drawString(this.font, "x1", centroCelularX + 24, itemY + 4, 0xCCCCCC, false);

            int y3 = y2 + btnHeight + separacion;
            boolean hover3 = mouseX >= startX && mouseX <= startX + btnWidth
                    && mouseY >= y3 + yOffset + slideOffset && mouseY <= y3 + btnHeight + yOffset + slideOffset;
            guiGraphics.blit(TEX_BOTONES_MENU, startX, y3, 0, hover3 ? 32 : 0, btnWidth, btnHeight, 256, 256);
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.convert"),
                    centroCelularX, y3 + 3, hover3 ? 0xFFFF55 : 0x999999);
            int crystalItemY = y3 + 12;
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND), centroCelularX - 47, crystalItemY);
            guiGraphics.drawString(this.font, "x1", centroCelularX - 31, crystalItemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD), centroCelularX - 13, crystalItemY);
            guiGraphics.drawString(this.font, "x3", centroCelularX + 3, crystalItemY + 4, 0xCCCCCC, false);
            guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL), centroCelularX + 21, crystalItemY);
            guiGraphics.drawString(this.font, "x1", centroCelularX + 37, crystalItemY + 4, 0xCCCCCC, false);

            for (int i = 0; i < 2; i++) {
                int yFuturo = y3 + ((btnHeight + separacion) * (i + 1));
                guiGraphics.blit(TEX_BOTONES_MENU, startX, yFuturo, 0, 64, btnWidth, btnHeight, 256, 256);
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.proximamente"), centroCelularX, yFuturo + 10, 0xCCCCCC);
            }
            guiGraphics.pose().popPose();

        } else {

            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.title")
                    .append(" ").append(Component.translatable("gui.mcasocial.version")),
                    centroCelularX, this.topPos + 21, 0xFFFFFF);
            ContactPortrait.renderProfileHeader(guiGraphics, this.font, this.contactUuid, this.entityId,
                    this.nombre, ClientSocialProfile.personalityDisplay(this.personalidad),
                    this.leftPos, this.topPos, 36, 55, yOffset, SocialInterfaceTheme.PHONE,
                    0xFFFFFF, 0xAAAAAA, 0x00FFFF);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centroCelularX,
                    this.topPos + (this.isBaby || this.isMinor || this.isFamily ? 63 : 65), 0);
            guiGraphics.pose().scale(0.95f, 0.95f, 1.0f);

            MutableComponent tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.0");
            if (this.afinidad >= 100) tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.3");
            else if (this.afinidad >= 75) tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.2");
            else if (this.afinidad >= 50) tagAmistad = Component.translatable("gui.mcasocial.tag.amistad.1");
            int colorAfinidad = this.afinidad < 0 ? 0xFF5555 : 0x00FF00;
            MutableComponent labelAmistad = Component.translatable("gui.mcasocial.label.amistad").append(Component.literal(": " + this.afinidad + " (")).append(tagAmistad).append(Component.literal(")"));

            guiGraphics.drawCenteredString(this.font, labelAmistad, 0, 20, colorAfinidad);

            if (this.isBaby || this.isMinor || this.isFamily) {
                MutableComponent labelParentesco = Component.translatable("gui.mcasocial.label.parentesco").append(Component.literal(": "));
                MutableComponent tagParentesco = this.isFamily ? Component.translatable("gui.mcasocial.tag.familiar") : Component.translatable("gui.mcasocial.tag.ninguno");
                guiGraphics.drawCenteredString(this.font, labelParentesco.append(tagParentesco), 0, 30, 0x555555);
                MutableComponent trustTag = Component.translatable(RelationshipActionCatalog.trustTagKey(this.trust));
                MutableComponent trustLine = Component.translatable("gui.mcasocial.relationship.trust")
                        .append(Component.literal(": " + this.trust + " (")).append(trustTag).append(Component.literal(")"));
                guiGraphics.drawCenteredString(this.font, trustLine, 0, 40, 0x55CCFF);
            } else {
                MutableComponent tagRomance = Component.translatable("gui.mcasocial.tag.romance.0");
                if (this.isSpouse) tagRomance = Component.translatable("gui.mcasocial.tag.romance.4");
                else if (this.romance >= 75) tagRomance = Component.translatable("gui.mcasocial.tag.romance.3");
                else if (this.romance >= 50) tagRomance = Component.translatable("gui.mcasocial.tag.romance.2");
                else if (this.romance >= 25) tagRomance = Component.translatable("gui.mcasocial.tag.romance.1");
                int colorRomance = this.romance < 0 ? 0xFF5555 : (this.romance == 0 ? 0x555555 : 0xFF69B4);
                MutableComponent labelRomance = Component.translatable("gui.mcasocial.label.romance").append(Component.literal(": " + this.romance + " (")).append(tagRomance).append(Component.literal(")"));
                guiGraphics.drawCenteredString(this.font, labelRomance, 0, 30, colorRomance);
            }
            guiGraphics.pose().popPose();

            super.render(guiGraphics, mouseX, mouseY, partialTicks);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centroCelularX, this.topPos + this.imageHeight - 35, 0);
            guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
            if (this.isBaby) {
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.hint.bebe"), 0, 0, 0xAAAAAA);
            } else if (this.afinidad < 10 && !this.isFamily && !this.isMinor) {
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.romance_hint"), 0, 0, 0x555555);
            }
            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().popPose();
        if (!this.mostrarMenuTuerca) {
            ContactPortrait.renderRefreshTooltip(guiGraphics, this.font, this.contactUuid,
                    this.leftPos, this.topPos, 36, yOffset, SocialInterfaceTheme.PHONE, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && !this.mostrarMenuTuerca) {
            long elapsed = Util.getMillis() - this.timeOpened;
            float progress = Math.min(1.0F, elapsed / 350.0F);
            progress = 1.0F - (float) Math.pow(1.0F - progress, 3);
            float yOffset = this.height * (1.0F - progress);
            if (ContactPortrait.refreshAt(this.contactUuid, this.entityId, this.leftPos,
                    this.topPos, 36, yOffset, SocialInterfaceTheme.PHONE, mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0) {
            long elapsed = Util.getMillis() - this.timeOpened;
            float progress = Math.min(1.0f, elapsed / 350.0f);
            progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            float yOffset = this.height * (1.0f - progress);

            int tuercaX = this.leftPos + this.imageWidth - 28;
            int tuercaY = this.topPos + 18;
            int tamanoRenderTuerca = 16;

            if (mouseX >= tuercaX && mouseX <= tuercaX + tamanoRenderTuerca && mouseY >= (tuercaY + yOffset) && mouseY <= (tuercaY + tamanoRenderTuerca + yOffset)) {
                this.mostrarMenuTuerca = !this.mostrarMenuTuerca;
                if (this.mostrarMenuTuerca) this.menuOpenTime = Util.getMillis();
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 1.2F);
                }
                return true;
            }

            if (this.mostrarMenuTuerca) {
                int btnWidth = 117;
                int btnHeight = 30;
                int startX = this.leftPos + ((this.imageWidth - btnWidth) / 2);
                int startY = this.topPos + 51;

                float menuProgress = Math.min(1.0f, (Util.getMillis() - this.menuOpenTime) / 200.0f);
                menuProgress = 1.0f - (float) Math.pow(1.0f - menuProgress, 3);
                int slideOffset = (int) (-30 * (1.0f - menuProgress));

                if (mouseX >= this.leftPos && mouseX <= this.leftPos + this.imageWidth && mouseY >= (this.topPos + yOffset) && mouseY <= (this.topPos + this.imageHeight + yOffset)) {
                    int y1 = startY;
                    if (mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y1 + yOffset + slideOffset) && mouseY <= (y1 + btnHeight + yOffset + slideOffset)) {
                        this.mostrarMenuTuerca = false;
                        return true;
                    }

                    int y2 = y1 + btnHeight + 5;
                    if (mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= (y2 + yOffset + slideOffset) && mouseY <= (y2 + btnHeight + yOffset + slideOffset)) {
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 0.8F);
                        }

                        ModMessages.sendToServer(new MorphInterfacePacket(1));
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
