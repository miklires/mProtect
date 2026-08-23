package io.github.miklires.mprotect.update;

public record SemanticVersion(int major, int minor, int patch, boolean prerelease) implements Comparable<SemanticVersion> {
    public static SemanticVersion parse(String value) {
        String[] buildSplit = value.trim().split("\\+", 2);
        String coreAndPre = buildSplit[0];
        int dash = coreAndPre.indexOf('-');
        boolean prerelease = dash >= 0;
        String core = dash >= 0 ? coreAndPre.substring(0, dash) : coreAndPre;
        String[] parts = core.split("\\.");
        return new SemanticVersion(number(parts, 0), number(parts, 1), number(parts, 2), prerelease);
    }

    private static int number(String[] parts, int index) {
        if (index >= parts.length || !parts[index].matches("\\d+")) return 0;
        try { return Integer.parseInt(parts[index]); } catch (NumberFormatException exception) { return 0; }
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        if (result == 0) result = Boolean.compare(other.prerelease, prerelease);
        return result;
    }
}
