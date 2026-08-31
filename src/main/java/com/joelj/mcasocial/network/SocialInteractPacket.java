package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialActionService;
import com.joelj.mcasocial.social.SocialCapability;
import com.joelj.mcasocial.social.SocialContactHelper;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialDiscoveryService;
import com.joelj.mcasocial.social.SocialContact;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SocialInteractPacket {
    private static final int MAX_ACTION_LENGTH = 32;

    private final int entityId;
    private final String accion;
    private final boolean isFamily;

    public SocialInteractPacket(int entityId, String accion, boolean isFamily) {
        this.entityId = entityId;
        this.accion = accion;
        this.isFamily = isFamily;
    }

    public SocialInteractPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.accion = buf.readUtf(MAX_ACTION_LENGTH);
        this.isFamily = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeUtf(this.accion);
        buf.writeBoolean(this.isFamily);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !SocialActionService.isValidAction(this.accion)) return;

            Entity entity = player.level().getEntity(this.entityId);
            if (entity == null) return;

            entity.getCapability(SocialCapability.SOCIAL_CAPABILITY).ifPresent(data -> {
                SocialActionService.RoleInfo role = SocialActionService.inspectRole(entity, player, this.isFamily);
                if (SocialDiscoveryService.isDiscoveryAction(this.accion)) {
                    SocialContactHelper.remember(player, entity, data, role.isBaby(), role.isMinor(),
                            role.ageStage(), role.isFamily(), role.isSpouse());
                    player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
                        SocialContact contact = contacts.getContact(entity.getUUID());
                        if (contact == null) return;
                        SocialDiscoveryService.perform(player, contact, role, SocialActionService.InteractionChannel.FACE_TO_FACE);
                        data.setAffinity(contact.getAffinity());
                        contact.updateSocialValues(data);
                        contacts.addOrUpdate(contact);
                    });
                } else {
                    SocialActionService.perform(player, entity, data, this.accion, role, SocialActionService.InteractionChannel.FACE_TO_FACE);
                    SocialContactHelper.remember(player, entity, data, role.isBaby(), role.isMinor(),
                            role.ageStage(), role.isFamily(), role.isSpouse());
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
