package io.github.miklires.mprotect.config;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.model.CheckType;
import io.github.miklires.mprotect.model.ProtectionAction;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigManager {
    private static final int VERSION = 2;
    private final MProtectPlugin plugin;
    private Set<Material> blockedMaterials = Set.of();
    private Set<String> blockedCommands = Set.of();
    private Set<String> blockedNamespaces = Set.of();
    private Set<String> blockedRoots = Set.of();
    private long revision;

    public ConfigManager(MProtectPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);
        int previousVersion = config.getInt("config-version", 0);
        if (previousVersion < 2) {
            List<String> namespaces = new java.util.ArrayList<>(config.getStringList("commands.blocked-namespaces"));
            namespaces.removeIf(value -> value.equalsIgnoreCase("minecraft"));
            config.set("commands.blocked-namespaces", namespaces);
        }
        if (config.getInt("config-version", 0) < VERSION) config.set("config-version", VERSION);
        blockedMaterials = materials(config.getStringList("items.blocked-materials"));
        blockedCommands = lower(config.getStringList("commands.blocked"));
        blockedNamespaces = lower(config.getStringList("commands.blocked-namespaces"));
        blockedRoots = lower(config.getStringList("commands.blocked-roots"));
        validate(config);
        plugin.saveConfig();
        revision++;
    }

    private void validate(FileConfiguration config) {
        integer(config, "items.fallback-scan-minutes", 1, 1440, 5);
        integer(config, "items.components.max-serialized-bytes", 1024, 1_048_576, 65_536);
        integer(config, "items.components.max-container-depth", 0, 8, 2);
        integer(config, "items.components.max-container-items", 1, 100_000, 1728);
        integer(config, "items.text.max-name-characters", 1, 1024, 128);
        integer(config, "items.text.max-lore-lines", 0, 256, 64);
        integer(config, "items.text.max-lore-line-characters", 1, 8192, 512);
        integer(config, "items.text.max-lore-total-characters", 1, 131_072, 4096);
        integer(config, "items.potions.max-amplifier", 0, 255, 4);
        integer(config, "items.potions.max-duration-ticks", 1, 2_000_000, 72_000);
        integer(config, "items.fireworks.max-power", 0, 127, 3);
        integer(config, "items.fireworks.max-effects", 0, 128, 8);
        decimal(config, "items.attributes.max-absolute-amount", 0.0, 1_000_000.0, 2048.0);
        integer(config, "books.max-pages", 1, 100, 50);
        integer(config, "books.max-page-characters", 1, 8192, 1024);
        integer(config, "books.max-total-characters", 1, 1_000_000, 20_000);
        integer(config, "books.max-title-characters", 1, 128, 32);
        integer(config, "books.max-author-characters", 1, 64, 16);
        integer(config, "books.max-component-json-characters", 256, 1_000_000, 32_768);
        integer(config, "signs.max-line-characters", 1, 8192, 384);
        integer(config, "signs.max-total-characters", 1, 32_768, 1024);
        integer(config, "anvils.max-name-characters", 1, 1024, 64);
        integer(config, "anvils.max-repair-cost", 0, 1_000_000, 1000);
        integer(config, "commands.max-length", 16, 32_768, 512);
        integer(config, "commands.rate-limit.commands", 1, 1000, 12);
        integer(config, "commands.rate-limit.window-seconds", 1, 300, 3);
        integer(config, "entities.max-per-chunk", 1, 100_000, 120);
        integer(config, "entities.max-per-type-per-chunk", 1, 100_000, 40);
        integer(config, "chunk-loads.max-new-chunks", 1, 10_000, 24);
        integer(config, "chunk-loads.window-seconds", 1, 300, 5);
        integer(config, "alerts.deduplication-seconds", 1, 300, 3);
        integer(config, "alerts.max-pending-buckets", 64, 100_000, 2048);
        integer(config, "storage.retention-days", 1, 3650, 30);
        String storageFile = config.getString("storage.file", "violations");
        if (!io.github.miklires.mprotect.storage.SafeFileName.storage(storageFile).equals(storageFile))
            invalid(config, "storage.file", "violations");
        action(config, "items.action", ProtectionAction.REMOVE);
        action(config, "books.action", ProtectionAction.REMOVE);
        String replacement = config.getString("items.replacement", "AIR");
        if (Material.matchMaterial(replacement) == null) invalid(config, "items.replacement", "AIR");
        String language = config.getString("language.default", "en_US");
        if (!Set.of("en_US", "ru_RU").contains(language)) invalid(config, "language.default", "en_US");
        String permission = config.getString("alerts.permission", "mprotect.alerts");
        if (permission == null || !permission.matches("[a-z0-9._-]{1,128}"))
            invalid(config, "alerts.permission", "mprotect.alerts");
    }

    private void action(FileConfiguration config, String path, ProtectionAction fallback) {
        String value = config.getString(path, fallback.name());
        if (ProtectionAction.parse(value, null) == null) invalid(config, path, fallback.name());
    }

    private void integer(FileConfiguration config, String path, int min, int max, int fallback) {
        int value = config.getInt(path, fallback);
        if (value < min || value > max) invalid(config, path, fallback);
    }

    private void decimal(FileConfiguration config, String path, double min, double max, double fallback) {
        double value = config.getDouble(path, fallback);
        if (!Double.isFinite(value) || value < min || value > max) invalid(config, path, fallback);
    }

    private void invalid(FileConfiguration config, String path, Object fallback) {
        plugin.getLogger().warning("Invalid config value at " + path + "; using " + fallback);
        config.set(path, fallback);
    }

    private Set<Material> materials(List<String> values) {
        Set<Material> parsed = EnumSet.noneOf(Material.class);
        for (String value : values) {
            Material material = Material.matchMaterial(value);
            if (material == null) plugin.getLogger().warning("Unknown material at items.blocked-materials: " + value);
            else parsed.add(material);
        }
        return Set.copyOf(parsed);
    }

    private Set<String> lower(List<String> values) {
        Set<String> parsed = new HashSet<>();
        for (String value : values) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) parsed.add(normalized);
        }
        return Set.copyOf(parsed);
    }

    public boolean enabled(CheckType type) {
        String section = switch (type) {
            case ITEMS, BOOKS, SIGNS, ANVILS, COMMANDS, CREATIVE, ENTITIES -> type.key();
            case CHUNKS -> "chunk-loads";
        };
        return plugin.getConfig().getBoolean(section + ".enabled", true);
    }
    public Set<Material> blockedMaterials() { return blockedMaterials; }
    public Set<String> blockedCommands() { return blockedCommands; }
    public Set<String> blockedNamespaces() { return blockedNamespaces; }
    public Set<String> blockedRoots() { return blockedRoots; }
    public ProtectionAction itemAction() { return ProtectionAction.parse(text("items.action", "REMOVE"), ProtectionAction.REMOVE); }
    public ProtectionAction bookAction() { return ProtectionAction.parse(text("books.action", "REMOVE"), ProtectionAction.REMOVE); }
    public Material replacement() { return Material.matchMaterial(text("items.replacement", "AIR")); }
    public String language() { return text("language.default", "en_US"); }
    public boolean bool(String path, boolean fallback) { return plugin.getConfig().getBoolean(path, fallback); }
    public int integer(String path, int fallback) { return plugin.getConfig().getInt(path, fallback); }
    public double decimal(String path, double fallback) { return plugin.getConfig().getDouble(path, fallback); }
    public String text(String path, String fallback) {
        String value = plugin.getConfig().getString(path, fallback);
        return value == null ? fallback : value;
    }
    public long revision() { return revision; }
}
