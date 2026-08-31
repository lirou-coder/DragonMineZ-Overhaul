package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.config.KiClashConfigured;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public final class KiClashAttackResolver {
    public static final String LAUNCHED_TAG = "dmzrevamp_ki_clash_launched";
    private static final String BASIC_KI_BLAST = "BASIC_KI_BLAST";
    private static final Set<String> BLOCKED_TYPES = Set.of(BASIC_KI_BLAST);
    private KiClashAttackResolver() {}

    public static boolean isAllowed(AbstractKiProjectile projectile) {
        String type = resolveType(projectile);
        return type != null && !BLOCKED_TYPES.contains(type) && KiClashConfigured.allows(type);
    }

    public static boolean isLaunched(AbstractKiProjectile projectile) {
        return projectile.getPersistentData().getBoolean(LAUNCHED_TAG) || projectile.isFiring();
    }

    public static String resolveType(AbstractKiProjectile projectile) {
        if (projectile.getOwner() instanceof LivingEntity owner) {
            KiAttackData[] resolved = new KiAttackData[1];
            StatsProvider.get(StatsCapability.INSTANCE, owner).ifPresent(data -> {
                TechniqueData technique = data.getTechniques().getUnlockedTechniques().get(projectile.getTechniqueId());
                if (technique instanceof KiAttackData ki) resolved[0] = ki;
            });
            KiAttackData attack = resolved[0];
            if (attack != null) {
                return attack.getKiType().name();
            }
        }
        if (projectile instanceof KiWaveEntity) return "WAVE";
        if (projectile instanceof KiLaserEntity) return projectile.getKiRenderType() == 1 ? "BEAM" : "LASER";
        if (projectile instanceof KiDiskEntity) return "DISK";
        // Ki Control's secondary-function basic blast has no technique id. It used
        // to fall through as MEDIUM_BALL and therefore inherited custom-technique
        // clash permission. Keep that distinct and permanently blocked.
        if (projectile instanceof KiBlastEntity) {
            String techniqueId = projectile.getTechniqueId();
            return techniqueId == null || techniqueId.isBlank() ? BASIC_KI_BLAST : "MEDIUM_BALL";
        }
        return null;
    }
}
