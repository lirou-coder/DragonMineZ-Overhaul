package com.dmzrevamp.racial;

import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiEvents;
import com.dragonminez.common.racial.LethalContext;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.racial.impl.FrostDemonReserve;
import com.dragonminez.common.racial.impl.HumanAdaptation;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.world.damagesource.DamageSource;

/** Makes DMZ 2.2's native racial modules available to Overhaul racial ids. */
public final class DmzNativeRacialBridge {
    private DmzNativeRacialBridge() {
    }

    public static void register() {
        RacialRegistry.register(new FrostBridge());
        RacialRegistry.register(new HumanBridge());
        RacialRegistry.register(new SaiyanBridge());
        RacialRegistry.register(new NamekBridge());
        RacialRegistry.register(new BioAndroidBridge());
    }

    private static final class FrostBridge extends FrostDemonReserve {
        @Override public String id() { return "frostrevamp"; }
    }

    private static final class HumanBridge extends HumanAdaptation {
        @Override public String id() { return "humanrevamp"; }
    }

    /** Overhaul owns Zenkai stat gains; this exposes its progressive release bonus to DMZ. */
    private static final class SaiyanBridge implements RacialAbility {
        @Override public String id() { return "saiyanrevamp"; }

        @Override
        public double maxReleaseMultiplier(StatsData data) {
            int maximum = SaiyanRpgZenkaiEvents.getEffectiveUseLimit();
            if (maximum <= 0 || data == null || data.getPlayer() == null) return 1D;
            int uses = Math.max(0, data.getPlayer().getPersistentData()
                    .getInt(SaiyanRpgZenkaiEvents.ZENKAI_USES_TAG));
            double progress = Math.min(1D, uses / (double) maximum);
            double configured = DmzRevampRacialConfigs.saiyanRpg().maxZenkaiReleaseBonus;
            double releasePoints = Math.max(0D, Double.isFinite(configured) ? configured : 0D) * 100D * progress;
            int potentialUnlock = data.getSkills().hasSkill("potentialunlock")
                    ? data.getSkills().getSkillLevel("potentialunlock") : 0;
            double baseRelease = 50D + potentialUnlock * 5D;
            return 1D + releasePoints / Math.max(1D, baseRelease);
        }
    }

    /** Assimilation remains owned by Overhaul; DMZ supplies regeneration and its lifecycle. */
    private static final class NamekBridge implements RacialAbility {
        private final NamekAssimilation nativeAbility = new NamekAssimilation();

        @Override public String id() { return "namekianrevamp"; }
        @Override public void onSecond(RacialContext ctx) { nativeAbility.onSecond(ctx); }
        @Override public void onDamageTakenPost(RacialContext ctx, float damage) {
            nativeAbility.onDamageTakenPost(ctx, damage);
        }
        @Override public void onDeath(RacialContext ctx) { nativeAbility.onDeath(ctx); }
    }

    /**
     * Bio Android keeps its unscaled native form action while borrowing passive modules from the
     * other races. Overhaul's existing event handlers apply the deliberately weakened copies of
     * Human, Saiyan and Frost Demon stat bonuses.
     */
    private static final class BioAndroidBridge implements RacialAbility {
        private final BioAndroidEvolution evolution = new BioAndroidEvolution();
        private final HumanAdaptation human = new HumanAdaptation();
        private final FrostDemonReserve reserve = new FrostDemonReserve();
        private final NamekAssimilation namek = new NamekAssimilation();

        @Override public String id() { return "bioandroidrevamp"; }
        @Override public boolean hasActiveAction() { return evolution.hasActiveAction(); }
        @Override public boolean hasActiveAction(StatsData data) { return evolution.hasActiveAction(data); }
        @Override public boolean canActivate(RacialContext ctx) { return evolution.canActivate(ctx); }
        @Override public int chargeSeconds(RacialContext ctx) { return evolution.chargeSeconds(ctx); }
        @Override public boolean onActivate(RacialContext ctx) { return evolution.onActivate(ctx); }
        @Override public boolean onSecondaryActivate(RacialContext ctx) { return evolution.onSecondaryActivate(ctx); }

        @Override public void onTick(RacialContext ctx) {
            evolution.onTick(ctx);
            human.onTick(ctx);
        }

        @Override public void onSecond(RacialContext ctx) {
            evolution.onSecond(ctx);
            tickScaledNamekRegen(ctx);
            reserve.onSecond(ctx);
        }

        private void tickScaledNamekRegen(RacialContext ctx) {
            StatsData data = ctx.data();
            if (!data.getCooldowns().hasCooldown(Cooldowns.NAMEK_REGEN_ACTIVE)) return;
            var config = ctx.config().getNamekian();
            double factor = Math.max(0D, DmzRevampRacialConfigs.bioAndroid().effectMultiplier);
            int seconds = Math.max(1, config.getRegenChannelSeconds());
            ctx.player().heal((float) (ctx.player().getMaxHealth() * config.getRegenHealthRatio() * factor / seconds));
            data.getResources().removeEnergy((float) (data.getMaxEnergy() * config.getRegenEnergyCost() * factor / seconds));
            data.getResources().removeStamina((float) (data.getMaxStamina() * config.getRegenStaminaCost() * factor / seconds));
        }

        @Override public double modifyDamageTaken(RacialContext ctx, double damage, DamageSource source) {
            return human.modifyDamageTaken(ctx, reserve.modifyDamageTaken(ctx, damage, source), source);
        }

        @Override public double modifyDamageTaken(RacialContext ctx, double damage, double rawDamage, DamageSource source) {
            return human.modifyDamageTaken(ctx, reserve.modifyDamageTaken(ctx, damage, source), rawDamage, source);
        }

        @Override public void onDamageTakenPost(RacialContext ctx, float damage) {
            namek.onDamageTakenPost(ctx, damage);
        }

        @Override public int consumeFormUpkeep(RacialContext ctx, int energyDrain) {
            return reserve.consumeFormUpkeep(ctx, energyDrain);
        }

        @Override public double modifyFormStatMultiplier(StatsData data, String group, double multiplier) {
            return reserve.modifyFormStatMultiplier(data, group, multiplier);
        }

        @Override public void onDeath(RacialContext ctx) {
            evolution.onDeath(ctx);
            human.onDeath(ctx);
            reserve.onDeath(ctx);
        }

        @Override public void onLogout(RacialContext ctx) {
            evolution.onLogout(ctx);
        }

        @Override public void onLogin(RacialContext ctx) {
            evolution.onLogin(ctx);
            human.onLogin(ctx);
        }

        @Override public void onRespawn(RacialContext ctx) {
            human.onRespawn(ctx);
        }

        @Override public void onDimensionChange(RacialContext ctx) {
            evolution.onDimensionChange(ctx);
            human.onDimensionChange(ctx);
            reserve.onDimensionChange(ctx);
        }
    }
}
