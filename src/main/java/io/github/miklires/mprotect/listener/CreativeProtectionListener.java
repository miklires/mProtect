package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CreativeProtectionListener implements Listener {
    private final MProtectPlugin plugin;

    public CreativeProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent event) {
        if (!enabled() || allowed(event.getPlayer()) || !restricted(event.getNewGameMode())) return;
        event.setCancelled(true);
        plugin.violations().record(event.getPlayer(), CheckType.CREATIVE, "unauthorized " + event.getNewGameMode().name().toLowerCase());
        plugin.messages().send(event.getPlayer(), "blocked");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!enabled() || allowed(player) || !restricted(player.getGameMode())
                || !plugin.config().bool("creative.return-to-survival-on-join", true)) return;
        plugin.scheduler().player(player, () -> {
            if (player.isOnline() && restricted(player.getGameMode()) && !allowed(player)) {
                player.setGameMode(GameMode.SURVIVAL);
                plugin.violations().record(player, CheckType.CREATIVE, "unauthorized game mode restored on join");
            }
        });
    }

    private boolean enabled() { return plugin.config().enabled(CheckType.CREATIVE); }
    private boolean allowed(Player player) { return player.hasPermission(plugin.config().text("creative.allowed-permission", "mprotect.bypass.creative")); }
    private boolean restricted(GameMode mode) { return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR; }
}
