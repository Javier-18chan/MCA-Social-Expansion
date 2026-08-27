package com.joelj.mcasocial.social;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SocialContact implements ISocialData {
    private UUID uuid;
    private String name;
    private String personality;
    private int affinity;
    private int romance;
    private int trust = 8;
    private int reputation;
    private boolean baby;
    private boolean minor;
    private SocialAgeStage ageStage = SocialAgeStage.ADULT;
    private boolean family;
    private boolean spouse;
    private boolean favorite;
    private boolean hasUnlockedLetter;
    private int selectedInterfaceId;
    private int spamCount;
    private long lockoutTime;
    private long lastSeenGameTime;
    private boolean pendingSync;

    private final Map<String, Long> lastActionTimes = new HashMap<>();
    private final Map<String, Integer> actionUseCounts = new HashMap<>();

    public SocialContact(UUID uuid, String name, String personality) {
        this.uuid = uuid;
        this.name = name;
        this.personality = personality;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public boolean isBaby() { return baby; }
    public boolean isMinor() { return minor; }
    public SocialAgeStage getAgeStage() { return ageStage; }
    public boolean isFamily() { return family; }
    public boolean isSpouse() { return spouse; }
    public boolean isFavorite() { return favorite; }
    public long getLastSeenGameTime() { return lastSeenGameTime; }
    public boolean isPendingSync() { return pendingSync; }

    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public void setPendingSync(boolean pendingSync) { this.pendingSync = pendingSync; }

    public void updateIdentity(String name, String personality, boolean baby, boolean minor, boolean family, boolean spouse, long lastSeenGameTime) {
        updateIdentity(name, personality, baby, minor, SocialAgeStage.fromFlags(baby, minor),
                family, spouse, lastSeenGameTime);
    }

    public void updateIdentity(String name, String personality, boolean baby, boolean minor, SocialAgeStage ageStage,
                               boolean family, boolean spouse, long lastSeenGameTime) {
        this.name = name;
        this.personality = personality;
        this.baby = baby;
        this.minor = minor;
        this.ageStage = ageStage == null ? SocialAgeStage.fromFlags(baby, minor) : ageStage;
        this.family = family;
        this.spouse = spouse;
        this.lastSeenGameTime = lastSeenGameTime;
    }

    public void updateSocialValues(ISocialData data) {
        this.affinity = data.getAffinity();
        this.romance = data.getRomance();
        this.trust = data.getTrust();
        this.reputation = data.getReputation();
        this.hasUnlockedLetter = data.hasUnlockedLetter();
        this.selectedInterfaceId = data.getSelectedInterfaceId();
    }

    public void pushSocialValuesTo(ISocialData data) {
        data.setAffinity(this.affinity);
        data.setRomance(this.romance);
        data.setTrust(this.trust);
        data.setReputation(this.reputation);
        data.setHasUnlockedLetter(this.hasUnlockedLetter);
        data.setSelectedInterfaceId(this.selectedInterfaceId);
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
    @Override public void setLastActionTime(String action, long time) { lastActionTimes.put(action, time); }
    @Override public int getSpamCount() { return spamCount; }
    @Override public void setSpamCount(int count) { this.spamCount = count; }
    @Override public long getLockoutTime() { return lockoutTime; }
    @Override public void setLockoutTime(long time) { this.lockoutTime = time; }
    @Override public int getActionUseCount(String action) { return actionUseCounts.getOrDefault(action, 0); }
    @Override public void setActionUseCount(String action, int count) { actionUseCounts.put(action, count); }
    @Override public boolean hasUnlockedLetter() { return hasUnlockedLetter; }
    @Override public void setHasUnlockedLetter(boolean unlocked) { this.hasUnlockedLetter = unlocked; }
    @Override public int getSelectedInterfaceId() { return selectedInterfaceId; }
    @Override public void setSelectedInterfaceId(int id) { this.selectedInterfaceId = SocialInterfaceType.normalize(id); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Uuid", uuid);
        tag.putString("Name", name == null ? "" : name);
        tag.putString("Personality", personality == null ? "" : personality);
        tag.putInt("Affinity", affinity);
        tag.putInt("Romance", romance);
        tag.putInt("Trust", trust);
        tag.putInt("Reputation", reputation);
        tag.putBoolean("Baby", baby);
        tag.putBoolean("Minor", minor);
        tag.putInt("AgeStage", ageStage.ordinal());
        tag.putBoolean("Family", family);
        tag.putBoolean("Spouse", spouse);
        tag.putBoolean("Favorite", favorite);
        tag.putBoolean("HasUnlockedLetter", hasUnlockedLetter);
        tag.putInt("SelectedInterfaceId", selectedInterfaceId);
        tag.putInt("SpamCount", spamCount);
        tag.putLong("LockoutTime", lockoutTime);
        tag.putLong("LastSeenGameTime", lastSeenGameTime);
        tag.putBoolean("PendingSync", pendingSync);
        tag.put("LastActionTimes", saveLongMap(lastActionTimes));
        tag.put("ActionUseCounts", saveIntMap(actionUseCounts));
        return tag;
    }

    public static SocialContact load(CompoundTag tag) {
        SocialContact contact = new SocialContact(
                tag.hasUUID("Uuid") ? tag.getUUID("Uuid") : UUID.randomUUID(),
                tag.getString("Name"),
                tag.getString("Personality")
        );
        contact.affinity = tag.getInt("Affinity");
        contact.romance = tag.getInt("Romance");
        contact.trust = tag.getInt("Trust");
        contact.reputation = tag.getInt("Reputation");
        contact.baby = tag.getBoolean("Baby");
        contact.minor = tag.getBoolean("Minor");
        contact.ageStage = tag.contains("AgeStage")
                ? SocialAgeStage.fromOrdinal(tag.getInt("AgeStage"))
                : SocialAgeStage.fromFlags(contact.baby, contact.minor);
        contact.family = tag.getBoolean("Family");
        contact.spouse = tag.getBoolean("Spouse");
        contact.favorite = tag.getBoolean("Favorite");
        contact.hasUnlockedLetter = tag.getBoolean("HasUnlockedLetter");
        contact.selectedInterfaceId = SocialInterfaceType.normalize(tag.getInt("SelectedInterfaceId"));
        contact.spamCount = tag.getInt("SpamCount");
        contact.lockoutTime = tag.getLong("LockoutTime");
        contact.lastSeenGameTime = tag.getLong("LastSeenGameTime");
        contact.pendingSync = tag.getBoolean("PendingSync");
        loadLongMap(tag.getCompound("LastActionTimes"), contact.lastActionTimes);
        loadIntMap(tag.getCompound("ActionUseCounts"), contact.actionUseCounts);
        return contact;
    }

    public SocialContact copy() {
        SocialContact copy = new SocialContact(uuid, name, personality);
        copy.affinity = affinity;
        copy.romance = romance;
        copy.trust = trust;
        copy.reputation = reputation;
        copy.baby = baby;
        copy.minor = minor;
        copy.ageStage = ageStage;
        copy.family = family;
        copy.spouse = spouse;
        copy.favorite = favorite;
        copy.hasUnlockedLetter = hasUnlockedLetter;
        copy.selectedInterfaceId = selectedInterfaceId;
        copy.spamCount = spamCount;
        copy.lockoutTime = lockoutTime;
        copy.lastSeenGameTime = lastSeenGameTime;
        copy.pendingSync = pendingSync;
        copy.lastActionTimes.putAll(lastActionTimes);
        copy.actionUseCounts.putAll(actionUseCounts);
        return copy;
    }

    private static CompoundTag saveLongMap(Map<String, Long> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach(tag::putLong);
        return tag;
    }

    private static CompoundTag saveIntMap(Map<String, Integer> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach(tag::putInt);
        return tag;
    }

    private static void loadLongMap(CompoundTag tag, Map<String, Long> map) {
        for (String key : tag.getAllKeys()) {
            map.put(key, tag.getLong(key));
        }
    }

    private static void loadIntMap(CompoundTag tag, Map<String, Integer> map) {
        for (String key : tag.getAllKeys()) {
            map.put(key, tag.getInt(key));
        }
    }
}
