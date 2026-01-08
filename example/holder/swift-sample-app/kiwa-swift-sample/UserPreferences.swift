//
//  UserPreferences.swift
//  test
//
//  Persists user preferences like email address
//

import Foundation

class UserPreferences: ObservableObject {
    private let emailKey = "user_email"
    private let onboardedKey = "user_onboarded"

    @Published var email: String {
        didSet {
            UserDefaults.standard.set(email, forKey: emailKey)
        }
    }

    @Published var isOnboarded: Bool {
        didSet {
            UserDefaults.standard.set(isOnboarded, forKey: onboardedKey)
        }
    }

    init() {
        self.email = UserDefaults.standard.string(forKey: emailKey) ?? ""
        self.isOnboarded = UserDefaults.standard.bool(forKey: onboardedKey)
    }

    func completeOnboarding(with email: String) {
        self.email = email.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        self.isOnboarded = true
    }

    func reset() {
        self.email = ""
        self.isOnboarded = false
    }
}
