package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiClashTeams;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KiBlastEntity.class, KiDiskEntity.class})
public abstract class KiBlastClashLockMixin {
    @Inject(method = "m_8119_", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$holdClashingSphere(CallbackInfo ci) {
        if (KiClashTeams.tickClashingSolidProjectile((AbstractKiProjectile) (Object) this)) ci.cancel();
    }
}
