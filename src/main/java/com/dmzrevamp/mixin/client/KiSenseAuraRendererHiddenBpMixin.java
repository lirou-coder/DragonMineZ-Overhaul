package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.KiSenseDangerStyle;
import com.dmzrevamp.client.AuraStyleCacheEntry;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(value = KiSenseAuraRenderer.class, remap = false)
public abstract class KiSenseAuraRendererHiddenBpMixin {
    private static final ThreadLocal<Double> DMZREVAMP_AURA_BP_RATIO = ThreadLocal.withInitial(() -> 0D);
    private static final Map<Integer, AuraStyleCacheEntry> DMZREVAMP_AURA_STYLE_CACHE = new HashMap<>();
    private static Set<Integer> dmzrevamp$visibleAuraIds = Set.of();
    private static Object dmzrevamp$visibleAuraLevel;
    private static long dmzrevamp$visibleAuraTick = Long.MIN_VALUE;

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

        long gameTime = minecraft.level.getGameTime();
        if (dmzrevamp$visibleAuraLevel == minecraft.level && dmzrevamp$visibleAuraTick == gameTime) {
            return dmzrevamp$visibleAuraIds;
        }

        Set<Integer> searchEntities = KiSenseScan.getSearchEntities();
        Set<Integer> visible = null;
        for (int id : searchEntities) {
            Entity entity = minecraft.level.getEntity(id);
            if (entity instanceof LivingEntity livingEntity && ManualBattlePowerStatEvents.isKiSenseHiddenEntity(livingEntity)) {
                if (visible == null) visible = new HashSet<>(searchEntities);
                visible.remove(id);
            }
        }
        dmzrevamp$visibleAuraLevel = minecraft.level;
        dmzrevamp$visibleAuraTick = gameTime;
        dmzrevamp$visibleAuraIds = visible == null ? searchEntities : visible;
        return dmzrevamp$visibleAuraIds;
    }


    @Inject(method = "auraColors", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dmzrevamp$useDangerBattlePowerAuraColors(LivingEntity entity,
                                                                  double ratio,
                                                                  CallbackInfoReturnable<float[][]> cir) {
        // The renderer can run many times between game ticks (and with shader packs it draws
        // three aura layers per target). Reuse exact BP/style work for the same entity for five
        // ticks; the displayed danger bands do not need frame-rate updates.
        double effectiveRatio = ratio;
        if (entity != null && !(entity instanceof net.minecraft.world.entity.player.Player)) {
            Minecraft minecraft = Minecraft.getInstance();
            long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
            int entityId = entity.getId();
            AuraStyleCacheEntry cached = DMZREVAMP_AURA_STYLE_CACHE.get(entityId);
            if (cached == null || cached.entity != entity || gameTime - cached.calculatedAt >= 5L) {
                double target = AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(entity);
                double own = Math.max(1D, KiSenseScan.getMyBP());
                effectiveRatio = Double.isFinite(target) && target >= 0D ? target / own : 0D;
                cached = new AuraStyleCacheEntry(entity, gameTime, gameTime, effectiveRatio,
                        KiSenseDangerStyle.auraColors(effectiveRatio));
                DMZREVAMP_AURA_STYLE_CACHE.put(entityId, cached);
            } else {
                cached.lastSeenAt = gameTime;
                effectiveRatio = cached.ratio;
            }
            cached.lastSeenAt = gameTime;
            DMZREVAMP_AURA_BP_RATIO.set(effectiveRatio);
            cir.setReturnValue(cached.colors);
            return;
        }
        DMZREVAMP_AURA_BP_RATIO.set(effectiveRatio);
        cir.setReturnValue(KiSenseDangerStyle.auraColors(effectiveRatio));
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
        if (minecraft.level != null && minecraft.level.getGameTime() % 100L == 0L) {
            long cutoff = minecraft.level.getGameTime() - 100L;
            Iterator<Map.Entry<Integer, AuraStyleCacheEntry>> iterator = DMZREVAMP_AURA_STYLE_CACHE.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().lastSeenAt < cutoff) iterator.remove();
            }
        }
    }

}
