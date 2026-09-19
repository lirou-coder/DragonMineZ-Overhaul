package com.dmzrevamp.mixin;

import com.dmzrevamp.config.ExtraDifficultiesConfig;
import com.dmzrevamp.revamp.quest.ExtraDifficulties;
import com.dragonminez.common.quest.Difficulty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Arrays;

@Mixin(value = Difficulty.class, remap = false)
public abstract class DifficultyExtraValuesMixin {
    @Shadow @Final @Mutable private static Difficulty[] $VALUES;

    @Invoker("<init>")
    private static Difficulty dmzrevamp$newDifficulty(String name, int ordinal) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void dmzrevamp$appendDifficulties(CallbackInfo ci) {
        if (Arrays.stream($VALUES).anyMatch(value -> ExtraDifficulties.NIGHTMARE.equals(value.name()))) return;
        Difficulty[] expanded = Arrays.copyOf($VALUES, $VALUES.length + 2);
        expanded[expanded.length - 2] = dmzrevamp$newDifficulty(ExtraDifficulties.NIGHTMARE, expanded.length - 2);
        expanded[expanded.length - 1] = dmzrevamp$newDifficulty(ExtraDifficulties.YOU_MUST_DIE, expanded.length - 1);
        $VALUES = expanded;
    }

    @Inject(method = "hpMultiplier", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$hp(CallbackInfoReturnable<Double> cir) {
        if (ExtraDifficulties.isExtra((Difficulty) (Object) this))
            cir.setReturnValue(ExtraDifficultiesConfig.values(((Difficulty) (Object) this).name()).hpMultiplier);
    }

    @Inject(method = "damageMultiplier", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$damage(CallbackInfoReturnable<Double> cir) {
        if (ExtraDifficulties.isExtra((Difficulty) (Object) this))
            cir.setReturnValue(ExtraDifficultiesConfig.values(((Difficulty) (Object) this).name()).damageMultiplier);
    }

    @Inject(method = "tpMultiplier", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$tp(CallbackInfoReturnable<Double> cir) {
        if (ExtraDifficulties.isExtra((Difficulty) (Object) this))
            cir.setReturnValue(ExtraDifficultiesConfig.values(((Difficulty) (Object) this).name()).tpMultiplier);
    }

    @Inject(method = "questRewardMultiplier", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$reward(CallbackInfoReturnable<Double> cir) {
        if (ExtraDifficulties.isExtra((Difficulty) (Object) this))
            cir.setReturnValue(ExtraDifficultiesConfig.values(((Difficulty) (Object) this).name()).questRewardMultiplier);
    }

    @Inject(method = "aiTierId", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$aiTier(CallbackInfoReturnable<Integer> cir) {
        String name = ((Difficulty) (Object) this).name();
        if (ExtraDifficulties.NIGHTMARE.equals(name)) cir.setReturnValue(4);
        else if (ExtraDifficulties.YOU_MUST_DIE.equals(name)) cir.setReturnValue(5);
    }
}
