package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.GiftInteractionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GiftInteractPacket {
    private static final int MAX_ITEM_ID_LENGTH = 128;

    private final int entityId;
    private final String itemName;
    private final boolean family;

    public GiftInteractPacket(int entityId, String itemName, boolean family) {
        this.entityId = entityId;
        this.itemName = itemName;
        this.family = family;
    }

    public GiftInteractPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.itemName = buf.readUtf(MAX_ITEM_ID_LENGTH);
        this.family = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(itemName);
        buf.writeBoolean(family);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) GiftInteractionService.perform(player, entityId, itemName, family);
        });
        context.setPacketHandled(true);
        return true;
    }
}
