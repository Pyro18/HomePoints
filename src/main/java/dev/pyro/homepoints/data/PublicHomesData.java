package dev.pyro.homepoints.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PublicHomesData {
    private final Map<String, Home> publicHomes;

    public PublicHomesData() {
        this.publicHomes = new HashMap<>();
    }

    public void setPublicHome(Home home) {
        publicHomes.put(home.getName(), home);
    }

    public boolean deletePublicHome(String name) {
        return publicHomes.remove(name) != null;
    }

    public Home getPublicHome(String name) {
        return publicHomes.get(name);
    }

    public Set<String> getPublicHomeNames() {
        return publicHomes.keySet();
    }

    public boolean hasPublicHome(String name) {
        return publicHomes.containsKey(name);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList homesList = new NbtList();

        for (Home home : publicHomes.values()) {
            homesList.add(home.toNbt());
        }

        nbt.put("publicHomes", homesList);
        return nbt;
    }

    public static PublicHomesData fromNbt(NbtCompound nbt) {
        PublicHomesData data = new PublicHomesData();

        if (nbt.contains("publicHomes", 9)) { // 9 = TAG_LIST
            NbtList homesList = nbt.getList("publicHomes", 10); // 10 = TAG_COMPOUND
            for (int i = 0; i < homesList.size(); i++) {
                Home home = Home.fromNbt(homesList.getCompound(i));
                data.publicHomes.put(home.getName(), home);
            }
        }

        return data;
    }
}