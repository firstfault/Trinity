package me.f1nal.trinity.update;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class UpdateChecker {
    private static final String RELEASES_URL = "https://github.com/firstfault/Trinity/releases";
    private static final URI RELEASES_API = URI.create(
            "https://api.github.com/repos/firstfault/Trinity/releases?per_page=30");
    private static final Type RELEASE_LIST_TYPE = new TypeToken<List<GitHubRelease>>() { }.getType();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4L))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private UpdateChecker() {
    }

    public static CompletableFuture<Optional<UpdateRelease>> checkAsync(String currentVersion) {
        HttpRequest request = HttpRequest.newBuilder(RELEASES_API)
                .timeout(Duration.ofSeconds(6L))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Trinity-Update-Checker")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException(
                                "GitHub returned HTTP " + response.statusCode()));
                    }
                    return findUpdate(currentVersion, response.body());
                });
    }

    static Optional<UpdateRelease> findUpdate(String currentVersion, String responseBody) {
        Optional<SemanticVersion> parsedCurrent = SemanticVersion.parse(currentVersion);
        if (parsedCurrent.isEmpty() || responseBody == null) return Optional.empty();

        List<GitHubRelease> releases;
        try {
            releases = GSON.fromJson(responseBody, RELEASE_LIST_TYPE);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        if (releases == null) return Optional.empty();

        SemanticVersion current = parsedCurrent.get();
        return releases.stream()
                .filter(release -> release != null && !release.draft)
                .map(release -> Candidate.create(release, current.isPrerelease()))
                .flatMap(Optional::stream)
                .filter(candidate -> candidate.version.compareTo(current) > 0)
                .max(Comparator.comparing(Candidate::version))
                .map(candidate -> new UpdateRelease(candidate.version.toString(),
                        candidate.url == null || candidate.url.isBlank() ? RELEASES_URL : candidate.url));
    }

    private static final class GitHubRelease {
        @SerializedName("tag_name")
        private String tagName;
        @SerializedName("html_url")
        private String htmlUrl;
        private boolean draft;
        private boolean prerelease;
    }

    private record Candidate(SemanticVersion version, String url) {
        private static Optional<Candidate> create(GitHubRelease release, boolean includePrereleases) {
            Optional<SemanticVersion> version = SemanticVersion.parse(release.tagName);
            if (version.isEmpty()) return Optional.empty();
            if (!includePrereleases && (release.prerelease || version.get().isPrerelease())) {
                return Optional.empty();
            }
            return Optional.of(new Candidate(version.get(), release.htmlUrl));
        }
    }
}
