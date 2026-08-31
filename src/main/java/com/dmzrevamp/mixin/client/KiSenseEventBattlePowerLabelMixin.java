package com.dmzrevamp.mixin.client;

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
            if (Float.isFinite(cached) && cached >= Integer.MAX_VALUE) {
                return original;
            }
            if (Float.isFinite(cached) && cached > 0F && cached < Float.MAX_VALUE) {
                return "BP: " + formatBattlePower(cached);
            }
            return original;
        }
        String numeric = original.substring(4).replace(".", "");
        try {
            double value = Double.parseDouble(numeric);
            if (value >= Integer.MAX_VALUE) {
                return original;
            }
            return "BP: " + formatBattlePower(value);
        } catch (NumberFormatException ignored) {
            return original;
        }
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
        if (ratio >= 1.75D) return 0xFF5555;
        if (ratio >= 1.25D) return 0xFFAA00;
        if (ratio >= 0.75D) return 0xFFFF55;
        return 0x55FFFF;
    }

    private static float dmzrevamp$dangerScale(LivingEntity entity) {
        if (ModList.get().isLoaded(DMZ_COMBAT_REMADE)) {
            return 1.0F;
        }
        double ratio = dmzrevamp$battlePowerRatio(entity);
        if (ratio >= 1.75D) return 1.75F;
        if (ratio >= 1.25D) return 1.4F;
        if (ratio >= 0.75D) return 1.2F;
        return 1.0F;
    }

    private static double dmzrevamp$battlePowerRatio(LivingEntity entity) {
        float own = KiSenseScan.getMyBP();
        float target = entity == null ? 0F : KiSenseScan.getCachedBP(entity.getId());
        if (!Float.isFinite(own) || own <= 0F || !Float.isFinite(target) || target < 0F) {
            return 0D;
        }
        return target / (double) own;
    }
}
