Releasing
=========

 1. Create a new branch called `release/X.Y.Z`
 2. `git checkout -b release/X.Y.Z`
 3. Change the version in `gradle.properties` to your desired release version
 4. Update the `CHANGELOG.md` for the impending release.
 5. `git commit -am "Create release X.Y.Z."` (where X.Y.Z is the new version)
 6. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 7. Upgrade to next version by changing version in `gradle.properties`
 8. `git commit -am "Prepare snapshot X.Y.Z-SNAPSHOT"`
 9. `git push && git push --tags`
 10. Create a PR to merge the new branch into `master`
 11. The CI pipeline will recognize the tag and upload, close and promote the artifacts automatically

Example (stable release)
========
 1. Current VERSION_NAME in `gradle.properties` = 4.9.1
 2. `git checkout -b release/4.9.2`
 3. Change VERSION_NAME = 4.9.2 (next higher version)
 4. Update CHANGELOG.md
 5. `git commit -am "Create release 4.9.2"`
 6. `git tag -a 4.9.2 -m "Version 4.9.2"`
 6. `git push && git push --tags`
 7. Change VERSION_NAME = 4.9.3 (next higher version)
 8. `git commit -am "Prepare snapshot 4.9.3-SNAPSHOT"`
 9. `git push && git push --tags`
 10. Merging PR master will create a snapshot release 4.9.3-SNAPSHOT and tag push will create stable release 4.9.2