package com.dmzrevamp.revamp.prestige;

public interface PrestigeDataAccess {
    int dmzrevamp$getPrestigeCount();

    void dmzrevamp$setPrestigeCount(int count);

    boolean dmzrevamp$isPrestigeEligibilityKnown();

    boolean dmzrevamp$isPrestigeEligible();

    void dmzrevamp$setPrestigeEligibility(boolean known, boolean eligible);
}
