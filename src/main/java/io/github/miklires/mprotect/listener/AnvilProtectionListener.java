package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.model.CheckType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AnvilProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    public AnvilProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrepare(PrepareAnvilEvent event) {
        if (!plugin.config().enabled(CheckType.ANVILS) || !(event.getView().getPlayer() instanceof Player player)
                || player.hasPermission("mprotect.bypass.anvils")) return;
        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta()) return;
        ItemMeta meta = result.getItemMeta();
        Component displayName = meta.displayName();
        int maxCost = plugin.config().integer("anvils.max-repair-cost", 1000);
        if (event.getView().getRepairCost() > maxCost) {
            event.setResult(null);
            plugin.violations().record(player, CheckType.ANVILS, "repair cost " + event.getView().getRepairCost() + " > " + maxCost);
            plugin.messages().send(player, "blocked");
            return;
        }
        if (displayName == null) return;
        String name = plain.serialize(displayName);
        int maxName = plugin.config().integer("anvils.max-name-characters", 64);
        if (name.length() > maxName) {
            event.setResult(null);
            plugin.violations().record(player, CheckType.ANVILS, "name length " + name.length() + " > " + maxName);
            plugin.messages().send(player, "blocked");
            return;
        }
        if (plugin.config().bool("anvils.strip-formatting", true)) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            result.setItemMeta(meta);
            event.setResult(result);
        }
    }
}
