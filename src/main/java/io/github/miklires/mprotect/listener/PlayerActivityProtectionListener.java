package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.KeyedRateLimiter;
import io.github.miklires.mprotect.model.CheckType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.time.Duration;
import java.util.UUID;

public final class PlayerActivityProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final KeyedRateLimiter<ActionKey> rates = new KeyedRateLimiter<>(16_384);

    public PlayerActivityProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) protect(player, event, "inventory-clicks");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) protect(player, event, "inventory-clicks");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        protect(event.getPlayer(), event, "interactions");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { protect(event.getPlayer(), event, "block-changes"); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) { protect(event.getPlayer(), event, "block-changes"); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) { protect(event.getPlayer(), event, "item-drops"); }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player player) protect(player, event, "projectiles");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        for (String action : new String[]{"inventory-clicks", "interactions", "block-changes", "item-drops", "projectiles"})
            rates.remove(new ActionKey(player, action));
    }

    private void protect(Player player, Cancellable event, String action) {
        if (!plugin.config().enabled(CheckType.ACTIVITY) || player.hasPermission("mprotect.bypass.activity")) return;
        String section = "activity." + action;
        int events = plugin.config().integer(section + ".events", 80);
        int seconds = plugin.config().integer(section + ".window-seconds", 2);
        if (rates.allow(new ActionKey(player.getUniqueId(), action), events, Duration.ofSeconds(seconds), System.nanoTime())) return;
        event.setCancelled(true);
        plugin.violations().record(player, CheckType.ACTIVITY, action + " rate exceeded");
        plugin.messages().send(player, "activity-rate");
    }

    private record ActionKey(UUID player, String action) {}
}
