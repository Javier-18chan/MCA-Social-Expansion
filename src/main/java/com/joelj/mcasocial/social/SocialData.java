package com.joelj.mcasocial.social;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SocialData implements ISocialData {
    private int affinity = 0;
    private int romance = 0;
    private int trust = 8;
    private int reputation = 0;
    private String personality;

    private Map<String, Long> lastActionTimes = new HashMap<>();
    private Map<String, Integer> actionUseCounts = new HashMap<>();

    private int spamCount = 0;
    private long lockoutTime = 0;

    private boolean hasUnlockedLetter = false;
    private int selectedInterfaceId = 0;

    private static final String[] TRAITS = {
            "Extrovertido", "Tímido", "Ambicioso", "Chismoso",
            "Orgulloso", "Leal", "Sarcástico", "Romántico"
    };

    public SocialData() {
        Random random = new Random();
        this.personality = TRAITS[random.nextInt(TRAITS.length)];
    }

    @Override public int getAffinity() { return affinity; }
    @Override public void setAffinity(int value) { this.affinity = value; }
    @Override public int getRomance() { return romance; }
    @Override public void setRomance(int value) { this.romance = value; }
    @Override public int getTrust() { return trust; }
    @Override public void setTrust(int value) { this.trust = value; }
    @Override public int getReputation() { return reputation; }
    @Override public void setReputation(int value) { this.reputation = value; }
    @Override public String getPersonality() { return personality; }
    @Override public void setPersonality(String type) { this.personality = type; }

    @Override public long getLastActionTime(String action) { return lastActionTimes.getOrDefault(action, 0L); }
    @Override public void setLastActionTime(String action, long time) { this.lastActionTimes.put(action, time); }

    @Override public int getSpamCount() { return spamCount; }
    @Override public void setSpamCount(int count) { this.spamCount = count; }
    @Override public long getLockoutTime() { return lockoutTime; }
    @Override public void setLockoutTime(long time) { this.lockoutTime = time; }

    @Override public int getActionUseCount(String action) { return actionUseCounts.getOrDefault(action, 0); }
    @Override public void setActionUseCount(String action, int count) { this.actionUseCounts.put(action, count); }

    @Override public boolean hasUnlockedLetter() { return hasUnlockedLetter; }
    @Override public void setHasUnlockedLetter(boolean unlocked) { this.hasUnlockedLetter = unlocked; }

    public int getSelectedInterfaceId() { return selectedInterfaceId; }
    public void setSelectedInterfaceId(int id) { this.selectedInterfaceId = SocialInterfaceType.normalize(id); }
}
