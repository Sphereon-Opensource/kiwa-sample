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

package com.sphereon.kiwa.sample.ui.elicense.keystore

import com.sphereon.core.api.context.SessionExecution
import com.sphereon.crypto.core.kms.KeyManagerService
import com.sphereon.crypto.kms.keystore.software.SoftwareKeyStoreService
import com.sphereon.crypto.kms.provider.software.SoftwareKmsProvider
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.elicense.sdk.intern.crypto.KiwaCryptoServices.Companion.KIWA_WALLET_CERT_ALIAS
import com.sphereon.kiwa.sample.ui.elicense.issuance.TestPidIssuerImpl.Companion.PID_ISSUER_KEY_ALIAS
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Implementation of KeyCleanupService that manages ephemeral key cleanup.
 * 
 * This service coordinates with the SimpleMdocStore and KeyManagerService to
 * identify and remove keys that are no longer needed while preserving essential
 * system keys.
 */
@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, boundType = KeyCleanupService::class)
class KeyCleanupServiceImpl(
    private val mdocStore: SimpleMdocStore,
    private val kms: KeyManagerService,
    execution: SessionExecution
) : KeyCleanupService {

    private val log = execution.log.logManager.withTag("KeyCleanupService")
    private val cleanupMutex = Mutex()
    private var isCleanupRunning = false

    companion object {
        /**
         * Set of key aliases that should always be preserved during cleanup.
         * These are essential system keys required for wallet operations.
         */
        private val PRESERVED_KEY_ALIASES = setOf(
            KIWA_WALLET_CERT_ALIAS,  // kiwa-wallet-certificate
            PID_ISSUER_KEY_ALIAS,      // test-pid-issuer
            "sectigo" // Root CA for Kiwa APIs (for older platforms)
        )
    }

    override suspend fun cleanupEphemeralKeys() = withContext(Dispatchers.IO) {
        cleanupMutex.withLock {
            if (isCleanupRunning) {
                log.info("Key cleanup already running, skipping")
                return@withContext
            }
            isCleanupRunning = true
        }

        try {
            log.info("Starting ephemeral key cleanup")

            // Get the software KMS provider
            val provider = kms.getProviderById(kms.defaultProviderId()) as? SoftwareKmsProvider
            if (provider == null) {
                log.warn("SoftwareKmsProvider not found, cannot perform key cleanup")
                return@withContext
            }

            val keyStore = provider.keyStore as SoftwareKeyStoreService

            // Step 1: Get all key aliases from stored documents
            val documents = mdocStore.getDocuments()
            val documentKeyAliases = documents.map { it.keyAlias }.toSet()
            val documentCertAliases = documents.mapNotNull { it.certAlias }.toSet()
            
            log.info("Found ${documents.size} documents with ${documentKeyAliases.size} unique key aliases")

            // Step 2: Combine all aliases that should be preserved
            val aliasesToPreserve = documentKeyAliases + documentCertAliases + PRESERVED_KEY_ALIASES
            log.info("Total aliases to preserve: ${aliasesToPreserve.size}")
            log.debug("Preserved aliases: ${aliasesToPreserve.joinToString(", ")}")

            // Step 3: Clean up keys
            val allKeys = keyStore.listKeys()
            log.info("Found ${allKeys.size} keys in keystore")
            
            var deletedCount = 0
            allKeys.forEach { keyInfo ->
                if (keyInfo.alias !in aliasesToPreserve) {
                    try {
                        keyStore.deleteKey(keyInfo)
                        log.info("Deleted ephemeral key with alias: ${keyInfo.alias}")
                        deletedCount++
                    } catch (e: CancellationException) {
                        val isCompositionCancellation = e.message?.contains("left the composition") == true ||
                                e::class.simpleName?.contains("Composition") == true
                        if (isCompositionCancellation) {
                            log.warn("Skipped key deletion due to composition scope issue: ${keyInfo.alias}")
                        } else {
                            throw e
                        }
                    } catch (e: Exception) {
                        log.error("Failed to delete key with alias ${keyInfo.alias}: ${e.message}")
                    }
                }
            }

            // Step 4: Clean up certificates
            val allCertAliases = keyStore.listCertificateAliases()
            log.info("Found ${allCertAliases.size} certificates in keystore")
            
            var deletedCertCount = 0
            allCertAliases.forEach { certAlias ->
                if (certAlias !in aliasesToPreserve) {
                    try {
                        keyStore.deleteCertificate(certAlias)
                        log.info("Deleted ephemeral certificate with alias: $certAlias")
                        deletedCertCount++
                    } catch (e: CancellationException) {
                        val isCompositionCancellation = e.message?.contains("left the composition") == true ||
                                e::class.simpleName?.contains("Composition") == true
                        if (isCompositionCancellation) {
                            log.warn("Skipped certificate deletion due to composition scope issue: $certAlias")
                        } else {
                            throw e
                        }
                    } catch (e: Exception) {
                        log.error("Failed to delete certificate with alias $certAlias: ${e.message}")
                    }
                }
            }

            log.info("Key cleanup completed: deleted $deletedCount keys and $deletedCertCount certificates")

        } catch (e: CancellationException) {
            val isCompositionCancellation = e.message?.contains("left the composition") == true ||
                    e::class.simpleName?.contains("Composition") == true
            if (isCompositionCancellation) {
                log.warn("Key cleanup cancelled due to composition scope being left - this is expected when navigating away")
            } else {
                log.error("Key cleanup cancelled: ${e.message}", exception = e)
            }
        } catch (e: Exception) {
            log.error("Error during key cleanup: ${e.message}", exception = e)
        } finally {
            cleanupMutex.withLock {
                isCleanupRunning = false
            }
        }
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val keyCleanupService: KeyCleanupService
    }
}
