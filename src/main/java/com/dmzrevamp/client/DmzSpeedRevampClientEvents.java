package com.dmzrevamp.client;

import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.DmzSpeedRevampEvents;
import com.dragonminez.client.events.DMZClientEvent;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzSpeedRevampClientEvents {
    private static final int EMPTY_ATTACK_SPRINT_GRACE_TICKS = 8;
    private static final int ENTITY_HIT_SPRINT_CANCEL_TICKS = 8;

    private static int speedRampTicks;
    private static int emptyAttackSprintGraceTicks;
    private static int entityHitSprintCancelTicks;
    private static boolean wasSprinting;

    // Forge calls the static client tick hook directly, so this event holder should not be instantiated.
    private DmzSpeedRevampClientEvents() {
    }

    @SubscribeEvent
    // Mirrors the server SPD ramp locally and keeps fast fluid-running responsive on the client.
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!DmzRevampConfig.ENABLE_SPD_MOVEMENT_SPEED_MODIFIERS.get()) {
            resetRampState();
            return;
        }
        if (player == null || minecraft.level == null) {
            resetRampState();
            return;
        }

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null || !data.getStatus().isHasCreatedCharacter()) {
            resetRampState();
            return;
        }

        boolean sprintingForRamp = player.isSprinting();
        if (sprintingForRamp) {
            wasSprinting = true;
        } else if (shouldRestoreSprintAfterEmptyAttack(minecraft, player)) {
            player.setSprinting(true);
            sprintingForRamp = true;
        }

        if (sprintingForRamp) {
            speedRampTicks = Math.min(DmzRevampConfig.REVAMP_SPEED_RAMP_TICKS.get(), speedRampTicks + 1);
        } else if (speedRampTicks > 0) {
            speedRampTicks = Math.max(0, speedRampTicks - DmzRevampHelper.getRampDecayStep());
        }

        if (emptyAttackSprintGraceTicks > 0) {
            emptyAttackSprintGraceTicks--;
        }
        if (entityHitSprintCancelTicks > 0) {
            entityHitSprintCancelTicks--;
        }
        wasSprinting = sprintingForRamp;

        double movementSpeedBonusPercent = DmzSpeedRevampEvents.getCurrentMovementSpeedBonusPercent(player, data, speedRampTicks);
        DmzSpeedRevampEvents.applyFluidRun(player, 100D + movementSpeedBonusPercent);
    }

    @SubscribeEvent
    // Tracks DMZ's own melee target result so empty swings do not cancel running, while real hits still do.
    public static void onPlayerAttackHit(DMZClientEvent.PlayerAttackHit event) {
        if (event.getPlayer() == null || event.getPlayer() != Minecraft.getInstance().player) {
            return;
        }

        if (event.getTargets().isEmpty()) {
            if (wasSprinting || event.getPlayer().isSprinting()) {
                emptyAttackSprintGraceTicks = EMPTY_ATTACK_SPRINT_GRACE_TICKS;
            }
            return;
        }

        emptyAttackSprintGraceTicks = 0;
        entityHitSprintCancelTicks = ENTITY_HIT_SPRINT_CANCEL_TICKS;
        event.getPlayer().setSprinting(false);
    }

    private static boolean shouldRestoreSprintAfterEmptyAttack(Minecraft minecraft, LocalPlayer player) {
        if (!wasSprinting || entityHitSprintCancelTicks > 0 || !isTryingToRun(player)) {
            return false;
        }

        boolean attackIsActive = minecraft.options.keyAttack.isDown();
        if (minecraft instanceof Minecraft_DMZ dmzMinecraft) {
            attackIsActive = attackIsActive || dmzMinecraft.getSwingProgress() < 0.98F;
        }
        if (!attackIsActive) {
            return false;
        }

        if (emptyAttackSprintGraceTicks <= 0) {
            emptyAttackSprintGraceTicks = EMPTY_ATTACK_SPRINT_GRACE_TICKS;
        }
        return true;
    }

    private static boolean isTryingToRun(LocalPlayer player) {
        return player.input != null
                && player.input.forwardImpulse > 0F
                && !player.input.shiftKeyDown
                && !player.isPassenger()
                && !player.isUsingItem();
    }

    private static void resetRampState() {
        speedRampTicks = 0;
        emptyAttackSprintGraceTicks = 0;
        entityHitSprintCancelTicks = 0;
        wasSprinting = false;
    }
}
