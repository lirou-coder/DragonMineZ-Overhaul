package com.dmzrevamp.mixin;

import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.UUID;

@Mixin(value = StrikeAttackHandler.class, remap = false)
public interface StrikeAttackHandlerStateAccessor {
    @Accessor("ACTIVE")
    static Map<UUID, Object> dmzrevamp$getActiveStrikes() {
        throw new AssertionError();
    }

    @Accessor("PENDING")
    static Map<UUID, Object> dmzrevamp$getPendingStrikes() {
        throw new AssertionError();
    }

    @Invoker("applyStrikeDamage")
    static void dmzrevamp$invokeApplyStrikeDamage(ServerPlayer player, LivingEntity target,
                                                   double damage, String techniqueId, boolean finalHit) {
        throw new AssertionError();
    }
}
