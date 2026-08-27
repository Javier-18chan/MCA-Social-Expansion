package com.joelj.mcasocial.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.UUID;
import com.joelj.mcasocial.social.SocialAgeStage;
import com.joelj.mcasocial.social.SocialInterfaceType;
import java.util.function.Supplier;

public class OpenSocialScreenPacket {
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_PERSONALITY_LENGTH = 64;

    public final int entityId;
    public final String nombre;
    public final String personalidad;
    public final int afinidad;
    public final int romance;
    public final int trust;
    public final boolean isBaby;
    public final boolean isMinor;
    public final boolean isFamily;
    public final boolean isSpouse;
    public final SocialAgeStage ageStage;
    public final int interfaceType;
    public final boolean remoteContact;
    public final UUID contactUuid;

    public OpenSocialScreenPacket(int entityId, String nombre, String personalidad, int afinidad, int romance, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse, int interfaceType) {
        this(entityId, nombre, personalidad, afinidad, romance, 8, isBaby, isMinor,
                isFamily, isSpouse, SocialAgeStage.fromFlags(isBaby, isMinor), interfaceType, null);
    }

    public OpenSocialScreenPacket(int entityId, String nombre, String personalidad, int afinidad, int romance, boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse, int interfaceType, UUID contactUuid) {
        this(entityId, nombre, personalidad, afinidad, romance, 8, isBaby, isMinor,
                isFamily, isSpouse, SocialAgeStage.fromFlags(isBaby, isMinor), interfaceType, contactUuid);
    }

    public OpenSocialScreenPacket(int entityId, String nombre, String personalidad, int afinidad, int romance, int trust,
                                  boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse,
                                  SocialAgeStage ageStage, int interfaceType, UUID contactUuid) {
        this.entityId = entityId;
        this.nombre = nombre;
        this.personalidad = personalidad;
        this.afinidad = afinidad;
        this.romance = romance;
        this.trust = trust;
        this.isBaby = isBaby;
        this.isMinor = isMinor;
        this.isFamily = isFamily;
        this.isSpouse = isSpouse;
        this.ageStage = ageStage == null ? SocialAgeStage.fromFlags(isBaby, isMinor) : ageStage;
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
        this.remoteContact = contactUuid != null;
        this.contactUuid = contactUuid;
    }

    public OpenSocialScreenPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.nombre = buf.readUtf(MAX_NAME_LENGTH);
        this.personalidad = buf.readUtf(MAX_PERSONALITY_LENGTH);
        this.afinidad = buf.readInt();
        this.romance = buf.readInt();
        this.trust = buf.readInt();
        this.isBaby = buf.readBoolean();
        this.isMinor = buf.readBoolean();
        this.isFamily = buf.readBoolean();
        this.isSpouse = buf.readBoolean();
        this.ageStage = SocialAgeStage.fromOrdinal(buf.readInt());
        int interfaceId = buf.readInt();
        this.interfaceType = SocialInterfaceType.normalize(interfaceId);
        this.remoteContact = buf.readBoolean();
        this.contactUuid = this.remoteContact ? buf.readUUID() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.nombre);
        buf.writeUtf(this.personalidad);
        buf.writeInt(this.afinidad);
        buf.writeInt(this.romance);
        buf.writeInt(this.trust);
        buf.writeBoolean(this.isBaby);
        buf.writeBoolean(this.isMinor);
        buf.writeBoolean(this.isFamily);
        buf.writeBoolean(this.isSpouse);
        buf.writeInt(this.ageStage.ordinal());
        buf.writeInt(this.interfaceType);
        buf.writeBoolean(this.remoteContact);
        if (this.remoteContact) {
            buf.writeUUID(this.contactUuid);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.joelj.mcasocial.client.ClientScreenOpener.open(this));
        });
        context.setPacketHandled(true);
        return true;
    }
}
