Releasing
========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT verson.
 2. Update the `CHANGELOG.md` for the impending release.
 3. `git commit -am "Prepare for release X.Y."` (where X.Y is the new version)
 4. `git tag -a X.Y -m "Version X.Y"` (where X.Y is the new version)
 5. `./gradlew clean uploadArchives`
 6. Update the `gradle.properties` to the next SNAPSHOT version.
 7. `git commit -am "Prepare next development version."`
 8. `git push && git push --tags`
 9. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
