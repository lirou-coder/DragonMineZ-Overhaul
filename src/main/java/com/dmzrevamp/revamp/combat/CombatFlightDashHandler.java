package com.dmzrevamp.revamp.combat;

import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.ComboManager;
import com.dmzrevamp.network.CombatFlightDashImpulseS2CPacket;
import com.dmzrevamp.network.DmzRevampNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

public final class CombatFlightDashHandler {
    private static final int FLIGHT_COMBAT = 1;

    private CombatFlightDashHandler() {
    }

    public static boolean tryHandleDoubleDash(ServerPlayer player, float zInput, float xInput, StatsData data) {
        if (player == null || data == null || !data.getStatus().isHasCreatedCharacter()) {
            return false;
        }
        if (!data.getSkills().isSkillActive("fly") || data.getStatus().getFlightMode() != FLIGHT_COMBAT) {
            return false;
        }
        CombatConfig config = ConfigManager.getCombatConfig();
        if (ComboManager.canTeleport(player.getUUID()) || isPerfectEvasionWindow(data, config)) {
            return false;
        }
        if (player.hasEffect(MainEffects.STUN.get()) || data.getStatus().isStunned()) {
            return true;
        }

        Cooldowns cooldowns = data.getCooldowns();
        boolean doubleDash = cooldowns.hasCooldown(Cooldowns.DASH_ACTIVE)
                && !cooldowns.hasCooldown(Cooldowns.DOUBLEDASH_CD);
        if (!doubleDash) {
            return true;
        }

        int baselineDrain = config.getBaselineFormDrain();
        int kiCost = player.isCreative() || player.isSpectator() ? 0 : (int) Math.ceil(baselineDrain * 0.25D);
        double distance = groundDashDistance(player, true);
        DMZEvent.PlayerDashEvent event = new DMZEvent.PlayerDashEvent(
                player,
                DMZEvent.PlayerDashEvent.DashType.DOUBLE,
                distance,
                kiCost
        );
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return true;
        }

        kiCost = Math.max(0, event.getKiCost());
        distance = Math.max(0D, event.getDistance());
        if (!player.isCreative() && (data.getResources().getCurrentEnergy() < kiCost || player.getFoodData().getFoodLevel() <= 3)) {
            return true;
        }

        if (kiCost > 0) {
            data.getResources().removeEnergy(kiCost);
        }
        applyCombatFlightMotion(player, xInput, zInput, distance);
        applyCooldownsAndEffects(player, data, config);
        notifyClientImpulse(player, xInput, zInput, data);
        sendDashAnimation(player, xInput, zInput);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        return true;
    }

    private static boolean isPerfectEvasionWindow(StatsData data, CombatConfig config) {
        if (!config.getEnablePerfectEvasion()) {
            return false;
        }
        long lastHurtTime = data.getStatus().getLastHurtTime();
        return lastHurtTime > 0L && System.currentTimeMillis() - lastHurtTime <= config.getPerfectEvasionWindowMs();
    }

    public static double groundDashDistance(ServerPlayer player, boolean doubleDash) {
        double baseDistance = 4D;
        double movementScale = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) / 0.1D;
        double distance = baseDistance * movementScale;
        return doubleDash ? distance * 1.5D : distance;
    }

    private static void applyCombatFlightMotion(ServerPlayer player, float xInput, float zInput, double distance) {
        Vec3 forward = Vec3.directionFromRotation(0F, player.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0D, 1D, 0D)).normalize();
        Vec3 direction = forward.scale(zInput).add(right.scale(xInput));
        if (direction.lengthSqr() <= 1.0E-6D) {
            direction = forward;
        } else {
            direction = direction.normalize();
        }

        Vec3 currentMotion = player.getDeltaMovement();
        double serverSpeed = player.getPersistentData().getDouble("dmz_server_speed");
        double speedBase = Math.max(currentMotion.length(), serverSpeed);
        double flyDashSpeed = Math.min(3.8D, Math.max(0.55D, distance * 0.3D + speedBase * 2.5D));
        Vec3 dashMotion = direction.scale(flyDashSpeed);
        player.setDeltaMovement(currentMotion.x + dashMotion.x, 0D, currentMotion.z + dashMotion.z);
        player.fallDistance = 0F;
        player.hasImpulse = true;
    }

    private static void applyCooldownsAndEffects(ServerPlayer player, StatsData data, CombatConfig config) {
        int dashCooldownTicks = config.getDashCooldownSeconds() * 20;
        int doubleDashCooldownTicks = config.getDoubleDashCooldownSeconds() * 20;
        Cooldowns cooldowns = data.getCooldowns();
        cooldowns.setCooldown(Cooldowns.DASH_CD, dashCooldownTicks);
        cooldowns.setCooldown(Cooldowns.DOUBLEDASH_CD, doubleDashCooldownTicks);
        cooldowns.removeCooldown(Cooldowns.DASH_ACTIVE);
        player.addEffect(new MobEffectInstance(MainEffects.DOUBLEDASH_CD.get(), doubleDashCooldownTicks, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MainEffects.DASH_CD.get(), dashCooldownTicks, 0, false, false, true));

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5D, player.getZ(), 1, 0D, 0D, 0D, 0D);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.5F + player.getRandom().nextFloat() * 0.3F);
    }

    private static void sendDashAnimation(ServerPlayer player, float xInput, float zInput) {
        int variant = getDashDirectionFromInput(xInput, zInput);
        variant += 4;
        NetworkHandler.sendToTrackingEntityAndSelf(
                new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.DASH, variant, player.getId()),
                player
        );
    }

    private static int getDashDirectionFromInput(float xInput, float zInput) {
        if (zInput > 0F && xInput == 0F) {
            return 1;
        }
        if (zInput < 0F && xInput == 0F) {
            return 2;
        }
        if (xInput < 0F && zInput == 0F) {
            return 4;
        }
        if (xInput > 0F && zInput == 0F) {
            return 3;
        }
        return zInput < 0F ? 2 : 1;
    }

    public static void notifyClientImpulse(ServerPlayer player, float xInput, float zInput, StatsData data) {
        if (player == null || data == null
                || !data.getSkills().isSkillActive("fly")
                || data.getStatus().getFlightMode() != FLIGHT_COMBAT) {
            return;
        }
        int direction = getFlightImpulseDirectionFromInput(xInput, zInput);
        DmzRevampNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CombatFlightDashImpulseS2CPacket(direction)
        );
    }

    /** CombatFlightHandler uses 0/1/2/3 for forward/back/left/right, unlike dash animations. */
    private static int getFlightImpulseDirectionFromInput(float xInput, float zInput) {
        if (zInput > 0F) {
            return 0;
        }
        if (zInput < 0F) {
            return 1;
        }
        if (xInput < 0F) {
            return 2;
        }
        if (xInput > 0F) {
            return 3;
        }
        return 0;
    }

}
