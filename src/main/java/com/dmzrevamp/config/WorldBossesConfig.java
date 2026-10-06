package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import net.minecraft.world.phys.AABB;

/** Per-world-boss balance files. Values are deliberately data driven so users can edit them. */
public final class WorldBossesConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("worldbosses");
    private static final Map<String, Boss> BOSSES = new LinkedHashMap<>();
    private WorldBossesConfig() {}

    public static synchronized void initialize() { reload(); }
    public static synchronized void reload() {
        BOSSES.clear();
        for (String key : new String[]{"janemba", "turles", "metal_cooler_core", "gomah", "tamagami_1", "tamagami_2", "tamagami_3"}) {
            try {
                Path path = ROOT.resolve(key + ".json");
                if (!Files.exists(path)) {
                    Files.createDirectories(ROOT);
                    Files.writeString(path, GSON.toJson(defaultBoss(key)));
                }
                Boss boss = GSON.fromJson(Files.readString(path), Boss.class);
                if (boss == null) boss = defaultBoss(key);
                boss.sanitize(defaultBoss(key));
                Files.writeString(path, GSON.toJson(boss));
                BOSSES.put(key, boss);
            } catch (Exception e) {
                LOGGER.warn("Could not load world boss config {}: {}", key, e.getMessage());
                BOSSES.put(key, defaultBoss(key));
            }
        }
    }
    public static Boss get(String key) { return BOSSES.getOrDefault(key, defaultBoss(key)); }

    public static void markBossesForReconfiguration(MinecraftServer server) {
        if (server == null) return;
        for (var level : server.getAllLevels()) {
            for (WorldBossEntity boss : level.getEntitiesOfClass(WorldBossEntity.class,
                    new AABB(-30_000_000, -30_000_000, -30_000_000, 30_000_000, 30_000_000, 30_000_000))) {
                boss.getPersistentData().remove("dmzrevamp_worldboss_configured");
            }
        }
    }

    public static final class Boss {
        @SerializedName(value = "Enabled", alternate = {"enabled"})
        public boolean enabled = true;
        @SerializedName(value = "ScaleWithMorePlayers", alternate = {"scaleWithMorePlayers"})
        public boolean scaleWithMorePlayers = true;
        @SerializedName(value = "HpIncreasePerPlayer", alternate = {"hpIncreasePerPlayer"})
        public double hpIncreasePerPlayer = 1.0;
        @SerializedName(value = "DamageIncreasePerPlayer", alternate = {"damageIncreasePerPlayer"})
        public double damageIncreasePerPlayer = 0.2;
        @SerializedName(value = "DefenseIncreasePerPlayer", alternate = {"defenseIncreasePerPlayer"})
        public double defenseIncreasePerPlayer = 0.2;
        @SerializedName(value = "MinionsStatShare", alternate = {"minionsStatShare"})
        public double minionsStatShare = 0.1;
        @SerializedName(value = "BossStats", alternate = {"bossStats"})
        public Map<String, Stats> bossStats = new LinkedHashMap<>();
        void sanitize(Boss fallback) {
            if (!Double.isFinite(hpIncreasePerPlayer) || hpIncreasePerPlayer < 0) hpIncreasePerPlayer = fallback.hpIncreasePerPlayer;
            if (!Double.isFinite(damageIncreasePerPlayer) || damageIncreasePerPlayer < 0) damageIncreasePerPlayer = fallback.damageIncreasePerPlayer;
            if (!Double.isFinite(defenseIncreasePerPlayer) || defenseIncreasePerPlayer < 0) defenseIncreasePerPlayer = fallback.defenseIncreasePerPlayer;
            if (!Double.isFinite(minionsStatShare) || minionsStatShare < 0) minionsStatShare = fallback.minionsStatShare;
            if (bossStats == null || bossStats.isEmpty()) bossStats = fallback.bossStats;
            bossStats.values().forEach(s -> { if (s != null) s.sanitize(); });
        }
    }
    public static final class Stats {
        @SerializedName(value = "HP", alternate = {"hp"}) public double hp;
        @SerializedName(value = "MeleeDamage", alternate = {"meleeDamage"}) public double meleeDamage;
        @SerializedName(value = "KiDamage", alternate = {"kiDamage"}) public double kiDamage;
        @SerializedName(value = "Defense", alternate = {"defense"}) public double defense;
        @SerializedName(value = "MovementSpeed", alternate = {"movementSpeed"}) public double movementSpeed;
        Stats(double hp, double melee, double ki, double defense, double speed) { this.hp=hp; meleeDamage=melee; kiDamage=ki; this.defense=defense; movementSpeed=speed; }
        public Stats() {}
        void sanitize() { hp=Math.max(1,hp); meleeDamage=Math.max(0,meleeDamage); kiDamage=Math.max(0,kiDamage); defense=Math.max(0,defense); movementSpeed=Math.max(0,movementSpeed); }
    }
    private static Boss defaultBoss(String key) {
        Boss b = new Boss();
        switch (key) {
            case "janemba" -> {
                b.bossStats.put("base", new Stats(513000,51300,128000,76900,0.599));
                b.bossStats.put("transformed", new Stats(500000,161000,250000,145000,0.6));
            }
            case "turles" -> b.bossStats.put("base", new Stats(2870,1050,1440,861,0.276));
            case "metal_cooler_core" -> b.bossStats.put("base", new Stats(97900,8710,30500,20700,0.482));
            case "gomah" -> b.bossStats.put("base", new Stats(557000,111000,188000,118000,0.46));
            case "tamagami_1" -> { b.bossStats.put("base", new Stats(197000,75900,50600,56200,0.5)); b.bossStats.put("powered", new Stats(207000,79200,54800,67000,0.52)); }
            case "tamagami_2" -> { b.bossStats.put("base", new Stats(161000,56800,47300,52000,0.5)); b.bossStats.put("powered", new Stats(171000,64200,56200,61500,0.52)); }
            case "tamagami_3" -> { b.bossStats.put("base", new Stats(122000,45600,41800,41800,0.48)); b.bossStats.put("powered", new Stats(138000,50600,62100,48300,0.5)); }
        }
        return b;
    }
}
