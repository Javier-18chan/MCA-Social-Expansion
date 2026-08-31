package com.joelj.mcasocial.social;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ISocialContacts {
    Collection<SocialContact> getContacts();

    SocialContact getContact(UUID uuid);

    void addOrUpdate(SocialContact contact);

    void removeContact(UUID uuid);

    void copyFrom(ISocialContacts other);

    DeliveryDelayMode getDeliveryDelayMode();

    void setDeliveryDelayMode(DeliveryDelayMode mode);

    List<PendingSocialAction> getPendingActions();

    boolean queueAction(PendingSocialAction action);

    List<PendingSocialAction> removeDueActions(long gameTime);
}
