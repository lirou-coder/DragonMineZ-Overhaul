package com.dmzrevamp.mixin;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.revamp.prestige.PrestigeDataAccess;
import com.dmzrevamp.revamp.strike.RaceExclusiveStrikeEvents;
import com.dmzrevamp.revamp.strike.StrikeAttackTemplates;
import com.dmzrevamp.racial.RevampRacialResetHelper;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatsData.class)
public abstract class StatsDataPrestigeDataMixin implements PrestigeDataAccess {
    @Unique
    private static final String DMZREVAMP_PRESTIGE_COUNT = "DmzRevampPrestigeCount";
    @Unique
    private static final String DMZREVAMP_PRESTIGE_ELIGIBLE = "DmzRevampPrestigeEligible";
    @Unique
    private int dmzrevamp$prestigeCount;
    @Unique
    private boolean dmzrevamp$prestigeEligibilityKnown;
    @Unique
    private boolean dmzrevamp$prestigeEligible;

    @Override
    public int dmzrevamp$getPrestigeCount() {
        return Math.max(0, dmzrevamp$prestigeCount);
    }

    @Override
    public void dmzrevamp$setPrestigeCount(int count) {
        dmzrevamp$prestigeCount = Math.max(0, count);
    }

    @Override
    public boolean dmzrevamp$isPrestigeEligibilityKnown() {
        return dmzrevamp$prestigeEligibilityKnown;
    }

    @Override
    public boolean dmzrevamp$isPrestigeEligible() {
        return dmzrevamp$prestigeEligible;
    }

    @Override
    public void dmzrevamp$setPrestigeEligibility(boolean known, boolean eligible) {
        dmzrevamp$prestigeEligibilityKnown = known;
        dmzrevamp$prestigeEligible = eligible;
    }

    @Inject(method = "save", at = @At("RETURN"), remap = false)
    private void dmzrevamp$savePrestige(CallbackInfoReturnable<CompoundTag> cir) {
        boolean eligible = com.dmzrevamp.revamp.prestige.PrestigeSystem
                .canPrestigeAuthoritative((StatsData) (Object) this);
        dmzrevamp$setPrestigeEligibility(true, eligible);
        cir.getReturnValue().putInt(DMZREVAMP_PRESTIGE_COUNT, dmzrevamp$getPrestigeCount());
        cir.getReturnValue().putBoolean(DMZREVAMP_PRESTIGE_ELIGIBLE, eligible);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = false)
    private void dmzrevamp$loadPrestige(CompoundTag tag, CallbackInfo ci) {
        // ProgressionSyncS2C deliberately sends a partial StatsData tag and then
        // reuses StatsData.load(). Missing data in that packet means "unchanged",
        // not zero. Only a full save/sync carrying our key may replace Prestige.
        if (tag != null && tag.contains(DMZREVAMP_PRESTIGE_COUNT, Tag.TAG_ANY_NUMERIC)) {
            dmzrevamp$setPrestigeCount(tag.getInt(DMZREVAMP_PRESTIGE_COUNT));
        }
        if (tag != null && tag.contains(DMZREVAMP_PRESTIGE_ELIGIBLE, Tag.TAG_BYTE)) {
            dmzrevamp$setPrestigeEligibility(true, tag.getBoolean(DMZREVAMP_PRESTIGE_ELIGIBLE));
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"), remap = false)
    private void dmzrevamp$copyPrestige(StatsData oldData, CallbackInfo ci) {
        PrestigeDataAccess oldAccess = (PrestigeDataAccess) oldData;
        dmzrevamp$setPrestigeCount(oldAccess.dmzrevamp$getPrestigeCount());
        dmzrevamp$setPrestigeEligibility(
                oldAccess.dmzrevamp$isPrestigeEligibilityKnown(),
                oldAccess.dmzrevamp$isPrestigeEligible()
        );
    }

    @Inject(method = "resetPlayerProgress", at = @At("TAIL"), remap = false)
    private void dmzrevamp$protectCharacterReset(
            net.minecraft.server.level.ServerPlayer player,
            Integer keepPercentage,
            boolean keepSkills,
            boolean restoreSaiyanTail,
            CallbackInfo ci
    ) {
        StatsData data = (StatsData) (Object) this;
        // Race-exclusive techniques must never leak into the next character,
        // even when the command is configured to preserve ordinary skills.
        // The racial passive skills themselves still obey DMZ's keepSkills option.
        data.getTechniques().removeTechnique(StrikeAttackTemplates.ANDROID_ABSORPTION);
        data.getTechniques().removeTechnique(RaceExclusiveStrikeEvents.SLEEP_RECOVERY_ID);
        data.getTechniques().removeTechnique(StrikeAttackTemplates.NAMEKIAN_REGENERATION);
        RevampRacialResetHelper.resetAll(player, data);
        if (!keepSkills) {
            DmzSkillProgressionCompat.resetProgression(player, true);
        }

        // A literal 100% stat reset is the only reset variant that preserves
        // rebirth progress. Null and every percentage below 100 are full resets.
        if (keepPercentage == null || keepPercentage != 100) {
            dmzrevamp$setPrestigeCount(0);
        }
    }
}
