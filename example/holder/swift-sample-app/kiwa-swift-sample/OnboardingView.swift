//
//  OnboardingView.swift
//  test
//
//  First-start screen to collect user's email address
//

import SwiftUI

struct OnboardingView: View {
    @ObservedObject var userPreferences: UserPreferences
    @State private var email: String = ""
    @State private var showError: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "wallet.pass")
                .font(.system(size: 80))
                .foregroundColor(.blue)

            Text("Kiwa E-License")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Enter your email address to get started")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            VStack(spacing: 16) {
                TextField("Email address", text: $email)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 32)

                if showError {
                    Text("Please enter a valid email address")
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Button(action: continueAction) {
                Text("Continue")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isValidEmail ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
            .disabled(!isValidEmail)
            .padding(.horizontal, 32)

            Spacer()
            Spacer()
        }
    }

    private var isValidEmail: Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        return email.range(of: emailRegex, options: .regularExpression) != nil
    }

    private func continueAction() {
        if isValidEmail {
            userPreferences.completeOnboarding(with: email)
        } else {
            showError = true
        }
    }
}
