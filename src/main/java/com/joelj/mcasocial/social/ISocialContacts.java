package com.joelj.mcasocial.social;

import java.util.Collection;
import java.util.UUID;

public interface ISocialContacts {
    Collection<SocialContact> getContacts();

    SocialContact getContact(UUID uuid);

    void addOrUpdate(SocialContact contact);

    void removeContact(UUID uuid);

    void copyFrom(ISocialContacts other);
}
