# Changelog

## 0.2.0 — Not yet release

- Based on Kiwa SDK 0.8.0 and Identity Development Kit 0.11.0
- minSdk to 30, because of external library dependencies
- Removed direct deepencies on the Sphereon Identity Development Kit, as they are exposed via the Kiwa SDK

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
