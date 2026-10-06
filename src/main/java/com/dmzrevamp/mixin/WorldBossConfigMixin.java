package com.dmzrevamp.mixin;

import com.dmzrevamp.config.WorldBossesConfig;
import com.dmzrevamp.entity.DmzRevampAttributes;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies editable Overhaul stats once the boss becomes active. */
@Mixin(value = WorldBossEntity.class, remap = false)
public abstract class WorldBossConfigMixin {
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void dmzrevamp$tickApplyConfiguredStats(CallbackInfo ci) {
        dmzrevamp$applyConfiguredStats();
    }

    @Unique
    private void dmzrevamp$applyConfiguredStats() {
        WorldBossEntity boss = (WorldBossEntity) (Object) this;
        if (boss.level().isClientSide || boss.getPersistentData().getBoolean("dmzrevamp_worldboss_configured")) return;
        WorldBossesConfig.Boss cfg = WorldBossesConfig.get(dmzrevamp$configKey(boss));
        if (!cfg.enabled) return;
        WorldBossesConfig.Stats stats = cfg.bossStats.get(dmzrevamp$phaseKey(boss));
        if (stats == null) stats = cfg.bossStats.get("base");
        if (stats == null) return;
        int players = 1;
        if (cfg.scaleWithMorePlayers && boss.level() instanceof ServerLevel level) {
            players = Math.max(1, level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                    boss.getBoundingBox().inflate(150), p -> p.isAlive()).size());
        }
        double hp = stats.hp * (1D + Math.max(0, players - 1) * cfg.hpIncreasePerPlayer);
        double damage = 1D + Math.max(0, players - 1) * cfg.damageIncreasePerPlayer;
        double defense = 1D + Math.max(0, players - 1) * cfg.defenseIncreasePerPlayer;
        dmzrevamp$set(boss, Attributes.MAX_HEALTH, hp);
        dmzrevamp$set(boss, Attributes.ATTACK_DAMAGE, stats.meleeDamage * damage);
        dmzrevamp$set(boss, EntityAttributes.KI_BLAST_DAMAGE.get(), stats.kiDamage * damage);
        dmzrevamp$set(boss, DmzRevampAttributes.MOB_DEFENSE.get(), stats.defense * defense);
        dmzrevamp$set(boss, Attributes.MOVEMENT_SPEED, stats.movementSpeed);
        boss.setHealth((float) hp);
        boss.getPersistentData().putBoolean("dmzrevamp_worldboss_configured", true);
    }

    @Inject(method = "wakeUp", at = @At("TAIL"), require = 0)
    private void dmzrevamp$configureAtEngagement(net.minecraft.world.entity.player.Player trigger, CallbackInfo ci) {
        WorldBossEntity boss = (WorldBossEntity) (Object) this;
        boss.getPersistentData().remove("dmzrevamp_worldboss_configured");
        dmzrevamp$applyConfiguredStats();
    }

    @Inject(method = "returnToSleep", at = @At("TAIL"), require = 0)
    private void dmzrevamp$allowReconfigurationAfterReset(CallbackInfo ci) {
        ((WorldBossEntity) (Object) this).getPersistentData().remove("dmzrevamp_worldboss_configured");
    }

    @Unique
    private static String dmzrevamp$configKey(WorldBossEntity boss) {
        if (boss instanceof AllWorldBossesEntity.JanembaFat || boss instanceof AllWorldBossesEntity.SuperJanemba) return "janemba";
        if (boss instanceof AllWorldBossesEntity.Turles) return "turles";
        if (boss instanceof AllWorldBossesEntity.MetalCoolerCore) return "metal_cooler_core";
        if (boss instanceof AllWorldBossesEntity.Gomah) return "gomah";
        if (boss instanceof AllWorldBossesEntity.Tamagami1) return "tamagami_1";
        if (boss instanceof AllWorldBossesEntity.Tamagami2) return "tamagami_2";
        return "tamagami_3";
    }

    @Unique
    private static String dmzrevamp$phaseKey(WorldBossEntity boss) {
        if (boss instanceof AllWorldBossesEntity.SuperJanemba) return "transformed";
        return boss instanceof AllWorldBossesEntity.Tamagami tamagami && tamagami.isPowered() ? "powered" : "base";
    }

    @Unique
    private static void dmzrevamp$set(net.minecraft.world.entity.LivingEntity entity,
                                       net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(Math.max(0D, value));
    }
}
