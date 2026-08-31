package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cancels fully-negated ordinary hits before vanilla applies hurt animation/knockback. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdaptiveDefenseFullNegationEvents {
    private AdaptiveDefenseFullNegationEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelOrdinaryFullyNegatedHit(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player victim) || victim.level().isClientSide()) {
            return;
        }
        AdaptiveDefenseMoreConfigured.Config config = AdaptiveDefenseMoreConfigured.get();
        if (!config.enable || event.getAmount() <= 0F) {
            return;
        }
        if (!isEligibleOrdinaryHit(event.getSource(), victim)) {
            return;
        }

        StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, victim).resolve().orElse(null);
        if (stats == null) {
            return;
        }
        double defense = stats.getDefense() * Math.max(1D, stats.getTotalMultiplier("DEF"));
        double referenceDamage = event.getAmount();
        double cancellationPoint = referenceDamage * config.cancelDamageMitigationThreshold;
        if (Double.isFinite(defense) && Double.isFinite(referenceDamage) && defense > 0D
                && referenceDamage > 0D
                && defense >= cancellationPoint) {
            event.setCanceled(true);
            int variant = victim.getRandom().nextInt(3);
            SoundEvent sound = switch (variant) {
                case 0 -> MainSounds.BLOCK1.get();
                case 1 -> MainSounds.BLOCK2.get();
                default -> MainSounds.BLOCK3.get();
            };
            victim.level().playSound(null, victim.blockPosition(), sound, SoundSource.PLAYERS, 1.0F,
                    0.9F + victim.getRandom().nextFloat() * 0.1F);
        }
    }

    private static boolean isEligibleOrdinaryHit(DamageSource source, Player victim) {
        // Context and DMZ damage keys cover both built-in and Overhaul custom Ki/Strike attacks.
        if (AdaptiveDefenseDamageContext.current() != null
                || MainDamageTypes.isKiblastDamage(source)
                || MainDamageTypes.isStrikeAttackDamage(source)
                || source.getDirectEntity() instanceof AbstractKiProjectile) {
            return false;
        }

        // Environmental damage and effect ticks are not physical attacks that Defense may fully negate.
        if (source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL)
                || source.is(DamageTypes.DRAGON_BREATH)) {
            return false;
        }

        Entity attacker = source.getEntity();
        return attacker instanceof LivingEntity && attacker != victim;
    }

}
