package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialContact;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.joelj.mcasocial.social.SocialInterfaceType;
import java.util.function.Supplier;

public class OpenContactsScreenPacket {
    private static final int MAX_CONTACTS = 128;
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_PERSONALITY_LENGTH = 64;

    public final List<SocialContact> contacts;
    public final int interfaceType;

    public OpenContactsScreenPacket(Collection<SocialContact> contacts, int interfaceType) {
        this.contacts = contacts.stream().limit(MAX_CONTACTS).map(SocialContact::copy).toList();
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public OpenContactsScreenPacket(FriendlyByteBuf buf) {
        int size = Math.min(buf.readInt(), MAX_CONTACTS);
        List<SocialContact> loadedContacts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(MAX_NAME_LENGTH);
            String personality = buf.readUtf(MAX_PERSONALITY_LENGTH);
            SocialContact contact = new SocialContact(uuid, name, personality);
            contact.setAffinity(buf.readInt());
            contact.setRomance(buf.readInt());
            contact.setTrust(buf.readInt());
            boolean baby = buf.readBoolean();
            boolean minor = buf.readBoolean();
            com.joelj.mcasocial.social.SocialAgeStage ageStage =
                    com.joelj.mcasocial.social.SocialAgeStage.fromOrdinal(buf.readInt());
            boolean family = buf.readBoolean();
            boolean spouse = buf.readBoolean();
            contact.updateIdentity(name, personality, baby, minor, ageStage, family, spouse, buf.readLong());
            contact.setFavorite(buf.readBoolean());
            loadedContacts.add(contact);
        }
        this.contacts = loadedContacts;
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        int count = Math.min(this.contacts.size(), MAX_CONTACTS);
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            SocialContact contact = this.contacts.get(i);
            buf.writeUUID(contact.getUuid());
            buf.writeUtf(contact.getName() == null ? "" : contact.getName());
            buf.writeUtf(contact.getPersonality() == null ? "" : contact.getPersonality());
            buf.writeInt(contact.getAffinity());
            buf.writeInt(contact.getRomance());
            buf.writeInt(contact.getTrust());
            buf.writeBoolean(contact.isBaby());
            buf.writeBoolean(contact.isMinor());
            buf.writeInt(contact.getAgeStage().ordinal());
            buf.writeBoolean(contact.isFamily());
            buf.writeBoolean(contact.isSpouse());
            buf.writeLong(contact.getLastSeenGameTime());
            buf.writeBoolean(contact.isFavorite());
        }
        buf.writeInt(this.interfaceType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.joelj.mcasocial.client.ClientScreenOpener.openContacts(this))
        );
        context.setPacketHandled(true);
        return true;
    }
}
