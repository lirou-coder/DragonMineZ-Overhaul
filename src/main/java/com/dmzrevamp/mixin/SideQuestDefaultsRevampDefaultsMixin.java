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

/** Replaces only the selected QUEST-spawn kill sidequests; DMZ retains every other default. */
@Mixin(targets = "com.dragonminez.common.quest.SideQuestDefaults", remap = false)
public abstract class SideQuestDefaultsRevampDefaultsMixin {
    @Unique
    private static final ThreadLocal<String> dmzrevamp$currentSideQuestDefaultPath = new ThreadLocal<>();

    @Inject(method = "writeQuestFile", at = @At("HEAD"))
    private static void dmzrevamp$captureSideQuestPath(Path folder, String fileName, JsonObject quest, CallbackInfo ci) {
        Path category = folder.getFileName();
        dmzrevamp$currentSideQuestDefaultPath.set("sidequests/"
                + (category == null ? "" : category.toString()) + "/" + fileName);
    }

    @ModifyArg(
            method = "writeQuestFile",
            at = @At(value = "INVOKE", target = "Lcom/dragonminez/common/quest/QuestUpgrader;upgradeOrWrite(Ljava/nio/file/Path;Ljava/nio/file/Path;Lcom/google/gson/JsonObject;)V"),
            index = 2
    )
    private static JsonObject dmzrevamp$useSelectedSideQuestDefault(JsonObject original) {
        JsonObject replacement = DefaultConfigSnapshots.questDefault(dmzrevamp$currentSideQuestDefaultPath.get());
        return replacement == null ? original : replacement;
    }

    @Inject(method = "writeQuestFile", at = @At("RETURN"))
    private static void dmzrevamp$clearSideQuestPath(Path folder, String fileName, JsonObject quest, CallbackInfo ci) {
        dmzrevamp$currentSideQuestDefaultPath.remove();
    }
}
