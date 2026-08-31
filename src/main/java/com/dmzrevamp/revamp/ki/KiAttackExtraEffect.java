package com.dmzrevamp.revamp.ki;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class KiAttackExtraEffect {
    public enum Mode {
        NONE,
        HARMFUL,
        BENEFICIAL
    }

    private Mode mode = Mode.NONE;
    private String effectId = "";
    private int level = 1;
    private int durationSeconds = 5;

    public Mode mode() {
        return mode;
    }

    public String effectId() {
        return effectId;
    }

    public int level() {
        return level;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public boolean isActive() {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        return mode != Mode.NONE && !effectId.isBlank() && KiAttackExtraEffectRules.isAllowed(id);
    }

    public float costWeight() {
        if (!isActive()) {
            return 0.0F;
        }
        return Math.max(1, level) * 0.18F + Math.max(1, durationSeconds) * 0.035F;
    }

    public void set(Mode mode, String effectId, int level, int durationSeconds) {
        this.mode = mode == null ? Mode.NONE : mode;
        this.effectId = effectId == null ? "" : effectId;
        this.level = Math.max(1, Math.min(5, level));
        if (this.mode == Mode.NONE || !KiAttackExtraEffectRules.isAllowed(ResourceLocation.tryParse(this.effectId))) {
            this.mode = Mode.NONE;
            this.effectId = "";
            this.durationSeconds = 0;
        } else {
            this.durationSeconds = KiAttackExtraEffectRules.clampDurationSeconds(this.mode, durationSeconds);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", mode.name());
        tag.putString("EffectId", effectId);
        tag.putInt("Level", level);
        tag.putInt("Duration", durationSeconds);
        return tag;
    }

    public void load(CompoundTag tag) {
        Mode loadedMode = Mode.NONE;
        try {
            loadedMode = Mode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException ignored) {
        }
        set(loadedMode, tag.getString("EffectId"), tag.getInt("Level"), tag.getInt("Duration"));
    }
}
