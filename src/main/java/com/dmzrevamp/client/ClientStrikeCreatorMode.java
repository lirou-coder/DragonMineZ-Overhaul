package com.dmzrevamp.client;

public final class ClientStrikeCreatorMode {
    private static boolean nextCreatorIsStrike;
    private static boolean nextSkillsMenuIsStrike;

    private ClientStrikeCreatorMode() {
    }

    public static void markNextCreatorAsStrike() {
        nextCreatorIsStrike = true;
    }

    public static boolean consumeNextCreatorIsStrike() {
        boolean value = nextCreatorIsStrike;
        nextCreatorIsStrike = false;
        return value;
    }

    public static void markNextSkillsMenuAsStrike() {
        nextSkillsMenuIsStrike = true;
    }

    public static boolean consumeNextSkillsMenuIsStrike() {
        boolean value = nextSkillsMenuIsStrike;
        nextSkillsMenuIsStrike = false;
        return value;
    }
}
