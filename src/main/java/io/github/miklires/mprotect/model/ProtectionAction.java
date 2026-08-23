package io.github.miklires.mprotect.model;

import java.util.Locale;

public enum ProtectionAction {
    REMOVE, REPLACE, LOG, KICK;

    public static ProtectionAction parse(String value, ProtectionAction fallback) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
