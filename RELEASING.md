Releasing
=========

 1. Change the version in `gradle.properties` to your desired release version
 2. Update the `CHANGELOG.md` for the impending release.
 3. `git commit -am "Create release X.Y.Z."` (where X.Y.Z is the new version)
 4. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 5. Change the version in `gradle.properties` to the next SNAPSHOT version
 6. `git push && git push --tags`
 7. The CI pipeline will upload, close and promote the artifacts
