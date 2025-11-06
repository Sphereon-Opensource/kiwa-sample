# Changelog

## 0.1.1 — NOT YET RELEASED

### Highlights
- Based on Kiwa SDK 0.7.0 and Identity Development Kit 0.10.0
- Usage of new Engagement features:
  - Seperate NFC and QR access to engagement. Creating new engagements cancels existing ones for easy of use
  - Eventhub for access to all engagement and transfer events
  - UI Projector for easy UI integration. See [MdocEngagementPresenterImpl](example/holder/ui/elicense/impl/src/commonMain/kotlin/com/sphereon/kiwa/sample/ui/elicense/engagement/qr/MdocEngagementPresenterImpl.kt)
- Cleanup of ephemeral keys that made the sample app slower over time
- NFC engagement for Android hooked up to new engagement UI Projector

### Android
- minSdk lowered to 27, compileSdk 35 (extension 15), Java 17 toolchain
