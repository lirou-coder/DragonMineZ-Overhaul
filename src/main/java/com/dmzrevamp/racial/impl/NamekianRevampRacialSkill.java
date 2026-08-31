package com.dmzrevamp.racial.impl;

import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.config.racial.NamekianRevampRacialConfig;
import com.dmzrevamp.racial.CustomRacialSkill;
import com.dmzrevamp.racial.PermanentRacialBonusHelper;
import com.dmzrevamp.racial.PersistentRacialCooldown;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.QuestEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;

public class NamekianRevampRacialSkill implements CustomRacialSkill {
    public static final String BONUS_KEY = "Assimilation";
    public static final String USES_TAG = "dmzrevamp_namekian_assimilation_uses";
    public static final String COOLDOWN_KEY = "DmzRevampNamekianAssimilation";
    public static final String LAST_USE_TAG = "dmzrevamp_namekian_assimilation_last_use_epoch_ms";

    @Override public String id() { return "namekianrevamp"; }
    @Override public String cooldownKey() { return COOLDOWN_KEY; }

    @Override
    public boolean showsRacialActionButton(StatsData data) {
        return ConfigManager.getServerConfig().getRacialSkills().getEnableRacialSkills()
                && DmzRevampRacialConfigs.namekianRevamp().enabled;
    }

    @Override
    public Integer getActionCharge(ServerPlayer player, StatsData data) {
        return Math.max(0, DmzRevampRacialConfigs.namekianRevamp().assimilationChargeTicks);
    }

    @Override
    public Boolean performAction(ServerPlayer player, StatsData data) {
        assimilate(player, data);
        return true;
    }

    @Override @OnlyIn(Dist.CLIENT)
    public Component getSkillTitle() { return Component.literal("Namekian Assimilation"); }

    @Override @OnlyIn(Dist.CLIENT)
    public Component getSkillDescription(StatsData data) {
        NamekianRevampRacialConfig config = DmzRevampRacialConfigs.namekianRevamp();
        return Component.translatable("skill.dragonminez.racial_namekianrevamp.desc",
                Math.round(config.healthRegenRatio * 100D), Math.round(config.statBoostRatio * 100D));
    }

    @Override @OnlyIn(Dist.CLIENT)
    public Component getRaceSelectionDescription() { return getSkillDescription(null); }

    public static void assimilate(ServerPlayer player, StatsData data) {
        NamekianRevampRacialConfig config = DmzRevampRacialConfigs.namekianRevamp();
        if (!config.enabled || !ConfigManager.getServerConfig().getRacialSkills().getEnableRacialSkills()) return;
        if (PersistentRacialCooldown.isActive(player, data, COOLDOWN_KEY, LAST_USE_TAG, config.cooldownSeconds)) return;

        int previousUses = player.getPersistentData().getInt(USES_TAG);
        double decayPerUse = getDecayPerUse(config);
        double efficiency = Math.max(0D, 1D - previousUses * decayPerUse);
        if (efficiency <= 0D) {
            player.displayClientMessage(Component.translatable("message.dragonminez.racial.limit_reached"), true);
            return;
        }

        LivingEntity target = findTarget(player, 3D);
        if (!isValidTarget(target, config) || target instanceof MastersEntity || target instanceof PunchMachineEntity) {
            player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.invalid_target"), true);
            return;
        }
        if (TargetHelper.getRelation(player, target) == TargetHelper.Relation.FRIENDLY) return;
        if (!player.isCreative() && !canOverpower(data, target)) {
            player.displayClientMessage(Component.translatable("message.dragonminez.racial.target_too_strong"), true);
            return;
        }

        boolean changed = false;
        double gainRatio = Math.max(0D, config.statBoostRatio) * efficiency;
        for (String configuredStat : safeStats(config.boostedStats)) {
            String stat = configuredStat == null ? "" : configuredStat.trim().toUpperCase(Locale.ROOT);
            int current = PermanentRacialBonusHelper.getBaseStatValueForRacialBonusCap(data, stat);
            changed |= PermanentRacialBonusHelper.addOrAccumulateBaseCappedStat(
                    data, stat, BONUS_KEY, current * gainRatio,
                    getEffectiveMaxBonusBaseStatRatio(config), true);
        }
        if (!changed) return;

        if (config.healthRegenRatio > 0D) player.heal((float) (player.getMaxHealth() * config.healthRegenRatio));
        QuestEvents.creditQuestKill(player, target);
        target.discard();
        player.getPersistentData().putInt(USES_TAG, previousUses + 1);
        data.getResources().addRacialSkillCount(1);
        PersistentRacialCooldown.markUsed(player, data, COOLDOWN_KEY, LAST_USE_TAG, config.cooldownSeconds);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.success"), true);
    }

    public static void resetAssimilation(ServerPlayer player, StatsData data) {
        for (String stat : new String[]{"STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE"}) {
            data.getBonusStats().removeBonus(stat, BONUS_KEY);
        }
        player.getPersistentData().putInt(USES_TAG, 0);
        data.getResources().setRacialSkillCount(0);
        PersistentRacialCooldown.clear(player, data, COOLDOWN_KEY, LAST_USE_TAG);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    private static boolean isValidTarget(LivingEntity target, NamekianRevampRacialConfig config) {
        if (target instanceof ServerPlayer player) {
            return config.allowNamekianPlayers && StatsProvider.get(StatsCapability.INSTANCE, player)
                    .map(d -> "namekian".equalsIgnoreCase(d.getCharacter().getRaceName())).orElse(false);
        }
        return config.allowNamekianNpcs && (target instanceof NamekWarriorEntity
                || target instanceof NamekTraderEntity
                || target != null && target.getName().getString().contains("Piccolo") && !(target instanceof MastersEntity));
    }

    private static boolean canOverpower(StatsData data, LivingEntity target) {
        double maxDamage = Math.max(data.getMaxMeleeDamage(), Math.max(data.getMaxStrikeDamage(), data.getMaxKiDamage()));
        if (target.getHealth() > maxDamage) return false;
        if (target instanceof ServerPlayer player) {
            return StatsProvider.get(StatsCapability.INSTANCE, player).map(other -> other.getLevel() < data.getLevel()).orElse(false);
        }
        return true;
    }

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1D);
        for (Entity entity : player.level().getEntities(player, search,
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isAlive())) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(start) || box.clip(start, end).isPresent()) return (LivingEntity) entity;
        }
        return null;
    }

    private static List<String> safeStats(List<String> stats) { return stats == null ? List.of() : stats; }

    private static double getDecayPerUse(NamekianRevampRacialConfig config) {
        return Double.isFinite(config.effectDecayPerUse) ? Math.max(0D, config.effectDecayPerUse) : 0D;
    }

    private static double getEffectiveMaxBonusBaseStatRatio(NamekianRevampRacialConfig config) {
        double configured = Double.isFinite(config.maxBonusCurrentStatRatio)
                ? Math.max(0D, config.maxBonusCurrentStatRatio) : 1D;
        double decay = getDecayPerUse(config);
        return decay <= 0D ? Double.POSITIVE_INFINITY : configured / decay;
    }
}
