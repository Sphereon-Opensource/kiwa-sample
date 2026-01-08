//
//  LicenseListView.swift
//  test
//
//  Displays stored e-licenses
//

import SwiftUI
import KiwaSdk

struct LicenseListView: View {
    @ObservedObject var licenseStore: LicenseStore
    @ObservedObject var userPreferences: UserPreferences
    let kiwaServices: KiwaServices
    let log: LogService

    @State private var showingAddLicense = false
    @State private var showingSettings = false

    var body: some View {
        NavigationView {
            Group {
                if licenseStore.licenses.isEmpty {
                    emptyStateView
                } else {
                    licenseList
                }
            }
            .navigationTitle("My Licenses")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddLicense = true }) {
                        Image(systemName: "plus")
                    }
                }
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { showingSettings = true }) {
                        Image(systemName: "gear")
                    }
                }
            }
            .sheet(isPresented: $showingAddLicense) {
                PinEntryView(
                    licenseStore: licenseStore,
                    kiwaServices: kiwaServices,
                    email: userPreferences.email,
                    log: log
                )
            }
            .sheet(isPresented: $showingSettings) {
                SettingsView(userPreferences: userPreferences, licenseStore: licenseStore)
            }
        }
    }

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "wallet.pass")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text("No Licenses Yet")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Tap the + button to add your first e-license")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button(action: { showingAddLicense = true }) {
                Label("Add License", systemImage: "plus")
                    .fontWeight(.semibold)
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private var licenseList: some View {
        List {
            ForEach(licenseStore.licenses) { license in
                LicenseCard(license: license)
            }
            .onDelete { indexSet in
                for index in indexSet {
                    let license = licenseStore.licenses[index]
                    licenseStore.removeLicense(id: license.id)
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}

struct LicenseCard: View {
    let license: StoredLicense
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Image(systemName: "creditcard.fill")
                    .font(.title2)
                    .foregroundColor(.blue)

                VStack(alignment: .leading, spacing: 2) {
                    Text("E-License")
                        .font(.headline)
                    Text("Added \(formattedDate)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button(action: { withAnimation { isExpanded.toggle() } }) {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.gray)
                }
            }

            // Expanded details
            if isExpanded {
                Divider()

                ForEach(sortedDisplayFields, id: \.key) { item in
                    HStack(alignment: .top) {
                        Text(formatFieldName(item.key))
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .frame(width: 120, alignment: .leading)
                        Text(item.value)
                            .font(.caption)
                            .fontWeight(.medium)
                        Spacer()
                    }
                }

                Divider()

                Text("ID: \(license.id)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 8)
    }

    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: license.storedAt)
    }

    private var sortedDisplayFields: [(key: String, value: String)] {
        license.displayFields
            .sorted { $0.key < $1.key }
            .filter { !$0.key.isEmpty && !$0.value.isEmpty }
            .prefix(20)  // Limit to first 20 fields
            .map { (key: $0.key, value: $0.value) }
    }

    private func formatFieldName(_ name: String) -> String {
        name
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: ".", with: " > ")
            .capitalized
    }
}

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var userPreferences: UserPreferences
    @ObservedObject var licenseStore: LicenseStore
    @State private var showingResetConfirm = false

    var body: some View {
        NavigationView {
            List {
                Section("Account") {
                    HStack {
                        Text("Email")
                        Spacer()
                        Text(userPreferences.email)
                            .foregroundColor(.secondary)
                    }
                }

                Section("Data") {
                    HStack {
                        Text("Stored Licenses")
                        Spacer()
                        Text("\(licenseStore.licenses.count)")
                            .foregroundColor(.secondary)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        showingResetConfirm = true
                    } label: {
                        HStack {
                            Image(systemName: "trash")
                            Text("Reset All Data")
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .alert("Reset All Data?", isPresented: $showingResetConfirm) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) {
                    licenseStore.clearAll()
                    userPreferences.reset()
                }
            } message: {
                Text("This will delete all licenses and reset the app to its initial state.")
            }
        }
    }
}
