package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ChunkKey;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SculkBloomEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.time.Duration;

public final class PhysicsProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final KeyedRateLimiter<ChunkKey> updates = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> fluids = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> spread = new KeyedRateLimiter<>(16_384);

    public PhysicsProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (allow(updates, ChunkKey.of(event.getBlock()), "physics.block-updates")) return;
        event.setCancelled(true);
        report("block physics rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent event) {
        if (allow(fluids, ChunkKey.of(event.getBlock()), "physics.fluids")) return;
        event.setCancelled(true);
        report("fluid update rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (allow(spread, ChunkKey.of(event.getBlock()), "physics.spread")) return;
        event.setCancelled(true);
        report("block spread rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (allow(spread, ChunkKey.of(event.getBlock()), "physics.spread")) return;
        event.setCancelled(true);
        report("fire ignition rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (allow(spread, ChunkKey.of(event.getBlock()), "physics.spread")) return;
        event.setCancelled(true);
        report("fire update rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGameEvent(BlockReceiveGameEvent event) {
        if (allow(spread, ChunkKey.of(event.getBlock()), "physics.spread")) return;
        event.setCancelled(true);
        report("sculk game-event rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSculkBloom(SculkBloomEvent event) {
        if (allow(spread, ChunkKey.of(event.getBlock()), "physics.spread")) return;
        event.setCancelled(true);
        report("sculk bloom rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSponge(SpongeAbsorbEvent event) {
        multi(event.getBlocks().size(), event.getBlock().getLocation(), event::setCancelled, "sponge update");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        multi(event.getBlocks().size(), event.getBlock().getLocation(), event::setCancelled, "fertilize update");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        multi(event.getBlocks().size(), event.getLocation(), event::setCancelled, "structure growth");
    }

    private boolean allow(KeyedRateLimiter<ChunkKey> limiter, ChunkKey key, String section) {
        if (!plugin.config().enabled(CheckType.PHYSICS)) return true;
        return limiter.allow(key, plugin.config().integer(section + ".events", 500),
                Duration.ofSeconds(plugin.config().integer(section + ".window-seconds", 1)), System.nanoTime());
    }

    private void multi(int blocks, Location location, java.util.function.Consumer<Boolean> cancel, String name) {
        if (!plugin.config().enabled(CheckType.PHYSICS)) return;
        int maximum = plugin.config().integer("physics.max-multi-block-changes", 512);
        if (blocks <= maximum) return;
        cancel.accept(true);
        report(name + " affected " + blocks + " blocks", location);
    }

    private void report(String detail, Location location) {
        plugin.violations().recordSystem(CheckType.PHYSICS, detail, location);
    }
}
