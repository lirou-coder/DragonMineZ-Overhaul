package com.dmzrevamp.revamp.classes.skills;

import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.passives.IClassPassive;
import com.dragonminez.common.passives.handlers.TankPassive;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ClassPassiveAliases {
    private static boolean registered;
    private static final Set<String> REGISTERED_SKILLS = new LinkedHashSet<>();

    private ClassPassiveAliases() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClassSkillHelper.registeredClassSkills().forEach(ClassPassiveAliases::registerAliasForSkill);
        ClassPassives.register(() -> "martialartist");
        ClassPassives.register(() -> "speedster");
        ClassPassives.register(() -> "duelist");
        ClassPassives.register(new RevampTankPassive());
        registered = true;
    }

    public static synchronized void onClassSkillRegistered(String skillId) {
        if (registered) {
            registerAliasForSkill(skillId);
        }
    }

    private static void registerAliasForSkill(String skillId) {
        if (skillId == null || !REGISTERED_SKILLS.add(skillId)) {
            return;
        }
        if (ClassSkillHelper.TANK.equals(skillId)) {
            return;
        }
        ClassPassives.register(new RevampPassive(skillId));
    }

    private record RevampPassive(String skillId) implements IClassPassive {
        @Override
        public String classKey() {
            return ClassSkillHelper.classIdForSkill(skillId);
        }
    }

    private static final class RevampTankPassive extends TankPassive {
        @Override
        public String classKey() {
            return "tank";
        }
    }

}
