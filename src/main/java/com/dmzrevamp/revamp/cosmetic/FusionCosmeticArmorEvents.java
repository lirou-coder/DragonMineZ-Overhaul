package com.dmzrevamp.revamp.cosmetic;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.InternalCosmeticArmorSyncS2CPacket;
import com.dragonminez.common.events.DMZEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID)
public final class FusionCosmeticArmorEvents {
    private static final String COSMETIC_ARMOR_MODID = "cosmeticarmorreworked";

    private FusionCosmeticArmorEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFusion(DMZEvent.FusionEvent event) {
        ServerPlayer controllingPlayer = event.getInitiator();
        LivingEntity target = event.getTarget();

        if (event.getType() == DMZEvent.FusionEvent.FusionType.METAMORU) {
            InternalCosmeticArmorRows.setRow(
                    controllingPlayer,
                    InternalCosmeticArmorRows.ROW_FUSION,
                    item("gogeta_armor_boots"),
                    item("gogeta_armor_leggings"),
                    item("gogeta_armor_chestplate"),
                    ItemStack.EMPTY,
                    true
            );
            return;
        }

        if (!(target instanceof ServerPlayer otherPlayer)) {
            return;
        }

        if (event.getType() == DMZEvent.FusionEvent.FusionType.POTHALA) {
            if (applySpecialPotaraSet(controllingPlayer, otherPlayer)) {
                return;
            }

            ItemStack helmet = effectiveArmor(otherPlayer, EquipmentSlot.HEAD);
            ItemStack leggings = effectiveArmor(otherPlayer, EquipmentSlot.LEGS);
            ItemStack chestplate = effectiveArmor(controllingPlayer, EquipmentSlot.CHEST);
            ItemStack boots = effectiveArmor(controllingPlayer, EquipmentSlot.FEET);
            InternalCosmeticArmorRows.setRow(
                    controllingPlayer,
                    InternalCosmeticArmorRows.ROW_FUSION,
                    boots,
                    leggings,
                    chestplate,
                    helmet,
                    false
            );
        }
    }

    private static boolean applySpecialPotaraSet(ServerPlayer controllingPlayer, ServerPlayer otherPlayer) {
        ArmorSet controllingSet = ArmorSet.of(controllingPlayer);
        ArmorSet otherSet = ArmorSet.of(otherPlayer);

        if (matchesPair(controllingSet, otherSet, FusionCosmeticArmorEvents::isGokuBlackArmor, FusionCosmeticArmorEvents::isZamasuArmor)) {
            setFullFusionSet(controllingPlayer, "fusion_zamasu");
            return true;
        }
        if (matchesPair(controllingSet, otherSet, FusionCosmeticArmorEvents::isGokuArmor, FusionCosmeticArmorEvents::isVegetaArmor)) {
            setFullFusionSet(controllingPlayer, "vegetto");
            return true;
        }
        if (matchesPair(controllingSet, otherSet, FusionCosmeticArmorEvents::isCauliflaArmor, FusionCosmeticArmorEvents::isKaleArmor)) {
            setFullFusionSet(controllingPlayer, "kefla");
            return true;
        }

        return false;
    }

    private static boolean matchesPair(ArmorSet first, ArmorSet second, ArmorPredicate left, ArmorPredicate right) {
        return (left.matches(first) && right.matches(second)) || (right.matches(first) && left.matches(second));
    }

    private static void setFullFusionSet(ServerPlayer player, String armorPrefix) {
        InternalCosmeticArmorRows.setRow(
                player,
                InternalCosmeticArmorRows.ROW_FUSION,
                item(armorPrefix + "_armor_boots"),
                item(armorPrefix + "_armor_leggings"),
                item(armorPrefix + "_armor_chestplate"),
                ItemStack.EMPTY,
                true
        );
    }

    private static boolean isGokuBlackArmor(ArmorSet set) {
        return set.matchesExactPrefix("blackgoku");
    }

    private static boolean isZamasuArmor(ArmorSet set) {
        return set.matchesExactPrefix("zamasu");
    }

    private static boolean isGokuArmor(ArmorSet set) {
        return set.matchesNamedArmor("goku") && !set.matchesExactPrefix("blackgoku");
    }

    private static boolean isVegetaArmor(ArmorSet set) {
        return set.matchesNamedArmor("vegeta");
    }

    private static boolean isCauliflaArmor(ArmorSet set) {
        return set.matchesExactPrefix("caulifla");
    }

    private static boolean isKaleArmor(ArmorSet set) {
        return set.matchesExactPrefix("kale");
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
            DmzRevampNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> watcher), new InternalCosmeticArmorSyncS2CPacket(target.getUUID(), InternalCosmeticArmorRows.copyServerRows(target)));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InternalCosmeticArmorRows.clearAll(player);
        }
    }

    public static void clearFusionRow(ServerPlayer player) {
        InternalCosmeticArmorRows.clearRow(player, InternalCosmeticArmorRows.ROW_FUSION);
    }

    private static ItemStack effectiveArmor(ServerPlayer player, EquipmentSlot slot) {
        if (ModList.get().isLoaded(COSMETIC_ARMOR_MODID)) {
            ItemStack cosmetic = cosmeticArmorStack(player.getUUID(), slot);
            if (cosmetic != null) {
                return cosmetic;
            }
        }
        return vanillaArmor(player, slot).copy();
    }

    private static ItemStack cosmeticArmorStack(UUID playerId, EquipmentSlot slot) {
        try {
            Class<?> apiClass = Class.forName("lain.mods.cos.api.CosArmorAPI");
            Object stacks = apiClass.getMethod("getCAStacks", UUID.class).invoke(null, playerId);
            if (stacks == null) {
                return null;
            }
            int slotIndex = InternalCosmeticArmorRows.slotIndex(slot);
            Method isSkinArmor = stacks.getClass().getMethod("isSkinArmor", int.class);
            if ((Boolean) isSkinArmor.invoke(stacks, slotIndex)) {
                return ItemStack.EMPTY;
            }
            Method getStackInSlot = stacks.getClass().getMethod("getStackInSlot", int.class);
            ItemStack stack = (ItemStack) getStackInSlot.invoke(stacks, slotIndex);
            return stack.isEmpty() ? null : stack.copy();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static ItemStack vanillaArmor(Player player, EquipmentSlot slot) {
        int index = InternalCosmeticArmorRows.slotIndex(slot);
        return index >= 0 ? player.getInventory().armor.get(index) : ItemStack.EMPTY;
    }

    private static ItemStack item(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("dragonminez", id));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    private interface ArmorPredicate {
        boolean matches(ArmorSet set);
    }

    private record ArmorSet(String chestplate, String leggings, String boots) {
        private static ArmorSet of(ServerPlayer player) {
            return new ArmorSet(
                    itemPath(effectiveArmor(player, EquipmentSlot.CHEST)),
                    itemPath(effectiveArmor(player, EquipmentSlot.LEGS)),
                    itemPath(effectiveArmor(player, EquipmentSlot.FEET))
            );
        }

        private boolean matchesExactPrefix(String prefix) {
            return chestplate.equals(prefix + "_armor_chestplate")
                    && leggings.equals(prefix + "_armor_leggings")
                    && boots.equals(prefix + "_armor_boots");
        }

        private boolean matchesNamedArmor(String name) {
            return isNamedArmorPiece(chestplate, name, "_armor_chestplate")
                    && isNamedArmorPiece(leggings, name, "_armor_leggings")
                    && isNamedArmorPiece(boots, name, "_armor_boots");
        }

        private static boolean isNamedArmorPiece(String path, String name, String suffix) {
            return path.endsWith(suffix) && path.contains(name);
        }

        private static String itemPath(ItemStack stack) {
            if (stack.isEmpty()) {
                return "";
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key == null || !"dragonminez".equals(key.getNamespace())) {
                return "";
            }
            return key.getPath();
        }
    }
}
