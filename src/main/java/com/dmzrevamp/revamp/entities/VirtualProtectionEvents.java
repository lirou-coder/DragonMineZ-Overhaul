package com.dmzrevamp.revamp.entities;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.quest.QuestSpawnAttributeApplier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies the explicit Protection stat from entity/quest data without converting other stats. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VirtualProtectionEvents {
    private static final int PROTECTION_CAP = 20;

    private VirtualProtectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyVirtualProtection(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || event.getAmount() <= 0F
                || event.getSource().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return;
        }

        int virtualProtection = Math.max(0,
                (int) Math.round(QuestSpawnAttributeApplier.protectionValue(mob)));
        if (virtualProtection <= 0) {
            return;
        }

        int equipment = Math.min(PROTECTION_CAP, equipmentProtection(mob));
        int combined = Math.min(PROTECTION_CAP, equipment + virtualProtection);
        double equipmentRemainder = PROTECTION_CAP + 5D - equipment;
        double combinedRemainder = PROTECTION_CAP + 5D - combined;
        event.setAmount((float) (event.getAmount() * (combinedRemainder / equipmentRemainder)));
    }

    private static int equipmentProtection(LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
        }
        return Math.max(0, total);
    }
}
