package com.dmzrevamp.mixin;

import com.dmzrevamp.revamp.quest.RaidTournamentConfigBridge;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.config.TournamentDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.dragonminez.common.config.ConfigLoader", remap = false)
public abstract class ConfigLoaderRaidTournamentFieldsMixin {
    @Redirect(method = "loadConfig",
            at = @At(value = "INVOKE", target = "Lcom/google/gson/Gson;fromJson(Lcom/google/gson/JsonElement;Ljava/lang/Class;)Ljava/lang/Object;"),
            require = 0)
    private Object dmzrevamp$readRevampRaidAndTournamentFields(Gson gson, JsonElement tree, Class<?> type) {
        Object result = gson.fromJson(tree, type);
        if (!(tree instanceof JsonObject root)) return result;
        if (result instanceof RaidDefinition raid) {
            JsonArray waves = root.has("waves") && root.get("waves").isJsonArray()
                    ? root.getAsJsonArray("waves") : new JsonArray();
            for (int waveIndex = 0; waveIndex < waves.size(); waveIndex++) {
                JsonObject waveJson = waves.get(waveIndex).isJsonObject() ? waves.get(waveIndex).getAsJsonObject() : null;
                if (waveJson == null || !waveJson.has("spawns") || !waveJson.get("spawns").isJsonArray()) continue;
                JsonArray spawns = waveJson.getAsJsonArray("spawns");
                if (raid.getWaves() == null || waveIndex >= raid.getWaves().size()) continue;
                for (int spawnIndex = 0; spawnIndex < spawns.size(); spawnIndex++) {
                    if (spawns.get(spawnIndex).isJsonObject()
                            && spawnIndex < raid.getWaves().get(waveIndex).getSpawns().size()) {
                        RaidTournamentConfigBridge.populate(spawns.get(spawnIndex).getAsJsonObject(),
                                raid.getWaves().get(waveIndex).getSpawns().get(spawnIndex));
                    }
                }
            }
        } else if (result instanceof TournamentDefinition tournament) {
            String[] names = {"champion", "semifinalist", "finalist"};
            TournamentDefinition.Fighter[] fighters = {
                    tournament.getChampion(), tournament.getSemifinalist(), tournament.getFinalist()
            };
            for (int i = 0; i < names.length; i++) {
                if (root.has(names[i]) && root.get(names[i]).isJsonObject()) {
                    RaidTournamentConfigBridge.populate(root.getAsJsonObject(names[i]), fighters[i]);
                }
            }
            if (root.has("contenders") && root.get("contenders").isJsonArray() && tournament.getContenders() != null) {
                JsonArray contendersJson = root.getAsJsonArray("contenders");
                for (int i = 0; i < Math.min(contendersJson.size(), tournament.getContenders().size()); i++) {
                    if (contendersJson.get(i).isJsonObject()) {
                        RaidTournamentConfigBridge.populate(contendersJson.get(i).getAsJsonObject(), tournament.getContenders().get(i));
                    }
                }
            }
        }
        return result;
    }
}
