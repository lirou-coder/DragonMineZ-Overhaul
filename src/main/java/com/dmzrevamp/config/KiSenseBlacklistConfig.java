package com.dmzrevamp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Entity types that must be invisible to every BP, Ki Sense, and scouter system. */
public final class KiSenseBlacklistConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("dmzrevamp").resolve("kiSenseBlacklist.json");
    private static final List<String> DEFAULTS = List.of("dummmmmmy:target_dummy", "minecraft:armor_stand");
    private static volatile Set<ResourceLocation> entityTypes = Set.of();

    private KiSenseBlacklistConfig() {}

    public static synchronized void initialize() {
        reload();
    }

    public static synchronized void reload() {
        try {
            Files.createDirectories(PATH.getParent());
            if (!Files.isRegularFile(PATH)) Files.writeString(PATH, GSON.toJson(new Values()));
            Values values = GSON.fromJson(Files.readString(PATH), Values.class);
            if (values == null) values = new Values();
            if (values.entities == null) values.entities = new ArrayList<>(DEFAULTS);

            LinkedHashSet<ResourceLocation> parsed = new LinkedHashSet<>();
            for (String raw : values.entities) {
                ResourceLocation id = parseId(raw);
                if (id != null) parsed.add(id);
            }
            entityTypes = Set.copyOf(parsed);
            Files.writeString(PATH, GSON.toJson(values));
        } catch (Exception ignored) {
            LinkedHashSet<ResourceLocation> fallback = new LinkedHashSet<>();
            for (String raw : DEFAULTS) {
                ResourceLocation id = parseId(raw);
                if (id != null) fallback.add(id);
            }
            entityTypes = Set.copyOf(fallback);
        }
    }

    /** Use the stable constructor instead of tryParse, whose mapped name is not
     * available in every supported 1.20.1 runtime remapping path. */
    private static ResourceLocation parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new ResourceLocation(raw.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean contains(Entity entity) {
        if (entity == null) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && entityTypes.contains(id);
    }

    public static final class Values {
        public List<String> entities = new ArrayList<>(DEFAULTS);
    }
}
