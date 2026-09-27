package com.dmzrevamp.racial.impl;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dmzrevamp.racial.PermanentRacialBonusHelper;
import com.dmzrevamp.racial.PersistentRacialCooldown;
import com.dmzrevamp.effect.DmzRevampEffectHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.MainEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaiyanRpgZenkaiEvents {
    private static final String ZENKAI_BONUS_KEY = "Zenkai";
    private static final String PENDING_STACKS_TAG = "dmzrevamp_zenkai_pending_stacks";
    private static final String PENDING_LETHAL_STACKS_TAG = "dmzrevamp_zenkai_pending_lethal_stacks";
    private static final String STACK_NOTICE_TAG = "dmzrevamp_zenkai_stack_notice";
    private static final String LETHAL_NOTICE_TAG = "dmzrevamp_zenkai_lethal_notice";
    private static final String DAMAGE_EVAL_TAG = "dmzrevamp_zenkai_damage_eval";
    private static final String DAMAGE_BEFORE_HP_TAG = "dmzrevamp_zenkai_before_hp";
    private static final String DAMAGE_AMOUNT_TAG = "dmzrevamp_zenkai_final_damage";
    private static final String DAMAGE_FRIENDLY_TAG = "dmzrevamp_zenkai_friendly_damage";
    public static final String ZENKAI_USES_TAG = "dmzrevamp_zenkai_uses";
    public static final String LAST_ZENKAI_USE_TAG = "dmzrevamp_zenkai_last_use_epoch_ms";

    private SaiyanRpgZenkaiEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0F) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!isZenkaiRacial(data) || isZenkaiOnCooldown(player, data)) {
                return;
            }

            Entity attacker = resolveAttacker(event.getSource());
            boolean shadowDummyAttack = attacker instanceof ShadowDummyEntity;
            if (shadowDummyAttack && !DmzRevampRacialConfigs.saiyanRpg().shadowDummyGiveZenkai) {
                return;
            }
            boolean friendlyFist = shadowDummyAttack
                    ? DmzRevampRacialConfigs.saiyanRpg().shadowDummyFriendlyFist
                    : isFriendlyFistAttack(attacker);
            // Evaluate on the following player tick, after every LivingDamageEvent listener and
            // the actual health write. This prevents pre-mitigation damage from creating Zenkais.
            player.getPersistentData().putBoolean(DAMAGE_EVAL_TAG, true);
            player.getPersistentData().putFloat(DAMAGE_BEFORE_HP_TAG, player.getHealth());
            player.getPersistentData().putFloat(DAMAGE_AMOUNT_TAG, event.getAmount());
            player.getPersistentData().putBoolean(DAMAGE_FRIENDLY_TAG, friendlyFist);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!isZenkaiRacial(data)) {
                clearTracking(player);
                return;
            }
            isZenkaiOnCooldown(player, data); // Rebuild DMZ's ticking cooldown from the persisted timestamp when needed.
            evaluateFinalDamage(player, data);

            int lethalStacks = player.getPersistentData().getInt(PENDING_LETHAL_STACKS_TAG);
            if (lethalStacks > 0 && player.isAlive() && player.getHealth() > 0F) {
                player.getPersistentData().putInt(PENDING_STACKS_TAG, Math.max(player.getPersistentData().getInt(PENDING_STACKS_TAG), lethalStacks));
                player.getPersistentData().putInt(PENDING_LETHAL_STACKS_TAG, 0);
            }

            if (player.getHealth() + 0.0001F >= player.getMaxHealth()) {
                awardZenkai(player, data);
            }
        });
    }

    private static void evaluateFinalDamage(ServerPlayer player, StatsData data) {
        if (!player.getPersistentData().getBoolean(DAMAGE_EVAL_TAG)) return;
        player.getPersistentData().putBoolean(DAMAGE_EVAL_TAG, false);
        if (!player.isAlive() || isZenkaiOnCooldown(player, data)) return;

        float healthAfter = player.getHealth();
        int stacks = calculateStacks(data, healthAfter, player.getMaxHealth(),
                player.getPersistentData().getBoolean(DAMAGE_FRIENDLY_TAG));
        if (stacks <= 0) return;

        boolean friendly = player.getPersistentData().getBoolean(DAMAGE_FRIENDLY_TAG);
        float before = player.getPersistentData().getFloat(DAMAGE_BEFORE_HP_TAG);
        float finalDamage = player.getPersistentData().getFloat(DAMAGE_AMOUNT_TAG);
        boolean lethalSurvived = finalDamage >= before && healthAfter > 0F;
        if (lethalSurvived && !friendly) {
            stacks = Math.min(getDeathPreventionMaxStacks(data), stacks * 2);
            player.getPersistentData().putInt(PENDING_LETHAL_STACKS_TAG,
                    Math.max(player.getPersistentData().getInt(PENDING_LETHAL_STACKS_TAG), stacks));
            if (!player.getPersistentData().getBoolean(LETHAL_NOTICE_TAG)) {
                player.displayClientMessage(Component.translatable("message.dmzrevamp.zenkai.near_death")
                        .withStyle(ChatFormatting.RED), true);
                player.getPersistentData().putBoolean(LETHAL_NOTICE_TAG, true);
            }
        } else {
            player.getPersistentData().putInt(PENDING_STACKS_TAG,
                    Math.max(player.getPersistentData().getInt(PENDING_STACKS_TAG), stacks));
            if (!player.getPersistentData().getBoolean(STACK_NOTICE_TAG)) {
                String key = lethalSurvived && friendly
                        ? "message.dmzrevamp.zenkai.friendly_fist" : "message.dmzrevamp.zenkai.wounded";
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
                player.getPersistentData().putBoolean(STACK_NOTICE_TAG, true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearTracking(player);
        }
    }

    private static int calculateStacks(StatsData data, float healthAfterHit, float maxHealth, boolean friendlyFist) {
        double thresholdPercent = getActivationHealthThreshold(data) * 100D;
        double currentPercent = (Math.max(0F, healthAfterHit) / Math.max(0.0001F, maxHealth)) * 100D;
        if (currentPercent > thresholdPercent) {
            return 0;
        }

        int rawStacks = Math.max(1, (int) Math.ceil(thresholdPercent - currentPercent));
        int maxStacks = getMaxStacks(data);
        rawStacks = Math.max(0, Math.min(maxStacks, rawStacks));
        if (friendlyFist) {
            rawStacks = Math.max(1, (int) Math.floor(rawStacks * getFriendlyFistMultiplier(data)));
        }
        return rawStacks;
    }

    private static void awardZenkai(ServerPlayer player, StatsData data) {
        int stacks = player.getPersistentData().getInt(PENDING_STACKS_TAG);
        if (stacks <= 0) {
            return;
        }
        // Damage and healing may be processed by different events/ticks. Recheck atomically at award time.
        if (isZenkaiOnCooldown(player, data)) {
            clearTracking(player);
            return;
        }

        int previousUses = player.getPersistentData().getInt(ZENKAI_USES_TAG);
        int useLimit = getEffectiveUseLimit();
        if (useLimit > 0 && previousUses >= useLimit) {
            clearTracking(player);
            return;
        }
        double efficiency = Math.max(0D, 1D - (previousUses * getDecayPerUse()));
        if (efficiency <= 0D) {
            clearTracking(player);
            return;
        }

        double totalRatio = stacks * getStatBonusPerStack(data) * efficiency;
        if (isBioAndroid(data)) {
            totalRatio *= DmzRevampRacialConfigs.bioAndroid().effectMultiplier;
        }

        boolean changedStats = false;
        for (String statName : DmzRevampRacialConfigs.saiyanRpg().boostedStats) {
            String normalizedStat = statName.toUpperCase(Locale.ROOT);
            int rawStatValue = PermanentRacialBonusHelper.getBaseStatValueForRacialBonusCap(data, normalizedStat);
            int increase = (int) Math.floor(rawStatValue * totalRatio);
            if (increase <= 0) {
                continue;
            }
            if ("RES".equals(normalizedStat)) {
                changedStats |= addZenkaiBonus(data, "DEF", increase);
                changedStats |= addZenkaiBonus(data, "STM", increase);
                data.getBonusStats().removeBonus("RES", ZENKAI_BONUS_KEY);
            } else {
                changedStats |= addZenkaiBonus(data, normalizedStat, increase);
            }
        }

        if (!changedStats) {
            clearTracking(player);
            return;
        }

        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        player.getPersistentData().putInt(ZENKAI_USES_TAG, previousUses + 1);
        PersistentRacialCooldown.markUsed(player, data, SaiyanRpgZenkaiRacialSkill.COOLDOWN_KEY,
                LAST_ZENKAI_USE_TAG, getCooldownSeconds(data));
        player.addEffect(DmzRevampEffectHelper.create(MainEffects.SAIYAN_PASSIVE.get(),
                getCooldownSeconds(data) * 20, 0, false, false, true));
        player.displayClientMessage(Component.translatable("message.dmzrevamp.zenkai.recovered")
                .withStyle(ChatFormatting.GREEN), true);
        clearTracking(player);
    }

    private static double getExistingZenkaiBonus(StatsData data, String stat) {
        return data.getBonusStats().getBonuses(stat).stream()
                .filter(bonus -> ZENKAI_BONUS_KEY.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value)
                .sum();
    }

    public static void resetZenkai(ServerPlayer player, StatsData data) {
        boolean changed = false;
        for (String stat : new String[]{"STR", "SKP", "RES", "DEF", "STM", "VIT", "PWR", "ENE"}) {
            if (getExistingZenkaiBonus(data, stat) <= 0D) {
                continue;
            }
            data.getBonusStats().removeBonus(stat, ZENKAI_BONUS_KEY);
            changed = true;
        }
        player.getPersistentData().putInt(ZENKAI_USES_TAG, 0);
        clearTracking(player);
        PersistentRacialCooldown.clear(player, data, SaiyanRpgZenkaiRacialSkill.COOLDOWN_KEY, LAST_ZENKAI_USE_TAG);
        if (changed) {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    private static boolean addZenkaiBonus(StatsData data, String stat, int increase) {
        return PermanentRacialBonusHelper.addOrAccumulateBaseCappedStat(
                data,
                stat,
                ZENKAI_BONUS_KEY,
                increase,
                getEffectiveMaxBonusBaseStatRatio(),
                true
        );
    }

    private static double getEffectiveMaxBonusBaseStatRatio() {
        double capRatio = DmzRevampRacialConfigs.saiyanRpg().maxBonusBaseStatRatio;
        return Double.isFinite(capRatio) ? Math.max(0D, capRatio) : 1.0D;
    }

    private static boolean isFriendlyFistAttack(Entity attacker) {
        if (!(attacker instanceof ServerPlayer player)) {
            return false;
        }
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> data.getStatus().isFriendlyFistEnabled())
                .orElse(false);
    }

    private static Entity resolveAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ShadowDummyEntity) {
            return attacker;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof ShadowDummyEntity ? direct : attacker;
    }

    private static boolean isZenkaiRacial(StatsData data) {
        String racialId = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        return "saiyanrevamp".equalsIgnoreCase(racialId) || "bioandroidrevamp".equalsIgnoreCase(racialId);
    }

    private static boolean isBioAndroid(StatsData data) {
        return "bioandroidrevamp".equalsIgnoreCase(CustomRacialActionHelper.getConfiguredRacialSkillId(data));
    }

    private static void clearTracking(ServerPlayer player) {
        player.getPersistentData().putInt(PENDING_STACKS_TAG, 0);
        player.getPersistentData().putInt(PENDING_LETHAL_STACKS_TAG, 0);
        player.getPersistentData().putBoolean(STACK_NOTICE_TAG, false);
        player.getPersistentData().putBoolean(LETHAL_NOTICE_TAG, false);
    }

    private static double getActivationHealthThreshold(StatsData data) {
        return DmzRevampRacialConfigs.saiyanRpg().activationHealthThreshold;
    }

    private static int getMaxStacks(StatsData data) {
        return DmzRevampRacialConfigs.saiyanRpg().maxStacks;
    }

    private static int getDeathPreventionMaxStacks(StatsData data) {
        return DmzRevampRacialConfigs.saiyanRpg().deathPreventionMaxStacks;
    }

    private static double getStatBonusPerStack(StatsData data) {
        return DmzRevampRacialConfigs.saiyanRpg().statBonusPerStack;
    }

    private static double getDecayPerUse() {
        double decay = DmzRevampRacialConfigs.saiyanRpg().zenkaiDecayPerUse;
        return Double.isFinite(decay) ? Math.max(0D, decay) : 0D;
    }

    public static int getEffectiveUseLimit() {
        double decay = getDecayPerUse();
        int automatic = decay > 0D ? Math.max(0, (int) Math.floor(1D / decay)) : 0;
        int configured = DmzRevampRacialConfigs.saiyanRpg().maxZenkais;
        if (configured < 0 || automatic > 0 && configured > automatic) configured = 0;
        return configured > 0 ? configured : automatic;
    }

    public static int getUses(StatsData data) {
        return data == null || data.getPlayer() == null ? 0
                : data.getPlayer().getPersistentData().getInt(ZENKAI_USES_TAG);
    }

    private static double getFriendlyFistMultiplier(StatsData data) {
        return DmzRevampRacialConfigs.saiyanRpg().friendlyFistStackMultiplier;
    }

    private static int getCooldownSeconds(StatsData data) {
        return DmzRevampRacialConfigs.saiyanRpg().cooldownSeconds;
    }

    private static boolean isZenkaiOnCooldown(ServerPlayer player, StatsData data) {
        return PersistentRacialCooldown.isActive(player, data, SaiyanRpgZenkaiRacialSkill.COOLDOWN_KEY,
                LAST_ZENKAI_USE_TAG, getCooldownSeconds(data));
    }
}
