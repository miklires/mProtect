package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.model.CheckType;
import io.github.miklires.mprotect.model.ProtectionAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BookProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();

    public BookProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEdit(PlayerEditBookEvent event) {
        if (!plugin.config().enabled(CheckType.BOOKS) || event.getPlayer().hasPermission("mprotect.bypass.books")) return;
        BookMeta meta = event.getNewBookMeta();
        String violation = violation(meta);
        if (violation != null) {
            block(event, violation);
            return;
        }
        if (plugin.config().bool("books.strip-formatting", false)) {
            List<Component> stripped = meta.pages().stream().map(page -> (Component) Component.text(plain.serialize(page))).toList();
            meta.pages(stripped);
            event.setNewBookMeta(meta);
        }
    }

    private String violation(BookMeta meta) {
        int maxPages = plugin.config().integer("books.max-pages", 50);
        if (meta.pages().size() > maxPages) return "page count " + meta.pages().size() + " > " + maxPages;
        int maxPage = plugin.config().integer("books.max-page-characters", 1024);
        int maxTotal = plugin.config().integer("books.max-total-characters", 20_000);
        int maxJson = plugin.config().integer("books.max-component-json-characters", 32_768);
        int total = 0;
        for (Component page : meta.pages()) {
            int length = plain.serialize(page).length();
            total += length;
            if (length > maxPage) return "page length " + length + " > " + maxPage;
            if (gson.serialize(page).length() > maxJson) return "component JSON exceeds " + maxJson;
        }
        if (total > maxTotal) return "total length " + total + " > " + maxTotal;
        String title = meta.getTitle();
        int maxTitle = plugin.config().integer("books.max-title-characters", 32);
        if (title != null && title.length() > maxTitle) return "title length " + title.length() + " > " + maxTitle;
        return null;
    }

    private void block(PlayerEditBookEvent event, String detail) {
        Player player = event.getPlayer();
        plugin.violations().record(player, CheckType.BOOKS, detail);
        ProtectionAction action = plugin.config().bookAction();
        if (action != ProtectionAction.LOG) event.setCancelled(true);
        if (action == ProtectionAction.KICK) player.kick(plugin.messages().component("kick", Map.of("check", CheckType.BOOKS.key())));
        plugin.messages().send(player, "blocked");
    }
}
