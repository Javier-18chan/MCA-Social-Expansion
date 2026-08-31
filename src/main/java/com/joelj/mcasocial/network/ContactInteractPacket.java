package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialActionService;
import com.joelj.mcasocial.social.DelayedSocialActionService;
import com.joelj.mcasocial.social.SocialInterfaceType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;

public class ContactInteractPacket {
    private static final int MAX_ACTION_LENGTH = 32;

    private final UUID contactUuid;
    private final String action;
    private final int interfaceType;

    public ContactInteractPacket(UUID contactUuid, String action, int interfaceType) {
        this.contactUuid = contactUuid;
        this.action = action;
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public ContactInteractPacket(FriendlyByteBuf buf) {
        this.contactUuid = buf.readUUID();
        this.action = buf.readUtf(MAX_ACTION_LENGTH);
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.contactUuid);
        buf.writeUtf(this.action);
        buf.writeInt(this.interfaceType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !RequestContactsPacket.hasSocialDevice(player) || !SocialActionService.isValidAction(this.action)) return;

            String dedupKey = player.getUUID() + ":" + this.contactUuid + ":" + this.action + ":" + this.interfaceType;
            long now = System.currentTimeMillis();
            Long lastTime = DEDUP_CACHE.get(dedupKey);
            if (lastTime != null && now - lastTime < 500) {
                return;
            }
            DEDUP_CACHE.put(dedupKey, now);

            DelayedSocialActionService.submit(player, this.contactUuid, this.action, this.interfaceType);
        });
        context.setPacketHandled(true);
        return true;
    }

    private static final java.util.Map<String, Long> DEDUP_CACHE = new ConcurrentHashMap<>();
}
