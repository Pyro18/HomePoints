package dev.pyro.homepoints.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.pyro.homepoints.data.Home;
import dev.pyro.homepoints.data.HomesManager;
import dev.pyro.homepoints.data.PlayerHomesData;
import dev.pyro.homepoints.util.Messages;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShareHomeCommand {

    private static final Map<UUID, Map<UUID, String>> pendingShares = new HashMap<>();

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_PLAYER_HOMES = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            HomesManager manager = HomesManager.get(player.getServer());
            PlayerHomesData data = manager.getPlayerData(player.getUuid());
            return CommandSource.suggestMatching(data.getHomeNames(), builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sharehome")
                .then(CommandManager.argument("home_name", StringArgumentType.word())
                        .suggests(SUGGEST_PLAYER_HOMES)
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ShareHomeCommand::shareHome)
                        )
                )
        );

        dispatcher.register(CommandManager.literal("acceptshare")
                .then(CommandManager.argument("from_player", StringArgumentType.word())
                        .then(CommandManager.argument("home_name", StringArgumentType.word())
                                .executes(ShareHomeCommand::acceptShare)
                        )
                )
        );
    }

    private static int shareHome(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity fromPlayer)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String homeName = StringArgumentType.getString(context, "home_name");
        ServerPlayerEntity toPlayer;

        try {
            toPlayer = EntityArgumentType.getPlayer(context, "player");
        } catch (Exception e) {
            fromPlayer.sendMessage(Messages.error("Player not found!"));
            return 0;
        }

        if (fromPlayer.getUuid().equals(toPlayer.getUuid())) {
            fromPlayer.sendMessage(Messages.error("You can't share a home with yourself!"));
            return 0;
        }

        HomesManager manager = HomesManager.get(fromPlayer.getServer());
        Home home = manager.getPlayerHome(fromPlayer.getUuid(), homeName);

        if (home == null) {
            fromPlayer.sendMessage(Messages.error("Home '" + homeName + "' not found!"));
            return 0;
        }

        PlayerHomesData targetData = manager.getPlayerData(toPlayer.getUuid());
        if (!targetData.hasHome(homeName) && targetData.getHomeCount() >= PlayerHomesData.MAX_HOMES) {
            fromPlayer.sendMessage(Messages.error(toPlayer.getName().getString() +
                    " has reached the maximum number of homes!"));
            return 0;
        }

        pendingShares.computeIfAbsent(fromPlayer.getUuid(), k -> new HashMap<>())
                .put(toPlayer.getUuid(), homeName);

        fromPlayer.sendMessage(Messages.success("Share request sent to " + toPlayer.getName().getString() + "!"));
        toPlayer.sendMessage(Messages.shareHomeMessage(homeName, fromPlayer.getName().getString()));

        return 1;
    }

    private static int acceptShare(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity toPlayer)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String fromPlayerName = StringArgumentType.getString(context, "from_player");
        String homeName = StringArgumentType.getString(context, "home_name");

        ServerPlayerEntity fromPlayer = toPlayer.getServer().getPlayerManager()
                .getPlayer(fromPlayerName);

        if (fromPlayer == null) {
            toPlayer.sendMessage(Messages.error("Player " + fromPlayerName + " is not online!"));
            return 0;
        }

        Map<UUID, String> playerShares = pendingShares.get(fromPlayer.getUuid());
        if (playerShares == null || !homeName.equals(playerShares.get(toPlayer.getUuid()))) {
            toPlayer.sendMessage(Messages.error("No pending share request from " + fromPlayerName +
                    " for home '" + homeName + "'!"));
            return 0;
        }

        HomesManager manager = HomesManager.get(toPlayer.getServer());

        Home originalHome = manager.getPlayerHome(fromPlayer.getUuid(), homeName);
        if (originalHome == null) {
            toPlayer.sendMessage(Messages.error("The shared home no longer exists!"));
            playerShares.remove(toPlayer.getUuid());
            return 0;
        }

        Home copiedHome = new Home(
                originalHome.getName(),
                originalHome.getPosition(),
                originalHome.getDimension(),
                originalHome.getYaw(),
                originalHome.getPitch(),
                toPlayer.getUuidAsString()
        );

        boolean success = manager.setPlayerHome(toPlayer.getUuid(), copiedHome);

        if (success) {
            toPlayer.sendMessage(Messages.success("Home '" + homeName + "' added to your homes!"));
            fromPlayer.sendMessage(Messages.success(toPlayer.getName().getString() +
                    " accepted your home share!"));

            playerShares.remove(toPlayer.getUuid());
            if (playerShares.isEmpty()) {
                pendingShares.remove(fromPlayer.getUuid());
            }

            return 1;
        } else {
            toPlayer.sendMessage(Messages.error("You've reached the maximum of " +
                    PlayerHomesData.MAX_HOMES + " homes!"));
            return 0;
        }
    }
}