/*
 * © 2026 Sphereon International B.V.
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

package com.sphereon.kiwa.sample.ui.elicense.issuance

import com.sphereon.core.api.IdkResult
import com.sphereon.core.api.asErrorResult
import com.sphereon.core.api.asOkResult
import com.sphereon.core.api.error.IdkError
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.ManagedKeyInfo
import com.sphereon.crypto.core.KeyInfo
import com.sphereon.crypto.core.cose.CoseCryptoProviderToCallbackAdapter
import com.sphereon.crypto.core.cose.CoseKey
import com.sphereon.crypto.core.cose.CoseKeyType
import com.sphereon.crypto.core.x509.Certificate
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.elicense.sdk.KiwaServices
import com.sphereon.kiwa.elicense.sdk.auth.model.GetWalletCertificateRequestOptions
import com.sphereon.kiwa.elicense.sdk.holder.license.model.AssignDeviceLicenseRequest
import com.sphereon.kiwa.elicense.sdk.holder.license.model.IssueLicenseResult
import com.sphereon.kiwa.elicense.sdk.intern.crypto.KiwaCryptoServices.Companion.KIWA_WALLET_CERT_ALIAS
import com.sphereon.kiwa.sample.ui.auth.settings.UserPreferences
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import com.sphereon.mdoc.data.eu.Pid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, boundType = KiwaWalletService::class)
class KiwaWalletServiceImpl(
    override val services: KiwaServices,
    val userPrefs: UserPreferences,
    val storage: SimpleMdocStore
) : KiwaWalletService {
    val holder = services.holder

    val certificateStore = services.crypto.certificateStoreService
    val auth = services.auth

    init {
        DefaultCallbacks.setCoseCryptoDefault(
            CoseCryptoProviderToCallbackAdapter(keyManagerServiceProvider = {services.crypto.keyManagerService})
        )
    }

    override suspend fun assignElicense(code: String): IssueLicenseResult = withContext(Dispatchers.IO) {
        ensureWalletCertificate(alias = KIWA_WALLET_CERT_ALIAS)
        println("assignElicense: $code and ")
        val assignResult = holder.assignLicense(
            AssignDeviceLicenseRequest(code = code, email = userPrefs.username.lowercase().trim())
        )
        println("assignElicense result: $assignResult")
        if (assignResult.isOk) {
            holder.confirmLicense()
            return@withContext getElicenses()
        }
        assignResult.error.asErrorResult()
    }

    override suspend fun getElicenses(): IssueLicenseResult = withContext(Dispatchers.IO) {
        val issueLicenseResult = holder.issueLicense()

        if (issueLicenseResult.isOk) {
            val documents = issueLicenseResult.value.documents

            println("Elicense size: ${documents.mobileeIDdocuments.size}")

            // Process removed documents - remove them from storage, but preserve PID documents
            processRemovedDocuments(documents.removedDocuments)

            val deviceKeyInfo = geDeviceKeyInfo()
            if (deviceKeyInfo.isErr) {
                println(
                    "We should be returning here as we do not get the device key ${deviceKeyInfo.error.message.defaultMessage}"
                )
                return@withContext deviceKeyInfo.error.asErrorResult()
            }
            // Process new/updated documents - add them to storage if not already present
            processNewDocuments(documents.mobileeIDdocuments, deviceKeyInfo.value)
        }

        issueLicenseResult
    }

    private suspend fun processRemovedDocuments(removedDocuments: Array<com.sphereon.mdoc.data.device.Document>) {
        for (removedDocument in removedDocuments) {
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
    }

    private suspend fun processNewDocuments(
        newDocuments: Array<com.sphereon.mdoc.data.device.Document>,
        deviceKeyInfo: ManagedKeyInfo<CoseKeyType>
    ) {
        for (newDocument in newDocuments) {
            // Each license document needs a device key. In the Kiwa implementation it is always the same key
            val hasDoc = storage.hasDocument(newDocument)
            if (!hasDoc) {
                storage.storeDocument(mdoc = newDocument, keyInfo = deviceKeyInfo)
            }
        }
    }

    /**
     * Generates device key information for storing documents.
     * Each document needs to be associated with a device key for cryptographic operations.
     */
    private suspend fun geDeviceKeyInfo(
        alias: String = KIWA_WALLET_CERT_ALIAS
    ): IdkResult<ManagedKeyInfo<CoseKeyType>, IdkError> {
        // Try to get existing certificate and key
        try {
            val certificate = services.crypto.certificateStoreService.getCertificate(alias = alias)
            val keyInfo = services.crypto.keyManagerService.getKey(KeyInfo<CoseKey>(alias = alias))
            val amendedInfo = certificate.amendCoseKeyInfo(keyInfo)
            return ManagedKeyInfo.fromKeyInfo(amendedInfo).asOkResult()
        } catch (e: NoSuchElementException) {
            // Certificate or key does not exist yet, log and proceed to create new certificate
            println("Certificate or key not found for alias $alias: ${e.message}")
            // Fall through to create new certificate
        } catch (e: IllegalArgumentException) {
            // Invalid alias or key format
            println("Invalid certificate or key for alias $alias: ${e.message}")
            // Fall through to create new certificate
        }

        // Create new certificate if it doesn't exist
        val certResult = auth.getWalletCertificate(GetWalletCertificateRequestOptions(alias = alias))
        return if (certResult.isErr) {
            println("Could not get certificate result for $alias")
            certResult.error.asErrorResult()
        } else {
            ManagedKeyInfo.fromKeyInfo(certResult.value.managedKeyInfo).asOkResult()
        }
    }

    override suspend fun ensureWalletCertificate(alias: String): Certificate {
        return services.ensureWalletCertificate(alias)
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val kiwaWalletService: KiwaWalletService
    }
}
