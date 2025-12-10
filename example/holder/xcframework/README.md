# Kiwa SDK XCFramework

This is a standalone Gradle project for building the Kiwa SDK as an iOS XCFramework.

## Purpose

This project creates a Swift/XCFramework with the baseName `KiwaSdk` that includes all necessary dependencies from the Kiwa Holder SDK and Sphereon Identity Development Kit.

## Building the XCFramework

This project is independent from the main root project. Running `./gradlew clean build` in the root project will **not** automatically build this framework (due to the time it takes).

To build the XCFramework:

```bash
cd example/holder/xcframework
./gradlew assembleKiwaSdkXCFramework
```

The generated XCFramework will be located at:
```
build/XCFrameworks/release/KiwaSdk.xcframework
```

## Build Tasks

- `./gradlew assembleKiwaSdkXCFramework` - Builds the XCFramework for all iOS targets (iosX64, iosArm64, iosSimulatorArm64)
- `./gradlew clean` - Cleans the build directory
- `./gradlew tasks` - Lists all available tasks

## Dependencies

The XCFramework exports the following dependencies:
- Kiwa Holder SDK (public and impl)
- Sphereon Core API
- Sphereon Data Link HTTP Client
- Sphereon mDoc Core and Data Transfer
- Sphereon Transport modules (NFC, BLE, REST API, OID4VP)
- Sphereon Logger

## Requirements

- JDK 17 or higher
- macOS with Xcode installed
- Gradle 8.x
- Access to Kiwa and Sphereon repositories (credentials required)

## Configuration

Repository credentials should be set via environment variables or gradle properties:
- `KIWA_REPO_USER` or `kiwaRepoUser`
- `KIWA_REPO_PASSWORD` or `kiwaRepoPassword`

## Version Management

This project references the root project's `gradle/libs.versions.toml` file to avoid duplication and ensure version consistency. Any dependency version updates made in the root project will automatically be reflected in the XCFramework build.
