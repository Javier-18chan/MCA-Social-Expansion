package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;

public final class SocialMemory {
    private int tension;
    private String lastConflict = "";
    private long lastConflictTime = -1L;
    private boolean apologyAccepted;
    private long lastApologyAttempt = -1L;
    private long lastApologyTime = -1L;
    private long lastReconciliationAttempt = -1L;
    private long interestsTime = -1L;
    private long plansTime = -1L;
    private long memoriesTime = -1L;

    public int tension() { return tension; }
    public boolean apologyAccepted() { return apologyAccepted; }
    public long lastConflictTime() { return lastConflictTime; }
    public long lastApologyAttempt() { return lastApologyAttempt; }
    public long lastApologyTime() { return lastApologyTime; }
    public long lastReconciliationAttempt() { return lastReconciliationAttempt; }

    public void recordConflict(String kind, int severity, long time) {
        int repeated = kind.equals(lastConflict) && recent(lastConflictTime, time, 24000L) ? 1 : 0;
        tension = Math.min(10, tension + severity + repeated);
        lastConflict = kind;
        lastConflictTime = time;
        apologyAccepted = false;
    }

    public void recordApologyAttempt(long time, boolean accepted) {
        lastApologyAttempt = time;
        if (accepted) {
            apologyAccepted = true;
            lastApologyTime = time;
            tension = Math.max(1, tension - 1);
        }
    }

    public void recordReconciliationAttempt(long time, boolean accepted) {
        lastReconciliationAttempt = time;
        if (accepted) {
            tension = 0;
            lastConflict = "";
            apologyAccepted = false;
        }
    }

    public boolean recentlyDiscussed(String topic, long time) {
        return recent(topicTime(topic), time, 48000L);
    }

    public void recordTopic(String topic, long time) {
        switch (topic) {
            case "interests" -> interestsTime = time;
            case "plans" -> plansTime = time;
            case "memories" -> memoriesTime = time;
            default -> { }
        }
    }

    private long topicTime(String topic) {
        return switch (topic) {
            case "interests" -> interestsTime;
            case "plans" -> plansTime;
            case "memories" -> memoriesTime;
            default -> -1L;
        };
    }

    public static boolean recent(long previous, long now, long interval) {
        return previous >= 0 && now >= previous && now - previous < interval;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Tension", tension);
        tag.putString("LastConflict", lastConflict);
        tag.putLong("LastConflictTime", lastConflictTime);
        tag.putBoolean("ApologyAccepted", apologyAccepted);
        tag.putLong("LastApologyAttempt", lastApologyAttempt);
        tag.putLong("LastApologyTime", lastApologyTime);
        tag.putLong("LastReconciliationAttempt", lastReconciliationAttempt);
        tag.putLong("InterestsTime", interestsTime);
        tag.putLong("PlansTime", plansTime);
        tag.putLong("MemoriesTime", memoriesTime);
        return tag;
    }

    public void load(CompoundTag tag) {
        tension = Math.max(0, Math.min(10, tag.getInt("Tension")));
        lastConflict = tag.getString("LastConflict");
        lastConflictTime = time(tag, "LastConflictTime");
        apologyAccepted = tension > 0 && tag.getBoolean("ApologyAccepted");
        lastApologyAttempt = time(tag, "LastApologyAttempt");
        lastApologyTime = time(tag, "LastApologyTime");
        lastReconciliationAttempt = time(tag, "LastReconciliationAttempt");
        interestsTime = time(tag, "InterestsTime");
        plansTime = time(tag, "PlansTime");
        memoriesTime = time(tag, "MemoriesTime");
    }

    public void copyFrom(SocialMemory other) {
        load(other.save());
    }

    private static long time(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getLong(key) : -1L;
    }
}
