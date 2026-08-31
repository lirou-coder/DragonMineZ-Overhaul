package com.dmzrevamp.revamp.classes.skills;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.stats.StatsData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClassSkillHelper {
    public static final String WARRIOR = "class_warrior";
    public static final String BERSERKER = "class_berserker";
    public static final String SPIRITUALIST = "class_spiritualist";
    public static final String TANK = "class_tank";
    public static final String SPEEDSTER = "class_speedster";
    public static final String CLERIC = "class_cleric";
    public static final String MARTIAL_ARTIST = "class_martial_artist";
    public static final String PALADIN = "class_paladin";
    public static final String DUELIST = "class_duelist";
    public static final String KI_ASSASSIN = "class_ki_assassin";

    public static final List<String> ALL_SKILLS = new ArrayList<>();

    private static final Map<String, String> CLASS_TO_SKILL = new LinkedHashMap<>();
    private static final Map<String, ClassSkillDefinition> CLASS_SKILLS = new LinkedHashMap<>();

    static {
        registerClassSkill("warrior", WARRIOR, "Warrior", null);
        registerClassSkill("berserker", BERSERKER, "Berserker", null);
        registerClassSkill("spiritualist", SPIRITUALIST, "Spiritualist", null);
        registerClassSkill("martialartist", MARTIAL_ARTIST, "Martial Artist", null);
        registerClassSkill("cleric", CLERIC, "Cleric", null);
        registerClassSkill("paladin", PALADIN, "Paladin", null);
        registerClassSkill("tank", TANK, "Tank", null);
        registerClassSkill("speedster", SPEEDSTER, "Speedster", null);
        registerClassSkill("duelist", DUELIST, "Duelist", null);
        registerClassSkill("kiassassin", KI_ASSASSIN, "Ki Assassin", null);
    }

    private ClassSkillHelper() {
    }

    public static String getSkillForCurrentClass(StatsData data) {
        return data == null || data.getCharacter() == null ? null : getSkillForClass(data.getCharacter().getCharacterClass());
    }

    public static String getSkillForClass(String classId) {
        return CLASS_TO_SKILL.get(normalizeClass(classId));
    }

    public static boolean isClassSkill(String skillId) {
        return skillId != null && CLASS_SKILLS.containsKey(skillId);
    }

    public static synchronized void registerClassSkill(String classId, String skillId, String displayName, ClassDescriptionProvider descriptionProvider) {
        String normalizedClass = normalizeClass(classId);
        if (normalizedClass.isEmpty() || skillId == null || skillId.isBlank()) {
            return;
        }

        String normalizedSkill = skillId.trim();
        CLASS_TO_SKILL.put(normalizedClass, normalizedSkill);
        CLASS_SKILLS.put(normalizedSkill, new ClassSkillDefinition(normalizedClass, normalizedSkill, displayName == null || displayName.isBlank() ? normalizedSkill : displayName, descriptionProvider));
        if (!ALL_SKILLS.contains(normalizedSkill)) {
            ALL_SKILLS.add(normalizedSkill);
        }
        ClassPassiveAliases.onClassSkillRegistered(normalizedSkill);
    }

    public static synchronized List<String> registeredClassSkills() {
        return Collections.unmodifiableList(new ArrayList<>(ALL_SKILLS));
    }

    public static String classIdForSkill(String skillId) {
        for (Map.Entry<String, String> entry : CLASS_TO_SKILL.entrySet()) {
            if (entry.getValue().equals(skillId)) {
                return entry.getKey();
            }
        }
        return normalizeClass(skillId == null ? "" : skillId.replace("class_", ""));
    }

    public static boolean hasClassPassive(StatsData data, String skillId) {
        return skillId != null && skillId.equals(getSkillForCurrentClass(data));
    }

    public static boolean hasMasterSkill(StatsData data, String baseSkillId) {
        return false;
    }

    public static int level(StatsData data, String skillId) {
        return 0;
    }

    public static double percent25(StatsData data, String skillId) {
        return 0D;
    }

    public static double percent50(StatsData data, String skillId) {
        return 0D;
    }

    public static int maxStacks(StatsData data, String skillId, int fallback) {
        return hasClassPassive(data, skillId) ? Math.max(0, (int) Math.round(ClassPassives.value(data, "maxStacks", fallback))) : 0;
    }

    public static long stackDurationTicks(StatsData data, String skillId, long fallback) {
        return hasClassPassive(data, skillId) ? Math.max(1L, Math.round(ClassPassives.value(data, "stackDurationTicks", fallback))) : 0L;
    }

    public static double warriorStaminaRegenPerStack(StatsData data) {
        return hasClassPassive(data, WARRIOR) ? ClassPassives.value(data, "staminaRegenPerStack", 0.05D) : 0D;
    }

    public static double warriorDefensePenetrationPerStack(StatsData data) {
        return hasClassPassive(data, WARRIOR) ? ClassPassives.value(data, "defensePenetrationPerStack", 0.01D) : 0D;
    }

    public static double berserkerCritChancePerMissingHpPercent(StatsData data) {
        return hasClassPassive(data, BERSERKER) ? ClassPassives.value(data, "critChancePerMissingHpPercent", 0.005D) : 0D;
    }

    public static double berserkerCritDamagePerMissingHpPercent(StatsData data) {
        return hasClassPassive(data, BERSERKER) ? ClassPassives.value(data, "critDamagePerMissingHpPercent", 0.01D) : 0D;
    }

    public static double speedsterSpeedBonusPerStack(StatsData data) {
        return hasClassPassive(data, SPEEDSTER) ? ClassPassives.value(data, "speedBonusPerStack", 0.01D) : 0D;
    }

    public static double speedsterMeleeDamageSpeedSharePerStack(StatsData data) {
        return hasClassPassive(data, SPEEDSTER) ? ClassPassives.value(data, "meleeDamageSpeedSharePerStack", 0.05D) : 0D;
    }

    public static double duelistParryPoiseDamageBonus(StatsData data) {
        return hasClassPassive(data, DUELIST) ? ClassPassives.value(data, "parryPoiseDamageBonus", 0.10D) : 0D;
    }

    public static double duelistGuardBrokenDamageBonus(StatsData data) {
        return hasClassPassive(data, DUELIST) ? ClassPassives.value(data, "guardBrokenDamageBonus", 0.50D) : 0D;
    }

    public static double duelistGuardBrokenKnockbackBonus(StatsData data) {
        return hasClassPassive(data, DUELIST) ? ClassPassives.value(data, "guardBrokenKnockbackBonus", 1.0D) : 0D;
    }

    public static double duelistKiParrySpeedBonus(StatsData data) {
        return hasClassPassive(data, DUELIST) ? ClassPassives.value(data, "kiParrySpeedBonus", 0.20D) : 0D;
    }

    public static double kiAssassinCastReduction(StatsData data, boolean hasEffects) {
        if (!hasClassPassive(data, KI_ASSASSIN)) {
            return 0D;
        }
        return hasEffects ? ClassPassives.value(data, "effectCastTimeReduction", 0.20D) : ClassPassives.value(data, "noEffectCastTimeReduction", 0.50D);
    }

    public static double kiAssassinSpeedIncrease(StatsData data, boolean hasEffects) {
        if (!hasClassPassive(data, KI_ASSASSIN)) {
            return 0D;
        }
        return hasEffects ? ClassPassives.value(data, "effectSpeedIncrease", 0.20D) : ClassPassives.value(data, "noEffectSpeedIncrease", 0.50D);
    }

    public static double martialArtistMissingHpBonus(StatsData data, net.minecraft.world.entity.LivingEntity target) {
        if (!hasClassPassive(data, MARTIAL_ARTIST) || target == null || target.getMaxHealth() <= 0F) {
            return 0D;
        }
        double threshold = ClassPassives.value(data, "targetHpThreshold", 0.50D);
        double damageBonus = ClassPassives.value(data, "damageBonus", 0.25D);
        double hpRatio = target.getHealth() / target.getMaxHealth();
        return hpRatio <= threshold ? damageBonus : 0D;
    }

    public static double kiCostReduction(StatsData data, String skillId) {
        return hasClassPassive(data, skillId) ? ClassPassives.value(data, "costReduction", 0.20D) : 0D;
    }

    public static double kiCooldownReduction(StatsData data, String skillId, boolean hasEffects) {
        if (!hasClassPassive(data, skillId)) {
            return 0D;
        }
        return hasEffects ? ClassPassives.value(data, "effectCooldownReduction", 0.15D) : ClassPassives.value(data, "cooldownReduction", 0.20D);
    }

    public static double kiEffectDurationBonus(StatsData data, String skillId) {
        return hasClassPassive(data, skillId) ? ClassPassives.value(data, "effectDurationBonus", 0.25D) : 0D;
    }

    public static int potentialMaxRelease(StatsData data) {
        return 50 + (data.getSkills().getSkillLevel("potentialunlock") * 5);
    }

    public static String getDisplayName(String skillId) {
        ClassSkillDefinition definition = CLASS_SKILLS.get(skillId);
        return definition != null ? definition.displayName() : skillId;
    }

    public static String getDescription(StatsData data, String skillId) {
        return switch (skillId) {
            case WARRIOR -> "Each Melee hit grants you a Fury Stack if not blocked, and they stack up to "
                    + intValue(data, skillId, "maxStacks", 10)
                    + ". Each stack increases STM regen by "
                    + formatPercent(value(data, skillId, "staminaRegenPerStack", 0.05D))
                    + " and Defense Penetration by "
                    + formatPercent(value(data, skillId, "defensePenetrationPerStack", 0.01D))
                    + ". Stacks refresh when you deal damage and fade if you stop.";
            case BERSERKER -> "For each 1% HP you lose, you gain +"
                    + formatPercent(value(data, skillId, "critChancePerMissingHpPercent", 0.005D))
                    + " Crit Chance and +"
                    + formatPercent(value(data, skillId, "critDamagePerMissingHpPercent", 0.01D))
                    + " Crit Damage";
            case SPIRITUALIST -> "Damage Ki attacks have "
                    + formatPercent(value(data, skillId, "cooldownReduction", 0.20D))
                    + " reduced cooldown and cost. If the attack has an effect, cooldown is reduced by "
                    + formatPercent(value(data, skillId, "effectCooldownReduction", 0.15D))
                    + " instead, but effects last "
                    + formatPercent(value(data, skillId, "effectDurationBonus", 0.25D))
                    + " longer.";
            case CLERIC -> "Healing Ki attacks have "
                    + formatPercent(value(data, skillId, "cooldownReduction", 0.20D))
                    + " reduced cooldown and cost. If the attack has an effect, cooldown is reduced by "
                    + formatPercent(value(data, skillId, "effectCooldownReduction", 0.15D))
                    + " instead, but effects last "
                    + formatPercent(value(data, skillId, "effectDurationBonus", 0.25D))
                    + " longer.";
            case MARTIAL_ARTIST -> "Melee, Strike and Ki Attacks deal "
                    + formatPercent(value(data, skillId, "damageBonus", 0.25D))
                    + " more damage against enemies below "
                    + formatPercent(value(data, skillId, "targetHpThreshold", 0.50D))
                    + " HP.";
            case SPEEDSTER -> "Each Melee hit grants you a Momentum Stack if not blocked, and they stack up to "
                    + intValue(data, skillId, "maxStacks", 10)
                    + ". Each stack increases SPD by "
                    + formatPercent(value(data, skillId, "speedBonusPerStack", 0.01D))
                    + " and you gain Additional damage equal to "
                    + formatPercent(value(data, skillId, "meleeDamageSpeedSharePerStack", 0.05D))
                    + " of your SPD. Stacks refresh when you deal damage and fade if you stop.";
            case DUELIST -> "Parrying Melee attacks deals "
                    + formatPercent(value(data, skillId, "parryPoiseDamageBonus", 0.10D))
                    + " more poise damage on the enemy and Parrying a Ki Blast makes it go to the way you are looking with "
                    + formatPercent(value(data, skillId, "kiParrySpeedBonus", 0.20D))
                    + " more speed and can harm the caster. Attacking someone with Guard Broken deals "
                    + formatPercent(value(data, skillId, "guardBrokenDamageBonus", 0.50D))
                    + " more damage and "
                    + formatPercent(value(data, skillId, "guardBrokenKnockbackBonus", 1.0D))
                    + " more knockback";
            case KI_ASSASSIN -> "Ki attacks without effects have "
                    + formatPercent(value(data, skillId, "noEffectCastTimeReduction", 0.50D))
                    + " reduced cast time and "
                    + formatPercent(value(data, skillId, "noEffectSpeedIncrease", 0.50D))
                    + " more speed. Ki attacks with effects have "
                    + formatPercent(value(data, skillId, "effectCastTimeReduction", 0.20D))
                    + " reduced cast time and "
                    + formatPercent(value(data, skillId, "effectSpeedIncrease", 0.20D))
                    + " more speed.";
            default -> {
                ClassSkillDefinition definition = CLASS_SKILLS.get(skillId);
                yield definition != null && definition.descriptionProvider() != null ? definition.descriptionProvider().describe(data, skillId) : "";
            }
        };
    }

    public static int adjustKiCostWithSupport(ServerPlayerLike player, StatsData data, int originalCost, boolean allowSpiritualist) {
        return Math.max(0, originalCost);
    }

    public static String formatPercent(double value) {
        double percent = value * 100D;
        if (Math.abs(percent - Math.rint(percent)) < 0.001D) {
            return Integer.toString((int) Math.rint(percent)) + "%";
        }
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private static String normalizeClass(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    private static double value(StatsData data, String skillId, String key, double fallback) {
        return hasClassPassive(data, skillId) ? ClassPassives.value(data, key, fallback) : fallback;
    }

    private static int intValue(StatsData data, String skillId, String key, int fallback) {
        return Math.max(0, (int) Math.round(value(data, skillId, key, fallback)));
    }

    public interface ServerPlayerLike {
        void addPendingKiRefund(int amount);
    }

    public interface ClassDescriptionProvider {
        String describe(StatsData data, String skillId);
    }

    public record ClassSkillDefinition(String classId, String skillId, String displayName, ClassDescriptionProvider descriptionProvider) {
    }
}
