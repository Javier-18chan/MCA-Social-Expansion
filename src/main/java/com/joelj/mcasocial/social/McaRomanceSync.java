package com.joelj.mcasocial.social;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public final class McaRomanceSync {
    private McaRomanceSync() {}

    public static void reconcile(VillagerEntityMCA villager, ServerPlayer player, ISocialData data,
                                 @Nullable SocialContact contact) {
        try {
            var memories = villager.getVillagerBrain().getMemoriesForPlayer(player);
            int mcaHearts = memories.getHearts();
            int backup = data.getRomance();
            if (backup == 0 && contact != null) {
                backup = contact.getRomance();
            }

            if (mcaHearts == 0 && backup != 0) {
                data.setRomance(backup);
                memories.setHearts(backup);
            } else if (mcaHearts != data.getRomance()) {
                data.setRomance(mcaHearts);
            }
        } catch (Exception ignored) {}
    }

    public static void pushCached(VillagerEntityMCA villager, ServerPlayer player, ISocialData data) {
        try {
            villager.getVillagerBrain().getMemoriesForPlayer(player).setHearts(data.getRomance());
        } catch (Exception ignored) {}
    }
}
