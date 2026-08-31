package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.items.RevampKiWeaponDamage;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.item.weapons.BlasterCannonItem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlasterCannonItem.class, remap = false)
public abstract class BlasterCannonItemRevampDamageMixin {
    @Redirect(
            method = "m_7203_",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/init/entities/ki/KiBlastEntity;setupKiBlast(Lnet/minecraft/world/entity/LivingEntity;FFIIFI)V"),
            require = 0
    )
    private void dmzrevamp$scaleBlasterDamageWithKiDamage(KiBlastEntity blast, LivingEntity owner, float damage, float speed, int colorMain, int colorBorder, float size, int maxLife) {
        blast.setupKiBlast(owner, RevampKiWeaponDamage.scaleWithUserKiDamage(owner, damage), speed, colorMain, colorBorder, size, maxLife);
    }
}
