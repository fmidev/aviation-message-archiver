# Changelog - aviation-message-archiver

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- ...

### Changed

- ...

### Deprecated

- ...

### Removed

- ...

### Fixed

- ...

### Security

- ...

## [v1.4.0] - 2026-02-13

### Added

- Added support for archiving all IWXXM versions starting from 2.1.0. This enables support for future versions too, 
  unless there are significant schema changes.

### Changed

- METAR/SPECI observation time is now obtained directly from the `GenericAviationWeatherMessage` model [#159]
- Collect identifier is now obtained directly from the `MeteorologicalBulletin` model [#162]

### Fixed

- AMQP publication now ignores nil messages gracefully [#161]

## [v1.3.0] - 2025-11-21

### Added

- Ability to archive IWXXM 2025-2 Space Weather Advisories [#158]

## [v1.2.2] - 2025-10-17

### Added

- Option to set the declared RabbitMQ AMQP 1.0 queue exclusive [#157]

### Fixed

- Retrying logic on AMQP exceptions [#156]

## [v1.2.1] - 2025-10-13

### Fixed

- Spring configuration issues [c416349]

## [v1.2.0] - 2025-10-09

### Added

- Ability to publish IWXXM messages to RabbitMQ AMQP 1.0 broker using a customizable post-action framework [#124]

## [v1.1.0] - 2024-02-07

### Added

- Ability to archive IWXXM 2023-1 SIGMETs and AIRMETs. [#123]

### Changed

- Use fmi-avi-messageconverter-bom to manage library versions [#119]

### Security

- Bump postgresql from 42.3.4 to 42.4.1 [#117]
- Dependency upgrades [#118]

## [v1.0.1] - 2022-06-06

### Added

- Add support for files conforming to GTS socket protocol. [#116]

## [v1.0.0] - 2022-05-16

Initial release.

[Unreleased]: https://github.com/fmidev/aviation-message-archiver/compare/aviation-message-archiver-1.4.0...HEAD

[v1.4.0]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.4.0

[v1.3.0]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.3.0

[v1.2.2]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.2.2

[v1.2.1]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.2.1

[v1.2.0]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.2.0

[v1.1.0]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.1.0

[v1.0.1]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.0.1

[v1.0.0]: https://github.com/fmidev/aviation-message-archiver/releases/tag/aviation-message-archiver-1.0.0

[#116]: https://github.com/fmidev/aviation-message-archiver/pull/116

[#117]: https://github.com/fmidev/aviation-message-archiver/pull/117

[#118]: https://github.com/fmidev/aviation-message-archiver/pull/118

[#119]: https://github.com/fmidev/aviation-message-archiver/pull/119

[#123]: https://github.com/fmidev/aviation-message-archiver/pull/123

[#124]: https://github.com/fmidev/aviation-message-archiver/pull/124

[c416349]: https://github.com/fmidev/aviation-message-archiver/commit/c416349a5481414b99d1740de35940b4f9e0267d

[#156]: https://github.com/fmidev/aviation-message-archiver/pull/156

[#157]: https://github.com/fmidev/aviation-message-archiver/pull/157

[#158]: https://github.com/fmidev/aviation-message-archiver/pull/158

[#159]: https://github.com/fmidev/aviation-message-archiver/pull/159

[#161]: https://github.com/fmidev/aviation-message-archiver/pull/161

[#162]: https://github.com/fmidev/aviation-message-archiver/pull/162