package io.github.miklires.mprotect.model;

import java.time.Instant;
import java.util.UUID;

public record ViolationRecord(
        long id,
        Instant occurredAt,
        UUID playerId,
        String playerName,
        CheckType check,
        String detail,
        String world,
        int blockX,
        int blockY,
        int blockZ
) {}
