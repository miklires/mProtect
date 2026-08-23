package io.github.miklires.mprotect.util;

import io.github.miklires.mprotect.MProtectPlugin;
import org.bukkit.entity.Player;
import org.bukkit.World;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class PluginScheduler {
    private final MProtectPlugin plugin;

    public PluginScheduler(MProtectPlugin plugin) {
        this.plugin = plugin;
    }

    public void global(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }

    public void player(Player player, Runnable task) {
        player.getScheduler().execute(plugin, task, null, 1L);
    }

    public void region(World world, int chunkX, int chunkZ, Runnable task) {
        plugin.getServer().getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
    }

    public void async(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> task.run());
    }

    public void delayedAsync(Runnable task, Duration delay) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, ignored -> task.run(), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void globalTimer(Runnable task, long delayTicks, long periodTicks) {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> task.run(), delayTicks, periodTicks);
    }
}
