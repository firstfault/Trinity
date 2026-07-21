package me.f1nal.trinity.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {
    @Test
    void comparesStableAndPrereleaseVersions() {
        SemanticVersion prerelease = version("v1.2.3-alpha1");
        SemanticVersion stable = version("1.2.3");

        assertTrue(stable.compareTo(prerelease) > 0);
        assertTrue(version("1.3.0-alpha1").compareTo(stable) > 0);
    }

    @Test
    void naturallyOrdersTrinityNumberedPrereleases() {
        assertTrue(version("0.0.3-alpha10").compareTo(version("0.0.3-alpha2")) > 0);
    }

    @Test
    void ignoresTagPrefixAndBuildMetadataInDisplayVersion() {
        assertEquals("2.1.0-beta.2", version("v2.1.0-beta.2+build.8").toString());
    }

    private static SemanticVersion version(String value) {
        return SemanticVersion.parse(value).orElseThrow();
    }
}
