package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.ki.KiAttackCategory;
import com.dmzrevamp.revamp.ki.KiAttackEquipRules;
import com.dragonminez.common.network.C2S.EquipTechniqueC2S;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipTechniqueC2S.class)
public abstract class EquipTechniqueLimitMixin {
    @Shadow(remap = false)
    @Final
    private int slotIndex;

    @Shadow(remap = false)
    @Final
    private String techniqueId;

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$limitAdvancedAndUltimateEquips(ServerPlayer player, StatsData data, CallbackInfo ci) {
        KiAttackCategory blocked = KiAttackEquipRules.blockedCategory(data.getTechniques(), slotIndex, techniqueId);
        if (blocked == null) {
            return;
        }

        String label = blocked == KiAttackCategory.ULTIMATE ? "Ultimate" : "Advanced";
        Component message = Component.literal("You can't equip more " + label + " skills.");
        player.sendSystemMessage(message);
        player.displayClientMessage(message, true);
        ci.cancel();
    }
}
