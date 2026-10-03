package com.dmzrevamp.mixin.client;

import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dragonminez.client.gui.character.util.RacialSkillParts;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adapts Overhaul racial ids to DMZ 2.2's individual, scrollable racial-skill entries. */
@Mixin(value = RacialSkillParts.class, priority = 1100)
public abstract class RacialSkillPartsRevampMixin {
    @Inject(method = "build", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dmzrevamp$buildRevampParts(String racialSkill, boolean android, boolean allVariants,
                                                   CallbackInfoReturnable<List<RacialSkillParts.Part>> cir) {
        if (racialSkill == null) return;
        String id = racialSkill.toLowerCase(Locale.ROOT);
        List<RacialSkillParts.Part> parts = switch (id) {
            case "humanrevamp" -> dmzrevamp$human(android, allVariants);
            case "saiyanrevamp" -> dmzrevamp$saiyan(allVariants);
            case "frostdemonrevamp", "frostrevamp" -> dmzrevamp$frost(allVariants);
            case "namekianrevamp" -> dmzrevamp$namekian(allVariants);
            case "majinrevamp" -> dmzrevamp$majin(allVariants);
            case "bioandroidrevamp" -> dmzrevamp$bioAndroid(allVariants);
            default -> null;
        };
        if (parts != null) cir.setReturnValue(parts);
    }

    private static List<RacialSkillParts.Part> dmzrevamp$nativeParts(String id, boolean android, boolean allVariants) {
        return new ArrayList<>(allVariants
                ? RacialSkillParts.forRace(id)
                : RacialSkillParts.forPlayer(id, android));
    }

    private static List<RacialSkillParts.Part> dmzrevamp$human(boolean android, boolean allVariants) {
        List<RacialSkillParts.Part> parts = dmzrevamp$nativeParts("human", android, allVariants);
        var config = DmzRevampRacialConfigs.humanRpg();
        dmzrevamp$replace(parts, "racial_human.blood_fueled_ki", new RacialSkillParts.Part(
                "racial_humanrevamp.ki_boosting_body",
                Component.translatable("skill.dragonminez.racial_humanrevamp.ki_boosting_body"),
                Component.translatable("skill.dragonminez.racial_humanrevamp.ki_boosting_body.desc",
                        dmzrevamp$pct(config.kiRegenBonus), dmzrevamp$pct(config.fullKiPowerBoost),
                        dmzrevamp$pct(config.fullKiThreshold), dmzrevamp$pct(config.minimumKiThreshold))));
        dmzrevamp$replace(parts, "racial_human.android_core", new RacialSkillParts.Part(
                "racial_humanrevamp.android_core",
                Component.translatable("skill.dragonminez.racial_human.android_core"),
                Component.translatable("skill.dragonminez.racial_humanrevamp.android_core.desc",
                        dmzrevamp$pct(config.androidUpgradedKiRegenBonusMultiplier - 1D),
                        dmzrevamp$pct(1D - config.androidUpgradedFullKiPowerBoostMultiplier))));
        return parts;
    }

    private static List<RacialSkillParts.Part> dmzrevamp$saiyan(boolean allVariants) {
        List<RacialSkillParts.Part> parts = dmzrevamp$nativeParts("saiyan", false, allVariants);
        var config = DmzRevampRacialConfigs.saiyanRpg();
        dmzrevamp$replace(parts, "racial_saiyan.zenkai", new RacialSkillParts.Part(
                "racial_saiyanrevamp.zenkai",
                Component.translatable("skill.dragonminez.racial_saiyan.zenkai"),
                Component.translatable("skill.dragonminez.racial_saiyanrevamp.zenkai.desc", config.cooldownSeconds)));
        dmzrevamp$replace(parts, "racial_saiyan.limitless_power", new RacialSkillParts.Part(
                "racial_saiyanrevamp.limitless_power",
                Component.translatable("skill.dragonminez.racial_saiyan.limitless_power"),
                Component.translatable("skill.dragonminez.racial_saiyanrevamp.limitless_power.desc",
                        dmzrevamp$pct(config.maxZenkaiReleaseBonus))));
        return parts;
    }

    private static List<RacialSkillParts.Part> dmzrevamp$frost(boolean allVariants) {
        List<RacialSkillParts.Part> parts = dmzrevamp$nativeParts("frostdemon", false, allVariants);
        var config = DmzRevampRacialConfigs.frostDemon();
        dmzrevamp$replace(parts, "racial_frostdemon.prodigious_strength", new RacialSkillParts.Part(
                "racial_frostdemonrevamp.dangerously_fast",
                Component.translatable("skill.dragonminez.racial_frostdemonrevamp.dangerously_fast"),
                Component.translatable("skill.dragonminez.racial_frostdemonrevamp.dangerously_fast.desc",
                        dmzrevamp$pct(config.attackStaminaCostReduction))));
        return parts;
    }

    private static List<RacialSkillParts.Part> dmzrevamp$namekian(boolean allVariants) {
        List<RacialSkillParts.Part> parts = dmzrevamp$nativeParts("namekian", false, allVariants);
        var config = DmzRevampRacialConfigs.namekianRevamp();
        dmzrevamp$replace(parts, "racial_namekian.assimilation", new RacialSkillParts.Part(
                "racial_namekianrevamp.assimilation",
                Component.translatable("skill.dragonminez.racial_namekian.assimilation"),
                Component.translatable("skill.dragonminez.racial_namekianrevamp.assimilation.desc",
                        config.assimilationChargeTicks / 20D, dmzrevamp$pct(config.healthRegenRatio),
                        dmzrevamp$pct(config.statBoostRatio), config.cooldownSeconds)));
        return parts;
    }

    private static List<RacialSkillParts.Part> dmzrevamp$majin(boolean allVariants) {
        List<RacialSkillParts.Part> parts = dmzrevamp$nativeParts("majin", false, allVariants);
        var config = DmzRevampRacialConfigs.majinRevamp();
        dmzrevamp$replace(parts, "racial_majin.absorption", new RacialSkillParts.Part(
                "racial_majinrevamp.absorption",
                Component.translatable("skill.dragonminez.racial_majin.absorption"),
                Component.translatable("skill.dragonminez.racial_majinrevamp.absorption.desc",
                        config.absorptionChargeTicks / 20D, dmzrevamp$pct(config.healthRegenRatio),
                        dmzrevamp$pct(config.statCopyRatio))));
        return parts;
    }

    private static List<RacialSkillParts.Part> dmzrevamp$bioAndroid(boolean allVariants) {
        List<RacialSkillParts.Part> parts = dmzrevamp$nativeParts("bioandroid", false, allVariants);
        dmzrevamp$replace(parts, "racial_bioandroid.evolution", new RacialSkillParts.Part(
                "racial_bioandroidrevamp.evolution",
                Component.translatable("skill.dragonminez.racial_bioandroid.evolution"),
                Component.translatable("skill.dragonminez.racial_bioandroidrevamp.evolution.desc",
                        dmzrevamp$pct(DmzRevampRacialConfigs.bioAndroid().effectMultiplier))));
        return parts;
    }

    private static void dmzrevamp$replace(List<RacialSkillParts.Part> parts, String id,
                                          RacialSkillParts.Part replacement) {
        for (int i = 0; i < parts.size(); i++) {
            if (id.equals(parts.get(i).id())) {
                parts.set(i, replacement);
                return;
            }
        }
        parts.add(0, replacement);
    }

    private static long dmzrevamp$pct(double ratio) {
        return Math.round(Math.max(0D, ratio) * 100D);
    }
}
