# Kiwa Swift Sample

A sample iOS application demonstrating the Kiwa e-license SDK integration from Swift. This app shows the complete flow of activating and storing e-licenses using the KiwaSdk framework.

## Prerequisites

- Xcode 16.2 or later
- iOS 18.2+ deployment target
- The KiwaSdk.xcframework (built from the Kotlin Multiplatform project)

## Project Structure

```
kiwa-swift-sample/
├── kiwa-swift-sample.xcodeproj/    # Xcode project
├── kiwa-swift-sample/              # Main app source
│   ├── KiwaSwiftSampleApp.swift    # App entry point
│   ├── KiwaManager.swift           # SDK initialization and management
│   ├── OnboardingView.swift        # First-launch email collection
│   ├── LicenseListView.swift       # Main screen with stored licenses
│   ├── LicenseStore.swift          # License persistence and CBOR decoding
│   ├── PinEntryView.swift          # PIN entry for license activation
│   ├── UserPreferences.swift       # UserDefaults wrapper
│   └── ContentView.swift           # Content view
├── kiwa-swift-sampleTests/         # Unit tests
├── kiwa-swift-sampleUITests/       # UI tests
└── kiwa-swift-sample.entitlements  # Keychain entitlements
```

## Build Commands

### Build the project

```bash
xcodebuild -project kiwa-swift-sample.xcodeproj -scheme kiwa-swift-sample build -destination 'platform=iOS Simulator,name=iPhone 16'
```

### Run unit tests

```bash
xcodebuild -project kiwa-swift-sample.xcodeproj -scheme kiwa-swift-sample test -destination 'platform=iOS Simulator,name=iPhone 16'
```

### Clean build

```bash
xcodebuild -project kiwa-swift-sample.xcodeproj -scheme kiwa-swift-sample clean
```

## Building the KiwaSdk Framework

Before building this project, you need to build the KiwaSdk.xcframework from the Kotlin Multiplatform project. The framework should be located at:

```
../xcframework/build/XCFrameworks/release/KiwaSdk.xcframework
```

To build the framework, run the following from the `xcframework` directory:

```bash
./gradlew assembleXCFramework
```

## App Flow

1. **Onboarding** (`OnboardingView.swift`): First-launch email collection, persisted in UserDefaults
2. **License List** (`LicenseListView.swift`): Main screen showing stored licenses with expandable details
3. **License Activation** (`PinEntryView.swift`): 8-digit PIN entry to activate new licenses

## SDK Integration Pattern

```swift
// 1. Configure KMS and tenant properties
DefaultPrincipalMapPropertySource.shared.addProperties(map: [...])
DefaultTenantMapPropertySource.shared.addProperties(map: [...])

// 2. Initialize SDK components
let appComponent = KiwaSdkAppComponent.companion.doInit(application:, appId:, profile:, version:)
let userInstance = appComponent.userContextManager.createOrGetFromInputs(...)
let sessionInstance = userInstance.sessionContextManager.createOrGetFromId(...)
let kiwaServices = sessionInstance.getKiwaServices()

// 3. License activation flow:
//    a. Get wallet certificate (for mTLS)
//    b. Assign license with code + email
//    c. Confirm license
//    d. Issue license and decode CBOR response
```

## SDK Configuration

KMS provider settings (configured in `KiwaManager.swift`):
- Provider type: `software`
- Keystore type: `apple`
- API environment: `acceptance`

## Keychain Requirements

The app requires Keychain entitlements for secure key storage. These are configured in `kiwa-swift-sample.entitlements`.

## Notes

- The `DEBUG=1` preprocessor definition was removed from the project to avoid a conflict with a property named `DEBUG` in the KiwaSdk header.

## License

Copyright Sphereon
