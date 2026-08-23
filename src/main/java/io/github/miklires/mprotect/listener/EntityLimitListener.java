package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
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
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityLimitListener implements Listener {
    private final MProtectPlugin plugin;
    private final ConcurrentHashMap<ChunkKey, ChunkCounter> counters = new ConcurrentHashMap<>();

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
        if (!counter.canAdd(event.getEntityType(), maxTotal, maxType)) {
            event.setCancelled(true);
            plugin.violations().recordSystem(CheckType.ENTITIES,
                    "blocked " + event.getEntityType().name().toLowerCase() + " spawn at chunk limit", event.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterSpawn(EntitySpawnEvent event) {
        if (enabled()) counters.computeIfAbsent(key(event.getEntity()), ignored -> new ChunkCounter()).add(event.getEntityType());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        ChunkCounter counter = counters.get(key(event.getEntity()));
        if (counter != null) counter.remove(event.getEntityType());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoad(ChunkLoadEvent event) { if (enabled()) load(event.getChunk()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(ChunkUnloadEvent event) { counters.remove(key(event.getChunk())); }

    private void load(Chunk chunk) {
        ChunkCounter counter = new ChunkCounter();
        for (Entity entity : chunk.getEntities()) counter.add(entity.getType());
        counters.put(key(chunk), counter);
    }

    private boolean enabled() { return plugin.config().enabled(CheckType.ENTITIES); }
    private ChunkKey key(Entity entity) { return new ChunkKey(entity.getWorld().getUID(), entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4); }
    private ChunkKey key(Chunk chunk) { return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()); }

    private record ChunkKey(UUID world, int x, int z) {}

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
