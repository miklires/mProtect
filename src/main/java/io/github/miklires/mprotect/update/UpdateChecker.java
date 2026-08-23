package io.github.miklires.mprotect.update;

import io.github.miklires.mprotect.MProtectPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final Pattern VERSION = Pattern.compile("\\\"version_number\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern TYPE = Pattern.compile("\\\"version_type\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final MProtectPlugin plugin;

    public UpdateChecker(MProtectPlugin plugin) { this.plugin = plugin; }

    public void start() {
        if (!plugin.config().bool("updates.enabled", true)) return;
        String project = plugin.config().text("updates.modrinth-project-id", "").trim();
        if (project.isEmpty()) return;
        plugin.scheduler().async(() -> check(project));
    }

    private void check(String project) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            String filters = "?loaders=%5B%22paper%22%5D&game_versions=%5B%2226.2%22%5D";
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + project + "/version" + filters))
                    .timeout(Duration.ofSeconds(5)).header("User-Agent", "miklires/mProtect").build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            SemanticVersion current = SemanticVersion.parse(plugin.getPluginMeta().getVersion());
            String latest = null;
            for (String object : response.body().split("\\},\\s*\\{")) {
                Matcher version = VERSION.matcher(object);
                Matcher type = TYPE.matcher(object);
                if (!version.find() || !type.find() || !type.group(1).equals("release")) continue;
                if (latest == null || SemanticVersion.parse(version.group(1)).compareTo(SemanticVersion.parse(latest)) > 0) latest = version.group(1);
            }
            if (latest != null && SemanticVersion.parse(latest).compareTo(current) > 0) {
                plugin.getLogger().info("mProtect " + latest + " is available on Modrinth");
            }
        } catch (Exception exception) {
            plugin.getLogger().fine("Update check failed: " + exception.getMessage());
        }
    }
}
