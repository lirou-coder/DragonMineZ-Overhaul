package com.dmzrevamp.command;

import com.dmzrevamp.DmzRevampMod;
import com.dmzrevamp.config.LevelingRevampConfig;
import com.dmzrevamp.racial.CustomRacialCooldownEvents;
import com.dmzrevamp.revamp.classes.skills.ClassSkillEvents;
import com.dmzrevamp.revamp.fusion.FusionRevampLogic;
import com.dmzrevamp.revamp.prestige.PrestigeSystem;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber(modid = DmzRevampMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DmzRevampCommands {
    // Forge calls the static registration hook directly, so this command holder should not be instantiated.
    private DmzRevampCommands() {
    }

    @SubscribeEvent
    // Adds Overhaul commands when Minecraft builds the server command tree.
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    // Registers admin cooldown resets and the player fusion-finish shortcut.
    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzrevamp")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("resetcooldowns")
                        .executes(context -> resetCooldowns(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> resetCooldowns(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));
        dispatcher.register(Commands.literal("dmzfusion")
                .then(Commands.literal("finish")
                        .executes(context -> finishFusion(context.getSource(), context.getSource().getPlayerOrException()))));
        dispatcher.register(Commands.literal("dmzprestige")
                .requires(source -> source.hasPermission(2))
                .then(prestigeOperation("add", PrestigeOperation.ADD))
                .then(prestigeOperation("set", PrestigeOperation.SET))
                .then(prestigeOperation("remove", PrestigeOperation.REMOVE))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> changePrestige(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        PrestigeOperation.CLEAR,
                                        0)))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> prestigeOperation(
            String name,
            PrestigeOperation operation
    ) {
        return Commands.literal(name)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("number", IntegerArgumentType.integer(0))
                                .executes(context -> changePrestige(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        operation,
                                        IntegerArgumentType.getInteger(context, "number")))));
    }

    private static int changePrestige(
            CommandSourceStack source,
            ServerPlayer player,
            PrestigeOperation operation,
            int amount
    ) {
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
        if (data == null) {
            source.sendFailure(Component.literal("Could not access " + player.getGameProfile().getName() + "'s DMZ stats."));
            return 0;
        }

        int current = PrestigeSystem.count(data);
        long requested = switch (operation) {
            case ADD -> (long) current + amount;
            case SET -> amount;
            case REMOVE -> (long) current - amount;
            case CLEAR -> 0L;
        };
        int maximum = LevelingRevampConfig.get().Prestige.maxPrestigeCount;
        int updated = (int) Math.max(0L, Math.min(maximum, requested));
        PrestigeSystem.setCount(data, updated);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        source.sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName() + "'s prestige to " + updated + "."), true);
        return 1;
    }

    private enum PrestigeOperation {
        ADD,
        SET,
        REMOVE,
        CLEAR
    }

    // Clears Overhaul's racial and class runtime cooldowns for each selected player.
    private static int resetCooldowns(CommandSourceStack source, Collection<ServerPlayer> players) throws CommandSyntaxException {
        int changed = 0;
        for (ServerPlayer player : players) {
            boolean removed = CustomRacialCooldownEvents.clearAllRacialCooldowns(player);
            removed |= ClassSkillEvents.clearClassCooldowns(player);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            if (removed) {
                changed++;
            }
        }

        int totalPlayers = players.size();
        source.sendSuccess(() -> Component.literal("Reset Dragon Mine Z: Overhaul racial and class cooldowns for " + totalPlayers + " player" + (totalPlayers == 1 ? "." : "s.")), true);
        return Math.max(1, changed);
    }

    private static int finishFusion(CommandSourceStack source, ServerPlayer player) {
        int changed = FusionRevampLogic.finishFusion(player, true);
        if (changed > 0) {
            source.sendSuccess(() -> Component.literal("Fusion finished."), true);
        } else {
            source.sendFailure(Component.literal("You are not fused."));
        }
        return changed;
    }
}
