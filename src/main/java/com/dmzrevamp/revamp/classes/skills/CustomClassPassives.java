package com.dmzrevamp.revamp.classes.skills;

import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;

import java.util.Locale;
import java.util.Map;

/** Parser, registration and user-facing documentation for JSON-defined class passives. */
public final class CustomClassPassives {
    public static final String TUTORIAL = """
            DRAGON MINE Z: OVERHAUL - CUSTOM CLASS PASSIVES
            =================================================

            This file explains how to add an Overhaul passive to any custom class JSON in
            config/dragonminez/classes. Put the settings inside the class passive.values object.
            Keys are case-insensitive, but using the spelling shown below is recommended.

            Enable a custom passive with:
              "passive": {
                "enabled": true,
                "values": {
                  "Custom Passive": true,
                  "PassiveType": 1
                }
              }

            PassiveType
              1 = Simple: one permanent bonus or reduction.
              2 = Combo: timed stacks earned by configured combat actions.
              3 = Resource: a bonus scaled by current HP, Ki, or Stamina.
              4 = Special: a unique predefined mechanic.

            PassiveType, Type, Effect, MaxStacks, StackTime, and ResourceType are
            whole-number fields. Do not write decimal values for those keys.

            SIMPLE (PassiveType 1)
              Required: Effect, Value. Value is a decimal percentage (0.2 means 20%).
              Effect 1 = Melee and Strike defense penetration
              Effect 2 = Ki damage defense penetration
              Effect 3 = Incoming damage ignored
              Effect 4 = Strike Attack cost reduction
              Effect 5 = Ki Attack cost reduction
              Effect 6 = Strike Attack cooldown reduction
              Effect 7 = Ki Attack cooldown reduction
              Effect 8 = HP regeneration increase
              Effect 9 = passive and active Ki regeneration increase
              Effect 10 = Stamina regeneration increase

            COMBO (PassiveType 2)
              Required: Type, MaxStacks, StackTime, Effect, MaxValue.
              MaxValue is a decimal percentage and is reached at maximum stacks (0.2 = 20%).
              Every stack has its own StackTime duration in ticks.
              Type 1 = successful Melee or Strike hits
              Type 2 = successful Ki Attack hits
              Type 3 = blocked incoming attacks
              Type 4 = parried incoming attacks
              Type 5 = evaded attacks (perfect dodge/counter)
              Effect 1 = Melee and Strike damage
              Effect 2 = Ki damage
              Effect 3 = all outgoing damage
              Effect 4 = Defense
              Effect 5 = Speed
              Effect 6 = HP regeneration
              Effect 7 = Ki regeneration
              Effect 8 = Stamina regeneration
              Effect 9 = critical damage
              Effect 10 = critical chance
              Effect 11 = critical chance and critical damage

            RESOURCE (PassiveType 3)
              Required: Type, Effect, Value, ResourceType.
              Value is the maximum decimal percentage bonus (0.2 = 20%).
              Type 1 = HP, Type 2 = Ki, Type 3 = Stamina.
              ResourceType 1 = reaches full strength at 10% resource or less.
              ResourceType 2 = reaches full strength at 90% resource or more.
              Effect uses the same Effect list as COMBO.

            SPECIAL (PassiveType 4)
              Required: Type, Value. Value is a decimal multiplier (0.5 means 50%).
              Type 1 = add a share of Speed as final Melee/Strike damage
              Type 2 = add a share of Speed as final Ki damage
              Type 3 = landing a Strike Attack empowers the next Strike Attack
              Type 4 = landing a Ki Attack empowers the next Ki Attack
              Type 5 = a parry reflects a share of the original damage
              Type 6 = at 50% HP or less, set Release to the Potential Unlock limit multiplied
                       by (1 + Value) for 30 seconds; its 90-second cooldown starts afterward

            Example - Special Potential passive:
              "passive": {
                "enabled": true,
                "values": {
                  "Custom Passive": true,
                  "PassiveType": 4,
                  "Type": 6,
                  "Value": 0.5
                }
              }

            Run /dmzreload after saving class JSON files. Existing players must use a class
            configured with Custom Passive enabled for its mechanics to apply.
            """;

    private CustomClassPassives() {}

    public static boolean isCustom(RaceStatsConfig.ClassStats stats) {
        return definition(stats) != null;
    }

    public static void registerClass(String classId, String displayName) {
        String normalized = normalize(classId);
        if (normalized.isEmpty()) return;
        ClassSkillHelper.registerClassSkill(normalized, "class_" + normalized,
                displayName == null || displayName.isBlank() ? classId : displayName,
                CustomClassPassives::description);
    }

    public static Definition definition(StatsData data) {
        if (data == null || data.getCharacter() == null) return null;
        return definition(DmzClassConfigManager.getConfiguredClassStats(data.getCharacter().getCharacterClass()));
    }

    public static Definition definitionForSkill(String skillId) {
        return definition(DmzClassConfigManager.getConfiguredClassStats(ClassSkillHelper.classIdForSkill(skillId)));
    }

    public static Definition definition(RaceStatsConfig.ClassStats stats) {
        if (stats == null || stats.getPassive() == null || !stats.getPassive().isEnabled()) return null;
        Map<String, Double> values = stats.getPassive().getValues();
        if (values == null || read(values, "Custom Passive", 0D) == 0D) return null;
        return new Definition(
                integer(values, "PassiveType", 0), integer(values, "Effect", 0),
                integer(values, "Type", 0), finite(read(values, "Value", read(values, "value", 0D))),
                Math.max(0, integer(values, "MaxStacks", 0)), Math.max(1, integer(values, "StackTime", 1)),
                finite(read(values, "MaxValue", 0D)), integer(values, "ResourceType", 0)
        );
    }

    public static String description(StatsData ignored, String skillId) {
        Definition definition = definitionForSkill(skillId);
        if (definition == null) return "";
        return switch (definition.passiveType()) {
            case 1 -> simpleEffect(definition.effect()) + " with a value equal to " + percentNumber(definition.value() * 100D) + "%";
            case 2 -> "For each " + actionName(definition.type()) + " you gain one stack, up to "
                    + definition.maxStacks() + ". Each stack " + effectName(definition.effect())
                    + ", with the maximum bonus being " + percentNumber(definition.maxValue() * 100D) + "%.";
            case 3 -> "You gain " + effectName(definition.effect()) + " according to your "
                    + resourceName(definition.type()) + ". The effect increases as that resource approaches "
                    + (definition.resourceType() == 1 ? "10% or less" : "90% or more") + ".";
            case 4 -> specialDescription(definition.type(), definition.value());
            default -> "Invalid custom passive type.";
        };
    }

    public static double configuredPercent(double value) {
        return Math.max(0D, finite(value));
    }

    private static String simpleEffect(int effect) {
        return switch (effect) {
            case 1 -> "Melee and Strike Damage gain Defense Penetration";
            case 2 -> "Ki Damage gains Defense Penetration";
            case 3 -> "Incoming damage is ignored";
            case 4 -> "Strike Attack cost is reduced";
            case 5 -> "Ki Attack cost is reduced";
            case 6 -> "Strike Attack cooldown is reduced";
            case 7 -> "Ki Attack cooldown is reduced";
            case 8 -> "HP regeneration is increased";
            case 9 -> "passive and active Ki regeneration are increased";
            case 10 -> "Stamina regeneration is increased";
            default -> "The configured effect is invalid";
        };
    }

    public static String effectName(int effect) {
        return switch (effect) {
            case 1 -> "increases Melee and Strike Damage";
            case 2 -> "increases Ki Damage";
            case 3 -> "increases all damage";
            case 4 -> "increases Defense";
            case 5 -> "increases Speed";
            case 6 -> "increases HP regeneration";
            case 7 -> "increases Ki regeneration";
            case 8 -> "increases Stamina regeneration";
            case 9 -> "increases Critical Damage";
            case 10 -> "increases Critical Chance";
            case 11 -> "increases Critical Chance and Critical Damage";
            default -> "has an invalid configured effect";
        };
    }

    private static String actionName(int type) {
        return switch (type) {
            case 1 -> "successful Melee or Strike Attack";
            case 2 -> "successful Ki Attack";
            case 3 -> "blocked attack";
            case 4 -> "parried attack";
            case 5 -> "perfectly dodged or countered attack";
            default -> "configured action";
        };
    }

    private static String resourceName(int type) {
        return switch (type) { case 1 -> "HP"; case 2 -> "Ki"; case 3 -> "Stamina"; default -> "resource"; };
    }

    private static String specialDescription(int type, double value) {
        String percent = percentNumber(value * 100D) + "%";
        return switch (type) {
            case 1 -> "Your Speed stat adds a Melee Damage bonus equal to " + percent + " of its value.";
            case 2 -> "Your Speed stat adds a Ki Damage bonus equal to " + percent + " of its value.";
            case 3 -> "After landing a Strike Attack, your next Strike Attack deals " + percent + " more damage.";
            case 4 -> "After landing a Ki Attack, your next Ki Attack deals " + percent + " more damage.";
            case 5 -> "Parrying any attack deals " + percent + " of that attack's damage directly to the attacker.";
            case 6 -> "Dropping to 50% HP or less activates your maximum potential, raising your Release to "
                    + percentNumber((1D + Math.max(0D, value)) * 100D)
                    + "% of your maximum limit! It lasts 30 seconds, followed by a 90-second cooldown.";
            default -> "Invalid special passive type.";
        };
    }

    private static double read(Map<String, Double> values, String key, double fallback) {
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue() == null ? fallback : entry.getValue();
            }
        }
        return fallback;
    }

    private static int integer(Map<String, Double> values, String key, int fallback) {
        return (int) Math.round(read(values, key, fallback));
    }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0D; }
    private static String percentNumber(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D
                ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
    }
    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    public record Definition(int passiveType, int effect, int type, double value,
                             int maxStacks, int stackTime, double maxValue, int resourceType) {}
}
