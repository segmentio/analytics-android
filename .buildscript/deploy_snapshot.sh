#!/bin/bash
#
# Deploy to Sonatype's snapshot repo.
#
# Adapted from https://coderwall.com/p/9b_lfq,
# http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/
# and https://github.com/square/javawriter

SLUG="segmentio/analytics-android"
JDK="oraclejdk8"
BRANCH="master"

if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping snapshot deployment: was pull request."
elif [ "$TRAVIS_BRANCH" != "$BRANCH" ]; then
  echo "Skipping snapshot deployment: wrong branch. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
else
  echo "Deploying snapshot..."
  ./gradlew clean build uploadArchives -PNEXUS_USERNAME=${env.CI_DEPLOY_NEXUS_USERNAME} -PNEXUS_PASSWORD=${env.CI_DEPLOY_NEXUS_PASSWORD}

fi
