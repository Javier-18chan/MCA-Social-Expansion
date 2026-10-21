package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record SpontaneousSocialMessage(UUID id, UUID contactUuid, String dialogueKey, int interfaceType,
                                       long deliveryGameTime, boolean delivered, boolean replied,
                                       String responseKey, long responseGameTime, boolean responseDelivered) {
    public SpontaneousSocialMessage deliveredCopy(String selectedDialogueKey) {
        return new SpontaneousSocialMessage(id, contactUuid, selectedDialogueKey, interfaceType,
                deliveryGameTime, true, replied, responseKey, responseGameTime, responseDelivered);
    }

    public SpontaneousSocialMessage repliedCopy(String selectedResponseKey, long responseTime) {
        return new SpontaneousSocialMessage(id, contactUuid, dialogueKey, interfaceType,
                deliveryGameTime, true, true, selectedResponseKey, responseTime, selectedResponseKey.isEmpty());
    }

    public SpontaneousSocialMessage responseDeliveredCopy() {
        return new SpontaneousSocialMessage(id, contactUuid, dialogueKey, interfaceType,
                deliveryGameTime, delivered, replied, responseKey, responseGameTime, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("ContactUuid", contactUuid);
        tag.putString("DialogueKey", dialogueKey);
        tag.putInt("InterfaceType", interfaceType);
        tag.putLong("DeliveryGameTime", deliveryGameTime);
        tag.putBoolean("Delivered", delivered);
        tag.putBoolean("Replied", replied);
        tag.putString("ResponseKey", responseKey);
        tag.putLong("ResponseGameTime", responseGameTime);
        tag.putBoolean("ResponseDelivered", responseDelivered);
        return tag;
    }

    public static SpontaneousSocialMessage load(CompoundTag tag) {
        if (!tag.hasUUID("Id") || !tag.hasUUID("ContactUuid")) return null;
        String key = tag.getString("DialogueKey");
        if (!key.startsWith("chat.mcasocial.spontaneous.") || key.length() > 100) return null;
        String responseKey = tag.getString("ResponseKey");
        if (!responseKey.isEmpty() && (!responseKey.startsWith("chat.mcasocial.spontaneous.response.")
                || responseKey.length() > 100)) return null;
        return new SpontaneousSocialMessage(tag.getUUID("Id"), tag.getUUID("ContactUuid"), key,
                SocialInterfaceType.normalize(tag.getInt("InterfaceType")),
                tag.getLong("DeliveryGameTime"), tag.getBoolean("Delivered"), tag.getBoolean("Replied"),
                responseKey, tag.getLong("ResponseGameTime"),
                responseKey.isEmpty() || tag.getBoolean("ResponseDelivered"));
    }
}
