package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ItemValidator;
import io.github.miklires.mprotect.model.CheckType;
import io.github.miklires.mprotect.service.ItemActionHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;

public final class ItemProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final ItemValidator validator;
    private final ItemActionHandler actions;

    public ItemProtectionListener(MProtectPlugin plugin) {
        this.plugin = plugin;
        this.validator = new ItemValidator(plugin);
        this.actions = new ItemActionHandler(plugin);
    }

    public void startFallbackScan() {
        long minutes = plugin.config().integer("items.fallback-scan-minutes", 5);
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, ignored -> plugin.scheduler().global(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) plugin.scheduler().player(player, () -> scan(player, player.getInventory()));
        }), minutes, minutes, TimeUnit.MINUTES);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!enabled() || !(event.getEntity() instanceof Player player) || bypass(player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (actions.apply(player, item, validator.validate(item), replacement -> {
            if (replacement == null) event.getItem().remove();
            else event.getItem().setItemStack(replacement);
        })) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled() || bypass(event.getPlayer())) return;
        ItemStack item = event.getItemDrop().getItemStack();
        if (actions.apply(event.getPlayer(), item, validator.validate(item), replacement -> {
            if (replacement == null) event.getItemDrop().remove();
            else event.getItemDrop().setItemStack(replacement);
        })) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!enabled() || !(event.getWhoClicked() instanceof Player player) || bypass(player)) return;
        ItemStack current = event.getCurrentItem();
        if (current != null && actions.apply(player, current, validator.validate(current), event::setCurrentItem)) event.setCancelled(true);
        ItemStack cursor = event.getCursor();
        if (!cursor.getType().isAir() && actions.apply(player, cursor, validator.validate(cursor), event.getView()::setCursor)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!enabled() || !(event.getWhoClicked() instanceof Player player) || bypass(player)) return;
        for (ItemStack item : event.getNewItems().values()) {
            var result = validator.validate(item);
            if (!result.safe()) {
                plugin.violations().record(player, CheckType.ITEMS, result.detail());
                event.setCancelled(true);
                plugin.messages().send(player, "blocked");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCraft(PrepareItemCraftEvent event) {
        if (!enabled() || !(event.getView().getPlayer() instanceof Player player) || bypass(player)) return;
        ItemStack result = event.getInventory().getResult();
        var validation = validator.validate(result);
        if (!validation.safe()) {
            plugin.violations().record(player, CheckType.ITEMS, validation.detail());
            event.getInventory().setResult(null);
            plugin.messages().send(player, "blocked");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (enabled() && event.getPlayer() instanceof Player player && !bypass(player)) scan(player, event.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (enabled() && !bypass(event.getPlayer())) plugin.scheduler().player(event.getPlayer(), () -> scan(event.getPlayer(), event.getPlayer().getInventory()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled() || bypass(event.getPlayer()) || event.getItem() == null || event.getHand() == null) return;
        EquipmentSlot hand = event.getHand();
        if (actions.apply(event.getPlayer(), event.getItem(), validator.validate(event.getItem()), replacement ->
                event.getPlayer().getInventory().setItem(hand, replacement))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!enabled() || bypass(event.getPlayer())) return;
        EquipmentSlot hand = event.getHand();
        if (actions.apply(event.getPlayer(), event.getItemInHand(), validator.validate(event.getItemInHand()), replacement ->
                event.getPlayer().getInventory().setItem(hand, replacement))) event.setCancelled(true);
    }

    private void scan(Player player, Inventory inventory) {
        if (!enabled() || bypass(player)) return;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int currentSlot = slot;
            ItemStack item = inventory.getItem(slot);
            if (item != null) actions.apply(player, item, validator.validate(item), replacement -> inventory.setItem(currentSlot, replacement));
        }
    }

    private boolean enabled() { return plugin.config().enabled(CheckType.ITEMS); }
    private boolean bypass(Player player) { return player.hasPermission("mprotect.bypass.items"); }
}
