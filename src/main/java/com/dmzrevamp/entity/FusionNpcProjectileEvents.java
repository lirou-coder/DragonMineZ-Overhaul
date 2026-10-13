package com.dmzrevamp.entity;

import com.dmzrevamp.DmzRevampMod;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FusionNpcProjectileEvents {
    private FusionNpcProjectileEvents() {}

    @SubscribeEvent
    public static void applySignatureDamage(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractKiProjectile projectile)
                || !(projectile.getOwner() instanceof DBSagasEntity owner)) return;

        int main = owner.getCurrentPoolColorMain();
        int border = owner.getCurrentPoolColorBorder();
        int outline = owner.getCurrentPoolColorOutline();
        if (projectile instanceof KiWaveEntity
                && main == FusionNpcArsenal.FINAL_KAME_CORE
                && border == FusionNpcArsenal.FINAL_KAME_OUTER
                && outline == FusionNpcArsenal.FINAL_KAME_OUTLINE) {
            markAndSetDamage(projectile, "final_kamehameha", owner.getKiBlastDamage() * 2.0F);
        } else if (projectile instanceof KiWaveEntity
                && main == FusionNpcArsenal.BIG_BANG_KAME_CORE
                && border == FusionNpcArsenal.BIG_BANG_KAME_OUTER
                && outline == FusionNpcArsenal.BIG_BANG_KAME_OUTLINE) {
            markAndSetDamage(projectile, "big_bang_kamehameha", owner.getKiBlastDamage() * 2.0F);
        }
    }

    private static void markAndSetDamage(AbstractKiProjectile projectile, String id, float damage) {
        projectile.getPersistentData().putString("dmzrevamp:npc_technique", id);
        projectile.setKiDamage(Math.max(0.0F, damage));
    }
}
