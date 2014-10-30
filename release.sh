#!/bin/bash

set -e

if [[ $# -ne 1 ]]; then
  echo "Supply v (major), m (minor) or l(revision) to bump version number."
  exit 1
fi

# Validate that there are no uncommitted files
if [[ $(git status 2> /dev/null | tail -n1) != "nothing to commit, working directory clean" ]]; then
  echo "Working directory dirty. Please revert or commit."
  exit 1
fi

set -x

# Verify the build
./gradlew build

# Infer the release version
newReleaseVersion=$(shtool version release.version)
newReleaseCode=$(echo $newReleaseVersion | sed -e 's/\.//g')

# Update the version codes and commit
sed -i '' "/VERSION_NAME=/s/=.*/=$newReleaseVersion/" gradle.properties
sed -i '' "/VERSION_CODE=/s/=.*/=$newReleaseCode/" gradle.properties
git commit -a -m "[gradle-release-task] prepare release"

# Build and upload artifacts
./gradlew clean build uploadArchives

# Tag the release
git tag $newReleaseVersion

# Prepare for the next version
shtool version --increase "$1" release.version
nextReleaseVersion=$(shtool version release.version)
nextReleaseCode=$(echo $nextReleaseVersion | sed -e 's/\.//g')

sed -i '' "/VERSION_NAME=/s/=.*/=$nextReleaseVersion-SNAPSHOT/" gradle.properties
sed -i '' "/VERSION_CODE=/s/=.*/=$nextReleaseCode/" gradle.properties
git commit -a -m "[gradle-release-task] prepare for next development iteration"
