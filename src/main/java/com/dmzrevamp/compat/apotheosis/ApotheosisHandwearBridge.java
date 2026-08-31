package com.dmzrevamp.compat.apotheosis;

import com.google.common.collect.Multimap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ApotheosisHandwearBridge {
    private static final String APOTHEOSIS = "apotheosis";
    private static final Reflection REFLECTION = Reflection.create();

    private ApotheosisHandwearBridge() {
    }

    public static void addMainHandModifiers(ItemStack stack, Multimap<Attribute, AttributeModifier> modifiers) {
        if (isUnavailable()) {
            return;
        }
        REFLECTION.addMainHandModifiers(stack, modifiers);
    }

    public static float applyOutgoingHurt(ItemStack stack, DamageSource source, LivingEntity target, float amount) {
        if (isUnavailable()) {
            return amount;
        }
        return REFLECTION.applyOutgoingHurt(stack, source, target, amount);
    }

    public static void runPostAttack(ItemStack stack, LivingEntity attacker, LivingEntity target) {
        if (!isUnavailable()) {
            REFLECTION.runPostAttack(stack, attacker, target);
        }
    }

    private static boolean isUnavailable() {
        return !ModList.get().isLoaded(APOTHEOSIS) || !REFLECTION.ready;
    }

    private static final class Reflection {
        private final boolean ready;
        private final Method socketHelperGetGems;
        private final Method socketedGemsAddModifiers;
        private final Method socketedGemsOnHurt;
        private final Method socketedGemsDoPostAttack;
        private final Method lootCategoryForItem;
        private final Method affixHelperGetAffixes;
        private final Method affixInstanceAddModifiers;
        private final Method affixInstanceOnHurt;
        private final Method affixInstanceDoPostAttack;

        private Reflection(boolean ready,
                           Method socketHelperGetGems,
                           Method socketedGemsAddModifiers,
                           Method socketedGemsOnHurt,
                           Method socketedGemsDoPostAttack,
                           Method lootCategoryForItem,
                           Method affixHelperGetAffixes,
                           Method affixInstanceAddModifiers,
                           Method affixInstanceOnHurt,
                           Method affixInstanceDoPostAttack) {
            this.ready = ready;
            this.socketHelperGetGems = socketHelperGetGems;
            this.socketedGemsAddModifiers = socketedGemsAddModifiers;
            this.socketedGemsOnHurt = socketedGemsOnHurt;
            this.socketedGemsDoPostAttack = socketedGemsDoPostAttack;
            this.lootCategoryForItem = lootCategoryForItem;
            this.affixHelperGetAffixes = affixHelperGetAffixes;
            this.affixInstanceAddModifiers = affixInstanceAddModifiers;
            this.affixInstanceOnHurt = affixInstanceOnHurt;
            this.affixInstanceDoPostAttack = affixInstanceDoPostAttack;
        }

        private static Reflection create() {
            try {
                Class<?> socketHelper = Class.forName("dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper");
                Class<?> socketedGems = Class.forName("dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems");
                Class<?> lootCategory = Class.forName("dev.shadowsoffire.apotheosis.adventure.loot.LootCategory");
                Class<?> affixHelper = Class.forName("dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper");
                Class<?> affixInstance = Class.forName("dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance");
                return new Reflection(
                        true,
                        socketHelper.getMethod("getGems", ItemStack.class),
                        socketedGems.getMethod("addModifiers", lootCategory, EquipmentSlot.class, BiConsumer.class),
                        socketedGems.getMethod("onHurt", DamageSource.class, LivingEntity.class, float.class),
                        socketedGems.getMethod("doPostAttack", LivingEntity.class, net.minecraft.world.entity.Entity.class),
                        lootCategory.getMethod("forItem", ItemStack.class),
                        affixHelper.getMethod("getAffixes", ItemStack.class),
                        affixInstance.getMethod("addModifiers", EquipmentSlot.class, BiConsumer.class),
                        affixInstance.getMethod("onHurt", DamageSource.class, LivingEntity.class, float.class),
                        affixInstance.getMethod("doPostAttack", LivingEntity.class, net.minecraft.world.entity.Entity.class)
                );
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return new Reflection(false, null, null, null, null, null, null, null, null, null);
            }
        }

        private void addMainHandModifiers(ItemStack stack, Multimap<Attribute, AttributeModifier> modifiers) {
            invokeQuietly(() -> {
                BiConsumer<Attribute, AttributeModifier> add = modifiers::put;
                Object category = lootCategoryForItem.invoke(null, stack);
                Object gems = socketHelperGetGems.invoke(null, stack);
                socketedGemsAddModifiers.invoke(gems, category, EquipmentSlot.MAINHAND, add);
                for (Object affix : affixes(stack).values()) {
                    affixInstanceAddModifiers.invoke(affix, EquipmentSlot.MAINHAND, add);
                }
            });
        }

        private float applyOutgoingHurt(ItemStack stack, DamageSource source, LivingEntity target, float amount) {
            try {
                float modified = amount;
                Object gems = socketHelperGetGems.invoke(null, stack);
                modified = (Float) socketedGemsOnHurt.invoke(gems, source, target, modified);
                for (Object affix : affixes(stack).values()) {
                    modified = (Float) affixInstanceOnHurt.invoke(affix, source, target, modified);
                }
                return modified;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return amount;
            }
        }

        private void runPostAttack(ItemStack stack, LivingEntity attacker, LivingEntity target) {
            invokeQuietly(() -> {
                Object gems = socketHelperGetGems.invoke(null, stack);
                socketedGemsDoPostAttack.invoke(gems, attacker, target);
                for (Object affix : affixes(stack).values()) {
                    affixInstanceDoPostAttack.invoke(affix, attacker, target);
                }
            });
        }

        @SuppressWarnings("unchecked")
        private Map<?, ?> affixes(ItemStack stack) throws ReflectiveOperationException {
            return (Map<?, ?>) affixHelperGetAffixes.invoke(null, stack);
        }

        private static void invokeQuietly(ReflectiveAction action) {
            try {
                action.run();
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
    }

    private interface ReflectiveAction {
        void run() throws ReflectiveOperationException;
    }
}
