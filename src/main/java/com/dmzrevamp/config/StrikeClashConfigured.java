package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/** Server-authoritative tuning for melee/Strike clashes. */
public final class StrikeClashConfigured {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp")
            .resolve("StrikeClashConfigured.json");
    private static Config cached = new Config();

    private StrikeClashConfigured() {
    }

    public static synchronized void initialize() {
        reload();
    }

    public static synchronized void reload() {
        try {
            if (!Files.exists(PATH)) {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(new Config()));
            }
            Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
            cached = (loaded == null ? new Config() : loaded).sanitize();
            Files.writeString(PATH, GSON.toJson(cached));
        } catch (Exception exception) {
            LOGGER.warn("Could not load StrikeClashConfigured.json: {}", exception.getMessage());
            cached = new Config();
        }
    }

    public static Config get() {
        return cached;
    }

    public static final class Config {
        public boolean enabled = true;
        public float meterSpeedPerTick = 0.005F;
        public float goodAreaLow = 0.78F;
        public float goodAreaHigh = 0.96F;
        public float momentumGainDefaultMultiplier = 1.0F;
        public float offWindowMomentumEfficiency = 0.18F;
        public float momentumDecayPerTick = 0.96F;
        public float innerAdvantageLow = 0.20F;
        public float innerAdvantageHigh = 0.80F;
        public int maxClashDurationTicks = 600;
        public boolean meleeDMGInfluence = true;
        public float meleeDMGInfluenceMultiplier = 0.75F;
        public boolean goodAreaSpeedInfluence = true;
        public float goodAreaSpeedInfluenceMultiplier = 1.0F;
        public float winnerDamageIncreaseMultiplier = 1.2F;
        public boolean strikeAttackHasDelay = true;
        public int strikeAttackDelayTicks = 10;

        private Config sanitize() {
            meterSpeedPerTick = finiteClamp(meterSpeedPerTick, 0.0001F, 1F, 0.01F);
            goodAreaLow = finiteClamp(goodAreaLow, 0F, 1F, 0.78F);
            goodAreaHigh = finiteClamp(goodAreaHigh, goodAreaLow, 1F, 0.96F);
            momentumGainDefaultMultiplier = finiteClamp(momentumGainDefaultMultiplier, 0F, Float.MAX_VALUE, 1F);
            offWindowMomentumEfficiency = finiteClamp(offWindowMomentumEfficiency, 0F, 1F, 0.18F);
            momentumDecayPerTick = finiteClamp(momentumDecayPerTick, 0F, 1F, 0.96F);
            innerAdvantageLow = finiteClamp(innerAdvantageLow, 0F, 0.5F, 0.20F);
            innerAdvantageHigh = finiteClamp(innerAdvantageHigh, 0.5F, 1F, 0.80F);
            maxClashDurationTicks = Math.max(20, maxClashDurationTicks);
            meleeDMGInfluenceMultiplier = finiteClamp(meleeDMGInfluenceMultiplier, 0F, Float.MAX_VALUE, 0.75F);
            goodAreaSpeedInfluenceMultiplier = finiteClamp(goodAreaSpeedInfluenceMultiplier, 0F, Float.MAX_VALUE, 1F);
            winnerDamageIncreaseMultiplier = finiteClamp(winnerDamageIncreaseMultiplier, 0F, Float.MAX_VALUE, 1.2F);
            strikeAttackDelayTicks = Math.max(0, strikeAttackDelayTicks);
            return this;
        }
    }

    private static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
