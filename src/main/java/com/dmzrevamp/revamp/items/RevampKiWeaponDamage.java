package com.dmzrevamp.revamp.items;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class RevampKiWeaponDamage {
    private static final float KI_DAMAGE_SCALE = 0.6F;

    private RevampKiWeaponDamage() {
    }

    public static float scaleWithUserKiDamage(LivingEntity owner, float baseDamage) {
        if (!(owner instanceof Player player)) {
            return baseDamage;
        }

        // Ki weapons keep their fixed item damage, then gain part of the player's current Ki DMG stat.
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> baseDamage + (float) data.getKiDamage() * KI_DAMAGE_SCALE)
                .orElse(baseDamage);
    }
}
