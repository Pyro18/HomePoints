package dev.pyro.homepoints.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlayerHomesData {
    public static final int MAX_HOMES = 100;

    private final Map<String, Home> homes;

    public PlayerHomesData() {
        this.homes = new HashMap<>();
    }

    public boolean setHome(Home home) {
        if (!homes.containsKey(home.getName()) && homes.size() >= MAX_HOMES) {
            return false;
        }
        homes.put(home.getName(), home);
        return true;
    }

    public boolean deleteHome(String name) {
        return homes.remove(name) != null;
    }

    public Home getHome(String name) {
        return homes.get(name);
    }

    public Set<String> getHomeNames() {
        return homes.keySet();
    }

    public boolean hasHome(String name) {
        return homes.containsKey(name);
    }

    public int getHomeCount() {
        return homes.size();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList homesList = new NbtList();

        for (Home home : homes.values()) {
            homesList.add(home.toNbt());
        }

        nbt.put("homes", homesList);
        return nbt;
    }

    public static PlayerHomesData fromNbt(NbtCompound nbt) {
        PlayerHomesData data = new PlayerHomesData();

        if (nbt.contains("homes", 9)) { // 9 = TAG_LIST
            NbtList homesList = nbt.getList("homes", 10); // 10 = TAG_COMPOUND
            for (int i = 0; i < homesList.size(); i++) {
                Home home = Home.fromNbt(homesList.getCompound(i));
                data.homes.put(home.getName(), home);
            }
        }

        return data;
    }
}