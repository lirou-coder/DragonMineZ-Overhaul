package com.dmzrevamp.mixin.client;

import com.dmzrevamp.revamp.quest.ExtraDifficulties;
import com.dragonminez.client.gui.character.QuestTreeScreen;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.common.quest.Difficulty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = QuestTreeScreen.class, remap = false)
public abstract class QuestTreeExtraDifficultiesMixin extends BaseMenuScreen {
    @Unique private static final ResourceLocation DMZREVAMP$SMOOTH_FONT =
            ResourceLocation.fromNamespaceAndPath("dragonminez", "smooth");
    @Unique private float dmzrevamp$difficultyListScroll;
    @Unique private float dmzrevamp$difficultyListMaxScroll;

    @Shadow @Final @Mutable private static Difficulty[] DIFFICULTY_OPTIONS;
    @Shadow @Final @Mutable private float[] diffOptScroll;
    @Shadow @Final @Mutable private float[] diffOptMaxScroll;
    @Shadow @Final @Mutable private ScrollbarState[] diffOptBars;

    protected QuestTreeExtraDifficultiesMixin() {
        super(Component.empty());
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void dmzrevamp$showAllDifficulties(CallbackInfo ci) {
        DIFFICULTY_OPTIONS = Difficulty.values();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void dmzrevamp$resizeDifficultyUiState(CallbackInfo ci) {
        int count = DIFFICULTY_OPTIONS.length;
        diffOptScroll = new float[count];
        diffOptMaxScroll = new float[count];
        diffOptBars = new ScrollbarState[count];
        for (int i = 0; i < count; i++) diffOptBars[i] = new ScrollbarState();
    }

    @Shadow
    private boolean shouldShowDifficultySelect() {
        throw new AssertionError();
    }

    @ModifyArg(
            method = "getDifficultyOptionRect",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/client/gui/character/QuestTreeScreen$PanelRect;<init>(IIII)V"),
            index = 1
    )
    private int dmzrevamp$scrollDifficultyOptionY(int y) {
        return y - Math.round(dmzrevamp$difficultyListScroll);
    }

    @Inject(
            method = "renderDifficultySelectOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280618_()V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void dmzrevamp$clipDifficultyList(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        int popupX = (getUiWidth() - 220) / 2;
        int popupY = (getUiHeight() - 196) / 2;
        int viewportTop = popupY + 46;
        int viewportBottom = popupY + 189;
        int contentHeight = Math.max(0, DIFFICULTY_OPTIONS.length * 47 - 5);
        dmzrevamp$difficultyListMaxScroll = Math.max(0F, contentHeight - (viewportBottom - viewportTop));
        dmzrevamp$difficultyListScroll = net.minecraft.util.Mth.clamp(
                dmzrevamp$difficultyListScroll, 0F, dmzrevamp$difficultyListMaxScroll);
        graphics.enableScissor(
                toScreenCoord(popupX + 8), toScreenCoord(viewportTop),
                toScreenCoord(popupX + 212), toScreenCoord(viewportBottom));
    }

    @Inject(method = "renderDifficultySelectOverlay", at = @At("TAIL"))
    private void dmzrevamp$finishDifficultyListClip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        graphics.disableScissor();
    }

    @Inject(method = "m_6050_(DDD)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzrevamp$scrollWholeDifficultyList(double mouseX, double mouseY, double delta,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (!shouldShowDifficultySelect() || dmzrevamp$difficultyListMaxScroll <= 0F) return;
        double uiX = toUiX(mouseX);
        double uiY = toUiY(mouseY);
        int popupX = (getUiWidth() - 220) / 2;
        int popupY = (getUiHeight() - 196) / 2;
        if (uiX >= popupX + 8 && uiX <= popupX + 212 && uiY >= popupY + 46 && uiY <= popupY + 189) {
            dmzrevamp$difficultyListScroll = net.minecraft.util.Mth.clamp(
                    dmzrevamp$difficultyListScroll - (float) Math.signum(delta) * 47F,
                    0F, dmzrevamp$difficultyListMaxScroll);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "difficultyLabel", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$extraDifficultyLabel(Difficulty difficulty, CallbackInfoReturnable<Component> cir) {
        if (difficulty == null) return;
        if (ExtraDifficulties.NIGHTMARE.equals(difficulty.name())) {
            cir.setReturnValue(Component.translatable("gui.dmzrevamp.quest_tree.difficulty.nightmare")
                    .setStyle(Style.EMPTY.withFont(DMZREVAMP$SMOOTH_FONT)));
        } else if (ExtraDifficulties.YOU_MUST_DIE.equals(difficulty.name())) {
            cir.setReturnValue(Component.translatable("gui.dmzrevamp.quest_tree.difficulty.you_must_die")
                    .setStyle(Style.EMPTY.withFont(DMZREVAMP$SMOOTH_FONT)));
        }
    }

    @Inject(method = "difficultyLabel", at = @At("RETURN"), cancellable = true)
    private void dmzrevamp$useSmoothDifficultyTitles(Difficulty difficulty,
                                                      CallbackInfoReturnable<Component> cir) {
        Component label = cir.getReturnValue();
        if (label != null) {
            cir.setReturnValue(label.copy().withStyle(style -> style.withFont(DMZREVAMP$SMOOTH_FONT)));
        }
    }

    @Inject(method = "difficultyColor", at = @At("HEAD"), cancellable = true)
    private void dmzrevamp$extraDifficultyColor(Difficulty difficulty, boolean highlight,
                                                CallbackInfoReturnable<Integer> cir) {
        if (difficulty == null) return;
        if (ExtraDifficulties.NIGHTMARE.equals(difficulty.name())) {
            cir.setReturnValue(highlight ? 0xFFCC80FF : 0xFFAA55FF);
        } else if (ExtraDifficulties.YOU_MUST_DIE.equals(difficulty.name())) {
            cir.setReturnValue(highlight ? 0xFFAA2020 : 0xFF770000);
        }
    }
}
