package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.forms.RequiredDmzLevelForm;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.FormRequisites;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Integrates Overhaul's level requirement into DMZ 2.2's purchase and tooltip pipeline. */
@Mixin(value = FormRequisites.class, remap = false)
public abstract class FormRequisitesRequiredDmzLevelMixin {
    @Unique
    private static final ThreadLocal<FormRequisites.Lock> DMZREVAMP_LEVEL_LOCK = new ThreadLocal<>();

    @Inject(method = "check", at = @At("RETURN"), cancellable = true)
    private static void dmzrevamp$includeRequiredDmzLevel(StatsData data, FormConfig.FormData form,
                                                          boolean clientSide,
                                                          CallbackInfoReturnable<FormRequisites.Lock> cir) {
        DMZREVAMP_LEVEL_LOCK.remove();
        if (data == null || !(form instanceof RequiredDmzLevelForm extension)) return;
        int required = Math.max(1, extension.dmzrevamp$getRequiredDMZLevel());
        if (required <= 1 || data.getLevel() >= required) return;

        FormRequisites.Lock current = cir.getReturnValue();
        FormRequisites.Lock combined = new FormRequisites.Lock(
                Math.max(required, current.level()), current.sagaId(), current.questId());
        DMZREVAMP_LEVEL_LOCK.set(combined);
        cir.setReturnValue(combined);
    }

    @Inject(method = "describe", at = @At("RETURN"), cancellable = true)
    private static void dmzrevamp$useRequiredLevelTooltip(FormRequisites.Lock lock, boolean clientSide,
                                                          CallbackInfoReturnable<List<Component>> cir) {
        FormRequisites.Lock marked = DMZREVAMP_LEVEL_LOCK.get();
        DMZREVAMP_LEVEL_LOCK.remove();
        if (lock == null || lock != marked || !lock.levelLocked()) return;
        List<Component> lines = new java.util.ArrayList<>(cir.getReturnValue());
        // DMZ appends its native level line after the optional quest line.
        if (!lines.isEmpty()) lines.remove(lines.size() - 1);
        lines.add(Component.translatable("gui.dmzrevamp.skills.required_dmz_level", lock.level()));
        cir.setReturnValue(lines);
    }
}
