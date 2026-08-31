package com.dmzrevamp.client;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.DmzSpeedRevampEvents;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzFlightBoostClientEvents {
    private static final UUID CLIENT_FLY_SPEED_UUID = UUID.fromString("47ba2127-fb49-4f7b-8f7c-8e16e830b8d3");

    // Forge calls the static client tick hook directly, so this event holder should not be instantiated.
    private DmzFlightBoostClientEvents() {
    }

    @SubscribeEvent
    // Mirrors the server's flight-speed ramp locally so flight feels responsive between stat sync packets.
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            DmzRevampClientHooks.setFlightBoostModeEnabled(false);
            return;
        }
        if (!DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
            DmzRevampClientHooks.setFlightBoostModeEnabled(false);
            clearLocalFlySpeedModifier(player);
            return;
        }

        boolean hasFly = player.hasEffect(MainEffects.FLY.get());
        DmzRevampClientHooks.tickLocalFlightMovement(
                hasFly && isProvidingFlightInput(minecraft),
                DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get(),
                DmzRevampConfig.REVAMP_SPEED_RESET_DELAY_TICKS.get(),
                minecraft.level.getGameTime()
        );

        if (!hasFly) {
            DmzRevampClientHooks.setFlightBoostModeEnabled(false);
            clearLocalFlySpeedModifier(player);
            return;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null || !data.getStatus().isHasCreatedCharacter()) {
            clearLocalFlySpeedModifier(player);
            return;
        }

        syncLocalFlySpeedModifier(player, DmzSpeedRevampEvents.getActiveFlightSpeedMultiplier(
                player,
                data,
                DmzRevampClientHooks.getLocalFlightMovementTicks()
        ) - 1D);
    }

    // Treats any movement, jump, sneak, or sprint input as active flight movement for ramping.
    private static boolean isProvidingFlightInput(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.input != null
                && (minecraft.player.isSprinting()
                || minecraft.options.keySprint.isDown()
                || Math.abs(minecraft.player.input.leftImpulse) > 1.0E-3F
                || Math.abs(minecraft.player.input.forwardImpulse) > 1.0E-3F
                || minecraft.player.input.jumping
                || minecraft.player.input.shiftKeyDown
                || minecraft.options.keyJump.isDown()
                || minecraft.options.keyShift.isDown());
    }

    private static void syncLocalFlySpeedModifier(LocalPlayer player, double amount) {
        AttributeInstance flySpeed = player.getAttribute(EntityAttributes.FLY_SPEED.get());
        if (flySpeed == null) {
            return;
        }
        AttributeModifier existing = flySpeed.getModifier(CLIENT_FLY_SPEED_UUID);
        if (existing != null) {
            flySpeed.removeModifier(existing);
        }
        double sanitized = Double.isFinite(amount) ? Math.max(-0.99D, amount) : 0D;
        if (Math.abs(sanitized) > 0.000001D) {
            flySpeed.addTransientModifier(new AttributeModifier(
                    CLIENT_FLY_SPEED_UUID,
                    "Dragon Mine Z: Overhaul client flight speed",
                    sanitized,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void clearLocalFlySpeedModifier(LocalPlayer player) {
        AttributeInstance flySpeed = player.getAttribute(EntityAttributes.FLY_SPEED.get());
        if (flySpeed == null) {
            return;
        }
        AttributeModifier existing = flySpeed.getModifier(CLIENT_FLY_SPEED_UUID);
        if (existing != null) {
            flySpeed.removeModifier(existing);
        }
    }
}
