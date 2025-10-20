package dev.pyro.homepoints;

import dev.pyro.homepoints.commands.HomeCommands;
import dev.pyro.homepoints.commands.PublicHomeCommands;
import dev.pyro.homepoints.commands.ShareHomeCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomePoints implements ModInitializer {

    public static final String MOD_ID = "homepoints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Home Points mod initialized!");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            HomeCommands.register(dispatcher);
            PublicHomeCommands.register(dispatcher);
            ShareHomeCommand.register(dispatcher);

            LOGGER.info("Home Points commands registered successfully!");
        });
    }
}