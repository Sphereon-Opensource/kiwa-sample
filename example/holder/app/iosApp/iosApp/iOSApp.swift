import SwiftUI
import KiwaSampleApp

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate

    init() {
        // Initialize global properties

        // self.appComponent = IosAppComponent.companion.doInit(application: UIApplication.shared, appId: "sample-app", profile: "testing", version: "0.1.0")

        /* self.userInstance = appComponent.userContextManager.createOrGetFromInputs(tenantInput: DefaultTenantInputString(tenant: "example.com"), principalInput: DefaultPrincipalInputString(principal: "hello@example.com"), makeActive: true)
        self.sessionInstance = userInstance.sessionContextManager.createOrGetFromId(sessionId: "test-session", makeActive: true)

        self.kiwaServices = sessionInstance.getKiwaServices() */
    }

    var body: some Scene {
        WindowGroup {
            ContentView(appComponent: appComponent, sessionInstance: sessionInstance, kiwaServices: kiwaServices)
        }
    }
}
