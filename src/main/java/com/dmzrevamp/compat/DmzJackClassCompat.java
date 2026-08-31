package com.dmzrevamp.compat;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.List;

public final class DmzJackClassCompat {
    private DmzJackClassCompat() {}

    public static void registerIfPresent() {
        try {
            if (FMLLoader.getLoadingModList() == null || FMLLoader.getLoadingModList().getModFileById("dmzjackclass") == null) return;
        } catch (Throwable ignored) {
            return;
        }
        RaceStatsConfig.ClassStats stats = DmzClassConfigManager.createClassStats(
                3, 3, 3, 3, 3, 3,
                0.6D, 0.6D, 0.6D, 0.6D, 1.0D, 0.6D, 1.2D);
        RaceStatsConfig.Passive passive = new RaceStatsConfig.Passive();
        passive.setEnabled(true);
        passive.getValues().put("strStkPwrBoost", 0.2D);
        passive.getValues().put("eneResBoost", 0.15D);
        passive.getValues().put("parityThreshold", 0.1D);
        stats.setPassive(passive);
        DmzClassConfigManager.registerClassDefault("jack", stats,
                new DmzClassConfigManager.ClassMetadata("jack", "Jack", "#FFFFFF", List.of()));
    }
}
