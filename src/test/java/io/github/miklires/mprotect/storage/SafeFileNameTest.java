package io.github.miklires.mprotect.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeFileNameTest {
    @Test void acceptsSimpleLocalNames() {
        assertEquals("security_log-2", SafeFileName.storage("security_log-2"));
    }

    @Test void rejectsTraversalAndSpecialPaths() {
        assertEquals("violations", SafeFileName.storage(".."));
        assertEquals("violations", SafeFileName.storage("../outside"));
        assertEquals("violations", SafeFileName.storage("C:drive"));
    }
}
