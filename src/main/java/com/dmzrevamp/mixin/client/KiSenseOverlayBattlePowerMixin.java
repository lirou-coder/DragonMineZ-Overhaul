package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.KiSenseDangerStyle;
import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dmzrevamp.util.CompactNumberFormatter;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.gui.hud.HudSprites;
import com.dragonminez.client.gui.hud.KiSenseOverlay;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reapplies Overhaul danger styling to the DMZ 2.2 combat Ki Sense overlay. */
@Mixin(value = KiSenseOverlay.class, remap = false)
public abstract class KiSenseOverlayBattlePowerMixin {
    private static final float BP_SCALE = 0.85F;
    private static final float BORDER = HudSprites.DEFAULT_HP.border();
    private static final float WIDTH = HudSprites.DEFAULT_HP.width();

    @Inject(method = "drawBattlePower", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$renderDangerBattlePower(
            GuiGraphics graphics, Minecraft minecraft, int entityId, boolean player, CallbackInfo ci) {
        Entity raw = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        if (!(raw instanceof LivingEntity entity)) {
            return;
        }

        double battlePower = dmzrevamp$battlePower(entity, player);
        boolean hidden = !Double.isFinite(battlePower) || battlePower >= Float.MAX_VALUE;
        String text = hidden ? "BP: ???" : "BP: " + CompactNumberFormatter.format(battlePower, 10_000_000D).replace(",", ".");
        double ratio = dmzrevamp$ratio(minecraft, battlePower);
        int color = KiSenseDangerStyle.color(ratio);
        float scale = BP_SCALE * KiSenseDangerStyle.combatLabelScale(ratio);
        // Keep the lower edge anchored above the HP bar. Increasing the danger
        // scale therefore expands the label upward instead of through the bar.
        float y = -BORDER - 1.5F - minecraft.font.lineHeight * scale;
        HudRender.dmzText(graphics, text, WIDTH / 2.0F, y, scale, 0.5F, color, 1.0F);
        ci.cancel();
    }

    private static double dmzrevamp$battlePower(LivingEntity entity, boolean player) {
        if (ManualBattlePowerStatEvents.isKiSenseHiddenEntity(entity)) {
            return Double.POSITIVE_INFINITY;
        }
        if (player && entity instanceof Player target) {
            return StatsProvider.get(StatsCapability.INSTANCE, target)
                    .map(data -> data.getBattlePowerExact()).orElse(0D);
        }
        return AccurateMobBattlePowerCalculator.calculateCurvedBattlePowerExact(entity);
    }

    private static double dmzrevamp$ratio(Minecraft minecraft, double target) {
        double own = 0D;
        if (minecraft.player != null) {
            own = StatsProvider.get(StatsCapability.INSTANCE, minecraft.player)
                    .map(data -> data.getBattlePowerExact()).orElse(0D);
        }
        if (!Double.isFinite(own) || own <= 0D || !Double.isFinite(target) || target < 0D) {
            return 0D;
        }
        return target / own;
    }
}
