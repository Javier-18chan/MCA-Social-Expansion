package com.joelj.mcasocial.social;

import forge.net.conczin.mca.server.world.data.FamilyTree;
import forge.net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class SocialKinship {
    private SocialKinship() {}

    public static int generation(ServerPlayer player, UUID villager) {
        if (villager == null) return 0;
        var tree = FamilyTree.get((net.minecraft.server.level.ServerLevel) player.level());
        return FamilyGeneration.distance(villager, player.getUUID(), id -> tree.getOrEmpty(id)
                .map(node -> node.streamParents().toList()).orElse(List.of()));
    }

    public static String gender(ServerPlayer player) {
        var data = PlayerSaveData.get(player);
        return data != null && data.getGender().toString().equalsIgnoreCase("FEMALE") ? "mama" : "papa";
    }

    public static Component address(ServerPlayer player, UUID villager, boolean capitalized) {
        return address(player, generation(player, villager), capitalized);
    }

    private static Component address(ServerPlayer player, int generation, boolean capitalized) {
        String relation = switch (generation) {
            case 1 -> "parent";
            case 2 -> "grandparent";
            case 3 -> "greatgrandparent";
            default -> "";
        };
        if (relation.isEmpty()) return player.getDisplayName();
        return Component.translatable("chat.mcasocial.family.address." + relation + "." + gender(player)
                + (capitalized ? ".capital" : ""));
    }

    public static MutableComponent dialogue(ServerPlayer player, UUID villager, String key) {
        if (key.contains(".papa") || key.contains(".mama")) {
            int generation = generation(player, villager);
            return Component.translatable(key, address(player, generation, false), address(player, generation, true));
        }
        return Component.translatable(key);
    }

    public static String greeting(ServerPlayer player, SocialContact contact, UUID messageId) {
        int generation = generation(player, contact.getUuid());
        String relation = switch (generation) {
            case 1 -> "";
            case 2 -> "grandparent.";
            case 3 -> "greatgrandparent.";
            default -> "relative.";
        };
        String prefix = "chat.mcasocial.spontaneous.greeting.";
        if (relation.isEmpty()) return prefix + gender(player);
        int variant = Math.floorMod(messageId.hashCode(), 3) + 1;
        return prefix + relation + (generation == 2 || generation == 3 ? gender(player) + "." : "") + variant;
    }

    public static String spontaneousKey(ServerPlayer player, SocialContact contact, int variant, String fallback) {
        if (!contact.isFamily() || contact.isSpouse()) return fallback;
        int generation = generation(player, contact.getUuid());
        if (generation != 2 && generation != 3) return fallback;
        String stage = switch (contact.getAgeStage()) {
            case BABY, CHILD -> "child";
            case TEEN -> "teen";
            default -> "adult";
        };
        return "chat.mcasocial.spontaneous.descendant." + generation + "." + stage + "." + variant;
    }
}
