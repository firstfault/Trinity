package me.f1nal.trinity.update;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {
    @Test
    void prereleaseBuildsReceiveNewerPrereleases() {
        Optional<UpdateRelease> update = UpdateChecker.findUpdate("0.0.3-alpha1", """
                [
                  {"tag_name":"v0.0.3-alpha2","html_url":"https://example.test/alpha2","draft":false,"prerelease":true},
                  {"tag_name":"v0.0.3-alpha10","html_url":"https://example.test/alpha10","draft":false,"prerelease":true}
                ]
                """);

        assertEquals("0.0.3-alpha10", update.orElseThrow().version());
        assertEquals("https://example.test/alpha10", update.orElseThrow().url());
    }

    @Test
    void stableBuildsIgnorePrereleases() {
        Optional<UpdateRelease> update = UpdateChecker.findUpdate("1.0.0", """
                [
                  {"tag_name":"v1.1.0-alpha1","html_url":"https://example.test/alpha","draft":false,"prerelease":true},
                  {"tag_name":"v1.0.1","html_url":"https://example.test/stable","draft":false,"prerelease":false}
                ]
                """);

        assertEquals("1.0.1", update.orElseThrow().version());
    }

    @Test
    void ignoresDraftsOlderVersionsAndInvalidResponses() {
        String releases = """
                [
                  {"tag_name":"v2.0.0","html_url":"https://example.test/draft","draft":true,"prerelease":false},
                  {"tag_name":"v0.9.0","html_url":"https://example.test/old","draft":false,"prerelease":false}
                ]
                """;

        assertTrue(UpdateChecker.findUpdate("1.0.0", releases).isEmpty());
        assertTrue(UpdateChecker.findUpdate("1.0.0", "not json").isEmpty());
        assertTrue(UpdateChecker.findUpdate("development", releases).isEmpty());
    }
}
