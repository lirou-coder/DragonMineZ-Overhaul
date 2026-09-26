package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ClientStrikeClashState;
import com.dmzrevamp.revamp.ki.ConfiguredClashMeter;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.common.combat.clash.ClashMeter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses the Strike-specific meter parameters when creating the input packet. */
@Mixin(value = ClientBeamClashState.class, remap = false)
public abstract class ClientBeamClashStateMeterMixin {
    @Redirect(method = "onLocalPress", at = @At(value = "INVOKE",
            target = "Lcom/dragonminez/common/combat/clash/ClashMeter;sample(JF)Lcom/dragonminez/common/combat/clash/ClashMeter$Sample;"))
    private static ClashMeter.Sample dmzrevamp$sampleActiveClash(long seed, float time) {
        return ClientStrikeClashState.isActive()
                ? ConfiguredClashMeter.sampleStrike(seed, time, ClientStrikeClashState.goodAreaMultiplier(),
                        ClientStrikeClashState.meterSpeedMultiplier(), ClientStrikeClashState.areaSizeMultiplier(),
                        ClientStrikeClashState.perfectFraction(), ClientStrikeClashState.minimumGoodEfficiency())
                : ConfiguredClashMeter.sampleKi(seed, time,
                        com.dmzrevamp.client.ClientKiClashMeterState.speedMultiplier(),
                        com.dmzrevamp.client.ClientKiClashMeterState.areaSizeMultiplier(),
                        com.dmzrevamp.client.ClientKiClashMeterState.perfectFraction(),
                        com.dmzrevamp.client.ClientKiClashMeterState.minimumGoodEfficiency());
    }
}
