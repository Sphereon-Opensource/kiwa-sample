# Kiwa Sample Applications

This repository contains sample applications demonstrating integration with the Kiwa eLicense SDK. The samples showcase how to build mobile wallet applications for storing, managing, and presenting digital licenses.

## Documentation

For comprehensive documentation, guides, and API reference, visit:

**[https://docs.sphereon.com/kiwa/guides/getting-started](https://docs.sphereon.com/kiwa/guides/getting-started)**

## Sample Applications

| Sample App | Platforms | Language | Description |
|------------|-----------|----------|-------------|
| [Compose Multiplatform](#compose-multiplatform-app) | Android + iOS | Kotlin (KMP) | Full-featured cross-platform app with shared UI |
| [Swift Simple](#swift-simple-app) | iOS only | Swift | Native iOS app with XCFramework integration |

## Compose Multiplatform App

Located at `example/holder/compose-sample-app/` - a full-featured e-license wallet built with Kotlin Multiplatform and Jetpack Compose.

### Features

- License retrieval, display, and verification
- NFC and QR code engagement
- BLE data transfer
- Shared UI across Android and iOS

### Project Structure

```
example/holder/compose-sample-app/
├── composeApp/                 # Main KMP Compose module
│   └── src/
│       ├── commonMain/         # Shared code
│       ├── androidMain/        # Android-specific code
│       └── iosMain/            # iOS-specific code (Kotlin)
└── iosApp/                     # iOS Xcode wrapper
```

### Building

```bash
# Android
./gradlew :example:holder:compose-sample-app:composeApp:assembleDebug

# iOS - build framework then open Xcode
./gradlew :example:holder:compose-sample-app:composeApp:linkDebugFrameworkIosSimulatorArm64
open example/holder/compose-sample-app/iosApp/iosApp.xcodeproj
```

## Swift Simple App

Located at `example/holder/swift-sample-app/` - a native iOS app demonstrating direct XCFramework integration.

### Features

- License activation via PIN entry
- License storage and display
- mDoc CBOR decoding
- Native SwiftUI interface

### Project Structure

```
example/holder/swift-sample-app/
├── kiwa-swift-sample.xcodeproj/
└── kiwa-swift-sample/
    ├── KiwaSwiftSampleApp.swift
    ├── KiwaManager.swift
    ├── LicenseListView.swift
    └── PinEntryView.swift
```

For detailed documentation, see:
- [XCFramework README](./example/holder/xcframework/README.md)
- [CocoaPods Integration Guide](./example/holder/xcframework/COCOAPODS.md)

### Building

```bash
open example/holder/swift-sample-app/kiwa-swift-sample.xcodeproj
```

Requires `KiwaSdk.xcframework` - see [XCFramework](#xcframework-build) section.

## XCFramework Build

Located at `example/holder/xcframework/` - a standalone Gradle project for building the iOS XCFramework.

### Building

```bash
cd example/holder/xcframework

# Build XCFramework
./gradlew assembleXCFrameworkKiwaSdk

# Build release only
./gradlew assembleReleaseXCFrameworkKiwaSdk
```

### Output

```
build/XCFrameworks/release/KiwaSdk.xcframework
build/XCFrameworks/debug/KiwaSdk.xcframework
```

### CocoaPods

A podspec is generated for CocoaPods integration:

```bash
./gradlew generatePodspec
```

The podspec is output to `build/cocoapods/KiwaSdk.podspec`.

## Shared UI Components

Located at `example/holder/ui/` - reusable UI modules shared across sample apps:

- `auth/` - Authentication UI components
- `card/` - Credential display components
- `core/` - Core UI utilities
- `elicense/` - eLicense-specific features

## Prerequisites

- **JDK 17** or higher
- **Android Studio** with Kotlin Multiplatform plugin
- **Xcode 15+** (for iOS builds)
- **Kiwa SDK Access**: Nexus credentials for SDK repositories
- **Subscription Key**: Valid `KIWA_SUBSCRIPTION_KEY` from Kiwa

### Configuration

Create or update `local.properties`:

```properties
nexusUsername=YOUR_NEXUS_USERNAME
nexusPassword=YOUR_NEXUS_PASSWORD
```

Set subscription key via environment variable:

```bash
export KIWA_SUBSCRIPTION_KEY=your_subscription_key
```

## Technology Stack

- **Kotlin Multiplatform (KMP)**: Cross-platform development
- **Jetpack Compose / Compose Multiplatform**: Shared UI
- **kotlin-inject + Anvil**: Compile-time dependency injection
- **SKIE**: Enhanced Swift interoperability
- **Ktor**: HTTP client

## Platform Requirements

| Platform | Minimum Version |
|----------|-----------------|
| Android | API 27 (Android 8.1) |
| iOS | iOS 15+ / Xcode 15+ |

## License

The sample app and UI code is provided as permissive Open-Source using an Apache 2.0 license. However, the Kiwa SDK itself is proprietary, requires a license, and does not allow redistribution. See [LICENSE.md](./LICENSE.md) for details.
