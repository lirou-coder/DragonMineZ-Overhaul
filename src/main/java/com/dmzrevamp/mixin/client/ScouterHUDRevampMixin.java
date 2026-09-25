package com.dmzrevamp.mixin.client;

import com.dmzrevamp.client.ScouterClientState;
import com.dmzrevamp.revamp.battlepower.CustomBattlePowerCalculator;
import com.dragonminez.client.gui.hud.ScouterHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScouterHUD.class, remap = false)
public abstract class ScouterHUDRevampMixin {
    @Shadow
    private static boolean mirrored;

    @ModifyConstant(method = {"performSmartScan", "renderInfo"}, constant = @Constant(intValue = 150000000), require = 0)
    private static int dmzrevamp$useConfiguredScouterBreakBattlePower(int original) {
        double configured = CustomBattlePowerCalculator.calculateScouterBreakBattlePower();
        if (!Double.isFinite(configured) || configured <= 0.0D) {
            return original;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, Math.round(configured)));
    }

    @Redirect(
            method = {"render", "renderInfo"},
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/hud/ScouterHUD;renderCustomNumbers(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)V"),
            require = 0
    )
    private static void dmzrevamp$drawNormalBattlePowerNumbers(GuiGraphics graphics, ResourceLocation texture, String battlePower) {
        ScouterClientState.drawBattlePowerText(graphics, battlePower, mirrored);
    }

    @Inject(method = "renderInfo", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dmzrevamp$renderMasterSearchInsteadOfEntitySearch(GuiGraphics graphics, Minecraft minecraft,
                                                                          ResourceLocation scouterTexture, CallbackInfo ci) {
        if (!ScouterClientState.isMasterSearch()) {
            return;
        }
        ScouterClientState.renderMasterSearch(graphics, scouterTexture, mirrored);
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
}
