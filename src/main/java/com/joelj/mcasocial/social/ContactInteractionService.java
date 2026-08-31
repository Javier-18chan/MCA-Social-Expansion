package com.joelj.mcasocial.social;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class ContactInteractionService {
    private ContactInteractionService() {}

    public static boolean perform(ServerPlayer player, UUID contactUuid, String action, int interfaceType) {
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (contacts == null) return false;

        SocialContact contact = contacts.getContact(contactUuid);
        if (contact == null) return false;

        Entity loadedEntity = ((ServerLevel) player.level()).getEntity(contactUuid);
        if (loadedEntity != null) {
            ISocialData data = loadedEntity.getCapability(SocialCapability.SOCIAL_CAPABILITY).orElse(null);
            if (data == null) return false;

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
            SocialActionService.InteractionChannel channel = SocialActionService.InteractionChannel.fromInterfaceType(interfaceType);
            if (SocialDiscoveryService.isDiscoveryAction(action)) {
                SocialDiscoveryService.perform(player, contact, role, channel);
                data.setAffinity(contact.getAffinity());
            } else {
                SocialActionService.perform(player, loadedEntity, data, action, role, channel);
            }
            contact.updateIdentity(loadedEntity.getName().getString(), data.getPersonality(),
                    role.isBaby(), role.isMinor(), role.ageStage(), role.isFamily(), role.isSpouse(),
                    player.level().getGameTime());
            contact.updateSocialValues(data);
        } else {
            SocialActionService.RoleInfo role = new SocialActionService.RoleInfo(
                    contact.getName(),
                    contact.isBaby(),
                    contact.isMinor(),
                    contact.isFamily(),
                    contact.isSpouse(),
                    contact.getAgeStage()
            );
            SocialActionService.InteractionChannel channel = SocialActionService.InteractionChannel.fromInterfaceType(interfaceType);
            if (SocialDiscoveryService.isDiscoveryAction(action)) {
                SocialDiscoveryService.perform(player, contact, role, channel);
            } else {
                SocialActionService.perform(player, null, contact, action, role, channel);
            }
            contact.setPendingSync(true);
        }

        contacts.addOrUpdate(contact);
        return true;
    }
}
