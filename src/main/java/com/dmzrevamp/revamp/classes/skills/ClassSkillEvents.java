package com.dmzrevamp.revamp.classes.skills;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.revamp.DmzRevampHelper;
import com.dmzrevamp.revamp.ki.RevampKiAttackData;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClassSkillEvents {
    private static final Map<UUID, AbstractKiProjectile> BLOCK_PROJECTILE_CACHE = new ConcurrentHashMap<>();
    private static final UUID BERSERKER_CRIT_CHANCE_UUID = UUID.fromString("31003f40-4e52-4d19-9614-2dd8f520e263");
    private static final UUID BERSERKER_CRIT_DAMAGE_UUID = UUID.fromString("e17c9b23-fd47-4d9d-8c7e-112a1ed781f4");
    private static final String WARRIOR_STACK_TAG = "dmzrevamp_warrior_fury_stacks";
    private static final String WARRIOR_STACK_EXPIRES_TAG = "dmzrevamp_warrior_fury_expires";
    private static final String SPEEDSTER_STACK_TAG = "dmzrevamp_speedster_momentum_stacks";
    private static final String SPEEDSTER_STACK_EXPIRES_TAG = "dmzrevamp_speedster_momentum_expires";
    private static final String SPEEDSTER_SPEED_BONUS = "Momentum Speed";
    private static final String SPEEDSTER_MELEE_BONUS = "Momentum Melee";

    // Forge calls the static event methods directly, so this event holder should not be instantiated.
    private ClassSkillEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Watches outgoing damage so class passives can react to hits after basic validity checks.
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0F) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer defender) {
            StatsData defenderData = getData(defender);
            if (defenderData != null) {
                // Normal class passives do not modify incoming damage here.
            }
        }

        ServerPlayer attacker = getAttackingPlayer(event.getSource());
        if (attacker == null) {
            return;
        }

        StatsData attackerData = getData(attacker);
        if (attackerData == null) {
            return;
        }

        applyOutgoingBonuses(event, attacker, attackerData);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rememberIncomingProjectile(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getDirectEntity() instanceof AbstractKiProjectile projectile) {
            BLOCK_PROJECTILE_CACHE.put(player.getUUID(), projectile);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearIncomingProjectile(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BLOCK_PROJECTILE_CACHE.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Applies addon class passive damage through DMZ's passive damage event.
    public static void onDamageModify(DMZEvent.DamageModifyEvent event) {
        if (!(event.getAttacker() instanceof ServerPlayer attacker)) {
            return;
        }
        StatsData data = getData(attacker);
        if (data == null) {
            return;
        }

        boolean meleeOrStrike = event.getSourceType() == DMZEvent.DamageSourceType.MELEE
                || event.getSourceType() == DMZEvent.DamageSourceType.STRIKE;

        if ((meleeOrStrike || event.getSourceType() == DMZEvent.DamageSourceType.KI)
                && ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.MARTIAL_ARTIST)) {
            event.setAmount(event.getAmount() * (1D + ClassSkillHelper.martialArtistMissingHpBonus(data, event.getVictim())));
        }

        if (meleeOrStrike && event.getAmount() > 0D) {
            grantWarriorFury(attacker, data);
            grantSpeedsterMomentum(attacker, data);
            event.setAmount(event.getAmount() + getSpeedsterMomentumMeleeFlatBonus(attacker, data));
            if (isGuardBroken(event.getVictim()) && ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.DUELIST)) {
                event.setAmount(event.getAmount() * (1D + ClassSkillHelper.duelistGuardBrokenDamageBonus(data)));
            }
            double furyPenetration = getActiveWarriorStacks(attacker) * ClassSkillHelper.warriorDefensePenetrationPerStack(data);
            if (furyPenetration > 0D) {
                event.setDefensePenetration(event.getDefensePenetration() + furyPenetration);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onStaminaRegen(DMZEvent.StaminaRegenEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        StatsData data = event.getStatsData();
        if (data == null || !ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.WARRIOR)) {
            return;
        }
        int stacks = getActiveWarriorStacks(player);
        if (stacks > 0) {
            event.setAmount(event.getAmount() * (1D + stacks * ClassSkillHelper.warriorStaminaRegenPerStack(data)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCritChance(DMZEvent.CritChanceEvent event) {
        // Berserker crit chance is applied through MainAttributes.CRIT_CHANCE so the stat screen and combat share one value.
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKiAttackFire(DMZEvent.KiAttackFireEvent event) {
        event.setCooldownTicks(adjustKiAttackCooldown(event.getStatsData(), event.getKiAttack(), event.getCooldownTicks()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Improves Duelist parries, either increasing melee poise damage or redirecting the blocked Ki projectile.
    public static void onPlayerBlock(DMZEvent.PlayerBlockEvent event) {
        if (!event.isParry() || event.getAttacker() == null || !event.getAttacker().isAlive()) {
            return;
        }

        StatsData data = getData(event.getVictim());
        if (data == null) {
            return;
        }

        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.DUELIST)) {
            return;
        }

        if (isLikelyMeleeParry(event, event.getVictim(), event.getAttacker())) {
            event.setPoiseDamage((float) (event.getPoiseDamage() * (1D + ClassSkillHelper.duelistParryPoiseDamageBonus(data))));
        } else {
            AbstractKiProjectile projectile = BLOCK_PROJECTILE_CACHE.remove(event.getVictim().getUUID());
            if (projectile != null) {
                redirectParriedKiBlast(projectile, event.getVictim(), data);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Refreshes time-limited class bonuses and syncs stat changes back to clients.
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StatsData data = getData(player);
        if (data == null || !data.getStatus().isHasCreatedCharacter()) {
            clearRuntime(player, data);
            return;
        }

        boolean changed = false;
        changed |= processClassStackBonuses(player, data);
        syncBerserkerCritBonuses(player, data);
        if (changed) {
            sync(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Handles the onLivingDeath event.
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearRuntime(player, getData(player));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    // Handles the onPlayerLoggedOut event.
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearRuntime(player, null);
        }
    }

    // Handles the adjustKiBlastCost logic for this class.
    public static int adjustKiBlastCost(ServerPlayer player, StatsData data, int originalCost) {
        return Math.max(0, originalCost);
    }

    // Handles the adjustNonDodgeKiCost logic for this class.
    public static int adjustNonDodgeKiCost(ServerPlayer player, StatsData data, int originalCost) {
        return Math.max(0, originalCost);
    }

    // Handles the adjustKiActionCost logic for this class.
    public static double adjustKiActionCost(ServerPlayer player, StatsData data, double originalCost) {
        return Math.max(0D, originalCost * CustomClassPassiveEvents.kiCostMultiplier(data));
    }

    public static double adjustKiActionCost(ServerPlayer player, StatsData data, KiAttackData technique, double originalCost) {
        double reduction = getKiUtilityCostReduction(data, technique);
        return Math.max(0D, originalCost * (1D - reduction) * CustomClassPassiveEvents.kiCostMultiplier(data));
    }

    public static int adjustKiAttackCooldown(StatsData data, KiAttackData technique, int cooldownTicks) {
        double reduction = getKiUtilityCooldownReduction(data, technique);
        return Math.max(1, (int) Math.round(cooldownTicks * (1D - reduction)
                * (1D - CustomClassPassiveEvents.kiCooldownReduction(data))));
    }

    public static double kiAssassinCastStepMultiplier(StatsData data, KiAttackData technique) {
        double reduction = ClassSkillHelper.kiAssassinCastReduction(data, hasAnyKiEffect(technique));
        return reduction <= 0D ? 1D : 1D / Math.max(0.0001D, 1D - Math.min(0.99D, reduction));
    }

    public static double kiAssassinProjectileSpeedMultiplier(StatsData data, KiAttackData technique) {
        double increase = ClassSkillHelper.kiAssassinSpeedIncrease(data, hasAnyKiEffect(technique));
        return 1D + Math.max(0D, increase);
    }

    // Handles the onKiActionSpent event.
    public static void onKiActionSpent(ServerPlayer player, StatsData data, double spentCost) {
    }

    // Handles the onKiBlastDamaged event.
    public static void onKiBlastDamaged(ServerPlayer player, StatsData data) {
    }

    // Clears temporary class state for commands or reset wishes.
    public static boolean clearClassCooldowns(ServerPlayer player) {
        StatsData data = getData(player);
        boolean removedAny = false;

        BLOCK_PROJECTILE_CACHE.remove(player.getUUID());
        player.getPersistentData().putInt(WARRIOR_STACK_TAG, 0);
        player.getPersistentData().putInt(SPEEDSTER_STACK_TAG, 0);
        removeBerserkerCritModifiers(player);
        if (removedAny) {
            sync(player);
        }
        return removedAny;
    }

    // Applies the applyOutgoingBonuses behavior.
    private static void applyOutgoingBonuses(LivingHurtEvent event, ServerPlayer attacker, StatsData data) {
        DamageSource source = event.getSource();
        boolean ki = isKiDamage(source, attacker);

        if (ki && source.getDirectEntity() instanceof KiBlastEntity) {
            onKiBlastDamaged(attacker, data);
        }
    }

    private static void grantWarriorFury(ServerPlayer player, StatsData data) {
        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.WARRIOR)) {
            return;
        }
        int maxStacks = ClassSkillHelper.maxStacks(data, ClassSkillHelper.WARRIOR, 10);
        if (maxStacks <= 0) {
            return;
        }
        player.getPersistentData().putInt(WARRIOR_STACK_TAG, Math.min(maxStacks, getActiveWarriorStacks(player) + 1));
        player.getPersistentData().putLong(WARRIOR_STACK_EXPIRES_TAG, player.level().getGameTime() + ClassSkillHelper.stackDurationTicks(data, ClassSkillHelper.WARRIOR, 100L));
    }

    private static int getActiveWarriorStacks(ServerPlayer player) {
        if (player.getPersistentData().getLong(WARRIOR_STACK_EXPIRES_TAG) < player.level().getGameTime()) {
            player.getPersistentData().putInt(WARRIOR_STACK_TAG, 0);
            return 0;
        }
        return Math.max(0, player.getPersistentData().getInt(WARRIOR_STACK_TAG));
    }

    private static void grantSpeedsterMomentum(ServerPlayer player, StatsData data) {
        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.SPEEDSTER)) {
            return;
        }
        int maxStacks = ClassSkillHelper.maxStacks(data, ClassSkillHelper.SPEEDSTER, 10);
        if (maxStacks <= 0) {
            return;
        }
        player.getPersistentData().putInt(SPEEDSTER_STACK_TAG, Math.min(maxStacks, getActiveSpeedsterStacks(player) + 1));
        player.getPersistentData().putLong(SPEEDSTER_STACK_EXPIRES_TAG, player.level().getGameTime() + ClassSkillHelper.stackDurationTicks(data, ClassSkillHelper.SPEEDSTER, 200L));
    }

    private static int getActiveSpeedsterStacks(ServerPlayer player) {
        if (player.getPersistentData().getLong(SPEEDSTER_STACK_EXPIRES_TAG) < player.level().getGameTime()) {
            player.getPersistentData().putInt(SPEEDSTER_STACK_TAG, 0);
            return 0;
        }
        return Math.max(0, player.getPersistentData().getInt(SPEEDSTER_STACK_TAG));
    }

    private static boolean processClassStackBonuses(ServerPlayer player, StatsData data) {
        boolean changed = false;
        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.WARRIOR)) {
            if (player.getPersistentData().getInt(WARRIOR_STACK_TAG) != 0) {
                player.getPersistentData().putInt(WARRIOR_STACK_TAG, 0);
            }
        } else {
            getActiveWarriorStacks(player);
        }

        int speedsterStacks = ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.SPEEDSTER) ? getActiveSpeedsterStacks(player) : 0;
        if (speedsterStacks <= 0) {
            changed |= removeBonusIfPresent(data, "SKP", SPEEDSTER_SPEED_BONUS);
            changed |= removeBonusIfPresent(data, "STR", SPEEDSTER_MELEE_BONUS);
            return changed;
        }

        double speedMultiplier = 1D + speedsterStacks * ClassSkillHelper.speedsterSpeedBonusPerStack(data);
        changed |= setBonusMultiplier(data, "SKP", SPEEDSTER_SPEED_BONUS, speedMultiplier);
        changed |= removeBonusIfPresent(data, "STR", SPEEDSTER_MELEE_BONUS);
        return changed;
    }

    private static double getSpeedsterMomentumMeleeFlatBonus(ServerPlayer player, StatsData data) {
        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.SPEEDSTER)) {
            return 0D;
        }
        int stacks = getActiveSpeedsterStacks(player);
        if (stacks <= 0) {
            return 0D;
        }
        double speedValue = DmzRevampHelper.getCurrentSpeedValue(data);
        return Math.max(0D, speedValue * stacks * ClassSkillHelper.speedsterMeleeDamageSpeedSharePerStack(data));
    }

    private static boolean setBonusMultiplier(StatsData data, String stat, String name, double multiplier) {
        double current = data.getBonusStats().getBonuses(stat).stream()
                .filter(bonus -> name.equals(bonus.name))
                .mapToDouble(bonus -> bonus.value)
                .findFirst()
                .orElse(0D);
        if (Math.abs(current - multiplier) <= 0.0001D) {
            return false;
        }
        data.getBonusStats().removeBonus(stat, name);
        data.getBonusStats().addBonus(stat, name, "*", multiplier);
        return true;
    }

    private static boolean removeBonusIfPresent(StatsData data, String stat, String name) {
        if (!data.getBonusStats().hasBonus(stat, name)) {
            return false;
        }
        data.getBonusStats().removeBonus(stat, name);
        return true;
    }

    private static void syncBerserkerCritBonuses(ServerPlayer player, StatsData data) {
        syncBerserkerCritAttribute(
                player,
                data,
                MainAttributes.CRIT_CHANCE.get(),
                BERSERKER_CRIT_CHANCE_UUID,
                "Berserker crit chance",
                ClassSkillHelper.berserkerCritChancePerMissingHpPercent(data)
        );
        syncBerserkerCritAttribute(
                player,
                data,
                MainAttributes.CRIT_DAMAGE.get(),
                BERSERKER_CRIT_DAMAGE_UUID,
                "Berserker crit damage",
                ClassSkillHelper.berserkerCritDamagePerMissingHpPercent(data)
        );
    }

    private static void syncBerserkerCritAttribute(ServerPlayer player, StatsData data, net.minecraft.world.entity.ai.attributes.Attribute attributeType, UUID uuid, String name, double perMissingHpPercent) {
        AttributeInstance attribute = player.getAttribute(attributeType);
        if (attribute == null) {
            return;
        }
        AttributeModifier existing = attribute.getModifier(uuid);
        if (existing != null) {
            attribute.removeModifier(existing);
        }
        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.BERSERKER) || player.getMaxHealth() <= 0F || perMissingHpPercent <= 0D) {
            return;
        }
        double missingHpPercent = Math.max(0D, 1D - (player.getHealth() / player.getMaxHealth())) * 100D;
        double amount = missingHpPercent * perMissingHpPercent;
        if (amount > 0D) {
            attribute.addTransientModifier(new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void syncBerserkerCritDamage(ServerPlayer player, StatsData data) {
        AttributeInstance critDamage = player.getAttribute(MainAttributes.CRIT_DAMAGE.get());
        if (critDamage == null) {
            return;
        }
        AttributeModifier existing = critDamage.getModifier(BERSERKER_CRIT_DAMAGE_UUID);
        if (existing != null) {
            critDamage.removeModifier(existing);
        }
        if (!ClassSkillHelper.hasClassPassive(data, ClassSkillHelper.BERSERKER) || player.getMaxHealth() <= 0F) {
            return;
        }
        double missingHpPercent = Math.max(0D, 1D - (player.getHealth() / player.getMaxHealth())) * 100D;
        double amount = missingHpPercent * ClassSkillHelper.berserkerCritDamagePerMissingHpPercent(data);
        if (amount > 0.000001D) {
            critDamage.addTransientModifier(new AttributeModifier(BERSERKER_CRIT_DAMAGE_UUID, "Berserker crit damage", amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static boolean isGuardBroken(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        StatsData data = getData(player);
        return data != null && data.getResources().getCurrentPoise() <= 0.01F;
    }

    private static void redirectParriedKiBlast(AbstractKiProjectile projectile, ServerPlayer defender, StatsData data) {
        projectile.setOwner(defender);
        double speed = Math.max(projectile.getDeltaMovement().length(), projectile.getKiSpeed());
        projectile.setDeltaMovement(defender.getLookAngle().normalize().scale(speed * (1D + ClassSkillHelper.duelistKiParrySpeedBonus(data))));
        projectile.hurtMarked = true;
    }

    private static double getKiUtilityCostReduction(StatsData data, KiAttackData technique) {
        if (isDamageKiAttack(technique)) {
            return ClassSkillHelper.kiCostReduction(data, ClassSkillHelper.SPIRITUALIST);
        }
        if (isHealKiAttack(technique)) {
            return ClassSkillHelper.kiCostReduction(data, ClassSkillHelper.CLERIC);
        }
        return 0D;
    }

    private static double getKiUtilityCooldownReduction(StatsData data, KiAttackData technique) {
        boolean hasEffects = hasAnyKiEffect(technique);
        if (isDamageKiAttack(technique)) {
            return ClassSkillHelper.kiCooldownReduction(data, ClassSkillHelper.SPIRITUALIST, hasEffects);
        }
        if (isHealKiAttack(technique)) {
            return ClassSkillHelper.kiCooldownReduction(data, ClassSkillHelper.CLERIC, hasEffects);
        }
        return 0D;
    }

    public static double kiEffectDurationMultiplier(StatsData data, KiAttackData technique) {
        if (!hasAnyKiEffect(technique)) {
            return 1D;
        }
        if (isDamageKiAttack(technique)) {
            return 1D + ClassSkillHelper.kiEffectDurationBonus(data, ClassSkillHelper.SPIRITUALIST);
        }
        if (isHealKiAttack(technique)) {
            return 1D + ClassSkillHelper.kiEffectDurationBonus(data, ClassSkillHelper.CLERIC);
        }
        return 1D;
    }

    private static boolean isDamageKiAttack(KiAttackData technique) {
        return technique != null && technique.getEffectiveUtility().name().equalsIgnoreCase("DAMAGE");
    }

    private static boolean isHealKiAttack(KiAttackData technique) {
        return technique != null && technique.getEffectiveUtility().name().equalsIgnoreCase("HEAL");
    }

    private static boolean hasAnyKiEffect(KiAttackData technique) {
        if (technique == null) {
            return false;
        }
        if (technique.getSecondaryEffectType() != KiAttackData.SecondaryEffectType.NONE) {
            return true;
        }
        if (technique instanceof RevampKiAttackData revamp) {
            return revamp.dmzrevamp$getThirdEffectType() != KiAttackData.SecondaryEffectType.NONE
                    || revamp.dmzrevamp$getFourthEffectType() != KiAttackData.SecondaryEffectType.NONE
                    || revamp.dmzrevamp$getExtraEffectOne().isActive()
                    || revamp.dmzrevamp$getExtraEffectTwo().isActive();
        }
        return false;
    }

    // Returns the value used by isLowHealthTarget.
    private static boolean isLowHealthTarget(LivingEntity target) {
        return target.getMaxHealth() > 0F && (target.getHealth() / target.getMaxHealth()) <= 0.30F;
    }

    // Returns the value used by isDirectPhysicalMelee.
    private static boolean isDirectPhysicalMelee(DamageSource source, ServerPlayer player) {
        return source.getEntity() == player && source.getDirectEntity() == player && !source.isIndirect();
    }

    // Returns the value used by isKiDamage.
    private static boolean isKiDamage(DamageSource source, ServerPlayer player) {
        return source.getEntity() == player && source.getDirectEntity() != player;
    }

    // Returns the value used by getAttackingPlayer.
    private static ServerPlayer getAttackingPlayer(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    // Returns the value used by isLikelyMeleeParry.
    private static boolean isLikelyMeleeParry(DMZEvent.PlayerBlockEvent event, ServerPlayer player, LivingEntity attacker) {
        if (attacker.distanceToSqr(player) > 16.0D) {
            return false;
        }
        double attackerMelee = attacker instanceof ServerPlayer serverPlayer && getData(serverPlayer) != null
                ? getData(serverPlayer).getMaxMeleeDamage()
                : attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return attackerMelee > 0D && event.getOriginalDamage() <= (attackerMelee * 1.75D);
    }

    // Returns the value used by getData.
    private static StatsData getData(ServerPlayer player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
    }

    // Handles the sync logic for this class.
    private static void sync(ServerPlayer player) {
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    // Removes stack counters and temporary stat modifiers when a player dies, logs out, or loses a valid character.
    private static void clearRuntime(ServerPlayer player, StatsData data) {
        BLOCK_PROJECTILE_CACHE.remove(player.getUUID());
        player.getPersistentData().putInt(WARRIOR_STACK_TAG, 0);
        player.getPersistentData().putInt(SPEEDSTER_STACK_TAG, 0);
        removeBerserkerCritModifiers(player);
        if (data != null) {
            data.getBonusStats().removeBonus("SKP", SPEEDSTER_SPEED_BONUS);
            data.getBonusStats().removeBonus("STR", SPEEDSTER_MELEE_BONUS);
        }
    }

    private static void removeBerserkerCritModifiers(ServerPlayer player) {
        AttributeInstance critChance = player.getAttribute(MainAttributes.CRIT_CHANCE.get());
        if (critChance != null) {
            AttributeModifier existing = critChance.getModifier(BERSERKER_CRIT_CHANCE_UUID);
            if (existing != null) {
                critChance.removeModifier(existing);
            }
        }
        AttributeInstance critDamage = player.getAttribute(MainAttributes.CRIT_DAMAGE.get());
        if (critDamage != null) {
            AttributeModifier existing = critDamage.getModifier(BERSERKER_CRIT_DAMAGE_UUID);
            if (existing != null) {
                critDamage.removeModifier(existing);
            }
        }
    }

}
