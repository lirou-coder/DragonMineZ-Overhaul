package com.dmzrevamp.mixin.compat.sdu;

import com.dmzrevamp.compat.sdu.RagnarokObjectiveOverhaulData;
import com.dmzrevamp.compat.sdu.client.OverhaulQuestOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.gui.components.AbstractWidget;
import net.shurui.dev.sdu.client.gui.saga.ObjectiveEditScreen;
import net.shurui.dev.sdu.saga.SagaData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(value = ObjectiveEditScreen.class, remap = false)
public abstract class SduObjectiveEditScreenOverhaulMixin {
    @Shadow private SagaData.Objective obj;
    @Inject(method = "m_7856_", at = @At("TAIL"), require = 0)
    private void dmzrevamp$addOverhaulOptions(CallbackInfo ci) {
        if (!"KILL".equalsIgnoreCase(obj.type)) return;
        int x = 12;
        int y = 218;
        for (var child : ((ObjectiveEditScreen) (Object) this).children()) {
            if (child instanceof AbstractWidget widget
                    && widget.getMessage().getContents() instanceof TranslatableContents translated
                    && "gui.dmz_ragnarok.npc.objective_edit.use_default_transform".equals(translated.getKey())) {
                x = widget.getX();
                y = widget.getY() + widget.getHeight() + 6;
                break;
            }
        }
        ((SduSagaBaseScreenInvoker) this).dmzrevamp$button(x, y, 260, 14, Component.literal("Overhaul Options"), () -> Minecraft.getInstance().setScreen(
                new OverhaulQuestOptionsScreen((ObjectiveEditScreen) (Object) this,
                        ((RagnarokObjectiveOverhaulData) obj).dmzrevamp$getOverhaulOptions())));
    }
}
