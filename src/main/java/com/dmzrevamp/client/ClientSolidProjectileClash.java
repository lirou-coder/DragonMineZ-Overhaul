package com.dmzrevamp.client;

import com.dragonminez.client.clash.ClientBeamClashState;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class ClientSolidProjectileClash {
    private ClientSolidProjectileClash() {}

    public static AbstractKiProjectile projectileFor(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientBeamClashState.isActive() || minecraft.level == null) return null;
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof AbstractKiProjectile projectile
                    && projectile.isClashLocked()
                    && (projectile instanceof KiBlastEntity || projectile instanceof KiDiskEntity)
                    && player.getUUID().equals(projectile.getOwnerUUID())) {
                return projectile;
            }
        }
        return null;
    }
}
