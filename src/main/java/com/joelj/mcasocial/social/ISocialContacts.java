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

    int getPreferredInterfaceId();

    void setPreferredInterfaceId(int interfaceType);

    List<PendingSocialAction> getPendingActions();

    boolean queueAction(PendingSocialAction action);

    List<PendingSocialAction> removeDueActions(long gameTime);

    boolean isSpontaneousEnabled();

    void setSpontaneousEnabled(boolean enabled);

    long getNextSpontaneousCheck();

    void setNextSpontaneousCheck(long gameTime);

    int getSpontaneousCount(long day);

    void recordSpontaneousMessage(long day);

    List<SpontaneousSocialMessage> getSpontaneousMessages();

    void addSpontaneousMessage(SpontaneousSocialMessage message);

    List<SpontaneousSocialMessage> deliverSpontaneousMessages(long gameTime,
                                                              java.util.function.Function<SpontaneousSocialMessage, String> dialogueKeySelector);

    SpontaneousSocialMessage replyToSpontaneousMessage(UUID messageId, String responseKey, long responseGameTime);

    List<SpontaneousSocialMessage> deliverSpontaneousReplies(long gameTime);
}
