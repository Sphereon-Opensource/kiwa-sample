import SwiftUI
import KiwaSampleApp

@main
class AppDelegate:  NSObject, UIApplicationDelegate {

    var appComponent: IosAppComponent!
    var appServices: AppServices!

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        self.appComponent = IosAppComponent.companion.doInit(application: UIApplication.shared, appId: "sample-app", profile: "testing", version: "0.1.0")
        self.appServices = (self.appComponent as! AppServicesComponent).appServices

        return true
    }
}
