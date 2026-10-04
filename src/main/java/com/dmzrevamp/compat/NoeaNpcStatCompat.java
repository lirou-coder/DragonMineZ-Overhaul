package com.dmzrevamp.compat;

import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dmzrevamp.revamp.battlepower.AccurateMobBattlePowerCalculator;
import com.dmzrevamp.revamp.battlepower.CustomBattlePowerCalculator;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Runtime-isolated bridge for Noea's spar, tournament and roaming combatants. */
public final class NoeaNpcStatCompat {
    private static final ThreadLocal<PlayerStats> CAPTURED_PLAYER = new ThreadLocal<>();

    private NoeaNpcStatCompat() {
    }

    public static void capturePlayer(ServerPlayer player) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null) {
            CAPTURED_PLAYER.remove();
            return;
        }
        CAPTURED_PLAYER.set(new PlayerStats(
                data.getMaxHealth(), data.getMaxMeleeDamage(), data.getMaxKiDamage(), data.getMaxFlatMitigation()));
    }

    public static void applyMatched(Mob mob, Object difficulty) {
        PlayerStats stats = CAPTURED_PLAYER.get();
        if (stats == null || mob == null) return;
        double health = invokeDouble(difficulty, "opponentHealth", stats.health);
        set(mob, Attributes.MAX_HEALTH, health);
        set(mob, Attributes.ATTACK_DAMAGE, stats.melee);
        set(mob, DmzRevampAttributes.MOB_DEFENSE.get(), stats.defense);
        setKi(mob, stats.ki);
        mob.setHealth(mob.getMaxHealth());
        ManualBattlePowerStatEvents.syncDmzBattlePower(mob);
    }

    public static void applyTarget(Mob mob, Object fighterOrMoment, double scale) {
        double configured = invokeDouble(fighterOrMoment, "battlePower", -1D);
        if (configured <= 0D) configured = invokeDouble(fighterOrMoment, "power", -1D);
        if (configured <= 0D) return;
        applyTarget(mob, configured * Math.max(0D, scale));
    }

    public static void applyTarget(Mob mob, double targetBattlePower) {
        if (mob == null || !Double.isFinite(targetBattlePower) || targetBattlePower <= 0D) return;
        double targetTotal = CustomBattlePowerCalculator.totalStatsForMobBattlePower(targetBattlePower);
        double distributable = Math.max(0D,
                targetTotal - AccurateMobBattlePowerCalculator.calculateNonCorePower(mob));

        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        List<CoreStat> stats = new ArrayList<>(4);
        add(stats, config, "maxHealth", Attributes.MAX_HEALTH);
        add(stats, config, "attackDamage", Attributes.ATTACK_DAMAGE);
        add(stats, config, "defense", DmzRevampAttributes.MOB_DEFENSE.get());
        add(stats, config, "kiDamage", null);
        if (stats.isEmpty()) return;

        double contribution = distributable / stats.size();
        for (CoreStat stat : stats) {
            double value = contribution / stat.weight;
            if (stat.attribute == null) setKi(mob, value);
            else set(mob, stat.attribute, stat.attribute == Attributes.MAX_HEALTH ? Math.max(1D, value) : value);
        }
        mob.setHealth(mob.getMaxHealth());
        ManualBattlePowerStatEvents.syncDmzBattlePower(mob);
    }

    private static void add(List<CoreStat> output, CustomBattlePowerConfig.Config config,
                            String key, Attribute attribute) {
        CustomBattlePowerConfig.StatRule rule = config.mobStats.get(key);
        if (rule != null && rule.enabled && Double.isFinite(rule.weight) && rule.weight > 0D
                && (attribute == null || attribute == Attributes.MAX_HEALTH || attribute == Attributes.ATTACK_DAMAGE
                || DmzRevampAttributes.MOB_DEFENSE.get() == attribute)) {
            output.add(new CoreStat(attribute, rule.weight));
        }
    }

    private static void set(Mob mob, Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && Double.isFinite(value)) instance.setBaseValue(Math.max(0D, value));
    }

    private static void setKi(Mob mob, double value) {
        double safe = Double.isFinite(value) ? Math.max(0D, value) : 0D;
        mob.getPersistentData().putDouble("KiBlastDamage", safe);
        mob.getPersistentData().putDouble("kiDamage", safe);
        if (mob instanceof DBSagasEntity saga) saga.setKiBlastDamage((float) Math.min(Float.MAX_VALUE, safe));
    }

    private static double invokeDouble(Object object, String method, double fallback, Object... args) {
        if (object == null) return fallback;
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) types[i] = double.class;
            Method accessor = object.getClass().getMethod(method, types);
            Object result = accessor.invoke(object, args);
            return result instanceof Number number ? number.doubleValue() : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private record CoreStat(Attribute attribute, double weight) {}
    private record PlayerStats(double health, double melee, double ki, double defense) {}
}
