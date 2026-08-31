package com.joelj.mcasocial.client;

import com.joelj.mcasocial.MCASocialExpansion;
import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.resources.ResourceLocation;

public enum SocialInterfaceTheme {
    PHONE(
            SocialInterfaceType.PHONE, 157, 251, 256,
            "textures/gui/smartphone_ui.png", "textures/gui/botones_menu_celular.png",
            "gui.mcasocial.home.phone_title", 4, 33, 5, 35
    ),
    LETTER(
            SocialInterfaceType.LETTER, 165, 279, 512,
            "textures/gui/carta_fondo.png", "textures/gui/botones_menu_carta.png",
            "gui.mcasocial.home.card_title", 4, 35, 5, 35
    ),
    CRYSTAL(
            SocialInterfaceType.CRYSTAL, 160, 278, 512,
            "textures/gui/crystal_ui.png", "textures/gui/crystal_buttons_2.png",
            "gui.mcasocial.home.crystal_title", 4, 35, 5, 35
    );

    private final int id;
    private final int imageWidth;
    private final int imageHeight;
    private final int textureSize;
    private final ResourceLocation background;
    private final ResourceLocation menuButtons;
    private final String homeTitleKey;
    private final int contactRows;
    private final int contactStep;
    private final int friendshipRows;
    private final int friendshipStep;

    SocialInterfaceTheme(int id, int imageWidth, int imageHeight, int textureSize,
                         String background, String menuButtons, String homeTitleKey,
                         int contactRows, int contactStep, int friendshipRows, int friendshipStep) {
        this.id = id;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.textureSize = textureSize;
        this.background = texture(background);
        this.menuButtons = texture(menuButtons);
        this.homeTitleKey = homeTitleKey;
        this.contactRows = contactRows;
        this.contactStep = contactStep;
        this.friendshipRows = friendshipRows;
        this.friendshipStep = friendshipStep;
    }

    public static SocialInterfaceTheme fromId(int id) {
        return switch (SocialInterfaceType.normalize(id)) {
            case SocialInterfaceType.LETTER -> LETTER;
            case SocialInterfaceType.CRYSTAL -> CRYSTAL;
            default -> PHONE;
        };
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(MCASocialExpansion.MODID, path);
    }

    public int id() { return id; }
    public int imageWidth() { return imageWidth; }
    public int imageHeight() { return imageHeight; }
    public int textureSize() { return textureSize; }
    public ResourceLocation background() { return background; }
    public ResourceLocation menuButtons() { return menuButtons; }
    public ResourceLocation filterButtons() {
        return switch (this) {
            case PHONE -> texture("textures/gui/boton_filtro_celular.png");
            case CRYSTAL -> texture("textures/gui/boton_filtro_crystal.png");
            case LETTER -> texture("textures/gui/boton_filtro.png");
        };
    }
    public ResourceLocation settingsTexture() {
        return switch (this) {
            case PHONE -> texture("textures/gui/tuerca_celular.png");
            case LETTER -> texture("textures/gui/tuerca_carta.png");
            case CRYSTAL -> texture("textures/gui/tuerca_crystal.png");
        };
    }
    public ResourceLocation settingsHoverTexture() {
        return switch (this) {
            case PHONE -> texture("textures/gui/tuerca_celular_hover.png");
            case LETTER -> texture("textures/gui/tuerca_carta_hover.png");
            case CRYSTAL -> texture("textures/gui/tuerca_crystal_hover.png");
        };
    }
    public String homeTitleKey() { return homeTitleKey; }
    public int contactRows() { return contactRows; }
    public int contactStep() { return contactStep; }
    public int friendshipRows() { return friendshipRows; }
    public int friendshipStep() { return friendshipStep; }
    public int titleColor() { return this == LETTER ? 0xFFFFFF : 0xFFFFFF; }
    public int textColor() { return this == LETTER ? 0xFFFFFF : 0xFFFFFF; }
    public int mutedColor() { return this == LETTER ? 0xE6E6E6 : 0xAAAAAA; }
    public int accentColor() { return this == LETTER ? 0xFFD54A : 0xFFFF55; }
    public int controlFillColor() { return this == LETTER ? 0xFFD7AA70 : 0xFF2C3444; }
    public int controlHoverColor() { return this == LETTER ? 0xFFE7C48E : 0xFF3A465C; }
    public int controlBorderColor() { return this == LETTER ? 0xFF6F3419 : 0xFF7EA4FF; }
}
