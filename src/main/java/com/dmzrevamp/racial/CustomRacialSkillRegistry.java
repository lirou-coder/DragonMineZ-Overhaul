// Registers custom racial skills so DMZ screens and racial-action logic can find this addon's replacements.
package com.dmzrevamp.racial;

import com.dmzrevamp.racial.impl.BioAndroidRacialSkill;
import com.dmzrevamp.racial.impl.FrostDemonRevampRacialSkill;
import com.dmzrevamp.racial.impl.HumanRpgAdrenalineRacialSkill;
import com.dmzrevamp.racial.impl.MajinRevampRacialSkill;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiRacialSkill;
import com.dmzrevamp.racial.impl.NamekianRevampRacialSkill;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CustomRacialSkillRegistry {
    public static final boolean ENABLED = true;
    private static final Map<String, CustomRacialSkill> SKILLS = new LinkedHashMap<>();
    private static boolean bootstrapped = false;

    // The registry is global state, so nobody should create a separate copy of it.
    private CustomRacialSkillRegistry() {
    }

    // Adds the built-in Overhaul racial skills once during mod startup.
    public static void bootstrap() {
        if (!ENABLED) {
            return;
        }
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        register(new HumanRpgAdrenalineRacialSkill());
        register(new SaiyanRpgZenkaiRacialSkill());
        register(new FrostDemonRevampRacialSkill());
        register(new BioAndroidRacialSkill());
        register(new MajinRevampRacialSkill());
        register(new NamekianRevampRacialSkill());
    }

    // Stores one racial skill by its lowercase id, replacing older entries with the same id.
    public static void register(CustomRacialSkill skill) {
        SKILLS.put(skill.id().toLowerCase(), skill);
    }

    // Finds a custom racial skill from the id stored in a race config.
    public static CustomRacialSkill get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return SKILLS.get(id.toLowerCase());
    }

    // Lets mixins quickly check whether DMZ should use Overhaul's custom racial handling for an id.
    public static boolean contains(String id) {
        return get(id) != null;
    }

    // Exposes all registered skills for screens or future integrations that need to list them.
    public static Collection<CustomRacialSkill> values() {
        return SKILLS.values();
    }
}
