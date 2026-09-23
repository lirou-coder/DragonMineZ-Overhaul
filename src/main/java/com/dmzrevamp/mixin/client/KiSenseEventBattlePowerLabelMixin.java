package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.KiSenseDangerStyle;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.client.events.KiSenseEvent;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.fml.ModList;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(value = KiSenseEvent.class, remap = false)
public abstract class KiSenseEventBattlePowerLabelMixin {
    private static final String DMZ_COMBAT_REMADE = "dmzcombatremade";
    private static final ThreadLocal<LivingEntity> DMZREVAMP_RENDERED_BP_ENTITY = new ThreadLocal<>();

    @Inject(method = "renderBPLabel", at = @At("HEAD"), remap = false, require = 0)
    private static void dmzrevamp$captureRenderedBattlePowerEntity(
            PoseStack poseStack,
            LivingEntity entity,
            float topY,
            CallbackInfo ci
    ) {
        DMZREVAMP_RENDERED_BP_ENTITY.set(entity);
    }

    @Inject(method = "renderBPLabel", at = @At("RETURN"), remap = false, require = 0)
    private static void dmzrevamp$releaseRenderedBattlePowerEntity(
            PoseStack poseStack,
            LivingEntity entity,
            float topY,
            CallbackInfo ci
    ) {
        DMZREVAMP_RENDERED_BP_ENTITY.remove();
    }

    @ModifyVariable(method = "renderBPLabel", at = @At("STORE"), ordinal = 0, require = 0)
    private static String dmzrevamp$compactLargeNpcBattlePower(String original, PoseStack poseStack, LivingEntity entity, float topY) {
        if (!original.startsWith("BP: ")) {
            return original;
        }
        if ("BP: ???".equals(original)) {
            if (ManualBattlePowerStatEvents.isKiSenseHiddenEntity(entity)) {
                return original;
            }

            long override = ManualBattlePowerStatEvents.displayedBattlePower(entity, -1L);
            if (override > 0L) {
                return "BP: " + formatBattlePower(override);
            }

            float cached = KiSenseScan.getCachedBP(entity.getId());
            if (Float.isFinite(cached) && cached > 0F && cached < Float.MAX_VALUE) {
                return "BP: " + formatBattlePower(cached);
            }
            return original;
        }
        String numeric = original.substring(4).replace(".", "");
        try {
            double value = Double.parseDouble(numeric);
            return "BP: " + formatBattlePower(value);
        } catch (NumberFormatException ignored) {
            return original;
        }
    }

    @ModifyVariable(method = "renderBPLabel", at = @At("STORE"), ordinal = 0, require = 0)
    private static boolean dmzrevamp$usePlayerBattlePowerLimitForMobs(boolean original,
                                                                      PoseStack poseStack,
                                                                      LivingEntity entity,
                                                                      float topY) {
        // DMZ normally checks Integer.MAX_VALUE for mobs. Their scan now follows the player
        // float path, so the hidden/overflow sentinel must also be Float.MAX_VALUE.
        return true;
    }

    private static String formatBattlePower(double value) {
        if (value < 10_000_000D) {
            return String.format(Locale.ROOT, "%,.0f", value).replace(",", ".");
        }
        if (value >= 1_000_000_000_000D) {
            return trim(value / 1_000_000_000_000D) + "T";
        }
        if (value >= 1_000_000_000D) {
            return trim(value / 1_000_000_000D) + "B";
        }
        return trim(value / 1_000_000D) + "M";
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    @ModifyConstant(
            method = "renderBPLabel",
            constant = @Constant(floatValue = 0.6F),
            remap = false,
            require = 0
    )
    private static float dmzrevamp$dangerLevelBattlePowerScale(float original) {
        return original * dmzrevamp$dangerScale(DMZREVAMP_RENDERED_BP_ENTITY.get());
    }

    @ModifyArg(
            method = "renderBPLabel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/events/KiSenseEvent;drawText(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/MutableComponent;FFIFLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"
            ),
            index = 4,
            remap = false,
            require = 0
    )
    private static int dmzrevamp$dangerLevelBattlePowerColor(int original) {
        if (ModList.get().isLoaded(DMZ_COMBAT_REMADE)) {
            return original;
        }
        double ratio = dmzrevamp$battlePowerRatio(DMZREVAMP_RENDERED_BP_ENTITY.get());
        return KiSenseDangerStyle.color(ratio);
    }

    private static float dmzrevamp$dangerScale(LivingEntity entity) {
        if (ModList.get().isLoaded(DMZ_COMBAT_REMADE)) {
            return 1.0F;
        }
        double ratio = dmzrevamp$battlePowerRatio(entity);
        return KiSenseDangerStyle.combatLabelScale(ratio);
    }

    private static double dmzrevamp$battlePowerRatio(LivingEntity entity) {
        float own = KiSenseScan.getMyBP();
        float target = entity == null ? 0F : KiSenseScan.getCachedBP(entity.getId());
        return KiSenseDangerStyle.ratio(own, target);
    }
}
