package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DeceasedVillagers extends SavedData {
    private static final String DATA_NAME = "mcasocial_deceased_villagers";
    private final Set<UUID> uuids = new HashSet<>();

    public static DeceasedVillagers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DeceasedVillagers::load, DeceasedVillagers::new, DATA_NAME);
    }

    private static DeceasedVillagers load(CompoundTag tag) {
        DeceasedVillagers data = new DeceasedVillagers();
        ListTag list = tag.getList("Uuids", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                data.uuids.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    public boolean contains(UUID uuid) {
        return uuids.contains(uuid);
    }

    public void mark(UUID uuid) {
        if (uuids.add(uuid)) setDirty();
    }

    public void unmark(UUID uuid) {
        if (uuids.remove(uuid)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        uuids.forEach(uuid -> list.add(StringTag.valueOf(uuid.toString())));
        tag.put("Uuids", list);
        return tag;
    }
}
