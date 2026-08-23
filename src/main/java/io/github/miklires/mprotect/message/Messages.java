package io.github.miklires.mprotect.message;

import io.github.miklires.mprotect.MProtectPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class Messages {
    private final MProtectPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration values;

    public Messages(MProtectPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String language = plugin.config().language();
        String resource = "lang/" + language + ".yml";
        File directory = new File(plugin.getDataFolder(), "lang");
        if (!directory.exists() && !directory.mkdirs()) plugin.getLogger().warning("Could not create lang directory");
        File file = new File(directory, language + ".yml");
        if (!file.exists()) plugin.saveResource(resource, false);
        values = YamlConfiguration.loadConfiguration(file);
        var defaults = plugin.getResource(resource);
        if (defaults != null) {
            values.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            values.options().copyDefaults(true);
            try { values.save(file); } catch (Exception exception) {
                plugin.getLogger().warning("Could not save language defaults: " + exception.getMessage());
            }
        }
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, ?> replacements) {
        sender.sendMessage(component(key, replacements));
    }

    public Component component(String key, Map<String, ?> replacements) {
        return miniMessage.deserialize(render(key, replacements));
    }

    public String render(String key, Map<String, ?> replacements) {
        String message = values.getString(key, key);
        for (var entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", miniMessage.escapeTags(String.valueOf(entry.getValue())));
        }
        return values.getString("prefix", "") + message;
    }
}
