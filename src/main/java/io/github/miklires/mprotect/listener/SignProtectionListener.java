package io.github.miklires.mprotect.listener;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ComponentSanitizer;
import io.github.miklires.mprotect.model.CheckType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public final class SignProtectionListener implements Listener {
    private final MProtectPlugin plugin;
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    public SignProtectionListener(MProtectPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChange(SignChangeEvent event) {
        if (!plugin.config().enabled(CheckType.SIGNS) || event.getPlayer().hasPermission("mprotect.bypass.signs")) return;
        int maxLine = plugin.config().integer("signs.max-line-characters", 384);
        int maxTotal = plugin.config().integer("signs.max-total-characters", 1024);
        int total = 0;
        for (int index = 0; index < event.lines().size(); index++) {
            Component line = event.line(index);
            if (line == null) continue;
            int length = plain.serialize(line).length();
            total += length;
            if (length > maxLine || total > maxTotal) {
                event.setCancelled(true);
                plugin.violations().record(event.getPlayer(), CheckType.SIGNS,
                        length > maxLine ? "line length " + length + " > " + maxLine : "total length " + total + " > " + maxTotal);
                plugin.messages().send(event.getPlayer(), "blocked");
                return;
            }
            if (plugin.config().bool("signs.strip-click-events", true) && ComponentSanitizer.hasClickEvent(line)) {
                event.line(index, ComponentSanitizer.removeClickEvents(line));
                plugin.violations().record(event.getPlayer(), CheckType.SIGNS, "click event removed");
            }
        }
    }
}
