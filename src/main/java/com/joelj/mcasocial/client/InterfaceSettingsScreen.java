package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.MorphInterfacePacket;
import com.joelj.mcasocial.item.ModItems;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InterfaceSettingsScreen extends Screen {
    private static final float TARGET_ICON_ALPHA = 0.42F;

    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private int leftPos;
    private int topPos;

    public InterfaceSettingsScreen(int interfaceType) {
        super(Component.translatable("gui.mcasocial.menu.title"));
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.theme.imageWidth()) / 2;
        this.topPos = (this.height - this.theme.imageHeight()) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        guiGraphics.blit(this.theme.background(), this.leftPos, this.topPos, 0, 0,
                this.theme.imageWidth(), this.theme.imageHeight(), this.theme.textureSize(), this.theme.textureSize());

        int centerX = this.leftPos + this.theme.imageWidth() / 2;
        guiGraphics.drawCenteredString(this.font, Component.translatable(this.theme.homeTitleKey())
                        .append(" ").append(Component.translatable("gui.mcasocial.version")),
                centerX, this.topPos + 16, this.theme.titleColor());
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.title"),
                centerX, this.topPos + 32, this.theme.textColor());

        int startX = this.leftPos + (this.theme.imageWidth() - 117) / 2;
        int startY = this.topPos + 65;
        drawInterfaceRow(guiGraphics, mouseX, mouseY, startX, startY,
                SocialInterfaceType.PHONE);
        drawInterfaceRow(guiGraphics, mouseX, mouseY, startX, startY + 35,
                SocialInterfaceType.LETTER);
        drawInterfaceRow(guiGraphics, mouseX, mouseY, startX, startY + 70,
                SocialInterfaceType.CRYSTAL);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawInterfaceRow(GuiGraphics guiGraphics, int mouseX, int mouseY, int startX, int y,
                                  int targetInterface) {
        int centerX = this.leftPos + this.theme.imageWidth() / 2;
        boolean hover = mouseX >= startX && mouseX <= startX + 117 && mouseY >= y && mouseY <= y + 30;
        guiGraphics.blit(this.theme.menuButtons(), startX, y, 0, hover ? 32 : 0, 117, 30, 256, 256);

        if (this.interfaceType == targetInterface) {
            String labelKey = switch (targetInterface) {
                case SocialInterfaceType.LETTER -> "gui.mcasocial.menu.carta_desbloqueada";
                case SocialInterfaceType.CRYSTAL -> "gui.mcasocial.menu.crystal";
                default -> "gui.mcasocial.menu.celular";
            };
            guiGraphics.drawCenteredString(this.font, Component.translatable(labelKey),
                    centerX, y + 10, hover ? 0xFFFF55 : 0xFFFFFF);
            return;
        }

        drawTargetInterfaceIcon(guiGraphics, startX, y, targetInterface);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.menu.convert"),
                centerX, y + 3, hover ? 0xFFFF55 : 0x999999);
        drawCost(guiGraphics, startX, y + 12, targetInterface);
    }

    private void drawTargetInterfaceIcon(GuiGraphics guiGraphics, int startX, int y, int targetInterface) {
        ItemStack stack = switch (targetInterface) {
            case SocialInterfaceType.LETTER -> new ItemStack(ModItems.CARTA.get());
            case SocialInterfaceType.CRYSTAL -> new ItemStack(ModItems.CRYSTAL.get());
            default -> new ItemStack(ModItems.SMARTPHONE.get());
        };
        int iconX = startX + 8;
        int iconY = y + 7;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(iconX, iconY, 0);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, TARGET_ICON_ALPHA);
        guiGraphics.renderItem(stack, 0, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.pose().popPose();
    }

    private void drawCost(GuiGraphics guiGraphics, int startX, int itemY, int targetInterface) {
        if (targetInterface == SocialInterfaceType.PHONE) {
            drawItemWithCount(guiGraphics, Items.IRON_INGOT, startX + 31, itemY, "x7");
            drawItemWithCount(guiGraphics, Items.REDSTONE, startX + 62, itemY, "x1");
            drawItemWithCount(guiGraphics, Items.GLASS_PANE, startX + 90, itemY, "x1");
        } else if (targetInterface == SocialInterfaceType.LETTER) {
            drawItemWithCount(guiGraphics, Items.PAPER, startX + 43, itemY, "x4");
            drawItemWithCount(guiGraphics, Items.LEATHER, startX + 78, itemY, "x1");
        } else {
            drawItemWithCount(guiGraphics, Items.DIAMOND, startX + 31, itemY, "x1");
            drawItemWithCount(guiGraphics, Items.AMETHYST_SHARD, startX + 62, itemY, "x3");
            drawItemWithCount(guiGraphics, Items.ENDER_PEARL, startX + 90, itemY, "x1");
        }
    }

    private void drawItemWithCount(GuiGraphics guiGraphics, net.minecraft.world.item.Item item,
                                   int x, int y, String count) {
        guiGraphics.renderItem(new ItemStack(item), x, y);
        guiGraphics.drawString(this.font, count, x + 16, y + 4, 0xCCCCCC, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startX = this.leftPos + (this.theme.imageWidth() - 117) / 2;
            int startY = this.topPos + 65;
            for (int target = SocialInterfaceType.PHONE; target <= SocialInterfaceType.CRYSTAL; target++) {
                int y = startY + target * 35;
                if (target != this.interfaceType && mouseX >= startX && mouseX <= startX + 117
                        && mouseY >= y && mouseY <= y + 30) {
                    ModMessages.sendToServer(new MorphInterfacePacket(target));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void openAfterMorph(int targetInterface) {
        Minecraft.getInstance().setScreen(new SocialHomeScreen(targetInterface));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
