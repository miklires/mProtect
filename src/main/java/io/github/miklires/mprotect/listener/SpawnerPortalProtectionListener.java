package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.BlockKey;
import io.github.miklires.mprotect.check.ChunkKey;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.time.Duration;
import java.util.UUID;

public final class SpawnerPortalProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final KeyedRateLimiter<BlockKey> spawnerBlocks = new KeyedRateLimiter<>(32_768);
    private final KeyedRateLimiter<ChunkKey> spawnerChunks = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> portalCreates = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<UUID> portalPlayers = new KeyedRateLimiter<>(4096);
    private final KeyedRateLimiter<UUID> portalNotices = new KeyedRateLimiter<>(4096);

    public SpawnerPortalProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawner(SpawnerSpawnEvent event) {
        if (!plugin.config().enabled(CheckType.SPAWNERS)) return;
        Location location = event.getSpawner().getLocation();
        boolean blockAllowed = allow(spawnerBlocks, BlockKey.of(location), "spawners.per-block", 8, 10);
        boolean chunkAllowed = allow(spawnerChunks, ChunkKey.of(location), "spawners.per-chunk", 32, 10);
        if (blockAllowed && chunkAllowed) return;
        event.setCancelled(true);
        plugin.violations().recordSystem(CheckType.SPAWNERS,
                blockAllowed ? "spawner chunk rate exceeded" : "individual spawner rate exceeded", location);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (!plugin.config().enabled(CheckType.PORTALS)) return;
        Location location = portalLocation(event);
        if (location == null) return;
        int maximum = plugin.config().integer("portals.max-created-blocks", 128);
        boolean allowed = allow(portalCreates, ChunkKey.of(location), "portals.create", 4, 10);
        if (allowed && event.getBlocks().size() <= maximum) return;
        event.setCancelled(true);
        plugin.violations().recordSystem(CheckType.PORTALS,
                allowed ? "portal creation affected " + event.getBlocks().size() + " blocks" : "portal creation rate exceeded", location);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!plugin.config().enabled(CheckType.PORTALS) || event.getPlayer().hasPermission("mprotect.bypass.portals")) return;
        int events = plugin.config().integer("portals.player-use.events", 8);
        int seconds = plugin.config().integer("portals.player-use.window-seconds", 10);
        if (!portalPlayers.allow(event.getPlayer().getUniqueId(), events, Duration.ofSeconds(seconds), System.nanoTime())) {
            event.setCancelled(true);
            if (portalNotices.allow(event.getPlayer().getUniqueId(), 1, Duration.ofSeconds(1), System.nanoTime())) {
                plugin.violations().record(event.getPlayer(), CheckType.PORTALS, "portal use rate exceeded");
                plugin.messages().send(event.getPlayer(), "blocked");
            }
            return;
        }
        event.setSearchRadius(Math.min(event.getSearchRadius(), plugin.config().integer("portals.max-search-radius", 32)));
        event.setCreationRadius(Math.min(event.getCreationRadius(), plugin.config().integer("portals.max-creation-radius", 16)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        portalPlayers.remove(event.getPlayer().getUniqueId());
        portalNotices.remove(event.getPlayer().getUniqueId());
    }

    private Location portalLocation(PortalCreateEvent event) {
        if (event.getEntity() != null) return event.getEntity().getLocation();
        if (event.getBlocks().isEmpty()) return null;
        BlockState first = event.getBlocks().getFirst();
        return first.getLocation();
    }

    private <K> boolean allow(KeyedRateLimiter<K> limiter, K key, String section, int fallbackEvents, int fallbackSeconds) {
        return limiter.allow(key, plugin.config().integer(section + ".events", fallbackEvents),
                Duration.ofSeconds(plugin.config().integer(section + ".window-seconds", fallbackSeconds)), System.nanoTime());
    }
}
