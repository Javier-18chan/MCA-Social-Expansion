package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SocialContacts implements ISocialContacts {
    private static final int MAX_CONTACTS = 128;
    private final Map<UUID, SocialContact> contacts = new LinkedHashMap<>();

    @Override
    public Collection<SocialContact> getContacts() {
        return contacts.values();
    }

    @Override
    public SocialContact getContact(UUID uuid) {
        return contacts.get(uuid);
    }

    @Override
    public void addOrUpdate(SocialContact contact) {
        if (!contacts.containsKey(contact.getUuid()) && contacts.size() >= MAX_CONTACTS) {
            UUID firstKey = contacts.keySet().iterator().next();
            contacts.remove(firstKey);
        }
        contacts.put(contact.getUuid(), contact);
    }

    @Override
    public void removeContact(UUID uuid) {
        contacts.remove(uuid);
    }

    @Override
    public void copyFrom(ISocialContacts other) {
        contacts.clear();
        for (SocialContact contact : other.getContacts()) {
            addOrUpdate(contact.copy());
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SocialContact contact : contacts.values()) {
            list.add(contact.save());
        }
        tag.put("Contacts", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        contacts.clear();
        ListTag list = tag.getList("Contacts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            addOrUpdate(SocialContact.load(list.getCompound(i)));
        }
    }
}
