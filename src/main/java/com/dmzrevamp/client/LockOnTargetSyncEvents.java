package com.dmzrevamp.client;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.mixin.client.LockOnEventAccessor;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.SyncLockOnTargetC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LockOnTargetSyncEvents {
    private static int lastTargetId = Integer.MIN_VALUE;

    private LockOnTargetSyncEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastTargetId = Integer.MIN_VALUE;
            return;
        }
        LivingEntity target = LockOnEventAccessor.dmzrevamp$getLockedTarget();
        int targetId = target != null && target.isAlive() && target.level() == minecraft.player.level() ? target.getId() : -1;
        if (targetId == lastTargetId) return;
        lastTargetId = targetId;
        DmzRevampNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new SyncLockOnTargetC2SPacket(targetId));
    }
}
