package io.github.miklires.mprotect.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {
    @Test
    void parsesCoreAndPrereleaseVersions() {
        assertEquals(new SemanticVersion(1, 2, 3, false), SemanticVersion.parse("1.2.3"));
        assertEquals(new SemanticVersion(2, 0, 0, true), SemanticVersion.parse("2.0.0-beta.1+build"));
    }

    @Test
    void stableVersionWinsOverPrereleaseWithSameCore() {
        assertTrue(SemanticVersion.parse("1.0.0").compareTo(SemanticVersion.parse("1.0.0-beta.1")) > 0);
        assertTrue(SemanticVersion.parse("1.1.0").compareTo(SemanticVersion.parse("1.0.9")) > 0);
    }
}
