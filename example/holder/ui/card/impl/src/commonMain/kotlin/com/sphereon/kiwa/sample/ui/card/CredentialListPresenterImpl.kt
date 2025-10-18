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

package com.sphereon.kiwa.sample.ui.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.sphereon.crypto.core.kms.KeyManagerService
import com.sphereon.crypto.kms.keystore.software.SoftwareKeyStoreService
import com.sphereon.crypto.kms.provider.software.SoftwareKmsProvider
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.auth.AuthSessionService
import com.sphereon.kiwa.sample.ui.core.backstack.LocalBackstackScope
import com.sphereon.kiwa.sample.ui.core.logs.LogViewerScreenPresenter
import com.sphereon.kiwa.sample.ui.elicense.assignment.ElicenseAssignmentScreenPresenter
import com.sphereon.kiwa.sample.ui.elicense.engagement.qr.MdocEngagementPresenter
import com.sphereon.kiwa.sample.ui.elicense.issuance.TestPidIssuer
import com.sphereon.kiwa.sample.ui.elicense.settings.SubscriptionKeyPresenter
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleDocumentEntry
import com.sphereon.mdoc.data.device.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
@ContributesBinding(SessionScope::class, boundType = CredentialListPresenter::class)
@Suppress("LongParameterList") // DI constructor requires all these dependencies
class CredentialListPresenterImpl(
    val pidIssuer: TestPidIssuer,
    val storage: SimpleMdocStore,
    val kms: KeyManagerService,
    val detailsPresenter: CredentialDetailsPresenter,
    val mdocEngagementQrPresenter: MdocEngagementPresenter,
    val elicenseAssignmentScreenPresenter: ElicenseAssignmentScreenPresenter,
    val authSessionService: AuthSessionService,
    val subscriptionKeyPresenter: SubscriptionKeyPresenter,
    val logViewerScreenPresenter: LogViewerScreenPresenter,
) : CredentialListPresenter {
    @Composable
    override fun present(input: Unit): CredentialListPresenter.Model {
        println("CredentialListPresenter present")

        var documentEntries by remember { mutableStateOf<List<SimpleDocumentEntry>>(emptyList()) }
        var pendingDelete by remember { mutableStateOf<Document?>(null) }
        val scope = rememberCoroutineScope()
        val backstack = checkNotNull(LocalBackstackScope.current)

        LaunchedEffect(Unit) {
            storage.documentsFlow.collect { entries -> documentEntries = entries }
        }

        val onStateEvent: (CredentialListPresenter.StateEvent) -> Unit = { event ->
            println("CredentialListPresenter: Received event: ${event.javaClass.simpleName}")
            when (event) {
                is CredentialListPresenter.StateEvent.CreateNewCredential -> {
                    scope.launch { pidIssuer.issuePid() }
                }

                is CredentialListPresenter.StateEvent.AssignLicense -> {
                    backstack.push(elicenseAssignmentScreenPresenter)
                }

                is CredentialListPresenter.StateEvent.OpenCredentialDetails -> {
                    backstack.push(CredentialDetailsScreenPresenter(event.document, detailsPresenter))
                }

                is CredentialListPresenter.StateEvent.ConfirmDelete -> {
                    scope.launch {
                        storage.removeDocument(event.document)
                        pendingDelete = null
                    }
                }

                is CredentialListPresenter.StateEvent.CancelDelete -> pendingDelete = null
                is CredentialListPresenter.StateEvent.AttendedPresentation -> {
                    backstack.push(
                        MdocEngagementScreenPresenter(
                            MdocEngagementPresenter.Input(),
                            mdocEngagementQrPresenter
                        )
                    )
                }

                is CredentialListPresenter.StateEvent.GoToHome -> pendingDelete = null
                is CredentialListPresenter.StateEvent.SubscriptionKey -> {
                    backstack.push(SubscriptionKeyScreenPresenter(subscriptionKeyPresenter))
                }

                is CredentialListPresenter.StateEvent.Logout -> {
                    authSessionService.logout().also { backstack.clear() }
                }

                is CredentialListPresenter.StateEvent.ViewLogs -> {
                    backstack.push(logViewerScreenPresenter)
                }

                is CredentialListPresenter.StateEvent.ClearLicenses -> {
                    scope.launch { clearLicensesAndKeys() }
                }

                else -> {}
            }
        }

        val items = createCredentialRows(documentEntries) { doc -> pendingDelete = doc }

        return CredentialListPresenter.Model.CredentialList(
            items = items,
            pendingDelete = pendingDelete,
            onStateEvent = onStateEvent
        )
    }

    private suspend fun clearLicensesAndKeys() {
        try {
            withContext(Dispatchers.IO) {
                // Clear all documents from storage
                storage.clearAll()

                // Clear all keys and certificates from the KMS software keystore
                val provider = kms.getProviderById(kms.defaultProviderId()) as? SoftwareKmsProvider

                if (provider != null) {
                    val keyStore = provider.keyStore as SoftwareKeyStoreService

                    // List all keys and delete them one by one
                    val allKeys = keyStore.listKeys()
                    println("CredentialListPresenter: Found ${allKeys.size} keys to delete")

                    allKeys.forEach { keyInfo ->
                        println("CredentialListPresenter: Deleting key with alias: ${keyInfo.alias}")
                        keyStore.deleteKey(keyInfo)
                    }

                    // List all certificate aliases and delete them
                    val allCertAliases = keyStore.listCertificateAliases()
                    println("CredentialListPresenter: Found ${allCertAliases.size} certificates to delete")

                    allCertAliases.forEach { certAlias ->
                        println("CredentialListPresenter: Deleting certificate with alias: $certAlias")
                        keyStore.deleteCertificate(certAlias)
                    }

                    println("CredentialListPresenter: Successfully cleared all keys and certificates from keystore")
                } else {
                    println("CredentialListPresenter: SoftwareKmsProvider not found in KMS")
                }
            }
        } catch (e: SecurityException) {
            println("CredentialListPresenter: Security error clearing licenses and keys: ${e.message}")
        } catch (e: java.io.IOException) {
            println("CredentialListPresenter: IO error clearing licenses and keys: ${e.message}")
        }
    }

    private class MdocEngagementScreenPresenter(
        private val input: MdocEngagementPresenter.Input,
        private val delegate: MdocEngagementPresenter
    ) : MoleculePresenter<Any, MdocEngagementPresenter.Model> {

        @Composable
        override fun present(input: Any): MdocEngagementPresenter.Model {
            return delegate.present(this.input)
        }
    }

    private class SubscriptionKeyScreenPresenter(
        private val delegate: SubscriptionKeyPresenter
    ) : MoleculePresenter<Any, SubscriptionKeyPresenter.Model> {

        @Composable
        override fun present(input: Any): SubscriptionKeyPresenter.Model {
            return delegate.present(Unit)
        }
    }

    private fun createCredentialRows(
        documentEntries: List<SimpleDocumentEntry>,
        onDelete: (Document) -> Unit
    ): List<CredentialListItemPresenter.Model.Row> {
        return documentEntries.map { entry ->
            CredentialListItemPresenter.Model.Row(
                CredentialCardPresenter.Input.fromDocument(
                    entry.document,
                    variant = CredentialCardPresenter.Variant.Mini
                ).toModel(),
                document = entry.document,
                onDelete = { onDelete(entry.document) }
            )
        }
    }

    /**
     * Navigates to the engagement presenter with an existing engagement instance.
     * This method can be called from external components like NFC services when
     * an NFC engagement has already been initiated.
     */
    fun navigateToEngagementWithNfc() {
        // We need access to the backstack from the current composition
        // This method would need to be called from within a composable context
        // For now, we'll store the engagement and trigger navigation on next recomposition
        // TODO: Implement proper navigation handling for NFC events
    }

    @ContributesTo(SessionScope::class)
    interface Component {
        val presenter: CredentialListPresenter

        /**
         * Provides access to navigation methods for external components like NFC services.
         * This allows NFC engagement events to trigger navigation to the engagement presenter
         * with an existing engagement instance.
         */
        val credentialListPresenter: CredentialListPresenterImpl
    }
}
