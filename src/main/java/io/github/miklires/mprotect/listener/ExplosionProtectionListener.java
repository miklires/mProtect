package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ChunkKey;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.time.Duration;
import java.util.List;

public final class ExplosionProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final KeyedRateLimiter<ChunkKey> explosions = new KeyedRateLimiter<>(16_384);
    private final KeyedRateLimiter<ChunkKey> tnt = new KeyedRateLimiter<>(16_384);

    public ExplosionProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrime(TNTPrimeEvent event) {
        if (!enabled() || allow(tnt, ChunkKey.of(event.getBlock()), "explosions.tnt-prime")) return;
        event.setCancelled(true);
        report("TNT prime rate exceeded", event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        protect(event.getLocation(), event.blockList(), event.getYield(), event::setYield, event::setCancelled);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        protect(event.getBlock().getLocation(), event.blockList(), event.getYield(), event::setYield, event::setCancelled);
    }

    private void protect(Location location, List<Block> blocks, float yield,
                           java.util.function.Consumer<Float> setYield,
                           java.util.function.Consumer<Boolean> cancel) {
        if (!enabled()) return;
        if (!allow(explosions, ChunkKey.of(location), "explosions")) {
            cancel.accept(true);
            report("explosion rate exceeded", location);
            return;
        }
        int maximum = plugin.config().integer("explosions.max-affected-blocks", 256);
        if (blocks.size() > maximum) {
            int original = blocks.size();
            blocks.subList(maximum, original).clear();
            report("explosion affected blocks limited from " + original + " to " + maximum, location);
        }
        float maximumYield = (float) plugin.config().decimal("explosions.max-yield", 0.3);
        if (yield > maximumYield) setYield.accept(maximumYield);
    }

    private boolean enabled() { return plugin.config().enabled(CheckType.EXPLOSIONS); }

    private boolean allow(KeyedRateLimiter<ChunkKey> limiter, ChunkKey key, String section) {
        return limiter.allow(key, plugin.config().integer(section + ".events", 12),
                Duration.ofSeconds(plugin.config().integer(section + ".window-seconds", 2)), System.nanoTime());
    }

    private void report(String detail, Location location) {
        plugin.violations().recordSystem(CheckType.EXPLOSIONS, detail, location);
    }
}
