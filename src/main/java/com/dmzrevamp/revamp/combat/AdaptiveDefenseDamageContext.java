package com.dmzrevamp.revamp.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class AdaptiveDefenseDamageContext {
    private static final ThreadLocal<Entry> CURRENT = new ThreadLocal<>();

    private AdaptiveDefenseDamageContext() {
    }

    public enum AttackType {
        KI,
        STRIKE
    }

    public record Entry(AttackType type, double totalTechniqueDamage) {
    }

    public static Entry current() {
        return CURRENT.get();
    }

    public static Scope push(AttackType type, double totalTechniqueDamage) {
        Entry previous = CURRENT.get();
        double reference = Double.isFinite(totalTechniqueDamage) ? Math.max(0D, totalTechniqueDamage) : 0D;
        CURRENT.set(new Entry(type, reference));
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final Entry previous;
        private boolean closed;

        private Scope(Entry previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static boolean hurt(
            LivingEntity target,
            DamageSource source,
            float hitDamage,
            AttackType type,
            double totalTechniqueDamage
    ) {
        Entry previous = CURRENT.get();
        double reference = Double.isFinite(totalTechniqueDamage)
                ? Math.max(hitDamage, totalTechniqueDamage)
                : hitDamage;
        CURRENT.set(new Entry(type, Math.max(0D, reference)));
        try {
            return target.hurt(source, hitDamage);
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
