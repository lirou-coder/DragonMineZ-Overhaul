package com.dmzrevamp.revamp.strike;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LivingWorldStrikeClashGraceEvents {
    private LivingWorldStrikeClashGraceEvents() {}

    @SubscribeEvent
    public static void tickGraceWindow(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide
                && entity.getClass().getName().startsWith("com.dmzlivingworld.entity.")) {
            StrikeClashManager.tickLivingWorldGrace(entity);
        }
    }
}
