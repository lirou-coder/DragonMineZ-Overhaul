package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ScouterClientState;
import com.dmzrevamp.revamp.battlepower.CustomBattlePowerCalculator;
import com.dmzrevamp.revamp.battlepower.ManualBattlePowerStatEvents;
import com.dragonminez.client.gui.hud.ScouterHUD;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.config.GeneralUserConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(value = ScouterHUD.class, remap = false)
public abstract class ScouterHUDRevampMixin {
    @Redirect(method = "lambda$static$1",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/config/GeneralUserConfig;getAlternativeHud()Ljava/lang/Boolean;"),
            require = 0)
    private static Boolean dmzrevamp$renderScouterOnAlternativeHud(GeneralUserConfig config) {
        return Boolean.FALSE;
    }

    @Inject(method = "getEntityBP", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$useRevampBattlePower(LivingEntity entity, CallbackInfoReturnable<Double> cir) {
        if (entity instanceof Player player) {
            if (com.dragonminez.common.util.CuriosUtil.getFirstStack(player, "head_tech").getItem() == MainItems.ANTI_KI_CLOAK.get()) {
                cir.setReturnValue(0.0D);
                return;
            }
            StatsProvider.get(StatsCapability.INSTANCE, player)
                    .map(data -> (double) data.getBattlePower())
                    .ifPresent(cir::setReturnValue);
            return;
        }

        if (ManualBattlePowerStatEvents.isKiSenseHiddenEntity(entity)) {
            cir.setReturnValue(Double.MAX_VALUE);
            return;
        }

        long battlePower = ManualBattlePowerStatEvents.displayedBattlePower(entity, -1L);
        if (battlePower >= 0L) {
            cir.setReturnValue((double) battlePower);
        }
    }

    @Inject(method = "formatBP", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$formatScouterBattlePower(double battlePower, CallbackInfoReturnable<String> cir) {
        if (battlePower > 9999.0D) {
            cir.setReturnValue(String.format(Locale.ROOT, "%.1fK", battlePower / 1000.0D).replace(".0K", "K"));
        } else {
            cir.setReturnValue(Long.toString(Math.round(battlePower)));
        }
    }

    @ModifyConstant(method = {"performSmartScan", "lambda$static$1"}, constant = @Constant(doubleValue = 1.5E8D), require = 0)
    private static double dmzrevamp$useConfiguredScouterBreakBattlePower(double original) {
        return CustomBattlePowerCalculator.calculateScouterBreakBattlePower();
    }

    @ModifyConstant(method = "performSmartScan", constant = @Constant(doubleValue = 50.0D), require = 0)
    private static double dmzrevamp$scanLoadedEntitiesAcrossLongRange(double original) {
        return 2000.0D;
    }

    @ModifyConstant(method = "lambda$static$1", constant = @Constant(doubleValue = 50.0D, ordinal = 1), require = 0)
    private static double dmzrevamp$renderCachedSmartScanTargetAcrossLongRange(double original) {
        return 2000.0D;
    }

    @Redirect(
            method = "lambda$static$1",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/hud/ScouterHUD;renderCustomNumbers(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)V"),
            require = 0
    )
    private static void dmzrevamp$drawReadableBattlePower(GuiGraphics graphics, ResourceLocation texture, String battlePower) {
        ScouterClientState.drawBattlePowerText(graphics, battlePower);
    }

    @Redirect(
            method = "renderEntityInfo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280163_(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", ordinal = 0),
            require = 0
    )
    private static void dmzrevamp$centerExaminedEntityCircle(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        // The original draw is inside a 2x scaled transform; these local offsets center it between the four arrows.
        graphics.blit(texture, -3, 8, u, v, width, height, textureWidth, textureHeight);
    }

    @Inject(
            method = "lambda$static$1",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/hud/ScouterHUD;renderScouterFrame(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;)V", shift = At.Shift.AFTER),
            cancellable = true,
            require = 0
    )
    private static void dmzrevamp$renderMasterSearchInsteadOfEntitySearch(ForgeGui forgeGui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) {
        if (!ScouterClientState.isMasterSearch()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ResourceLocation texture = dmzrevamp$scouterTextureFor(player);
            ScouterClientState.renderMasterSearch(graphics, texture);
        }
        graphics.pose().popPose();
        ci.cancel();
    }

    @Inject(method = "onClientTick", at = @At("TAIL"), require = 0)
    private static void dmzrevamp$tickScouterState(net.minecraftforge.event.TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            ScouterClientState.tick(Minecraft.getInstance().player);
        }
    }

    @Inject(method = "damageScouter", at = @At("HEAD"), require = 0)
    private static void dmzrevamp$unlockWhenHighBattlePowerDamagesScouter(Player player, CallbackInfo ci) {
        ScouterClientState.forceUnlockScouterBackedLock();
    }

    @Inject(method = "setRenderingInfo", at = @At("RETURN"), require = 0)
    private static void dmzrevamp$syncExternalScouterShutdown(boolean rendering, CallbackInfo ci) {
        if (!rendering) {
            ScouterClientState.forceOff();
        }
    }

    private static ResourceLocation dmzrevamp$scouterTextureFor(Player player) {
        var stack = com.dragonminez.common.util.CuriosUtil.getFirstStack(player, "head_tech");
        String id = stack.getItem().getDescriptionId();
        if (id.contains("blue_scouter")) {
            return ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/gui/scouter/scouter_blue.png");
        }
        if (id.contains("red_scouter")) {
            return ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/gui/scouter/scouter_red.png");
        }
        if (id.contains("purple_scouter")) {
            return ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/gui/scouter/scouter_purple.png");
        }
        return ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/gui/scouter/scouter_green.png");
    }
}
