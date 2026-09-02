package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ChunkKey;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;

public final class EntityLimitListener implements Listener {
    private final MProtectPlugin plugin;
    private final ConcurrentHashMap<ChunkKey, ChunkCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ChunkKey> locations = new ConcurrentHashMap<>();
    private final KeyedRateLimiter<ChunkKey> spawnRates = new KeyedRateLimiter<>(16_384);

    public EntityLimitListener(MProtectPlugin plugin) { this.plugin = plugin; }

    public void initializeLoadedChunks() {
        plugin.scheduler().global(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    plugin.scheduler().region(world, chunk.getX(), chunk.getZ(), () -> load(chunk));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        if (!enabled() || !plugin.config().bool("entities.cancel-excess-spawns", true)) return;
        ChunkKey key = key(event.getEntity());
        ChunkCounter counter = counters.computeIfAbsent(key, ignored -> new ChunkCounter());
        int maxTotal = plugin.config().integer("entities.max-per-chunk", 120);
        int maxType = plugin.config().integer("entities.max-per-type-per-chunk", 40);
        int typeLimit = plugin.config().integer("entities.per-type-limits." + event.getEntityType().name(), maxType);
        int rate = plugin.config().integer("entities.spawn-rate.events", 80);
        int seconds = plugin.config().integer("entities.spawn-rate.window-seconds", 2);
        boolean rateAllowed = spawnRates.allow(key, rate, Duration.ofSeconds(seconds), System.nanoTime());
        if (!rateAllowed || !counter.canAdd(event.getEntityType(), maxTotal, typeLimit)) {
            event.setCancelled(true);
            plugin.violations().recordSystem(CheckType.ENTITIES,
                    "blocked " + event.getEntityType().name().toLowerCase() + (rateAllowed ? " spawn at chunk limit" : " spawn burst"), event.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterSpawn(EntitySpawnEvent event) {
        if (enabled()) {
            ChunkKey key = key(event.getEntity());
            counters.computeIfAbsent(key, ignored -> new ChunkCounter()).add(event.getEntityType());
            locations.put(event.getEntity().getUniqueId(), key);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        ChunkKey key = locations.remove(event.getEntity().getUniqueId());
        if (key == null) key = key(event.getEntity());
        ChunkCounter counter = counters.get(key);
        if (counter != null) counter.remove(event.getEntityType());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoad(ChunkLoadEvent event) { if (enabled()) load(event.getChunk()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(ChunkUnloadEvent event) {
        counters.remove(key(event.getChunk()));
        for (Entity entity : event.getChunk().getEntities()) locations.remove(entity.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(EntityMoveEvent event) { move(event.getEntity(), event.getTo()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) { if (event.getTo() != null) move(event.getEntity(), event.getTo()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleMove(VehicleMoveEvent event) { move(event.getVehicle(), event.getTo()); }

    private void load(Chunk chunk) {
        ChunkCounter counter = new ChunkCounter();
        for (Entity entity : chunk.getEntities()) {
            counter.add(entity.getType());
            locations.put(entity.getUniqueId(), key(chunk));
        }
        counters.put(key(chunk), counter);
    }

    private void move(Entity entity, org.bukkit.Location destination) {
        if (!enabled()) return;
        ChunkKey next = ChunkKey.of(destination);
        ChunkKey previous = locations.put(entity.getUniqueId(), next);
        if (previous == null || previous.equals(next)) return;
        ChunkCounter oldCounter = counters.get(previous);
        if (oldCounter != null) oldCounter.remove(entity.getType());
        counters.computeIfAbsent(next, ignored -> new ChunkCounter()).add(entity.getType());
    }

    private boolean enabled() { return plugin.config().enabled(CheckType.ENTITIES); }
    private ChunkKey key(Entity entity) { return ChunkKey.of(entity.getLocation()); }
    private ChunkKey key(Chunk chunk) { return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()); }

    private static final class ChunkCounter {
        private int total;
        private final Map<EntityType, Integer> types = new EnumMap<>(EntityType.class);
        private synchronized boolean canAdd(EntityType type, int maxTotal, int maxType) {
            return total < maxTotal && types.getOrDefault(type, 0) < maxType;
        }
        private synchronized void add(EntityType type) {
            total++;
            types.merge(type, 1, Integer::sum);
        }
        private synchronized void remove(EntityType type) {
            if (total > 0) total--;
            types.computeIfPresent(type, (ignored, count) -> count <= 1 ? null : count - 1);
        }
    }
}
