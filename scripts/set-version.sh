#!/usr/bin/env bash
set -euo pipefail

# This script has a regular and a `--release` mode.
#
# In regular mode this script will:
# - update `versionNumber` in `/build.gradle.kts`
# - in `CHANGELOG.md`, if it doesn't exist, add a next release heading
# - on confirmation, commit the changes
# - suggest a git command to push the changes
#
# In release mode:
# - ask to confirm `versionNumber` in `/build.gradle.kts`
# - in `BoxStore.java` update `JNI_VERSION` to match `VERSION`
# - in `README.md` update version numbers in known locations
# - in `CHANGELOG.md` change the next version header to the new version and date
# - on confirmation, commit the changes, create a release tag
# - suggest git commands to push the changes

# Parse optional --release flag
releaseFlag=false
for arg in "$@"; do
    case "$arg" in
        --release) releaseFlag=true ;;
    esac
done

buildScriptFile="../build.gradle.kts"
propVersionNumber="versionNumber"

boxStorePath="$(dirname "$0")/../objectbox-java/src/main/java/io/objectbox/BoxStore.java"
readmePath="$(dirname "$0")/../README.md"
changelogPath="$(dirname "$0")/../CHANGELOG.md"
nextReleaseHeading="## Next release"

# Extract the value of `versionNumber` in build.gradle.kts and store it in the versionCurrent variable
buildScriptPath="$(dirname "$0")/$buildScriptFile"
# Regex matches 'versionNumber = "..."', \K to only capture the version string inside the quotes
versionCurrent=$(grep --only-matching --perl-regexp "$propVersionNumber"' = "\K[^"]+' "$buildScriptPath" || true)
if [[ -z "$versionCurrent" ]]; then
    echo "Error: could not find '$propVersionNumber' in '$buildScriptFile'"
    exit 1
fi

echo ""
echo "Maven artifacts version ($propVersionNumber in $buildScriptFile)"
echo "Current:    $versionCurrent"

if $releaseFlag; then
    # Release: Confirm the current version
    read -r -p "Press enter to confirm the current version, or enter a custom one: " versionInput
    if [[ -n "$versionInput" ]]; then
        versionNew="$versionInput"
    else
        versionNew="$versionCurrent"
    fi
else
    # Suggest a next version
    # Increment the last number in the version string (like 5.4.2 -> 5.4.3, 5.4.2-preview1 -> 5.4.2-preview2)
    # Regex: captures the trailing digits and replaces them with a value increased by 1
    [[ "$versionCurrent" =~ ^(.*[^0-9])([0-9]+)$ ]] || { echo "Error: $propVersionNumber '$versionCurrent' does not end with a number"; exit 1; }
    versionSuggested="${BASH_REMATCH[1]}$(( BASH_REMATCH[2] + 1 ))"
    echo "Suggested:  $versionSuggested"
    read -r -p "Press enter to use the suggested version, or enter a custom one: " versionInput
    if [[ -n "$versionInput" ]]; then
        versionNew="$versionInput"
    else
        versionNew="$versionSuggested"
    fi
fi
echo "Version will be $versionNew"

# Change the value of `versionNumber` in build.gradle.kts to the value of versionNew
sed --in-place "s/$propVersionNumber = \"$versionCurrent\"/$propVersionNumber = \"$versionNew\"/" "$buildScriptPath"

if $releaseFlag; then
    # In BoxStore.java set JNI_VERSION to the value of VERSION (not using versionNew!) and print the used value
    # Regex matches 'String VERSION = "...", \K to only capture the version string inside the quotes
    versionJni=$(grep --only-matching --perl-regexp 'String VERSION = "\K[^"]+' "$boxStorePath" || true)
    if [[ -z "$versionJni" ]]; then
        echo "Error: could not find VERSION in BoxStore.java"
        exit 1
    fi
    echo "Release: setting BoxStore.JNI_VERSION to $versionJni"
    sed --in-place "s/String JNI_VERSION = \".*\"/String JNI_VERSION = \"$versionJni\"/" "$boxStorePath"

    # Change version strings in README.md to versionNew
    echo "Release: updating README.md version strings"
    sed --in-place "s/objectbox = \".*\"/objectbox = \"$versionNew\"/g" "$readmePath"
    sed --in-place "s/id(\"io.objectbox\") version \".*\"/id(\"io.objectbox\") version \"$versionNew\"/g" "$readmePath"
    sed --in-place "s/val objectboxVersion by extra(\".*\")/val objectboxVersion by extra(\"$versionNew\")/g" "$readmePath"
    sed --in-place "s/ext.objectboxVersion = \".*\"/ext.objectboxVersion = \"$versionNew\"/g" "$readmePath"

    # Change header "Next release" in CHANGELOG.md to "versionNew - YYYY-MM-DD"
    echo "Release: updating CHANGELOG.md heading"
    today=$(date +"%Y-%m-%d")
    if grep --quiet "^$nextReleaseHeading" "$changelogPath"; then
        sed --in-place "s/^$nextReleaseHeading/## $versionNew - $today/" "$changelogPath"
    else
        echo "⚠️ '$nextReleaseHeading' heading not found in CHANGELOG.md, check it contains changes for this release!"
    fi
else
    # In CHANGELOG.md, if the first secondary heading is not "## Next release", add it
    firstHeading=$(grep --max-count=1 "^## " "$changelogPath" || true)
    if [[ "$firstHeading" != "$nextReleaseHeading" ]]; then
        echo "Adding '$nextReleaseHeading' to CHANGELOG.md"
        sed --in-place "0,/^## /{s/^## /$nextReleaseHeading\n\n## /}" "$changelogPath"
    fi
fi

# After confirmation commit any changes and for a release create a tag
# Example for a release:
# git commit --all --message="Prepare release 5.4.0"
# git tag V5.4.0
# For a regular run:
# git commit --all --message="Publishing: increase version 5.4.0 -> 5.4.1"
if $releaseFlag; then
    commitMessage="Prepare release $versionNew"
else
    commitMessage="Publishing: increase version $versionCurrent -> $versionNew"
fi
tagRelease="V$versionNew"

echo ""
echo "About to run git commands:"
echo "  git commit --all --message=\"$commitMessage\""
if $releaseFlag; then
    echo "  git tag $tagRelease"
fi

read -r -p "Reviewed changes? Proceed? [y/N] " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 1
fi

git commit --all --message="$commitMessage"
if $releaseFlag; then
    git tag "$tagRelease"
fi

# Print suggested git commands to push the changes
# Note: add `--push-option=ci.skip` to avoid running CI
# Examples:
# git push origin publish --push-option=ci.skip
# git push origin V5.4.0 --push-option=ci.skip

currentBranch=$(git rev-parse --abbrev-ref HEAD)
echo ""
echo "Suggested commands to push these changes:"
echo "  git push origin $currentBranch --push-option=ci.skip"
if $releaseFlag; then
    echo "  git push origin $tagRelease --push-option=ci.skip"
fi
