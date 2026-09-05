package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.combat.AdaptiveDefenseDamageContext;
import com.dmzrevamp.revamp.strike.StrikeClashManager;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.helper.ComboManager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ComboManager.class, remap = false)
public abstract class SagaComboManagerStrikeClashMixin {
    @Unique
    private static final ThreadLocal<AdaptiveDefenseDamageContext.Scope> DMZREVAMP_ADAPTIVE_DEFENSE_SCOPE = new ThreadLocal<>();

    @Inject(method = "handleCombo", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dmzrevamp$pauseComboDuringStrikeClash(
            DBSagasEntity user,
            LivingEntity target,
            int comboId,
            int timer,
            CallbackInfo ci
    ) {
        if (user != null && StrikeClashManager.isClashing(user.getUUID())) {
            ci.cancel();
            return;
        }
        if (user == null || target == null) return;
        DBSagasEntity.ComboType combo = DBSagasEntity.ComboType.fromId(comboId);
        if (combo == null || combo == DBSagasEntity.ComboType.SLEEP_RECOVERY) return;

        double totalDamage = user.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * combo.getTier().getDamageMultiplier();
        DMZREVAMP_ADAPTIVE_DEFENSE_SCOPE.set(AdaptiveDefenseDamageContext.push(
                AdaptiveDefenseDamageContext.AttackType.STRIKE,
                totalDamage
        ));
    }

    @Inject(method = "handleCombo", at = @At("RETURN"), require = 1)
    private static void dmzrevamp$clearNpcComboAdaptiveDefenseContext(
            DBSagasEntity user,
            LivingEntity target,
            int comboId,
            int timer,
            CallbackInfo ci
    ) {
        AdaptiveDefenseDamageContext.Scope scope = DMZREVAMP_ADAPTIVE_DEFENSE_SCOPE.get();
        if (scope != null) {
            scope.close();
            DMZREVAMP_ADAPTIVE_DEFENSE_SCOPE.remove();
        }
    }
}
