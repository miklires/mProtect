package io.github.miklires.mprotect.command;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.model.CheckType;
import io.github.miklires.mprotect.model.ViolationRecord;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProtectCommand implements BasicCommand {
    private static final java.util.regex.Pattern PLAYER_NAME = java.util.regex.Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final MProtectPlugin plugin;

    public ProtectCommand(MProtectPlugin plugin) { this.plugin = plugin; }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();
        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "status" -> status(sender);
            case "violations" -> violations(sender, args.length > 1 ? args[1] : null);
            case "test" -> test(sender, args.length > 1 ? args[1] : "");
            case "reload" -> reload(sender);
            default -> plugin.messages().send(sender, "usage");
        }
    }

    private void status(CommandSender sender) {
        if (!allowed(sender, "mprotect.command.status")) return;
        plugin.messages().send(sender, "status-header", Map.of("version", plugin.getPluginMeta().getVersion()));
        for (CheckType type : CheckType.values()) {
            plugin.messages().send(sender, "status-line", Map.of("check", type.key(),
                    "state", plugin.config().enabled(type) ? "ON" : "OFF", "count", plugin.violations().today(type)));
        }
        plugin.messages().send(sender, "status-total", Map.of("count", plugin.violations().todayTotal()));
    }

    private void violations(CommandSender sender, String playerName) {
        if (!allowed(sender, "mprotect.command.violations")) return;
        if (playerName != null && !PLAYER_NAME.matcher(playerName).matches()) {
            plugin.messages().send(sender, "invalid-player");
            return;
        }
        plugin.store().recent(playerName, 10).thenAccept(records -> reply(sender, () -> showViolations(sender, playerName, records)));
    }

    private void showViolations(CommandSender sender, String playerName, List<ViolationRecord> records) {
        if (records.isEmpty()) { plugin.messages().send(sender, "violations-empty"); return; }
        plugin.messages().send(sender, "violations-header", Map.of("filter", playerName == null ? "all" : playerName));
        for (ViolationRecord record : records) plugin.messages().send(sender, "violations-line", Map.of(
                "time", TIME.format(record.occurredAt()), "player", record.playerName(), "check", record.check().key(), "detail", record.detail()));
    }

    private void test(CommandSender sender, String input) {
        if (!allowed(sender, "mprotect.command.test")) return;
        CheckType.parse(input).ifPresentOrElse(type -> plugin.messages().send(sender,
                plugin.config().enabled(type) ? "test-ok" : "test-disabled", Map.of("check", type.key())),
                () -> plugin.messages().send(sender, "test-unknown"));
    }

    private void reload(CommandSender sender) {
        if (!allowed(sender, "mprotect.command.reload")) return;
        plugin.reloadSafeSettings();
        plugin.messages().send(sender, "reloaded");
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        plugin.messages().send(sender, "no-permission");
        return false;
    }

    private void reply(CommandSender sender, Runnable task) {
        if (sender instanceof Player player) plugin.scheduler().player(player, task);
        else plugin.scheduler().global(task);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options;
        if (args.length <= 1) options = List.of("status", "violations", "test", "reload");
        else if (args.length == 2 && args[0].equalsIgnoreCase("test")) options = java.util.Arrays.stream(CheckType.values()).map(CheckType::key).toList();
        else if (args.length == 2 && args[0].equalsIgnoreCase("violations")) options = plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        else options = List.of();
        return options.stream().filter(value -> value.startsWith(input)).toList();
    }
}
