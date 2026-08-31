package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SocialContacts implements ISocialContacts {
    private static final int MAX_CONTACTS = 128;
    private static final int MAX_PENDING_ACTIONS = 16;
    private final Map<UUID, SocialContact> contacts = new LinkedHashMap<>();
    private final List<PendingSocialAction> pendingActions = new ArrayList<>();
    private DeliveryDelayMode deliveryDelayMode = DeliveryDelayMode.SHORT;

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
        this.deliveryDelayMode = other.getDeliveryDelayMode();
        this.pendingActions.clear();
        this.pendingActions.addAll(other.getPendingActions());
    }

    @Override
    public DeliveryDelayMode getDeliveryDelayMode() {
        return deliveryDelayMode;
    }

    @Override
    public void setDeliveryDelayMode(DeliveryDelayMode mode) {
        this.deliveryDelayMode = mode == null ? DeliveryDelayMode.SHORT : mode;
    }

    @Override
    public List<PendingSocialAction> getPendingActions() {
        return List.copyOf(pendingActions);
    }

    @Override
    public boolean queueAction(PendingSocialAction action) {
        if (action == null || pendingActions.size() >= MAX_PENDING_ACTIONS) return false;
        boolean duplicate = pendingActions.stream().anyMatch(pending ->
                pending.contactUuid().equals(action.contactUuid()) && pending.action().equals(action.action()));
        if (duplicate) return false;
        pendingActions.add(action);
        return true;
    }

    @Override
    public List<PendingSocialAction> removeDueActions(long gameTime) {
        List<PendingSocialAction> due = new ArrayList<>();
        Iterator<PendingSocialAction> iterator = pendingActions.iterator();
        while (iterator.hasNext()) {
            PendingSocialAction action = iterator.next();
            if (action.deliveryGameTime() <= gameTime) {
                due.add(action);
                iterator.remove();
            }
        }
        return due;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 3);
        tag.putInt("DeliveryDelayMode", deliveryDelayMode.ordinal());
        ListTag list = new ListTag();
        for (SocialContact contact : contacts.values()) {
            list.add(contact.save());
        }
        tag.put("Contacts", list);
        ListTag pendingList = new ListTag();
        for (PendingSocialAction action : pendingActions) {
            pendingList.add(action.save());
        }
        tag.put("PendingActions", pendingList);
        return tag;
    }

    public void load(CompoundTag tag) {
        contacts.clear();
        pendingActions.clear();
        deliveryDelayMode = tag.contains("DeliveryDelayMode")
                ? DeliveryDelayMode.fromOrdinal(tag.getInt("DeliveryDelayMode"))
                : DeliveryDelayMode.SHORT;
        ListTag list = tag.getList("Contacts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            addOrUpdate(SocialContact.load(list.getCompound(i)));
        }
        ListTag pendingList = tag.getList("PendingActions", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingList.size() && pendingActions.size() < MAX_PENDING_ACTIONS; i++) {
            PendingSocialAction action = PendingSocialAction.load(pendingList.getCompound(i));
            if (action != null) pendingActions.add(action);
        }
    }
}
