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

package com.sphereon.ui.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import com.sphereon.crypto.core.kms.IKeyManagerService
import com.sphereon.crypto.kms.keystore.software.SoftwareKeyStoreService
import com.sphereon.crypto.kms.provider.software.ISoftwareKmsProvider
import com.sphereon.di.session.SureSessionScope
import com.sphereon.mdoc.data.device.Document
import com.sphereon.mdoc.engagement.IEngagementInstance
import com.sphereon.ui.auth.IAuthSessionService
import com.sphereon.ui.core.backstack.LocalBackstackScope
import com.sphereon.ui.core.logs.ILogViewerScreenPresenter
import com.sphereon.ui.elicense.assignment.IElicenseAssignmentScreenPresenter
import com.sphereon.ui.elicense.engagement.qr.IMdocEngagementQrPresenter
import com.sphereon.ui.elicense.issuance.ITestPidIssuer
import com.sphereon.ui.elicense.settings.ISubscriptionKeyPresenter
import com.sphereon.ui.elicense.store.ISimpleMdocStore
import com.sphereon.ui.elicense.store.SimpleDocumentEntry

@Inject
@ContributesBinding(SureSessionScope::class, boundType = ICredentialListPresenter::class)
class CredentialListPresenter(
    val pidIssuer: ITestPidIssuer,
    val storage: ISimpleMdocStore,
    val kms: IKeyManagerService,
    val detailsPresenter: ICredentialDetailsPresenter,
    val mdocEngagementQrPresenter: IMdocEngagementQrPresenter,
    val elicenseAssignmentScreenPresenter: IElicenseAssignmentScreenPresenter,
    val authSessionService: IAuthSessionService,
    val subscriptionKeyPresenter: ISubscriptionKeyPresenter,
    val logViewerScreenPresenter: ILogViewerScreenPresenter,
) : ICredentialListPresenter {
    @Composable
    override fun present(input: Unit): ICredentialListPresenter.Model {
        println("CredentialListPresenter present")

        var documentEntries by remember { mutableStateOf<List<SimpleDocumentEntry>>(emptyList()) }
        var pendingDelete by remember { mutableStateOf<Document?>(null) }
        val scope = rememberCoroutineScope()
        val backstack = checkNotNull(LocalBackstackScope.current)

        // Reactive updates from storage
        LaunchedEffect(Unit) {
            storage.documentsFlow.collect { entries -> documentEntries = entries }
        }

        // TODO: NFC engagement event handling would require moving event system to a shared module
        // For now, this is handled by the LandingPresenter

        val onStateEvent: (ICredentialListPresenter.StateEvent) -> Unit = { event ->
            println("CredentialListPresenter: Received event: ${event.javaClass.simpleName}")
            when (event) {
                is ICredentialListPresenter.StateEvent.CreateNewCredential -> {
                    scope.launch {
                        pidIssuer.issuePid()
                        // storage flow will emit new list
                    }
                }

                is ICredentialListPresenter.StateEvent.AssignLicense -> {
                    backstack.push(elicenseAssignmentScreenPresenter)
                }

                is ICredentialListPresenter.StateEvent.OpenCredentialDetails -> {
                    backstack.push(CredentialDetailsScreenPresenter(event.document, detailsPresenter))
                }

                is ICredentialListPresenter.StateEvent.ConfirmDelete -> {
                    scope.launch {
                        storage.removeDocument(event.document)
                        pendingDelete = null
                        // storage flow will emit new list
                    }
                }

                is ICredentialListPresenter.StateEvent.CancelDelete -> {
                    pendingDelete = null
                }

                is ICredentialListPresenter.StateEvent.AttendedPresentation -> {
                    backstack.push(
                        MdocEngagementScreenPresenter(
                            IMdocEngagementQrPresenter.Input(),
                            mdocEngagementQrPresenter
                        )
                    )
                }

                is ICredentialListPresenter.StateEvent.GoToHome -> {
                    // Home action - we're already on the credential list, so this could:
                    // 1. Scroll to top (if we had a scroll state)
                    // 2. Refresh the list
                    // 3. Clear any pending actions
                    pendingDelete = null
                    // For now, just clear any pending delete action
                }

                is ICredentialListPresenter.StateEvent.SubscriptionKey -> {
                    backstack.push(SubscriptionKeyScreenPresenter(subscriptionKeyPresenter))
                }

                is ICredentialListPresenter.StateEvent.Logout -> {
                    authSessionService.logout().also { backstack.clear() }
                }

                is ICredentialListPresenter.StateEvent.ViewLogs -> {
                    backstack.push(logViewerScreenPresenter)
                }

                is ICredentialListPresenter.StateEvent.ClearLicenses -> {
                    scope.launch {
                        try {
                            // Both storage and keystore operations are disk I/O intensive
                            withContext(Dispatchers.IO) {
                                // Clear all documents from storage (disk I/O operation)
                                storage.clearAll()

                                // Clear all keys and certificates from the KMS software keystore
                                val provider = kms.getProviderById(kms.defaultProviderId()) as? ISoftwareKmsProvider

                                if (provider != null) {
                                    val keyStore = provider.keyStore as SoftwareKeyStoreService

                                    // List all keys and delete them one by one (use forEach to avoid ambiguity)
                                    val allKeys = keyStore.listKeys()
                                    println("CredentialListPresenter: Found ${allKeys.size} keys to delete")

                                    allKeys.forEach { keyInfo ->
                                        println("CredentialListPresenter: Deleting key with alias: ${keyInfo.alias}")
                                        keyStore.deleteKey(keyInfo)
                                    }

                                    // List all certificate aliases and delete them
                                    val allCertAliases = keyStore.listCertificateAliases()
                                    println(
                                        "CredentialListPresenter: Found ${allCertAliases.size} certificates to delete"
                                    )

                                    allCertAliases.forEach { certAlias ->
                                        println("CredentialListPresenter: Deleting certificate with alias: $certAlias")
                                        keyStore.deleteCertificate(certAlias)
                                    }

                                    println(
                                        "CredentialListPresenter: Successfully cleared all keys and certificates from keystore"
                                    )
                                } else {
                                    println("CredentialListPresenter: SoftwareKmsProvider not found in KMS")
                                }
                            }
                        } catch (e: Exception) {
                            println("CredentialListPresenter: Error clearing licenses and keys: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }

                else -> {}
            }
        }

        val items = documentEntries.map { entry ->
            ICredentialListItemPresenter.Model.Row(
                ICredentialCardPresenter.Input.fromDocument(
                    entry.document,
                    variant = ICredentialCardPresenter.Variant.Mini
                ).toModel(),
                document = entry.document,
                onDelete = { pendingDelete = entry.document }
            )
        }

        return ICredentialListPresenter.Model.CredentialList(
            items = items,
            pendingDelete = pendingDelete,
            onStateEvent = onStateEvent
        )
    }

    private class MdocEngagementScreenPresenter(
        private val input: IMdocEngagementQrPresenter.Input,
        private val delegate: IMdocEngagementQrPresenter
    ) : software.amazon.app.platform.presenter.molecule.MoleculePresenter<Any, IMdocEngagementQrPresenter.Model> {

        @Composable
        override fun present(input: Any): IMdocEngagementQrPresenter.Model {
            return delegate.present(this.input)
        }
    }

    private class SubscriptionKeyScreenPresenter(
        private val delegate: ISubscriptionKeyPresenter
    ) : software.amazon.app.platform.presenter.molecule.MoleculePresenter<Any, ISubscriptionKeyPresenter.Model> {

        @Composable
        override fun present(input: Any): ISubscriptionKeyPresenter.Model {
            return delegate.present(Unit)
        }
    }

    /**
     * Navigates to the engagement presenter with an existing engagement instance.
     * This method can be called from external components like NFC services when
     * an NFC engagement has already been initiated.
     *
     * @param existingEngagement The engagement instance created from NFC interaction
     */
    fun navigateToEngagementWithNfc(existingEngagement: IEngagementInstance) {
        // We need access to the backstack from the current composition
        // This method would need to be called from within a composable context
        // For now, we'll store the engagement and trigger navigation on next recomposition
        // TODO: Implement proper navigation handling for NFC events
    }

    @ContributesTo(SureSessionScope::class)
    interface Component {
        val presenter: ICredentialListPresenter

        /**
         * Provides access to navigation methods for external components like NFC services.
         * This allows NFC engagement events to trigger navigation to the engagement presenter
         * with an existing engagement instance.
         */
        val credentialListPresenter: CredentialListPresenter
    }
}
