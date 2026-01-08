//
//  KiwaSwiftSampleApp.swift
//  kiwa-swift-sample
//
//  Created by Sphereon on 01/12/2025.
//

import SwiftUI
import KiwaSdk

@main
struct KiwaSwiftSampleApp: SwiftUI.App {
    @StateObject private var userPreferences = UserPreferences()
    @StateObject private var licenseStore = LicenseStore()
    @StateObject private var kiwaManager = KiwaManager()

    var body: some Scene {
        WindowGroup {
            RootView(
                userPreferences: userPreferences,
                licenseStore: licenseStore,
                kiwaManager: kiwaManager
            )
        }
    }
}

struct RootView: View {
    @ObservedObject var userPreferences: UserPreferences
    @ObservedObject var licenseStore: LicenseStore
    @ObservedObject var kiwaManager: KiwaManager

    var body: some View {
        Group {
            if userPreferences.isOnboarded {
                if kiwaManager.isReady,
                   let kiwaServices = kiwaManager.kiwaServices,
                   let log = kiwaManager.log {
                    LicenseListView(
                        licenseStore: licenseStore,
                        userPreferences: userPreferences,
                        kiwaServices: kiwaServices,
                        log: log
                    )
                } else {
                    // Show loading while SDK initializes
                    ProgressView("Initializing...")
                        .onAppear {
                            kiwaManager.setupUserContext(email: userPreferences.email)
                        }
                }
            } else {
                OnboardingView(userPreferences: userPreferences)
            }
        }
        .onChange(of: userPreferences.email) { _, newEmail in
            if !newEmail.isEmpty {
                kiwaManager.setupUserContext(email: newEmail)
            }
        }
    }
}
