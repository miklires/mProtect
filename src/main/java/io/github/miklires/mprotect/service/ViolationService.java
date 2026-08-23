package io.github.miklires.mprotect.service;

import io.github.miklires.mprotect.MProtectPlugin;
import io.github.miklires.mprotect.alert.DiscordNotifier;
import io.github.miklires.mprotect.model.CheckType;
import io.github.miklires.mprotect.model.ViolationRecord;
import io.github.miklires.mprotect.storage.ViolationStore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class ViolationService {
    private final MProtectPlugin plugin;
    private final ViolationStore store;
    private final DiscordNotifier discord;
    private final Map<CheckType, LongAdder> counters = new EnumMap<>(CheckType.class);
    private final ConcurrentHashMap<AlertKey, AlertBucket> alerts = new ConcurrentHashMap<>();
    private volatile LocalDate counterDate = LocalDate.now(ZoneOffset.UTC);

    public ViolationService(MProtectPlugin plugin, ViolationStore store) {
        this.plugin = plugin;
        this.store = store;
        this.discord = new DiscordNotifier(plugin);
        for (CheckType type : CheckType.values()) counters.put(type, new LongAdder());
    }

    public void record(Player player, CheckType type, String unsafeDetail) {
        rotateCounters();
        counters.get(type).increment();
        String detail = sanitize(unsafeDetail);
        Location location = player.getLocation();
        ViolationRecord record = new ViolationRecord(0, Instant.now(), player.getUniqueId(), player.getName(), type, detail,
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        store.save(record);
        enqueueAlert(record);
    }

    public void recordSystem(CheckType type, String unsafeDetail, Location location) {
        rotateCounters();
        counters.get(type).increment();
        ViolationRecord record = new ViolationRecord(0, Instant.now(), new UUID(0, 0), "server", type, sanitize(unsafeDetail),
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        store.save(record);
        enqueueAlert(record);
    }

    public long today(CheckType type) {
        rotateCounters();
        return counters.get(type).sum();
    }

    public long todayTotal() {
        rotateCounters();
        return counters.values().stream().mapToLong(LongAdder::sum).sum();
    }

    private void enqueueAlert(ViolationRecord record) {
        AlertKey key = new AlertKey(record.playerId(), record.check(), record.detail());
        AlertBucket bucket = alerts.computeIfAbsent(key, ignored -> new AlertBucket(record));
        bucket.count.incrementAndGet();
        if (bucket.scheduled.compareAndSet(false, true)) {
            int seconds = plugin.config().integer("alerts.deduplication-seconds", 3);
            plugin.scheduler().delayedAsync(() -> flush(key, bucket), Duration.ofSeconds(seconds));
        }
    }

    private void flush(AlertKey key, AlertBucket bucket) {
        alerts.remove(key, bucket);
        int count = bucket.count.get();
        ViolationRecord record = bucket.record;
        if (plugin.config().bool("alerts.staff-chat", true)) {
            String permission = plugin.config().text("alerts.permission", "mprotect.alerts");
            plugin.scheduler().global(() -> {
                for (Player staff : plugin.getServer().getOnlinePlayers()) {
                    if (staff.hasPermission(permission)) plugin.scheduler().player(staff, () -> plugin.messages().send(staff, "alert", Map.of(
                            "player", record.playerName(), "check", record.check().key(), "detail", record.detail(), "count", count)));
                }
            });
        }
        discord.send(record, count);
    }

    private synchronized void rotateCounters() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (today.equals(counterDate)) return;
        counters.values().forEach(LongAdder::reset);
        counterDate = today;
    }

    private String sanitize(String detail) {
        String normalized = detail.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }

    private record AlertKey(UUID playerId, CheckType check, String detail) {}
    private static final class AlertBucket {
        private final ViolationRecord record;
        private final AtomicInteger count = new AtomicInteger();
        private final AtomicBoolean scheduled = new AtomicBoolean();
        private AlertBucket(ViolationRecord record) { this.record = record; }
    }
}
