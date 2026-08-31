package com.dmzrevamp.revamp.battlepower;

import com.dmzrevamp.config.CustomBattlePowerConfig;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public final class AccurateMobBattlePowerCalculator {
    public static final long HIDDEN_BATTLE_POWER = Integer.MAX_VALUE;
    public static final int STORED_VISIBLE_MAX = Integer.MAX_VALUE - 1;

    private static final ResourceLocation AUTOLEVELING_PROJECTILE_DAMAGE =
            new ResourceLocation("autoleveling", "monster.projectile_damage_bonus");
    private static final ResourceLocation APOTHIC_ARROW_DAMAGE =
            new ResourceLocation("attributeslib", "arrow_damage");
    private static final ResourceLocation AUTOLEVELING_EXPLOSION_DAMAGE =
            new ResourceLocation("autoleveling", "monster.explosion_damage_bonus");
    private static final ResourceLocation IRONS_SPELL_POWER =
            new ResourceLocation("irons_spellbooks", "spell_power");

    private AccurateMobBattlePowerCalculator() {
    }

    public static long calculateCurvedBattlePower(LivingEntity entity) {
        double totalPower = calculateTotalPower(entity);
        if (!Double.isFinite(totalPower) || totalPower <= 0D) {
            return 0L;
        }
        return CustomBattlePowerCalculator.calculateMobBattlePower(totalPower);
    }

    public static int toStoredVisibleBattlePower(long battlePower) {
        if (battlePower <= 0L) {
            return 0;
        }
        return (int) Math.min(STORED_VISIBLE_MAX, battlePower);
    }

    private static double calculateTotalPower(LivingEntity entity) {
        CustomBattlePowerConfig.Config config = CustomBattlePowerConfig.get();
        double total = 0D;
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "maxHealth", attributeValue(entity, Attributes.MAX_HEALTH));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "attackDamage", attributeValue(entity, Attributes.ATTACK_DAMAGE));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "armor", attributeValue(entity, Attributes.ARMOR));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "armorToughness", attributeValue(entity, Attributes.ARMOR_TOUGHNESS));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "protection", protectionLevels(entity));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "resistance", resistanceEffectPower(entity));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "movementOrFlyingSpeed", movementOrFlyingSpeed(entity));
        total += weightedMobValue(config.mobStats, "kiDamage", "kiBlastDamage", kiDamage(entity));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "arrowDamage",
                optionalAttributeValue(entity, "attributeslib", APOTHIC_ARROW_DAMAGE, true));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "autoLevelingProjectileDamage", autoLevelingBonus(entity, AUTOLEVELING_PROJECTILE_DAMAGE));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "autoLevelingExplosionDamage", autoLevelingBonus(entity, AUTOLEVELING_EXPLOSION_DAMAGE));
        total += CustomBattlePowerConfig.weightedValue(config.mobStats, "ironsSpellPower", ironsSpellPower(entity));
        return total;
    }

    private static double weightedMobValue(Map<String, CustomBattlePowerConfig.StatRule> rules, String key, String legacyKey, double value) {
        if (rules != null && rules.containsKey(key)) {
            return CustomBattlePowerConfig.weightedValue(rules, key, value);
        }
        return CustomBattlePowerConfig.weightedValue(rules, legacyKey, value);
    }

    private static double protectionLevels(LivingEntity entity) {
        int levels = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            levels += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, armor);
        }
        return levels + QuestSpawnAttributeApplier.protectionValue(entity);
    }

    private static double resistanceEffectPower(LivingEntity entity) {
        MobEffectInstance resistance = entity.getEffect(MobEffects.DAMAGE_RESISTANCE);
        // The configurable weight is per Resistance level. Amplifier 3 is level 4.
        return resistance == null ? 0D : resistance.getAmplifier() + 1D;
    }

    private static double movementOrFlyingSpeed(LivingEntity entity) {
        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            return sanitize(movement.getValue());
        }
        return attributeValue(entity, EntityAttributes.FLY_SPEED.get());
    }

    private static double kiDamage(LivingEntity entity) {
        double questKiDamage = sanitize(QuestSpawnAttributeApplier.questKiDamageValue(entity));
        if (questKiDamage > 0D) {
            return questKiDamage;
        }

        CompoundTag tag = entity.getPersistentData();
        if (tag.contains("KiBlastDamage")) {
            double savedKiDamage = sanitize(tag.getDouble("KiBlastDamage"));
            if (savedKiDamage > 0D) {
                return savedKiDamage;
            }
        }
        if (tag.contains("kiDamage")) {
            double savedKiDamage = sanitize(tag.getDouble("kiDamage"));
            if (savedKiDamage > 0D) {
                return savedKiDamage;
            }
        }

        if (entity instanceof DBSagasEntity saga) {
            double sagaKiDamage = sanitize(saga.getKiBlastDamage());
            if (sagaKiDamage > 0D) {
                return sagaKiDamage;
            }
        }

        double mainKiDamage = positivePower(entity, MainAttributes.KI_DAMAGE.get());
        if (mainKiDamage > 0D) {
            return mainKiDamage;
        }
        return positivePower(entity, EntityAttributes.KI_BLAST_DAMAGE.get());
    }

    private static double autoLevelingBonus(LivingEntity entity, ResourceLocation id) {
        if (!ModList.get().isLoaded("autoleveling")) {
            return 0D;
        }

        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
        if (instance == null) {
            return 0D;
        }
        return Math.max(0D, sanitize(instance.getValue()) - 1D);
    }

    private static double optionalAttributeValue(
            LivingEntity entity,
            String modId,
            ResourceLocation id,
            boolean subtractDefaultOne
    ) {
        if (!ModList.get().isLoaded(modId)) return 0D;
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
        if (instance == null) return 0D;
        double value = sanitize(instance.getValue());
        return Math.max(0D, subtractDefaultOne ? value - 1D : value);
    }

    private static double ironsSpellPower(LivingEntity entity) {
        if (!ModList.get().isLoaded("irons_spellbooks")) {
            return 0D;
        }

        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(IRONS_SPELL_POWER);
        AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
        if (instance == null) {
            return 0D;
        }
        return Math.max(0D, sanitize(instance.getValue()));
    }

    private static double positivePower(LivingEntity entity, Attribute attribute) {
        return Math.max(0D, attributeValue(entity, attribute));
    }

    private static double attributeValue(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0D : sanitize(instance.getValue());
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? value : 0D;
    }
}
