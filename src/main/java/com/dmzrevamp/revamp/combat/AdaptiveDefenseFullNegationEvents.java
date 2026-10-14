package com.dmzrevamp.revamp.combat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.FullNegationS2CPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Suppresses residual impact feedback when DMZ's own mitigation fully negates a hit. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdaptiveDefenseFullNegationEvents {
    private static final String NEGATION_TICK_TAG = "dmzrevamp_full_negation_tick";

    private AdaptiveDefenseFullNegationEvents() {
    }

    public static void markFullyNegated(LivingEntity victim) {
        victim.getPersistentData().putLong(NEGATION_TICK_TAG, victim.level().getGameTime());
        victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        victim.hasImpulse = true;
        victim.hurtMarked = true;
        victim.hurtTime = 0;
        victim.hurtDuration = 0;
        if (victim instanceof net.minecraft.server.level.ServerPlayer player) {
            DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FullNegationS2CPacket());
        }
    }

    public static boolean isMarkedFullyNegated(LivingEntity entity) {
        return entity.getPersistentData().getLong(NEGATION_TICK_TAG) == entity.level().getGameTime();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelFullyNegatedKnockback(LivingKnockBackEvent event) {
        if (isMarkedFullyNegated(event.getEntity())) event.setCanceled(true);
    }
}
