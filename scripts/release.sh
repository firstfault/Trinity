#!/usr/bin/env bash

set -euo pipefail

dry_run=false
publish=false
assume_yes=false
notes_file=""

usage() {
    cat <<'EOF'
Usage: ./scripts/release.sh [options]

Options:
  --dry-run            Build and verify release artifacts without publishing
  --publish            Publish the GitHub release immediately instead of drafting it
  --yes                Skip the tag confirmation prompt
  --notes-file PATH    Use PATH as the GitHub release notes
  -h, --help           Show this help
EOF
}

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

while (($# > 0)); do
    case "$1" in
        --dry-run)
            dry_run=true
            ;;
        --publish)
            publish=true
            ;;
        --yes)
            assume_yes=true
            ;;
        --notes-file)
            (($# >= 2)) || fail "--notes-file requires a path."
            notes_file=$2
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            fail "Unknown option: $1"
            ;;
    esac
    shift
done

if [[ $dry_run == true && $publish == true ]]; then
    fail "--dry-run and --publish cannot be used together."
fi

script_directory=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
repository_root=$(CDPATH= cd -- "$script_directory/.." && pwd)
original_directory=$PWD
trap 'cd "$original_directory"' EXIT
cd "$repository_root"

[[ -f build.gradle.kts && -e .git ]] ||
    fail "Release script must be run from the Trinity repository."

command -v git >/dev/null 2>&1 || fail "Git is required."
command -v shasum >/dev/null 2>&1 || fail "shasum is required."

main_source_path="src/main/java/me/f1nal/trinity/Main.java"
version=$(
    sed -nE 's/.*public[[:space:]]+static[[:space:]]+final[[:space:]]+String[[:space:]]+VERSION[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' \
        "$main_source_path" | head -n 1
)
[[ -n $version ]] || fail "Could not read Main.VERSION from $main_source_path."
if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z][0-9A-Za-z.-]*)?(\+[0-9A-Za-z][0-9A-Za-z.-]*)?$ ]]; then
    fail "Main.VERSION '$version' is not a supported semantic version."
fi

tag="v$version"
branch=$(git branch --show-current)
[[ $branch == "master" ]] ||
    fail "Releases must be made from master; current branch is '$branch'."

if [[ -n $(git status --porcelain --untracked-files=no) ]]; then
    fail "Tracked files contain uncommitted changes. Commit or revert them before releasing."
fi

if [[ -n $(git ls-files --others --exclude-standard -- src Decompiler/src) ]]; then
    fail "Untracked source or resource files would affect the build. Add or remove them before releasing."
fi

origin=$(git remote get-url origin)
case "$origin" in
    *github.com:*)
        repository=${origin#*github.com:}
        ;;
    *github.com/*)
        repository=${origin#*github.com/}
        ;;
    *)
        fail "Origin '$origin' is not a recognized GitHub repository URL."
        ;;
esac
repository=${repository%.git}
if [[ ! $repository =~ ^[^/]+/[^/]+$ ]]; then
    fail "Origin '$origin' is not a recognized GitHub repository URL."
fi

get_java_major() {
    local java_command=$1
    local version_text
    version_text=$("$java_command" -version 2>&1) || return 1
    if [[ $version_text =~ version[[:space:]]+\"(1\.)?([0-9]+) ]]; then
        printf '%s\n' "${BASH_REMATCH[2]}"
        return 0
    fi
    return 1
}

java_command=$(command -v java || true)
java_major=""
if [[ -n $java_command ]]; then
    java_major=$(get_java_major "$java_command" || true)
fi

if ((java_major < 17 || java_major > 20)); then
    compatible_java_home=""
    if [[ -x /usr/libexec/java_home ]]; then
        for requested_java_major in 20 19 18 17; do
            candidate_java_home=$(
                /usr/libexec/java_home -v "$requested_java_major" 2>/dev/null || true
            )
            if [[ -z $candidate_java_home || ! -x $candidate_java_home/bin/java ]]; then
                continue
            fi

            candidate_java_major=$(get_java_major "$candidate_java_home/bin/java" || true)
            if [[ -n $candidate_java_major ]] &&
                    ((candidate_java_major >= 17 && candidate_java_major <= 20)); then
                compatible_java_home=$candidate_java_home
                java_major=$candidate_java_major
                break
            fi
        done
    fi

    if [[ -z $compatible_java_home ]]; then
        active_java=${java_major:-unknown}
        fail "Gradle 8.4 requires JDK 17-20; active Java is $active_java and no compatible installed JDK was found."
    fi

    export JAVA_HOME=$compatible_java_home
    export PATH="$JAVA_HOME/bin:$PATH"
    hash -r
    printf 'Using compatible JDK %s from %s.\n' "$java_major" "$JAVA_HOME"
fi

if [[ -n $notes_file ]]; then
    [[ -f $notes_file ]] || fail "Release notes file '$notes_file' does not exist."
    notes_directory=$(CDPATH= cd -- "$(dirname "$notes_file")" && pwd)
    notes_file="$notes_directory/$(basename "$notes_file")"
fi

reuse_tag=false
replace_tag=false
release_exists=false
if [[ $dry_run == false ]]; then
    command -v gh >/dev/null 2>&1 ||
        fail "GitHub CLI is required to publish. Install it with 'brew install gh' and run 'gh auth login'."

    gh auth status

    printf 'Checking origin/master and existing releases...\n'
    git fetch origin master --tags

    head_commit=$(git rev-parse HEAD)
    remote_commit=$(git rev-parse origin/master)
    if [[ $head_commit != "$remote_commit" ]]; then
        fail "HEAD does not match origin/master. Push or synchronize master before releasing."
    fi

    existing_tag=$(git tag --list "$tag")
    if [[ -n $existing_tag ]]; then
        tag_commit=$(git rev-list -n 1 "$tag")
        if [[ $tag_commit != "$head_commit" ]]; then
            replace_tag=true
            printf 'warning: Tag %q points to %s and will be moved to HEAD (%s).\n' \
                "$tag" "$tag_commit" "$head_commit" >&2
        else
            reuse_tag=true
            printf 'warning: Tag %q already points to HEAD and will be reused.\n' "$tag" >&2
        fi
    fi

    if gh release view "$tag" --repo "$repository" >/dev/null 2>&1; then
        release_exists=true
        printf 'warning: GitHub release %q already exists and will be replaced.\n' "$tag" >&2
    fi
fi

printf '\033[36mPreparing Trinity %s from %s (%s).\033[0m\n' "$version" "$repository" "$branch"
printf 'Building and testing Java 17-compatible release JAR...\n'
./gradlew clean build --console=plain

source_jar="$repository_root/build/libs/Trinity.jar"
[[ -f $source_jar ]] ||
    fail "Gradle completed without producing build/libs/Trinity.jar."

release_directory="$repository_root/build/release"
mkdir -p "$release_directory"
release_jar_name="Trinity-$version.jar"
release_jar="$release_directory/$release_jar_name"
checksum_file="$release_jar.sha256"
cp -f "$source_jar" "$release_jar"

hash=$(shasum -a 256 "$release_jar" | awk '{print $1}')
printf '%s  %s\n' "$hash" "$release_jar_name" > "$checksum_file"

reported_version=$(java -jar "$release_jar" --version)
if [[ $reported_version != "$version" ]]; then
    fail "Release JAR reported '$reported_version', expected '$version'."
fi

printf '\033[32mRelease artifacts are ready:\033[0m\n'
printf '  %s\n' "$release_jar" "$checksum_file"

if [[ $dry_run == true ]]; then
    printf '\033[33mDry run complete; no tag or GitHub release was created.\033[0m\n'
    exit 0
fi

if [[ $publish == true ]]; then
    release_kind="published"
else
    release_kind="draft"
fi

if [[ $assume_yes == false ]]; then
    if [[ $replace_tag == true || $release_exists == true ]]; then
        tag_action="replace the existing tag/release"
    elif [[ $reuse_tag == true ]]; then
        tag_action="push the existing tag"
    else
        tag_action="create and push the tag"
    fi

    printf "Type '%s' to %s, then create a %s GitHub release: " \
        "$tag" "$tag_action" "$release_kind"
    read -r confirmation
    [[ $confirmation == "$tag" ]] || fail "Release cancelled."
fi

if [[ $release_exists == true ]]; then
    printf '\033[33mDeleting existing GitHub release %s...\033[0m\n' "$tag"
    gh release delete "$tag" --repo "$repository" --yes
fi

if [[ $replace_tag == true ]]; then
    git tag -f -a "$tag" -m "Trinity $version"
elif [[ $reuse_tag == false ]]; then
    git tag -a "$tag" -m "Trinity $version"
fi

if [[ $replace_tag == true ]]; then
    git push origin "refs/tags/$tag" --force
else
    git push origin "$tag"
fi

release_arguments=(
    release create "$tag"
    "$release_jar"
    "$checksum_file"
    --repo "$repository"
    --verify-tag
    --title "Trinity $version"
)
if [[ -n $notes_file ]]; then
    release_arguments+=(--notes-file "$notes_file")
else
    release_arguments+=(--generate-notes)
fi
if [[ $version == *-* ]]; then
    release_arguments+=(--prerelease)
fi
if [[ $publish == false ]]; then
    release_arguments+=(--draft)
fi

if ! gh "${release_arguments[@]}"; then
    printf "warning: Tag '%s' was pushed, but release creation failed. Fix the reported issue and rerun this script; it will safely reuse the tag.\n" \
        "$tag" >&2
    fail "Creating GitHub release $tag failed."
fi

printf '\033[32mCreated %s release for %s.\033[0m\n' "$release_kind" "$tag"
printf 'https://github.com/%s/releases\n' "$repository"
