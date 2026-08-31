package com.dmzrevamp.item;

import com.dmzrevamp.compat.apotheosis.ApotheosisHandwearBridge;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class HandwearItem extends Item implements ICurioItem {
    private static final String ACTIVE_WITH_EMPTY_HAND_TAG = "DmzRevampActiveWithEmptyHand";
    public static final String EMPTY_HAND_BONUS_NAME = "Empty Hand Bonus";
    private static final double EMPTY_HAND_STAT_MULTIPLIER = 1.05D;
    private final HandwearType type;

    public HandwearItem(HandwearType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public HandwearType getHandwearType() {
        return type;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return HandwearHelper.HANDS_SLOT.equals(slotContext.identifier());
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        applyDmzBonus(slotContext.entity(), type, HandwearHelper.canUseHandwear(slotContext.entity()));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        applyDmzBonus(slotContext.entity(), type, false);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        boolean active = HandwearHelper.canUseHandwear(slotContext.entity());
        boolean previous = stack.getOrCreateTag().getBoolean(ACTIVE_WITH_EMPTY_HAND_TAG);
        if (active != previous) {
            stack.getOrCreateTag().putBoolean(ACTIVE_WITH_EMPTY_HAND_TAG, active);
            // Main-hand changes do not fire a Curios equip event. Update DMZ's
            // persistent STR/PWR bonus at the same point that Curios modifiers
            // are invalidated so neither system can keep a stale empty-hand bonus.
            applyDmzBonus(slotContext.entity(), type, active);
            // Curios caches attribute modifiers, so the cache must refresh when the empty-hand rule changes.
            CuriosApi.getCuriosInventory(slotContext.entity()).ifPresent(handler -> {
                handler.clearCachedSlotModifiers();
                handler.getStacksHandler(HandwearHelper.HANDS_SLOT).ifPresent(curioStacks -> curioStacks.clearCachedModifiers());
            });
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        if (!HandwearHelper.HANDS_SLOT.equals(slotContext.identifier())) {
            return modifiers;
        }

        if (HandwearHelper.canUseHandwear(slotContext.entity())) {
            // Apotheosis writes sword affix and gem attributes as main-hand modifiers; Curios needs them copied here.
            ApotheosisHandwearBridge.addMainHandModifiers(stack, modifiers);
        }
        return modifiers;
    }

    @Override
    public int getLootingLevel(SlotContext slotContext, DamageSource source, LivingEntity target, int baseLooting, ItemStack stack) {
        if (!HandwearHelper.HANDS_SLOT.equals(slotContext.identifier()) || !HandwearHelper.canUseHandwear(slotContext.entity())) {
            return baseLooting;
        }
        return Math.max(baseLooting, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, stack));
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.SWEEPING_EDGE) {
            return false;
        }
        return enchantment.canEnchant(new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack damaged, ItemStack material) {
        if (type == HandwearType.GLOVES) {
            return material.is(Items.LEATHER);
        }
        return material.is(net.minecraft.tags.ItemTags.WOOL);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        // Apotheosis uses the sword tool action to select its sword affix
        // category. This is classification metadata only: the item remains a
        // plain Item and receives no SwordItem combat or mining behavior.
        return toolAction == ToolActions.SWORD_DIG || super.canPerformAction(stack, toolAction);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static void applyDmzBonus(LivingEntity entity, HandwearType type, boolean active) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        String stat = type == HandwearType.WRISTBANDS ? "STR" : "PWR";
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            boolean hadBonus = data.getBonusStats().hasBonus(stat, EMPTY_HAND_BONUS_NAME);
            if (active && !hadBonus) {
                // DMZ stat bonuses are separate from Minecraft attributes and are read by DMZ stat calculations.
                data.getBonusStats().addBonusSplit(stat, EMPTY_HAND_BONUS_NAME, "*", EMPTY_HAND_STAT_MULTIPLIER, false);
            } else if (!active && hadBonus) {
                data.getBonusStats().removeBonusSplit(stat, EMPTY_HAND_BONUS_NAME);
            }
            if (active != hadBonus) {
                NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
            }
        });
    }
}
