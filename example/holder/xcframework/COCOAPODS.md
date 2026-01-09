# KiwaSdk CocoaPods Distribution

This document describes how to consume the KiwaSdk XCFramework via CocoaPods, including usage in React Native projects.

## WARNING:

The Kiwa SDK is proprietary software. No-one other than Sphereon may make the software available to third parties.
This means it is not allowed to distribute the CocoaPods to the outside world or third parties without prior written
consent from Sphereon.

## Publishing to Nexus

### Build and Publish

```bash
cd example/holder/xcframework

# Build XCFramework and publish to Nexus
./gradlew publishXcframeworkPublicationToNexusRepository

# Generate the podspec (for reference/local use)
./gradlew generatePodspec
```

The XCFramework will be published by Sphereon to the following customer (restricted) repositories:

- **Snapshots**: `https://nexus.sphereon.com/repository/kiwa-snapshots/com/sphereon/kiwa/kiwa-sdk-ios/`
- **Releases**: `https://nexus.sphereon.com/repository/kiwa-releases/com/sphereon/kiwa/kiwa-sdk-ios/`

## Consuming in React Native

### 1. Configure Nexus Authentication

Add your Nexus credentials to your environment:

```bash
export KIWA_REPO_USER="your-username"
export KIWA_REPO_PASSWORD="your-password"
```

Or add to your `~/.netrc` file:

```
machine nexus.sphereon.com
login your-username
password your-password
```

### 2. Add to Podfile

In your React Native project's `ios/Podfile`:

```ruby
# Option A: Reference the podspec from the repository
pod 'KiwaSdk', :podspec => 'https://raw.githubusercontent.com/Sphereon-Opensource/kiwa-examples/main/example/holder/xcframework/build/cocoapods/KiwaSdk.podspec'

# Option B: Reference a local podspec (for development)
pod 'KiwaSdk', :podspec => '/path/to/kiwa-sample/example/holder/xcframework/build/cocoapods/KiwaSdk.podspec'

# Option C: Use a private spec repo
source 'https://github.com/your-org/private-podspecs.git'
pod 'KiwaSdk', '~> 0.13.0'
```

### 3. Install Dependencies

```bash
cd ios
pod install
```

## Direct XCFramework Integration (Alternative)

If you prefer to integrate the XCFramework directly without CocoaPods:

### 1. Download XCFramework

```bash
# For snapshots
curl -u "$KIWA_REPO_USER:$KIWA_REPO_PASSWORD" \
  -O "https://nexus.sphereon.com/repository/kiwa-snapshots/com/sphereon/kiwa/kiwa-sdk-ios/0.13.0/kiwa-sdk-ios-0.13.0.zip"

unzip kiwa-sdk-ios-0.13.0.zip
```

### 2. Add to Xcode Project

1. Drag `KiwaSdk.xcframework` into your Xcode project
2. Ensure "Embed & Sign" is selected for the framework
3. Add to your target's "Frameworks, Libraries, and Embedded Content"

## React Native Native Module Bridge

To use KiwaSdk from JavaScript, you'll need to create a native module bridge:

### Swift Bridge Example

```swift
// ios/KiwaSdkBridge.swift
import Foundation
import KiwaSdk

@objc(KiwaSdkBridge)
class KiwaSdkBridge: NSObject {

    @objc
    func initialize(_ resolve: @escaping RCTPromiseResolveBlock,
                   reject: @escaping RCTPromiseRejectBlock) {
        // Initialize KiwaSdk components
        let component = KiwaSdkAppComponent.companion.create()
        resolve(true)
    }

    @objc
    static func requiresMainQueueSetup() -> Bool {
        return false
    }
}
```

### Objective-C Module Header

```objc
// ios/KiwaSdkBridge.m
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(KiwaSdkBridge, NSObject)

RCT_EXTERN_METHOD(initialize:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

@end
```

## Troubleshooting

### Authentication Issues

If you encounter 401/403 errors:

1. Verify your Nexus credentials are correct
2. Check that you have read access to the repository
3. For CocoaPods, ensure credentials are in `~/.netrc`

### Framework Not Found

1. Run `pod deintegrate && pod install` to clean reinstall
2. Verify the XCFramework exists at the expected Nexus URL
3. Check your Podfile is referencing the correct version

### Architecture Issues

The XCFramework includes:

- `ios-arm64` (physical devices)
- `ios-arm64-simulator` (Apple Silicon simulators)
- `ios-x86_64-simulator` (Intel simulators)

If you see architecture errors, ensure you're building for a supported platform.
