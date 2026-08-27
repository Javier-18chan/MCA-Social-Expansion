package com.joelj.mcasocial.network;

import com.joelj.mcasocial.social.SocialActionService;
import com.joelj.mcasocial.social.SocialCapability;
import com.joelj.mcasocial.social.SocialContact;
import com.joelj.mcasocial.social.SocialContactsCapability;
import com.joelj.mcasocial.social.SocialInterfaceType;
import com.joelj.mcasocial.social.McaRomanceSync;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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

            player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
                SocialContact contact = contacts.getContact(this.contactUuid);
                if (contact == null) return;

                Entity loadedEntity = ((ServerLevel) player.level()).getEntity(this.contactUuid);
                if (loadedEntity != null) {
                    loadedEntity.getCapability(SocialCapability.SOCIAL_CAPABILITY).ifPresent(data -> {
                        if (contact.isPendingSync()) {
                            contact.pushSocialValuesTo(data);
                            contact.setPendingSync(false);
                            if (loadedEntity instanceof VillagerEntityMCA villager) {
                                McaRomanceSync.pushCached(villager, player, data);
                            }
                        } else if (loadedEntity instanceof VillagerEntityMCA villager) {
                            McaRomanceSync.reconcile(villager, player, data, contact);
                        }

                        SocialActionService.RoleInfo inspected = SocialActionService.inspectRole(loadedEntity, player, contact.isFamily());
                        SocialActionService.RoleInfo role = new SocialActionService.RoleInfo(
                                loadedEntity.getName().getString(),
                                inspected.isBaby(),
                                inspected.isMinor(),
                                inspected.isFamily() || contact.isFamily(),
                                inspected.isSpouse() || contact.isSpouse(),
                                inspected.ageStage()
                        );
                        SocialActionService.perform(player, loadedEntity, data, this.action, role, SocialActionService.InteractionChannel.fromInterfaceType(this.interfaceType));
                        contact.updateIdentity(loadedEntity.getName().getString(), data.getPersonality(),
                                role.isBaby(), role.isMinor(), role.ageStage(), role.isFamily(), role.isSpouse(),
                                player.level().getGameTime());
                        contact.updateSocialValues(data);
                        contacts.addOrUpdate(contact);
                    });
                } else {
                    SocialActionService.RoleInfo role = new SocialActionService.RoleInfo(
                            contact.getName(),
                            contact.isBaby(),
                            contact.isMinor(),
                            contact.isFamily(),
                            contact.isSpouse(),
                            contact.getAgeStage()
                    );
                    SocialActionService.perform(player, null, contact, this.action, role, SocialActionService.InteractionChannel.fromInterfaceType(this.interfaceType));
                    contact.setPendingSync(true);
                    contacts.addOrUpdate(contact);
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }

    private static final java.util.Map<String, Long> DEDUP_CACHE = new ConcurrentHashMap<>();
}
