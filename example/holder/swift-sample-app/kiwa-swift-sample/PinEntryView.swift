//
//  PinEntryView.swift
//  test
//
//  8-digit PIN entry for license activation
//

import SwiftUI
import KiwaSdk

enum ActivationState {
    case enteringPin
    case activating
    case success
    case error(String)
}

struct PinEntryView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var licenseStore: LicenseStore
    let kiwaServices: KiwaServices
    let email: String
    let log: LogService

    @State private var pin: String = ""
    @State private var state: ActivationState = .enteringPin

    private let pinLength = 8

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                switch state {
                case .enteringPin:
                    pinEntryContent
                case .activating:
                    activatingContent
                case .success:
                    successContent
                case .error(let message):
                    errorContent(message: message)
                }
            }
            .navigationTitle("Activate License")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if case .enteringPin = state {
                        Button("Cancel") {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private var pinEntryContent: some View {
        VStack(spacing: 32) {
            Spacer()

            Text("Enter Activation Code")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Enter the 8-digit code from your license provider")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            // PIN display boxes
            HStack(spacing: 8) {
                ForEach(0..<pinLength, id: \.self) { index in
                    PinBox(
                        character: index < pin.count ? String(pin[pin.index(pin.startIndex, offsetBy: index)]) : nil,
                        isCurrent: index == pin.count
                    )
                }
            }
            .padding(.horizontal)

            // Number pad
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 16) {
                ForEach(1...9, id: \.self) { number in
                    NumberButton(number: "\(number)") {
                        appendDigit("\(number)")
                    }
                }

                NumberButton(number: "C", isDestructive: true) {
                    pin = ""
                }

                NumberButton(number: "0") {
                    appendDigit("0")
                }

                NumberButton(number: "<") {
                    if !pin.isEmpty {
                        pin.removeLast()
                    }
                }
            }
            .padding(.horizontal, 32)

            Spacer()
        }
    }

    private var activatingContent: some View {
        VStack(spacing: 24) {
            Spacer()
            ProgressView()
                .scaleEffect(2)
            Text("Activating License...")
                .font(.headline)
                .foregroundColor(.secondary)
            Spacer()
        }
    }

    private var successContent: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.green)
            Text("License Activated!")
                .font(.title)
                .fontWeight(.bold)
            Text("Your e-license has been added to your wallet")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Button("Done") {
                dismiss()
            }
            .buttonStyle(.borderedProminent)
            .padding(.top)

            Spacer()
        }
    }

    private func errorContent(message: String) -> some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "xmark.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.red)
            Text("Activation Failed")
                .font(.title)
                .fontWeight(.bold)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            HStack(spacing: 16) {
                Button("Try Again") {
                    pin = ""
                    state = .enteringPin
                }
                .buttonStyle(.bordered)

                Button("Cancel") {
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
            }
            .padding(.top)

            Spacer()
        }
    }

    private func appendDigit(_ digit: String) {
        guard pin.count < pinLength else { return }
        pin += digit

        if pin.count == pinLength {
            activateLicense()
        }
    }

    private func activateLicense() {
        state = .activating

        Task {
            do {
                // Step 1: Ensure wallet certificate exists
                log.info(message: "Getting wallet certificate...")
                let certResult = try await kiwaServices.auth.getWalletCertificate(
                    args: GetWalletCertificateRequestOptions(
                        alias: "kiwa-wallet-certificate",
                        keyPair: nil,
                        certificateSigningRequestPem: nil,
                        environment: .acceptance
                    )
                )

                var certError: String? = nil
                certResult.onFailure { error in
                    if let err = error {
                        certError = String(describing: err)
                    } else {
                        certError = "Failed to get wallet certificate"
                    }
                }

                if let error = certError {
                    await MainActor.run {
                        state = .error(error)
                    }
                    return
                }

                log.info(message: "Wallet certificate obtained")

                // Step 2: Assign the license
                log.info(message: "Assigning license with code: \(pin)")
                let assignRequest = AssignDeviceLicenseRequest(
                    environment: .acceptance,
                    code: pin,
                    email: email
                )

                let assignResult = try await kiwaServices.holder.assignLicense(request: assignRequest)

                var assignError: String? = nil
                assignResult.onFailure { error in
                    if let err = error {
                        assignError = String(describing: err)
                    } else {
                        assignError = "Failed to assign license"
                    }
                }

                if let error = assignError {
                    await MainActor.run {
                        state = .error(error)
                    }
                    return
                }

                log.info(message: "License assigned successfully")

                // Step 3: Confirm the license
                log.info(message: "Confirming license...")
                let confirmResult = try await kiwaServices.holder.confirmLicense(request: nil)

                var confirmError: String? = nil
                confirmResult.onFailure { error in
                    if let err = error {
                        confirmError = String(describing: err)
                    } else {
                        confirmError = "Failed to confirm license"
                    }
                }

                if let error = confirmError {
                    await MainActor.run {
                        state = .error(error)
                    }
                    return
                }

                log.info(message: "License confirmed")

                // Step 4: Issue/retrieve the license
                log.info(message: "Issuing license...")
                let issueResult = try await kiwaServices.holder.issueLicense(request: nil)

                var issueError: String? = nil
                issueResult.onSuccess { result in
                    if let rawResult = result {
                        DispatchQueue.main.async {
                            self.licenseStore.addLicenseFromRaw(rawCbor: rawResult.raw)
                        }
                    }
                }
                issueResult.onFailure { error in
                    if let err = error {
                        issueError = String(describing: err)
                    } else {
                        issueError = "Failed to issue license"
                    }
                }

                if let error = issueError {
                    await MainActor.run {
                        state = .error(error)
                    }
                    return
                }

                log.info(message: "License issued successfully")

                await MainActor.run {
                    state = .success
                }

            } catch {
                log.error(message: "Activation error: \(error)", exception: nil, errorResult: nil)
                await MainActor.run {
                    state = .error(error.localizedDescription)
                }
            }
        }
    }
}

struct PinBox: View {
    let character: String?
    let isCurrent: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .stroke(isCurrent ? Color.blue : Color.gray.opacity(0.5), lineWidth: isCurrent ? 2 : 1)
                .frame(width: 36, height: 48)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(.systemBackground))
                )

            if let char = character {
                Text(char)
                    .font(.title2)
                    .fontWeight(.semibold)
            }
        }
    }
}

struct NumberButton: View {
    let number: String
    var isDestructive: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(number)
                .font(.title)
                .fontWeight(.medium)
                .frame(width: 72, height: 72)
                .background(
                    Circle()
                        .fill(isDestructive ? Color.red.opacity(0.1) : Color.gray.opacity(0.1))
                )
                .foregroundColor(isDestructive ? .red : .primary)
        }
    }
}
