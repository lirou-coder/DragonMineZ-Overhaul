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
        /** Higher values make each seeded sweep cross the meter faster. */
        public float meterSpeedMultiplier = 1.0F;
        /** Base multiplier for DMZ 2.2's randomized 6.5%-9% good-area half width. */
        public float goodAreaSizeMultiplier = 1.0F;
        public float perfectAreaFraction = 0.35F;
        public float goodMinimumEfficiency = 0.5F;
        public float momentumGainDefaultMultiplier = 1.0F;
        public float momentumDecayPerTick = 0.96F;
        public float innerAdvantageLow = 0.20F;
        public float innerAdvantageHigh = 0.80F;
        public int maxClashDurationTicks = 600;
        public boolean meleeDMGInfluence = true;
        public float meleeDMGInfluenceMultiplier = 0.75F;
        public boolean goodAreaSpeedInfluence = true;
        /** Weight applied to the faster participant's Speed ratio when widening its good area. */
        public float goodAreaSpeedInfluenceMultiplier = 1.0F;
        public float winnerDamageIncreaseMultiplier = 1.2F;
        public boolean strikeAttackHasDelay = true;
        public int strikeAttackDelayTicks = 10;

        private Config sanitize() {
            meterSpeedMultiplier = finiteClamp(meterSpeedMultiplier, 0.05F, 8F, 1F);
            goodAreaSizeMultiplier = finiteClamp(goodAreaSizeMultiplier, 0.01F, 20F, 1F);
            perfectAreaFraction = finiteClamp(perfectAreaFraction, 0F, 1F, 0.35F);
            goodMinimumEfficiency = finiteClamp(goodMinimumEfficiency, 0F, 1F, 0.5F);
            momentumGainDefaultMultiplier = finiteClamp(momentumGainDefaultMultiplier, 0F, Float.MAX_VALUE, 1F);
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
