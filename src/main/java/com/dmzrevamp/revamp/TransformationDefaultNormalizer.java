package com.dmzrevamp.revamp;

import com.dragonminez.common.config.FormConfig;

import java.util.List;
import java.util.Map;

public final class TransformationDefaultNormalizer {
    private TransformationDefaultNormalizer() {
    }

    public static void normalizeDefaultFormConfigs(Map<String, FormConfig> formConfigs) {
        if (formConfigs == null) {
            return;
        }

        for (FormConfig config : formConfigs.values()) {
            normalizeDefaultFormConfig(config);
        }
    }

    private static void normalizeDefaultFormConfig(FormConfig config) {
        if (config == null || config.getForms() == null || config.getForms().isEmpty()) {
            return;
        }
        config.setConfigVersion(FormConfig.CURRENT_VERSION);
        if (config.getFormType().isBlank()) {
            config.setFormType("superforms");
        }

        Double firstSkpMultiplier = null;
        boolean first = true;
        for (FormConfig.FormData form : config.getForms().values()) {
            if (form == null) {
                continue;
            }
            normalizeFormatDefaults(form);

            if (first) {
                firstSkpMultiplier = form.getSkpMultiplier();
                first = false;
            } else if (firstSkpMultiplier != null && hasReducedSpeedDefault(form)) {
                form.setSkpMultiplier(firstSkpMultiplier);
            }

            if (getDouble(form.getSpeedMultiplier(), 1D) > 1D) {
                form.setSpeedMultiplier(1D);
            }
            if (getDouble(form.getAttackSpeed(), 1D) > 1D) {
                form.setAttackSpeed(1D);
            }
        }
    }

    private static void normalizeFormatDefaults(FormConfig.FormData form) {
        if (form.getTransformationAnimation().isBlank()) {
            form.setTransformationAnimation("transf.generic");
        }
        if (form.getIncompatibleWith() == null) {
            form.setIncompatibleWith(List.of("ultimate.ultimate"));
        }
        if (form.getShareMasteryWith() == null) {
            form.setShareMasteryWith(List.of());
        }
        if (form.getTriggerItemCosts() == null) {
            form.setTriggerItemCosts(List.of());
        }
        if (form.getDurationItemCosts() == null) {
            form.setDurationItemCosts(List.of());
        }
        if (form.getMobEffects() == null) {
            form.setMobEffects(List.of());
        }
        if (form.getOutlineShader() == null) {
            form.setOutlineShader(new FormConfig.FormData.OutlineShaderConfig());
        }
        if (form.getAllowFreeTransformOnMastery() == null) {
            form.setAllowFreeTransformOnMastery(50D);
        }
    }

    private static boolean hasReducedSpeedDefault(FormConfig.FormData form) {
        return getDouble(form.getSpeedMultiplier(), 1D) < 1D || getDouble(form.getAttackSpeed(), 1D) < 1D;
    }

    private static double getDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }
}
