import SwiftUI
import KiwaSampleApp

class AppDelegate: NSObject, UIApplicationDelegate {

    var appComponent: IosAppComponent!
    var appServices: AppServices!

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {

        // Configure KMS providers (principal-level properties) before component creation
        _ = DefaultPrincipalMapPropertySource.shared.addProperties(map: [
            "sample.app.testing.kms.providers.kiwa.id": "kiwa",
            "sample.app.testing.kms.providers.kiwa.type": "software",
            "sample.app.testing.kms.providers.kiwa.autocreatecertificate": "true",
            "sample.app.testing.kms.providers.kiwa.keystore.id": "kiwa",
            "sample.app.testing.kms.providers.kiwa.keystore.type": "apple",
            "sample.app.testing.kms.providers.kiwa.keystore.keyvisibility": "public",
            "sample.app.testing.kms.providers.kiwa.keystore.overwritealias": "true"
        ])

        // Configure tenant-level properties
        _ = DefaultTenantMapPropertySource.shared.addProperties(map: [
            "kiwa.api.environment": "acceptance",
            "kiwa.subscription.key.acceptance": "d785f1024ad84484aeb37d1b13f57934"
        ])

        // Initialize the Kiwa SDK component using the init method that also initializes the root scope provider
        self.appComponent = IosAppComponent.companion.doInit(
            application: UIApplication.shared,
            appId: "sample-app",
            profile: "testing",
            version: "0.1.0"
        )

        // Get AppServices directly from the component property
        self.appServices = self.appComponent.appServices

        // Initialize the AppServices with the component (sets up internal references)
        self.appServices.onCreate(appComponent: self.appComponent)

        return true
    }
}
