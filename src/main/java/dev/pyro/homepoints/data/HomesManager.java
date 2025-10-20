package dev.pyro.homepoints.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomesManager extends PersistentState {
    private static final String DATA_NAME = "homepoints_data";

    private final Map<UUID, PlayerHomesData> playerHomes;
    private final PublicHomesData publicHomes;

    public HomesManager() {
        this.playerHomes = new HashMap<>();
        this.publicHomes = new PublicHomesData();
    }

    public static HomesManager get(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return manager.getOrCreate(
                HomesManager::new,
                HomesManager::fromNbt,
                DATA_NAME
        );
    }

    public static HomesManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        HomesManager manager = new HomesManager();

        if (nbt.contains("playerHomes")) {
            NbtList playerList = nbt.getList("playerHomes");
            for (int i = 0; i < playerList.size(); i++) {
                NbtCompound playerNbt = playerList.getCompound(i);
                UUID playerUUID = UUID.fromString(playerNbt.getString("uuid"));
                PlayerHomesData data = PlayerHomesData.fromNbt(playerNbt.getCompound("data"));
                manager.playerHomes.put(playerUUID, data);
            }
        }

        if (nbt.contains("publicHomes")) {
            NbtCompound publicNbt = nbt.getCompound("publicHomes");
            PublicHomesData loaded = PublicHomesData.fromNbt(publicNbt);
            for (String homeName : loaded.getPublicHomeNames()) {
                manager.publicHomes.setPublicHome(loaded.getPublicHome(homeName));
            }
        }

        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList playerList = new NbtList();
        for (Map.Entry<UUID, PlayerHomesData> entry : playerHomes.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putString("uuid", entry.getKey().toString());
            playerNbt.put("data", entry.getValue().toNbt());
            playerList.add(playerNbt);
        }
        nbt.put("playerHomes", playerList);

        nbt.put("publicHomes", publicHomes.toNbt());

        return nbt;
    }

    public PlayerHomesData getPlayerData(UUID playerUUID) {
        return playerHomes.computeIfAbsent(playerUUID, k -> new PlayerHomesData());
    }

    public boolean setPlayerHome(UUID playerUUID, Home home) {
        boolean success = getPlayerData(playerUUID).setHome(home);
        if (success) {
            markDirty();
        }
        return success;
    }

    public boolean deletePlayerHome(UUID playerUUID, String homeName) {
        boolean success = getPlayerData(playerUUID).deleteHome(homeName);
        if (success) {
            markDirty();
        }
        return success;
    }

    public Home getPlayerHome(UUID playerUUID, String homeName) {
        return getPlayerData(playerUUID).getHome(homeName);
    }

    public void setPublicHome(Home home) {
        publicHomes.setPublicHome(home);
        markDirty();
    }

    public boolean deletePublicHome(String homeName) {
        boolean success = publicHomes.deletePublicHome(homeName);
        if (success) {
            markDirty();
        }
        return success;
    }

    public Home getPublicHome(String homeName) {
        return publicHomes.getPublicHome(homeName);
    }

    public PublicHomesData getPublicHomesData() {
        return publicHomes;
    }
}