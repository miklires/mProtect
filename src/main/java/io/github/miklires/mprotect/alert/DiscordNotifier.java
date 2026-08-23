package io.github.miklires.mprotect.alert;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.model.ViolationRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class DiscordNotifier {
    private final MProtectPlugin plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public DiscordNotifier(MProtectPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(ViolationRecord record, int count) {
        if (!plugin.config().bool("alerts.discord.enabled", false)) return;
        String configured = plugin.config().text("alerts.discord.webhook-url", "").trim();
        if (configured.isEmpty()) return;
        URI uri;
        try {
            uri = URI.create(configured);
            if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("HTTPS is required");
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid alerts.discord.webhook-url; Discord alerts are disabled");
            return;
        }
        String text = "mProtect: " + record.playerName() + " triggered " + record.check().key() + ": " + record.detail() + " (x" + count + ")";
        String body = "{\"content\":\"" + escape(text) + "\",\"allowed_mentions\":{\"parse\":[]}}";
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json").header("User-Agent", "mProtect-plugin")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                plugin.getLogger().fine("Discord alert failed with HTTP " + response.statusCode());
            }
        }).exceptionally(exception -> {
            plugin.getLogger().fine("Discord alert failed: " + exception.getMessage());
            return null;
        });
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }
}
