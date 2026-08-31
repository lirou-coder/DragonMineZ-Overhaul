package com.dmzrevamp.compat;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.mixin.common.RangedAttributeMixin;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AttributeFixCompatEvents {
    private static final double DMZ_UNBOUNDED_MAX = 3.4028234663852886E38D;
    private static final double FALLBACK_MAIN_STAT_MAX = 10000D;

    private AttributeFixCompatEvents() {
    }

    @SubscribeEvent
    public static void restoreDmzAttributeCaps(FMLLoadCompleteEvent event) {
        refreshMainAttributeCaps();
    }

    public static void refreshMainAttributeCaps() {
        double mainStatMax = getConfiguredMainStatMax();
        setMax(MainAttributes.STRENGTH, mainStatMax);
        setMax(MainAttributes.STRIKE_POWER, mainStatMax);
        setMax(MainAttributes.RESISTANCE, mainStatMax);
        setMax(MainAttributes.VITALITY, mainStatMax);
        setMax(MainAttributes.KI_POWER, mainStatMax);
        setMax(MainAttributes.ENERGY, mainStatMax);

        setMax(MainAttributes.MAX_ENERGY, DMZ_UNBOUNDED_MAX);
        setMax(MainAttributes.MAX_STAMINA, DMZ_UNBOUNDED_MAX);
        setMax(MainAttributes.MAX_POISE, DMZ_UNBOUNDED_MAX);
        setMax(MainAttributes.MELEE_DAMAGE, DMZ_UNBOUNDED_MAX);
        setMax(MainAttributes.STRIKE_DAMAGE, DMZ_UNBOUNDED_MAX);
        setMax(MainAttributes.KI_DAMAGE, DMZ_UNBOUNDED_MAX);
        setMax(MainAttributes.DEFENSE, DMZ_UNBOUNDED_MAX);
        setMax(EntityAttributes.KI_BLAST_DAMAGE, DMZ_UNBOUNDED_MAX);
        setMax(EntityAttributes.FLY_SPEED, DMZ_UNBOUNDED_MAX);
        setMax(EntityAttributes.KI_BLAST_SPEED, DMZ_UNBOUNDED_MAX);
    }

    private static double getConfiguredMainStatMax() {
        try {
            if (LevelingRevampConfig.levelsEnabled()) {
                return PrestigeSystem.maximumAttribute();
            }
            if (ConfigManager.getServerConfig() != null && ConfigManager.getServerConfig().getGameplay() != null) {
                return ConfigManager.getServerConfig().getGameplay().getMaxValue();
            }
        } catch (RuntimeException ignored) {
        }
        return FALLBACK_MAIN_STAT_MAX;
    }

    private static void setMax(RegistryObject<Attribute> attribute, double maxValue) {
        if (attribute.isPresent()) {
            setMax(attribute.get(), maxValue);
        }
    }

    private static void setMax(Attribute attribute, double maxValue) {
        if (attribute instanceof RangedAttribute rangedAttribute) {
            ((RangedAttributeMixin) rangedAttribute).setMaxValue(maxValue);
        }
    }
}
