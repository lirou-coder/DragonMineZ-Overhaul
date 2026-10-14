package com.dmzrevamp.client;

import net.minecraft.world.entity.LivingEntity;

/** Cache entry used by the Ki Sense aura renderer mixin. */
public final class AuraStyleCacheEntry {
    public final LivingEntity entity;
    public final long calculatedAt;
    public final double ratio;
    public final float[][] colors;
    public long lastSeenAt;

    public AuraStyleCacheEntry(LivingEntity entity, long calculatedAt, long lastSeenAt, double ratio, float[][] colors) {
        this.entity = entity;
        this.calculatedAt = calculatedAt;
        this.lastSeenAt = lastSeenAt;
        this.ratio = ratio;
        this.colors = colors;
    }
}
