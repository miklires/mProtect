package io.github.miklires.mprotect.check;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateWindowTest {
    @Test
    void rejectsEventsOverLimitUntilWindowExpires() {
        RateWindow window = new RateWindow(2, Duration.ofSeconds(3));
        assertTrue(window.allow(1_000_000_000L));
        assertTrue(window.allow(2_000_000_000L));
        assertFalse(window.allow(2_500_000_000L));
        assertTrue(window.allow(4_100_000_000L));
    }
}
