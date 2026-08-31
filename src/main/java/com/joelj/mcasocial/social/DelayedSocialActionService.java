package com.joelj.mcasocial.social;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class DelayedSocialActionService {
    private DelayedSocialActionService() {}

    public static void submit(ServerPlayer player, UUID contactUuid, String action, int interfaceType) {
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (contacts == null) return;

        int delayTicks = contacts.getDeliveryDelayMode().delayTicks(interfaceType);
        if (delayTicks <= 0) {
            ContactInteractionService.perform(player, contactUuid, action, interfaceType);
            return;
        }

        SocialContact contact = contacts.getContact(contactUuid);
        if (contact == null) return;

        long deliveryTime = player.level().getGameTime() + delayTicks;
        PendingSocialAction pending = new PendingSocialAction(contactUuid, action, interfaceType, deliveryTime);
        if (!contacts.queueAction(pending)) {
            player.displayClientMessage(Component.translatable("chat.mcasocial.delivery.queue_full"), true);
            return;
        }

        int seconds = Math.max(1, (int) Math.ceil(delayTicks / 20.0));
        player.displayClientMessage(Component.translatable(
                "chat.mcasocial.delivery.queued", contact.getName(), seconds), true);
        SocialInterfaceSounds.playSent(player, interfaceType);
    }

    public static void tick(ServerPlayer player) {
        ISocialContacts contacts = player.getCapability(SocialContactsCapability.SOCIAL_CONTACTS).orElse(null);
        if (contacts == null) return;

        List<PendingSocialAction> due = contacts.removeDueActions(player.level().getGameTime());
        for (PendingSocialAction action : due) {
            if (!SocialActionService.isValidAction(action.action())) continue;
            ContactInteractionService.perform(player, action.contactUuid(), action.action(), action.interfaceType());
        }
    }
}
