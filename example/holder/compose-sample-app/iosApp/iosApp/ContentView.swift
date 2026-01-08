import UIKit
import SwiftUI
import KiwaSampleApp

struct ComposeView: UIViewControllerRepresentable {
    let appComponent: IosAppComponent
    let appServices: AppServices

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(appComponent: appComponent, appServices: appServices)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let appDelegate: AppDelegate

    var body: some View {
        ComposeView(appComponent: appDelegate.appComponent, appServices: appDelegate.appServices)
            .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
