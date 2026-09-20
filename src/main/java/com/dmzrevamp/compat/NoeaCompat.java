package com.dmzrevamp.compat;

import com.dmzrevamp.revamp.battlepower.CustomBattlePowerCalculator;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.IdentityHashMap;
import java.util.Map;

/** Optional, reflection-isolated bridge for NoeaBosses. */
public final class NoeaCompat {
    public static final String MOD_ID = "noeabosses";
    private static volatile Reflection reflection;
    private static final ThreadLocal<Map<StatsData, String>> REAL_FUSION_RACES = new ThreadLocal<>();

    private NoeaCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isLoadedEarly() {
        try {
            return FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(MOD_ID) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Calculates only Overhaul's stable base. No Noea dynamic state is cached here. */
    public static double calculateBaseBattlePower(StatsData data) {
        return isLoaded()
                ? CustomBattlePowerCalculator.calculateFinitePlayerBattlePower(data)
                : CustomBattlePowerCalculator.calculatePlayerBattlePower(data);
    }

    /** Applies only Noea's permitted fusion override. Noea's RETURN hook applies Divine scaling once. */
    public static double applyDynamicBattlePower(StatsData data, double overhaulBase) {
        if (!isLoaded() || data == null || data.getPlayer() == null) {
            return overhaulBase;
        }
        try {
            Reflection api = reflection();
            Player player = data.getPlayer();
            Object noeaData = api.getOrMigrate.invoke(null, player);
            double base = overhaulBase;
            if (noeaOwnsFusionBattlePower(data, noeaData, api)) {
                long override = api.fusionBattlePowerOverride.getLong(noeaData);
                if (override > 0L) {
                    base = override;
                }
            }
            return base;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return overhaulBase;
        }
    }

    /**
     * DMZ Metamoru/Potara between two live players, including the Arm Band race bridge,
     * remains an Overhaul stat fusion. NPC and Noea-special fusions remain Noea-owned.
     */
    public static boolean overhaulOwnsStandardPlayerFusion(StatsData first, StatsData second) {
        if (!isLoaded() || first == null || second == null) {
            return true;
        }
        if (!(first.getPlayer() instanceof ServerPlayer) || !(second.getPlayer() instanceof ServerPlayer)) {
            return false;
        }
        try {
            Reflection api = reflection();
            Object noeaData = api.getOrMigrate.invoke(null, first.getPlayer());
            String method = string(api.fusionMethod.get(noeaData));
            return !"five_way".equals(method);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    public static void captureRealFusionRaces(StatsData first, StatsData second) {
        Map<StatsData, String> races = new IdentityHashMap<>();
        if (first != null) races.put(first, first.getCharacter().getRaceName());
        if (second != null) races.put(second, second.getCharacter().getRaceName());
        REAL_FUSION_RACES.set(races);
    }

    public static void clearRealFusionRaces() {
        REAL_FUSION_RACES.remove();
    }

    public static String realFusionRace(StatsData data) {
        Map<StatsData, String> races = REAL_FUSION_RACES.get();
        String race = races == null ? null : races.get(data);
        return race != null ? race : data.getCharacter().getRaceName();
    }

    public static Object findAliasedGodKiVariant(String race, String group, String form) {
        if (!isLoaded()) return null;
        String[] alias = aliasedGodKiVariantIds(race, group, form);
        if (alias == null) return null;
        try {
            return reflection().godKiVariantFind.invoke(null, alias[0], alias[1], alias[2]);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    /** Returns the canonical Noea ledger identity for an Overhaul form, without touching configs. */
    public static String[] aliasedGodKiVariantIds(String race, String group, String form) {
        String normalizedRace = string(race);
        String normalizedGroup = string(group);
        String normalizedForm = string(form);
        String aliasGroup = normalizedGroup;
        String alias = null;
        if ("saiyan".equals(normalizedRace) && "supersaiyan".equals(normalizedGroup)
                && "supersaiyan".equals(normalizedForm)) {
            alias = "supersaiyanmastered";
        } else if ("namekian".equals(normalizedRace) && "superforms".equals(normalizedGroup)
                && ("supernamekian2".equals(normalizedForm) || "ultranamekian".equals(normalizedForm))) {
            alias = "supernamekian";
        } else if ("namekian".equals(normalizedRace) && "giant".equals(normalizedGroup)
                && "giant".equals(normalizedForm)) {
            aliasGroup = "superform";
            alias = "giant";
        } else if ("human".equals(normalizedRace) && "androidforms".equals(normalizedGroup)
                && "superfusedandroid".equals(normalizedForm)) {
            alias = "fusedandroid";
        } else if ("frostdemon".equals(normalizedRace) && "evolutionforms".equals(normalizedGroup)
                && "fifthfp".equals(normalizedForm)) {
            alias = "fifth";
        }
        return alias == null ? null : new String[]{normalizedRace, aliasGroup, alias};
    }

    private static boolean noeaOwnsFusionBattlePower(StatsData data, Object noeaData, Reflection api)
            throws IllegalAccessException {
        if (api.fusionBattlePowerOverride.getLong(noeaData) <= 0L) {
            return false;
        }
        String method = string(api.fusionMethod.get(noeaData));
        String mobType = string(api.fusionMobEntityType.get(noeaData));
        if (!mobType.isBlank() || "five_way".equals(method)) {
            return true;
        }

        UUID partnerId = data.getStatus().getFusionPartnerUUID();
        if (data.getPlayer() instanceof ServerPlayer serverPlayer && partnerId != null) {
            ServerPlayer partner = serverPlayer.server.getPlayerList().getPlayer(partnerId);
            if (partner != null && (method.isBlank() || "dance".equals(method)
                    || "potara".equals(method) || "arm_band".equals(method))) {
                return false;
            }
        }
        // A positive override without a live player partner belongs to an NPC/master/special fusion.
        return true;
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }

    private static Reflection reflection() throws ReflectiveOperationException {
        Reflection current = reflection;
        if (current == null) {
            synchronized (NoeaCompat.class) {
                current = reflection;
                if (current == null) {
                    reflection = current = new Reflection();
                }
            }
        }
        return current;
    }

    private static final class Reflection {
        private final Method getOrMigrate;
        private final Field fusionBattlePowerOverride;
        private final Field fusionMethod;
        private final Field fusionMobEntityType;
        private final Method godKiVariantFind;

        private Reflection() throws ReflectiveOperationException {
            ClassLoader loader = NoeaCompat.class.getClassLoader();
            Class<?> dataClass = Class.forName("com.butterjaffa.noeabosses.V090Data", false, loader);
            Class<?> ledgerClass = Class.forName("com.butterjaffa.noeabosses.GodKiVariantLedger", false, loader);
            getOrMigrate = dataClass.getMethod("getOrMigrate", Player.class);
            fusionBattlePowerOverride = dataClass.getField("fusionBattlePowerOverride");
            fusionMethod = dataClass.getField("fusionMethod");
            fusionMobEntityType = dataClass.getField("fusionMobEntityType");
            godKiVariantFind = ledgerClass.getMethod("find", String.class, String.class, String.class);
        }
    }
}
