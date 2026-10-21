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
import java.util.function.Function;

public class SocialContacts implements ISocialContacts {
    private static final int MAX_CONTACTS = 128;
    private static final int MAX_PENDING_ACTIONS = 16;
    private static final int MAX_SPONTANEOUS_MESSAGES = 32;
    private final Map<UUID, SocialContact> contacts = new LinkedHashMap<>();
    private final List<PendingSocialAction> pendingActions = new ArrayList<>();
    private final List<SpontaneousSocialMessage> spontaneousMessages = new ArrayList<>();
    private DeliveryDelayMode deliveryDelayMode = DeliveryDelayMode.SHORT;
    private int preferredInterfaceId = SocialInterfaceType.PHONE;
    private boolean spontaneousEnabled = true;
    private long nextSpontaneousCheck;
    private long spontaneousDay = -1L;
    private int spontaneousCount;

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
            removeContact(firstKey);
        }
        contacts.put(contact.getUuid(), contact);
    }

    @Override
    public void removeContact(UUID uuid) {
        contacts.remove(uuid);
        pendingActions.removeIf(action -> action.contactUuid().equals(uuid));
        spontaneousMessages.removeIf(message -> message.contactUuid().equals(uuid));
    }

    @Override
    public void copyFrom(ISocialContacts other) {
        contacts.clear();
        for (SocialContact contact : other.getContacts()) {
            addOrUpdate(contact.copy());
        }
        this.deliveryDelayMode = other.getDeliveryDelayMode();
        this.preferredInterfaceId = other.getPreferredInterfaceId();
        this.pendingActions.clear();
        this.pendingActions.addAll(other.getPendingActions());
        this.spontaneousEnabled = other.isSpontaneousEnabled();
        this.nextSpontaneousCheck = other.getNextSpontaneousCheck();
        this.spontaneousDay = other instanceof SocialContacts saved ? saved.spontaneousDay : -1L;
        this.spontaneousCount = other instanceof SocialContacts saved ? saved.spontaneousCount : 0;
        this.spontaneousMessages.clear();
        this.spontaneousMessages.addAll(other.getSpontaneousMessages());
    }

    @Override
    public DeliveryDelayMode getDeliveryDelayMode() {
        return deliveryDelayMode;
    }

    @Override
    public void setDeliveryDelayMode(DeliveryDelayMode mode) {
        this.deliveryDelayMode = mode == null ? DeliveryDelayMode.SHORT : mode;
    }

    @Override public int getPreferredInterfaceId() { return preferredInterfaceId; }
    @Override public void setPreferredInterfaceId(int interfaceType) {
        preferredInterfaceId = SocialInterfaceType.normalize(interfaceType);
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

    @Override public boolean isSpontaneousEnabled() { return spontaneousEnabled; }
    @Override public void setSpontaneousEnabled(boolean enabled) {
        spontaneousEnabled = enabled;
        if (!enabled) spontaneousMessages.removeIf(message -> !message.delivered());
    }
    @Override public long getNextSpontaneousCheck() { return nextSpontaneousCheck; }
    @Override public void setNextSpontaneousCheck(long gameTime) { nextSpontaneousCheck = gameTime; }

    @Override
    public int getSpontaneousCount(long day) {
        if (spontaneousDay != day) {
            spontaneousDay = day;
            spontaneousCount = 0;
        }
        return spontaneousCount;
    }

    @Override
    public void recordSpontaneousMessage(long day) {
        getSpontaneousCount(day);
        spontaneousCount++;
    }

    @Override public List<SpontaneousSocialMessage> getSpontaneousMessages() {
        return List.copyOf(spontaneousMessages);
    }

    @Override
    public void addSpontaneousMessage(SpontaneousSocialMessage message) {
        if (message == null) return;
        if (spontaneousMessages.size() >= MAX_SPONTANEOUS_MESSAGES) spontaneousMessages.remove(0);
        spontaneousMessages.add(message);
    }

    @Override
    public List<SpontaneousSocialMessage> deliverSpontaneousMessages(long gameTime,
                                                                     Function<SpontaneousSocialMessage, String> dialogueKeySelector) {
        List<SpontaneousSocialMessage> due = new ArrayList<>();
        for (int i = 0; i < spontaneousMessages.size(); i++) {
            SpontaneousSocialMessage message = spontaneousMessages.get(i);
            if (!message.delivered() && message.deliveryGameTime() <= gameTime) {
                SpontaneousSocialMessage delivered = message.deliveredCopy(dialogueKeySelector.apply(message));
                spontaneousMessages.set(i, delivered);
                due.add(delivered);
            }
        }
        return due;
    }

    @Override
    public SpontaneousSocialMessage replyToSpontaneousMessage(UUID messageId, String responseKey, long responseGameTime) {
        for (int i = 0; i < spontaneousMessages.size(); i++) {
            SpontaneousSocialMessage message = spontaneousMessages.get(i);
            if (message.id().equals(messageId) && message.delivered() && !message.replied()) {
                spontaneousMessages.set(i, message.repliedCopy(responseKey, responseGameTime));
                return message;
            }
        }
        return null;
    }

    @Override
    public List<SpontaneousSocialMessage> deliverSpontaneousReplies(long gameTime) {
        List<SpontaneousSocialMessage> due = new ArrayList<>();
        for (int i = 0; i < spontaneousMessages.size(); i++) {
            SpontaneousSocialMessage message = spontaneousMessages.get(i);
            if (message.replied() && !message.responseDelivered()
                    && !message.responseKey().isEmpty() && message.responseGameTime() <= gameTime) {
                spontaneousMessages.set(i, message.responseDeliveredCopy());
                due.add(message);
            }
        }
        return due;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 7);
        tag.putInt("DeliveryDelayMode", deliveryDelayMode.ordinal());
        tag.putInt("PreferredInterfaceId", preferredInterfaceId);
        tag.putBoolean("SpontaneousEnabled", spontaneousEnabled);
        tag.putLong("NextSpontaneousCheck", nextSpontaneousCheck);
        tag.putLong("SpontaneousDay", spontaneousDay);
        tag.putInt("SpontaneousCount", spontaneousCount);
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
        ListTag incomingList = new ListTag();
        for (SpontaneousSocialMessage message : spontaneousMessages) incomingList.add(message.save());
        tag.put("SpontaneousMessages", incomingList);
        return tag;
    }

    public void load(CompoundTag tag) {
        contacts.clear();
        pendingActions.clear();
        spontaneousMessages.clear();
        deliveryDelayMode = tag.contains("DeliveryDelayMode")
                ? DeliveryDelayMode.fromOrdinal(tag.getInt("DeliveryDelayMode"))
                : DeliveryDelayMode.SHORT;
        preferredInterfaceId = SocialInterfaceType.normalize(tag.getInt("PreferredInterfaceId"));
        spontaneousEnabled = !tag.contains("SpontaneousEnabled") || tag.getBoolean("SpontaneousEnabled");
        nextSpontaneousCheck = tag.getLong("NextSpontaneousCheck");
        spontaneousDay = tag.contains("SpontaneousDay") ? tag.getLong("SpontaneousDay") : -1L;
        spontaneousCount = Math.max(0, Math.min(2, tag.getInt("SpontaneousCount")));
        ListTag list = tag.getList("Contacts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            addOrUpdate(SocialContact.load(list.getCompound(i)));
        }
        ListTag pendingList = tag.getList("PendingActions", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingList.size() && pendingActions.size() < MAX_PENDING_ACTIONS; i++) {
            PendingSocialAction action = PendingSocialAction.load(pendingList.getCompound(i));
            if (action != null) pendingActions.add(action);
        }
        ListTag incomingList = tag.getList("SpontaneousMessages", Tag.TAG_COMPOUND);
        for (int i = 0; i < incomingList.size() && spontaneousMessages.size() < MAX_SPONTANEOUS_MESSAGES; i++) {
            SpontaneousSocialMessage message = SpontaneousSocialMessage.load(incomingList.getCompound(i));
            if (message != null && contacts.containsKey(message.contactUuid())) spontaneousMessages.add(message);
        }
    }
}
