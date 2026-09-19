package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.battlepower.QuestPreviewBattlePowerCalculator;
import com.dmzrevamp.revamp.battlepower.QuestPreviewExtraStatsResolver;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.client.gui.quest.preview.QuestEnemyPreview;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Locale;

@Mixin(value = QuestEnemyPreview.class, remap = false)
public abstract class QuestEnemyPreviewRevampStatsMixin {
    private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");

    @Shadow
    private Quest boundQuest;

    @Shadow
    private int currentIndex;

    @Shadow
    private Difficulty boundDifficulty;

    @Shadow
    private int boundPartySize;

    @Inject(method = "renderStatsCard", at = @At("HEAD"), require = 0)
    private void dmzrevamp$capturePreviewEntity(
            GuiGraphics graphics,
            Font font,
            LivingEntity entity,
            int regionX,
            int regionY,
            int regionW,
            int regionH,
            int baseAlpha,
            CallbackInfo ci
    ) {
        dmzrevamp$currentPreviewEntity = entity;
    }

    @org.spongepowered.asm.mixin.Unique
    private LivingEntity dmzrevamp$currentPreviewEntity;

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "renderStatsCard",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/quest/preview/QuestEnemyPreview;abbreviate(J)Ljava/lang/String;"
            ),
            require = 0
    )
    private String dmzrevamp$useCustomQuestBattlePower(long originalBattlePower) {
        if (ManualBattlePowerStatEvents.isKiSenseHiddenEntity(dmzrevamp$currentPreviewEntity)) {
            return "???";
        }
        long calculatedBattlePower = QuestPreviewBattlePowerCalculator.calculate(
                boundQuest,
                dmzrevamp$currentKillObjective(),
                boundDifficulty,
                boundPartySize,
                dmzrevamp$currentPreviewEntity,
                dmzrevamp$viewerStats()
        );
        return dmzrevamp$abbreviateBattlePower(calculatedBattlePower > 0L ? calculatedBattlePower : originalBattlePower);
    }

    private static StatsData dmzrevamp$viewerStats() {
        return Minecraft.getInstance().player == null ? null
                : StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).resolve().orElse(null);
    }

    @Inject(
            method = "renderStatsCard",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lcom/dragonminez/client/gui/quest/preview/QuestEnemyPreview;collectThreats(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;",
                    shift = At.Shift.AFTER
            ),
            require = 0,
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void dmzrevamp$addQuestAttributeRows(
            GuiGraphics graphics,
            Font font,
            LivingEntity entity,
            int regionX,
            int regionY,
            int regionW,
            int regionH,
            int baseAlpha,
            CallbackInfo ci,
            float ease,
            int cardAlpha,
            @Coerce Object target,
            List<Component> lines,
            List<Component> threats
    ) {
        KillObjective objective = dmzrevamp$currentKillObjective();
        if (objective == null) {
            return;
        }
        QuestPreviewExtraStatsResolver.ExtraStats data = QuestPreviewBattlePowerCalculator.extraStats(boundQuest, objective);

        // Only explicit, positive quest fields belong in this card. Entity defaults and
        // zero-valued placeholders must not create rows that the pack author did not request.
        if (data.defense != null && data.defense > 0D) {
            double defense = data.defense * (boundDifficulty == null ? 1D : boundDifficulty.damageMultiplier());
            lines.add(dmzrevamp$stat("gui.dmzrevamp.quest_tree.preview.defense", dmzrevamp$formatNumber(defense), 0x55FFFF));
        }
        if (data.armor != null && data.armor > 0D) {
            lines.add(dmzrevamp$stat("gui.dmzrevamp.quest_tree.preview.armor", dmzrevamp$formatNumber(data.armor), 0x55FFFF));
        }
        if (data.armorToughness != null && data.armorToughness > 0D) {
            lines.add(dmzrevamp$stat("gui.dmzrevamp.quest_tree.preview.armor_toughness", dmzrevamp$formatNumber(data.armorToughness), 0x55FFFF));
        }
        if (data.protection != null && data.protection > 0D) {
            lines.add(dmzrevamp$stat("gui.dmzrevamp.quest_tree.preview.protection", dmzrevamp$formatNumber(data.protection), 0x55FFFF));
        }
        if (data.movementSpeed != null && data.movementSpeed > 0D) {
            lines.add(dmzrevamp$stat(
                    "gui.dmzrevamp.quest_tree.preview.movement_speed",
                    dmzrevamp$formatNumber((data.movementSpeed / 0.2D) * 100D) + "%",
                    0x55FFFF
            ));
        }
    }

    private KillObjective dmzrevamp$currentKillObjective() {
        if (boundQuest == null || currentIndex < 0) {
            return null;
        }

        int killIndex = 0;
        for (QuestObjective objective : boundQuest.getObjectives()) {
            if (objective instanceof KillObjective killObjective) {
                if (killIndex == currentIndex) {
                    return killObjective;
                }
                killIndex++;
            }
        }
        return null;
    }

    private static String dmzrevamp$abbreviateBattlePower(long value) {
        if (value >= 1_000_000_000L) {
            return dmzrevamp$trim(value / 1_000_000_000D) + "B";
        }
        if (value >= 1_000_000L) {
            return dmzrevamp$trim(value / 1_000_000D) + "M";
        }
        if (value >= 10_000L) {
            return dmzrevamp$trim(value / 1_000D) + "K";
        }
        return String.format("%,d", value);
    }

    private static String dmzrevamp$trim(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }

    private static MutableComponent dmzrevamp$stat(String key, String value, int valueColor) {
        return Component.translatable(key)
                .setStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(0xFFFFFF))
                .append(Component.literal(": ").setStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(0xFFFFFF)))
                .append(Component.literal(value).setStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(valueColor)));
    }

    private static String dmzrevamp$formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        if (value == Math.floor(value)) {
            long whole = value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) value;
            return dmzrevamp$abbreviateBattlePower(whole);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
