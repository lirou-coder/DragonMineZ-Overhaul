package com.dmzrevamp.mixin;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.server.events.players.TickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps DMZ resource regeneration progressing when its float-backed resources
 * become too large for the calculated per-tick regen to change the stored float.
 */
@Mixin(TickHandler.class)
public abstract class ResourceRegenPrecisionMixin {
    private static final float DMZ_INTEGER_ENERGY_LIMIT = (float) Integer.MAX_VALUE;

    @Redirect(
            method = "regenerateHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;m_21153_(F)V", remap = false)
    )
    private static void dmzrevamp$preserveHealthRegenPrecision(
            ServerPlayer entity, float requestedHealth, ServerPlayer player, StatsData data, double foodRegenMod) {
        float current = player.getHealth();
        float maximum = player.getMaxHealth();
        player.setHealth(progressFloatResource(current, requestedHealth, maximum));
    }

    @Redirect(
            method = "regenerateEnergy",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;setCurrentEnergy(F)V"),
            remap = false
    )
    private static void dmzrevamp$preserveEnergyRegenPrecision(
            Resources resources, float requestedEnergy, ServerPlayer player, StatsData data,
            boolean activeCharging, double foodRegenMod) {
        float current = resources.getCurrentEnergy();
        float maximum = effectiveMaximum(data.getMaxEnergy(), data);
        float requested = requestedEnergy;

        // DMZ stores the calculated energy as an int before this call. Once
        // that conversion saturates at Integer.MAX_VALUE, keep advancing by
        // representable float steps instead of leaving Ki permanently capped.
        if (maximum > DMZ_INTEGER_ENERGY_LIMIT && requested >= DMZ_INTEGER_ENERGY_LIMIT
                && current >= DMZ_INTEGER_ENERGY_LIMIT) {
            requested = Math.nextUp(current);
        }
        resources.setCurrentEnergy(progressFloatResource(current, requested, maximum));
    }

    @Redirect(
            method = "regenerateStamina",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/stats/character/Resources;setCurrentStamina(F)V"),
            remap = false
    )
    private static void dmzrevamp$preserveStaminaRegenPrecision(
            Resources resources, float requestedStamina, ServerPlayer player, StatsData data, double foodRegenMod) {
        float current = resources.getCurrentStamina();
        float maximum = effectiveMaximum(data.getMaxStamina(), data);
        resources.setCurrentStamina(progressFloatResource(current, requestedStamina, maximum));
    }

    private static float effectiveMaximum(float maximum, StatsData data) {
        if (data.getStatus().hasActiveShadowDummy()) {
            return maximum * (1.0F - data.getStatus().getShadowDummyPercent() / 100.0F);
        }
        return maximum;
    }

    private static float progressFloatResource(float current, float requested, float maximum) {
        if (!Float.isFinite(current) || !Float.isFinite(requested) || !Float.isFinite(maximum)) {
            return requested;
        }
        float bounded = Math.min(maximum, Math.max(0.0F, requested));
        if (current < maximum && bounded <= current) {
            bounded = Math.min(maximum, Math.nextUp(current));
        }
        return bounded;
    }
}
