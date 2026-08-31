package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.defaults.DefaultConfigSnapshots;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(targets = "com.dragonminez.common.quest.QuestDefaults", remap = false)
public abstract class QuestDefaultsRevampDefaultsMixin {
    @Unique
    private static final ThreadLocal<String> dmzrevamp$currentQuestDefaultPath = new ThreadLocal<>();

    @Inject(method = "writeQuest", at = @At("HEAD"))
    private static void dmzrevamp$captureQuestDefaultPath(Path folder, String fileName, JsonObject quest, CallbackInfo ci) {
        Path parent = folder.getFileName();
        String sagaFolder = parent == null ? "" : parent.toString();
        dmzrevamp$currentQuestDefaultPath.set(sagaFolder + "/" + fileName);
    }

    @ModifyArg(
            method = "writeQuest",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/QuestUpgrader;upgradeOrWrite(Ljava/nio/file/Path;Ljava/nio/file/Path;Lcom/google/gson/JsonObject;)V"),
            index = 2
    )
    private static JsonObject dmzrevamp$useCapturedQuestDefault(JsonObject original) {
        JsonObject replacement = DefaultConfigSnapshots.questDefault(dmzrevamp$currentQuestDefaultPath.get());
        return replacement == null ? original : replacement;
    }

    @Inject(method = "writeQuest", at = @At("RETURN"))
    private static void dmzrevamp$clearQuestDefaultPath(Path folder, String fileName, JsonObject quest, CallbackInfo ci) {
        dmzrevamp$currentQuestDefaultPath.remove();
    }
}
