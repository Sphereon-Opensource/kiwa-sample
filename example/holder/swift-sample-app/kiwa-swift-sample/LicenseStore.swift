//
//  LicenseStore.swift
//  test
//
//  Persists and manages e-licenses
//

import Foundation
import KiwaSdk

struct StoredLicense: Identifiable, Codable {
    let id: String
    let jsonData: String
    let rawCborBase64: String
    let storedAt: Date

    init(id: String, jsonData: String, rawCborBase64: String) {
        self.id = id
        self.jsonData = jsonData
        self.rawCborBase64 = rawCborBase64
        self.storedAt = Date()
    }

    var displayFields: [String: String] {
        guard let data = jsonData.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return [:]
        }
        var fields: [String: String] = [:]
        flattenJson(json, prefix: "", into: &fields)
        return fields
    }

    private func flattenJson(_ json: [String: Any], prefix: String, into fields: inout [String: String]) {
        for (key, value) in json {
            let fullKey = prefix.isEmpty ? key : "\(prefix).\(key)"
            if let stringValue = value as? String {
                fields[fullKey] = stringValue
            } else if let numberValue = value as? NSNumber {
                fields[fullKey] = numberValue.stringValue
            } else if let dictValue = value as? [String: Any] {
                flattenJson(dictValue, prefix: fullKey, into: &fields)
            } else if let arrayValue = value as? [Any] {
                fields[fullKey] = String(describing: arrayValue)
            }
        }
    }
}

class LicenseStore: ObservableObject {
    private let licensesKey = "stored_licenses"

    @Published var licenses: [StoredLicense] = []

    init() {
        loadLicenses()
    }

    private func loadLicenses() {
        guard let data = UserDefaults.standard.data(forKey: licensesKey),
              let decoded = try? JSONDecoder().decode([StoredLicense].self, from: data) else {
            licenses = []
            return
        }
        licenses = decoded
    }

    private func saveLicenses() {
        guard let encoded = try? JSONEncoder().encode(licenses) else { return }
        UserDefaults.standard.set(encoded, forKey: licensesKey)
    }

    func addLicense(_ license: StoredLicense) {
        if !licenses.contains(where: { $0.id == license.id }) {
            licenses.append(license)
            saveLicenses()
        }
    }

    func removeLicense(id: String) {
        licenses.removeAll { $0.id == id }
        saveLicenses()
    }

    func clearAll() {
        licenses = []
        saveLicenses()
    }

    func addLicenseFromRaw(rawCbor: KotlinByteArray) {
        let documents = ElicenseIssueDocuments.companion.decodeCbor(data: rawCbor)
        let simpleDisplay = documents.toSimpleDisplay()
        let jsonString = simpleDisplay.toJsonString()
        let rawBase64 = kotlinByteArrayToBase64(rawCbor)

        // Generate a unique ID based on the content
        let id = generateId(from: jsonString)

        let license = StoredLicense(
            id: id,
            jsonData: jsonString,
            rawCborBase64: rawBase64
        )
        addLicense(license)
    }

    private func generateId(from content: String) -> String {
        var hasher = Hasher()
        hasher.combine(content)
        hasher.combine(Date().timeIntervalSince1970)
        return String(format: "%08x", abs(hasher.finalize()))
    }

    private func kotlinByteArrayToBase64(_ byteArray: KotlinByteArray) -> String {
        var bytes = [UInt8]()
        for i in 0..<byteArray.size {
            bytes.append(UInt8(bitPattern: byteArray.get(index: i)))
        }
        return Data(bytes).base64EncodedString()
    }
}
