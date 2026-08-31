package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.DeliveryDelayMode;
import com.joelj.mcasocial.social.SocialContactsCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateSocialSettingsPacket {
    private final DeliveryDelayMode delayMode;

    public UpdateSocialSettingsPacket(DeliveryDelayMode delayMode) {
        this.delayMode = delayMode == null ? DeliveryDelayMode.SHORT : delayMode;
    }

    public UpdateSocialSettingsPacket(FriendlyByteBuf buf) {
        this.delayMode = DeliveryDelayMode.fromOrdinal(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(delayMode.ordinal());
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS)
                    .ifPresent(contacts -> contacts.setDeliveryDelayMode(delayMode));
        });
        context.setPacketHandled(true);
        return true;
    }
}
