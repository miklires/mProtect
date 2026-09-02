package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.RateWindow;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final Map<UUID, RateState> rates = new ConcurrentHashMap<>();
    private final KeyedRateLimiter<UUID> notices = new KeyedRateLimiter<>(4096);

    public CommandProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.config().enabled(CheckType.COMMANDS) || event.getPlayer().hasPermission("mprotect.bypass.commands")) return;
        String input = event.getMessage();
        int maxLength = plugin.config().integer("commands.max-length", 512);
        if (input.length() > maxLength) {
            block(event, "command length " + input.length() + " > " + maxLength, "command-blocked");
            return;
        }
        int limit = plugin.config().integer("commands.rate-limit.commands", 12);
        int seconds = plugin.config().integer("commands.rate-limit.window-seconds", 3);
        long revision = plugin.config().revision();
        RateState state = rates.compute(event.getPlayer().getUniqueId(), (ignored, current) ->
                current == null || current.revision() != revision
                        ? new RateState(revision, new RateWindow(limit, Duration.ofSeconds(seconds))) : current);
        if (!state.window().allow(System.nanoTime())) {
            block(event, "command rate exceeded", "command-rate");
            return;
        }
        String raw = input.substring(1).trim();
        if (raw.isEmpty()) return;
        String root = raw.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int colon = root.indexOf(':');
        String namespace = colon > 0 ? root.substring(0, colon) : "";
        String command = colon >= 0 ? root.substring(colon + 1) : root;
        if (!namespace.isEmpty() && plugin.config().blockedNamespaces().contains(namespace)) {
            block(event, "blocked namespace " + namespace, "command-blocked");
        } else if (plugin.config().blockedCommands().contains(command) || plugin.config().blockedRoots().contains(command)) {
            block(event, "blocked command " + command, "command-blocked");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        rates.remove(event.getPlayer().getUniqueId());
        notices.remove(event.getPlayer().getUniqueId());
    }

    private void block(PlayerCommandPreprocessEvent event, String detail, String message) {
        event.setCancelled(true);
        if (notices.allow(event.getPlayer().getUniqueId(), 1, Duration.ofSeconds(1), System.nanoTime())) {
            plugin.violations().record(event.getPlayer(), CheckType.COMMANDS, detail);
            plugin.messages().send(event.getPlayer(), message);
        }
    }

    private record RateState(long revision, RateWindow window) {}
}
