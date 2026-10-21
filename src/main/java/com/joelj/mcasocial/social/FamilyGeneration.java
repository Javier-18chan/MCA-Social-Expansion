package com.joelj.mcasocial.social;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class FamilyGeneration {
    private FamilyGeneration() {}

    static int distance(UUID descendant, UUID ancestor, Function<UUID, List<UUID>> parents) {
        if (descendant == null || ancestor == null || descendant.equals(ancestor)) return 0;
        Set<UUID> visited = new HashSet<>();
        visited.add(descendant);
        Set<UUID> current = Set.of(descendant);
        for (int generation = 1; generation <= 8 && !current.isEmpty(); generation++) {
            Set<UUID> next = new HashSet<>();
            for (UUID person : current) {
                for (UUID parent : parents.apply(person)) {
                    if (ancestor.equals(parent)) return generation;
                    if (parent != null && visited.add(parent)) next.add(parent);
                }
            }
            current = next;
        }
        return 0;
    }
}
