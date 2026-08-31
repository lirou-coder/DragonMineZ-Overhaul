package com.dmzrevamp;

import com.mojang.logging.LogUtils;
import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dmzrevamp.config.CustomStrikeAttacksConfig;
import com.dmzrevamp.config.DynamicGrowthCurveConfig;
import com.dmzrevamp.config.DmzRevampConfig;
import com.dmzrevamp.config.FusionsRevampedConfig;
import com.dmzrevamp.config.KiClashConfigured;
import com.dmzrevamp.config.StrikeClashConfigured;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.config.WeightMovementPenaltyConfig;
import com.dmzrevamp.config.AdaptiveDefenseMoreConfigured;
import com.dmzrevamp.config.racial.DmzRevampRacialConfigs;
import com.dmzrevamp.effect.DmzRevampEffects;
import com.dmzrevamp.entity.DmzRevampEntities;
import com.dmzrevamp.item.DmzRevampItems;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.racial.CustomRacialSkillRegistry;
import com.dmzrevamp.sound.DmzRevampSounds;
import com.dmzrevamp.revamp.classes.skills.ClassPassiveAliases;
import com.dmzrevamp.revamp.classes.DmzClassConfigManager;
import com.dmzrevamp.compat.DmzJackClassCompat;
import com.dmzrevamp.revamp.defaults.PrestigeSagaDefaults;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DmzRevampMod.MODID)
public class DmzRevampMod {
    public static final String MODID = "dmzrevamp";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Forge calls this once while loading the mod; this is where Overhaul registers its items, effects, configs, packets, and custom skill systems.
    public DmzRevampMod() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DmzRevampEffects.register(modEventBus);
        DmzRevampEntities.register(modEventBus);
        DmzRevampItems.register(modEventBus);
        DmzRevampSounds.register(modEventBus);
        LOGGER.warn("Dragon Mine Z: Overhaul does not migrate old generated configs. Delete config/dragonminez/classes and the affected config/dragonminez/races files when class, passive, or stat defaults change.");
        DmzRevampRacialConfigs.loadAll();
        AdaptiveDefenseMoreConfigured.initialize();
        ClassPassiveAliases.register();
        DmzJackClassCompat.registerIfPresent();
        // Load separated class data directly as well as through the DMZ config mixin.
        // Some development/runtime launch paths initialize ConfigManager before that
        // injection is observable, which used to postpone new class files until reload.
        DmzClassConfigManager.reload();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DmzRevampConfig.SPEC);
        CustomBattlePowerConfig.initialize();
        CustomStrikeAttacksConfig.initialize();
        DynamicGrowthCurveConfig.initialize();
        FusionsRevampedConfig.initialize();
        KiClashConfigured.initialize();
        StrikeClashConfigured.initialize();
        LevelingRevampConfig.initialize();
        WeightMovementPenaltyConfig.initialize();
        DmzRevampNetwork.register();
        CustomRacialSkillRegistry.bootstrap();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::writePrestigeSagaBeforeQuestLoad);
    }

    private void writePrestigeSagaBeforeQuestLoad(ServerAboutToStartEvent event) {
        var worldFolder = event.getServer().getWorldPath(LevelResource.ROOT);
        PrestigeSagaDefaults.writeAllDefaults(worldFolder.resolve("dragonminez").resolve("quests"));
    }
}
