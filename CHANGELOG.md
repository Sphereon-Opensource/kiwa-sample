# Changelog

## 0.13.1-SNAPSHOT — 20260108

- Based on Kiwa SDK 0.13.1-SNAPSHOT and Identity Development Kit 0.13.1-SNAPSHOT
- Version bump to 0.13.1-SNAPSHOT of the Kiwa SDK to align with IDK 0.13.1-SNAPSHOT
- minSdk to 30, because of external library dependencies
- Add sample Swift app, next to the ios and Android compose app
- XCFramework building added
- Cocoapods support
- Removed direct dependencies on the Sphereon Identity Development Kit, as they are exposed via the Kiwa SDK

### Breaking Changes

#### IdkResult API Overhaul (from IDK 0.13.1-SNAPSHOT)
The `IdkResult` type has been completely redesigned for better iOS/Swift/ObjC interoperability:
- **Changed from typealias to wrapper class**: `IdkResult` is now a proper class wrapping kotlin-result's `Result`, instead of a simple typealias
- **New `Ok` and `Err` subclasses**: Use `Ok(value)` and `Err(error)` constructors instead of kotlin-result's functions directly
- **Import changes**: Import `com.sphereon.core.api.Ok` and `com.sphereon.core.api.Err` (top-level functions)
- **New methods**: `component1()`, `component2()` for destructuring; `getOrThrow()` for exception-based handling
- **No more need to depend on com.github.michaelbull.result**

**Migration example:**
```kotlin
// Old (0.10)
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Err
return Ok(value)

// New (0.13)
import com.sphereon.core.api.Ok
import com.sphereon.core.api.Err
return Ok(value)
```

## 0.1.1 — Internal release

### Highlights

- Based on Kiwa SDK 0.7.0 and Identity Development Kit 0.10.0
- Usage of new Engagement features:
    - Seperate NFC and QR access to engagement. Creating new engagements cancels existing ones for easy of use
    - Eventhub for access to all engagement and transfer events
    - UI Projector for easy UI integration.
      See [MdocEngagementPresenterImpl](example/holder/ui/elicense/impl/src/commonMain/kotlin/com/sphereon/kiwa/sample/ui/elicense/engagement/qr/MdocEngagementPresenterImpl.kt)
- Cleanup of ephemeral keys that made the sample app slower over time
- Fixed the back button not working on the share screen
- NFC engagement for Android hooked up to new engagement UI Projector
- Display sub properties in case an object contains an array or map
- Thumbnail images shown in the details view, open in full-screen when clicked with close button

### Android

- minSdk lowered to 27, compileSdk 35 (extension 15), Java 17 toolchain
