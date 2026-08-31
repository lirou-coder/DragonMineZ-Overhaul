package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.client.render.effects.KiSenseAuraRenderer;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = KiSenseAuraRenderer.class, remap = false)
public abstract class KiSenseAuraRendererHiddenBpMixin {
    @Redirect(method = "renderAuras", at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/systems/kisense/KiSenseScan;getSearchEntities()Ljava/util/Set;"), require = 0)
    private static Set<Integer> dmzrevamp$hideUnknownBattlePowerAuras() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return KiSenseScan.getSearchEntities();
        }

        Set<Integer> visible = new HashSet<>();
        for (int id : KiSenseScan.getSearchEntities()) {
            Entity entity = minecraft.level.getEntity(id);
            if (!(entity instanceof LivingEntity livingEntity) || !ManualBattlePowerStatEvents.isKiSenseHiddenEntity(livingEntity)) {
                visible.add(id);
            }
        }
        return visible;
    }
}
