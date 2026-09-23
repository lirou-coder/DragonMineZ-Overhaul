package com.dmzrevamp.mixin;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DMZ master NPCs are passive PathfinderMobs and their attribute builder does not expose
 * ATTACK_DAMAGE. entities.json has a meleeDamage field, so add the attribute to the shared
 * master supplier so EntityConfigAttributeApplier can honor that field as it does for mobs.
 */
@Mixin(value = MastersEntity.class, remap = false)
public abstract class MastersEntityConfigAttributesMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"), remap = false, require = 0)
    private static void dmzrevamp$allowConfiguredMasterMeleeDamage(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        AttributeSupplier.Builder builder = cir.getReturnValue();
        if (builder != null && !builder.hasAttribute(Attributes.ATTACK_DAMAGE)) {
            builder.add(Attributes.ATTACK_DAMAGE, 1.0D);
        }
    }
}
