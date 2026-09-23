package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.KiSenseDangerStyle;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.client.render.effects.KiSenseAuraRenderer;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = KiSenseAuraRenderer.class, remap = false)
public abstract class KiSenseAuraRendererHiddenBpMixin {
    private static final ThreadLocal<Double> DMZREVAMP_AURA_BP_RATIO = ThreadLocal.withInitial(() -> 0D);

    @Redirect(
            method = "renderAuras",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/systems/kisense/KiSenseScan;getSearchEntities()Ljava/util/Set;"),
            require = 0
    )
    private static Set<Integer> dmzrevamp$hideUnknownBattlePowerAuras() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return KiSenseScan.getSearchEntities();
        }

        Set<Integer> visible = new HashSet<>();
        for (int id : KiSenseScan.getSearchEntities()) {
            Entity entity = minecraft.level.getEntity(id);
            if (!(entity instanceof LivingEntity livingEntity) || !ManualBattlePowerStatEvents.isKiSenseHiddenEntity(livingEntity)) {
                visible.add(id);
            }
        }
        return visible;
    }


    @Inject(method = "auraColors", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dmzrevamp$useDangerBattlePowerAuraColors(LivingEntity entity,
                                                                  double ratio,
                                                                  CallbackInfoReturnable<float[][]> cir) {
        // Deliberately ignore hostile/passive/neutral classification. If Ki Sense scanned the
        // LivingEntity, its aura color and scale are determined only by this exact BP ratio.
        DMZREVAMP_AURA_BP_RATIO.set(ratio);
        cir.setReturnValue(KiSenseDangerStyle.auraColors(ratio));
    }

    @Redirect(
            method = "renderAuras",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;m_14008_(DDD)D"),
            remap = false,
            require = 0
    )
    private static double dmzrevamp$useDangerBattlePowerAuraScale(double value, double min, double max) {
        return KiSenseDangerStyle.searchAuraScale(DMZREVAMP_AURA_BP_RATIO.get());
    }

    @Inject(method = "renderAuras", at = @At("RETURN"), remap = false, require = 0)
    private static void dmzrevamp$clearAuraBattlePowerRatio(Minecraft minecraft,
                                                            net.minecraftforge.client.event.RenderLevelStageEvent event,
                                                            boolean iris,
                                                            CallbackInfo ci) {
        DMZREVAMP_AURA_BP_RATIO.remove();
    }
}
