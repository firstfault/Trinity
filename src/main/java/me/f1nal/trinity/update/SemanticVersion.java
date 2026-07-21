package me.f1nal.trinity.update;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^[vV]?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
                    "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?" +
                    "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");
    private static final Pattern NUMERIC_IDENTIFIER = Pattern.compile("0|[1-9]\\d*");
    private static final Pattern NUMBERED_IDENTIFIER = Pattern.compile("^(.*?)(\\d+)$");

    private final BigInteger major;
    private final BigInteger minor;
    private final BigInteger patch;
    private final List<String> prerelease;
    private final String display;

    private SemanticVersion(Matcher matcher) {
        this.major = new BigInteger(matcher.group(1));
        this.minor = new BigInteger(matcher.group(2));
        this.patch = new BigInteger(matcher.group(3));
        String prereleaseText = matcher.group(4);
        this.prerelease = prereleaseText == null ? List.of()
                : List.of(prereleaseText.split("\\."));
        StringBuilder display = new StringBuilder()
                .append(major).append('.').append(minor).append('.').append(patch);
        if (prereleaseText != null) display.append('-').append(prereleaseText);
        this.display = display.toString();
    }

    static Optional<SemanticVersion> parse(String value) {
        if (value == null) return Optional.empty();
        Matcher matcher = VERSION_PATTERN.matcher(value.trim());
        return matcher.matches() ? Optional.of(new SemanticVersion(matcher)) : Optional.empty();
    }

    boolean isPrerelease() {
        return !prerelease.isEmpty();
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int comparison = major.compareTo(other.major);
        if (comparison != 0) return comparison;
        comparison = minor.compareTo(other.minor);
        if (comparison != 0) return comparison;
        comparison = patch.compareTo(other.patch);
        if (comparison != 0) return comparison;

        if (prerelease.isEmpty()) return other.prerelease.isEmpty() ? 0 : 1;
        if (other.prerelease.isEmpty()) return -1;

        int length = Math.min(prerelease.size(), other.prerelease.size());
        for (int i = 0; i < length; i++) {
            comparison = compareIdentifier(prerelease.get(i), other.prerelease.get(i));
            if (comparison != 0) return comparison;
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = NUMERIC_IDENTIFIER.matcher(left).matches();
        boolean rightNumeric = NUMERIC_IDENTIFIER.matcher(right).matches();
        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric != rightNumeric) return leftNumeric ? -1 : 1;

        Matcher leftNumbered = NUMBERED_IDENTIFIER.matcher(left);
        Matcher rightNumbered = NUMBERED_IDENTIFIER.matcher(right);
        if (leftNumbered.matches() && rightNumbered.matches()
                && leftNumbered.group(1).equals(rightNumbered.group(1))) {
            int comparison = new BigInteger(leftNumbered.group(2))
                    .compareTo(new BigInteger(rightNumbered.group(2)));
            if (comparison != 0) return comparison;
        }
        return left.compareTo(right);
    }

    @Override
    public String toString() {
        return display;
    }
}
