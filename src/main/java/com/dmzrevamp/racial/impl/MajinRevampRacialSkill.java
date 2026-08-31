package com.dmzrevamp.racial.impl;

import com.dmzrevamp.racial.CustomRacialSkill;
import com.dmzrevamp.config.racial.MajinRevampRacialConfig;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.racial.PermanentRacialBonusHelper;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;

public class MajinRevampRacialSkill implements CustomRacialSkill {
    public static final String ABSORPTION_BONUS_KEY = "Absorption";
    public static final String ABSORPTION_USES_TAG = "dmzrevamp_absorption_uses";

    @Override
    public String id() {
        return "majinrevamp";
    }

    @Override
    public boolean showsRacialActionButton(StatsData data) {
        GeneralServerConfig.RacialSkillsConfig dmzConfig = ConfigManager.getServerConfig().getRacialSkills();
        return dmzConfig.getEnableRacialSkills() && DmzRevampRacialConfigs.majinRevamp().enabled;
    }

    @Override
    public Integer getActionCharge(ServerPlayer player, StatsData data) {
        return Math.max(0, DmzRevampRacialConfigs.majinRevamp().absorptionChargeTicks);
    }

    @Override
    public Boolean performAction(ServerPlayer player, StatsData data) {
        performAbsorption(player, data);
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getSkillTitle() {
        return Component.literal("Majin Absorption");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getSkillDescription(StatsData data) {
        MajinRevampRacialConfig config = DmzRevampRacialConfigs.majinRevamp();
        return Component.translatable("skill.dragonminez.racial_majinrevamp.desc",
                percent(config.healthRegenRatio),
                percent(config.statCopyRatio));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getRaceSelectionDescription() {
        return getSkillDescription(null);
    }

    public static void performAbsorption(ServerPlayer player, StatsData data) {
        GeneralServerConfig.RacialSkillsConfig dmzConfig = ConfigManager.getServerConfig().getRacialSkills();
        MajinRevampRacialConfig config = DmzRevampRacialConfigs.majinRevamp();
        if (!dmzConfig.getEnableRacialSkills() || !config.enabled) {
            return;
        }

        LivingEntity target = getTargetEntity(player, 3D);
        if (target == null || target instanceof MastersEntity || target instanceof PunchMachineEntity) {
            return;
        }
        if (TargetHelper.getRelation(player, target) == TargetHelper.Relation.FRIENDLY) {
            return;
        }
        if (!canAbsorbTarget(player, data, target, config) && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.dragonminez.racial.target_too_strong"), true);
            return;
        }

        int previousUses = player.getPersistentData().getInt(ABSORPTION_USES_TAG);
        double efficiency = Math.max(0D, 1D - (previousUses * getDecayPerUse(config)));
        if (efficiency <= 0D) {
            return;
        }

        boolean changedStats = false;
        double statCopy = config.statCopyRatio * efficiency;
        if (target instanceof ServerPlayer targetPlayer && config.allowPlayerAbsorption) {
            boolean[] changed = new boolean[]{false};
            StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> changed[0] = applyPlayerAbsorption(data, targetData, statCopy, config));
            changedStats = changed[0];
        } else if (target instanceof Mob && config.allowMobAbsorption) {
            int increase = (int) Math.max(1D, target.getMaxHealth() * statCopy);
            for (String stat : boostedStats(config)) {
                changedStats |= addAbsorptionBonus(data, stat, increase, getEffectiveMaxBonusBaseStatRatio(config));
            }
        }

        if (!changedStats) {
            return;
        }

        double healthRegen = config.healthRegenRatio;
        if (healthRegen > 0D) {
            player.heal((float) (player.getMaxHealth() * healthRegen));
        }
        killAbsorbedTarget(player, target);
        data.getResources().addRacialSkillCount(1);
        player.getPersistentData().putInt(ABSORPTION_USES_TAG, previousUses + 1);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        player.displayClientMessage(Component.translatable("message.dragonminez.racial.majin.success"), true);
    }

    private static void killAbsorbedTarget(ServerPlayer player, LivingEntity target) {
        target.invulnerableTime = 0;
        // A player damage source lets DMZ's normal death handler award kill TP and quest kill progress.
        target.hurt(player.damageSources().playerAttack(player), Math.max(Float.MAX_VALUE / 4F, target.getMaxHealth() * 100F));
    }

    public static void resetAbsorption(ServerPlayer player, StatsData data) {
        boolean changed = false;
        for (String stat : new String[]{"STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE"}) {
            double existing = getExistingAbsorptionBonus(data, stat);
            if (existing <= 0D) {
                continue;
            }
            data.getBonusStats().removeBonus(stat, ABSORPTION_BONUS_KEY);
            changed = true;
        }
        player.getPersistentData().putInt(ABSORPTION_USES_TAG, 0);
        data.getResources().setRacialSkillCount(0);
        if (changed) {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    private static boolean applyPlayerAbsorption(StatsData data, StatsData targetData, double statCopy, MajinRevampRacialConfig config) {
        boolean changed = false;
        for (String stat : boostedStats(config)) {
            int value = getStatValue(targetData, stat);
            int increase = (int) Math.max(1D, value * statCopy);
            changed |= addAbsorptionBonus(data, stat, increase, getEffectiveMaxBonusBaseStatRatio(config));
        }
        return changed;
    }

    private static double getEffectiveMaxBonusBaseStatRatio(MajinRevampRacialConfig config) {
        double capRatio = config.maxBonusBaseStatRatio;
        double safeBaseCapRatio = Double.isFinite(capRatio) ? Math.max(0D, capRatio) : 1.0D;
        double decayPerUse = getDecayPerUse(config);
        return decayPerUse <= 0D ? Double.POSITIVE_INFINITY : safeBaseCapRatio / decayPerUse;
    }

    private static double getDecayPerUse(MajinRevampRacialConfig config) {
        double decay = config.effectDecayPerUse;
        return Double.isFinite(decay) ? Math.max(0D, decay) : 0D;
    }

    private static boolean addAbsorptionBonus(StatsData data, String stat, int increase, double capRatio) {
        String normalized = normalizeStat(stat);
        if (normalized.isEmpty() || increase <= 0) {
            return false;
        }
        return PermanentRacialBonusHelper.addOrAccumulateBaseCappedStat(
                data,
                normalized,
                ABSORPTION_BONUS_KEY,
                increase,
                capRatio,
                true
        );
    }

    private static double getExistingAbsorptionBonus(StatsData data, String stat) {
        return data.getBonusStats().getBonuses(stat).stream()
                .filter(bonus -> ABSORPTION_BONUS_KEY.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value)
                .sum();
    }

    private static LivingEntity getTargetEntity(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1D);
        for (Entity entity : player.level().getEntities(player, searchBox, entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isAlive())) {
            AABB targetBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (targetBox.contains(start) || targetBox.clip(start, end).isPresent()) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }

    private static boolean canAbsorbTarget(ServerPlayer player, StatsData data, LivingEntity target, MajinRevampRacialConfig config) {
        if (target instanceof ServerPlayer && !config.allowPlayerAbsorption) {
            return false;
        }
        if (target instanceof Mob && !config.allowMobAbsorption) {
            return false;
        }
        double threshold = Double.isFinite(config.targetCurrentHealthDamageThreshold) && config.targetCurrentHealthDamageThreshold > 0D
                ? config.targetCurrentHealthDamageThreshold
                : 1.0D;
        double requiredDamage = target.getHealth() * threshold;
        double maxAbsorptionDamage = Math.max(data.getMaxMeleeDamage(), data.getMaxKiDamage());
        return requiredDamage < maxAbsorptionDamage;
    }

    private static List<String> boostedStats(MajinRevampRacialConfig config) {
        return config.boostedStats == null ? List.of() : config.boostedStats;
    }

    private static int getStatValue(StatsData data, String stat) {
        return switch (normalizeStat(stat)) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "RES" -> data.getStats().getResistance();
            case "DEF", "STM" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0;
        };
    }

    private static String normalizeStat(String stat) {
        if (stat == null) {
            return "";
        }
        String normalized = stat.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE" -> normalized;
            default -> "";
        };
    }

    private static int percent(double value) {
        return (int) Math.round(value * 100D);
    }
}
