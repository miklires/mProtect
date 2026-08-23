package io.github.miklires.mprotect.model;

import java.util.Locale;
import java.util.Optional;

public enum CheckType {
    ITEMS, BOOKS, SIGNS, ANVILS, COMMANDS, CREATIVE, ENTITIES, CHUNKS;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<CheckType> parse(String value) {
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
