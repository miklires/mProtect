package io.github.miklires.mprotect.check;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public final class RateWindow {
    private final int limit;
    private final long windowNanos;
    private final Deque<Long> events = new ArrayDeque<>();

    public RateWindow(int limit, Duration window) {
        if (limit < 1 || window.isNegative() || window.isZero()) throw new IllegalArgumentException("Invalid rate window");
        this.limit = limit;
        this.windowNanos = window.toNanos();
    }

    public synchronized boolean allow(long nowNanos) {
        long cutoff = nowNanos - windowNanos;
        while (!events.isEmpty() && events.peekFirst() <= cutoff) events.removeFirst();
        if (events.size() >= limit) return false;
        events.addLast(nowNanos);
        return true;
    }
}
