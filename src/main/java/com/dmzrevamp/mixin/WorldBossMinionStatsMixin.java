package com.dmzrevamp.mixin;

import com.dmzrevamp.config.WorldBossesConfig;
import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives boss-created helper mobs the configured share of their boss' current stats. */
@Mixin(value = DBSagasEntity.class, remap = false)
public abstract class WorldBossMinionStatsMixin {
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void dmzrevamp$applyMinionShare(CallbackInfo ci) {
        DBSagasEntity self = (DBSagasEntity) (Object) this;
        if (self.level().isClientSide || self.getPersistentData().getBoolean("dmzrevamp_worldboss_minion_configured")) return;
        if (!(self instanceof AllWorldBossesEntity.MiniJanemba) && !(self instanceof AllWorldBossesEntity.MetalCoolerCopy)) return;
        WorldBossEntity boss = self.level().getEntitiesOfClass(WorldBossEntity.class, self.getBoundingBox().inflate(128), b -> b.isAlive()).stream().findFirst().orElse(null);
        if (boss == null) return;
        WorldBossesConfig.Boss cfg = WorldBossesConfig.get(boss instanceof AllWorldBossesEntity.MetalCoolerCore ? "metal_cooler_core" : "janemba_fat");
        double share = Math.max(0D, cfg.minionsStatShare);
        dmzrevamp$set(self, Attributes.MAX_HEALTH, boss.getMaxHealth() * share);
        dmzrevamp$set(self, Attributes.ATTACK_DAMAGE, boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * share);
        dmzrevamp$set(self, EntityAttributes.KI_BLAST_DAMAGE.get(), boss.getAttributeValue(EntityAttributes.KI_BLAST_DAMAGE.get()) * share);
        dmzrevamp$set(self, DmzRevampAttributes.MOB_DEFENSE.get(), boss.getAttributeValue(DmzRevampAttributes.MOB_DEFENSE.get()) * share);
        self.setHealth((float) Math.max(1D, boss.getMaxHealth() * share));
        self.getPersistentData().putBoolean("dmzrevamp_worldboss_minion_configured", true);
    }
    private static void dmzrevamp$set(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(Math.max(0D, value));
    }
}
