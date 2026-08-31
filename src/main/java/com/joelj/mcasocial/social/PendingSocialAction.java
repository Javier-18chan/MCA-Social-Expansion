package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record PendingSocialAction(UUID contactUuid, String action, int interfaceType, long deliveryGameTime) {
    public PendingSocialAction {
        interfaceType = SocialInterfaceType.normalize(interfaceType);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("ContactUuid", contactUuid);
        tag.putString("Action", action);
        tag.putInt("InterfaceType", interfaceType);
        tag.putLong("DeliveryGameTime", deliveryGameTime);
        return tag;
    }

    public static PendingSocialAction load(CompoundTag tag) {
        if (!tag.hasUUID("ContactUuid")) return null;
        String action = tag.getString("Action");
        if (action.isBlank()) return null;
        return new PendingSocialAction(
                tag.getUUID("ContactUuid"),
                action,
                tag.getInt("InterfaceType"),
                tag.getLong("DeliveryGameTime")
        );
    }
}
