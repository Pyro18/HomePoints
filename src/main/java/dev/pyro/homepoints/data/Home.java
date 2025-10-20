package dev.pyro.homepoints.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Home {
    private final String name;
    private final BlockPos position;
    private final RegistryKey<World> dimension;
    private final float yaw;
    private final float pitch;
    private final String owner;

    public Home(String name, BlockPos position, RegistryKey<World> dimension, float yaw, float pitch, String owner) {
        this.name = name;
        this.position = position;
        this.dimension = dimension;
        this.yaw = yaw;
        this.pitch = pitch;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public BlockPos getPosition() {
        return position;
    }

    public RegistryKey<World> getDimension() {
        return dimension;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getOwner() {
        return owner;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);
        nbt.putInt("x", position.getX());
        nbt.putInt("y", position.getY());
        nbt.putInt("z", position.getZ());
        nbt.putString("dimension", dimension.getValue().toString());
        nbt.putFloat("yaw", yaw);
        nbt.putFloat("pitch", pitch);
        nbt.putString("owner", owner);
        return nbt;
    }

    public static Home fromNbt(NbtCompound nbt) {
        String name = nbt.getString("name");
        BlockPos pos = new BlockPos(
                nbt.getInt("x"),
                nbt.getInt("y"),
                nbt.getInt("z")
        );
        RegistryKey<World> dimension = RegistryKey.of(
                RegistryKeys.WORLD,
                Identifier.of(nbt.getString("dimension"))
        );
        float yaw = nbt.getFloat("yaw");
        float pitch = nbt.getFloat("pitch");
        String owner = nbt.getString("owner");

        return new Home(name, pos, dimension, yaw, pitch, owner);
    }
}