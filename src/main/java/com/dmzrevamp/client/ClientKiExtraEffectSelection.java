package com.dmzrevamp.client;

import com.dmzrevamp.revamp.ki.KiAttackExtraEffect;
import com.dmzrevamp.revamp.ki.KiAttackExtraEffectRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientKiExtraEffectSelection {
    public KiAttackExtraEffect.Mode mode = KiAttackExtraEffect.Mode.NONE;
    public String effectId = "";
    public int level = 1;
    public int duration = 0;

    public void cycleEffect(boolean next) {
        if (mode == KiAttackExtraEffect.Mode.NONE) {
            effectId = "";
            return;
        }
        List<ResourceLocation> ids = new ArrayList<>();
        ForgeRegistries.MOB_EFFECTS.getEntries().stream()
                .filter(entry -> KiAttackExtraEffectRules.isAllowed(entry.getKey().location()))
                .filter(entry -> mode == KiAttackExtraEffect.Mode.BENEFICIAL == entry.getValue().isBeneficial())
                .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                .forEach(entry -> ids.add(entry.getKey().location()));
        if (ids.isEmpty()) {
            effectId = "";
            return;
        }
        int index = effectId.isBlank() ? -1 : ids.indexOf(ResourceLocation.tryParse(effectId));
        int nextIndex = next ? (index + 1) % ids.size() : (index - 1 + ids.size()) % ids.size();
        effectId = ids.get(nextIndex).toString();
    }

    public void onModeChanged() {
        if (mode == KiAttackExtraEffect.Mode.NONE) {
            effectId = "";
            duration = 0;
            return;
        }
        duration = KiAttackExtraEffectRules.clampDurationSeconds(mode, duration);
        if (effectId.isBlank() || !isCurrentEffectValidForMode()) {
            cycleEffect(true);
        }
    }

    public void adjustDuration(int delta) {
        if (mode == KiAttackExtraEffect.Mode.NONE) {
            duration = 0;
            return;
        }
        int step = KiAttackExtraEffectRules.durationStepSeconds(mode);
        int scaledDelta = delta < 0 ? -step : step;
        duration = KiAttackExtraEffectRules.clampDurationSeconds(mode, duration + scaledDelta);
    }

    private boolean isCurrentEffectValidForMode() {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (!KiAttackExtraEffectRules.isAllowed(id)) {
            return false;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        return effect != null && (mode == KiAttackExtraEffect.Mode.BENEFICIAL) == effect.isBeneficial();
    }

    public String summary() {
        if (mode == KiAttackExtraEffect.Mode.NONE) {
            return "None";
        }
        String name = effectId.isBlank() ? "Select" : effectId.substring(effectId.lastIndexOf(':') + 1);
        return mode.name() + " " + name + " L" + level + "/" + duration + "s";
    }

    public float costWeight() {
        if (mode == KiAttackExtraEffect.Mode.NONE || effectId.isBlank()) {
            return 0.0F;
        }
        return Math.max(1, level) * 0.18F + Math.max(1, duration) * 0.035F;
    }
}
