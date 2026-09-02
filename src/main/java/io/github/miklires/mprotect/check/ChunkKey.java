package io.github.miklires.mprotect.check;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.UUID;

public record ChunkKey(UUID world, int x, int z) {
    public static ChunkKey of(Location location) {
        return new ChunkKey(location.getWorld().getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static ChunkKey of(Block block) {
        return new ChunkKey(block.getWorld().getUID(), block.getX() >> 4, block.getZ() >> 4);
    }
}
