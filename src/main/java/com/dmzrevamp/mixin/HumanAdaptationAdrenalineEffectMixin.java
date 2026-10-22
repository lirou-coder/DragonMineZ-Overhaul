package com.dmzrevamp.mixin;

import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.racial.impl.HumanAdaptation;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HumanAdaptation.class)
public abstract class HumanAdaptationAdrenalineEffectMixin {
    @Redirect(
            method = "triggerAdrenaline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
            ),
            remap = false
    )
    private static boolean dmzrevamp$skipTexturelessRevampAdrenalineEffect(
            ServerPlayer receiver,
            MobEffectInstance effect,
            ServerPlayer player,
            StatsData data,
            GeneralServerConfig.HumanRacialConfig config
    ) {
        String racialSkill = CustomRacialActionHelper.getConfiguredRacialSkillId(data);
        if ("humanrevamp".equalsIgnoreCase(racialSkill)
                || "bioandroidrevamp".equalsIgnoreCase(racialSkill)) {
            return false;
        }
        return receiver.addEffect(effect);
    }
}
