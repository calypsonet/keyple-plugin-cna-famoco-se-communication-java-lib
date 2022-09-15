# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### CI
- Automation of the right to execute (x) shell scripts.

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