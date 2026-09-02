package io.github.miklires.mprotect.check;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded fixed-window limiter for hot event paths. */
public final class KeyedRateLimiter<K> {
    private final Map<K, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maximumKeys;

    public KeyedRateLimiter(int maximumKeys) {
        if (maximumKeys < 1) throw new IllegalArgumentException("maximumKeys must be positive");
        this.maximumKeys = maximumKeys;
    }

    public boolean allow(K key, int limit, Duration window, long nowNanos) {
        if (key == null || limit < 1 || window.isNegative() || window.isZero()) return false;
        long windowNanos = window.toNanos();
        Bucket existing = buckets.get(key);
        if (existing != null) return existing.allow(limit, windowNanos, nowNanos);
        if (buckets.size() >= maximumKeys) removeExpired(nowNanos, windowNanos);
        if (buckets.size() >= maximumKeys) return false;
        Bucket created = new Bucket(nowNanos);
        Bucket winner = buckets.putIfAbsent(key, created);
        return (winner == null ? created : winner).allow(limit, windowNanos, nowNanos);
    }

    public void remove(K key) { buckets.remove(key); }
    public void clear() { buckets.clear(); }
    public int size() { return buckets.size(); }

    private void removeExpired(long nowNanos, long windowNanos) {
        buckets.entrySet().removeIf(entry -> entry.getValue().expired(nowNanos, windowNanos));
    }

    private static final class Bucket {
        private long startedAt;
        private long lastSeen;
        private int count;

        private Bucket(long nowNanos) {
            startedAt = nowNanos;
            lastSeen = nowNanos;
        }

        private synchronized boolean allow(int limit, long windowNanos, long nowNanos) {
            if (nowNanos < startedAt || nowNanos - startedAt >= windowNanos) {
                startedAt = nowNanos;
                count = 0;
            }
            lastSeen = nowNanos;
            if (count >= limit) return false;
            count++;
            return true;
        }

        private synchronized boolean expired(long nowNanos, long windowNanos) {
            return nowNanos < lastSeen || nowNanos - lastSeen >= windowNanos;
        }
    }
}
