[CmdletBinding()]
param(
    [switch]$DryRun,
    [switch]$Publish,
    [switch]$Yes,
    [string]$NotesFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($DryRun -and $Publish) {
    throw "-DryRun and -Publish cannot be used together."
}

function Assert-LastExitCode {
    param([string]$Action)

    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE."
    }
}

function Get-JavaVersionText {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = "java"
    $startInfo.Arguments = "-version"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) {
            throw "Unable to start Java."
        }
        $standardOutput = $process.StandardOutput.ReadToEnd()
        $standardError = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "Reading the Java version failed with exit code $($process.ExitCode)."
        }
        return "$standardOutput`n$standardError"
    } finally {
        $process.Dispose()
    }
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repositoryRoot

try {
    if (-not (Test-Path "build.gradle.kts") -or -not (Test-Path ".git")) {
        throw "Release script must be run from the Trinity repository."
    }

    $mainSourcePath = "src/main/java/me/f1nal/trinity/Main.java"
    $mainSource = Get-Content -LiteralPath $mainSourcePath -Raw
    $versionMatch = [regex]::Match($mainSource,
            'public\s+static\s+final\s+String\s+VERSION\s*=\s*"(?<version>[^"]+)"')
    if (-not $versionMatch.Success) {
        throw "Could not read Main.VERSION from $mainSourcePath."
    }

    $version = $versionMatch.Groups["version"].Value
    if ($version -notmatch '^\d+\.\d+\.\d+(?:-[0-9A-Za-z][0-9A-Za-z.-]*)?(?:\+[0-9A-Za-z][0-9A-Za-z.-]*)?$') {
        throw "Main.VERSION '$version' is not a supported semantic version."
    }

    $tag = "v$version"
    $branch = (& git branch --show-current | Out-String).Trim()
    Assert-LastExitCode "Reading the current Git branch"
    if ($branch -ne "master") {
        throw "Releases must be made from master; current branch is '$branch'."
    }

    $trackedChanges = @(& git status --porcelain --untracked-files=no)
    Assert-LastExitCode "Checking the working tree"
    if (@($trackedChanges | Where-Object { $_ }).Count -ne 0) {
        throw "Tracked files contain uncommitted changes. Commit or revert them before releasing."
    }

    $untrackedSources = @(& git ls-files --others --exclude-standard -- src Decompiler/src)
    Assert-LastExitCode "Checking for untracked source files"
    if (@($untrackedSources | Where-Object { $_ }).Count -ne 0) {
        throw "Untracked source or resource files would affect the build. Add or remove them before releasing."
    }

    $origin = (& git remote get-url origin | Out-String).Trim()
    Assert-LastExitCode "Reading the origin remote"
    $repositoryMatch = [regex]::Match($origin,
            'github\.com[/:](?<repository>[^/]+/[^/]+?)(?:\.git)?$')
    if (-not $repositoryMatch.Success) {
        throw "Origin '$origin' is not a recognized GitHub repository URL."
    }
    $repository = $repositoryMatch.Groups["repository"].Value

    $javaVersionText = Get-JavaVersionText
    $javaVersionMatch = [regex]::Match($javaVersionText, 'version\s+"(?:1\.)?(?<major>\d+)')
    if (-not $javaVersionMatch.Success) {
        throw "Could not determine the active Java version."
    }
    $javaMajor = [int]$javaVersionMatch.Groups["major"].Value
    if ($javaMajor -lt 17 -or $javaMajor -gt 20) {
        throw "Gradle 8.4 must run this release with JDK 17-20; active Java is $javaMajor. Set JAVA_HOME and PATH first."
    }

    if ($NotesFile) {
        $NotesFile = (Resolve-Path -LiteralPath $NotesFile).Path
    }

    $reuseTag = $false
    $replaceTag = $false
    $releaseExists = $false
    if (-not $DryRun) {
        if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
            throw "GitHub CLI is required to publish. Install it from https://cli.github.com/ and run 'gh auth login'."
        }

        & gh auth status
        Assert-LastExitCode "GitHub authentication check"

        Write-Host "Checking origin/master and existing releases..."
        & git fetch origin master --tags
        Assert-LastExitCode "Fetching origin"

        $headCommit = (& git rev-parse HEAD | Out-String).Trim()
        Assert-LastExitCode "Reading HEAD"
        $remoteCommit = (& git rev-parse origin/master | Out-String).Trim()
        Assert-LastExitCode "Reading origin/master"
        if ($headCommit -ne $remoteCommit) {
            throw "HEAD does not match origin/master. Push or synchronize master before releasing."
        }

        $existingTag = (& git tag --list $tag | Out-String).Trim()
        Assert-LastExitCode "Checking existing tags"
        if ($existingTag) {
            $tagCommit = (& git rev-list -n 1 $tag | Out-String).Trim()
            Assert-LastExitCode "Reading existing tag $tag"
            if ($tagCommit -ne $headCommit) {
                $replaceTag = $true
                Write-Warning "Tag '$tag' points to $tagCommit and will be moved to HEAD ($headCommit)."
            } else {
                $reuseTag = $true
                Write-Warning "Tag '$tag' already points to HEAD and will be reused."
            }
        }

        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = "SilentlyContinue"
            & gh release view $tag --repo $repository *> $null
            $releaseExists = $LASTEXITCODE -eq 0
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        if ($releaseExists) {
            Write-Warning "GitHub release '$tag' already exists and will be replaced."
        }
    }

    Write-Host "Preparing Trinity $version from $repository ($branch)." -ForegroundColor Cyan
    Write-Host "Building and testing Java 17-compatible release JAR..."
    $gradle = Join-Path $repositoryRoot "gradlew.bat"
    & $gradle clean build --console=plain
    Assert-LastExitCode "Gradle build"

    $sourceJar = Join-Path $repositoryRoot "build/libs/Trinity.jar"
    if (-not (Test-Path -LiteralPath $sourceJar)) {
        throw "Gradle completed without producing build/libs/Trinity.jar."
    }

    $releaseDirectory = Join-Path $repositoryRoot "build/release"
    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    $releaseJarName = "Trinity-$version.jar"
    $releaseJar = Join-Path $releaseDirectory $releaseJarName
    $checksumFile = "$releaseJar.sha256"
    Copy-Item -LiteralPath $sourceJar -Destination $releaseJar -Force

    $hash = (Get-FileHash -LiteralPath $releaseJar -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath $checksumFile -Value "$hash  $releaseJarName" -Encoding Ascii

    $reportedVersion = (& java -jar $releaseJar --version 2>&1 | Out-String).Trim()
    Assert-LastExitCode "Release JAR version check"
    if ($reportedVersion -ne $version) {
        throw "Release JAR reported '$reportedVersion', expected '$version'."
    }

    Write-Host "Release artifacts are ready:" -ForegroundColor Green
    Write-Host "  $releaseJar"
    Write-Host "  $checksumFile"

    if ($DryRun) {
        Write-Host "Dry run complete; no tag or GitHub release was created." -ForegroundColor Yellow
        return
    }

    $releaseKind = if ($Publish) { "published" } else { "draft" }
    if (-not $Yes) {
        $tagAction = if ($replaceTag -or $releaseExists) {
            "replace the existing tag/release"
        } elseif ($reuseTag) {
            "push the existing tag"
        } else {
            "create and push the tag"
        }
        $confirmation = Read-Host "Type '$tag' to $tagAction, then create a $releaseKind GitHub release"
        if ($confirmation -ne $tag) {
            throw "Release cancelled."
        }
    }

    if ($releaseExists) {
        Write-Host "Deleting existing GitHub release $tag..." -ForegroundColor Yellow
        & gh release delete $tag --repo $repository --yes
        Assert-LastExitCode "Deleting existing GitHub release $tag"
    }

    if ($replaceTag) {
        & git tag -f -a $tag -m "Trinity $version"
        Assert-LastExitCode "Moving tag $tag"
    } elseif (-not $reuseTag) {
        & git tag -a $tag -m "Trinity $version"
        Assert-LastExitCode "Creating tag $tag"
    }
    if ($replaceTag) {
        & git push origin "refs/tags/$tag" --force
    } else {
        & git push origin $tag
    }
    Assert-LastExitCode "Pushing tag $tag"

    $releaseArguments = @(
        "release", "create", $tag,
        $releaseJar,
        $checksumFile,
        "--repo", $repository,
        "--verify-tag",
        "--title", "Trinity $version"
    )
    if ($NotesFile) {
        $releaseArguments += @("--notes-file", $NotesFile)
    } else {
        $releaseArguments += "--generate-notes"
    }
    if ($version.Contains("-")) {
        $releaseArguments += "--prerelease"
    }
    if (-not $Publish) {
        $releaseArguments += "--draft"
    }

    & gh @releaseArguments
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Tag '$tag' was pushed, but release creation failed. Fix the reported issue and rerun this script; it will safely reuse the tag."
        throw "Creating GitHub release $tag failed with exit code $LASTEXITCODE."
    }

    Write-Host "Created $releaseKind release for $tag." -ForegroundColor Green
    Write-Host "https://github.com/$repository/releases"
} finally {
    Pop-Location
}
