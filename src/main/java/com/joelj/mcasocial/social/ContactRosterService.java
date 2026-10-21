package com.joelj.mcasocial.social;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.UUID;

public final class ContactRosterService {
    private ContactRosterService() {}

    public static void refresh(ServerPlayer player, ISocialContacts contacts) {
        purgeDeceased(player, contacts);
        for (SocialContact contact : new ArrayList<>(contacts.getContacts())) {
            Entity entity = findLoaded(player.server, contact.getUuid());
            if (entity instanceof VillagerEntityMCA && entity.isAlive()) {
                contact.refreshName(entity.getName().getString());
            }
        }
    }

    public static void purgeDeceased(ServerPlayer player, ISocialContacts contacts) {
        DeceasedVillagers deceased = DeceasedVillagers.get(player.server);
        for (SocialContact contact : new ArrayList<>(contacts.getContacts())) {
            if (!deceased.contains(contact.getUuid())) continue;
            Entity entity = findLoaded(player.server, contact.getUuid());
            if (entity instanceof VillagerEntityMCA && entity.isAlive() && !entity.isRemoved()) {
                deceased.unmark(contact.getUuid());
            } else {
                contacts.removeContact(contact.getUuid());
            }
        }
    }

    public static Entity findLoaded(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }
}
