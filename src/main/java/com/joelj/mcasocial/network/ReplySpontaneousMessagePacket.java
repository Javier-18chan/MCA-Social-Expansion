package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.ContactRosterService;
import com.joelj.mcasocial.social.DeceasedVillagers;
import com.joelj.mcasocial.social.ISocialData;
import com.joelj.mcasocial.social.SocialCapability;
import com.joelj.mcasocial.social.SocialContact;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.SocialInterfaceSounds;
import com.joelj.mcasocial.social.SpontaneousSocialMessage;
import com.joelj.mcasocial.social.SpontaneousMessageService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ReplySpontaneousMessagePacket {
    private final UUID messageId;
    private final int choice;
    private final int interfaceType;

    public ReplySpontaneousMessagePacket(UUID messageId, int choice, int interfaceType) {
        this.messageId = messageId;
        this.choice = choice;
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public ReplySpontaneousMessagePacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readInt(), buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(messageId);
        buf.writeInt(choice);
        buf.writeInt(interfaceType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || choice < 0 || choice > 2 || !RequestContactsPacket.hasSocialDevice(player)) return;
            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
                SpontaneousSocialMessage message = contacts.getSpontaneousMessages().stream()
                        .filter(entry -> entry.id().equals(messageId) && entry.delivered() && !entry.replied())
                        .findFirst().orElse(null);
                if (message == null) return;
                SocialContact contact = contacts.getContact(message.contactUuid());
                if (contact == null || DeceasedVillagers.get(player.server).contains(message.contactUuid())) return;
                String mood = choice == 0 ? "warm" : "brief";
                String responseKey = choice == 2 ? "" : "chat.mcasocial.spontaneous.response."
                        + SpontaneousMessageService.role(contact) + "." + mood + "."
                        + (Math.floorMod(message.id().hashCode(), 2) + 1);
                int reactionTicks = 20 + Math.floorMod(message.id().hashCode(), 41);
                long responseTime = player.server.overworld().getGameTime()
                        + contacts.getDeliveryDelayMode().delayTicks(interfaceType) + reactionTicks;
                if (contacts.replyToSpontaneousMessage(messageId, responseKey, responseTime) == null) return;
                if (choice == 0) {
                    Entity loaded = ContactRosterService.findLoaded(player.server, contact.getUuid());
                    ISocialData data = loaded == null ? null
                            : loaded.getCapability(SocialCapability.SOCIAL_CAPABILITY).orElse(null);
                    if (data != null) {
                        int affinity = data.getAffinity() + 1;
                        data.setAffinity(affinity);
                        contact.setAffinity(affinity);
                    } else {
                        contact.setAffinity(contact.getAffinity() + 1);
                        contact.setPendingSync(true);
                    }
                }
                if (choice != 2) {
                    SocialInterfaceSounds.playSent(player, interfaceType);
                    String dimension = message.dialogueKey().contains(".nether.") ? "nether"
                            : message.dialogueKey().contains(".end.") ? "end" : "normal";
                    String playerKey = "chat.mcasocial.spontaneous.player_reply." + dimension + "." + mood;
                    player.sendSystemMessage(Component.literal("[").append(player.getDisplayName())
                            .append(Component.literal("] ")).append(Component.translatable(playerKey)));
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
