package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.PendingSocialAction;
import com.joelj.mcasocial.social.SocialContact;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialInterfaceType;
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
                List<OpenInboxScreenPacket.Entry> entries = new ArrayList<>();
                long gameTime = player.level().getGameTime();
                for (PendingSocialAction action : contacts.getPendingActions()) {
                    SocialContact contact = contacts.getContact(action.contactUuid());
                    String name = contact == null ? "" : contact.getName();
                    int remainingSeconds = Math.max(0,
                            (int) Math.ceil((action.deliveryGameTime() - gameTime) / 20.0));
                    entries.add(new OpenInboxScreenPacket.Entry(
                            name, action.action(), action.interfaceType(), remainingSeconds));
                }
                ModMessages.sendToPlayer(new OpenInboxScreenPacket(
                        entries, interfaceType, contacts.getDeliveryDelayMode()), player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
