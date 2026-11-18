# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Java source and target levels `1.6` -> `1.8`, and Kotlin `1.4.20` -> `2.3.21`: Android applications written in
  Kotlin must now use Kotlin `2.2` or later to compile against this library.
- Minimum Android SDK `19` -> `26` (Android 8.0), as for the other CNA plugins.
- Renamed directory `famoco-plugin` to `plugin`, as for the other CNA plugins.
- Logging through SLF4J instead of Timber, as for the other CNA plugins: `slf4j-api` is now a `compileOnly`
  dependency, and `timber` and `slf4j-timber` are no longer propagated to the applications, which must provide the
  SLF4J API and a binding (for example `slf4j-timber`).
- Build configuration aligned with the other Keyple Android plugins: the `keyple-gradle` plugin is no longer used (POM
  metadata in `gradle.properties`, license header in the `LICENSE_HEADER` file).
- Example app: Kotlin synthetic views replaced by View Binding, and target SDK `29` -> `35`.
### Removed
- Unused dependencies of the plugin: `androidx.legacy:legacy-support-v4`, `androidx.appcompat:appcompat` and
  `kotlin-stdlib-jdk7`, as well as the unused `viewBinding` build feature and `kotlin-android-extensions` plugin.
- Example app: unused resources, legacy launcher icons (the adaptive icon being used from Android 8.0), and the unused
  `keyple-plugin-android-nfc-java-lib-2.0.0-SNAPSHOT.aar` library file.
### Upgraded
- `keyple-common-java-api` from `2.0.0` to `2.0.2`
- `keyple-plugin-java-api` from `2.0.0` to `2.3.2`
- `keyple-util-java-lib` from `2.1.0` to `2.5.0`
- `slf4j-api` from `1.7.32` to `1.7.36` (`compileOnly`)
- Android Gradle plugin from `4.1.3` to `9.4.1`, Gradle from `6.8.3` to `9.8.1`, Dokka from `1.4.32` to `2.2.0`, and
  Spotless from `5.10.2` to `8.2.1`
- Android `compileSdk` from `29` to `35`
### CI
- Workflows migrated to the reusable workflows of `calypsonet-actions` (build and test, publication of the snapshots
  and of the releases to Maven Central via the Central Portal), with JDK `17`; the project version ends with
  `-SNAPSHOT`, the release version being given by the release tag.

## [2.0.2] - 2022-09-14
### CI
- Upgrade from JDK 8 to JDK 11 to fix the problem encountered by Spotless when formatting Kotlin.
- Restore release and publication tasks in maven central repository.
### Upgraded examples
- "Keyple Service Library" to version `2.1.0`
- "Keyple Service Resource Library" to version `2.0.2`
- "Keyple Card Calypso Library" to version `2.2.1`

## [2.0.1] - 2022-06-09
### Added
- `CHANGELOG.md` file (issue [#22]).
- CI: Forbid the publication of a version already released (issue [#20]).
### Fixed
- Removal of the unused Jacoco plugin for compiling Android applications that had an unwanted side effect when the application was launched (stacktrace with warnings).
### Upgraded
- "Keyple Util Library" to version `2.1.0` by removing the use of deprecated methods.
### Upgraded examples
- "Calypsonet Terminal Calypso API" to version `1.2.+`
- "Keyple Service Resource Library" to version `2.0.1`
- "Keyple Card Calypso Library" to version `2.2.0`
- "Keyple Plugin Android NFC Library" to version `2.0.1`

## [2.0.0] - 2021-10-06
### Changed
- Upgrade the component to comply with the new internal APIs of Keyple 2.0

## [1.0.0] - 2020-12-18
This is the initial release.

[unreleased]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/compare/2.0.2...HEAD
[2.0.2]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/compare/2.0.1...2.0.2
[2.0.1]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/compare/1.0.0...2.0.0
[1.0.0]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/releases/tag/1.0.0

[#22]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/issues/22
[#20]: https://github.com/calypsonet/keyple-plugin-cna-famoco-se-communication-java-lib/issues/20