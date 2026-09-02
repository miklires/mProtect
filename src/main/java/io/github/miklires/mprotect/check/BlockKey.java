package io.github.miklires.mprotect.check;

import org.bukkit.Location;

import java.util.UUID;

public record BlockKey(UUID world, int x, int y, int z) {
    public static BlockKey of(Location location) {
        return new BlockKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
