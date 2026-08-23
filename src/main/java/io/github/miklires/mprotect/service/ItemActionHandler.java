package io.github.miklires.mprotect.service;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.check.ValidationResult;
import io.github.miklires.mprotect.model.CheckType;
import io.github.miklires.mprotect.model.ProtectionAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public final class ItemActionHandler {
    private final MProtectPlugin plugin;

    public ItemActionHandler(MProtectPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean apply(Player player, ItemStack item, ValidationResult result, Consumer<ItemStack> replacement) {
        if (result.safe()) return false;
        plugin.violations().record(player, CheckType.ITEMS, result.detail());
        ProtectionAction action = plugin.config().itemAction();
        switch (action) {
            case LOG -> { return false; }
            case REMOVE -> replacement.accept(null);
            case REPLACE -> replacement.accept(new ItemStack(plugin.config().replacement()));
            case KICK -> {
                replacement.accept(null);
                player.kick(plugin.messages().component("kick", Map.of("check", CheckType.ITEMS.key())));
            }
        }
        plugin.messages().send(player, "blocked");
        return true;
    }
}
