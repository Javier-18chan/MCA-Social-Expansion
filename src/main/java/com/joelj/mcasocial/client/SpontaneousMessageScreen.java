package com.joelj.mcasocial.client;

import com.joelj.mcasocial.network.ModMessages;
import com.joelj.mcasocial.network.OpenInboxScreenPacket;
import com.joelj.mcasocial.network.ReplySpontaneousMessagePacket;
import com.joelj.mcasocial.network.RequestInboxPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SpontaneousMessageScreen extends Screen {
    private final OpenInboxScreenPacket.IncomingEntry entry;
    private final SocialInterfaceTheme theme;
    private final int interfaceType;
    private int leftPos;
    private int topPos;
    private boolean waitingForServer;

    public SpontaneousMessageScreen(OpenInboxScreenPacket.IncomingEntry entry, int interfaceType) {
        super(Component.translatable("gui.mcasocial.spontaneous.title"));
        this.entry = entry;
        this.theme = SocialInterfaceTheme.fromId(interfaceType);
        this.interfaceType = this.theme.id();
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - theme.imageWidth()) / 2;
        topPos = (height - theme.imageHeight()) / 2;
        int startX = leftPos + (theme.imageWidth() - theme.menuButtonWidth()) / 2;
        if (entry.remainingSeconds() == 0 && !entry.replied()) {
            int firstY = topPos + (theme == SocialInterfaceTheme.PHONE ? 113
                    : theme == SocialInterfaceTheme.MAGIC_MIRROR ? 176 : 143);
            addReply(startX, firstY, 0, "gui.mcasocial.spontaneous.reply.warm");
            addReply(startX, firstY + 33, 1, "gui.mcasocial.spontaneous.reply.brief");
            addReply(startX, firstY + 66, 2, "gui.mcasocial.spontaneous.reply.skip");
        } else {
            addRenderableWidget(new SocialMenuButton(startX,
                    topPos + (theme == SocialInterfaceTheme.PHONE ? 178
                            : theme == SocialInterfaceTheme.MAGIC_MIRROR ? 214 : 190),
                    theme.menuButtonWidth(), 30,
                    theme.menuButtons(), Component.translatable("gui.mcasocial.back"), button -> onClose()));
        }
    }

    private void addReply(int x, int y, int choice, String labelKey) {
        addRenderableWidget(new SocialMenuButton(x, y, theme.menuButtonWidth(), 30, theme.menuButtons(),
                Component.translatable(labelKey), button -> {
                    if (waitingForServer) return;
                    waitingForServer = true;
                    ModMessages.sendToServer(new ReplySpontaneousMessagePacket(entry.id(), choice, interfaceType));
                    minecraft.setScreen(null);
                }));
    }

    @Override public void renderBackground(GuiGraphics graphics) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, width, height, 0x90000000);
        theme.drawBackground(graphics, leftPos, topPos);
        int centerX = leftPos + theme.imageWidth() / 2;
        graphics.drawCenteredString(font, Component.translatable("gui.mcasocial.spontaneous.title"),
                centerX, topPos + 26 + theme.panelOffset(), theme.titleColor());
        graphics.drawCenteredString(font, entry.contactName(), centerX,
                topPos + 43 + theme.panelOffset(), theme.accentColor());
        Component body = entry.remainingSeconds() > 0
                ? Component.translatable("gui.mcasocial.inbox.remaining", entry.remainingSeconds())
                : Component.translatable(entry.dialogueKey());
        if (entry.remainingSeconds() == 0 && !entry.greetingKey().isEmpty()) {
            body = Component.translatable(entry.greetingKey()).append(Component.literal(" ")).append(body);
        }
        int textWidth = theme == SocialInterfaceTheme.MAGIC_MIRROR
                ? 145 : theme.imageWidth() - 28;
        var lines = font.split(body, textWidth);
        int bodyHeight = theme == SocialInterfaceTheme.PHONE ? 43 : 60;
        float scale = Math.min(1.0F, bodyHeight / (float) Math.max(11, lines.size() * 11));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, topPos + 64 + theme.panelOffset(), 0);
        graphics.pose().scale(scale, scale, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(font, lines.get(i), 0, i * 11, theme.textColor());
        }
        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        ModMessages.sendToServer(new RequestInboxPacket(interfaceType));
    }

    @Override public boolean isPauseScreen() { return false; }
}
