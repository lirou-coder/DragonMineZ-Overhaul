package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DefenseDamageCancelEvents {
    private DefenseDamageCancelEvents() {
    }

    // The old pre-hurt cancellation used the damage of one split hit. Full negation is now
    // decided inside StatsData, where the configured system can see the whole technique.
    public static void cancelLowDamageBeforeVanillaHit(LivingAttackEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0F || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasExternalLivingAttacker(event, player)) {
            return;
        }

        CombatConfig config = ConfigManager.getCombatConfig();
        if (config == null || !config.getCancelDamageEventIfMitigationTooHigh()) {
            return;
        }

        double threshold = config.getCancelDamageMitigationThreshold();
        if (!Double.isFinite(threshold) || threshold <= 0D) {
            return;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player)
                .filter(stats -> stats.getStatus().isHasCreatedCharacter())
                .orElse(null);
        if (data == null) {
            return;
        }

        double defense = data.getDefense();
        double incomingDamage = getEffectiveIncomingDamage(event);
        if (Double.isFinite(defense) && defense > 0D
                && Math.abs(defense - incomingDamage * threshold) <= Math.max(1.0E-6D, Math.abs(defense) * 1.0E-6D)) {
            event.setCanceled(true);
            playBlockSound(player);
        }
    }

    private static boolean hasExternalLivingAttacker(LivingAttackEvent event, ServerPlayer target) {
        return event.getSource().getEntity() instanceof LivingEntity attacker && attacker != target;
    }

    private static double getEffectiveIncomingDamage(LivingAttackEvent event) {
        double damage = event.getAmount();
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return damage;
        }

        StatsData attackerData = StatsProvider.get(StatsCapability.INSTANCE, attacker)
                .filter(stats -> stats.getStatus().isHasCreatedCharacter())
                .orElse(null);
        if (attackerData == null) {
            return damage;
        }

        double dmzDamage = attackerData.getMeleeDamage();
        if (!Double.isFinite(dmzDamage) || dmzDamage <= 0D) {
            return damage;
        }
        return damage + dmzDamage;
    }

    private static void playBlockSound(ServerPlayer player) {
        int variant = player.getRandom().nextInt(3);
        SoundEvent sound = switch (variant) {
            case 0 -> MainSounds.BLOCK1.get();
            case 1 -> MainSounds.BLOCK2.get();
            default -> MainSounds.BLOCK3.get();
        };
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0F,
                0.9F + player.getRandom().nextFloat() * 0.1F
        );
    }
}
