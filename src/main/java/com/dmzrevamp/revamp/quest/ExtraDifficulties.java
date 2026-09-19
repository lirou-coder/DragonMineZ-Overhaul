package com.dmzrevamp.revamp.quest;

import com.dragonminez.common.quest.Difficulty;

public final class ExtraDifficulties {
    public static final String NIGHTMARE = "NIGHTMARE";
    public static final String YOU_MUST_DIE = "YOU_MUST_DIE";

    private ExtraDifficulties() {}

    public static boolean isExtra(Difficulty difficulty) {
        return difficulty != null && (NIGHTMARE.equals(difficulty.name()) || YOU_MUST_DIE.equals(difficulty.name()));
    }
}
