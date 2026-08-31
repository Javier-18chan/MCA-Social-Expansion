package com.joelj.mcasocial.social;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class SocialContactHelper {
    private SocialContactHelper() {}

    public static void remember(ServerPlayer player, Entity entity, ISocialData data,
                                boolean isBaby, boolean isMinor, boolean isFamily, boolean isSpouse) {
        remember(player, entity, data, isBaby, isMinor, SocialAgeStage.fromFlags(isBaby, isMinor), isFamily, isSpouse);
    }

    public static void remember(ServerPlayer player, Entity entity, ISocialData data,
                                boolean isBaby, boolean isMinor, SocialAgeStage ageStage,
                                boolean isFamily, boolean isSpouse) {
        player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).ifPresent(contacts -> {
            SocialContact contact = contacts.getContact(entity.getUUID());
            if (contact == null) {
                contact = new SocialContact(entity.getUUID(), entity.getName().getString(), data.getPersonality());
            } else if (contact.isPendingSync()) {
                contact.pushSocialValuesTo(data);
                contact.setPendingSync(false);
            }

            contact.updateIdentity(
                    entity.getName().getString(),
                    data.getPersonality(),
                    isBaby,
                    isMinor,
                    ageStage,
                    isFamily,
                    isSpouse,
                    player.level().getGameTime()
            );
            contact.updateSocialValues(data);
            GiftPreferenceProfile.ensure(contact);
            contacts.addOrUpdate(contact);
        });
    }
}
