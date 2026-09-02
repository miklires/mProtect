package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ChunkKey;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

import java.time.Duration;

public final class RedstoneAutomationListener implements Listener {
    private final MProtectPlugin plugin;
    private final KeyedRateLimiter<ChunkKey> redstone = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> pistons = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> hoppers = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> dispensers = new KeyedRateLimiter<>(16_384);

    public RedstoneAutomationListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRedstone(BlockRedstoneEvent event) {
        if (!plugin.config().enabled(CheckType.REDSTONE) || event.getOldCurrent() == event.getNewCurrent()) return;
        if (allow(redstone, ChunkKey.of(event.getBlock()), "redstone")) return;
        event.setNewCurrent(event.getOldCurrent());
        report(CheckType.REDSTONE, "redstone update rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        piston(event.getBlock().getLocation(), event.getBlocks().size(), event::setCancelled);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        piston(event.getBlock().getLocation(), event.getBlocks().size(), event::setCancelled);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!plugin.config().enabled(CheckType.AUTOMATION)) return;
        Location location = event.getInitiator().getLocation();
        if (location == null) location = event.getSource().getLocation();
        if (location == null || allow(hoppers, ChunkKey.of(location), "automation.hoppers")) return;
        event.setCancelled(true);
        report(CheckType.AUTOMATION, "hopper transfer rate exceeded", location);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (!plugin.config().enabled(CheckType.AUTOMATION)) return;
        Location location = event.getInventory().getLocation();
        if (location == null || allow(hoppers, ChunkKey.of(location), "automation.hoppers")) return;
        event.setCancelled(true);
        report(CheckType.AUTOMATION, "hopper pickup rate exceeded", location);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (!plugin.config().enabled(CheckType.AUTOMATION)
                || allow(dispensers, ChunkKey.of(event.getBlock()), "automation.dispensers")) return;
        event.setCancelled(true);
        report(CheckType.AUTOMATION, "dispenser rate exceeded", event.getBlock().getLocation());
    }

    private void piston(Location location, int movedBlocks, java.util.function.Consumer<Boolean> cancel) {
        if (!plugin.config().enabled(CheckType.AUTOMATION)) return;
        int maximum = plugin.config().integer("automation.pistons.max-blocks-per-move", 12);
        if (movedBlocks <= maximum && allow(pistons, ChunkKey.of(location), "automation.pistons")) return;
        cancel.accept(true);
        report(CheckType.AUTOMATION, movedBlocks > maximum ? "piston moved too many blocks" : "piston rate exceeded", location);
    }

    private <K> boolean allow(KeyedRateLimiter<K> limiter, K key, String section) {
        int events = plugin.config().integer(section + ".events", 100);
        int seconds = plugin.config().integer(section + ".window-seconds", 1);
        return limiter.allow(key, events, Duration.ofSeconds(seconds), System.nanoTime());
    }

    private void report(CheckType type, String detail, Location location) {
        plugin.violations().recordSystem(type, detail, location);
    }
}
