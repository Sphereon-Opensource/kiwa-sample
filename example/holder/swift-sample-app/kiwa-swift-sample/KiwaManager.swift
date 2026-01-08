//
//  KiwaManager.swift
//  test
//
//  Manages KiwaSdk initialization and user context creation
//

import Foundation
import KiwaSdk
import UIKit

class KiwaManager: ObservableObject {
    let appComponent: KiwaSdkAppComponentMerged

    private var currentEmail: String?
    private var userInstance: UserContextInstance?
    private var sessionInstance: SessionInstance?

    @Published private(set) var isReady: Bool = false

    init() {
        // Initialize global properties for KMS (Key Management Service)
        DefaultPrincipalMapPropertySource.shared.addProperties(map: [
            "test.app.testing.kms.providers.kiwa.id": "kiwa",
            "test.app.testing.kms.providers.kiwa.type": "software",
            "test.app.testing.kms.providers.kiwa.autocreatecertificate": "true",
            "test.app.testing.kms.providers.kiwa.keystore.id": "kiwa",
            "test.app.testing.kms.providers.kiwa.keystore.type": "apple",
            "test.app.testing.kms.providers.kiwa.keystore.keyvisibility": "public",
            "test.app.testing.kms.providers.kiwa.keystore.overwritealias": "true",
        ])

        // Tenant configuration for API environment
        DefaultTenantMapPropertySource.shared.addProperties(map: [
            "kiwa.api.environment": "acceptance",
            "kiwa.subscription.key.acc": "d785f1024ad84484aeb37d1b13f57934",
        ])

        let application = UIApplication.shared
        self.appComponent = KiwaSdkAppComponent.companion.doInit(
            application: application,
            appId: "test-app",
            profile: "testing",
            version: "0.1.0"
        )
    }

    /// Initialize or update the user context with the given email
    func setupUserContext(email: String) {
        let normalizedEmail = email.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)

        // Skip if already set up with same email
        if currentEmail == normalizedEmail && isReady {
            return
        }

        // Extract domain from email for tenant
        let tenant = extractDomain(from: normalizedEmail)

        self.userInstance = appComponent.userContextManager.createOrGetFromInputs(
            tenantInput: DefaultTenantInputString(tenant: tenant),
            principalInput: DefaultPrincipalInputString(principal: normalizedEmail),
            makeActive: true
        )

        self.sessionInstance = userInstance?.sessionContextManager.createOrGetFromId(
            sessionId: "main-session",
            makeActive: true
        )

        self.currentEmail = normalizedEmail
        self.isReady = true
    }

    var kiwaServices: KiwaServices? {
        sessionInstance?.getKiwaServices()
    }

    var log: LogService? {
        sessionInstance?.component.logManager.withTag(tag: "test-app")
    }

    private func extractDomain(from email: String) -> String {
        if let atIndex = email.lastIndex(of: "@") {
            return String(email[email.index(after: atIndex)...])
        }
        return "example.com"
    }
}
