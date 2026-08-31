package com.dmzrevamp.racial;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.effect.DmzRevampEffectHelper;
import com.dmzrevamp.effect.DmzRevampEffects;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiRacialSkill;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CustomRacialCooldownEvents {
    private static final Map<String, RegistryObject<net.minecraft.world.effect.MobEffect>> COOLDOWN_EFFECTS = Map.of(
            SaiyanRpgZenkaiRacialSkill.COOLDOWN_KEY, MainEffects.SAIYAN_PASSIVE
    );

    private CustomRacialCooldownEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            for (var entry : COOLDOWN_EFFECTS.entrySet()) {
                syncCooldownEffect(player, data, entry.getKey(), entry.getValue());
            }
        });
    }

    // Removes every racial cooldown tracked by this addon from one player.
    public static boolean clearAllRacialCooldowns(ServerPlayer player) {
        final boolean[] removedAny = {false};
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> removedAny[0] = clearAllRacialCooldowns(data));
        player.removeEffect(DmzRevampEffects.RACIAL_COOLDOWN.get());
        for (var effect : COOLDOWN_EFFECTS.values()) {
            player.removeEffect(effect.get());
        }
        return removedAny[0];
    }

    // Clears the cooldown keys stored inside DMZ stats data.
    public static boolean clearAllRacialCooldowns(StatsData data) {
        boolean removedAny = false;
        for (CustomRacialSkill skill : CustomRacialSkillRegistry.values()) {
            String cooldownKey = skill.cooldownKey();
            if (cooldownKey == null || cooldownKey.isBlank()) {
                continue;
            }
            if (data.getCooldowns().hasCooldown(cooldownKey)) {
                data.getCooldowns().removeCooldown(cooldownKey);
                removedAny = true;
            }
        }
        return removedAny;
    }

    // Mirrors a stat cooldown into a visible mob effect icon when one is configured.
    private static void syncCooldownEffect(ServerPlayer player, StatsData data, String cooldownKey, RegistryObject<net.minecraft.world.effect.MobEffect> effect) {
        int activeCooldown = data.getCooldowns().getCooldown(cooldownKey);
        if (activeCooldown > 0) {
            MobEffectInstance activeInstance = player.getEffect(effect.get());
            if (activeInstance == null || activeInstance.getDuration() < Math.max(20, activeCooldown - 5)) {
                player.addEffect(DmzRevampEffectHelper.create(effect.get(), activeCooldown, 0));
            }
        } else if (player.hasEffect(effect.get())) {
            player.removeEffect(effect.get());
        }
    }
}
