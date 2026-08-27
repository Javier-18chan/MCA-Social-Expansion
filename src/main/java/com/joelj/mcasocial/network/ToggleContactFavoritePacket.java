package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialContact;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ToggleContactFavoritePacket {
    private final UUID contactUuid;
    private final boolean favorite;

    public ToggleContactFavoritePacket(UUID contactUuid, boolean favorite) {
        this.contactUuid = contactUuid;
        this.favorite = favorite;
    }

    public ToggleContactFavoritePacket(FriendlyByteBuf buf) {
        this.contactUuid = buf.readUUID();
        this.favorite = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.contactUuid);
        buf.writeBoolean(this.favorite);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !RequestContactsPacket.hasSocialDevice(player)) return;

            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
                SocialContact contact = contacts.getContact(this.contactUuid);
                if (contact == null) return;
                contact.setFavorite(this.favorite);
                contacts.addOrUpdate(contact);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
