package com.joelj.mcasocial.social;

public interface ISocialData {
    int getAffinity();
    void setAffinity(int value);

    int getRomance();
    void setRomance(int value);

    int getTrust();
    void setTrust(int value);

    int getReputation();
    void setReputation(int value);

    String getPersonality();
    void setPersonality(String type);

    long getLastActionTime(String action);
    void setLastActionTime(String action, long time);

    int getSpamCount();
    void setSpamCount(int count);

    long getLockoutTime();
    void setLockoutTime(long time);

    int getActionUseCount(String action);
    void setActionUseCount(String action, int count);

    boolean hasUnlockedLetter();
    void setHasUnlockedLetter(boolean unlocked);

    int getSelectedInterfaceId();
    void setSelectedInterfaceId(int id);
}
