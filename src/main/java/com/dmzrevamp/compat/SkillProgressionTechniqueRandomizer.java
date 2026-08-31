package com.dmzrevamp.compat;

import com.dmzrevamp.revamp.ki.KiAttackArchetype;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffectRules;
import com.dmzrevamp.revamp.ki.RevampKiAttackData;
import com.dmzrevamp.revamp.strike.CustomStrikeType;
import com.dmzrevamp.revamp.strike.RevampStrikeAttackData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-authoritative rolls for hidden Overhaul creator fields. */
public final class SkillProgressionTechniqueRandomizer {
    private static final float THIRD_CHANCE = 0.50F;
    private static final float FOURTH_CHANCE = 0.25F;
    private static final float EXTRA_ONE_CHANCE = 0.50F;
    private static final float EXTRA_TWO_CHANCE = 0.25F;

    private SkillProgressionTechniqueRandomizer() {
    }

    public static void randomizeKi(ServerPlayer player, KiAttackData attack, RevampKiAttackData revamp) {
        RandomSource random = player.getRandom();
        KiAttackData.SecondaryEffectType effectType = attack.getUtility() == KiAttackData.Utility.HEAL
                ? KiAttackData.SecondaryEffectType.BUFF
                : KiAttackData.SecondaryEffectType.DEBUFF;
        KiAttackExtraEffect.Mode extraMode = attack.getUtility() == KiAttackData.Utility.HEAL
                ? KiAttackExtraEffect.Mode.BENEFICIAL
                : KiAttackExtraEffect.Mode.HARMFUL;

        applySecondaryRoll(random, revamp, true, effectType, THIRD_CHANCE);
        applySecondaryRoll(random, revamp, false, effectType, FOURTH_CHANCE);
        applyExtraRoll(random, revamp.dmzrevamp$getExtraEffectOne(), extraMode, EXTRA_ONE_CHANCE);
        applyExtraRoll(random, revamp.dmzrevamp$getExtraEffectTwo(), extraMode, EXTRA_TWO_CHANCE);
        revamp.dmzrevamp$setArchetype(KiAttackArchetype.NORMAL, randomProjectileCount(random, attack.getKiType()), 30);
    }

    public static void randomizeStrike(ServerPlayer player, CustomStrikeType strikeType, RevampStrikeAttackData revamp) {
        RandomSource random = player.getRandom();
        KiAttackData.SecondaryEffectType effectType = strikeType.isEvasive()
                ? KiAttackData.SecondaryEffectType.BUFF
                : KiAttackData.SecondaryEffectType.DEBUFF;
        KiAttackExtraEffect.Mode extraMode = strikeType.isEvasive()
                ? KiAttackExtraEffect.Mode.BENEFICIAL
                : KiAttackExtraEffect.Mode.HARMFUL;

        EffectRoll secondary = effectRoll(random, effectType);
        revamp.dmzrevamp$setSecondaryEffect(secondary.type(), secondary.stat(), secondary.intensity(), secondary.duration());
        applySecondaryRoll(random, revamp, true, effectType, THIRD_CHANCE);
        applySecondaryRoll(random, revamp, false, effectType, FOURTH_CHANCE);
        applyExtraRoll(random, revamp.dmzrevamp$getExtraEffectOne(), extraMode, EXTRA_ONE_CHANCE);
        applyExtraRoll(random, revamp.dmzrevamp$getExtraEffectTwo(), extraMode, EXTRA_TWO_CHANCE);
    }

    private static void applySecondaryRoll(
            RandomSource random,
            Object data,
            boolean third,
            KiAttackData.SecondaryEffectType effectType,
            float chance
    ) {
        EffectRoll roll = random.nextFloat() < chance
                ? effectRoll(random, effectType)
                : EffectRoll.none();
        if (data instanceof RevampKiAttackData ki) {
            if (third) {
                ki.dmzrevamp$setThirdEffect(roll.type(), roll.stat(), roll.intensity(), roll.duration());
            } else {
                ki.dmzrevamp$setFourthEffect(roll.type(), roll.stat(), roll.intensity(), roll.duration());
            }
        } else if (data instanceof RevampStrikeAttackData strike) {
            if (third) {
                strike.dmzrevamp$setThirdEffect(roll.type(), roll.stat(), roll.intensity(), roll.duration());
            } else {
                strike.dmzrevamp$setFourthEffect(roll.type(), roll.stat(), roll.intensity(), roll.duration());
            }
        }
    }

    private static EffectRoll effectRoll(RandomSource random, KiAttackData.SecondaryEffectType type) {
        KiAttackData.AffectedStat[] stats = KiAttackData.AffectedStat.values();
        return new EffectRoll(
                type,
                stats[random.nextInt(stats.length)],
                5.0F + random.nextInt(10) * 5.0F,
                1 + random.nextInt(8)
        );
    }

    private static void applyExtraRoll(RandomSource random, KiAttackExtraEffect target, KiAttackExtraEffect.Mode mode, float chance) {
        if (random.nextFloat() >= chance) {
            target.set(KiAttackExtraEffect.Mode.NONE, "", 1, 0);
            return;
        }
        List<ResourceLocation> eligible = eligibleEffects(mode);
        if (eligible.isEmpty()) {
            target.set(KiAttackExtraEffect.Mode.NONE, "", 1, 0);
            return;
        }
        ResourceLocation effect = eligible.get(random.nextInt(eligible.size()));
        int level = 1 + random.nextInt(5);
        int duration = 1 + random.nextInt(8);
        target.set(mode, effect.toString(), level, duration);
    }

    private static List<ResourceLocation> eligibleEffects(KiAttackExtraEffect.Mode mode) {
        boolean beneficial = mode == KiAttackExtraEffect.Mode.BENEFICIAL;
        List<ResourceLocation> result = new ArrayList<>();
        ForgeRegistries.MOB_EFFECTS.getEntries().stream()
                .filter(entry -> entry.getValue().isBeneficial() == beneficial)
                .map(entry -> entry.getKey().location())
                .filter(KiAttackExtraEffectRules::isAllowed)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(result::add);
        return result;
    }

    private static int randomProjectileCount(RandomSource random, KiAttackData.KiType type) {
        int max = switch (type) {
            case SMALL_BALL -> 10;
            case MEDIUM_BALL -> 5;
            default -> 1;
        };
        if (max <= 1 || random.nextBoolean()) {
            return 1;
        }
        return 2 + random.nextInt(max - 1);
    }

    private record EffectRoll(
            KiAttackData.SecondaryEffectType type,
            KiAttackData.AffectedStat stat,
            float intensity,
            int duration
    ) {
        private static EffectRoll none() {
            return new EffectRoll(KiAttackData.SecondaryEffectType.NONE, KiAttackData.AffectedStat.STR, 5.0F, 1);
        }
    }
}
