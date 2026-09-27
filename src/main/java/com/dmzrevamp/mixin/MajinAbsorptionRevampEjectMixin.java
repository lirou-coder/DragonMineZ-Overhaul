package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dmzrevamp.racial.impl.MajinRevampRacialSkill;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.impl.MajinAbsorption;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MajinAbsorption.class, remap = false)
public abstract class MajinAbsorptionRevampEjectMixin {
    @Inject(method = "ejectSlot", at = @At("HEAD"), cancellable = true)
    private static void dmzrevamp$ejectRevampSlot(ServerPlayer player, StatsData data, int index, CallbackInfo ci) {
        if (!"majinrevamp".equalsIgnoreCase(CustomRacialActionHelper.getConfiguredRacialSkillId(data))) return;
        ci.cancel();
        if (index < 0 || index >= data.getRacialData().getAbsorptions().size()) return;
        RacialData.AbsorptionSlot slot = data.getRacialData().getAbsorptions().remove(index);
        slot.grantedStats().forEach((stat, granted) -> {
            double current = data.getBonusStats().getBonuses(stat).stream()
                    .filter(bonus -> MajinRevampRacialSkill.ABSORPTION_BONUS_KEY.equals(bonus.name))
                    .mapToDouble(bonus -> bonus.value).sum();
            data.getBonusStats().removeBonus(stat, MajinRevampRacialSkill.ABSORPTION_BONUS_KEY);
            double remaining = Math.max(0D, current - Math.max(0, granted));
            if (remaining > 0D) {
                data.getBonusStats().addBonus(stat, MajinRevampRacialSkill.ABSORPTION_BONUS_KEY,
                        "+", remaining, true);
            }
        });
        data.getRacialData().removeOwnedBonusName(slot.bonusName());
        int uses = Math.max(0, player.getPersistentData().getInt(MajinRevampRacialSkill.ABSORPTION_USES_TAG) - 1);
        player.getPersistentData().putInt(MajinRevampRacialSkill.ABSORPTION_USES_TAG, uses);
        data.getResources().setRacialSkillCount(Math.max(0, data.getResources().getRacialSkillCount() - 1));
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }
}
