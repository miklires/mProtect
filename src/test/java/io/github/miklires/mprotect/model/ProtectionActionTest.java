package io.github.miklires.mprotect.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectionActionTest {
    @Test
    void parsesWithoutCaseSensitivityAndFallsBack() {
        assertEquals(ProtectionAction.KICK, ProtectionAction.parse("kick", ProtectionAction.LOG));
        assertEquals(ProtectionAction.REMOVE, ProtectionAction.parse("unknown", ProtectionAction.REMOVE));
    }
}
