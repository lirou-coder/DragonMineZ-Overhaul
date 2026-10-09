package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** DMZ-style flat and adaptive defense for every non-player living entity. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobDefenseEvents {
    private static final ThreadLocal<LivingEntity> PREMITIGATED_BOSS_DAMAGE = new ThreadLocal<>();
    private MobDefenseEvents() {}

    public static void markPreMitigatedBossDamage(LivingEntity entity) {
        PREMITIGATED_BOSS_DAMAGE.set(entity);
    }

    public static boolean consumePreMitigatedBossDamage(LivingEntity entity) {
        LivingEntity marked = PREMITIGATED_BOSS_DAMAGE.get();
        if (marked != entity) return false;
        PREMITIGATED_BOSS_DAMAGE.remove();
        return true;
    }

    public static void clearPreMitigatedBossDamage() {
        PREMITIGATED_BOSS_DAMAGE.remove();
    }

    // DMZ rebuilds player melee damage at HIGH and writes it back to the event. Run after that
    // authoritative calculation so mob defense cannot be overwritten by CombatEvent.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mitigate(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || event.getAmount() <= 0F) return;
        if (consumePreMitigatedBossDamage(entity)) return;
        AttributeInstance attribute = entity.getAttribute(DmzRevampAttributes.MOB_DEFENSE.get());
        if (attribute == null) return;
        double defense = Math.max(0D, attribute.getValue());
        if (defense <= 0D) return;

        event.setAmount(mitigatedDamage(entity, event.getAmount()));
    }

    /** Uses the authoritative mob-defense formula for server damage. */
    public static float mitigatedDamage(LivingEntity entity, float incomingAmount) {
        if (entity == null || entity instanceof Player || incomingAmount <= 0F) return incomingAmount;
        AttributeInstance attribute = entity.getAttribute(DmzRevampAttributes.MOB_DEFENSE.get());
        if (attribute == null) return incomingAmount;
        double defense = Math.max(0D, attribute.getValue());
        if (defense <= 0D) return incomingAmount;

        CombatConfig config = ConfigManager.getCombatConfig();
        double incoming = incomingAmount;
        // DMZ 2.2 removed flatMitigationFactor. Its current flat stage absorbs
        // defense directly, capped by flatMitigationMaxAbsorbFraction.
        double flat = Math.min(defense, incoming * config.getFlatMitigationMaxAbsorbFraction());
        double afterFlat = Math.max(0D, incoming - flat);

        // Mirrors the player's max-Defense damage-reduction stage, but deliberately uses only
        // mob_defense. Vanilla armor and Protection remain independent mitigation systems.
        double defenseReference = playerDefenseReference();
        double defenseReduction = DmzRevampHelper.getConfiguredDefenseStyleEffect(
                defense,
                defenseReference,
                config.getBaseDamageReductionCap()
        );
        double afterDefenseCurve = afterFlat * (1D - defenseReduction);

        if (AdaptiveDefenseMoreConfigured.get().adaptiveDefense.enabled) {
            afterDefenseCurve *= 1D - configuredAdaptiveMitigation(incoming, defense);
        } else if (config.getEnableAdaptativeDefenseMitigation() && defense > 0D) {
            // DMZ uses incoming damage / raw defense for its adaptive curve. The mob's
            // separate flat-mitigation factor must not alter that ratio.
            afterDefenseCurve *= 1D - adaptiveMitigation(incoming / defense, config);
        }
        // Mob Defense never invokes the player's complete-negation rule.
        return (float) Math.max(0.0001D, afterDefenseCurve);
    }

    private static double playerDefenseReference() {
        if (LevelingRevampConfig.levelsEnabled()) {
            return Math.max(1D, PrestigeSystem.attributeFormulaMaximum());
        }
        var gameplay = ConfigManager.getServerConfig().getGameplay();
        double configuredMaximum = Math.max(1D, gameplay.getMaxValue());
        return gameplay.getMaxLevelValueInsteadOfStats()
                ? configuredMaximum * 6D / 2D
                : configuredMaximum;
    }

    private static double configuredAdaptiveMitigation(double incoming, double defense) {
        AdaptiveDefenseMoreConfigured.AdaptiveDefense config = AdaptiveDefenseMoreConfigured.get().adaptiveDefense;
        AdaptiveDefenseDamageContext.Entry context = AdaptiveDefenseDamageContext.current();
        double reference = context == null ? incoming : Math.max(incoming, context.totalTechniqueDamage());
        double ratio = reference / Math.max(0.0001D, defense);
        double capPoint = 1D / config.adaptiveDefenseCapRatio;
        double mitigation;
        if (ratio <= capPoint) mitigation = config.adaptativeDefenseMitigationCap;
        else if (ratio < config.adaptativeMitigationParityRatio) {
            double progress = (config.adaptativeMitigationParityRatio - ratio)
                    / Math.max(0.0001D, config.adaptativeMitigationParityRatio - capPoint);
            mitigation = config.adaptativeMitigationParityValue
                    + (config.adaptativeDefenseMitigationCap - config.adaptativeMitigationParityValue) * progress;
        } else if (ratio < config.adaptativeMitigationZeroRatio) {
            mitigation = config.adaptativeMitigationParityValue
                    * (config.adaptativeMitigationZeroRatio - ratio)
                    / Math.max(0.0001D, config.adaptativeMitigationZeroRatio - config.adaptativeMitigationParityRatio);
        } else mitigation = 0D;
        if (context != null) {
            mitigation *= context.type() == AdaptiveDefenseDamageContext.AttackType.KI
                    ? config.adaptiveDefenseKiAttackEfficiency : config.adaptiveDefenseStrikeAttackEfficiency;
        }
        return Math.min(config.adaptativeDefenseMitigationCap, clamp(mitigation));
    }

    private static double adaptiveMitigation(double ratio, CombatConfig config) {
        if (!Double.isFinite(ratio) || ratio <= 0D) return 0D;
        double parityRatio = config.getAdaptativeMitigationParityRatio();
        double parityValue = config.getAdaptativeMitigationParityValue();
        double zeroRatio = config.getAdaptativeMitigationZeroRatio();
        double cap = config.getAdaptativeDefenseMitigationCap();
        double slope = parityValue / (zeroRatio - parityRatio);
        double mitigation = parityValue + slope * (parityRatio - ratio);
        if (!Double.isFinite(mitigation) || mitigation <= 0D) return 0D;
        return Math.min(mitigation, cap);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0D, Math.min(1D, value)) : 0D;
    }
}
