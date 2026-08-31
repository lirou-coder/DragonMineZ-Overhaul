package com.dmzrevamp.revamp.ki;

import com.dmzrevamp.DmzRevampMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KiAttackMobSpeedDebuffs {
    private static final String EXPIRES_TAG = "dmzrevamp_mob_spd_debuff_expires";
    private static final String AMOUNT_TAG = "dmzrevamp_mob_spd_debuff_amount";
    private static final UUID MOVEMENT_MODIFIER_ID = UUID.fromString("d74e34e4-6635-493f-a3cf-ff2368226a70");
    private static final UUID ATTACK_MODIFIER_ID = UUID.fromString("e260a04b-3776-4fd4-b6c8-ad8dd4dc5799");
    private static final String MODIFIER_NAME = "Dragon Mine Z: Overhaul ki SPD debuff";

    private KiAttackMobSpeedDebuffs() {
    }

    public static void apply(Mob mob, double factor, int durationTicks) {
        if (mob.level().isClientSide() || factor >= 0.0D || durationTicks <= 0) {
            return;
        }
        double amount = Math.max(-0.95D, factor);
        long expiresAt = mob.level().getGameTime() + durationTicks;
        CompoundTag tag = mob.getPersistentData();
        tag.putDouble(AMOUNT_TAG, amount);
        tag.putLong(EXPIRES_TAG, Math.max(expiresAt, tag.getLong(EXPIRES_TAG)));
        applyModifier(mob, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER_ID, amount);
        applyModifier(mob, Attributes.ATTACK_SPEED, ATTACK_MODIFIER_ID, amount);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        CompoundTag tag = mob.getPersistentData();
        if (!tag.contains(EXPIRES_TAG)) {
            return;
        }
        if (mob.level().getGameTime() >= tag.getLong(EXPIRES_TAG)) {
            removeModifier(mob, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER_ID);
            removeModifier(mob, Attributes.ATTACK_SPEED, ATTACK_MODIFIER_ID);
            tag.remove(EXPIRES_TAG);
            tag.remove(AMOUNT_TAG);
            return;
        }
        double amount = tag.getDouble(AMOUNT_TAG);
        applyModifier(mob, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER_ID, amount);
        applyModifier(mob, Attributes.ATTACK_SPEED, ATTACK_MODIFIER_ID, amount);
    }

    private static void applyModifier(Mob mob, Attribute attribute, UUID id, double amount) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.getAmount() == amount) {
            return;
        }
        if (current != null) {
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, MODIFIER_NAME, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeModifier(Mob mob, Attribute attribute, UUID id) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }
}
