package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class KiClashConfigured {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("KiClashConfigured.json");
    private static Config cached = new Config();

    private KiClashConfigured() {}

    public static synchronized void initialize() { reload(); }
    public static synchronized void reload() { loadFromDisk(); }
    public static Config get() { return cached; }

    public static boolean allows(String type) {
        if (type == null) return false;
        String normalized = normalize(type);
        return get().allowedKiAttacks.stream().map(KiClashConfigured::normalize).anyMatch(normalized::equals);
    }

    private static String normalize(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "BIG_BALL", "GIANTBALL" -> "GIANT_BALL";
            case "MEDIUMBALL" -> "MEDIUM_BALL";
            case "SMALLBALL" -> "SMALL_BALL";
            case "SPIRAL", "BEAM" -> "BEAM";
            default -> normalized;
        };
    }

    private static synchronized void loadFromDisk() {
        try {
            if (!Files.exists(PATH)) {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(new Config()));
            }
            Config loaded = GSON.fromJson(Files.readString(PATH), Config.class);
            cached = (loaded == null ? new Config() : loaded).sanitize();
            Files.writeString(PATH, GSON.toJson(cached));
        } catch (Exception exception) {
            LOGGER.warn("Could not load KiClashConfigured.json: {}", exception.getMessage());
            cached = new Config();
        }
    }

    public static final class Config {
        public float meterSpeedPerTick = 0.005F;
        public float goodAreaLow = 0.78F;
        public float goodAreaHigh = 0.96F;
        public float momentumGainDefaultMultiplier = 1.0F;
        public float offWindowMomentumEfficiency = 0.18F;
        public float momentumDecayPerTick = 0.96F;
        public float innerAdvantageLow = 0.20F;
        public float innerAdvantageHigh = 0.80F;
        public int maxClashDurationTicks = 600;
        public List<String> allowedKiAttacks = new ArrayList<>(List.of("Medium_Ball", "Giant_Ball", "Wave", "Laser", "Beam"));
        public boolean KiDMGInfluence = true;
        public float KiDMGInfluenceMultiplier = 0.75F;
        public boolean overchargeInfluence = true;
        public float overchargeInfluenceMultiplier = 0.75F;
        public boolean allowTransformationMidClash = true;
        public boolean cancelIffTooStrong = true;
        public float cancelMulti = 20.0F;
        public boolean AllowHelpers = true;
        public float helpersSizeIncreaseMultiplier = 0.75F;
        public float MomentumLossReducePerHelper = 0.10F;
        public float MaxMomentumLossPerHelper = 0.60F;

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
            KiDMGInfluenceMultiplier = finiteClamp(KiDMGInfluenceMultiplier, 0F, Float.MAX_VALUE, 0.75F);
            overchargeInfluenceMultiplier = finiteClamp(overchargeInfluenceMultiplier, 0F, Float.MAX_VALUE, 0.75F);
            helpersSizeIncreaseMultiplier = finiteClamp(helpersSizeIncreaseMultiplier, 0F, Float.MAX_VALUE, 0.75F);
            MomentumLossReducePerHelper = finiteClamp(MomentumLossReducePerHelper, 0F, 1F, 0.10F);
            MaxMomentumLossPerHelper = finiteClamp(MaxMomentumLossPerHelper, 0F, 1F, 0.60F);
            cancelMulti = finiteClamp(cancelMulti, 1F, Float.MAX_VALUE, 20F);
            Set<String> unique = new LinkedHashSet<>();
            if (allowedKiAttacks != null) allowedKiAttacks.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(KiClashConfigured::canonicalConfigName)
                    .forEach(unique::add);
            allowedKiAttacks = new ArrayList<>(unique);
            return this;
        }
    }

    private static String canonicalConfigName(String value) {
        return switch (normalize(value)) {
            case "SMALL_BALL" -> "Small_Ball";
            case "MEDIUM_BALL" -> "Medium_Ball";
            case "GIANT_BALL" -> "Giant_Ball";
            case "WAVE" -> "Wave";
            case "LASER" -> "Laser";
            case "BEAM" -> "Beam";
            case "DISK" -> "Disk";
            case "BARRAGE" -> "Barrage";
            case "SHIELD" -> "Shield";
            case "AREA" -> "Area";
            default -> value.trim();
        };
    }

    private static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
