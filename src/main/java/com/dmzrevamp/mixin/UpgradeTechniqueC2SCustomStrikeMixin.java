package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dragonminez.common.network.C2S.UpgradeTechniqueC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UpgradeTechniqueC2S.class, priority = 1100)
public abstract class UpgradeTechniqueC2SCustomStrikeMixin {
    @Shadow(remap = false)
    @Final
    private String techniqueId;

    @Shadow(remap = false)
    @Final
    private String statType;

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$upgradeCustomStrike(ServerPlayer player, StatsData data, CallbackInfo ci) {
        TechniqueData technique = data.getTechniques().getUnlockedTechniques().get(techniqueId);
        if (!(technique instanceof StrikeAttackData strike) || !(strike instanceof RevampStrikeAttackData revamp) || !revamp.dmzrevamp$isCustomStrike()) {
            return;
        }

        if (!strike.canUpgradeStat(statType)) {
            ci.cancel();
            return;
        }

        int cost = strike.getUpgradeXpCost(statType);
        if (strike.getExperience() < cost) {
            ci.cancel();
            return;
        }

        strike.setExperience(strike.getExperience() - cost);
        switch (statType) {
            case "damage" -> {
                strike.setDamageMultiplier(strike.getDamageMultiplier() + 0.075F);
                strike.setDamageLevel(strike.getDamageLevel() + 1);
            }
            case "cooldown" -> strike.setCooldownLevel(strike.getCooldownLevel() + 1);
            case "speed" -> {
                revamp.dmzrevamp$setDashSpeedMultiplier(Mth.clamp(revamp.dmzrevamp$getDashSpeedMultiplier() + 0.05F, 0.1F, 1.5F));
                revamp.dmzrevamp$setSpeedLevel(revamp.dmzrevamp$getSpeedLevel() + 1);
            }
            case "armor_pen" -> {
                revamp.dmzrevamp$setArmorPenetration(revamp.dmzrevamp$getArmorPenetration() + 1);
                revamp.dmzrevamp$setArmorPenLevel(revamp.dmzrevamp$getArmorPenLevel() + 1);
            }
            default -> {
                ci.cancel();
                return;
            }
        }

        NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
        ci.cancel();
    }
}
