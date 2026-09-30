package com.dmzrevamp.mixin;

import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.commands.StatsCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(StatsCommand.class)
public abstract class StatsCommandSpdAliasMixin {
    private static final ThreadLocal<Boolean> DMZREVAMP_PRESTIGE_SET_ALL_MAX =
            ThreadLocal.withInitial(() -> false);

    @Redirect(
            method = "modifyStats",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;toUpperCase()Ljava/lang/String;"),
            remap = false
    )
    private static String dmzrevamp$mapSpdAliasToSkp(String stat) {
        String upper = stat.toUpperCase();
        return "SPD".equals(upper) ? "SKP" : upper;
    }

    @Inject(method = "modifyStats", at = @At("HEAD"), remap = false)
    private static void dmzrevamp$beginPrestigeSetAllMax(CommandSourceStack source, String stat,
                                                         String amount, Collection<ServerPlayer> targets,
                                                         String mode, CallbackInfoReturnable<Integer> cir) {
        DMZREVAMP_PRESTIGE_SET_ALL_MAX.set(LevelingRevampConfig.prestigeEnabled()
                && "ALL".equalsIgnoreCase(stat)
                && "max".equalsIgnoreCase(amount)
                && "set".equalsIgnoreCase(mode));
    }

    @Inject(method = "modifyStats", at = @At("RETURN"), remap = false)
    private static void dmzrevamp$endPrestigeSetAllMax(CommandSourceStack source, String stat,
                                                       String amount, Collection<ServerPlayer> targets,
                                                       String mode, CallbackInfoReturnable<Integer> cir) {
        DMZREVAMP_PRESTIGE_SET_ALL_MAX.remove();
    }

    @Inject(method = "applyModification", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$setPrestigeMaximumPerAttribute(StatsData data, String stat,
                                                                int ignoredValue, String mode,
                                                                CallbackInfo ci) {
        if (!DMZREVAMP_PRESTIGE_SET_ALL_MAX.get()) return;

        var levels = LevelingRevampConfig.get().levelsAndAttributes;
        int progressionMaximum = levels.maxAttribute < 0
                ? PrestigeSystem.levelCap(data)
                : PrestigeSystem.effectiveMaximumAttribute(data);
        long target = (long) Math.max(0, progressionMaximum)
                + DmzRevampHelper.getInitialStatValue(data, stat);
        data.getStats().setStat(stat, (int) Math.min(Integer.MAX_VALUE, Math.max(0L, target)));
        ci.cancel();
    }
}
