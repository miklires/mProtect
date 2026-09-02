package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.RateWindow;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkLoadProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final Map<UUID, RateState> rates = new ConcurrentHashMap<>();
    private final KeyedRateLimiter<UUID> notices = new KeyedRateLimiter<>(4096);

    public ChunkLoadProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.config().enabled(CheckType.CHUNKS) || event.getPlayer().hasPermission("mprotect.bypass.chunks")) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;
        if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4) && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)) return;
        int limit = plugin.config().integer("chunk-loads.max-new-chunks", 24);
        int seconds = plugin.config().integer("chunk-loads.window-seconds", 5);
        long revision = plugin.config().revision();
        RateState state = rates.compute(event.getPlayer().getUniqueId(), (ignored, current) ->
                current == null || current.revision() != revision
                        ? new RateState(revision, new RateWindow(limit, Duration.ofSeconds(seconds))) : current);
        if (!state.window().allow(System.nanoTime())) {
            event.setCancelled(true);
            if (notices.allow(event.getPlayer().getUniqueId(), 1, Duration.ofSeconds(1), System.nanoTime())) {
                plugin.violations().record(event.getPlayer(), CheckType.CHUNKS, "chunk movement rate exceeded " + limit + " per " + seconds + "s");
                plugin.messages().send(event.getPlayer(), "blocked");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rates.remove(event.getPlayer().getUniqueId());
        notices.remove(event.getPlayer().getUniqueId());
    }

    private record RateState(long revision, RateWindow window) {}
}
