# Changelog

All notable changes to the MetricFlow SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-XX

### Added
- Initial release of MetricFlow SDK
- Crash monitoring with screenshot capture and ANR detection
- App startup time tracking
- Frame rate monitoring (FPS and dropped frames)
- Memory tracking with configurable sampling
- Network monitoring for OkHttp, Retrofit, Ktor, HttpURLConnection, and Volley
- LogCat collection with filtering
- Battery monitoring (level, status, health, temperature)
- Connectivity monitoring (WiFi, Cellular, Ethernet, VPN)
- Device information collection
- Comprehensive error handling and edge case validation
- Thread-safe operations throughout
- Memory leak prevention
- ProGuard/R8 rules
- Full documentation and examples

### Features
- **Performance Monitoring**: Crash detection, startup tracking, frame monitoring, memory tracking
- **Network Monitoring**: Support for all major Android networking libraries
- **System Monitoring**: Battery, connectivity, and device information
- **Production Ready**: Thread-safe, memory-safe, and thoroughly validated

### Technical Details
- Min SDK: 24 (Android 7.0)
- Compile SDK: 36
- Kotlin: 2.2.20
- Java: 11

---

## [Unreleased]

### Planned
- Custom event tracking
- Performance metrics export
- Remote configuration
- Advanced crash reporting integration

