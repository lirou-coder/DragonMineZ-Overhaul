package com.dmzrevamp.mixin.client;

import com.dragonminez.client.render.DMZRenderHand;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.stats.StatsData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DMZRenderHand.class, remap = false)
public abstract class DMZRenderHandKiWeaponBlockMixin {
    @Unique
    private static final ThreadLocal<Boolean> DMZREVAMP_PUSHED_KI_WEAPON_POSE =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "renderKiWeapon", at = @At("HEAD"))
    private void dmzrevamp$followBlockingHand(PoseStack stack, MultiBufferSource buffer, int light,
                                              AbstractClientPlayer player, StatsData stats, HumanoidArm arm,
                                              CallbackInfo ci) {
        boolean handOnlyFirstPerson = player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()
                && !FirstPersonManager.shouldRenderFirstPerson(player);
        boolean apply = handOnlyFirstPerson && stats.getStatus().isBlocking();
        DMZREVAMP_PUSHED_KI_WEAPON_POSE.set(apply);
        if (!apply) return;

        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        stack.pushPose();
        stack.translate(side * -0.25F, -0.15F, -0.4F);
        stack.mulPose(Axis.XP.rotationDegrees(-20.0F));
        stack.mulPose(Axis.YP.rotationDegrees(100.0F));
        stack.mulPose(Axis.ZP.rotationDegrees(side * 330.0F));
    }

    @Inject(method = "renderKiWeapon", at = @At("RETURN"))
    private void dmzrevamp$restorePose(PoseStack stack, MultiBufferSource buffer, int light,
                                      AbstractClientPlayer player, StatsData stats, HumanoidArm arm,
                                      CallbackInfo ci) {
        if (DMZREVAMP_PUSHED_KI_WEAPON_POSE.get()) {
            stack.popPose();
            DMZREVAMP_PUSHED_KI_WEAPON_POSE.set(false);
        }
    }
}
