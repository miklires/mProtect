package io.github.miklires.mprotect.storage;

public final class SafeFileName {
    private SafeFileName() {}

    public static String storage(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) return "violations";
        return value;
    }
}
