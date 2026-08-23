package io.github.miklires.mprotect.check;

public record ValidationResult(boolean safe, String detail) {
    public static ValidationResult pass() { return new ValidationResult(true, ""); }
    public static ValidationResult fail(String detail) { return new ValidationResult(false, detail); }
}
