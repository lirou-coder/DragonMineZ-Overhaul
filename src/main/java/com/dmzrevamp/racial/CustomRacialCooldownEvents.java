package com.dmzrevamp.racial;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.effect.DmzRevampEffectHelper;
import com.dmzrevamp.effect.DmzRevampEffects;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiRacialSkill;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiEvents;
import com.dmzrevamp.racial.impl.NamekianRevampRacialSkill;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
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
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            removedAny[0] = clearAllRacialCooldowns(data);
            PersistentRacialCooldown.clear(player, data, SaiyanRpgZenkaiRacialSkill.COOLDOWN_KEY,
                    SaiyanRpgZenkaiEvents.LAST_ZENKAI_USE_TAG);
            PersistentRacialCooldown.clear(player, data, NamekianRevampRacialSkill.COOLDOWN_KEY,
                    NamekianRevampRacialSkill.LAST_USE_TAG);
            // Remove the legacy Overhaul key so old saves cannot silently restore it.
            data.getCooldowns().removeCooldown("DmzRevampSaiyanZenkai");
        });
        player.removeEffect(DmzRevampEffects.RACIAL_COOLDOWN.get());
        player.removeEffect(DmzRevampEffects.ADRENALINE_COOLDOWN.get());
        player.removeEffect(DmzRevampEffects.REGENERATION_COOLDOWN.get());
        player.removeEffect(MainEffects.ADRENALINE.get());
        player.removeEffect(MainEffects.BIOANDROID_PASSIVE.get());
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
        String[] nativeRacialCooldowns = {
                Cooldowns.ZENKAI, Cooldowns.ZENKAI_KNOCKOUT, Cooldowns.ZENKAI_TEMP_BUFF,
                Cooldowns.ASSIMILATION, Cooldowns.ABSORPTION,
                Cooldowns.ADRENALINE, Cooldowns.ADRENALINE_ACTIVE,
                Cooldowns.ANDROID_BARRIER_CD, Cooldowns.NAMEK_REGEN, Cooldowns.NAMEK_REGEN_ACTIVE,
                Cooldowns.DRAIN, Cooldowns.DRAIN_ACTIVE,
                Cooldowns.BIO_EXPLODE_CD, Cooldowns.BIO_EXPLODE_RECOVERY,
                Cooldowns.MAJIN_REVIVE_CD, Cooldowns.MAJIN_REVIVE_ACTIVE
        };
        for (String key : nativeRacialCooldowns) {
            if (data.getCooldowns().hasCooldown(key)) removedAny = true;
            data.getCooldowns().removeCooldown(key);
        }
        for (String key : data.getCooldowns().getAllCooldowns().keySet()) {
            if (key.startsWith("AbsorptionSlot_") || key.startsWith("CellJrSlot_")) {
                data.getCooldowns().removeCooldown(key);
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
