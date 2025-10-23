# Changelog

## [0.7.0] - 2025-10-22

### Added
- Native Java notebook validation using json-schema-validator library
- nbformat v4 JSON schema for validation
- Automated schema update script with proper BSD-3-Clause license attribution
- Performance testing scripts and tooling
- Makefile for common development tasks
- Test notebook generation scripts

### Changed
- Replaced Python subprocess validation with native Java implementation, eliminating subprocess overhead
- Optimized JSON serialization to occur once per upload instead of twice, saving 1-2s per 10MB upload
- Added Gradle cache mount in Docker for faster builds
- Renamed original validator to `PythonJupyterNotebookValidator` (deprecated)

### Fixed
- Added transaction boundary to `updateNotebook()` method to prevent data inconsistency between storage and database operations

### Performance
- Significantly reduced notebook upload time by eliminating Python process spawn and IPC overhead
- Reduced processing overhead for large notebook uploads through single-pass JSON serialization

## [0.6.0] - Previous Release

(Earlier changes not documented)
