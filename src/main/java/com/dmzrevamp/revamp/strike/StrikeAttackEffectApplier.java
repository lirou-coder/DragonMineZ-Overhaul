package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffectRules;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.EntityStatDebuffs;
import com.dragonminez.common.stats.character.SecondaryStatEffects;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class StrikeAttackEffectApplier {
    private StrikeAttackEffectApplier() {
    }

    public static void applyExtras(StrikeAttackData data, LivingEntity attacker, LivingEntity target) {
        if (!(data instanceof RevampStrikeAttackData revamp)) {
            return;
        }
        boolean beneficial = revamp.dmzrevamp$getStrikeType().isEvasive();
        LivingEntity effectTarget = beneficial ? attacker : target;
        applySecondary(revamp.dmzrevamp$getSecondaryEffectType(), revamp.dmzrevamp$getSecondaryAffectedStat(), revamp.dmzrevamp$getSecondaryIntensity(), revamp.dmzrevamp$getSecondaryDuration(), beneficial, effectTarget);
        applySecondary(revamp.dmzrevamp$getThirdEffectType(), revamp.dmzrevamp$getThirdAffectedStat(), revamp.dmzrevamp$getThirdIntensity(), revamp.dmzrevamp$getThirdDuration(), beneficial, effectTarget);
        applySecondary(revamp.dmzrevamp$getFourthEffectType(), revamp.dmzrevamp$getFourthAffectedStat(), revamp.dmzrevamp$getFourthIntensity(), revamp.dmzrevamp$getFourthDuration(), beneficial, effectTarget);
        applyExtra(revamp.dmzrevamp$getExtraEffectOne(), beneficial, effectTarget);
        applyExtra(revamp.dmzrevamp$getExtraEffectTwo(), beneficial, effectTarget);
    }

    private static void applySecondary(KiAttackData.SecondaryEffectType type, KiAttackData.AffectedStat stat, float intensity, int durationSeconds, boolean beneficial, LivingEntity target) {
        if (type == KiAttackData.SecondaryEffectType.NONE || stat == null || intensity <= 0.0F || durationSeconds <= 0) {
            return;
        }
        if (beneficial != (type == KiAttackData.SecondaryEffectType.BUFF)) {
            return;
        }
        double modifier = intensity / 100.0D;
        if (!beneficial) {
            modifier = -modifier;
        }
        int durationTicks = Math.max(1, durationSeconds * 20);
        String statName = stat.name();
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, target).resolve().orElse(null);
        if (data != null) {
            SecondaryStatEffects effects = data.getSecondaryStatEffects();
            effects.apply(statName, modifier, durationTicks);
            if (target instanceof ServerPlayer player) {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            }
            return;
        }
        if (!beneficial && EntityStatDebuffs.isSupported(statName)) {
            EntityStatDebuffs.applyDebuff(target, statName, modifier, durationTicks);
        }
    }

    private static void applyExtra(KiAttackExtraEffect extra, boolean beneficial, LivingEntity target) {
        if (!extra.isActive()) {
            return;
        }
        if (beneficial && extra.mode() != KiAttackExtraEffect.Mode.BENEFICIAL) {
            return;
        }
        if (!beneficial && extra.mode() != KiAttackExtraEffect.Mode.HARMFUL) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(extra.effectId());
        if (!KiAttackExtraEffectRules.isAllowed(id)) {
            return;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null || effect.isBeneficial() != beneficial) {
            return;
        }
        int durationTicks = KiAttackExtraEffectRules.clampAppliedDurationTicks(extra.mode(), Math.max(1, extra.durationSeconds() * 20), target instanceof Player);
        int amplifier = Math.max(0, extra.level() - 1);
        MobEffectInstance current = target.getEffect(effect);
        if (current != null && current.getAmplifier() > amplifier && current.getDuration() >= durationTicks) {
            return;
        }
        target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
    }
}
