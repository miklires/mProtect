package io.github.miklires.mprotect.check;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyedRateLimiterTest {
    @Test
    void limitsEachKeyAndResetsAfterWindow() {
        KeyedRateLimiter<String> limiter = new KeyedRateLimiter<>(8);
        assertTrue(limiter.allow("chunk-a", 2, Duration.ofSeconds(1), 0));
        assertTrue(limiter.allow("chunk-a", 2, Duration.ofSeconds(1), 1));
        assertFalse(limiter.allow("chunk-a", 2, Duration.ofSeconds(1), 2));
        assertTrue(limiter.allow("chunk-b", 2, Duration.ofSeconds(1), 2));
        assertTrue(limiter.allow("chunk-a", 2, Duration.ofSeconds(1), Duration.ofSeconds(1).toNanos()));
    }

    @Test
    void neverGrowsPastConfiguredKeyLimit() {
        KeyedRateLimiter<Integer> limiter = new KeyedRateLimiter<>(2);
        assertTrue(limiter.allow(1, 1, Duration.ofSeconds(10), 0));
        assertTrue(limiter.allow(2, 1, Duration.ofSeconds(10), 0));
        assertFalse(limiter.allow(3, 1, Duration.ofSeconds(10), 0));
        assertEquals(2, limiter.size());
        assertTrue(limiter.allow(3, 1, Duration.ofSeconds(10), Duration.ofSeconds(10).toNanos()));
        assertEquals(1, limiter.size());
    }
}
