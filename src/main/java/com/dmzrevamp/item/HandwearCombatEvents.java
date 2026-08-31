package com.dmzrevamp.item;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.compat.apotheosis.ApotheosisHandwearBridge;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HandwearCombatEvents {
    private static final Map<UUID, Integer> TARGET_FIRE_BEFORE_HELD_HANDWEAR_ATTACK = new ConcurrentHashMap<>();

    private HandwearCombatEvents() {
    }

    @SubscribeEvent
    public static void syncEquippedDmzBonuses(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || event.player.tickCount % 20 != 0 || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            // Equipped is not the same as active: handwear only contributes while
            // it is in the Curios hands slot and the player's main hand is empty.
            boolean changed = syncBonus(data.getBonusStats(), "STR", HandwearHelper.hasActiveWristbands(player));
            changed |= syncBonus(data.getBonusStats(), "PWR", HandwearHelper.hasActiveGloves(player));
            if (changed) {
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rememberFireBeforeHeldHandwearAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.getMainHandItem().getItem() instanceof HandwearItem) {
            TARGET_FIRE_BEFORE_HELD_HANDWEAR_ATTACK.put(event.getEntity().getUUID(), event.getEntity().getRemainingFireTicks());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void runApotheosisHandwearAttackHooks(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getEntity() == attacker) {
            return;
        }
        cancelHeldHandwearEnchantments(event, attacker);
        HandwearHelper.getAnyActiveHandwear(attacker).ifPresent(stack -> {
            // Vanilla only reads the main hand for weapon enchantments; handwear has to contribute explicitly.
            event.setAmount(applyCurioWeaponEnchantments(stack, attacker, event.getEntity(), event.getAmount()));
            // These hooks let sword affixes and gems react as if the equipped handwear was the weapon.
            float amount = ApotheosisHandwearBridge.applyOutgoingHurt(stack, event.getSource(), event.getEntity(), event.getAmount());
            event.setAmount(amount);
            ApotheosisHandwearBridge.runPostAttack(stack, attacker, event.getEntity());
            damageActiveHandwear(stack, attacker);
        });
    }

    private static void damageActiveHandwear(ItemStack stack, LivingEntity attacker) {
        if (attacker.level().isClientSide() || stack.isEmpty() || !(stack.getItem() instanceof HandwearItem)) {
            return;
        }

        // Empty-hand Curios handwear loses durability like a weapon hit, including Unbreaking rolls.
        stack.hurtAndBreak(1, attacker, entity -> {
        });
    }

    private static float applyCurioWeaponEnchantments(ItemStack stack, LivingEntity attacker, LivingEntity target, float amount) {
        float modified = amount + EnchantmentHelper.getDamageBonus(stack, target.getMobType());
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }
        int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack);
        if (knockback > 0) {
            double x = attacker.getX() - target.getX();
            double z = attacker.getZ() - target.getZ();
            target.knockback(knockback * 0.5D, x, z);
        }
        if (attacker instanceof Player player
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack) > 0) {
            // Player#attack normally emits this enchanted-hit feedback from the
            // held stack. Curios handwear is virtual, so reproduce that packet.
            player.magicCrit(target);
        }
        EnchantmentHelper.doPostHurtEffects(target, attacker);
        EnchantmentHelper.doPostDamageEffects(attacker, target);
        return modified;
    }

    private static void cancelHeldHandwearEnchantments(LivingHurtEvent event, LivingEntity attacker) {
        ItemStack held = attacker.getMainHandItem();
        if (!(held.getItem() instanceof HandwearItem)) {
            return;
        }
        float bonus = EnchantmentHelper.getDamageBonus(held, event.getEntity().getMobType());
        if (bonus > 0F) {
            event.setAmount(Math.max(0F, event.getAmount() - bonus));
        }
        Integer previousFire = TARGET_FIRE_BEFORE_HELD_HANDWEAR_ATTACK.remove(event.getEntity().getUUID());
        if (previousFire != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, held) > 0) {
            event.getEntity().setRemainingFireTicks(previousFire);
        }
    }

    private static boolean syncBonus(com.dragonminez.common.stats.character.BonusStats bonuses, String stat, boolean equipped) {
        boolean hasBonus = bonuses.hasBonus(stat, HandwearItem.EMPTY_HAND_BONUS_NAME);
        if (equipped && !hasBonus) {
            // This mirrors DMZ's /dmzbonus stat * value name storage, not a Minecraft attribute modifier.
            bonuses.addBonusSplit(stat, HandwearItem.EMPTY_HAND_BONUS_NAME, "*", 1.05D, false);
            return true;
        }
        if (!equipped && hasBonus) {
            bonuses.removeBonusSplit(stat, HandwearItem.EMPTY_HAND_BONUS_NAME);
            return true;
        }
        return false;
    }
}
