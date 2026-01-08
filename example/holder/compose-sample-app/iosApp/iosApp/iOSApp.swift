import SwiftUI
import KiwaSampleApp

@main
struct iOSApp: SwiftUI.App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView(appDelegate: appDelegate)
        }
    }
}
