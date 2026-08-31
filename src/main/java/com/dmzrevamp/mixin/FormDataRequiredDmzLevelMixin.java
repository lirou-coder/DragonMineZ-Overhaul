package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.forms.RequiredDmzLevelForm;
import com.dragonminez.common.config.FormConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = FormConfig.FormData.class, remap = false)
public abstract class FormDataRequiredDmzLevelMixin implements RequiredDmzLevelForm {
    @Unique
    private Integer requiredDMZLevel = 1;

    @Override
    public int dmzrevamp$getRequiredDMZLevel() {
        return requiredDMZLevel == null ? 1 : Math.max(1, requiredDMZLevel);
    }
}
