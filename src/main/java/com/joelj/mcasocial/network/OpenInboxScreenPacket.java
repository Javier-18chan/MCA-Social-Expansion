package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.DeliveryDelayMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenInboxScreenPacket {
    private static final int MAX_ENTRIES = 32;
    private static final int MAX_TEXT_LENGTH = 128;

    public final List<Entry> entries;
    public final List<IncomingEntry> incomingEntries;
    public final int interfaceType;
    public final DeliveryDelayMode delayMode;

    public OpenInboxScreenPacket(List<Entry> entries, List<IncomingEntry> incomingEntries,
                                 int interfaceType, DeliveryDelayMode delayMode) {
        this.entries = entries.stream().limit(MAX_ENTRIES).toList();
        this.incomingEntries = incomingEntries.stream().limit(MAX_ENTRIES).toList();
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
        this.delayMode = delayMode == null ? DeliveryDelayMode.SHORT : delayMode;
    }

    public OpenInboxScreenPacket(FriendlyByteBuf buf) {
        int size = Math.min(buf.readInt(), MAX_ENTRIES);
        List<Entry> decoded = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            decoded.add(new Entry(
                    buf.readUtf(MAX_TEXT_LENGTH),
                    buf.readUtf(MAX_TEXT_LENGTH),
                    SocialInterfaceType.normalize(buf.readInt()),
                    Math.max(0, buf.readInt())
            ));
        }
        this.entries = decoded;
        int incomingSize = Math.min(buf.readInt(), MAX_ENTRIES);
        List<IncomingEntry> incoming = new ArrayList<>();
        for (int i = 0; i < incomingSize; i++) {
            incoming.add(new IncomingEntry(buf.readUUID(), buf.readUUID(), buf.readUtf(MAX_TEXT_LENGTH),
                    buf.readUtf(MAX_TEXT_LENGTH), buf.readUtf(MAX_TEXT_LENGTH),
                    SocialInterfaceType.normalize(buf.readInt()),
                    Math.max(0, buf.readInt()), buf.readBoolean()));
        }
        this.incomingEntries = incoming;
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
        this.delayMode = DeliveryDelayMode.fromOrdinal(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUtf(entry.contactName());
            buf.writeUtf(entry.action());
            buf.writeInt(entry.interfaceType());
            buf.writeInt(entry.remainingSeconds());
        }
        buf.writeInt(incomingEntries.size());
        for (IncomingEntry entry : incomingEntries) {
            buf.writeUUID(entry.id());
            buf.writeUUID(entry.contactUuid());
            buf.writeUtf(entry.contactName());
            buf.writeUtf(entry.dialogueKey());
            buf.writeUtf(entry.greetingKey());
            buf.writeInt(entry.interfaceType());
            buf.writeInt(entry.remainingSeconds());
            buf.writeBoolean(entry.replied());
        }
        buf.writeInt(interfaceType);
        buf.writeInt(delayMode.ordinal());
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.joelj.mcasocial.client.ClientScreenOpener.openInbox(this)));
        context.setPacketHandled(true);
        return true;
    }

    public record Entry(String contactName, String action, int interfaceType, int remainingSeconds) {}
    public record IncomingEntry(UUID id, UUID contactUuid, String contactName,
                                String dialogueKey, String greetingKey, int interfaceType,
                                int remainingSeconds, boolean replied) {}
}
