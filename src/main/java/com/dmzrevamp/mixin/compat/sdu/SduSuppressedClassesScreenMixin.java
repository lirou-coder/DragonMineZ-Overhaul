package com.dmzrevamp.mixin.compat.sdu;

import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.RequestSduClassEditorC2SPacket;
import net.minecraft.network.chat.Component;
import net.shurui.dev.sdu.client.DmzAssets;
import net.shurui.dev.sdu.client.gui.race.SuppressedClassesScreen;
import net.shurui.dev.sdu.race.SuppressedDefaultsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Pseudo
@Mixin(value = SuppressedClassesScreen.class, remap = false)
public abstract class SduSuppressedClassesScreenMixin {
    @Shadow
    private int scroll;

    @Inject(method = "allClasses", at = @At("HEAD"), cancellable = true, require = 0)
    private void dmzrevamp$listSeparatedGlobalClasses(CallbackInfoReturnable<List<String>> cir) {
        TreeSet<String> classes = new TreeSet<>(List.of(
                "warrior", "martialartist", "spiritualist", "berserker", "paladin", "tank", "cleric"));
        classes.addAll(DmzAssets.raceClasses());
        classes.addAll(SuppressedDefaultsClient.classes());
        classes.remove("race");
        cir.setReturnValue(new ArrayList<>(classes));
    }

    @Inject(method = "m_7856_", at = @At("TAIL"), require = 0)
    private void dmzrevamp$addGlobalClassEditorButtons(CallbackInfo ci) {
        TreeSet<String> classSet = new TreeSet<>(List.of(
                "warrior", "martialartist", "spiritualist", "berserker", "paladin", "tank", "cleric"));
        classSet.addAll(DmzAssets.raceClasses());
        List<String> classes = new ArrayList<>(classSet);
        classes.addAll(SuppressedDefaultsClient.classes());
        classes = new ArrayList<>(new TreeSet<>(classes));
        classes.remove("race");

        SduSagaBaseScreenInvoker screen = (SduSagaBaseScreenInvoker) this;
        int end = Math.min(classes.size(), scroll + 12);
        int y = 34;
        for (int index = scroll; index < end; index++) {
            String classId = classes.get(index);
            screen.dmzrevamp$button(200, y, 36, 14, Component.literal("Edit"),
                    () -> DmzRevampNetwork.CHANNEL.sendToServer(
                            new RequestSduClassEditorC2SPacket(classId, false)));
            y += 16;
        }

        screen.dmzrevamp$button(95, screen.dmzrevamp$footerY(), 110, screen.dmzrevamp$footerButtonHeight(), Component.literal("New Class"),
                () -> DmzRevampNetwork.CHANNEL.sendToServer(
                        new RequestSduClassEditorC2SPacket("new_class", true)));
    }
}
