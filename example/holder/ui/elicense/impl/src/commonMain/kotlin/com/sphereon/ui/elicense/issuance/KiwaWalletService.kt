/*
 * © 2025 Sphereon International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.sphereon.ui.elicense.issuance

import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.sphereon.core.api.SureResult
import com.sphereon.core.api.asErrorResult
import com.sphereon.core.api.asOkResult
import com.sphereon.core.api.error.ISureError
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.IManagedKeyInfo
import com.sphereon.crypto.core.KeyInfo
import com.sphereon.crypto.core.ManagedKeyInfo
import com.sphereon.crypto.core.cose.CoseCryptoProviderToCallbackAdapter
import com.sphereon.crypto.core.cose.ICoseKeyCbor
import com.sphereon.crypto.core.x509.Certificate
import com.sphereon.crypto.kms.keystore.memory.registerMemoryKeyStoreSerialization
import com.sphereon.crypto.kms.keystore.software.registerSoftwareKeyStoreSerialization
import com.sphereon.crypto.kms.provider.software.registerSoftwareKmsSerialization
import com.sphereon.di.session.SureSessionScope
import com.sphereon.kiwa.elicense.sdk.IKiwaServices
import com.sphereon.kiwa.elicense.sdk.auth.model.GetWalletCertificateRequestOpts
import com.sphereon.kiwa.elicense.sdk.holder.license.model.AssignDeviceLicenseRequest
import com.sphereon.kiwa.elicense.sdk.holder.license.model.IssueLicenseResult
import com.sphereon.kiwa.elicense.sdk.intern.crypto.IKiwaCryptoServices.Companion.KIWA_WALLET_CERT_ALIAS
import com.sphereon.mdoc.data.eu.Pid
import com.sphereon.ui.auth.settings.IUserPreferences
import com.sphereon.ui.elicense.store.ISimpleMdocStore

@Inject
@SingleIn(SureSessionScope::class)
@ContributesBinding(SureSessionScope::class, boundType = IKiwaWalletService::class)
class KiwaWalletService(
    override val services: IKiwaServices,
    val userPrefs: IUserPreferences,
    val storage: ISimpleMdocStore
) : IKiwaWalletService {
    val holder = services.holder

    val certificateStore = services.crypto.certificateStoreService
    val auth = services.auth

    init {
        DefaultCallbacks.setCoseCryptoDefault(
            CoseCryptoProviderToCallbackAdapter(keyManagerService = services.crypto.keyManagerService)
        )
        registerMemoryKeyStoreSerialization()
        registerSoftwareKeyStoreSerialization()
        registerSoftwareKmsSerialization()
    }

    override suspend fun assignElicense(code: String): IssueLicenseResult {
        ensureWalletCertificate(alias = KIWA_WALLET_CERT_ALIAS)
        println("assignElicense: $code and ")
        val assignResult = holder.assignLicense(
            AssignDeviceLicenseRequest(code = code, email = userPrefs.username.lowercase().trim())
        )
        println("assignElicense result: $assignResult")
        if (assignResult.isOk) {
            holder.confirmLicense()
            return getElicenses()
        }
        return assignResult.error.asErrorResult()
    }

    override suspend fun getElicenses(): IssueLicenseResult {
        val issueLicenseResult = holder.issueLicense()

        if (issueLicenseResult.isOk) {
            val documents = issueLicenseResult.value.documents

            println("Elicense size: ${documents.mobileeIDdocuments.size}")

            // Process removed documents - remove them from storage, but preserve PID documents
            for (removedDocument in documents.removedDocuments) {
                if (storage.hasDocument(removedDocument)) {
                    // Check if this is a PID document - preserve PID documents
                    val docType = removedDocument.docType.toString()
                    val isPidDocument = docType.contains("pid", ignoreCase = true) ||
                        docType.contains("eu.europa", ignoreCase = true) ||
                        removedDocument.docType == Pid.DOCTYPE

                    if (!isPidDocument) {
                        println("Removing document: ${removedDocument.docType}")
                        storage.removeDocument(removedDocument)
                    } else {
                        println("Preserving PID document: ${removedDocument.docType}")
                    }
                }
            }
            val deviceKeyInfo = geDeviceKeyInfo()
            if (deviceKeyInfo.isErr) {
                println(
                    "We should be returning here as we do not get the device key ${deviceKeyInfo.error.message.defaultMessage}"
                )
                return deviceKeyInfo.error.asErrorResult()
            }
            // Process new/updated documents - add them to storage if not already present
            for (newDocument in documents.mobileeIDdocuments) {
                // Each license document needs a device key. In the Kiwa implementation it is always the same key

                if (!storage.hasDocument(newDocument)) {
                    println("Adding new document: ${newDocument.docType}")

                    storage.storeDocument(mdoc = newDocument, keyInfo = deviceKeyInfo.value)
                } else {
                    println("Document already exists, skipping: ${newDocument.docType}")
                }
            }
        }

        return issueLicenseResult
    }

    /**
     * Generates device key information for storing documents.
     * Each document needs to be associated with a device key for cryptographic operations.
     */
    private suspend fun geDeviceKeyInfo(
        alias: String = KIWA_WALLET_CERT_ALIAS
    ): SureResult<IManagedKeyInfo<ICoseKeyCbor>, ISureError> {
        try {
            val certificate = services.crypto.certificateStoreService.getCertificate(alias = alias)
            val keyInfo = services.crypto.keyManagerService.getKey(KeyInfo<ICoseKeyCbor>(alias = alias))
            val amendedInfo = certificate.amendCoseKeyInfo(keyInfo)
            return ManagedKeyInfo.fromKeyInfo(amendedInfo).asOkResult()
        } catch (_e: Exception) {
            // Did not exist
        }

        val certResult = auth.getWalletCertificate(GetWalletCertificateRequestOpts(alias = alias))
        if (certResult.isErr) {
            println("Could not get certificate result for $alias")
            return certResult.error.asErrorResult()
        }
        return certResult.value.managedKeyInfo.asOkResult()
    }

    override suspend fun ensureWalletCertificate(alias: String): Certificate {
        return services.ensureWalletCertificate(alias)
    }

    @ContributesTo(SureSessionScope::class)
    interface Component {
        val kiwaWalletService: IKiwaWalletService
    }
}
