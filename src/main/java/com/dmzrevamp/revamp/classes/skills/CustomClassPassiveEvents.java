package com.dmzrevamp.revamp.classes.skills;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.fusion.FusionRevampLogic;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative implementation of JSON-defined custom class passives. */
@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CustomClassPassiveEvents {
    private static final String BONUS_NAME = "Custom Passive";
    private static final UUID CRIT_CHANCE_UUID = UUID.fromString("fb050fd4-10cc-4f34-af65-872142864fa1");
    private static final UUID CRIT_DAMAGE_UUID = UUID.fromString("a663d469-6a1e-47f6-8487-464485ac7a22");
    private static final String POTENTIAL_ACTIVE_UNTIL = "dmzrevamp_custom_potential_until";
    private static final String POTENTIAL_COOLDOWN_UNTIL = "dmzrevamp_custom_potential_cooldown";
    private static final String POTENTIAL_ORIGINAL_RELEASE = "dmzrevamp_custom_potential_release";
    private static final String POTENTIAL_SOURCE = "dmzrevamp_custom_potential_source";
    private static final String POTENTIAL_BONUS = "dmzrevamp_custom_potential_bonus";
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();
    private static boolean reflectingParry;

    private CustomClassPassiveEvents() {}

    public static double strikeCostMultiplier(StatsData data) {
        return simpleMultiplier(data, 4);
    }

    public static double kiCostMultiplier(StatsData data) {
        return simpleMultiplier(data, 5);
    }

    public static double strikeCooldownReduction(StatsData data) {
        return 1D - simpleMultiplier(data, 6);
    }

    public static double kiCooldownReduction(StatsData data) {
        return 1D - simpleMultiplier(data, 7);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyOutgoingDamage(DMZEvent.DamageModifyEvent event) {
        if (!(event.getAttacker() instanceof ServerPlayer player)) return;
        StatsData data = data(player);
        boolean physical = event.getSourceType() == DMZEvent.DamageSourceType.MELEE
                || event.getSourceType() == DMZEvent.DamageSourceType.STRIKE;
        boolean ki = event.getSourceType() == DMZEvent.DamageSourceType.KI;

        for (PassiveOwner owner : passiveOwners(data)) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() == 1) {
                if ((definition.effect() == 1 && physical) || (definition.effect() == 2 && ki)) {
                    event.setDefensePenetration(event.getDefensePenetration()
                            + CustomClassPassives.configuredPercent(definition.value()));
                }
            } else if (definition.passiveType() == 2 || definition.passiveType() == 3) {
                double strength = dynamicStrength(owner.player(), owner.data(), definition);
                if ((definition.effect() == 1 && physical) || (definition.effect() == 2 && ki)
                        || definition.effect() == 3) {
                    event.setAmount(event.getAmount() * (1D + strength));
                }
            } else if (definition.passiveType() == 4) {
                RuntimeState runtime = runtime(owner.player(), owner.data());
                if (definition.type() == 1 && physical) {
                    event.setAmount(event.getAmount() + DmzRevampHelper.getCurrentSpeedValue(owner.data()) * Math.max(0D, definition.value()));
                } else if (definition.type() == 2 && ki) {
                    event.setAmount(event.getAmount() + DmzRevampHelper.getCurrentSpeedValue(owner.data()) * Math.max(0D, definition.value()));
                } else if (definition.type() == 3 && event.getSourceType() == DMZEvent.DamageSourceType.STRIKE && runtime.nextStrikeEmpowered) {
                    event.setAmount(event.getAmount() * (1D + Math.max(0D, definition.value())));
                    runtime.nextStrikeEmpowered = false;
                } else if (definition.type() == 4 && ki && runtime.nextKiEmpowered) {
                    event.setAmount(event.getAmount() * (1D + Math.max(0D, definition.value())));
                    runtime.nextKiEmpowered = false;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterDamage(DMZEvent.DamageDealtEvent event) {
        if (!(event.getAttacker() instanceof ServerPlayer player) || event.getAmount() <= 0D
                || event.isBlocked() || event.isParried()) return;
        StatsData data = data(player);
        for (PassiveOwner owner : passiveOwners(data)) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() == 2) {
                boolean physical = event.getSourceType() == DMZEvent.DamageSourceType.MELEE
                        || event.getSourceType() == DMZEvent.DamageSourceType.STRIKE;
                if ((definition.type() == 1 && physical)
                        || (definition.type() == 2 && event.getSourceType() == DMZEvent.DamageSourceType.KI)) {
                    addStack(owner.player(), owner.data(), definition);
                }
            } else if (definition.passiveType() == 4) {
                RuntimeState runtime = runtime(owner.player(), owner.data());
                if (definition.type() == 3 && event.getSourceType() == DMZEvent.DamageSourceType.STRIKE) {
                    runtime.nextStrikeEmpowered = true;
                } else if (definition.type() == 4 && event.getSourceType() == DMZEvent.DamageSourceType.KI) {
                    runtime.nextKiEmpowered = true;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlock(DMZEvent.PlayerBlockEvent event) {
        StatsData data = data(event.getVictim());
        for (PassiveOwner owner : passiveOwners(data)) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() == 2
                    && ((definition.type() == 3 && !event.isParry()) || (definition.type() == 4 && event.isParry()))) {
                addStack(owner.player(), owner.data(), definition);
            }
            if (!reflectingParry && event.isParry() && definition.passiveType() == 4 && definition.type() == 5
                    && event.getAttacker() != null && event.getAttacker().isAlive()) {
                float reflected = (float) Math.max(0D, event.getOriginalDamage() * definition.value());
                if (reflected > 0F) {
                    reflectingParry = true;
                    try {
                        event.getAttacker().hurt(event.getAttacker().damageSources().playerAttack(event.getVictim()), reflected);
                    } finally {
                        reflectingParry = false;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEvasion(DMZEvent.PlayerEvasionEvent event) {
        StatsData data = data(event.getPlayer());
        for (PassiveOwner owner : passiveOwners(data)) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() == 2 && definition.type() == 5) {
                addStack(owner.player(), owner.data(), definition);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void healthRegen(DMZEvent.HealthRegenEvent event) {
        event.setAmount(event.getAmount() * regenMultiplier(event.getPlayer(), event.getStatsData(), 8, 6));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void energyRegen(DMZEvent.EnergyRegenEvent event) {
        event.setAmount(event.getAmount() * regenMultiplier(event.getPlayer(), event.getStatsData(), 9, 7));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void staminaRegen(DMZEvent.StaminaRegenEvent event) {
        event.setAmount(event.getAmount() * regenMultiplier(event.getPlayer(), event.getStatsData(), 10, 8));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void reduceIncomingDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0F) return;
        StatsData data = data(player);
        double multiplier = 1D;
        for (PassiveOwner owner : passiveOwners(data)) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() == 1 && definition.effect() == 3) {
                multiplier *= 1D - Mth.clamp(CustomClassPassives.configuredPercent(definition.value()), 0D, 1D);
            }
        }
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) return;
        StatsData data = data(player);
        List<PassiveOwner> owners = passiveOwners(data);
        if (owners.isEmpty() || data == null || !data.getStatus().isHasCreatedCharacter()) {
            clear(player, data);
            return;
        }
        RuntimeState runtime = runtime(player, data);
        boolean fusedNow = data.getStatus().isFused();
        if (runtime.wasFused && !fusedNow) {
            runtime.stackExpirations.clear();
            runtime.nextStrikeEmpowered = false;
            runtime.nextKiEmpowered = false;
            runtime.needsSync = true;
            if (player.getPersistentData().contains(POTENTIAL_ORIGINAL_RELEASE)) {
                boolean borrowed = isBorrowedPotential(player);
                restorePotentialRelease(player, data);
                if (borrowed) clearPotentialCooldown(player);
            }
        }
        runtime.wasFused = fusedNow;
        for (PassiveOwner owner : owners) {
            prune(runtime(owner.player(), owner.data()), player.level().getGameTime());
        }
        boolean changed = updateStatBonuses(player, data, owners);
        changed |= updatePotentialRelease(player, data, owners, fusionMembers(data));
        if (changed || (player.tickCount % 10 == 0 && runtime.needsSync)) {
            runtime.needsSync = false;
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player, data(player));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player, data(player));
    }

    private static double regenMultiplier(net.minecraft.world.entity.player.Player player, StatsData data,
                                          int simpleEffect, int dynamicEffect) {
        double multiplier = 1D;
        for (PassiveOwner owner : passiveOwners(data)) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() == 1 && definition.effect() == simpleEffect) {
                multiplier *= 1D + CustomClassPassives.configuredPercent(definition.value());
            } else if ((definition.passiveType() == 2 || definition.passiveType() == 3)
                    && definition.effect() == dynamicEffect) {
                multiplier *= 1D + dynamicStrength(owner.player(), owner.data(), definition);
            }
        }
        return multiplier;
    }

    private static double simpleMultiplier(StatsData data, int effect) {
        double multiplier = 1D;
        CustomClassPassives.Definition own = CustomClassPassives.definition(data);
        if (own != null && own.passiveType() == 1 && own.effect() == effect) {
            multiplier *= 1D - Mth.clamp(CustomClassPassives.configuredPercent(own.value()), 0D, 1D);
        }
        if (data != null && data.getStatus().isFused() && data.getPlayer() instanceof ServerPlayer) {
            StatsData partnerData = FusionRevampLogic.getFusionPartnerData(data);
            CustomClassPassives.Definition partner = CustomClassPassives.definition(partnerData);
            if (partner != null && partner.passiveType() == 1 && partner.effect() == effect) {
                multiplier *= 1D - Mth.clamp(CustomClassPassives.configuredPercent(partner.value()), 0D, 1D);
            }
        }
        return Mth.clamp(multiplier, 0D, 1D);
    }

    private static double dynamicStrength(ServerPlayer player, StatsData data, CustomClassPassives.Definition definition) {
        double maximum = CustomClassPassives.configuredPercent(definition.passiveType() == 2
                ? definition.maxValue() : definition.value());
        if (definition.passiveType() == 2) {
            RuntimeState runtime = runtime(player, data);
            prune(runtime, player.level().getGameTime());
            return definition.maxStacks() <= 0 ? 0D : maximum * runtime.stackExpirations.size() / definition.maxStacks();
        }
        if (definition.passiveType() != 3) return 0D;
        double ratio = switch (definition.type()) {
            case 1 -> player.getMaxHealth() <= 0F ? 0D : player.getHealth() / player.getMaxHealth();
            case 2 -> data.getMaxEnergy() <= 0F ? 0D : data.getResources().getCurrentEnergy() / data.getMaxEnergy();
            case 3 -> data.getMaxStamina() <= 0F ? 0D : data.getResources().getCurrentStamina() / data.getMaxStamina();
            default -> 0D;
        };
        double factor = definition.resourceType() == 1 ? (1D - ratio) / 0.9D : ratio / 0.9D;
        return maximum * Mth.clamp(factor, 0D, 1D);
    }

    private static void addStack(ServerPlayer player, StatsData data, CustomClassPassives.Definition definition) {
        if (definition.maxStacks() <= 0) return;
        RuntimeState runtime = runtime(player, data);
        prune(runtime, player.level().getGameTime());
        if (runtime.stackExpirations.size() >= definition.maxStacks()) runtime.stackExpirations.removeFirst();
        runtime.stackExpirations.addLast(player.level().getGameTime() + definition.stackTime());
        runtime.needsSync = true;
    }

    private static boolean updateStatBonuses(ServerPlayer player, StatsData data, List<PassiveOwner> owners) {
        double defense = 0D;
        double speed = 0D;
        double critDamage = 0D;
        double critChance = 0D;
        for (PassiveOwner owner : owners) {
            CustomClassPassives.Definition definition = owner.definition();
            if (definition.passiveType() != 2 && definition.passiveType() != 3) continue;
            double strength = dynamicStrength(owner.player(), owner.data(), definition);
            // DEF/SKP are DMZ bonuses and FusionRevampLogic already mirrors them.
            // Only write each owner's own source here or the mirrored entry is counted twice.
            if (owner.data() == data && definition.effect() == 4) defense += strength;
            if (owner.data() == data && definition.effect() == 5) speed += strength;
            if (definition.effect() == 9 || definition.effect() == 11) critDamage += strength;
            if (definition.effect() == 10 || definition.effect() == 11) critChance += strength;
        }
        boolean changed = setBonus(data, "DEF", defense);
        changed |= setBonus(data, "SKP", speed);
        changed |= updateAttribute(player, MainAttributes.CRIT_DAMAGE.get(), CRIT_DAMAGE_UUID,
                BONUS_NAME + " critical damage", critDamage);
        changed |= updateAttribute(player, MainAttributes.CRIT_CHANCE.get(), CRIT_CHANCE_UUID,
                BONUS_NAME + " critical chance", critChance);
        return changed;
    }

    private static boolean setBonus(StatsData data, String stat, double amount) {
        var bonuses = data.getBonusStats().getBonuses(stat);
        double existing = bonuses.stream().filter(bonus -> BONUS_NAME.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value).findFirst().orElse(0D);
        double desired = amount > 0D ? 1D + amount : 0D;
        if (Math.abs(existing - desired) < 0.0001D) return false;
        data.getBonusStats().removeBonus(stat, BONUS_NAME);
        if (desired > 0D) data.getBonusStats().addBonus(stat, BONUS_NAME, "*", desired);
        return true;
    }

    private static boolean updateAttribute(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attributeType,
                                           UUID uuid, String name, double amount) {
        AttributeInstance attribute = player.getAttribute(attributeType);
        if (attribute == null) return false;
        AttributeModifier existing = attribute.getModifier(uuid);
        if (existing == null && amount <= 0D) return false;
        if (existing != null && Math.abs(existing.getAmount() - amount) < 0.0001D) return false;
        if (existing != null) attribute.removeModifier(existing);
        if (amount > 0D) attribute.addTransientModifier(new AttributeModifier(
                uuid, name, amount, AttributeModifier.Operation.ADDITION));
        return true;
    }

    private static boolean updatePotentialRelease(ServerPlayer player, StatsData data, List<PassiveOwner> owners,
                                                  List<FusionMember> members) {
        RuntimeState runtime = runtime(player, data);
        long now = player.level().getGameTime();
        PassiveOwner source = owners.stream()
                .filter(owner -> owner.definition().passiveType() == 4 && owner.definition().type() == 6)
                .max(java.util.Comparator.comparingDouble(owner -> owner.definition().value()))
                .orElse(null);
        boolean special = source != null;
        long activeUntil = player.getPersistentData().getLong(POTENTIAL_ACTIVE_UNTIL);
        if (!data.getStatus().isFused() && isBorrowedPotential(player)) {
            boolean changed = player.getPersistentData().contains(POTENTIAL_ORIGINAL_RELEASE);
            if (changed) restorePotentialRelease(player, data);
            clearPotentialCooldown(player);
            return changed;
        }
        if (player.getPersistentData().hasUUID(POTENTIAL_SOURCE)
                && now >= player.getPersistentData().getLong(POTENTIAL_COOLDOWN_UNTIL)
                && !player.getPersistentData().contains(POTENTIAL_ORIGINAL_RELEASE)) {
            player.getPersistentData().remove(POTENTIAL_SOURCE);
        }
        if (!special) {
            if (player.getPersistentData().contains(POTENTIAL_ORIGINAL_RELEASE)) {
                restorePotentialRelease(player, data);
                clearPotentialCooldown(player);
                return true;
            }
            clearPotentialCooldown(player);
            return false;
        }
        if (activeUntil <= now && player.getPersistentData().contains(POTENTIAL_ORIGINAL_RELEASE)) {
            restorePotentialRelease(player, data);
            return true;
        }
        if (activeUntil <= now && members.stream().anyMatch(CustomClassPassiveEvents::isAtPotentialThreshold)
                && members.stream().allMatch(member -> now >= member.player().getPersistentData()
                .getLong(POTENTIAL_COOLDOWN_UNTIL))) {
            long until = now + 600L;
            long cooldown = until + 1800L;
            double bonus = Math.max(0D, source.definition().value());
            for (FusionMember member : members) {
                member.player().getPersistentData().putInt(POTENTIAL_ORIGINAL_RELEASE,
                        member.data().getResources().getPowerRelease());
                member.player().getPersistentData().putLong(POTENTIAL_ACTIVE_UNTIL, until);
                member.player().getPersistentData().putLong(POTENTIAL_COOLDOWN_UNTIL, cooldown);
                member.player().getPersistentData().putUUID(POTENTIAL_SOURCE, source.player().getUUID());
                member.player().getPersistentData().putDouble(POTENTIAL_BONUS, bonus);
                runtime(member.player(), member.data()).needsSync = true;
            }
            activeUntil = now + 600L;
        }
        if (activeUntil > now) {
            double bonus = Math.max(0D, player.getPersistentData().getDouble(POTENTIAL_BONUS));
            int target = Math.max(0, (int) Math.round(ClassSkillHelper.potentialMaxRelease(data) * (1D + bonus)));
            if (data.getResources().getPowerRelease() != target) {
                data.getResources().setPowerRelease(target);
                runtime.needsSync = true;
                return true;
            }
        }
        return false;
    }

    private static void restorePotentialRelease(ServerPlayer player, StatsData data) {
        int restingRelease = Math.max(0, (int) Math.round(ClassSkillHelper.potentialMaxRelease(data) / 2D));
        data.getResources().setPowerRelease(restingRelease);
        player.getPersistentData().remove(POTENTIAL_ORIGINAL_RELEASE);
        player.getPersistentData().remove(POTENTIAL_ACTIVE_UNTIL);
        player.getPersistentData().remove(POTENTIAL_BONUS);
    }

    private static void clearPotentialCooldown(ServerPlayer player) {
        player.getPersistentData().remove(POTENTIAL_COOLDOWN_UNTIL);
        player.getPersistentData().remove(POTENTIAL_SOURCE);
        player.getPersistentData().remove(POTENTIAL_BONUS);
    }

    private static boolean isAtPotentialThreshold(FusionMember member) {
        return member.player().getHealth() <= member.player().getMaxHealth() * 0.5F;
    }

    private static boolean isBorrowedPotential(ServerPlayer player) {
        return player.getPersistentData().hasUUID(POTENTIAL_SOURCE)
                && !player.getUUID().equals(player.getPersistentData().getUUID(POTENTIAL_SOURCE));
    }

    private static List<PassiveOwner> passiveOwners(StatsData data) {
        if (data == null || !(data.getPlayer() instanceof ServerPlayer player)) return List.of();
        ArrayList<PassiveOwner> owners = new ArrayList<>(2);
        CustomClassPassives.Definition own = CustomClassPassives.definition(data);
        if (own != null) owners.add(new PassiveOwner(player, data, own));

        if (data.getStatus().isFused()) {
            StatsData partnerData = FusionRevampLogic.getFusionPartnerData(data);
            if (partnerData != null && partnerData != data && partnerData.getPlayer() instanceof ServerPlayer partner) {
                CustomClassPassives.Definition partnerDefinition = CustomClassPassives.definition(partnerData);
                if (partnerDefinition != null) owners.add(new PassiveOwner(partner, partnerData, partnerDefinition));
            }
        }
        return owners;
    }

    /** Both members receive every custom passive while fused, even if one has no custom passive of their own. */
    private static List<FusionMember> fusionMembers(StatsData data) {
        if (data == null || !(data.getPlayer() instanceof ServerPlayer player)) return List.of();
        ArrayList<FusionMember> members = new ArrayList<>(2);
        members.add(new FusionMember(player, data));
        if (data.getStatus().isFused()) {
            StatsData partnerData = FusionRevampLogic.getFusionPartnerData(data);
            if (partnerData != null && partnerData != data && partnerData.getPlayer() instanceof ServerPlayer partner) {
                members.add(new FusionMember(partner, partnerData));
            }
        }
        return members;
    }

    private static RuntimeState runtime(ServerPlayer player, StatsData data) {
        String classId = data.getCharacter().getCharacterClass();
        RuntimeState state = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(classId));
        if (!state.classId.equalsIgnoreCase(classId)) {
            clear(player, data);
            state = new RuntimeState(classId);
            RUNTIME.put(player.getUUID(), state);
        }
        return state;
    }

    private static void prune(RuntimeState state, long now) {
        while (!state.stackExpirations.isEmpty() && state.stackExpirations.peekFirst() <= now) {
            state.stackExpirations.removeFirst();
            state.needsSync = true;
        }
    }

    private static void clear(ServerPlayer player, StatsData data) {
        RUNTIME.remove(player.getUUID());
        updateAttribute(player, MainAttributes.CRIT_DAMAGE.get(), CRIT_DAMAGE_UUID, BONUS_NAME + " critical damage", 0D);
        updateAttribute(player, MainAttributes.CRIT_CHANCE.get(), CRIT_CHANCE_UUID, BONUS_NAME + " critical chance", 0D);
        if (data != null) {
            data.getBonusStats().removeBonus("DEF", BONUS_NAME);
            data.getBonusStats().removeBonus("SKP", BONUS_NAME);
            if (player.getPersistentData().contains(POTENTIAL_ORIGINAL_RELEASE)) {
                boolean borrowed = isBorrowedPotential(player);
                restorePotentialRelease(player, data);
                if (borrowed) clearPotentialCooldown(player);
            }
        }
    }

    private static StatsData data(ServerPlayer player) {
        return player == null ? null : StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
    }

    private record PassiveOwner(ServerPlayer player, StatsData data, CustomClassPassives.Definition definition) {}
    private record FusionMember(ServerPlayer player, StatsData data) {}

    private static final class RuntimeState {
        final String classId;
        final Deque<Long> stackExpirations = new ArrayDeque<>();
        boolean nextStrikeEmpowered;
        boolean nextKiEmpowered;
        boolean needsSync;
        boolean wasFused;
        RuntimeState(String classId) { this.classId = classId == null ? "" : classId; }
    }
}
