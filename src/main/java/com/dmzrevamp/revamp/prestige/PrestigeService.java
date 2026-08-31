package com.dmzrevamp.revamp.prestige;

import com.dmzrevamp.compat.DmzSkillProgressionCompat;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.revamp.classes.skills.ClassSkillHelper;
import com.dmzrevamp.racial.CustomRacialCooldownEvents;
import com.dmzrevamp.racial.impl.MajinRevampRacialSkill;
import com.dmzrevamp.racial.impl.SaiyanRpgZenkaiEvents;
import com.dmzrevamp.racial.impl.NamekianRevampRacialSkill;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.init.MainSounds;
import com.dmzrevamp.network.DmzRevampNetwork;
import com.dmzrevamp.network.PrestigeFusionFlashS2CPacket;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

public final class PrestigeService {
    private PrestigeService() {
    }

    public static void tryPrestige(ServerPlayer player) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null || !LevelingRevampConfig.prestigeEnabled()) return;
        if (data.getPlayerQuestData().isInParty()) {
            player.sendSystemMessage(Component.literal("You cannot prestige while in a party. Leave your party to prestige"));
            return;
        }
        if (!PrestigeSystem.canPrestige(data)) return;

        LevelingRevampConfig.Prestige config = LevelingRevampConfig.get().Prestige;
        int nextCount = PrestigeSystem.count(data) + 1;
        resetBonusesAndRacials(player, data, config.bonusesLostOnPrestige);
        if (config.statRevertToInitialOnPrestige) {
            resetStatsToRaceAndClass(data);
        } else {
            reduceCurrentStats(data, config.statPercentageLossOnPrestige);
        }
        if (!config.keepSkillsOnPrestige) {
            removeNonPassiveSkills(data, config.keepFormsOnPrestige);
            // Prestige preserves Ki/Strike techniques, so their independent XP
            // records must survive along with the techniques themselves.
            DmzSkillProgressionCompat.resetProgression(player, false);
        }
        if (!config.keepFormsOnPrestige) {
            clearForms(player, data);
        }
        data.getResources().setTrainingPoints(0F);
        data.getResources().setPendingAttributePoints(0);
        data.getResources().setPowerRelease(0);
        data.getResources().setRelease(0);
        data.getDynamicGrowth().clear();
        if (config.resetQuestsOnPrestige) {
            data.getPlayerQuestData().resetAll();
            data.getPlayerQuestData().requestDifficultyReselect();
        }
        PrestigeSystem.setCount(data, nextCount);

        data.getResources().setCurrentEnergy(data.getMaxEnergy());
        data.getResources().setCurrentStamina(data.getMaxStamina());
        data.getResources().setCurrentPoise(data.getMaxPoise());
        player.setHealth(Math.min(data.getMaxHealth(), player.getMaxHealth()));
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
        playPrestigeFusionEffects(player);
    }

    private static void resetBonusesAndRacials(ServerPlayer player, StatsData data, boolean loseBonuses) {
        if (loseBonuses) {
            data.getBonusStats().clearAllStats();
        }
        // Racial progression always resets on rebirth. These helpers remove
        // only their own named bonuses when general bonuses are preserved.
        SaiyanRpgZenkaiEvents.resetZenkai(player, data);
        MajinRevampRacialSkill.resetAbsorption(player, data);
        NamekianRevampRacialSkill.resetAssimilation(player, data);
        CustomRacialCooldownEvents.clearAllRacialCooldowns(player);
    }

    private static void clearForms(ServerPlayer player, StatsData data) {
        var character = data.getCharacter();
        character.clearActiveForm(player);
        character.clearActiveStackForm(player);
        character.getFormMasteries().clear();
        character.getStackFormMasteries().clear();
        character.getFormsUsedBefore().clear();
        character.getStackFormsUsedBefore().clear();
        character.setSelectedFormGroup("");
        character.setSelectedForm("");
        character.setSelectedStackFormGroup("");
        character.setSelectedStackForm("");
        character.clearPreviousFormRecord();
        character.clearPreviousStackFormRecord();

        // Mirrors `/dmzform set <form> 0`: keeping every configured form skill
        // registered at level zero is required for DMZ's purchase screen to
        // offer level one again after the rebirth.
        var skillsConfig = ConfigManager.getSkillsConfig();
        for (String formSkill : skillsConfig.getFormSkills()) {
            data.getSkills().setSkillLevel(formSkill, 0);
        }
        for (String stackSkill : skillsConfig.getStackSkills()) {
            data.getSkills().setSkillLevel(stackSkill, 0);
        }
    }

    private static void playPrestigeFusionEffects(ServerPlayer player) {
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                MainSounds.FUSION.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
        DmzRevampNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new PrestigeFusionFlashS2CPacket(player.getId())
        );
    }

    private static void resetStatsToRaceAndClass(StatsData data) {
        RaceStatsConfig race = ConfigManager.getRaceStats(data.getCharacter().getRaceName());
        RaceStatsConfig.ClassStats classStats = race == null ? null : race.getClassStats(data.getCharacter().getCharacterClass());
        RaceStatsConfig.BaseStats base = classStats == null ? null : classStats.getBaseStats();
        if (base == null) return;
        Stats stats = data.getStats();
        stats.setStrength(value(base.getStrength()));
        stats.setStrikePower(value(base.getStrikePower()));
        stats.setResistance(value(base.getResistance()));
        stats.setVitality(value(base.getVitality()));
        stats.setKiPower(value(base.getKiPower()));
        stats.setEnergy(value(base.getEnergy()));
    }

    private static void reduceCurrentStats(StatsData data, double configuredLoss) {
        double loss = Double.isFinite(configuredLoss) ? Math.max(0D, Math.min(1D, configuredLoss)) : 0.95D;
        double retained = 1D - loss;
        Stats stats = data.getStats();
        stats.setStrength(reduced(stats.getStrength(), retained));
        stats.setStrikePower(reduced(stats.getStrikePower(), retained));
        stats.setResistance(reduced(stats.getResistance(), retained));
        stats.setVitality(reduced(stats.getVitality(), retained));
        stats.setKiPower(reduced(stats.getKiPower(), retained));
        stats.setEnergy(reduced(stats.getEnergy(), retained));
    }

    private static void removeNonPassiveSkills(StatsData data, boolean preserveForms) {
        Set<String> preserved = new HashSet<>();
        String classSkill = ClassSkillHelper.getSkillForCurrentClass(data);
        if (classSkill != null) preserved.add(classSkill.toLowerCase());
        RaceCharacterConfig race = ConfigManager.getRaceCharacter(data.getCharacter().getRaceName());
        if (race != null && race.getRacialSkill() != null) preserved.add(race.getRacialSkill().toLowerCase());
        if (preserveForms) {
            preserved.addAll(ConfigManager.getSkillsConfig().getFormSkills().stream().map(String::toLowerCase).toList());
            preserved.addAll(ConfigManager.getSkillsConfig().getStackSkills().stream().map(String::toLowerCase).toList());
        }
        for (String skill : Set.copyOf(data.getSkills().getAllSkills().keySet())) {
            if (!preserved.contains(skill.toLowerCase())) data.getSkills().removeSkill(skill);
        }
    }

    private static int value(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static int reduced(int value, double retained) {
        return Math.max(0, (int) Math.floor(Math.max(0, value) * retained));
    }
}
