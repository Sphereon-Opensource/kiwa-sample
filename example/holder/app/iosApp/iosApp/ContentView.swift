import UIKit
import SwiftUI
import KiwaSampleApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {

    let appComponent: IosAppComponent
    let sessionInstance: SessionInstance
    let kiwaServices: KiwaServices
    let log: LogService

    init(appComponent: IosAppComponent, sessionInstance: SessionInstance, kiwaServices: KiwaServices) {
        self.sessionInstance = sessionInstance
        self.kiwaServices = kiwaServices
        self.log = sessionInstance.component.logManager.withTag(tag: "sample-app")
    }


    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}



