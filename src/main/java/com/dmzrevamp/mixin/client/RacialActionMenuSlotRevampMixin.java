package com.dmzrevamp.mixin.client;

import com.dmzrevamp.racial.CustomRacialActionHelper;
import com.dmzrevamp.racial.CustomRacialSkill;
import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.nodes.RacialSkillNode;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RacialSkillNode.class)
public abstract class RacialActionMenuSlotRevampMixin extends AbstractRadialNode {
    @Inject(method = "label", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$labelCustomRacialAction(StatsData data, CallbackInfoReturnable<Component> cir) {
        if (!dmzrevamp$isVanillaRacialNode()) return;
        CustomRacialSkill skill = actionSkill(data);
        if (skill == null) {
            return;
        }

        ButtonInfo info = skill.buildButtonInfo(data);
        cir.setReturnValue(info != null && info.getLine1() != null
                ? info.getLine1() : Component.translatable("gui.action.dragonminez.racial." + skill.id()));
    }

    @Inject(method = "visible", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$showCustomRacialAction(StatsData data, CallbackInfoReturnable<Boolean> cir) {
        if (!dmzrevamp$isVanillaRacialNode()) return;
        CustomRacialSkill skill = actionSkill(data);
        if (skill == null) {
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "active", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$markCustomRacialActionActive(StatsData data, CallbackInfoReturnable<Boolean> cir) {
        if (!dmzrevamp$isVanillaRacialNode()) return;
        CustomRacialSkill skill = actionSkill(data);
        if (skill == null) {
            return;
        }
        cir.setReturnValue(skill.isActive(data));
    }

    @Inject(method = "onSelect", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$handleCustomRacialAction(StatsData data, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!dmzrevamp$isVanillaRacialNode()) return;
        if (handleClick(data, false)) ci.cancel();
    }

    @Inject(method = "labelColor", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$customRacialColor(StatsData data, CallbackInfoReturnable<Integer> cir) {
        if (!dmzrevamp$isVanillaRacialNode()) return;
        CustomRacialSkill skill = actionSkill(data);
        if (skill != null) cir.setReturnValue(skill.isActive(data) ? GREEN : RED);
    }

    @Override
    public String faceText(StatsData data) {
        if (!dmzrevamp$isVanillaRacialNode()) return "";
        CustomRacialSkill skill = actionSkill(data);
        if (skill == null) return "";
        ButtonInfo info = skill.buildButtonInfo(data);
        return info == null || info.getLine2() == null ? "" : info.getLine2().getString();
    }

    @Override
    public void onDoubleSelect(StatsData data) {
        if (dmzrevamp$isVanillaRacialNode()) handleClick(data, true);
    }

    private boolean handleClick(StatsData data, boolean rightClick) {
        CustomRacialSkill skill = actionSkill(data);
        if (skill == null) return false;
        if (!skill.handleButtonClick(data, rightClick)) {
            boolean wasActive = skill.isActive(data);
            NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.RACIAL));
            playToggle(!wasActive);
        }
        return true;
    }

    private static CustomRacialSkill actionSkill(StatsData data) {
        CustomRacialSkill skill = registeredSkill(data);
        // ButtonInfo describes an opted-in button; it must not create one by itself.
        // Falling through here preserves DMZ's native Saiyan tail toggle and keeps
        // passive racials such as frostrevamp out of the Actions menu.
        return skill != null && skill.showsRacialActionButton(data) ? skill : null;
    }

    private static CustomRacialSkill registeredSkill(StatsData data) {
        if (data == null || data.getCharacter() == null) return null;
        return CustomRacialActionHelper.getCustomRacialSkill(data);
    }

    private boolean dmzrevamp$isVanillaRacialNode() {
        // Mixin methods are inherited by subclasses. External mods commonly extend
        // RacialSkillNode for their own buttons, so never replace their behavior.
        return ((Object) this).getClass() == RacialSkillNode.class;
    }
}
