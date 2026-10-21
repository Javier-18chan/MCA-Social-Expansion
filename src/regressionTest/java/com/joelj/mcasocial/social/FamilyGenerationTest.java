package com.joelj.mcasocial.social;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FamilyGenerationTest {
    public static void main(String[] args) {
        UUID player = id(1), child = id(2), grandchild = id(3), greatGrandchild = id(4);
        UUID later = id(5), otherParent = id(6), sibling = id(7), unrelated = id(8);
        Map<UUID, List<UUID>> parents = new HashMap<>();
        parents.put(child, List.of(otherParent, player));
        parents.put(grandchild, List.of(child));
        parents.put(greatGrandchild, List.of(grandchild));
        parents.put(later, List.of(greatGrandchild));
        parents.put(sibling, List.of(otherParent));
        assertDistance(1, child, player, parents);
        assertDistance(2, grandchild, player, parents);
        assertDistance(3, greatGrandchild, player, parents);
        assertDistance(4, later, player, parents);
        assertDistance(0, sibling, player, parents);
        assertDistance(0, player, child, parents);
        assertDistance(0, unrelated, player, parents);
        assertDistance(0, player, player, parents);
        assertDistance(0, null, player, parents);
        parents.put(unrelated, List.of(sibling));
        parents.put(sibling, List.of(unrelated));
        assertDistance(0, unrelated, player, parents);
        parents.put(greatGrandchild, List.of(grandchild, player));
        assertDistance(1, greatGrandchild, player, parents);
        parents.put(unrelated, List.of(child, grandchild));
        assertDistance(2, unrelated, player, parents);
        System.out.println("Family generation regression checks passed.");
    }

    private static UUID id(long value) {
        return new UUID(0, value);
    }

    private static void assertDistance(int expected, UUID descendant, UUID ancestor,
                                       Map<UUID, List<UUID>> parents) {
        int actual = FamilyGeneration.distance(descendant, ancestor, id -> parents.getOrDefault(id, List.of()));
        if (actual != expected) {
            throw new AssertionError("Expected generation " + expected + ", got " + actual);
        }
    }
}
