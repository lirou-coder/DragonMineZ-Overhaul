package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.TournamentFighterStatsExtension;
import com.dragonminez.common.network.TournamentPackets;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = TournamentPackets.OpenBracketS2C.class, remap = false)
public abstract class TournamentFighterStatsPacketMixin {
    @Inject(method = "encode", at = @At("RETURN"))
    private static void dmzrevamp$encodeConfiguredFighterStats(TournamentPackets.OpenBracketS2C message,
                                                               FriendlyByteBuf buffer, CallbackInfo ci) {
        Map<String, TournamentPackets.OpenBracketS2C.FighterStats> stats = message.getStats();
        int count = 0;
        for (TournamentPackets.OpenBracketS2C.FighterStats fighterStats : stats.values()) {
            TournamentFighterStatsExtension extra = (TournamentFighterStatsExtension) (Object) fighterStats;
            if (extra.dmzrevamp$getDefense() != null || extra.dmzrevamp$getMovementSpeed() != null) count++;
        }

        buffer.writeBoolean(true);
        buffer.writeVarInt(count);
        for (Map.Entry<String, TournamentPackets.OpenBracketS2C.FighterStats> entry : stats.entrySet()) {
            TournamentFighterStatsExtension extra = (TournamentFighterStatsExtension) (Object) entry.getValue();
            Double defense = extra.dmzrevamp$getDefense();
            Double movementSpeed = extra.dmzrevamp$getMovementSpeed();
            if (defense == null && movementSpeed == null) continue;
            buffer.writeUtf(entry.getKey());
            buffer.writeBoolean(defense != null);
            if (defense != null) buffer.writeDouble(defense);
            buffer.writeBoolean(movementSpeed != null);
            if (movementSpeed != null) buffer.writeDouble(movementSpeed);
        }
    }

    @Inject(method = "decode", at = @At("RETURN"))
    private static void dmzrevamp$decodeConfiguredFighterStats(FriendlyByteBuf buffer,
                                                               CallbackInfoReturnable<TournamentPackets.OpenBracketS2C> cir) {
        if (!buffer.isReadable() || !buffer.readBoolean()) return;
        int count = Math.min(buffer.readVarInt(), 4096);
        TournamentPackets.OpenBracketS2C message = cir.getReturnValue();
        for (int i = 0; i < count && buffer.isReadable(); i++) {
            String id = buffer.readUtf();
            boolean hasDefense = buffer.readBoolean();
            Double defense = hasDefense ? buffer.readDouble() : null;
            boolean hasMovementSpeed = buffer.readBoolean();
            Double movementSpeed = hasMovementSpeed ? buffer.readDouble() : null;
            TournamentPackets.OpenBracketS2C.FighterStats fighterStats = message.getStats().get(id);
            if (fighterStats != null) {
                TournamentFighterStatsExtension extra = (TournamentFighterStatsExtension) (Object) fighterStats;
                extra.dmzrevamp$setDefense(defense);
                extra.dmzrevamp$setMovementSpeed(movementSpeed);
            }
        }
    }
}
