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
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

public class HomeCommands {

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_HOMES = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            HomesManager manager = HomesManager.get(player.getServer());
            PlayerHomesData data = manager.getPlayerData(player.getUuid());
            return CommandSource.suggestMatching(data.getHomeNames(), builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sethome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(HomeCommands::setHome)
                )
        );

        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(SUGGEST_HOMES)
                        .executes(HomeCommands::deleteHome)
                )
        );

        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(SUGGEST_HOMES)
                        .executes(HomeCommands::teleportHome)
                )
        );

        dispatcher.register(CommandManager.literal("homes")
                .executes(HomeCommands::listHomes)
        );
    }

    private static int setHome(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String homeName = StringArgumentType.getString(context, "name");

        if (!homeName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(Messages.error("Home name can only contain letters, numbers, and underscores!"));
            return 0;
        }

        HomesManager manager = HomesManager.get(player.getServer());
        BlockPos pos = player.getBlockPos();
        RegistryKey<World> dimension = player.getWorld().getRegistryKey();

        Home home = new Home(
                homeName,
                pos,
                dimension,
                player.getYaw(),
                player.getPitch(),
                player.getUuidAsString()
        );

        boolean success = manager.setPlayerHome(player.getUuid(), home);

        if (success) {
            player.sendMessage(Messages.success("Home '" + homeName + "' set at your current location!"));
            return 1;
        } else {
            player.sendMessage(Messages.error("You've reached the maximum of " + PlayerHomesData.MAX_HOMES + " homes!"));
            return 0;
        }
    }

    private static int deleteHome(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String homeName = StringArgumentType.getString(context, "name");
        HomesManager manager = HomesManager.get(player.getServer());

        boolean success = manager.deletePlayerHome(player.getUuid(), homeName);

        if (success) {
            player.sendMessage(Messages.success("Home '" + homeName + "' deleted!"));
            return 1;
        } else {
            player.sendMessage(Messages.error("Home '" + homeName + "' not found!"));
            return 0;
        }
    }

    private static int teleportHome(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String homeName = StringArgumentType.getString(context, "name");
        HomesManager manager = HomesManager.get(player.getServer());
        Home home = manager.getPlayerHome(player.getUuid(), homeName);

        if (home == null) {
            player.sendMessage(Messages.error("Home '" + homeName + "' not found!"));
            return 0;
        }

        ServerWorld targetWorld = player.getServer().getWorld(home.getDimension());
        if (targetWorld == null) {
            player.sendMessage(Messages.error("Target dimension not found!"));
            return 0;
        }

        BlockPos pos = home.getPosition();

        if (player.getWorld().getRegistryKey() == home.getDimension()) {
            player.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        } else {
            player.teleport(targetWorld, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    home.getYaw(), home.getPitch());
        }

        player.setYaw(home.getYaw());
        player.setPitch(home.getPitch());

        player.sendMessage(Messages.success("Teleported to home '" + homeName + "'!"));
        return 1;
    }

    private static int listHomes(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        HomesManager manager = HomesManager.get(player.getServer());
        PlayerHomesData data = manager.getPlayerData(player.getUuid());
        Set<String> homeNames = data.getHomeNames();

        if (homeNames.isEmpty()) {
            player.sendMessage(Messages.info("You don't have any homes yet. Use /sethome <name> to create one!"));
            return 0;
        }

        player.sendMessage(Text.literal("=== Your Homes (" + homeNames.size() + "/" +
                PlayerHomesData.MAX_HOMES + ") ===").formatted(Formatting.GOLD));

        homeNames.forEach(name -> {
            Home home = data.getHome(name);
            String dimensionName = home.getDimension().getValue().getPath();
            player.sendMessage(
                    Text.literal("  • ").formatted(Formatting.GRAY)
                            .append(Text.literal(name).formatted(Formatting.AQUA))
                            .append(Text.literal(" - ").formatted(Formatting.GRAY))
                            .append(Text.literal(dimensionName).formatted(Formatting.YELLOW))
            );
        });

        return 1;
    }
}