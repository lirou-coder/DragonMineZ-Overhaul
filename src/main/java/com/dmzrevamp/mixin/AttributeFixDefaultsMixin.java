package com.dmzrevamp.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Set;

@Mixin(targets = "net.darkhax.attributefix.config.AttributeConfig$Entry", remap = false)
public abstract class AttributeFixDefaultsMixin {
    private static final Double DMZ_UNBOUNDED_DEFAULT = (double) Long.MAX_VALUE;
    private static final Set<String> DMZREVAMP_UNBOUNDED_VANILLA_ATTRIBUTES = Set.of(
            "minecraft:max_health",
            "minecraft:attack_damage",
            "minecraft:armor",
            "minecraft:armor_toughness",
            "minecraft:generic.max_health",
            "minecraft:generic.attack_damage",
            "minecraft:generic.armor",
            "minecraft:generic.armor_toughness"
    );

    @Inject(method = "<init>", at = @At("RETURN"))
    private void dmzrevamp$useDmzAttributeFixMaxValues(ResourceLocation attributeId, RangedAttribute rangedAttribute, CallbackInfo ci) {
        if (attributeId != null && DMZREVAMP_UNBOUNDED_VANILLA_ATTRIBUTES.contains(attributeId.toString())) {
            setMaxDoubleValue(DMZ_UNBOUNDED_DEFAULT);
        }
    }

    private void setMaxDoubleValue(double maxValue) {
        try {
            Field maxField = this.getClass().getDeclaredField("max");
            maxField.setAccessible(true);
            Object max = maxField.get(this);
            if (max == null) {
                return;
            }

            Field defaultValue = max.getClass().getDeclaredField("defaultValue");
            Field value = max.getClass().getDeclaredField("value");
            defaultValue.setAccessible(true);
            value.setAccessible(true);
            defaultValue.setDouble(max, maxValue);
            value.setDouble(max, maxValue);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
