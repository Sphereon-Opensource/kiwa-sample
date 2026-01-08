# Kiwa SDK XCFramework

This is a standalone Gradle project for building the Kiwa SDK as an iOS XCFramework.

---

## IMPORTANT: Licensing and Redistribution

**WARNING: This XCFramework contains proprietary software.**

The generated `KiwaSdk.xcframework` includes the **Kiwa Holder SDK** (both public API and implementation modules), which is proprietary software owned by Sphereon International B.V.

- **License Required**: A valid commercial license agreement with Sphereon/Kiwa is required to use this SDK.
- **Redistribution Prohibited**: Redistribution of this SDK/XCFramework, in whole or in part, is strictly forbidden without explicit written permission.
- **Internal Use Only**: This framework is intended solely for the licensee's internal business purposes as outlined in the license agreement.

By building and using this XCFramework, you acknowledge that you have read, understood, and agreed to the terms of your license agreement with Sphereon/Kiwa.

For licensing inquiries, contact Sphereon International B.V.

---

## Purpose

This project creates a dynamic XCFramework named `KiwaSdk` that bundles:

1. **Kiwa Holder SDK** - The core e-license SDK for license holder operations
2. **Sphereon Identity Development Kit (IDK)** - Cryptographic operations, mDoc handling, and transport protocols

The XCFramework supports all iOS targets:
- `iosArm64` - Physical iOS devices (iPhone, iPad)
- `iosX64` - iOS Simulator on Intel Macs
- `iosSimulatorArm64` - iOS Simulator on Apple Silicon Macs

## Why a Separate Project?

Building an XCFramework is a time-consuming process that involves:
- Compiling Kotlin/Native code for multiple iOS architectures
- Running SKIE for Swift interop enhancements
- Linking all dependencies into a single framework
- Assembling the final XCFramework bundle

To avoid this overhead on every build of the main project, this XCFramework build is intentionally separated. The main project at the repository root will **not** build this framework automatically.

## Building the XCFramework

### Quick Start

```bash
cd example/holder/xcframework
./gradlew assembleXCFrameworkKiwaSdk
```

### Build Output

The generated XCFramework will be located at:
```
build/XCFrameworks/release/KiwaSdk.xcframework
build/XCFrameworks/debug/KiwaSdk.xcframework
```

### Available Build Tasks

| Task | Description |
|------|-------------|
| `assembleXCFrameworkKiwaSdk` | Build XCFramework for all iOS targets (debug + release) |
| `assembleReleaseXCFrameworkKiwaSdk` | Build release XCFramework only |
| `assembleDebugXCFrameworkKiwaSdk` | Build debug XCFramework only |
| `linkReleaseFrameworkIosArm64` | Build release framework for iOS devices |
| `linkDebugFrameworkIosSimulatorArm64` | Build debug framework for Apple Silicon simulators |
| `clean` | Clean the build directory |
| `tasks` | List all available tasks |

## Dependencies Included

The XCFramework exports and bundles the following dependencies:

### Kiwa SDK (Proprietary)
- `kiwa-holder-sdk-public` - Public API interfaces and models
- `kiwa-holder-sdk-impl` - Implementation and DI components

### Sphereon Identity Development Kit
- `lib-core-api-public` / `lib-core-api-default` - Core API abstractions
- `lib-data-link-http-client-public` / `lib-data-link-http-client-impl` - HTTP client
- `lib-mdoc-core-public` / `lib-mdoc-core-impl` - mDoc/mDL core functionality
- `lib-mdoc-datatransfer-public` / `lib-mdoc-datatransfer-impl` - mDoc data transfer
- `lib-mdoc-transport-ble` - Bluetooth Low Energy transport
- `lib-mdoc-transport-restapi` - REST API transport
- `lib-mdoc-transport-oid4vp` - OpenID4VP transport
- `lib-core-loggers-mobile-logger` - Mobile logging

## Requirements

### Build Environment
- **macOS** with Xcode 15+ installed (required for iOS compilation)
- **JDK 17** or higher
- **Gradle 8.x** (included via wrapper)

### Repository Access
Access to private Kiwa and Sphereon Maven repositories is required. Credentials must be configured via:

**Environment Variables (recommended):**
```bash
export KIWA_REPO_USER="your-username"
export KIWA_REPO_PASSWORD="your-password"
```

**Or Gradle Properties** (`~/.gradle/gradle.properties`):
```properties
kiwaRepoUser=your-username
kiwaRepoPassword=your-password
```

Alternative environment variables also supported:
- `NEXUS_USERNAME` / `NEXUS_PASSWORD`

## Integration with iOS Projects

### Adding to Xcode

1. Build the XCFramework using the commands above
2. In Xcode, go to your target's **General** tab
3. Under **Frameworks, Libraries, and Embedded Content**, click **+**
4. Click **Add Other...** > **Add Files...**
5. Navigate to `build/XCFrameworks/release/KiwaSdk.xcframework`
6. Set **Embed** to **Embed & Sign**

### Swift Usage

```swift
import KiwaSdk

// Create services instance
let kiwaServices: KiwaServices = ...

// Request wallet certificate
let options = GetWalletCertificateRequestOptions(
    alias: "myDeviceAlias",
    environment: .acceptance
)
let result = await kiwaServices.auth.commands.getWalletCertificate.execute(args: options)

// Issue a license
let issueResult = await kiwaServices.holder.commands.issue.execute(
    args: IssueLicenseRequest(environment: .acceptance)
)
```

## Version Management

This project references the root project's `gradle/libs.versions.toml` file to ensure version consistency. Dependency version updates in the root project are automatically reflected here.

Current versions are defined in:
```
../../../gradle/libs.versions.toml
```

## Troubleshooting

### Build Fails with "No KIWA_REPO_USER environment..."
Ensure repository credentials are configured. See the **Repository Access** section above.

### Build Fails on Non-macOS Systems
XCFramework compilation requires macOS with Xcode. The Kotlin/Native compiler needs access to iOS SDKs.

### Out of Memory Errors
Increase Gradle JVM memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx8092M
```

### Stale Cache Issues
Clean and rebuild:
```bash
./gradlew clean
./gradlew assembleXCFrameworkKiwaSdk --rerun-tasks
```
