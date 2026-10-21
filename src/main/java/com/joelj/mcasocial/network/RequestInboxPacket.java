package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.PendingSocialAction;
import com.joelj.mcasocial.social.SocialContact;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.ISocialContacts;
import com.joelj.mcasocial.social.SpontaneousSocialMessage;
import com.joelj.mcasocial.social.SpontaneousMessageService;
import com.joelj.mcasocial.social.ContactRosterService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestInboxPacket {
    private final int interfaceType;

    public RequestInboxPacket(int interfaceType) {
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public RequestInboxPacket(FriendlyByteBuf buf) {
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(interfaceType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !RequestContactsPacket.hasSocialDevice(player)) return;

            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
                ContactRosterService.refresh(player, contacts);
                sendInbox(player, contacts, interfaceType);
            });
        });
        context.setPacketHandled(true);
        return true;
    }

    static void sendInbox(ServerPlayer player, ISocialContacts contacts, int interfaceType) {
        List<OpenInboxScreenPacket.Entry> entries = new ArrayList<>();
        List<OpenInboxScreenPacket.IncomingEntry> incoming = new ArrayList<>();
        long gameTime = player.level().getGameTime();
        for (PendingSocialAction action : contacts.getPendingActions()) {
            SocialContact contact = contacts.getContact(action.contactUuid());
            String name = contact == null ? "" : contact.getName();
            int remainingSeconds = Math.max(0,
                    (int) Math.ceil((action.deliveryGameTime() - gameTime) / 20.0));
            entries.add(new OpenInboxScreenPacket.Entry(
                    name, action.action(), action.interfaceType(), remainingSeconds));
        }
        long incomingGameTime = player.server.overworld().getGameTime();
        List<SpontaneousSocialMessage> messages = contacts.getSpontaneousMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            SpontaneousSocialMessage message = messages.get(i);
            SocialContact contact = contacts.getContact(message.contactUuid());
            if (contact == null) continue;
            int remaining = message.delivered() ? 0 : Math.max(1,
                    (int) Math.ceil((message.deliveryGameTime() - incomingGameTime) / 20.0));
            incoming.add(new OpenInboxScreenPacket.IncomingEntry(message.id(), message.contactUuid(), contact.getName(),
                    message.dialogueKey(), SpontaneousMessageService.greetingKey(player, contact, message.id()),
                    message.interfaceType(), remaining, message.replied()));
        }
        ModMessages.sendToPlayer(new OpenInboxScreenPacket(
                entries, incoming, interfaceType, contacts.getDeliveryDelayMode()), player);
    }
}
