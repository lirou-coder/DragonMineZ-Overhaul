package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.DmzSpeedRevampEvents;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dmzrevamp.client.PrestigeConfirmationScreen;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dmzrevamp.revamp.stats.LongTpCostHelper;
import com.dragonminez.client.util.TextUtil;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dragonminez.client.gui.character.CharacterStatsScreen;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(CharacterStatsScreen.class)
public abstract class CharacterStatsScreenMixin extends BaseMenuScreen {
    private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");

    @Shadow(remap = false)
    private StatsData statsData;

    @Shadow(remap = false)
    private int tpMultiplier;

    @Shadow(remap = false)
    private void drawHexagon(
            GuiGraphics guiGraphics,
            int centerX,
            int centerY,
            float[] innerX,
            float[] innerY,
            float[] outerX,
            float[] outerY
    ) {
        throw new AssertionError();
    }

    @Unique
    private TexturedTextButton dmzrevamp$prestigeButton;

    @Unique
    private int dmzrevamp$hexCurveInvocation;

    protected CharacterStatsScreenMixin(Component title) {
        super(title);
    }

    @Redirect(
            method = "renderStatsInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/CharacterStatsScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    private MutableComponent dmzrevamp$configuredClassNameInStats(CharacterStatsScreen instance, String key, Object[] args) {
        if (statsData != null && key != null && key.equals("class.dragonminez."
                + statsData.getCharacter().getCharacterClass())) {
            int color = DmzClassConfigManager.getDisplayColor(statsData.getCharacter().getCharacterClass());
            return Component.literal(DmzClassConfigManager.getDisplayName(statsData.getCharacter().getCharacterClass()))
                    .withStyle(style -> style.withFont(DMZ_FONT).withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return instance.tr(key, args);
    }

    @ModifyArg(
            method = "renderStatsInfo",
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=gui.dragonminez.character_stats.class")),
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/util/TextUtil;drawStringWithBorder(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V",
                    ordinal = 1
            ),
            index = 5,
            remap = false,
            require = 0
    )
    private int dmzrevamp$configuredClassColorInStats(int original) {
        return statsData == null ? original
                : DmzClassConfigManager.getDisplayColor(statsData.getCharacter().getCharacterClass());
    }

    @Redirect(
            method = "getDamageReductionPercentages",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getConfiguredMaxValue()I"
            ),
            remap = false,
            require = 0
    )
    private int dmzrevamp$useGlobalAttributeMaximumInDefenseTooltip(StatsData data) {
        return LevelingRevampConfig.levelsEnabled()
                ? PrestigeSystem.attributeFormulaMaximum()
                : data.getConfiguredMaxValue();
    }

    @Redirect(
            method = "getDamageReductionPercentages",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;isMaxLevelValueInsteadOfStats()Z"
            ),
            remap = false,
            require = 0
    )
    private boolean dmzrevamp$avoidPrestigeCapConversionInDefenseTooltip(StatsData data) {
        return !LevelingRevampConfig.levelsEnabled() && data.isMaxLevelValueInsteadOfStats();
    }

    @Inject(
            method = "getDamageReductionPercentages",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void dmzrevamp$showCustomDefenseCurve(CallbackInfoReturnable<double[]> cir) {
        if (!com.dmzrevamp.config.DmzRevampConfig.CUSTOM_DEFENSE_AND_SPEED_EFFECTS_CURVE.get()
                || statsData == null) return;

        double[] original = cir.getReturnValue();
        if (original == null || original.length < 2) return;
        double baseCap = com.dragonminez.common.config.ConfigManager.getCombatConfig()
                .getBaseDamageReductionCap();
        double expectedMaxDefense = DmzRevampHelper.getDefenseCurveReference(statsData)
                * statsData.getStatScaling("DEF");
        double baseReduction = DmzRevampHelper.getConfiguredDefenseStyleEffect(
                statsData.getDefense(), expectedMaxDefense, baseCap);
        double enchantmentReduction = Math.max(0D, Math.min(1D, original[1] / 100D));
        double combinedReduction = 1D - (1D - baseReduction) * (1D - enchantmentReduction);
        cir.setReturnValue(new double[]{
                Math.max(0D, Math.min(100D, combinedReduction * 100D)),
                original[1]
        });
    }

    @Redirect(
            method = "renderStatisticsInfoHexagon",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;isMaxLevelValueInsteadOfStats()Z"
            ),
            remap = false,
            require = 0
    )
    private boolean dmzrevamp$useFixedMaximumForLevelingHexagon(StatsData data) {
        return LevelingRevampConfig.levelsEnabled() || data.isMaxLevelValueInsteadOfStats();
    }

    @Redirect(
            method = "renderStatisticsInfoHexagon",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"),
            remap = false,
            require = 0
    )
    private float dmzrevamp$setLevelingHexagonReference(float minimum, float originalReference) {
        if (!LevelingRevampConfig.levelsEnabled()) return Math.max(minimum, originalReference);
        return (float) Math.max(1D, statsData == null
                ? PrestigeSystem.attributeFormulaMaximum()
                : PrestigeSystem.hexStatReference(statsData));
    }

    @Inject(method = "renderStatisticsInfoHexagon", at = @At("HEAD"), remap = false, require = 1)
    private void dmzrevamp$resetHexCurveInvocation(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        dmzrevamp$hexCurveInvocation = 0;
    }

    @Redirect(
            method = "renderStatisticsInfoHexagon",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"),
            remap = false,
            require = 6
    )
    private float dmzrevamp$applyHexStatCurve(float maximum, float ratio) {
        String stat = switch (dmzrevamp$hexCurveInvocation++) {
            case 0 -> "STR";
            case 1 -> "RES";
            case 2 -> "ENE";
            case 3 -> "VIT";
            case 4 -> "PWR";
            default -> "SKP";
        };
        return dmzrevamp$applyConfiguredHexStatCurve(maximum, ratio, stat);
    }

    @Unique
    private float dmzrevamp$applyConfiguredHexStatCurve(float maximum, float ratio, String stat) {
        float linearProgress = Math.min(maximum, ratio);
        if (!LevelingRevampConfig.levelsEnabled()
                || !LevelingRevampConfig.get().levelsAndAttributes.customHexStatCurve) {
            return linearProgress;
        }
        double progress = 0.10D + Math.max(0D, ratio) * 2D;
        progress *= dmzrevamp$transformationHexFactor(stat);
        return (float) Math.max(0D, Math.min(1.50D, progress));
    }

    @Unique
    private double dmzrevamp$transformationHexFactor(String stat) {
        if (statsData == null || statsData.getCharacter() == null
                || (!statsData.getCharacter().hasActiveForm()
                && !statsData.getCharacter().hasActiveStackForm())) {
            return 1D;
        }
        double multiplier = "RES".equals(stat)
                ? (dmzrevamp$formAndStackMultiplier("DEF") + dmzrevamp$formAndStackMultiplier("STM")) / 2D
                : dmzrevamp$formAndStackMultiplier(stat);
        if (!Double.isFinite(multiplier)) return 1D;
        return Math.max(0D, 1D + multiplier / 50D);
    }

    @Unique
    private double dmzrevamp$formAndStackMultiplier(String stat) {
        double form = statsData.getFormMultiplier(stat);
        double stack = statsData.getStackFormMultiplier(stat);
        if (com.dragonminez.common.config.ConfigManager.getServerConfig().getGameplay()
                .getMultiplicationInsteadOfAdditionForMultipliers()) {
            return form * stack;
        }
        return 1D + (form - 1D) + (stack - 1D);
    }

    @Redirect(
            method = "renderStatisticsInfoHexagon",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/CharacterStatsScreen;drawHexagon(Lnet/minecraft/client/gui/GuiGraphics;II[F[F[F[F)V"
            ),
            remap = false,
            require = 1
    )
    private void dmzrevamp$reorderAndDrawHexagon(
            CharacterStatsScreen instance,
            GuiGraphics guiGraphics,
            int centerX,
            int centerY,
            float[] innerX,
            float[] innerY,
            float[] outerX,
            float[] outerY
    ) {
        if (innerX != null && innerY != null && innerX.length >= 6 && innerY.length >= 6) {
            double[] radii = new double[6];
            for (int index = 0; index < 6; index++) {
                radii[index] = Math.hypot(innerX[index] - centerX, innerY[index] - centerY);
            }
            int[] sourceByPosition = {3, 2, 1, 0, 5, 4};
            for (int position = 0; position < 6; position++) {
                double angle = Math.toRadians(60D * position - 90D);
                double radius = radii[sourceByPosition[position]];
                innerX[position] = centerX + (float) (radius * Math.cos(angle));
                innerY[position] = centerY + (float) (radius * Math.sin(angle));
            }
        }
        drawHexagon(guiGraphics, centerX, centerY, innerX, innerY, outerX, outerY);
    }

    @Redirect(
            method = "renderStatisticsInfoHexagon",
            slice = @Slice(from = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/CharacterStatsScreen;drawHexagon(Lnet/minecraft/client/gui/GuiGraphics;II[F[F[F[F)V",
                    shift = At.Shift.AFTER
            )),
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;toRadians(D)D"),
            remap = false,
            require = 12
    )
    private double dmzrevamp$reorderHexagonLabelAngle(double degrees) {
        if (degrees == -90D) return Math.toRadians(90D);
        if (degrees == -30D) return Math.toRadians(30D);
        if (degrees == 30D) return Math.toRadians(-30D);
        if (degrees == 90D) return Math.toRadians(-90D);
        if (degrees == 150D) return Math.toRadians(210D);
        if (degrees == 210D) return Math.toRadians(150D);
        return Math.toRadians(degrees);
    }

    @Shadow(remap = false)
    private void refreshStatButtons() {
    }

    @Inject(method = "m_7856_", at = @At("RETURN"), remap = false, require = 0)
    private void dmzrevamp$addPrestigeButton(CallbackInfo ci) {
        int rightPanelX = getUiWidth() - 158;
        int panelTop = getUiHeight() / 2 - 105;
        dmzrevamp$prestigeButton = new TexturedTextButton.Builder()
                .position(rightPanelX + 35, panelTop + 185)
                .size(74, 20)
                .texture(ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/gui/buttons/characterbuttons.png"))
                .textureCoords(0, 28, 0, 48)
                .textureSize(74, 20)
                .message(Component.literal("Prestige"))
                .onPress(ignored -> Minecraft.getInstance().setScreen(new PrestigeConfirmationScreen((CharacterStatsScreen) (Object) this)))
                .build();
        boolean available = statsData != null
                && LevelingRevampConfig.prestigeEnabled()
                && PrestigeSystem.canPrestige(statsData);
        dmzrevamp$prestigeButton.visible = available;
        dmzrevamp$prestigeButton.active = available;
        addRenderableWidget(dmzrevamp$prestigeButton);
    }

    @Inject(method = "updatePanelWidgetOffsets", at = @At("RETURN"), remap = false, require = 0)
    private void dmzrevamp$slidePrestigeButtonWithRightPanel(int leftOffset, int rightOffset, CallbackInfo ci) {
        if (dmzrevamp$prestigeButton != null) {
            dmzrevamp$prestigeButton.setX(getUiWidth() - 158 + 35 + rightOffset);
        }
    }

    @Inject(method = "m_86600_", at = @At("RETURN"), remap = false, require = 0)
    private void dmzrevamp$refreshPrestigeAvailability(CallbackInfo ci) {
        if (dmzrevamp$prestigeButton == null) return;
        boolean available = statsData != null && LevelingRevampConfig.prestigeEnabled() && PrestigeSystem.canPrestige(statsData);
        dmzrevamp$prestigeButton.visible = available;
        dmzrevamp$prestigeButton.active = available;
    }

    @Inject(method = "renderBattlePowerInfo", at = @At("RETURN"), remap = false, require = 0)
    private void dmzrevamp$renderPrestigeCount(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (statsData == null || !LevelingRevampConfig.prestigeEnabled()) return;
        int rowX = getUiWidth() - 137;
        int valueCenterX = getUiWidth() - 65;
        int rowY = getUiHeight() / 2 + 42;
        Component label = Component.translatable("gui.dmzrevamp.character_stats.prestige")
                .withStyle(style -> style.withFont(DMZ_FONT));
        Component separator = Component.literal(":")
                .withStyle(style -> style.withFont(DMZ_FONT));
        Component value = Component.literal(Integer.toString(PrestigeSystem.count(statsData)))
                .withStyle(style -> style.withFont(DMZ_FONT));
        TextUtil.drawStringWithBorder(
                graphics,
                font,
                label,
                rowX,
                rowY,
                8191446,
                0
        );
        TextUtil.drawStringWithBorder(
                graphics,
                font,
                separator,
                rowX + font.width(label),
                rowY,
                8191446,
                0
        );
        TextUtil.drawStringWithBorder(
                graphics,
                font,
                value,
                valueCenterX - font.width(value) / 2,
                rowY,
                16766891,
                0
        );

        int rowWidth = Math.max(120, valueCenterX + font.width(value) / 2 - rowX);
        if (mouseX >= rowX && mouseX <= rowX + rowWidth
                && mouseY >= rowY && mouseY <= rowY + font.lineHeight) {
            Component title = Component.literal("Prestige")
                    .withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.GOLD));
            List<Component> descriptions = List.of(Component.literal(
                    "Each Prestige break your limits and increase your natural strenght, but so does the enemies you may face."
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.YELLOW)));
            List<Component> values = List.of(
                    dmzrevamp$prestigeTooltipValue("Scale Increase: x", PrestigeSystem.scaleMultiplier(statsData)),
                    dmzrevamp$prestigeTooltipValue("Mastery Increase: x", PrestigeSystem.masteryMultiplier(statsData)),
                    dmzrevamp$prestigeTooltipValue("Saga Difficulty: x", PrestigeSystem.storyDifficultyMultiplier(statsData))
            );
            TextUtil.renderAdvancedTooltip(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    getUiWidth(),
                    getUiHeight(),
                    title,
                    descriptions,
                    values,
                    16742178
            );
        }
    }

    @Unique
    private static Component dmzrevamp$prestigeTooltipValue(String label, double multiplier) {
        return Component.literal(label + String.format(Locale.US, "%.2f", multiplier))
                .withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.GOLD));
    }

    @ModifyArg(
            method = "renderTpMultiplierInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/util/TextUtil;renderAdvancedTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;Ljava/util/List;Ljava/util/List;I)V"
            ),
            index = 8,
            remap = false,
            require = 0
    )
    private List<Component> dmzrevamp$appendPrestigeTpSource(List<Component> sources) {
        List<Component> adjusted = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
        if (statsData != null) {
            double prestige = PrestigeSystem.tpMultiplier(statsData);
            if (prestige > 1.000001D) {
                adjusted.add(Component.translatable(
                        "gui.dmzrevamp.character_stats.tp_multiplier.prestige",
                        String.format(Locale.US, "%.2f", prestige)
                ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.GOLD)));
            }
        }
        return adjusted;
    }

    @Redirect(
            method = "renderTpMultiplierInfo",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"),
            remap = false,
            require = 0
    )
    private double dmzrevamp$showPrestigeInTpMultiplierTotal(double minimum, double calculated) {
        double prestige = statsData == null ? 1D : PrestigeSystem.tpMultiplier(statsData);
        return Math.max(minimum, calculated + prestige - 1D);
    }

    @Redirect(
            method = {
                    "renderStatisticsInfoList",
                    "renderStatisticsInfoHexagon"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getStrikeDamage()D"
            ),
            remap = false
    )
    // Handles the showSpeedInsteadOfStrikeDamage logic for this class.
    private double dmzrevamp$showSpeedInsteadOfStrikeDamage(StatsData data) {
        return DmzRevampHelper.getCurrentSpeedValue(data);
    }

    @Redirect(
            method = {
                    "renderStatisticsInfoList",
                    "renderStatisticsInfoHexagon"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getMaxStrikeDamage()D"
            ),
            remap = false
    )
    // Handles the showMaxSpeedInsteadOfMaxStrikeDamage logic for this class.
    private double dmzrevamp$showMaxSpeedInsteadOfMaxStrikeDamage(StatsData data) {
        return DmzRevampHelper.getMaxSpeedValue(data);
    }

    @ModifyConstant(method = "initStatButtons", constant = @Constant(stringValue = "SKP"), remap = false, require = 0)
    // Handles the renameSkpButton logic for this class.
    private String dmzrevamp$renameSkpButton(String original) {
        return Component.translatable("gui.dragonminez.character_stats.spd").getString();
    }

    @Redirect(
            method = "renderStatsInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;m_237115_(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    // Handles the redirectStatsInfoSpdTranslation logic for this class.
    private MutableComponent dmzrevamp$redirectStatsInfoSpdTranslation(String key) {
        if ("gui.dragonminez.character_stats.skp".equals(key)) {
            return Component.translatable("gui.dragonminez.character_stats.spd")
                    .withStyle(Style.EMPTY.withFont(DMZ_FONT));
        }
        if ("gui.dragonminez.character_stats.skp.desc".equals(key)) {
            return Component.translatable("gui.dragonminez.character_stats.spd.desc")
                    .withStyle(Style.EMPTY.withFont(DMZ_FONT).withColor(ChatFormatting.GRAY));
        }
        return Component.translatable(key).withStyle(Style.EMPTY.withFont(DMZ_FONT));
    }

    @ModifyConstant(method = "renderStatisticsInfoHexagon", constant = @Constant(stringValue = "gui.dragonminez.character_stats.skp"), remap = false, require = 0)
    // Handles the renameHexagonSkpKey logic for this class.
    private String dmzrevamp$renameHexagonSkpKey(String original) {
        return "gui.dragonminez.character_stats.spd";
    }

    @ModifyConstant(method = "renderStatsInfo", constant = @Constant(stringValue = "skp"), remap = false, require = 0)
    // Handles the renameStatsInfoSkpKey logic for this class.
    private String dmzrevamp$renameStatsInfoSkpKey(String original) {
        return "spd";
    }

    @ModifyConstant(method = "renderStatsInfo", constant = @Constant(stringValue = "gui.dragonminez.character_stats.skp"), remap = false, require = 0)
    // Uses the SPD language key for the left stats menu label and hover title.
    private String dmzrevamp$renameStatsInfoSkpTranslationKey(String original) {
        return "gui.dragonminez.character_stats.spd";
    }

    @ModifyConstant(method = "createStatButton", constant = @Constant(stringValue = "gui.dragonminez.character_stats.skp.desc"), remap = false, require = 0)
    // Handles the renameSkpDescriptionKey logic for this class.
    private String dmzrevamp$renameSkpDescriptionKey(String original) {
        return "gui.dragonminez.character_stats.spd.desc";
    }

    @ModifyConstant(method = "createStatButton", constant = @Constant(stringValue = "Increase your damage with strike supers"), remap = false, require = 0)
    // Handles the strike power description replacement for this class.
    private String dmzrevamp$replaceSkpDescription(String original) {
        return "Increases your Movement and Attack Speed";
    }

    @Redirect(
            method = {
                    "initStatButtons",
                    "renderStatsInfo"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;calculateRecursiveCost(II)I"
            ),
            remap = false,
            require = 0
    )
    private int dmzrevamp$calculateSafeRecursiveTpCost(StatsData data, int amount, int maxValue) {
        return LongTpCostHelper.toSaturatedInt(LongTpCostHelper.calculateRecursiveCost(data, amount));
    }

    @Redirect(
            method = "renderStatsInfo",
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=gui.dragonminez.character_stats.tpc")),
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/text/NumberFormat;format(J)Ljava/lang/String;",
                    ordinal = 1
            ),
            require = 0
    )
    private String dmzrevamp$formatTpcLikeTp(NumberFormat formatter, long value) {
        if (statsData == null) {
            return formatter.format(value);
        }
        return LongTpCostHelper.formatLikeTp(LongTpCostHelper.calculateRecursiveCost(statsData, tpMultiplier));
    }

    @Inject(method = "lambda$initStatButtons$1", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void dmzrevamp$cycleExtendedTpMultiplier(net.minecraft.client.gui.components.Button button, CallbackInfo ci) {
        tpMultiplier = dmzrevamp$nextTpMultiplier(tpMultiplier);
        refreshStatButtons();
        ci.cancel();
    }

    @Unique
    private static int dmzrevamp$nextTpMultiplier(int current) {
        return switch (current) {
            case 1 -> 10;
            case 10 -> 100;
            case 100 -> 1000;
            case 1000 -> 10000;
            case 10000 -> 100000;
            default -> 1;
        };
    }

    @Redirect(
            method = "lambda$createStatButton$2",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;toUpperCase()Ljava/lang/String;"
            ),
            remap = false
    )
    // Handles the mapSpdButtonBackToSkp logic for this class.
    private String dmzrevamp$mapSpdButtonBackToSkp(String statName) {
        String localizedSpd = Component.translatable("gui.dragonminez.character_stats.spd").getString();
        if ("SPD".equalsIgnoreCase(statName) || localizedSpd.equalsIgnoreCase(statName)) {
            return "SKP";
        }
        return statName.toUpperCase();
    }

    @ModifyConstant(
            method = "renderStatisticsInfoList",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage"),
            remap = false
    )
    // Handles the renameListSpeedKey logic for this class.
    private String dmzrevamp$renameListSpeedKey(String original) {
        return "gui.dmzrevamp.character_stats.speed";
    }

    @ModifyConstant(
            method = "renderStatisticsInfoHexagon",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage"),
            remap = false
    )
    // Handles the renameHexSpeedKey logic for this class.
    private String dmzrevamp$renameHexSpeedKey(String original) {
        return "gui.dmzrevamp.character_stats.speed";
    }

    @ModifyConstant(
            method = "renderStatisticsInfoList",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage.tooltip1"),
            remap = false
    )
    // Handles the renameListSpeedTooltip1 logic for this class.
    private String dmzrevamp$renameListSpeedTooltip1(String original) {
        return "gui.dmzrevamp.character_stats.speed.tooltip1";
    }

    @ModifyConstant(
            method = "renderStatsInfo",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage.tooltip1"),
            remap = false,
            require = 0
    )
    // Handles the renameStatsInfoSpeedTooltip1 logic for this class.
    private String dmzrevamp$renameStatsInfoSpeedTooltip1(String original) {
        return "gui.dragonminez.character_stats.spd.desc";
    }

    @ModifyConstant(
            method = "renderStatisticsInfoHexagon",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage.tooltip1"),
            remap = false
    )
    // Handles the renameHexSpeedTooltip1 logic for this class.
    private String dmzrevamp$renameHexSpeedTooltip1(String original) {
        return "gui.dmzrevamp.character_stats.speed.tooltip1";
    }

    @ModifyConstant(
            method = "renderStatisticsInfoList",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage.tooltip2"),
            remap = false
    )
    // Handles the renameListSpeedTooltip2 logic for this class.
    private String dmzrevamp$renameListSpeedTooltip2(String original) {
        return "gui.dmzrevamp.character_stats.speed.tooltip2";
    }

    @ModifyConstant(
            method = "renderStatsInfo",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage.tooltip2"),
            remap = false,
            require = 0
    )
    // Handles the renameStatsInfoSpeedTooltip2 logic for this class.
    private String dmzrevamp$renameStatsInfoSpeedTooltip2(String original) {
        return "gui.dragonminez.character_stats.spd.desc_extra";
    }

    @ModifyConstant(
            method = "renderStatisticsInfoHexagon",
            constant = @Constant(stringValue = "gui.dragonminez.character_stats.strike_damage.tooltip2"),
            remap = false
    )
    // Handles the renameHexSpeedTooltip2 logic for this class.
    private String dmzrevamp$renameHexSpeedTooltip2(String original) {
        return "gui.dmzrevamp.character_stats.speed.tooltip2";
    }

    @Redirect(
            method = {
                    "renderStatisticsInfoList",
                    "renderStatisticsInfoHexagon"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/gui/character/CharacterStatsScreen;tr(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    // Handles the translateSpeedStatisticTooltip logic for this class.
    private MutableComponent dmzrevamp$translateSpeedStatisticTooltip(CharacterStatsScreen instance, String key, Object[] args) {
        if ("gui.dragonminez.character_stats.strike_damage.tooltip1".equals(key)
                || "gui.dmzrevamp.character_stats.speed.tooltip1".equals(key)) {
            return instance.tr("gui.dmzrevamp.character_stats.speed.tooltip1");
        }
        if ("gui.dragonminez.character_stats.strike_damage.tooltip2".equals(key)
                || "gui.dmzrevamp.character_stats.speed.tooltip2".equals(key)) {
            double scale = statsData != null ? statsData.getStatScaling("SKP") : 0D;
            return instance.tr(
                    "gui.dmzrevamp.character_stats.speed.tooltip2",
                    formatOneDecimal(scale)
            );
        }
        return instance.tr(key, args);
    }

    @Redirect(
            method = {
                    "renderStatisticsInfoList",
                    "renderStatisticsInfoHexagon"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/util/TextUtil;renderAdvancedTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;Ljava/util/List;Ljava/util/List;I)V"
            ),
            remap = false,
            require = 0
    )
    // Handles the addStatisticExtraLines logic for this class.
    private void dmzrevamp$addStatisticExtraLines(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, int screenWidth, int screenHeight, Component title, List<Component> lines, List<Component> extraLines, int borderColor) {
        List<Component> adjustedExtraLines = extraLines == null ? null : new ArrayList<>(extraLines);
        if (statsData != null) {
            adjustedExtraLines = addRevampStatisticLines(title, lines, adjustedExtraLines);
        }
        TextUtil.renderAdvancedTooltip(guiGraphics, font, mouseX, mouseY, screenWidth, screenHeight,
                dmzrevamp$smoothComponent(title), dmzrevamp$smoothComponents(lines),
                dmzrevamp$smoothComponents(adjustedExtraLines), borderColor);
    }

    @Redirect(
            method = "renderStatsInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/client/util/TextUtil;renderAdvancedTooltip(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;Ljava/util/List;Ljava/util/List;I)V"
            ),
            remap = false,
            require = 0
    )
    private void dmzrevamp$renderOriginalStatisticTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight,
            Component title,
            List<Component> lines,
            List<Component> extraLines,
            int borderColor
    ) {
        List<Component> adjustedExtraLines = extraLines == null ? null : new ArrayList<>(extraLines);
        if (adjustedExtraLines != null) {
            // Keep the earlier Ki Manipulation/Infusion cleanup without adding
            // the right-panel Overhaul statistic details to the left panel.
            adjustedExtraLines.removeIf(CharacterStatsScreenMixin::isStrikeDamageOnlyExtraLine);
        }
        TextUtil.renderAdvancedTooltip(guiGraphics, font, mouseX, mouseY, screenWidth, screenHeight,
                dmzrevamp$smoothComponent(title), dmzrevamp$smoothComponents(lines),
                dmzrevamp$smoothComponents(adjustedExtraLines), borderColor);
    }

    @Unique
    private static Component dmzrevamp$smoothComponent(Component component) {
        return component == null ? null : component.copy().withStyle(style -> style.withFont(DMZ_FONT));
    }

    @Unique
    private static List<Component> dmzrevamp$smoothComponents(List<Component> components) {
        if (components == null) return null;
        return components.stream().map(CharacterStatsScreenMixin::dmzrevamp$smoothComponent).toList();
    }

    // Handles the formatOneDecimal logic for this class.
    private static String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static Component smoothNumber(double value) {
        return Component.literal(formatOneDecimal(value))
                .withStyle(style -> style.withFont(DMZ_FONT).withColor(TextColor.fromRgb(0x55FFFF)));
    }

    // Handles the addRevampStatisticLines logic for this class.
    private List<Component> addRevampStatisticLines(Component title, List<Component> lines, List<Component> extraLines) {
        String key = findTooltipKey(title, lines);
        if (key == null) {
            return extraLines;
        }
        List<Component> adjusted = extraLines == null ? new ArrayList<>() : extraLines;
        // The list view titles its tooltips by derived statistic, while the
        // hexagon titles them by raw attribute. Map both right-panel views to
        // the same Overhaul detail groups.
        if (key.endsWith("character_stats.skp") || key.endsWith("character_stats.spd")) key = "character_stats.speed";
        else if (key.endsWith("character_stats.pwr")) key = "character_stats.ki_damage";
        else if (key.endsWith("character_stats.ene")) key = "character_stats.max_energy";
        else if (key.endsWith("character_stats.str")) key = "character_stats.melee_damage";

        if (key.contains("character_stats.strike_damage") || key.contains("character_stats.speed")) {
            adjusted.removeIf(CharacterStatsScreenMixin::isStrikeDamageOnlyExtraLine);
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.speed.max_running",
                    smoothNumber(DmzRevampHelper.getCurrentSpeedDisplayPercent(statsData))
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.speed.total_attack",
                    smoothNumber(DmzRevampHelper.getCurrentAttackSpeedDisplayPercent(statsData))
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.speed.cooldown_reduction",
                    smoothNumber(DmzRevampHelper.getSpdCooldownReduction(statsData) * 100D)
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
        } else if (key.contains("ki_damage")) {
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.ki_damage.ki_attack_speed",
                    smoothNumber(DmzRevampHelper.getSpdAttackSpeedIncrease(statsData) * 100D)
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.ki_damage.cast_time_reduction",
                    smoothNumber(DmzRevampHelper.getKiOverchargeCastTimeReduction(statsData) * 100D)
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
        } else if (key.contains("max_energy")) {
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.max_energy.combat_flight_speed",
                    smoothNumber(DmzSpeedRevampEvents.getMaximumCombatFlightSpeedDisplayPercent(Minecraft.getInstance().player, statsData))
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.max_energy.search_flight_speed",
                    smoothNumber(DmzSpeedRevampEvents.getMaximumSearchFlightSpeedDisplayPercent(Minecraft.getInstance().player, statsData))
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
        } else if (key.contains("melee_damage")) {
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.melee_damage.swim_speed",
                    smoothNumber(DmzRevampHelper.getCurrentSwimSpeedDisplayPercent(statsData))
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
            adjusted.add(Component.translatable(
                    "gui.dmzrevamp.character_stats.melee_damage.strike_dash_distance",
                    smoothNumber(DmzRevampHelper.getSpdAttackSpeedIncrease(statsData) * 100D)
            ).withStyle(style -> style.withFont(DMZ_FONT).withColor(ChatFormatting.AQUA)));
        }
        return adjusted;
    }

    private static boolean isStrikeDamageOnlyExtraLine(Component line) {
        String key = translationKey(line);
        if (key != null) {
            String normalizedKey = key.toLowerCase(Locale.ROOT);
            if (normalizedKey.contains("ki_fist")
                    || normalizedKey.contains("kifist")
                    || normalizedKey.contains("ki_weapon")
                    || normalizedKey.contains("kiweapon")
                    || normalizedKey.contains("ki_infusion")
                    || normalizedKey.contains("kiinfusion")
                    || normalizedKey.contains("infuse_damage")
                    || normalizedKey.contains("infusedamage")
                    || normalizedKey.contains("infusion_damage")
                    || normalizedKey.contains("shift_hint")) {
                return true;
            }
        }

        String text = line == null ? "" : line.getString().toLowerCase(Locale.ROOT);
        return text.contains("ki fist")
                || text.contains("ki weapon")
                || text.contains("ki weapons")
                || text.contains("punho de ki")
                || text.contains("arma de ki")
                || text.contains("armas de ki")
                || text.contains("puño de ki")
                || text.contains("infuse damage")
                || text.contains("ki infuse damage")
                || text.contains("infusion damage")
                || text.contains("dano de infusão")
                || text.contains("daño de infusión")
                || (text.contains("shift") && text.contains("advanced description"));
    }

    // Handles the findTooltipKey logic for this class.
    private static String findTooltipKey(Component title, List<Component> lines) {
        String titleKey = translationKey(title);
        if (titleKey != null) {
            return titleKey;
        }
        if (lines != null) {
            for (Component line : lines) {
                String lineKey = translationKey(line);
                if (lineKey != null) {
                    return lineKey;
                }
            }
        }
        return null;
    }

    // Handles the translationKey logic for this class.
    private static String translationKey(Component component) {
        if (component != null && component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return null;
    }
}
