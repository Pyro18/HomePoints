package dev.pyro.homepoints.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.pyro.homepoints.data.Home;
import dev.pyro.homepoints.data.HomesManager;
import dev.pyro.homepoints.data.PublicHomesData;
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

public class PublicHomeCommands {

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_PUBLIC_HOMES = (context, builder) -> {
        if (context.getSource().getServer() != null) {
            HomesManager manager = HomesManager.get(context.getSource().getServer());
            PublicHomesData data = manager.getPublicHomesData();
            return CommandSource.suggestMatching(data.getPublicHomeNames(), builder);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("psethome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(PublicHomeCommands::setPublicHome)
                )
        );

        dispatcher.register(CommandManager.literal("pdelhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(SUGGEST_PUBLIC_HOMES)
                        .executes(PublicHomeCommands::deletePublicHome)
                )
        );

        dispatcher.register(CommandManager.literal("phome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(SUGGEST_PUBLIC_HOMES)
                        .executes(PublicHomeCommands::teleportPublicHome)
                )
        );

        dispatcher.register(CommandManager.literal("phomes")
                .executes(PublicHomeCommands::listPublicHomes)
        );
    }

    private static int setPublicHome(CommandContext<ServerCommandSource> context) {
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

        Home existingHome = manager.getPublicHome(homeName);
        if (existingHome != null && !existingHome.getOwner().equals(player.getUuidAsString())) {
            player.sendMessage(Messages.error("Public home '" + homeName + "' already exists and you're not the owner!"));
            return 0;
        }

        Home home = new Home(
                homeName,
                pos,
                dimension,
                player.getYaw(),
                player.getPitch(),
                player.getUuidAsString()
        );

        manager.setPublicHome(home);

        if (existingHome != null) {
            player.sendMessage(Messages.success("Public home '" + homeName + "' updated!"));
        } else {
            player.sendMessage(Messages.success("Public home '" + homeName + "' created!"));
        }

        return 1;
    }

    private static int deletePublicHome(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String homeName = StringArgumentType.getString(context, "name");
        HomesManager manager = HomesManager.get(player.getServer());
        Home home = manager.getPublicHome(homeName);

        if (home == null) {
            player.sendMessage(Messages.error("Public home '" + homeName + "' not found!"));
            return 0;
        }

        if (!home.getOwner().equals(player.getUuidAsString())) {
            player.sendMessage(Messages.error("You're not the owner of public home '" + homeName + "'!"));
            return 0;
        }

        manager.deletePublicHome(homeName);
        player.sendMessage(Messages.success("Public home '" + homeName + "' deleted!"));

        return 1;
    }

    private static int teleportPublicHome(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        String homeName = StringArgumentType.getString(context, "name");
        HomesManager manager = HomesManager.get(player.getServer());
        Home home = manager.getPublicHome(homeName);

        if (home == null) {
            player.sendMessage(Messages.error("Public home '" + homeName + "' not found!"));
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

        player.sendMessage(Messages.success("Teleported to public home '" + homeName + "'!"));
        return 1;
    }

    private static int listPublicHomes(CommandContext<ServerCommandSource> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
            context.getSource().sendError(Messages.error("Only players can use this command!"));
            return 0;
        }

        HomesManager manager = HomesManager.get(player.getServer());
        PublicHomesData data = manager.getPublicHomesData();
        Set<String> homeNames = data.getPublicHomeNames();

        if (homeNames.isEmpty()) {
            player.sendMessage(Messages.info("There are no public homes yet!"));
            return 0;
        }

        player.sendMessage(Text.literal("=== Public Homes (" + homeNames.size() + ") ===")
                .formatted(Formatting.GOLD));

        homeNames.forEach(name -> {
            Home home = data.getPublicHome(name);
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