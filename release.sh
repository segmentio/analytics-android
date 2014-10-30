#!/bin/bash

# Look up the version in gradle.properties
releaseVersion=$(grep VERSION_NAME gradle.properties | cut -d= -f2)

# Validate that the permission ends with '-SNAPSHOT'
if [[ $(echo $releaseVersion | rev | cut -c-9 | rev) != "-SNAPSHOT" ]]; then
  echo "Must be on a snapshot version (found $releaseVersion) to make a release."
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
newReleaseVersion=$(echo $releaseVersion | rev | cut -c10- | rev)
newReleaseCode=$(echo $newReleaseVersion | sed -e 's/\.//g')

set -x

# Update the version codes and commit
sed -i '' '/VERSION_NAME=/s/=.*/=$newReleaseVersion/' gradle.properties
sed -i '' '/VERSION_CODE=/s/=.*/=$newReleaseCode/' gradle.properties
git commit -a -m "[gradle-release-task] prepare release"

# Build and upload artifacts
./gradlew clean build uploadArchives

# Tag the release
git tag $newReleaseVersion

# Infer the next version
nextReleaseCode=$(expr $newReleaseCode + 1)
nextReleaseCode=$(echo $nextReleaseCode | sed 's/\(.\)/\1./g')

sed -i '' '/VERSION_NAME=/s/=.*/=$nextReleaseVersion/' gradle.properties
sed -i '' '/VERSION_CODE=/s/=.*/=$nextReleaseCode/' gradle.properties
