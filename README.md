# KBSON

A [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) [BSON](https://bsonspec.org/) implementation.


## Build & Release process

**Note:** The following instructions are intended for internal use.

### To build the library:

```
$ git clone https://github.com/mongodb/kbson.git
$ cd kbson
$ ./gradlew clean check
```

**Note:** Requires a Mac to build all environments.

### Release process:

**Note:** Releases are automatically created from commits via [evergeen](https://evergreen.mongodb.com/waterfall/kbson).

Snapshot versions are published automatically by evergreen. 
See: https://oss.sonatype.org/content/repositories/snapshots/org/mongodb/kbson/

For an official release:

1. Update the version number in build.gradle.kts eg: `0.1.0`
2. Push to the main repo.
3. Tag eg: `r0.1.0` and push to the main repo
4. Evergreen will stage the release to [sonatype staging](oss.sonatype.org).
5. Check the staged release on sonatype and release.
6. Update the version number to be the next snapshot version eg: `0.2.0-SNAPSHOT`
