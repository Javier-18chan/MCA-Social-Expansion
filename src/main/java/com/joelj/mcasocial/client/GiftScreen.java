package com.joelj.mcasocial.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.joelj.mcasocial.network.GiftInteractPacket;
import com.joelj.mcasocial.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GiftScreen extends Screen {
    private final int entityId;
    private final String nombre;
    private final boolean isFamily;
    private final List<ItemStack> posiblesRegalos = new ArrayList<>();
    private final Set<Item> itemsEnInventario = new HashSet<>();

    public GiftScreen(int entityId, String nombre, boolean isFamily) {
        super(Component.translatable("gui.mcasocial.gift_title", nombre));
        this.entityId = entityId;
        this.nombre = nombre;
        this.isFamily = isFamily;
        inicializarListaRegalos();
    }

    private void inicializarListaRegalos() {
        posiblesRegalos.add(new ItemStack(Items.DIAMOND));
        posiblesRegalos.add(new ItemStack(Items.EMERALD));
        posiblesRegalos.add(new ItemStack(Items.GOLD_INGOT));
        posiblesRegalos.add(new ItemStack(Items.IRON_INGOT));
        posiblesRegalos.add(new ItemStack(Items.COPPER_INGOT));
        posiblesRegalos.add(new ItemStack(Items.NETHERITE_INGOT));
        posiblesRegalos.add(new ItemStack(Items.LAPIS_LAZULI));
        posiblesRegalos.add(new ItemStack(Items.AMETHYST_SHARD));
        posiblesRegalos.add(new ItemStack(Items.QUARTZ));

        posiblesRegalos.add(new ItemStack(Items.POPPY));
        posiblesRegalos.add(new ItemStack(Items.DANDELION));
        posiblesRegalos.add(new ItemStack(Items.BLUE_ORCHID));
        posiblesRegalos.add(new ItemStack(Items.ALLIUM));
        posiblesRegalos.add(new ItemStack(Items.AZURE_BLUET));
        posiblesRegalos.add(new ItemStack(Items.RED_TULIP));
        posiblesRegalos.add(new ItemStack(Items.ORANGE_TULIP));
        posiblesRegalos.add(new ItemStack(Items.WHITE_TULIP));
        posiblesRegalos.add(new ItemStack(Items.PINK_TULIP));

        posiblesRegalos.add(new ItemStack(Items.OXEYE_DAISY));
        posiblesRegalos.add(new ItemStack(Items.CORNFLOWER));
        posiblesRegalos.add(new ItemStack(Items.LILY_OF_THE_VALLEY));
        posiblesRegalos.add(new ItemStack(Items.APPLE));
        posiblesRegalos.add(new ItemStack(Items.GOLDEN_APPLE));
        posiblesRegalos.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        posiblesRegalos.add(new ItemStack(Items.CAKE));
        posiblesRegalos.add(new ItemStack(Items.CARROT));
        posiblesRegalos.add(new ItemStack(Items.PUMPKIN_PIE));

        posiblesRegalos.add(new ItemStack(Items.COOKIE));
        posiblesRegalos.add(new ItemStack(Items.SWEET_BERRIES));
        posiblesRegalos.add(new ItemStack(Items.GLOW_BERRIES));
        posiblesRegalos.add(new ItemStack(Items.HONEYCOMB));
        posiblesRegalos.add(new ItemStack(Items.BREAD));
        posiblesRegalos.add(new ItemStack(Items.BAKED_POTATO));
        posiblesRegalos.add(new ItemStack(Items.ROTTEN_FLESH));
        posiblesRegalos.add(new ItemStack(Items.SPIDER_EYE));
        posiblesRegalos.add(new ItemStack(Items.BONE));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

        guiGraphics.fill(0, 0, this.width, this.height, 0x90000000);
        refrescarItemsEnInventario();

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int itemsPerRow = 9;
        int rows = 4;

        int slotSize = 26;
        int gap = 4;
        int step = slotSize + gap;

        int gridWidth = (itemsPerRow * slotSize) + ((itemsPerRow - 1) * gap);
        int gridHeight = (rows * slotSize) + ((rows - 1) * gap);

        int startX = centerX - (gridWidth / 2);
        int startY = centerY - (gridHeight / 2) + 15;

        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.mcasocial.gift_title", this.nombre), centerX, startY - 20, 0xFFFF55);

        for (int i = 0; i < posiblesRegalos.size(); i++) {
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + (col * step);
            int y = startY + (row * step);

            guiGraphics.fill(x, y, x + slotSize, y + slotSize, 0xFF555555);
            guiGraphics.fill(x + 1, y + 1, x + slotSize - 1, y + slotSize - 1, 0xCC000000);

            ItemStack stack = posiblesRegalos.get(i);
            boolean tieneItem = verificarSiTieneItem(stack);

            int itemX = x + 5;
            int itemY = y + 5;

            if (tieneItem) {
                guiGraphics.renderItem(stack, itemX, itemY);
                if (mouseX >= x && mouseX <= x + slotSize && mouseY >= y && mouseY <= y + slotSize) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            } else {
                RenderSystem.setShaderColor(0.2f, 0.2f, 0.2f, 1.0f);
                guiGraphics.renderItem(stack, itemX, itemY);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private boolean verificarSiTieneItem(ItemStack stack) {
        return itemsEnInventario.contains(stack.getItem());
    }

    private void refrescarItemsEnInventario() {
        itemsEnInventario.clear();
        if (Minecraft.getInstance().player == null) return;

        var inventory = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                itemsEnInventario.add(stack.getItem());
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        refrescarItemsEnInventario();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int itemsPerRow = 9;
        int rows = 4;
        int slotSize = 26;
        int gap = 4;
        int step = slotSize + gap;

        int gridWidth = (itemsPerRow * slotSize) + ((itemsPerRow - 1) * gap);
        int gridHeight = (rows * slotSize) + ((rows - 1) * gap);

        int startX = centerX - (gridWidth / 2);
        int startY = centerY - (gridHeight / 2) + 15;

        for (int i = 0; i < posiblesRegalos.size(); i++) {
            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + (col * step);
            int y = startY + (row * step);

            if (mouseX >= x && mouseX <= x + slotSize && mouseY >= y && mouseY <= y + slotSize) {
                ItemStack stack = posiblesRegalos.get(i);
                if (verificarSiTieneItem(stack)) {
                    String registryName = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    ModMessages.sendToServer(new GiftInteractPacket(this.entityId, registryName, this.isFamily));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
