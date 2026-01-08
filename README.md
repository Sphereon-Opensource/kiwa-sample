# Kiwa Sample Application

This repository contains a sample holder application demonstrating the integration and usage of the Kiwa eLicense SDK and Sphereon's IDK (Identity Development Kit). The sample app
showcases how to build a mobile wallet application for storing, managing, and presenting digital credentials.

## Overview

This sample holder app demonstrates how to integrate with Kiwa eLicense platform for digital credential management. Built using Kotlin Multiplatform, it provides a practical
example of implementing a credential wallet that can securely store and present digital licenses, certificates, and other credentials on mobile devices.

### Key Features Demonstrated

* **Credential Storage**: Secure storage of digital credentials on mobile devices
* **Credential Presentation**: Present credentials for verification using QR codes or NFC
* **Offline Verification**: Demonstrate offline credential verification capabilities
* **Cross-Platform Support**: Single codebase for both Android and iOS

## Sample App Structure

### Holder App (`example/holder/`)

The main sample application demonstrating:

* **Mobile Wallet Implementation**: Complete wallet app for credential storage and presentation
* **Kiwa SDK Integration**: How to integrate with the Kiwa eLicense SDK
* **IDK Integration**: Usage of Sphereon's IDK (Identity Development Kit) for digital identity features
* **UI Components**: Example user interface for credential management

## Technology Stack

* **Kotlin Multiplatform (KMP)**: Cross-platform mobile development
* **Ktor**: HTTP client for network communication
* **Kiwa eLicense SDK**: Core SDK for interacting with Kiwa services
* **Sphereon IDK (Identity Development Kit)**: Digital identity capabilities
* **Gradle**: Build automation and dependency management
* **Detekt**: Code quality and static analysis

## Getting Started

### Prerequisites

* **Development Environment**:
    - JDK 17 or higher
    - Android Studio or IntelliJ IDEA
    - Kotlin Multiplatform setup

* **Platform Requirements**:
    - Android SDK (API 28+) for Android development
    - Xcode for iOS development
    - Device with Bluetooth and NFC capabilities (for full feature testing)

### Running the Sample App

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd kiwa-sample
   ```

2. **Setup Dependencies**
   ```bash
   ./gradlew build
   ```

3. **Configure API Keys**

   Add your API configuration to the appropriate configuration files (refer to individual module documentation).

4. **Run on Android**
   ```bash
   ./gradlew :example:holder:app:installDebug
   ```

5. **Run on iOS**

   Open the iOS project in Xcode and run on device or simulator.

6. **Build iOS XCFramework** (optional)

   For iOS distribution, a separate XCFramework build is available. See [XCFramework README](./example/holder/xcframework/README.md) for details.

   ```bash
   cd example/holder/xcframework
   ./gradlew assembleXCFrameworkKiwaSdk
   ```

   > **Note**: The XCFramework build is intentionally separated from the main build due to its long compilation time.

### Sample Code Examples

The sample app demonstrates key integration patterns:

**Initialize the SDK**

```kotlin
val kiwaConfig = KiwaConfiguration(
    apiKey = "your-api-key",
    environment = Environment.SANDBOX
)
val kiwaClient = KiwaElicenseClient(kiwaConfig)
```

**Store a Credential**

```kotlin
val storedCredential = walletService.storeCredential(credential)
```

**Present a Credential**

```kotlin
val presentationResult = walletService.presentCredential(
    credential = credential,
    presentationMode = PresentationMode.QR_CODE
)
```

## Code Quality

This project uses [Detekt](https://detekt.dev/) for static code analysis to maintain high code quality standards. The configuration is specifically tailored for Kotlin
multiplatform projects.

### Running Code Analysis

```bash
# Run Detekt analysis on all modules
./gradlew detekt

# Generate baseline file (suppress existing issues)
./gradlew detektBaseline

# Run with auto-correction (where applicable)
./gradlew detekt --auto-correct
```

### Using Convenience Scripts

**Linux/macOS:**
```bash
./scripts/run-detekt.sh
```

**Windows:**
```powershell
./scripts/run-detekt.ps1
```

For detailed information about the Detekt configuration, see [`config/detekt/README.md`](./config/detekt/README.md).

## Project Structure

```
kiwa-sample/
├── example/
│   └── holder/              # Sample holder/wallet application
│       ├── app/             # Main application module (Android & iOS)
│       │   ├── composeApp/  # Kotlin Multiplatform Compose app
│       │   └── iosApp/      # iOS Xcode project
│       ├── ui/              # UI components and screens
│       └── xcframework/     # Standalone XCFramework build (see README)
├── config/                  # Configuration files
├── gradle/                  # Gradle configuration
└── scripts/                 # Utility scripts
```

> **Note**: The `xcframework/` directory is a standalone Gradle project excluded from the main build. See [XCFramework README](./example/holder/xcframework/README.md) for build instructions and licensing information.

## Documentation

For comprehensive SDK documentation and API references:

* **Kiwa eLicense SDK**: Refer to the official Kiwa SDK documentation
* **Sphereon IDK**: Visit the [Sphereon Documentation Portal](https://docs.sphereon.com/)
* **Sample App Modules**: Check individual `README.md` files in each module

## Contributing

This is a sample application for demonstration purposes. For SDK-related issues or feature requests, please refer to the respective SDK documentation and support channels.

## License

Please be aware this software is proprietary and requires a [LICENSE](./LICENSE.md). The example app and UI code is provided as permissive Open-Source using an Apache2 license.
However the SDK itself is not Open-Source, requires a license and does not allow redistribution! Source code licenses and other license options are available upon request.
