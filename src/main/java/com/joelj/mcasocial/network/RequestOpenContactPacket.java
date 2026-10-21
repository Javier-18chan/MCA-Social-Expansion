package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialContact;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.SocialActionService;
import com.joelj.mcasocial.social.SocialCapability;
import com.joelj.mcasocial.social.McaRomanceSync;
import com.joelj.mcasocial.social.GiftPreferenceProfile;
import com.joelj.mcasocial.social.ContactRosterService;
import com.joelj.mcasocial.social.DeceasedVillagers;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestOpenContactPacket {
    private final UUID contactUuid;
    private final int interfaceType;

    public RequestOpenContactPacket(UUID contactUuid, int interfaceType) {
        this.contactUuid = contactUuid;
        this.interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public RequestOpenContactPacket(FriendlyByteBuf buf) {
        this.contactUuid = buf.readUUID();
        this.interfaceType = SocialInterfaceType.normalize(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.contactUuid);
        buf.writeInt(this.interfaceType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !RequestContactsPacket.hasSocialDevice(player)) return;

            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
                if (DeceasedVillagers.get(player.server).contains(this.contactUuid)) {
                    contacts.removeContact(this.contactUuid);
                    player.displayClientMessage(Component.translatable("chat.mcasocial.contacts.deceased"), true);
                    ModMessages.sendToPlayer(new OpenContactsScreenPacket(contacts.getContacts(), this.interfaceType), player);
                    return;
                }
                SocialContact contact = contacts.getContact(this.contactUuid);
                if (contact == null) return;

                Entity loadedEntity = ContactRosterService.findLoaded(player.server, this.contactUuid);
                if (loadedEntity != null) {
                    loadedEntity.getCapability(SocialCapability.SOCIAL_CAPABILITY).ifPresent(data -> {
                        if (loadedEntity instanceof VillagerEntityMCA villager) {
                            if (contact.isPendingSync()) {
                                contact.pushSocialValuesTo(data);
                                contact.setPendingSync(false);
                                McaRomanceSync.pushCached(villager, player, data);
                            } else {
                                McaRomanceSync.reconcile(villager, player, data, contact);
                            }
                        }
                        SocialActionService.RoleInfo role = SocialActionService.inspectRole(loadedEntity, player, contact.isFamily());
                        contact.updateIdentity(loadedEntity.getName().getString(), data.getPersonality(),
                                role.isBaby(), role.isMinor(), role.ageStage(), role.isFamily(), role.isSpouse(),
                                player.level().getGameTime());
                        contact.updateSocialValues(data);
                        contacts.addOrUpdate(contact);
                    });
                }

                GiftPreferenceProfile.ensure(contact);

                ModMessages.sendToPlayer(new OpenSocialScreenPacket(
                        -1,
                        contact.getName(),
                        contact.getPersonality(),
                        contact.getAffinity(),
                        contact.getRomance(),
                        contact.getTrust(),
                        contact.isBaby(),
                        contact.isMinor(),
                        contact.isFamily(),
                        contact.isSpouse(),
                        contact.getAgeStage(),
                        this.interfaceType,
                        contact.getUuid(),
                        contact.getKnowledgeLevel(),
                        contact.getFavoriteGiftCategory(),
                        contact.getLikedGiftCategory(),
                        contact.getDislikedGiftCategory()
                ), player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
