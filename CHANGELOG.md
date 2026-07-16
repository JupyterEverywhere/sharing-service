# Changelog

## [0.10.2] - 2026-07-15

### Security

- Removed bearer tokens, decoded claims, and parser details from application diagnostics
- Required a nonblank JWT signing key of at least 32 UTF-8 bytes at startup
- Bounded authentication inputs and normalized invalid refresh credentials to a generic response
- Removed query-parameter bearer token support; clients must use the `Authorization` header
- Added constant-time comparison for shared authentication secrets
- Updated Spring Boot to 3.5.16 and patched Tomcat, Netty, Jackson, and PostgreSQL JDBC dependencies to clear all HIGH/CRITICAL CVEs

### Testing

- Added credential-leak and authentication-boundary regression tests

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
